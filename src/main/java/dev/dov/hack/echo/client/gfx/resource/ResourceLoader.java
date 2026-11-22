package dev.dov.hack.echo.client.gfx.resource;

public interface ResourceLoader {
    String loadText(String path);

    byte[] loadBytes(String path);
}
