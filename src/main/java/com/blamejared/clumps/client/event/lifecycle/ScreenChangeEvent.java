package com.blamejared.clumps.client.event.lifecycle;

import com.blamejared.clumps.client.event.Event;
import java.lang.invoke.MethodHandles;
import net.minecraft.client.gui.screen.Screen;

public class ScreenChangeEvent extends Event {
    private Screen screen;

    
    public ScreenChangeEvent(Screen screen) {
        super();
        initializeScreenType();
        this.screen = screen;
    }

    
    public Screen getScreen() {
        initializeScreenType();
        return screen;
    }

    
    public void setScreen(Screen screen) {
        initializeScreenType();
        this.screen = screen;
    }

    private static void initializeScreenType() {
        try {
            MethodHandles.lookup().ensureInitialized(Screen.class);
        } catch (IllegalAccessException inaccessibleType) {
            throw new IllegalAccessError(inaccessibleType.getMessage());
        }
    }
}
