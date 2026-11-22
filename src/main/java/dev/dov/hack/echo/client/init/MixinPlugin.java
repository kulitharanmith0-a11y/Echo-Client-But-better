package dev.dov.hack.echo.client.init;

import java.io.InputStream;
import java.util.List;
import java.util.Set;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.service.MixinService;

public final class MixinPlugin implements IMixinConfigPlugin {
    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return targetClassName.startsWith("net.minecraft.")
                || targetClassName.startsWith("com.mojang.")
                || isTargetAvailable(targetClassName);
    }

    private static boolean isTargetAvailable(String className) {
        try (InputStream stream = MixinService.getService()
                .getResourceAsStream(className.replace('.', '/') + ".class")) {
            return stream != null;
        } catch (Throwable unavailable) {
            return false;
        }
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
