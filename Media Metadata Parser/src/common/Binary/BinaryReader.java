package common.Binary;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.charset.Charset;

/**
 * A generic interface for positional, stream-based binary reading.
 *
 * <p>
 * Supports seeking, skipping, and mark/reset operations, as well as reading primitive values using
 * a configurable {@link ByteOrder}.
 * </p>
 */
public interface BinaryReader extends Closeable
{
    byte readByte() throws IOException;
    int readUnsignedByte() throws IOException;
    byte[] readBytes(int length) throws IOException;
    short readShort() throws IOException;
    int readUnsignedShort() throws IOException;
    int readInteger() throws IOException;
    long readUnsignedInteger() throws IOException;
    int readUnsignedInteger24() throws IOException;
    long readLong() throws IOException;
    float readFloat() throws IOException;
    double readDouble() throws IOException;
    byte peek(long offset) throws IOException;
    byte[] peek(long offset, int length) throws IOException;
    String readString() throws IOException;
    String readString(Charset charset) throws IOException;
}