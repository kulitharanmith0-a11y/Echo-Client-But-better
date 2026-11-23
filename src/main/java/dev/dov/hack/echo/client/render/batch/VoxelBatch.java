package dev.dov.hack.echo.client.render.batch;

import dev.dov.hack.echo.client.render.NativeMemoryWrites;
import dev.dov.hack.echo.client.render.world.WorldRenderContext;

import dev.dov.hack.echo.client.render.world.SectionMeshUpdate;
import dev.dov.hack.echo.client.EchoClient;
import dev.dov.hack.echo.client.chunk.SectionData;
import dev.dov.hack.echo.client.chunk.SectionUpdateListener;
import dev.dov.hack.echo.client.gfx.VoxelAllocationSizes;
import dev.dov.hack.echo.client.gfx.VoxelBuffer;
import dev.dov.hack.echo.client.gfx.buffer.Buffer;
import dev.dov.hack.echo.client.gfx.buffer.BufferAccess;
import dev.dov.hack.echo.client.gfx.buffer.BufferInfo;
import dev.dov.hack.echo.client.gfx.command.CommandEncoder;
import dev.dov.hack.echo.client.gfx.command.RenderPass;
import dev.dov.hack.echo.client.gfx.device.RenderDevice;
import dev.dov.hack.echo.client.gfx.pipeline.GraphicsPipeline;
import dev.dov.hack.echo.client.gfx.pipeline.GraphicsPipelineInfo;
import dev.dov.hack.echo.client.gfx.vertex.VertexFormat;
import java.util.ArrayDeque;
import java.util.Arrays;
import net.minecraft.util.math.Vec3d;
import org.joml.FrustumIntersection;
import org.lwjgl.system.MemoryUtil;
import dev.dov.hack.echo.client.render.batch.RenderBatch;

public final class VoxelBatch implements RenderBatch, SectionUpdateListener {
    private static final int FACE_BYTES = 8;
    private static final int PALETTE_ENTRIES = 4096;
    private static final long PALETTE_BYTES = 16384L;
    private static final long PUSH_CONSTANT_BYTES = 16L;

    private final RenderDevice device;
    private final Buffer cameraUniforms;
    private final GraphicsPipeline pipeline;
    private final Buffer paletteUniforms;
    private final long pushConstants;
    private final ArrayDeque<SectionMeshUpdate> pendingUpdates;
    private final int[] palette;
    private boolean paletteDirty;
    private final VoxelBuffer meshStorage;
    private int sectionCount;
    private long[] meshOffsets;
    private int[] allocationClasses;
    private int[] directionFaceCounts;
    private int[] sectionPositions;
    private final FrustumIntersection frustum;

    public VoxelBatch(RenderDevice device, Buffer cameraUniforms) {
        pushConstants = MemoryUtil.nmemAllocChecked(PUSH_CONSTANT_BYTES);
        pendingUpdates = new ArrayDeque<>();
        palette = new int[PALETTE_ENTRIES];
        meshOffsets = new long[256];
        allocationClasses = new int[256];
        directionFaceCounts = new int[1536];
        sectionPositions = new int[768];
        frustum = new FrustumIntersection();
        this.device = device;
        this.cameraUniforms = cameraUniforms;
        GraphicsPipelineInfo info = new GraphicsPipelineInfo(device, "shaders/wgsl/3d/voxel.wgsl")
                .vertexAttribute(0, VertexFormat.Int2);
        info.pushConstantBytes = 16;
        info.cullEnabled = true;
        info.blendEnabled = false;
        pipeline = device.createGraphicsPipeline(info);
        paletteUniforms = device.createBuffer(BufferInfo.builder().usage(2).size(PALETTE_BYTES)
                .mapped(true).explicitFlush(true).build());
        meshStorage = new VoxelBuffer(device);
    }

    @Override
    public void onSectionAdded(int index, SectionData section) {
        onSectionUpdated(index, section);
    }

    @Override
    public void onSectionUpdated(int index, SectionData section) {
        synchronized (pendingUpdates) {
            pendingUpdates.add(new SectionMeshUpdate(SectionMeshUpdate.UPSERT, index, section.faces().toIntArray(), section.faceCounts().clone(), section.sectionX, section.sectionY, section.sectionZ));
        }
    }

    @Override
    public void onSectionRemoved(int lastIndex) {
        synchronized (pendingUpdates) {
            pendingUpdates.add(new SectionMeshUpdate(SectionMeshUpdate.REMOVE, lastIndex, null, null, 0, 0, 0));
        }
    }

    @Override
    public void onCleared() {
        synchronized (pendingUpdates) {
            pendingUpdates.add(new SectionMeshUpdate(SectionMeshUpdate.CLEAR, 0, null, null, 0, 0, 0));
        }
    }

    @Override
    public void onPaletteUpdated(int[] colors) {
        synchronized (pendingUpdates) {
            System.arraycopy(colors, 0, palette, 0, PALETTE_ENTRIES);
            paletteDirty = true;
        }
    }

    
    @Override
    public void reset() {
        synchronized (pendingUpdates) {
            while (!pendingUpdates.isEmpty()) {
                applyUpdate(pendingUpdates.poll());
            }
            if (paletteDirty) {
                long address = paletteUniforms.mappedAddress();
                for (int index = 0; index < PALETTE_ENTRIES; index++) {
                    MemoryUtil.memPutInt(address + (long) index * 4L, palette[index]);
                }
                paletteUniforms.flush(0L, PALETTE_BYTES);
                paletteDirty = false;
            }
        }
    }

    private void applyUpdate(SectionMeshUpdate update) {
        switch (update.operation()) {
            case SectionMeshUpdate.UPSERT -> {
                ensureCapacity(update.sectionIndex() + 1);
                if (update.sectionIndex() < sectionCount) {
                    releaseSection(update.sectionIndex());
                }
                sectionCount = Math.max(sectionCount, update.sectionIndex() + 1);
                uploadSection(update);
            }
            case SectionMeshUpdate.REMOVE -> {
                if (update.sectionIndex() < sectionCount) {
                    releaseSection(update.sectionIndex());
                    sectionCount = update.sectionIndex();
                }
            }
            case SectionMeshUpdate.CLEAR -> {
                sectionCount = 0;
                meshStorage.reset();
            }
            default -> {
            }
        }
    }

    private void uploadSection(SectionMeshUpdate update) {
        int index = update.sectionIndex();
        int faceCount = update.packedFaces().length / 2;
        System.arraycopy(update.faceCounts(), 0, directionFaceCounts, index * 6, 6);
        sectionPositions[index * 3] = update.sectionX();
        sectionPositions[index * 3 + 1] = update.sectionY();
        sectionPositions[index * 3 + 2] = update.sectionZ();
        if (faceCount == 0) {
            allocationClasses[index] = 0;
            meshOffsets[index] = 0L;
            return;
        }
        int sizeClass = VoxelAllocationSizes.sizeClassFor(faceCount);
        long offset = meshStorage.allocate(sizeClass);
        int[] packed = update.packedFaces();
        long address = meshStorage.getBuffer().mappedAddress() + offset;
        for (int element = 0; element < faceCount * 2; element++) {
            MemoryUtil.memPutInt(address + (long) element * 4L, packed[element]);
        }
        meshStorage.getBuffer().flush(offset, (long) faceCount * FACE_BYTES);
        allocationClasses[index] = sizeClass;
        meshOffsets[index] = offset;
    }

    private void releaseSection(int index) {
        if (allocationClasses[index] != 0) {
            meshStorage.release(meshOffsets[index], allocationClasses[index]);
            allocationClasses[index] = 0;
        }
    }

    private void ensureCapacity(int required) {
        if (required <= meshOffsets.length) {
            return;
        }
        int capacity = Integer.highestOneBit(required - 1) << 1;
        meshOffsets = Arrays.copyOf(meshOffsets, capacity);
        allocationClasses = Arrays.copyOf(allocationClasses, capacity);
        directionFaceCounts = Arrays.copyOf(directionFaceCounts, capacity * 6);
        sectionPositions = Arrays.copyOf(sectionPositions, capacity * 3);
    }

    @Override
    public void prepare(CommandEncoder commands) {
        commands.transitionBuffer(meshStorage.getBuffer().wholeSlice(), 1, BufferAccess.VERTEX_READ);
        commands.transitionBuffer(paletteUniforms.wholeSlice(), 1, BufferAccess.UNIFORM_READ);
    }

    @Override
    public void draw(CommandEncoder commands, RenderPass pass) {
        WorldRenderContext context = EchoClient.worldRenderer;
        if (sectionCount == 0 || context.viewProjectionMatrix == null) {
            return;
        }
        frustum.set(context.viewProjectionMatrix);
        Vec3d camera = context.cameraPosition;
        pass.setPipeline(pipeline);
        pass.setBuffer(0, cameraUniforms.wholeSlice());
        pass.setBuffer(1, paletteUniforms.wholeSlice());
        pass.setVertexBuffer(meshStorage.getBuffer().wholeSlice());
        for (int section = 0; section < sectionCount; section++) {
            float relativeX = (float) ((sectionPositions[section * 3] << 4) - camera.x);
            float relativeY = (float) ((sectionPositions[section * 3 + 1] << 4) - camera.y);
            float relativeZ = (float) ((sectionPositions[section * 3 + 2] << 4) - camera.z);
            if (allocationClasses[section] == 0
                    || !frustum.testAab(relativeX, relativeY, relativeZ, relativeX + 16.0F, relativeY + 16.0F, relativeZ + 16.0F)) {
                continue;
            }
            NativeMemoryWrites.putFloat4(pushConstants, relativeX, relativeY, relativeZ, 0.0F);
            pass.setPushConstants(pushConstants, 16);
            int firstInstance = (int) (meshOffsets[section] / FACE_BYTES);
            int runStart = firstInstance;
            int runCount = 0;
            for (int direction = 0; direction < 6; direction++) {
                int count = directionFaceCounts[section * 6 + direction];
                if (count == 0) {
                    continue;
                }
                float relativeAxis = direction < 2 ? relativeY : direction < 4 ? relativeX : relativeZ;
                boolean visible = (direction & 1) == 0 ? relativeAxis > -16.0F : relativeAxis < 0.0F;
                if (visible) {
                    if (runCount == 0) {
                        runStart = firstInstance;
                    }
                    runCount += count;
                } else if (runCount != 0) {
                    pass.drawInstanced(4, runCount, 0, runStart);
                    runCount = 0;
                }
                firstInstance += count;
            }
            if (runCount != 0) {
                pass.drawInstanced(4, runCount, 0, runStart);
            }
        }
    }

    private boolean destroyed;

    @Override
    public void destroy() {
        if (destroyed) {
            return;
        }
        destroyed = true;
        if (meshStorage != null) {
            meshStorage.destroy();
        }
        if (paletteUniforms != null) {
            device.release(paletteUniforms);
        }
        if (pushConstants != 0L) {
            MemoryUtil.nmemFree(pushConstants);
        }
    }
}
