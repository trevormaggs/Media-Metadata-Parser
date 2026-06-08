package common.binary;

import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.charset.Charset;

/**
 * Defines the core data-reading contract for binary streaming components, supporting standard stream positioning and navigation operations.
 */
public interface BinaryInput extends AutoCloseable
{
    @Override
    void close() throws IOException;
    void setByteOrder(ByteOrder order);
    ByteOrder getByteOrder();
    void mark();
    void reset();
    long length() throws IOException;
    long getCurrentPosition() throws IOException;
    void skip(long n) throws IOException;
    void seek(long position) throws IOException;
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