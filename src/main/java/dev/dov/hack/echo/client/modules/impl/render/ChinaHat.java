package dev.dov.hack.echo.client.modules.impl.render;

import dev.dov.hack.echo.client.EchoClient;
import dev.dov.hack.echo.client.modules.Module;
import dev.dov.hack.echo.client.modules.ModuleInfo;
import dev.dov.hack.echo.client.render.world.DepthTestedWorldGeometry;
import dev.dov.hack.echo.client.render.world.WorldRenderContext;
import dev.dov.hack.echo.client.settings.BooleanSetting;
import dev.dov.hack.echo.client.settings.ChoiceSetting;
import dev.dov.hack.echo.client.settings.ColorSetting;
import dev.dov.hack.echo.client.settings.FloatSetting;
import dev.dov.hack.echo.client.utils.strings.Concat;
import java.awt.Color;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class ChinaHat extends Module {
    public static ChinaHat instance;
    private static final double FULL_CIRCLE = Math.PI * 2;
    private static final int SEGMENTS = 96;
    private static ChoiceSetting mode;
    private static ColorSetting color;
    private static FloatSetting opacity;
    private static FloatSetting spinSpeed;
    private static FloatSetting radius;
    private static FloatSetting height;
    private static BooleanSetting outline;

    public ChinaHat() {
        super(new ModuleInfo(dev.dov.hack.echo.client.init.CatalogText.literal("China Hat"), dev.dov.hack.echo.client.init.CatalogText.literal("Wears a translucent cone hat on your head"), dev.dov.hack.echo.client.modules.ModuleCategory.RENDER));
    }

    @Override
    public void postInitialize() {
        instance = this;
        color.setVisibilityPredicate(ignored -> mode.isSelected("Static"));
        opacity.setVisibilityPredicate(ignored -> mode.isSelected("Rainbow"));
        spinSpeed.setVisibilityPredicate(ignored -> mode.isSelected("Rainbow"));
        color.tooltip("Appearance");
        opacity.tooltip("Appearance");
        spinSpeed.tooltip("Timing");
        outline.tooltip("Appearance");
    }

    public void render(Matrix4f transform) {
        WorldRenderContext context = EchoClient.worldRenderer;
        if (context == null) {
            return;
        }
        DepthTestedWorldGeometry geometry = context.getDepthTestedGeometry();
        Vec3d camera = context.cameraPosition;
        float ringRadius = radius.getValue();
        float coneHeight = height.getValue();
        boolean rainbow = mode.isSelected("Rainbow");
        int alpha = rainbow ? Math.round(opacity.getValue() * 255.0f) : color.getAlpha();
        float hueOffset = rainbow
                ? (float) ((System.currentTimeMillis() % 400000L) / 1000.0 * spinSpeed.getValue())
                : 0.0f;
        boolean drawOutline = outline.getValue();
        Vector3f transformed = new Vector3f();
        transform.transformPosition(0.0f, coneHeight, 0.0f, transformed);
        double tipX = camera.x + transformed.x;
        double tipY = camera.y + transformed.y;
        double tipZ = camera.z + transformed.z;
        double[] rimX = new double[SEGMENTS + 1];
        double[] rimY = new double[SEGMENTS + 1];
        double[] rimZ = new double[SEGMENTS + 1];
        int[] rimColors = new int[SEGMENTS + 1];
        for (int index = 0; index <= SEGMENTS; index++) {
            float fraction = (float) index / 96.0f;
            double angle = FULL_CIRCLE * fraction;
            transform.transformPosition((float) (-Math.sin(angle) * ringRadius), 0.0f, (float) (Math.cos(angle) * ringRadius), transformed);
            rimX[index] = camera.x + transformed.x;
            rimY[index] = camera.y + transformed.y;
            rimZ[index] = camera.z + transformed.z;
            rimColors[index] = vertexColor(rainbow, fraction, hueOffset, alpha);
        }

        for (int pass = 0; pass < 2; pass++) {
            boolean positiveFacingPass = pass == 0;
            for (int index = 1; index <= SEGMENTS; index++) {
                double edgeX = rimX[index] - rimX[index - 1];
                double edgeY = rimY[index] - rimY[index - 1];
                double edgeZ = rimZ[index] - rimZ[index - 1];
                double apexX = tipX - rimX[index - 1];
                double apexY = tipY - rimY[index - 1];
                double apexZ = tipZ - rimZ[index - 1];
                double normalX = edgeY * apexZ - edgeZ * apexY;
                double normalY = edgeZ * apexX - edgeX * apexZ;
                double normalZ = edgeX * apexY - edgeY * apexX;
                double toCameraX = camera.x - (tipX + rimX[index - 1] + rimX[index]) / 3.0;
                double toCameraY = camera.y - (tipY + rimY[index - 1] + rimY[index]) / 3.0;
                double toCameraZ = camera.z - (tipZ + rimZ[index - 1] + rimZ[index]) / 3.0;
                boolean positiveFacing = normalX * toCameraX + normalY * toCameraY + normalZ * toCameraZ > 0.0;
                if (positiveFacing != positiveFacingPass) {
                    continue;
                }
                int tipColor = vertexColor(rainbow, ((float) index - 0.5f) / 96.0f, hueOffset, alpha);
                geometry.drawTriangle(tipX, tipY, tipZ, tipColor, rimX[index - 1], rimY[index - 1], rimZ[index - 1], rimColors[index - 1], rimX[index], rimY[index], rimZ[index], rimColors[index]);
            }
        }
        if (drawOutline) {
            for (int index = 1; index <= SEGMENTS; index++) {
                geometry.drawLine(rimX[index - 1], rimY[index - 1], rimZ[index - 1], rimX[index], rimY[index], rimZ[index], rimColors[index - 1] | 0xff000000);
            }
        }
    }

    private static int vertexColor(boolean rainbow, float fraction, float hueOffset, int alpha) {
        if (!rainbow) {
            return packArgb(color.getRed(), color.getGreen(), color.getBlue(), alpha);
        }
        int rgb = Color.HSBtoRGB(fraction + hueOffset, 1.0f, 1.0f);
        return alpha << 24 | rgb & 0xffffff;
    }

    private static int packArgb(int red, int green, int blue, int alpha) {
        return (alpha & 255) << 24 | (red & 255) << 16 | (green & 255) << 8 | blue & 255;
    }

    
    public static void appendBootstrapSettings(ChinaHat Module) {
        dev.dov.hack.echo.client.init.CatalogSettings.append(Module, ChinaHat.mode);
        dev.dov.hack.echo.client.init.CatalogSettings.append(Module, ChinaHat.color);
        dev.dov.hack.echo.client.init.CatalogSettings.append(Module, ChinaHat.opacity);
        dev.dov.hack.echo.client.init.CatalogSettings.append(Module, ChinaHat.spinSpeed);
        dev.dov.hack.echo.client.init.CatalogSettings.append(Module, ChinaHat.radius);
        dev.dov.hack.echo.client.init.CatalogSettings.append(Module, ChinaHat.height);
        dev.dov.hack.echo.client.init.CatalogSettings.append(Module, ChinaHat.outline);
    }

    
    public static void assignBootstrapSettings(ChinaHat Module, dev.dov.hack.echo.client.settings.FloatSetting preparedHeight, dev.dov.hack.echo.client.settings.ChoiceSetting preparedMode, dev.dov.hack.echo.client.settings.FloatSetting preparedSpinSpeed, dev.dov.hack.echo.client.settings.ColorSetting preparedColor, dev.dov.hack.echo.client.settings.BooleanSetting preparedOutline, dev.dov.hack.echo.client.settings.FloatSetting preparedRadius, dev.dov.hack.echo.client.settings.FloatSetting preparedOpacity) {
        ChinaHat.height = preparedHeight;
        ChinaHat.mode = preparedMode;
        ChinaHat.spinSpeed = preparedSpinSpeed;
        ChinaHat.color = preparedColor;
        ChinaHat.outline = preparedOutline;
        ChinaHat.radius = preparedRadius;
        ChinaHat.opacity = preparedOpacity;
    }
}
