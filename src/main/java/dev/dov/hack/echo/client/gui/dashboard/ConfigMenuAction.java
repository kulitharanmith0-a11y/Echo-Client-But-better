package dev.dov.hack.echo.client.gui.dashboard;

public enum ConfigMenuAction {
    EDIT("Edit details", false), ADD_COPY("Add another copy", false), PUBLISH("Publish", false), UPDATE_PUBLIC("Update public", false), GET_UPDATE("Get update", false), UNPUBLISH("Unpublish", false), DELETE("Delete", true);

    private static final String CONFIRM_UNPUBLISH = "Click to unpublish";
    private static final String CONFIRM_REPLACE = "Click to replace";
    private static final String CONFIRM_PUBLISHED_DELETE = "Delete and unpublish";
    private static final String CONFIRM_DELETE = "Click to delete";

    public final String label;
    public final boolean destructive;

    ConfigMenuAction(String label, boolean destructive) {
        this.label = label;
        this.destructive = destructive;
    }

    public String confirmationLabel(ConfigCard card) {
        if (this == UNPUBLISH) return CONFIRM_UNPUBLISH;
        if (this == GET_UPDATE) return CONFIRM_REPLACE;
        return card.isPublished() ? CONFIRM_PUBLISHED_DELETE : CONFIRM_DELETE;
    }
}
