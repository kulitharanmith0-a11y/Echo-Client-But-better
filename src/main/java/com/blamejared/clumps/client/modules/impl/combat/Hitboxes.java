package com.blamejared.clumps.client.modules.impl.combat;

import com.blamejared.clumps.client.init.CatalogSettings;
import com.blamejared.clumps.client.init.CatalogText;
import com.blamejared.clumps.client.event.Subscribe;
import com.blamejared.clumps.client.event.world.EntityHitboxEvent;
import com.blamejared.clumps.client.modules.ModuleCategory;
import com.blamejared.clumps.client.modules.Module;
import com.blamejared.clumps.client.modules.ModuleInfo;
import com.blamejared.clumps.client.settings.BooleanSetting;
import com.blamejared.clumps.client.settings.EntityTypeListSetting;
import com.blamejared.clumps.client.settings.FloatSetting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;

public class Hitboxes extends Module {
    private static final double STANDING_PLAYER_HEIGHT = 1.8;
    public static BooleanSetting uniform;
    public static FloatSetting expandX;
    public static FloatSetting expandYUp;
    public static FloatSetting expandYDown;
    public static FloatSetting expandZ;
    public static FloatSetting expandSize;
    public static BooleanSetting staticHitbox;
    public static EntityTypeListSetting entityTypes = new EntityTypeListSetting(CatalogText.literal("Targets"));
    private static String expansionSection;
    private static String staticHitboxSection;

    public Hitboxes() {
        this(new ModuleInfo(CatalogText.literal("Hitboxes"), CatalogText.literal("Modifies hitbox sizes"), ModuleCategory.COMBAT, false));
    }

    public Hitboxes(ModuleInfo info) {
        super(info);
    }

    @Override
    public void postInitialize() {
        expandX.setVisibilityPredicate(ignored -> !uniform.getValue());
        expandYUp.setVisibilityPredicate(ignored -> !uniform.getValue());
        expandYDown.setVisibilityPredicate(ignored -> !uniform.getValue());
        expandZ.setVisibilityPredicate(ignored -> !uniform.getValue());
        expandSize.setVisibilityPredicate(ignored -> uniform.getValue());
        expandX.setAllowOutOfRange(true);
        expandYUp.setAllowOutOfRange(true);
        expandYDown.setAllowOutOfRange(true);
        expandZ.setAllowOutOfRange(true);
        if (expansionSection == null) expansionSection = "Appearance";
        expandSize.tooltip(expansionSection);
        if (staticHitboxSection == null) staticHitboxSection = "Appearance";
        staticHitbox.tooltip(staticHitboxSection);
    }

    @Subscribe
    private void onHitbox(EntityHitboxEvent event) {
        Entity entity = event.getEntity();
        if (!entityTypes.accepts(entity)) {
            return;
        }
        Box box = baseBoundingBox(entity).expand((double) entity.getTargetingMargin());
        if (uniform.getValue()) {
            float expansion = expandSize.getValue();
            event.setBoundingBox(box.expand((double) expansion, (double) expansion, (double) expansion));
            return;
        }
        float x = expandX.getValue();
        float up = expandYUp.getValue();
        float down = expandYDown.getValue();
        float z = expandZ.getValue();
        event.setBoundingBox(new Box(box.minX - (double) x, box.minY - (double) down, box.minZ - (double) z, box.maxX + (double) x, box.maxY + (double) up, box.maxZ + (double) z));
    }

    private static Box baseBoundingBox(Entity entity) {
        Box box = entity.getBoundingBox();
        if (staticHitbox.getValue() && entity instanceof PlayerEntity player && player.isGliding()) {
            box = new Box(box.minX, box.minY, box.minZ, box.maxX, box.minY + STANDING_PLAYER_HEIGHT, box.maxZ);
        }
        return box;
    }

    public static void appendBootstrapSettings(Hitboxes Module) {
        CatalogSettings.append(Module, uniform);
        CatalogSettings.append(Module, expandX);
        CatalogSettings.append(Module, expandYUp);
        CatalogSettings.append(Module, expandYDown);
        CatalogSettings.append(Module, expandZ);
        CatalogSettings.append(Module, expandSize);
        CatalogSettings.append(Module, staticHitbox);
        CatalogSettings.append(Module, entityTypes);
    }

    public static void assignBootstrapSettings(Hitboxes Module, BooleanSetting preparedStaticHitbox, FloatSetting preparedExpandX, BooleanSetting preparedUniform, FloatSetting preparedExpandYUp, FloatSetting preparedExpandZ, FloatSetting preparedExpandSize, FloatSetting preparedExpandYDown) {
        staticHitbox = preparedStaticHitbox;
        expandX = preparedExpandX;
        uniform = preparedUniform;
        expandYUp = preparedExpandYUp;
        expandZ = preparedExpandZ;
        expandSize = preparedExpandSize;
        expandYDown = preparedExpandYDown;
    }
}
