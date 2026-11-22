package dev.dov.hack.echo.client.utils.audio;

import java.nio.ShortBuffer;

record PcmAudio(ShortBuffer samples, int channels, int sampleRate) {
}
