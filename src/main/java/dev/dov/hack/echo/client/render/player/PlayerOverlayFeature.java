package dev.dov.hack.echo.client.render.player;

import dev.dov.hack.echo.client.modules.impl.render.ChinaHat;
import dev.dov.hack.echo.client.modules.impl.render.Freecam;
import dev.dov.hack.echo.client.gfx.MatrixStackTransforms;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;

public class PlayerOverlayFeature extends FeatureRenderer<PlayerEntityRenderState, PlayerEntityModel> {
    private static final float MAX_MODEL_SCALE_SQUARED = 16.0f;
    private static final float HEAD_ROTATION_DEGREES = 180.0f;
    private static final float HEAD_OFFSET = 0.42f;

    public PlayerOverlayFeature(FeatureRendererContext<PlayerEntityRenderState, PlayerEntityModel> context) {
        super(context);
    }

    @Override
    public void render(MatrixStack matrices, OrderedRenderCommandQueue commands, int light, PlayerEntityRenderState state, float limbAngle, float limbDistance) {
        boolean bypass = Freecam.isPerspectiveRefreshBypassed();
        ChinaHat chinaHat = ChinaHat.instance;
        if (!bypass && chinaHat == null) {
            return;
        }
        if (!chinaHat.isEnabled()) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || state.id != client.player.getId()) {
            return;
        }

        Matrix4f matrix = matrices.peek().getPositionMatrix();
        float scaleSquared = matrix.m00() * matrix.m00()
                + matrix.m01() * matrix.m01() + matrix.m02() * matrix.m02();
        if (!bypass) {
            if (scaleSquared > MAX_MODEL_SCALE_SQUARED) {
                return;
            }
            matrices.push();
        }

        PlayerEntityModel model = getContextModel();
        model.getRootPart().applyTransform(matrices);
        model.head.applyTransform(matrices);
        MatrixStackTransforms.rotate(matrices, RotationAxis.POSITIVE_Z.rotationDegrees(HEAD_ROTATION_DEGREES));
        matrices.translate(0.0f, HEAD_OFFSET, 0.0f);
        chinaHat.render(matrices.peek().getPositionMatrix());
        matrices.pop();
    }
}
