package dev.dov.hack.echo.client.modules.internals;

import static dev.dov.hack.echo.client.init.CatalogText.literal;

import dev.dov.hack.echo.client.EchoClient;
import dev.dov.hack.echo.client.init.CatalogSettings;
import dev.dov.hack.echo.client.config.NameRegistry;
import dev.dov.hack.echo.client.event.EventBus;
import dev.dov.hack.echo.client.event.EventPriority;
import dev.dov.hack.echo.client.event.Subscribe;
import dev.dov.hack.echo.client.event.input.AttackKeyEvent;
import dev.dov.hack.echo.client.event.lifecycle.ClientTickEvent;
import dev.dov.hack.echo.client.modules.ModuleCategory;
import dev.dov.hack.echo.client.modules.Module;
import dev.dov.hack.echo.client.modules.ModuleInfo;
import dev.dov.hack.echo.client.handlers.AttackCooldownTracker;
import dev.dov.hack.echo.client.render.bridge.MinecraftRenderState;
import dev.dov.hack.echo.client.settings.BooleanSetting;
import dev.dov.hack.echo.client.settings.ChoiceSetting;
import dev.dov.hack.echo.client.settings.FloatSetting;
import dev.dov.hack.echo.client.settings.IntSetting;
import dev.dov.hack.echo.client.settings.KeybindSetting;
import dev.dov.hack.echo.client.settings.MultiChoiceSetting;
import dev.dov.hack.echo.client.utils.interaction.EntityInteractions;
import dev.dov.hack.echo.client.utils.items.InventoryItems;
import dev.dov.hack.echo.client.utils.strings.Concat;
import dev.dov.hack.echo.client.utils.targeting.EntityPredicates;
import dev.dov.hack.echo.client.utils.targeting.TargetSelection;
import java.util.Optional;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.scoreboard.Team;
import net.minecraft.text.Style;
import net.minecraft.text.TextColor;
import net.minecraft.util.hit.EntityHitResult;

public class Targets extends Module {
    private static final CharSequence SCOREBOARD = literal("Scoreboard");
    private static final CharSequence COLOR = literal("Color");
    public static final CharSequence NAME = literal("Name");
    public static final CharSequence PLAYERS = literal("Players");
    public static final CharSequence HOSTILE = literal("Hostile");
    public static final CharSequence PASSIVE = literal("Passive");
    public static final CharSequence NEUTRAL = literal("Neutral");
    public static final CharSequence ARMOR_STANDS = literal("Armor Stands");
    public static final CharSequence INVISIBLE = literal("Invisible");
    public static final CharSequence NAKED = literal("Naked");
    public static final CharSequence MOVEMENT = literal("Movement");
    public static final CharSequence TAB_LIST = literal("Tab List");
    public static final CharSequence SCALE = literal("Scale");
    public static final CharSequence BLISS = literal("Bliss");
    public static final CharSequence ZETA_MMC = literal("Zeta / MMC");
    private static final CharSequence SHIELD_TICK = literal("Tick");
    private static final CharSequence SHIELD_VANILLA = literal("Vanilla");
    public static MultiChoiceSetting entityTypes;
    public static BooleanSetting ignoreTeams;
    public static MultiChoiceSetting teamChecks;
    public static BooleanSetting ignoreFriends;
    public static BooleanSetting antiBot;
    public static MultiChoiceSetting antiBotChecks;
    public static BooleanSetting keepTarget;
    public static BooleanSetting lastAttacked;
    public static FloatSetting keepTargetRange;
    public static KeybindSetting manualTargetSelect;
    public static KeybindSetting friendBind;
    public static ChoiceSetting shieldDetection;
    public static IntSetting shieldTicks;
    public static BooleanSetting pingCompensation;

    public static LivingEntity currentTarget = null;
    public static boolean shieldBreakActive = false;
    private boolean manualSelectWasPressed;
    private boolean friendBindWasPressed;

    
    public Targets() {
        this(new ModuleInfo(literal("Targets"), literal("Global target settings for combat modules"), ModuleCategory.INTERNALS));
    }

    
    public Targets(ModuleInfo info) {
        super(info);
    }

    
    public static void assignBootstrapSettings(Targets Module, BooleanSetting preparedAntiBot, FloatSetting preparedKeepTargetRange, BooleanSetting preparedIgnoreFriends, BooleanSetting preparedLastAttacked, IntSetting preparedShieldTicks, MultiChoiceSetting preparedAntiBotChecks, KeybindSetting preparedManualTargetSelect, ChoiceSetting preparedShieldDetection, BooleanSetting preparedIgnoreTeams, MultiChoiceSetting preparedEntityTypes, MultiChoiceSetting preparedTeamChecks, BooleanSetting preparedKeepTarget, KeybindSetting preparedFriendBind, BooleanSetting preparedPingCompensation) {
        antiBot = preparedAntiBot;
        keepTargetRange = preparedKeepTargetRange;
        ignoreFriends = preparedIgnoreFriends;
        lastAttacked = preparedLastAttacked;
        shieldTicks = preparedShieldTicks;
        antiBotChecks = preparedAntiBotChecks;
        manualTargetSelect = preparedManualTargetSelect;
        shieldDetection = preparedShieldDetection;
        ignoreTeams = preparedIgnoreTeams;
        entityTypes = preparedEntityTypes;
        teamChecks = preparedTeamChecks;
        keepTarget = preparedKeepTarget;
        friendBind = preparedFriendBind;
        pingCompensation = preparedPingCompensation;
    }

    
    public static void appendBootstrapSettings(Targets Module) {
        CatalogSettings.append(Module, entityTypes);
        CatalogSettings.append(Module, ignoreTeams);
        CatalogSettings.append(Module, teamChecks);
        CatalogSettings.append(Module, ignoreFriends);
        CatalogSettings.append(Module, antiBot);
        CatalogSettings.append(Module, antiBotChecks);
        CatalogSettings.append(Module, keepTarget);
        CatalogSettings.append(Module, lastAttacked);
        CatalogSettings.append(Module, keepTargetRange);
        CatalogSettings.append(Module, manualTargetSelect);
        CatalogSettings.append(Module, friendBind);
        CatalogSettings.append(Module, shieldDetection);
        CatalogSettings.append(Module, shieldTicks);
        CatalogSettings.append(Module, pingCompensation);
    }

    
    public static CharSequence bootstrapShieldDetectionOption() {
        return SHIELD_TICK;
    }

    
    public static CharSequence bootstrapShieldDetectionOption2() {
        return SHIELD_VANILLA;
    }

    @Override
    public void postInitialize() {
        antiBotChecks.setVisibilityPredicate(ignored -> antiBot.getValue());
        antiBotChecks.setSelected(literal("Name"), true);
        antiBotChecks.setSelected(literal("Movement"), true);
        antiBotChecks.setSelected(literal("Tab List"), true);
        antiBotChecks.setSelected(literal("Scale"), true);
        antiBotChecks.setSelected(literal("Bliss"), false);
        antiBotChecks.setSelected(ZETA_MMC, false);
        teamChecks.setVisibilityPredicate(ignored -> ignoreTeams.getValue());
        teamChecks.setSelected(literal("Color"), true);
        teamChecks.setSelected(literal("Name"), false);
        teamChecks.setSelected(literal("Scoreboard"), true);
        lastAttacked.setVisibilityPredicate(ignored -> keepTarget.getValue());
        shieldTicks.setVisibilityPredicate(ignored -> !usesVanillaShieldDetection());
        pingCompensation.setVisibilityPredicate(ignored -> !usesVanillaShieldDetection());
        keepTarget.onChange(Targets::onKeepTargetChanged);
        EventBus.register(this);
        entityTypes.tooltip(literal("Entity Types"));
        ignoreTeams.tooltip(literal("Teams"));
        teamChecks.tooltip(literal("Teams"));
        ignoreFriends.tooltip(literal("Friends"));
        antiBot.tooltip(literal("AntiBot"));
        antiBotChecks.tooltip(literal("AntiBot"));
        keepTarget.tooltip(literal("Keep Target"));
        lastAttacked.tooltip(literal("Keep Target"));
        keepTargetRange.tooltip(literal("Keep Target"));
        manualTargetSelect.tooltip(literal("Keep Target"));
        friendBind.tooltip(literal("Friends"));
        shieldDetection.tooltip(literal("Shield"));
        shieldTicks.tooltip(literal("Shield"));
        pingCompensation.tooltip(literal("Shield"));
    }

    
    @Override
    public void toggle() {
    }

    
    @Override
    public void setEnabled(boolean requested) {
    }

    
    @Override
    public boolean isEnabled() {
        return false;
    }

    @Override
    public boolean isKeybindable() {
        return false;
    }

    
    public static boolean usesVanillaShieldDetection() {
        return shieldDetection.isSelected(SHIELD_VANILLA);
    }

    private static void onKeepTargetChanged() {
        if (!keepTarget.getValue()) {
            TargetSelection.clearTargets();
        }
    }

    @Subscribe
    public void onFriendBind(ClientTickEvent event) {
        if (isOutsideWorld() || MinecraftRenderState.getScreen() != null) {
            return;
        }
        boolean pressed = friendBind.getKey().isPressed();
        boolean justPressed = pressed && !friendBindWasPressed;
        friendBindWasPressed = pressed;
        if (!justPressed || EchoClient.friendManager == null) {
            return;
        }
        LivingEntity target = EntityInteractions.getManualSelectTarget();
        if (!(target instanceof PlayerEntity player)) {
            return;
        }
        String name = player.getName().getString();
        if (EchoClient.friendManager.remove(name)) {
            NameRegistry.postNotification(this, Concat.strings("Removed ", name, " from friends"));
            return;
        }
        EchoClient.friendManager.add(name);
        NameRegistry.postNotification(this, Concat.strings("Added ", name, " to friends"));
    }

    @Subscribe(priority = EventPriority.HIGH)
    public void onManualTargetSelect(AttackKeyEvent event) {
        if (isOutsideWorld() || MinecraftRenderState.getScreen() != null) {
            return;
        }
        if (!manualTargetSelect.getKey().isPressed()) {
            manualSelectWasPressed = false;
            return;
        }
        if (manualSelectWasPressed) {
            return;
        }
        manualSelectWasPressed = true;
        if (!keepTarget.getValue()) {
            return;
        }
        LivingEntity target = EntityInteractions.getManualSelectTarget();
        if (target == null || !TargetSelection.isValidTarget(target)) {
            return;
        }
        TargetSelection.selectTarget(target);
    }

    @Subscribe(priority = EventPriority.LOWEST)
    public void onShieldTarget(AttackKeyEvent event) {
        if (isOutsideWorld() || !event.hasClicks()) {
            return;
        }
        if (!(client.crosshairTarget instanceof EntityHitResult hit)
                || !(hit.getEntity() instanceof PlayerEntity player)) {
            return;
        }
        if (!EntityPredicates.isBlocking(player) || EntityPredicates.isOutsideBlockingArc(player)
                || AttackCooldownTracker.hasNonFireHurtWindow(player)
                || !TargetSelection.isValidTarget(player)) {
            return;
        }
        if (!InventoryItems.isAxe(client.player.getMainHandStack())) {
            return;
        }
        currentTarget = player;
    }

    
    public boolean isSameTeam(Entity entity) {
        if (!(entity instanceof PlayerEntity player) || client.player == null || client.world == null) {
            return false;
        }
        if (teamChecks.isSelected(SCOREBOARD)) {
            Team other = client.world.getScoreboard().getTeam(player.getNameForScoreboard());
            Team local = client.world.getScoreboard().getTeam(client.player.getNameForScoreboard());
            if (other != null && other == local) {
                return true;
            }
        }
        if (teamChecks.isSelected(COLOR)) {
            TextColor other = getNameColor(player);
            TextColor local = getNameColor(client.player);
            if (other != null && other.equals(local)) {
                return true;
            }
        }
        if (teamChecks.isSelected(NAME)) {
            String other = bracketedNameTag(player.getDisplayName().getString());
            String local = bracketedNameTag(client.player.getDisplayName().getString());
            if (other != null && other.equals(local)) {
                return true;
            }
        }
        return false;
    }

    
    public static TextColor getNameColor(PlayerEntity player) {
        String name = player.getNameForScoreboard();
        return player.getDisplayName().visit((style, segment) -> {
            if (style.getColor() != null && segment.contains(name)) {
                return Optional.of(style.getColor());
            }
            return Optional.<TextColor>empty();
        }, Style.EMPTY).orElse(null);
    }

    private static String bracketedNameTag(String text) {
        int start = text.indexOf('[');
        if (start == -1) {
            return null;
        }
        int end = text.indexOf(']', start);
        return end == -1 ? null : text.substring(start, end + 1);
    }

}
