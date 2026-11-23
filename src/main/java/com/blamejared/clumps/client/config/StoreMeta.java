package com.blamejared.clumps.client.config;

public record StoreMeta(
        String id, String name, String description, String author, String serverName, String serverIp, byte[] serverIcon, String type, boolean publicVisibility, long createdAt, long updatedAt, boolean active, boolean owned, String sourceId, String publishedId, boolean publishedChanges, boolean updateAvailable) {

    
    public boolean isPublished() {
        return !publishedId.isBlank();
    }
}
