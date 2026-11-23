package com.blamejared.clumps.client.modules.impl.render;

import com.blamejared.clumps.client.ClumpsClient;
import com.blamejared.clumps.client.init.CatalogSettings;
import com.blamejared.clumps.client.init.CatalogText;
import com.blamejared.clumps.client.event.EventPriority;
import com.blamejared.clumps.client.event.Subscribe;
import com.blamejared.clumps.client.event.render.HudRenderEvent;
import com.blamejared.clumps.client.event.render.WorldRenderEvent;
import com.blamejared.clumps.client.modules.ModuleCategory;
import com.blamejared.clumps.client.modules.Module;
import com.blamejared.clumps.client.modules.ModuleInfo;
import com.blamejared.clumps.client.hud.ItemHudLayout;
import com.blamejared.clumps.client.render.bridge.MinecraftRenderState;
import com.blamejared.clumps.client.render.world.OverlayWorldGeometry;
import com.blamejared.clumps.client.render.world.ProjectedBounds;
import com.blamejared.clumps.client.render.ui.UiRenderer;
import com.blamejared.clumps.client.render.world.WorldProjection;
import com.blamejared.clumps.client.settings.BooleanSetting;
import com.blamejared.clumps.client.settings.ColorSetting;
import com.blamejared.clumps.client.settings.CustomWidgetSetting;
import com.blamejared.clumps.client.settings.ItemListSetting;
import com.blamejared.clumps.client.settings.MultiChoiceSetting;
import com.blamejared.clumps.client.ui.UiBounds;
import com.blamejared.clumps.client.render.font.FontAsset;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import net.minecraft.client.render.Frustum;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public class ItemESP extends Module {
    private static final CharSequence BOX_MODE = CatalogText.literal("Box");
    private static final CharSequence TWO_DIMENSIONAL_MODE = CatalogText.literal("2D");
    private static MultiChoiceSetting modes;
    private static ItemListSetting whitelist;
    private static BooleanSetting showName;
    private static BooleanSetting fill;
    private static BooleanSetting tracers;
    private static ColorSetting color;
    private static CustomWidgetSetting itemEspWidget = new CustomWidgetSetting(
            CatalogText.literal("Preview"), 240.0f, ItemESP::renderPreview, () -> getPreviewItems().size());
    private static final float NAME_SIZE = 7.0f;
    private static final int PREVIEW_LIMIT = 12;
    private static List<ItemStack> previewItems = List.of();
    private static int previewSelectionHash = Integer.MIN_VALUE;
    private Queue<Runnable> pendingHudDraws;
    private static String fillTooltip;
    private static String appearanceTooltip;
    private static String previewTooltip;
    private static String emptyItemName;
    private static String countSeparator;

    public ItemESP() {
        super(new ModuleInfo(CatalogText.literal("Item ESP"), CatalogText.literal("Highlights dropped items"), ModuleCategory.RENDER));
    }

    @Override
    public void postInitialize() {
        pendingHudDraws = new ArrayDeque<>();
        super.postInitialize();
        fill.setVisibilityPredicate(ignored -> modes.isSelected(TWO_DIMENSIONAL_MODE));
        if (fillTooltip == null) fillTooltip = "Appearance";
        fill.tooltip(fillTooltip);
        if (appearanceTooltip == null) appearanceTooltip = "Appearance";
        tracers.tooltip(appearanceTooltip);
        color.tooltip(appearanceTooltip);
        if (previewTooltip == null) previewTooltip = "Preview";
        itemEspWidget.tooltip(previewTooltip);
    }

    
    @Subscribe
    public void onWorldRender(WorldRenderEvent event) {
        Freecam.isPerspectiveRefreshBypassed();
        if (client.world == null) return;
        OverlayWorldGeometry geometry = event.getRenderContext().getOverlayGeometry();
        UiRenderer renderer = ClumpsClient.uiRenderer;
        float tickProgress = event.getTickProgress();
        Vec3d camera = MinecraftRenderState.getCamera(client).getCameraPos();
        Frustum frustum = new Frustum(event.getViewMatrix(), event.getProjectionMatrix());
        frustum.setPosition(camera.x, camera.y, camera.z);
        boolean boxes = modes.isSelected(BOX_MODE);
        boolean twoDimensional = modes.isSelected(TWO_DIMENSIONAL_MODE);
        boolean names = showName.getValue();
        boolean filterItems = whitelist.getSelectedCount() > 0;
        int fillColor = color.getArgb();
        int outlineColor = fillColor | 0xFF000000;
        int borderColor = 0xFF000000;
        float borderWidth = 0.5f;

        for (Entity entity : client.world.getEntities()) {
            if (!(entity instanceof ItemEntity item)) continue;
            ItemStack stack = item.getStack();
            if (stack.isEmpty()) continue;
            if (filterItems && !whitelist.isEntrySelected(stack.getItem())) continue;
            Vec3d position = item.getLerpedPos(tickProgress);
            Box bounds = item.getBoundingBox();
            double width = bounds.getLengthX();
            double height = bounds.getLengthY();
            double depth = bounds.getLengthZ();
            double x = position.x - width / 2.0;
            double y = position.y;
            double z = position.z - depth / 2.0;
            if (boxes) {
                geometry.drawBox(x, y, z, width, height, depth, fillColor);
                WorldProjection.drawBoxOutline(geometry, new Box(x, y, z, x + width, y + height, z + depth), outlineColor);
            }
            if (tracers.getValue()) {
                geometry.drawTracer(position.x, position.y + height / 2.0, position.z, outlineColor);
            }
            if (!twoDimensional && !names) continue;
            Box interpolatedBounds = new Box(x, y, z, x + width, y + height, z + depth);
            if (!frustum.isVisible(interpolatedBounds)) continue;
            ProjectedBounds projected = WorldProjection.projectBox(event, interpolatedBounds);
            if (!projected.visible) continue;
            float screenX = UiRenderer.snapPixel((float) projected.minX);
            float screenY = UiRenderer.snapPixel((float) projected.minY);
            float screenWidth = UiRenderer.snapPixel((float) projected.maxX) - screenX;
            float screenHeight = UiRenderer.snapPixel((float) projected.maxY) - screenY;
            if (twoDimensional) {
                if (fill.getValue()) {
                    pendingHudDraws.add(() -> renderer.drawRoundedRect(
                            screenX, screenY, screenWidth, screenHeight, 0.0f, fillColor));
                }
                pendingHudDraws.add(() -> drawOutlinedRectangle(renderer, screenX, borderWidth, screenY, screenWidth, screenHeight, borderColor, outlineColor));
            }
            if (names) {
                String name = itemName(stack);
                float textWidth = FontAsset.INTER.measureText(name, NAME_SIZE);
                float nameX = screenX + screenWidth / 2.0f - textWidth / 2.0f;
                float nameY = screenY + screenHeight + 2.0f;
                pendingHudDraws.add(() -> renderer.drawTextWithShadow(
                        FontAsset.INTER, name, nameX, nameY, NAME_SIZE, -1));
            }
        }
    }

    
    @Subscribe(priority = EventPriority.HIGHEST)
    public void onHudRender(HudRenderEvent event) {
        boolean drainAll = Freecam.isPerspectiveRefreshEnabled();
        while (!pendingHudDraws.isEmpty()) {
            pendingHudDraws.poll().run();
            if (!drainAll) break;
        }
    }

    private static void drawOutlinedRectangle(UiRenderer renderer, float x, float thickness, float y, float width, float height, int borderColor, int outlineColor) {
        WorldProjection.drawRectangleBorder(renderer, x - thickness, y - thickness, width + thickness * 2.0f, height + thickness * 2.0f, thickness * 3.0f, borderColor);
        WorldProjection.drawRectangleBorder(renderer, x, y, width, height, thickness, outlineColor);
    }

    
    private static void renderPreview(ItemHudLayout layout) {
        Freecam.isPerspectiveRefreshBypassed();
        List<ItemStack> items = getPreviewItems();
        ItemStack stack = items.isEmpty() ? ItemStack.EMPTY : items.get(layout.getIndex() % items.size());
        float size = Math.min(layout.getWidth(), layout.getHeight()) * 0.34f;
        float centerX = layout.getWidth() * 0.5f;
        float centerY = layout.getHeight() * 0.44f;
        UiBounds itemBounds = new UiBounds(centerX - size * 0.5f, centerY - size * 0.5f, size, size);
        UiBounds highlightBounds = itemBounds.expand(size * 0.06f);
        int fillColor = color.getArgb();
        if (tracers.getValue()) {
            layout.drawLine(layout.getWidth() * 0.12f, layout.getHeight(), highlightBounds.centerX(), highlightBounds.centerY(), layout.getLineWidth(), fillColor | 0xFF000000);
        }
        layout.drawShadow(new UiBounds(centerX - size * 0.32f, centerY + size * 0.46f, size * 0.64f, size * 0.1f), size * 0.05f, size * 0.12f, 0x8C000000);
        layout.drawItem(stack, itemBounds);
        layout.drawItemHighlight(highlightBounds, fillColor, modes.isSelected(BOX_MODE), modes.isSelected(TWO_DIMENSIONAL_MODE), fill.getValue());
        if (showName.getValue()) {
            layout.drawCenteredText(FontAsset.INTER, itemName(stack), highlightBounds.centerX(), highlightBounds.bottom() + 8.0f, 12.0f, -1);
        }
    }

    private static String itemName(ItemStack stack) {
        boolean checkEmpty = Freecam.isPerspectiveRefreshEnabled();
        if (checkEmpty && stack.isEmpty()) {
            if (emptyItemName == null) emptyItemName = "No items selected";
            return emptyItemName;
        }
        String name = stack.getName().getString();
        if (stack.getCount() > 1) {
            if (countSeparator == null) countSeparator = " x";
            return name + countSeparator + stack.getCount();
        }
        return name;
    }

    
    private static List<ItemStack> getPreviewItems() {
        Freecam.isPerspectiveRefreshBypassed();
        Set<Identifier> selected = whitelist.getSelectedIds();
        if (selected.hashCode() == previewSelectionHash && !previewItems.isEmpty()) return previewItems;
        previewSelectionHash = selected.hashCode();
        if (selected.isEmpty()) {
            previewItems = List.of(new ItemStack(Items.DIAMOND_SWORD), new ItemStack(Items.GOLDEN_APPLE, 3), new ItemStack(Items.NETHERITE_INGOT), new ItemStack(Items.ENDER_PEARL, 16), new ItemStack(Items.TOTEM_OF_UNDYING));
        } else {
            Registry<Item> registry = whitelist.getRegistry();
            Objects.requireNonNull(registry);
            previewItems = selected.stream().map(registry::get).filter(Objects::nonNull)
                    .limit(PREVIEW_LIMIT).map(ItemStack::new).toList();
        }
        return previewItems;
    }

    public static void appendBootstrapSettings(ItemESP Module) {
        CatalogSettings.append(Module, modes);
        CatalogSettings.append(Module, whitelist);
        CatalogSettings.append(Module, showName);
        CatalogSettings.append(Module, fill);
        CatalogSettings.append(Module, tracers);
        CatalogSettings.append(Module, color);
        CatalogSettings.append(Module, itemEspWidget);
    }

    public static void assignBootstrapSettings(ItemESP Module, ItemListSetting preparedWhitelist, BooleanSetting preparedFill, MultiChoiceSetting preparedModes, BooleanSetting preparedShowName, BooleanSetting preparedTracers, ColorSetting preparedColor) {
        whitelist = preparedWhitelist;
        fill = preparedFill;
        modes = preparedModes;
        showName = preparedShowName;
        tracers = preparedTracers;
        color = preparedColor;
    }
}
