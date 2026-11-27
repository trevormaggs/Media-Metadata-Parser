package tif.tagspecs;

import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import batch.MediaRecord;
import batch.MetadataInspectionEvent;
import common.ByteValueConverter;
import common.binary.ByteArrayReader;
import tif.CameraMarkerNote;
import tif.DirectoryIFD.EntryIFD;
import tif.IFDHandler;
import tif.TifFieldType;

public class CanonMakerNote extends CameraMarkerNote
{
    @Override
    public boolean supportsMake(String cameraMake)
    {
        return cameraMake != null && cameraMake.toLowerCase().contains("canon");
    }

    @Override
    public List<MetadataInspectionEvent> processMakerNote(EntryIFD entry, MediaRecord record)
    {
        byte[] rawBytes = entry.getByteArray();
        List<MetadataInspectionEvent> events = new ArrayList<>();

        if (rawBytes == null || rawBytes.length < 2) return Collections.emptyList();

        try (ByteArrayReader reader = new ByteArrayReader(rawBytes, ByteOrder.LITTLE_ENDIAN))
        {
            //int entryCount = reader.readUnsignedShort();
            // Iterate and parse 12-byte IFD blocks...
        }

        catch (Exception exc)
        {
            // Handle parsing exceptions
        }

        return events;
    }

    public List<MetadataInspectionEvent> processMakerNote2(EntryIFD entry, MediaRecord record)
    {
        byte[] rawBytes = entry.getByteArray();
        List<MetadataInspectionEvent> events = new ArrayList<>();

        if (rawBytes == null || rawBytes.length < 2)
        {
            return Collections.emptyList();
        }

        // Canon MakerNotes use Little-Endian byte order
        try (ByteArrayReader reader = new ByteArrayReader(rawBytes, ByteOrder.LITTLE_ENDIAN))
        {
            int entryCount = reader.readUnsignedShort();

            for (int i = 0; i < entryCount; i++)
            {
                if (reader.remaining() >= 12)
                {
                    int tagId = reader.readUnsignedShort();
                    TifFieldType fieldType = TifFieldType.getTiffType(reader.readUnsignedShort());
                    long count = reader.readUnsignedInteger();
                    byte[] valueBytes = reader.readBytes(4);
                    long valueOrOffset = ByteValueConverter.toUnsignedInteger(valueBytes, reader.getByteOrder());
                    long totalBytes = count * fieldType.getFieldSize();

                    Object rawValue = null;

                    if (totalBytes <= IFDHandler.ENTRY_MAX_VALUE_LENGTH)
                    {
                        rawValue = valueOrOffset;
                    }
                    else if (valueOrOffset >= 0 && (valueOrOffset + totalBytes) <= reader.length())
                    {
                        byte[] block = reader.peek(valueOrOffset, (int) totalBytes);

                        if (fieldType == TifFieldType.TYPE_ASCII)
                        {
                            rawValue = new String(ByteValueConverter.readFirstNullTerminatedByteArray(block), StandardCharsets.UTF_8);
                        }
                        else if (fieldType == TifFieldType.TYPE_LONG_S || fieldType == TifFieldType.TYPE_LONG_U)
                        {
                            rawValue = ByteValueConverter.toIntegerArray(block, reader.getByteOrder());
                        }
                        else if (fieldType == TifFieldType.TYPE_UNDEFINED)
                        {
                            rawValue = block;
                        }
                    }

                    String[] infoPair = translateCanonTag(tagId, rawValue);

                    if (infoPair != null && infoPair.length >= 2 && infoPair[0] != null)
                    {
                        events.add(new MetadataInspectionEvent(record, "Canon", infoPair[0], infoPair[1]));
                    }
                }
            }
        }

        catch (Exception exc)
        {
            // Gracefully handle malformed binary streams
        }

        return events;
    }

    private String[] translateCanonTag(int tagId, Object rawValue)
    {
        return null;
    }
}