package com.blamejared.clumps.client.settings;

import com.blamejared.clumps.client.MinecraftAccess;
import com.blamejared.clumps.client.utils.MathRng;

public class HotbarSlotsSetting extends Setting implements MinecraftAccess {
    private final boolean[] selectedSlots = new boolean[9];

    public HotbarSlotsSetting(CharSequence label) { super(label); }
    public boolean isSelected(int slot) { return selectedSlots[slot]; }
    public void select(int slot) { selectedSlots[slot] = true; }
    public void setSelected(int slot, boolean selected) { selectedSlots[slot] = selected; }
    public void toggle(int slot) { selectedSlots[slot] = !selectedSlots[slot]; }

    public boolean permitsCurrentSlot() {
        boolean anySelected = false;
        for (boolean selected : selectedSlots) {
            if (selected) { anySelected = true; break; }
        }
        if (!anySelected) return true;
        if (CLIENT.player == null) return false;
        return selectedSlots[CLIENT.player.getInventory().getSelectedSlot()];
    }

    public int chooseSelectedSlot() {
        int[] slots = new int[selectedSlots.length];
        int count = 0;
        for (int slot = 0; slot < selectedSlots.length; slot++) {
            if (selectedSlots[slot]) slots[count++] = slot;
        }
        return count == 0 ? -1 : slots[MathRng.randomInt(0, count)];
    }
}
