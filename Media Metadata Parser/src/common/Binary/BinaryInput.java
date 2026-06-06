package common.Binary;

import java.io.IOException;
import java.nio.charset.Charset;

public interface BinaryInput
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