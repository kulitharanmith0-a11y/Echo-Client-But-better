package dev.dov.hack.echo.client.hud;

import dev.dov.hack.echo.client.render.svg.SvgColorMode;

public enum HudIcon {
    ARROW_UP("gui/client/hud/arrow-up.svg"), CAMERA("gui/client/hud/camera.svg"), DANGER("gui/client/hud/danger.svg"), EYE("gui/client/hud/eye.svg"), EYE_SLASH("gui/client/hud/eye-slash.svg"), FLASH("gui/client/hud/flash.svg"), FLASH_SLASH("gui/client/hud/flash-slash.svg"), GLOBAL("gui/client/hud/global.svg"), HEART("gui/client/hud/heart.svg"), KEYBOARD("gui/client/hud/keyboard.svg"), PLANET("gui/client/svg/planet.svg"), PROFILE("gui/client/hud/profile.svg"), REFRESH_CIRCLE("gui/client/hud/refresh-circle.svg"), SCAN("gui/client/hud/scan.svg"), SHIELD_SECURITY("gui/client/hud/shield-security.svg"), SHIELD_TICK("gui/client/hud/shield-tick.svg"), STAR("gui/client/hud/star.svg"), TAG("gui/client/hud/tag.svg"), LOGO("gui/client/svg/logo.svg", SvgColorMode.PRESERVE);

    public final String path;
    public final SvgColorMode mode;

    HudIcon(String path) {
        this(path, SvgColorMode.ALPHA_MASK);
    }

    HudIcon(String path, SvgColorMode mode) {
        this.path = path;
        this.mode = mode;
    }
}
