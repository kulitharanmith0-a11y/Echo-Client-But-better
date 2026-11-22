package dev.dov.hack.echo.client.mixin;

import dev.dov.hack.echo.client.EchoClient;
import dev.dov.hack.echo.client.event.render.OcclusionGraphEvent;
import net.minecraft.client.render.ChunkRenderingDataPreparer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ChunkRenderingDataPreparer.class)
public class ChunkRenderingDataPreparerMixin {
    @ModifyVariable(method = "updateSectionOcclusionGraph", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private boolean modifyUpdateSectionOcclusionGraph(boolean cullChunks) {
        if (EchoClient.eventsDisabled) {
            return cullChunks;
        }
        if (!cullChunks) {
            return false;
        }
        OcclusionGraphEvent event = new OcclusionGraphEvent();
        event.post();
        return event.cancelled ? false : cullChunks;
    }
}
