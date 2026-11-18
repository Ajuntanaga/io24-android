package dev.ajuntanaga.io24;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

/** Streaming PCM16 WAV writer; memory use is independent of take length. */
final class WavTake implements Closeable {
    static final long MAX_DATA = 0xffff_ffffL - 36;
    private final RandomAccessFile file;
    private final int rate, channels;
    private long bytes;
    private boolean closed;
    private boolean finalized;

    WavTake(File path, int rate, int channels) throws IOException {
        if (rate <= 0 || channels < 1 || channels > 2) throw new IllegalArgumentException();
        this.rate = rate;
        this.channels = channels;
        file = new RandomAccessFile(path, "rw");
        file.setLength(0);
        header();
    }

    void write(byte[] pcm, int length) throws IOException {
        if (closed || length < 0 || length > pcm.length || length % (channels * 2) != 0)
            throw new IOException("Invalid PCM frame");
        if (bytes + length > MAX_DATA) throw new IOException("WAV size limit reached; take saved");
        file.write(pcm, 0, length);
        bytes += length;
    }

    long frames() { return bytes / (channels * 2); }
    boolean finalized() { return finalized; }

    private void le(long value, int count) throws IOException {
        for (int i = 0; i < count; i++) file.write((int) (value >> (i * 8)) & 255);
    }

    private void header() throws IOException {
        file.seek(0);
        file.writeBytes("RIFF"); le(bytes + 36, 4); file.writeBytes("WAVEfmt ");
        le(16, 4); le(1, 2); le(channels, 2); le(rate, 4);
        le((long) rate * channels * 2, 4); le(channels * 2, 2); le(16, 2);
        file.writeBytes("data"); le(bytes, 4);
    }

    @Override public void close() throws IOException {
        if (closed) return;
        closed = true;
        try { header(); file.getFD().sync(); finalized = true; } finally { file.close(); }
    }
}
