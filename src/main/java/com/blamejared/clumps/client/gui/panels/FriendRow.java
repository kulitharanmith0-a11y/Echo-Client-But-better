package com.blamejared.clumps.client.gui.panels;

import net.minecraft.client.network.PlayerListEntry;

final class FriendRow {
    final String playerName;
    final PlayerListEntry playerEntry;
    boolean friend;

    FriendRow(String playerName, PlayerListEntry playerEntry, boolean friend) {
        this.playerName = playerName;
        this.playerEntry = playerEntry;
        this.friend = friend;
    }
}
