package common.Binary;

import java.io.IOException;
import java.nio.ByteOrder;

/**
 * Defines the core data-writing contract for binary streaming components. Satisfies the segregation
 * pattern ensuring read-only clients cannot write.
 */
public interface BinaryOutput extends AutoCloseable
{
    @Override
    void close() throws IOException;
    void setByteOrder(ByteOrder order);
    ByteOrder getByteOrder();
    void mark();
    void reset();
    long length() throws IOException;
    long getCurrentPosition() throws IOException;
    void seek(long position) throws IOException;
    void writeByte(byte value) throws IOException;
    void writeBytes(byte[] data) throws IOException;
    void writeShort(short value) throws IOException;
    void writeInteger(int value) throws IOException;
    void writeLong(long value) throws IOException;
    void writeFloat(float value) throws IOException;
    void writeDouble(double value) throws IOException;
}