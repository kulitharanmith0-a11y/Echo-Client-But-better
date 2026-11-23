package com.blamejared.clumps.client.gui.dashboard;

import java.util.ArrayList;
import java.util.List;

public final class ConfigCardRules {
    
    public interface PublishingAccess {
        boolean canPublish();
    }

    private final List<ConfigCard> cards;
    private final PublishingAccess publishing;

    
    public ConfigCardRules(List<ConfigCard> cards, PublishingAccess publishing) {
        this.cards = cards;
        this.publishing = publishing;
    }

    
    public ConfigCard find(String id) {
        if (id == null || id.isBlank()) return null;
        for (ConfigCard card : cards) {
            if (card.id.equals(id)) return card;
        }
        return null;
    }

    
    public ConfigCard firstCopy(ConfigCard source) {
        for (ConfigCard card : cards) {
            if (!card.publicVisibility && card.sourceId.equals(source.id)) return card;
        }
        return null;
    }

    
    public int copyCount(ConfigCard source) {
        int count = 0;
        for (ConfigCard card : cards) {
            if (!card.publicVisibility && card.sourceId.equals(source.id)) count++;
        }
        return count;
    }

    
    public ConfigPrimaryAction primaryAction(ConfigCard card) {
        if (!card.publicVisibility) return ConfigPrimaryAction.LOAD;
        if (card.owned) return ConfigPrimaryAction.LOAD_OWN;
        int copies = copyCount(card);
        if (copies > 1) return ConfigPrimaryAction.SHOW_COPIES;
        return copies == 1 ? ConfigPrimaryAction.LOAD_COPY : ConfigPrimaryAction.ADD;
    }

    
    public boolean hasLocalLoadTarget(ConfigCard card) {
        if (!card.publicVisibility) return true;
        if (card.owned) return false;
        return firstCopy(card) != null;
    }

    
    public boolean localNameExists(String name) {
        for (ConfigCard card : cards) {
            if (!card.publicVisibility && card.name.equalsIgnoreCase(name)) return true;
        }
        return false;
    }

    
    public void select(String activeId) {
        for (ConfigCard card : cards) {
            card.selected = card.id.equals(activeId);
        }
    }

    
    public List<ConfigMenuAction> menuActions(ConfigCard card) {
        List<ConfigMenuAction> actions = new ArrayList<>();
        if (card.publicVisibility) {
            actions.add(ConfigMenuAction.ADD_COPY);
            return actions;
        }
        actions.add(ConfigMenuAction.EDIT);
        if (publishing.canPublish()) {
            actions.add(card.isPublished() ? ConfigMenuAction.UPDATE_PUBLIC : ConfigMenuAction.PUBLISH);
        }
        if (card.selected && card.updateAvailable) actions.add(ConfigMenuAction.GET_UPDATE);
        if (card.isPublished()) actions.add(ConfigMenuAction.UNPUBLISH);
        actions.add(ConfigMenuAction.DELETE);
        return actions;
    }
}
