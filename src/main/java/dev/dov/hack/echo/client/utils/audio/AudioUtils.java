package dev.dov.hack.echo.client.utils.audio;

import dev.dov.hack.echo.client.MinecraftAccess;
import dev.dov.hack.echo.client.gfx.util.ClientResources;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import net.minecraft.sound.SoundCategory;
import org.lwjgl.openal.AL10;
import org.lwjgl.stb.STBVorbis;
import org.lwjgl.stb.STBVorbisInfo;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

public final class AudioUtils implements MinecraftAccess {
    private static final Map<String, String> soundResources = Map.of();
    private static final Map<String, PcmAudio> decodedAudio = new HashMap<>();
    private static final Map<String, Integer> audioBuffers = new HashMap<>();
    private static final List<Integer> playingSources = new ArrayList<>();
    private static final ConcurrentLinkedQueue<PlaybackRequest> playbackRequests = new ConcurrentLinkedQueue<>();
    private static volatile boolean buffersInitialized;

    private AudioUtils() {
    }

    
    public static void preload() {
        for (Map.Entry<String, String> resource : soundResources.entrySet()) {
            String name = resource.getKey();
            String path = resource.getValue();
            byte[] bytes = ClientResources.readBytes(path);
            if (bytes == null) {
                continue;
            }
            ByteBuffer data = MemoryUtil.memAlloc(bytes.length);
            data.put(bytes).flip();
            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer error = stack.mallocInt(1);
                long decoder = STBVorbis.stb_vorbis_open_memory(data, error, null);
                if (decoder == 0L) {
                    MemoryUtil.memFree((java.nio.Buffer) data);
                    continue;
                }
                STBVorbisInfo info = STBVorbisInfo.malloc(stack);
                STBVorbis.stb_vorbis_get_info(decoder, info);
                int channels = info.channels();
                int sampleRate = info.sample_rate();
                int sampleCount = STBVorbis.stb_vorbis_stream_length_in_samples(decoder);
                ShortBuffer samples = MemoryUtil.memAllocShort(sampleCount * channels);
                STBVorbis.stb_vorbis_get_samples_short_interleaved(decoder, channels, samples);
                STBVorbis.stb_vorbis_close(decoder);
                decodedAudio.put(name, new PcmAudio(samples, channels, sampleRate));
            }
            MemoryUtil.memFree((java.nio.Buffer) data);
        }
    }

    
    public static void initializeBuffers() {
        if (buffersInitialized) {
            return;
        }
        buffersInitialized = true;
        for (Map.Entry<String, PcmAudio> entry : decodedAudio.entrySet()) {
            PcmAudio audio = entry.getValue();
            int[] buffer = new int[1];
            AL10.alGenBuffers(buffer);
            int format = audio.channels() == 1 ? AL10.AL_FORMAT_MONO16 : AL10.AL_FORMAT_STEREO16;
            audio.samples().rewind();
            AL10.alBufferData(buffer[0], format, audio.samples(), audio.sampleRate());
            audioBuffers.put(entry.getKey(), buffer[0]);
        }
    }

    
    public static void rebuildBuffers() {
        for (int source : playingSources) {
            AL10.alSourceStop(source);
            AL10.alDeleteSources(source);
        }
        playingSources.clear();
        playbackRequests.clear();
        for (int buffer : audioBuffers.values()) {
            AL10.alDeleteBuffers(buffer);
        }
        audioBuffers.clear();
        buffersInitialized = false;
        initializeBuffers();
    }

    
    public static void play(String name, float gain, float pitch) {
        if (!buffersInitialized) {
            return;
        }
        Integer buffer = audioBuffers.get(name);
        if (buffer == null) {
            return;
        }
        float masterVolume = CLIENT.options.getSoundVolume(SoundCategory.MASTER);
        float uiVolume = CLIENT.options.getSoundVolume(SoundCategory.UI);
        float effectiveGain = gain * masterVolume * uiVolume;
        if (effectiveGain <= 0.0F) {
            return;
        }
        playbackRequests.offer(new PlaybackRequest(buffer, effectiveGain, pitch));
    }

    
    public static void tick() {
        PlaybackRequest request;
        while ((request = playbackRequests.poll()) != null) {
            int[] source = new int[1];
            AL10.alGenSources(source);
            if (source[0] == 0 || AL10.alGetError() != AL10.AL_NO_ERROR) {
                continue;
            }
            AL10.alSourcei(source[0], AL10.AL_BUFFER, request.bufferId());
            AL10.alSourcef(source[0], AL10.AL_GAIN, request.gain());
            AL10.alSourcef(source[0], AL10.AL_PITCH, request.pitch());
            AL10.alSourcei(source[0], AL10.AL_SOURCE_RELATIVE, AL10.AL_TRUE);
            AL10.alSource3f(source[0], AL10.AL_POSITION, 0.0F, 0.0F, 0.0F);
            AL10.alSourcePlay(source[0]);
            playingSources.add(source[0]);
        }
        playingSources.removeIf(source -> {
            int state = AL10.alGetSourcei(source, AL10.AL_SOURCE_STATE);
            if (state == AL10.AL_STOPPED || state == AL10.AL_INITIAL) {
                AL10.alDeleteSources(source);
                return true;
            }
            return false;
        });
    }

    
    public static void shutdown() {
        for (int source : playingSources) {
            AL10.alSourceStop(source);
            AL10.alDeleteSources(source);
        }
        playingSources.clear();
        for (int buffer : audioBuffers.values()) {
            AL10.alDeleteBuffers(buffer);
        }
        audioBuffers.clear();
        for (PcmAudio audio : decodedAudio.values()) {
            MemoryUtil.memFree((java.nio.Buffer) audio.samples());
        }
        decodedAudio.clear();
        buffersInitialized = false;
    }
}
