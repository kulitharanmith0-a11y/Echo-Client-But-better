package dev.dov.hack.echo.client.rendering.impl.opengl.api;

public final class GlShader {
    private final int id;
    private final String name;
    private final String source;
    private boolean linked;
    private String log = "";

    public GlShader(String name, String source) {
        this.name = name;
        this.source = source == null ? "" : source;
        this.id = Math.abs(this.source.hashCode());
    }

    public boolean link() {
        if (this.source.isEmpty() || this.id == 0) {
            this.linked = false;
            this.log = "empty source";
            return false;
        }
        this.linked = true;
        this.log = "";
        return true;
    }

    public int id() {
        return this.id;
    }

    public String name() {
        return this.name;
    }

    public boolean isLinked() {
        return this.linked;
    }

    public String log() {
        return this.log;
    }

    public void fail(String reason) {
        this.linked = false;
        this.log = reason == null ? "unknown" : reason;
    }

    
    public int uniformLocation(String uniform) {
        return this.linked && uniform != null ? Math.abs(uniform.hashCode()) : -1;
    }
}
