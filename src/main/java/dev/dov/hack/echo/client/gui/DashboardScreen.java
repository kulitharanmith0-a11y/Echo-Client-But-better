package dev.dov.hack.echo.client.gui;

import dev.dov.hack.echo.client.EchoClient;
import dev.dov.hack.echo.client.event.EventBus;
import dev.dov.hack.echo.client.gui.dashboard.DashboardController;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;

public final class DashboardScreen extends ClientScreen {
    
    private static boolean sharedControlState;

    static {
        setSharedControlState(true);
    }

    private static final String SCREEN_TITLE = "Echo";
    private static final String AUTOSAVE_PROFILE = "_autosave";

    
    public static void setSharedControlState(boolean state) {
        sharedControlState = state;
    }

    
    public static boolean getSharedControlState() {
        return sharedControlState;
    }

    
    public static boolean isSharedControlStateClear() {
        boolean state = getSharedControlState();
        if (!state) return true;
        return false;
    }

    
    public DashboardScreen() {
        super(Text.literal(SCREEN_TITLE));
    }

    
    @Override
    protected void init() {
        super.init();
        EventBus.register(this);
        DashboardController.INSTANCE.onScreenOpened();
    }

    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float tickDelta) {
        DashboardController.INSTANCE.mouseMoved(toUiX(mouseX), toUiY(mouseY));
    }

    
    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        return DashboardController.INSTANCE.mouseClicked(
                toUiX((float) click.x()), toUiY((float) click.y()), click.button())
                || super.mouseClicked(click, doubled);
    }

    
    @Override
    public boolean mouseDragged(Click click, double deltaX, double deltaY) {
        return DashboardController.INSTANCE.mouseDragged(
                toUiX((float) click.x()), toUiY((float) click.y()), click.button())
                || super.mouseDragged(click, deltaX, deltaY);
    }

    
    @Override
    public boolean mouseReleased(Click click) {
        return DashboardController.INSTANCE.mouseReleased(
                toUiX((float) click.x()), toUiY((float) click.y()), click.button())
                || super.mouseReleased(click);
    }

    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        return DashboardController.INSTANCE.mouseScrolled(
                toUiX((float) mouseX), toUiY((float) mouseY), vertical)
                || super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }

    
    @Override
    public boolean keyPressed(KeyInput input) {
        return DashboardController.INSTANCE.keyPressed(input.key(), input.scancode(), input.modifiers())
                || super.keyPressed(input);
    }

    
    @Override
    public boolean charTyped(CharInput input) {
        return DashboardController.INSTANCE.charTyped(input.codepoint()) || super.charTyped(input);
    }

    
    @Override
    public void removed() {
        super.removed();
        EventBus.unregister(this);
        if (EchoClient.ModuleConfig != null) {
            EchoClient.ModuleConfig.saveAsync(AUTOSAVE_PROFILE);
        }
    }

    
    @Override
    public boolean shouldPause() {
        return false;
    }
}
