package dev.dov.hack.echo.client.gfx.shader.glsl;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.jadyen.hack.echo.natives.NativeModule;
import dev.jadyen.hack.echo.natives.NativeModuleInfo;
import dev.dov.hack.echo.client.gfx.shader.PrimitiveKind;
import dev.dov.hack.echo.client.gfx.shader.ScalarKind;
import dev.dov.hack.echo.client.gfx.shader.wgsl.WgslToGlsl;
import java.util.Map;

public final class GlslWriter {
    public GlslOptions options;
    public GlslPipelineOptions pipelineOptions;
    public GlslReflection reflection = new GlslReflection();

    public GlslWriter(GlslOptions options, GlslPipelineOptions pipelineOptions) {
        this.options = options;
        this.pipelineOptions = pipelineOptions;
    }

    public String write(NativeModule module, NativeModuleInfo info) {
        String json = WgslToGlsl.transpile(
            module.wgslSource(),
            options.version,
            pipelineOptions.stage.ordinal(),
            pipelineOptions.entryPoint
        );
        return readOutput(json);
    }

    public String readOutput(String json) {
        JsonObject output = JsonParser.parseString(json).getAsJsonObject();
        JsonObject reflected = output.getAsJsonObject("reflection");
        for (Map.Entry<String, JsonElement> entry
                : reflected.getAsJsonObject("texture_mapping").entrySet()) {
            JsonObject value = entry.getValue().getAsJsonObject();
            TextureMapping mapping = new TextureMapping();
            mapping.textureBinding = value.get("texture_binding").getAsInt();
            JsonElement sampler = value.get("sampler_binding");
            mapping.samplerBinding = sampler.isJsonNull() ? null : sampler.getAsInt();
            reflection.textureMappings.put(entry.getKey(), mapping);
        }
        for (JsonElement entry : reflected.getAsJsonArray("push_constant_items")) {
            JsonObject value = entry.getAsJsonObject();
            PushConstantItem item = new PushConstantItem();
            item.accessPath = value.get("access_path").getAsString();
            item.offset = value.get("offset").getAsInt();
            item.kind = PrimitiveKind.valueOf(value.get("kind").getAsString());
            item.scalarKind = ScalarKind.valueOf(value.get("scalar_kind").getAsString());
            item.components = value.get("components").getAsInt();
            item.columns = value.get("columns").getAsInt();
            reflection.pushConstantItems.add(item);
        }
        return output.get("source").getAsString();
    }
}
