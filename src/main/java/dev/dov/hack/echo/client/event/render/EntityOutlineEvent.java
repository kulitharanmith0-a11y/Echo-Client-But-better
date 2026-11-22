package dev.dov.hack.echo.client.event.render;

import dev.dov.hack.echo.client.event.Event;
import net.minecraft.entity.Entity;

public class EntityOutlineEvent extends Event {
    private final Entity entity;
    private boolean outlined;

    public EntityOutlineEvent(Entity entity, boolean outlined) {
        this.entity = entity;
        this.outlined = outlined;
    }

    public Entity getEntity() {
        return entity;
    }

    public boolean isOutlined() {
        return outlined;
    }

    public void setOutlined(boolean outlined) {
        this.outlined = outlined;
    }
}
