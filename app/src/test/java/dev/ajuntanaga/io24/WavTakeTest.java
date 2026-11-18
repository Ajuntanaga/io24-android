package dev.ajuntanaga.io24;

import static org.junit.Assert.*;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import org.junit.Test;

public class WavTakeTest {
    @Test public void stereoHeaderAndSamplesRoundTrip() throws Exception {
        File file = File.createTempFile("take", ".wav");
        try {
            try (WavTake wav = new WavTake(file, 48000, 2)) {
                wav.write(new byte[]{0, 1, 2, 3, 4, 5, 6, 7}, 8);
                assertEquals(2, wav.frames());
            }
            byte[] bytes = Files.readAllBytes(file.toPath());
            ByteBuffer data = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
            assertEquals(52, bytes.length);
            assertEquals("RIFF", new String(bytes, 0, 4, java.nio.charset.StandardCharsets.US_ASCII));
            assertEquals(44, data.getInt(4));
            assertEquals(2, data.getShort(22));
            assertEquals(48000, data.getInt(24));
            assertEquals(192000, data.getInt(28));
            assertEquals(8, data.getInt(40));
            assertEquals(7, bytes[51]);
        } finally { file.delete(); }
    }
    @Test public void monoRejectsPartialFrameAndFinalizesOnError() throws Exception {
        File file = File.createTempFile("take", ".wav");
        try {
            try (WavTake wav = new WavTake(file, 48000, 1)) {
                wav.write(new byte[]{1, 2}, 2);
                assertThrows(java.io.IOException.class, () -> wav.write(new byte[3], 3));
            }
            ByteBuffer data = ByteBuffer.wrap(Files.readAllBytes(file.toPath())).order(ByteOrder.LITTLE_ENDIAN);
            assertEquals(1, data.getShort(22));
            assertEquals(2, data.getInt(40));
        } finally { file.delete(); }
    }
    @Test public void emptyTakeHasValidHeaderAndCloseIsIdempotent() throws Exception {
        File file = File.createTempFile("take", ".wav");
        try {
            WavTake wav = new WavTake(file, 44100, 1);
            wav.close(); wav.close();
            assertEquals(44, file.length());
            assertThrows(java.io.IOException.class, () -> wav.write(new byte[2], 2));
        } finally { file.delete(); }
    }
}
