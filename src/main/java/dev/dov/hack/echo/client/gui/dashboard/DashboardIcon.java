package dev.dov.hack.echo.client.gui.dashboard;

import dev.dov.hack.echo.client.render.svg.SvgColorMode;

public enum DashboardIcon {
    COMBAT("gui/client/svg/combat_icon.svg"), VISUALS("gui/client/svg/visuals_icon.svg"), UTILITY("gui/client/svg/utility_icon.svg"), WORLD("gui/client/svg/world_icon.svg"), PLANET("gui/client/svg/planet.svg"), MANAGER("gui/client/svg/manager_icon.svg"), LEGIT("gui/client/svg/legit_icon.svg"), CONFIGS("gui/client/svg/configs_icon.svg"), SCRIPTS("gui/client/svg/scripts_icon.svg"), FRIENDS("gui/client/svg/friends_icon.svg"), PROFILE("gui/client/svg/profile_icon.svg"), OVERLAYS("gui/client/hud/layer.svg"), MODIFY_HUD("gui/client/svg/modify_hud_icon.svg"), PLUS("gui/client/svg/plus_icon.svg"), CHEVRON("gui/client/svg/chevron_icon.svg"), DROPDOWN_CHEVRON("gui/client/svg/dropdown_chevron.svg"), DROPDOWN_CHEVRON_UP("gui/client/svg/dropdown_chevron_up.svg"), GHOST("gui/client/svg/ghost_icon.svg"), GHOST_DARK("gui/client/svg/ghost_icon.svg"), DANGER("gui/client/svg/danger_icon.svg"), DANGER_DARK("gui/client/svg/danger_icon.svg"), FILTER("gui/client/svg/filter_icon.svg"), LOGO("gui/client/svg/logo.svg", SvgColorMode.PRESERVE), SIDEBAR_GLOW("gui/client/svg/sidebar_glow_panel.svg", SvgColorMode.PRESERVE), BIND_TOGGLE("gui/client/svg/bind_toggle.svg"), BIND_HOLD("gui/client/svg/bind_hold.svg"), SCRIPT_CARD("gui/client/svg/script_card.svg"), PREVIEW_PREV("gui/client/svg/arrow-short-left.svg"), PREVIEW_NEXT("gui/client/svg/arrow-short-right.svg"), EDITOR_SCRIPT("gui/client/svg/editor_script.svg"), EDITOR_IMPORT("gui/client/svg/editor_import.svg"), EDITOR_RUN("gui/client/svg/editor_run.svg", SvgColorMode.PRESERVE), DELETE("gui/client/svg/delete_icon.svg"), MORE("gui/client/svg/more_icon.svg"), PUBLIC("gui/client/svg/Public.svg"), PRIVATE("gui/client/svg/Private.svg");

    
    public final String path;
    
    public final SvgColorMode colorMode;

    DashboardIcon(String path) {
        this(path, SvgColorMode.ALPHA_MASK);
    }

    DashboardIcon(String path, SvgColorMode colorMode) {
        this.path = path;
        this.colorMode = colorMode;
    }

    
    public DashboardIcon darkVariant() {
        return switch (this) {
            case GHOST -> GHOST_DARK;
            case DANGER -> DANGER_DARK;
            default -> this;
        };
    }
}
