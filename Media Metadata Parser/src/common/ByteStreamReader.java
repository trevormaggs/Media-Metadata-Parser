package common;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.file.Path;

/**
 * A generic interface for positional, stream-based binary reading.
 *
 * <p>
 * Supports seeking, skipping, and mark/reset operations, as well as reading primitive values using
 * a configurable {@link ByteOrder}.
 * </p>
 */
public interface ByteStreamReader extends Closeable
{
    Path getFilename(); // Maybe change to Path getPath()?
    void setByteOrder(ByteOrder order);
    ByteOrder getByteOrder();
    long length();
    long getCurrentPosition() throws IOException;
    void skip(long n) throws IOException;
    void seek(long n) throws IOException;
    void mark() throws IOException;
    void reset() throws IOException;
    byte peek(long offset) throws IOException;
    byte[] peek(long offset, int length) throws IOException;
    byte readByte() throws IOException;
    byte[] readBytes(int length) throws IOException;
    int readUnsignedByte() throws IOException;
    short readShort() throws IOException;
    int readUnsignedShort() throws IOException;
    int readInteger() throws IOException;
    long readUnsignedInteger() throws IOException;
    int readUnsignedInt24() throws IOException;
    long readLong() throws IOException;
    float readFloat() throws IOException;
    double readDouble() throws IOException;
    String readString() throws IOException;
}