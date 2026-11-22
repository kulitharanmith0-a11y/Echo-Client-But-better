package dev.dov.hack.echo.client.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.Base64;

public final class ProfileCodec {
    private ProfileCodec() { }

    
    public static JsonObject parseObject(String text) {
        try {
            JsonElement element = JsonParser.parseString(text);
            return element.isJsonObject() ? element.getAsJsonObject() : new JsonObject();
        } catch (Exception exception) {
            return new JsonObject();
        }
    }

    
    public static boolean isSuccessful(JsonObject response) {
        return response != null && (!response.has("success") || response.get("success").getAsBoolean());
    }

    
    public static JsonObject objectField(JsonObject object, String name) {
        if (object == null || !object.has(name) || !object.get(name).isJsonObject()) {
            return null;
        }
        return object.getAsJsonObject(name);
    }

    
    public static String textField(JsonObject object, String name) {
        return textField(object, name, "");
    }

    
    public static String textField(JsonObject object, String name, String fallback) {
        if (object == null || !object.has(name) || object.get(name).isJsonNull()) {
            return fallback;
        }
        return object.get(name).getAsString();
    }

    
    public static long timestampField(JsonObject object, String name) {
        try {
            if (object == null || !object.has(name) || object.get(name).isJsonNull()) {
                return 0L;
            }
            return object.get(name).getAsLong();
        } catch (Exception exception) {
            return 0L;
        }
    }

    
    public static boolean booleanField(JsonObject object, String name) {
        return booleanField(object, name, false);
    }

    
    public static boolean booleanField(JsonObject object, String name, boolean fallback) {
        try {
            if (object == null || !object.has(name) || object.get(name).isJsonNull()) {
                return fallback;
            }
            return object.get(name).getAsBoolean();
        } catch (Exception exception) {
            return fallback;
        }
    }

    
    public static byte[] decodeIcon(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return Base64.getDecoder().decode(text);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    
    public static StoreMeta metadata(JsonObject object) {
        return new StoreMeta(
                textField(object, "id"), textField(object, "name"), textField(object, "description"), textField(object, "author"), textField(object, "serverName"), textField(object, "serverIp"), decodeIcon(textField(object, "serverIconBase64")), textField(object, "type", "GHOST"), "PUBLIC".equalsIgnoreCase(textField(object, "visibility")), timestampField(object, "createdAt"), timestampField(object, "updatedAt"), booleanField(object, "active"), booleanField(object, "owned", true), textField(object, "sourceId"), textField(object, "publishedId"), booleanField(object, "publishedChanges"), booleanField(object, "updateAvailable"));
    }
}
