package common.Binary;

import java.io.IOException;

public interface BinaryOutput
{
    void writeByte(byte value) throws IOException;
    void writeBytes(byte[] data) throws IOException;
    void writeShort(short value) throws IOException;
    void writeInteger(int value) throws IOException;
    void writeLong(long value) throws IOException;
    void writeFloat(float value) throws IOException;
    void writeDouble(double value) throws IOException;
}