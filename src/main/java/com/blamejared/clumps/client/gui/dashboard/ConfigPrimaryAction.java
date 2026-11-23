package com.blamejared.clumps.client.gui.dashboard;

public enum ConfigPrimaryAction {
    LOAD("Load", null, 0.0f), LOAD_OWN("Load", null, 0.0f), LOAD_COPY("Load", null, 0.0f), ADD("Load", null, 0.0f), SHOW_COPIES("Show copies", null, 0.0f), SAVE("Save", null, 72.0f), GET_UPDATE("Get update", "Replace?", 100.0f), UNPUBLISH("Unpublish", "Unpublish?", 100.0f);

    
    public final String label;
    
    public final String confirmationLabel;
    
    public final float secondaryWidth;

    ConfigPrimaryAction(String label, String confirmationLabel, float secondaryWidth) {
        this.label = label;
        this.confirmationLabel = confirmationLabel;
        this.secondaryWidth = secondaryWidth;
    }

    
    public boolean isSave() {
        return this == SAVE;
    }

    
    public boolean isGetUpdate() {
        return this == GET_UPDATE;
    }
}
