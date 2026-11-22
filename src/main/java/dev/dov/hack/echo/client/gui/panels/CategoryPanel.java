package dev.dov.hack.echo.client.gui.panels;

import dev.dov.hack.echo.client.EchoClient;
import dev.dov.hack.echo.client.MinecraftAccess;
import dev.dov.hack.echo.client.modules.ModuleCategory;
import dev.dov.hack.echo.client.modules.Module;
import dev.dov.hack.echo.client.modules.internals.ClickGui;
import dev.dov.hack.echo.client.keyboard.InputKey;
import dev.dov.hack.echo.client.render.ui.UiRenderer;
import dev.dov.hack.echo.client.gui.CategoryPanelScreen;
import dev.dov.hack.echo.client.ui.ColorTheme;
import dev.dov.hack.echo.client.gui.animations.Easing;
import dev.dov.hack.echo.client.gui.animations.ScrollTween;
import dev.dov.hack.echo.client.gui.animations.Tween;
import dev.dov.hack.echo.client.render.font.FontAsset;
import dev.dov.hack.echo.client.gfx.texture.TextureView;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.List;
import net.minecraft.client.gui.Click;

public class CategoryPanel implements MinecraftAccess {
    private static final float HEADER_ICON_SIZE = 8.0f;
    private static final float ICON_GAP = 4.0f;
    private final ModuleCategory moduleCategory;
    private final List<FeatureCard> cards;
    private float x;
    private float y;
    private final float width;
    private float height;
    private float scroll;
    private float scrollTarget;
    private float maxScroll;
    private static final float SCROLL_DURATION_MILLIS = 320.0f;
    private final Tween scrolling;
    public boolean expanded;
    private FeatureCard expandedCard;
    private int index;
    private final CategoryPanelScreen screen;
    private final ScrollTween position;
    private final Tween expansion;

    public CategoryPanel(CategoryPanelScreen screen, ModuleCategory moduleCategory, float x, float y, int index) {
        cards = new ObjectArrayList<>();
        width = ColorTheme.PANEL_WIDTH;
        scroll = 0.0f;
        scrollTarget = 0.0f;
        maxScroll = 0.0f;
        scrolling = new Tween(Easing.EASE_OUT_BACK, SCROLL_DURATION_MILLIS);
        expanded = true;
        expandedCard = null;
        expansion = new Tween(Easing.EASE_OUT_CUBIC, 400.0);
        this.screen = screen;
        this.moduleCategory = moduleCategory;
        this.x = x;
        this.y = y;
        height = ColorTheme.PANEL_HEADER_HEIGHT;
        position = new ScrollTween(Easing.EASE_OUT_CUBIC, 500.0f);
        position.start(this.x, this.y, screen.panelSlotX(index), y);
        this.index = index;
        for (Module feature : EchoClient.ModuleManager.featuresInCategory(moduleCategory)) {
            cards.add(new FeatureCard(screen, feature));
        }
        updateExpansion();
    }

    public void addFeature(Module feature) {
        cards.add(new FeatureCard(screen, feature));
        updateExpansion();
    }

    public void removeFeature(Module feature) {
        cards.removeIf(card -> card.getFeature() == feature);
        updateExpansion();
    }

    public void animatePosition(float x, float y) {
        position.start(this.x, this.y, x, y);
    }

    public void render(UiRenderer renderer, int mouseX, int mouseY, float opacity, TextureView backdrop) {
        if (!position.isFinished() && screen.draggedPanelIndex != index) {
            x = position.x();
            y = position.y();
        }
        updateExpansion();
        height = (float) expansion.value();
        int background = ColorTheme.multiplyAlpha(ColorTheme.PANEL_FOREGROUND.getRGB(), opacity);
        if (backdrop != null) {
            renderer.drawBackdrop(backdrop, x, y, width, height, ColorTheme.CATEGORY_BODY_RADIUS, opacity);
        }
        renderer.drawRoundedRect(x, y, width, height, ColorTheme.CATEGORY_BODY_RADIUS, background);
        int foreground = ColorTheme.multiplyAlpha(ColorTheme.SELECTED_OPTION_TEXT, opacity);
        renderer.drawText(FontAsset.INTER_SEMI_BOLD, moduleCategory.name, x + ColorTheme.CONTENT_PADDING, y + ColorTheme.PANEL_HEADER_HEIGHT / 2.0f - 5.0f, HEADER_ICON_SIZE, foreground);
        float right = x + ColorTheme.PANEL_WIDTH - ColorTheme.CONTENT_PADDING;
        if (!moduleCategory.icon.isEmpty() && FontAsset.LUCIDE != null) {
            float glyphWidth = FontAsset.LUCIDE.measureText(moduleCategory.icon, HEADER_ICON_SIZE);
            float glyphX = right - glyphWidth;
            renderer.drawText(FontAsset.LUCIDE, moduleCategory.icon, glyphX, y + ColorTheme.PANEL_HEADER_HEIGHT / 2.0f - 4.0f, HEADER_ICON_SIZE, foreground);
            right = glyphX - ICON_GAP;
        }
        if (showLock(mouseX, mouseY) && FontAsset.LUCIDE != null) {
            float glyphWidth = FontAsset.LUCIDE.measureText("\ue1f9", HEADER_ICON_SIZE);
            int lockColor = screen.isLayoutLocked(this)
                    ? ColorTheme.multiplyAlpha(ColorTheme.SELECTED_OPTION_TEXT, opacity)
                    : ColorTheme.multiplyAlpha(ColorTheme.PRIMARY_TEXT, opacity);
            renderer.drawText(FontAsset.LUCIDE, "\ue1f9", right - glyphWidth, y + ColorTheme.PANEL_HEADER_HEIGHT / 2.0f - 4.0f, HEADER_ICON_SIZE, lockColor);
        }
        if (height - ColorTheme.PANEL_HEADER_HEIGHT - 4.0f <= 0.0f) {
            return;
        }
        renderer.pushScissor(x, y + ColorTheme.PANEL_HEADER_HEIGHT, ColorTheme.PANEL_WIDTH, height - ColorTheme.PANEL_HEADER_HEIGHT);
        renderCards(renderer, x, y, mouseX, mouseY, opacity);
        renderer.popScissor();
    }

    private void renderCards(UiRenderer renderer, float x, float y, int mouseX, int mouseY, float opacity) {
        List<FeatureCard> visibleCards = visibleCards();
        float contentHeight = 0.0f;
        for (FeatureCard card : visibleCards) {
            card.layout(x, 0.0f);
            contentHeight += cardHeight(card) + ColorTheme.CATEGORY_ROW_GAP;
        }
        maxScroll = Math.max(0.0f, contentHeight - (height - ColorTheme.PANEL_HEADER_HEIGHT - 4.0f));
        scroll = (float) scrolling.value();
        if (scroll > maxScroll) {
            scroll = maxScroll;
            scrollTarget = maxScroll;
            scrolling.start(maxScroll, maxScroll);
        }
        float cardY = y + ColorTheme.PANEL_HEADER_HEIGHT + 2.0f - scroll;
        boolean locked = screen.isLayoutLocked(this);
        for (FeatureCard card : visibleCards) {
            card.layout(x, cardY);
            float cardHeight = cardHeight(card);
            if (cardY + cardHeight >= y + ColorTheme.PANEL_HEADER_HEIGHT && cardY <= y + height) {
                card.render(renderer, mouseX, mouseY, opacity, locked);
            }
            cardY += cardHeight + ColorTheme.CATEGORY_ROW_GAP;
        }
    }

    public void updateExpansion() {
        float target = ColorTheme.PANEL_HEADER_HEIGHT;
        if (expanded) {
            target += 2.0f;
            for (FeatureCard card : visibleCards()) {
                target += cardHeight(card) + ColorTheme.CATEGORY_ROW_GAP;
                if (target >= ColorTheme.MAX_PANEL_HEIGHT) {
                    target = ColorTheme.MAX_PANEL_HEIGHT;
                    break;
                }
            }
        }
        expansion.setTarget(target);
    }

    public boolean mouseClicked(Click click) {
        if (!expanded) {
            return false;
        }
        boolean locked = screen.isLayoutLocked(this);
        for (FeatureCard card : visibleCards()) {
            float cardHeight = cardHeight(card);
            if (!ColorTheme.contains(card.getX(), card.getY(), card.getWidth(), cardHeight, (float) click.x(), (float) click.y())) {
                continue;
            }
            if (locked) {
                return clickVisibility(card, click);
            }
            if (click.y() <= card.getY() + ColorTheme.FEATURE_ROW_HEIGHT) {
                return clickHeader(card, click);
            }
            return card.mouseClicked(click);
        }
        return false;
    }

    private boolean clickVisibility(FeatureCard card, Click click) {
        if (!InputKey.MOUSE_LEFT.matches(click.button())) {
            return true;
        }
        boolean visible = !card.getFeature().isVisible();
        card.getFeature().setVisible(visible);
        if (!visible && expandedCard == card) {
            expandedCard.expanded = false;
            expandedCard = null;
        }
        updateExpansion();
        return true;
    }

    private boolean clickHeader(FeatureCard card, Click click) {
        if (InputKey.MOUSE_LEFT.matches(click.button())) {
            Module feature = card.getFeature();
            if (feature.getKey() != InputKey.NONE && InputKey.LEFT_SHIFT.isPressed()) {
                feature.setHoldMode(!feature.isHoldMode());
                return true;
            }
            if (card.isKeyHovered((float) click.x())) {
                screen.bindingCard = card;
                return true;
            }
            feature.toggle();
            return true;
        }
        if (InputKey.MOUSE_RIGHT.matches(click.button()) && !card.getFeature().settings.isEmpty()) {
            boolean open = !card.expanded;
            if (open && expandedCard != null && expandedCard != card) {
                expandedCard.expanded = false;
            }
            card.expanded = open;
            expandedCard = open ? card : null;
            updateExpansion();
            return true;
        }
        if (InputKey.MOUSE_MIDDLE.matches(click.button())) {
            screen.bindingCard = card;
            return true;
        }
        return false;
    }

    public void mouseReleased(double mouseX, double mouseY, int button) {
        cards.stream().anyMatch(card -> card.mouseReleased(mouseX, mouseY, button));
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double vertical) {
        if (!screen.isLayoutLocked(this)) {
            float cardY = y + ColorTheme.PANEL_HEADER_HEIGHT + 2.0f - scroll;
            for (FeatureCard card : visibleCards()) {
                float cardHeight = cardHeight(card);
                if (card.expanded && mouseY >= cardY && mouseY <= cardY + cardHeight
                        && card.mouseScrolled(mouseX, mouseY, vertical)) {
                    return true;
                }
                cardY += cardHeight + ColorTheme.CATEGORY_ROW_GAP;
            }
        }
        float speed;
        if (screen.ClickGui != null) {
            ClickGui present = screen.ClickGui;
            speed = ClickGui.scrollSpeed.getValue();
        } else {
            speed = ColorTheme.SCROLL_SPEED;
        }
        float from = (float) scrolling.value();
        float target = (float) Math.clamp((double) scrollTarget - vertical * (double) speed, 0.0, (double) maxScroll);
        if (target == scrollTarget) {
            return false;
        }
        scrollTarget = target;
        scrolling.start(from, scrollTarget);
        return true;
    }

    public void charTyped(char character, int modifiers) {
        cards.stream().anyMatch(card -> card.charTyped(character, modifiers));
    }

    public boolean keyPressed(int key, int scanCode, int modifiers) {
        return cards.stream().anyMatch(card -> card.keyPressed(key, scanCode, modifiers));
    }

    public boolean isLockHovered(double mouseX, double mouseY) {
        if (FontAsset.LUCIDE == null) {
            return false;
        }
        if (!screen.isLayoutLocked(this) && !ColorTheme.contains(x, y, width, ColorTheme.PANEL_HEADER_HEIGHT, (float) mouseX, (float) mouseY)) {
            return false;
        }
        float categoryWidth = moduleCategory.icon.isEmpty() ? 0.0f : FontAsset.LUCIDE.measureText(moduleCategory.icon, HEADER_ICON_SIZE);
        float lockWidth = FontAsset.LUCIDE.measureText("\ue1f9", HEADER_ICON_SIZE);
        float gap = categoryWidth > 0.0f ? ICON_GAP : 0.0f;
        float lockX = x + ColorTheme.PANEL_WIDTH - ColorTheme.CONTENT_PADDING - categoryWidth - gap - lockWidth;
        float lockY = y + ColorTheme.PANEL_HEADER_HEIGHT / 2.0f - 4.0f;
        return ColorTheme.contains(lockX - 2.0f, lockY - 1.0f, lockWidth + 4.0f, 10.0f, (float) mouseX, (float) mouseY);
    }

    private boolean showLock(int mouseX, int mouseY) {
        if (FontAsset.LUCIDE == null) {
            return false;
        }
        if (screen.isLayoutLocked(this)) {
            return true;
        }
        return ColorTheme.contains(x, y, width, height, mouseX, mouseY);
    }

    private List<FeatureCard> visibleCards() {
        if (screen.isLayoutLocked(this)) {
            return cards;
        }
        List<FeatureCard> visible = new ObjectArrayList<>(cards);
        visible.removeIf(card -> !card.getFeature().isVisible());
        return visible;
    }

    private float cardHeight(FeatureCard card) {
        return screen.isLayoutLocked(this) ? ColorTheme.FEATURE_ROW_HEIGHT : card.getHeight();
    }

    public ModuleCategory getCategory() { return moduleCategory; }
    public float getX() { return x; }
    public void setX(float x) { this.x = x; }
    public float getY() { return y; }
    public void setY(float y) { this.y = y; }
    public float getWidth() { return width; }
    public float getHeight() { return height; }
    public void setIndex(int index) { this.index = index; }
    public ScrollTween getPositionTween() { return position; }
}
