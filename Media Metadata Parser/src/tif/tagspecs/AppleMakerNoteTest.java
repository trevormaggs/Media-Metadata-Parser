package tif.tagspecs;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import batch.MediaRecord;
import batch.MetadataInspectionEvent;
import common.ByteValueConverter;
import common.binary.ByteArrayReader;
import tif.DirectoryIFD.EntryIFD;
import tif.IFDHandler;
import tif.TifFieldType;
import tif.CameraMarkerNote;

public class AppleMakerNoteTest extends CameraMarkerNote
{
    private static final byte[] APPLE_IOS_IDENTIFIER = "Apple iOS\0".getBytes(StandardCharsets.UTF_8);
    private static final int PREAMBLE_LENGTH = 12; // 10 bytes signature + 2 bytes version/padding

    @Override
    public boolean supportsMake(String cameraMake)
    {
        return (cameraMake != null && cameraMake.toLowerCase().contains("apple"));
    }

    @Override
    public List<MetadataInspectionEvent> processMakerNote(EntryIFD entry, MediaRecord record)
    {
        final int headerLength = 14;
        byte[] rawBytes = entry.getByteArray();
        List<MetadataInspectionEvent> events = new ArrayList<>();

        if (rawBytes == null || rawBytes.length < PREAMBLE_LENGTH + 4 || !Arrays.equals(Arrays.copyOf(rawBytes, APPLE_IOS_IDENTIFIER.length), APPLE_IOS_IDENTIFIER))
        {
            return Collections.emptyList();
        }

        try (ByteArrayReader reader = new ByteArrayReader(rawBytes))
        {
            reader.setByteOrder(ByteOrder.BIG_ENDIAN);
            reader.seek(headerLength);

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
                    String tagName = lookupTagName(tagId);
                    String str = resolveTagValue(reader, rawBytes, fieldType, count, valueOrOffset, tagId);

                    if (str != null && !str.isEmpty())
                    {
                        events.add(new MetadataInspectionEvent(record, "Apple", tagName, str));
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

    private String resolveTagValue(ByteArrayReader reader, byte[] rawBytes, TifFieldType fieldType, long count, long valueOrOffset, int tagId)
    {
        long totalBytes = count * fieldType.getFieldSize();

        if (totalBytes <= IFDHandler.ENTRY_MAX_VALUE_LENGTH)
        {
            int valInt = (int) valueOrOffset;
            
            if (tagId == 0x0007) return valInt == 1 ? "Yes" : "No"; // AEStable
            if (tagId == 0x0014) return translateImageCaptureType(valInt);
            if (tagId == 0x002e) return translateCameraType(valInt);

            return String.valueOf(valueOrOffset);
        }
        
        else
        {
            if (valueOrOffset >= 0 && (valueOrOffset + totalBytes) <= rawBytes.length)
            {
                try
                {
                    byte[] block = reader.peek(valueOrOffset, (int) totalBytes);

                    if (fieldType == TifFieldType.TYPE_ASCII)
                    {
                        int len = block.length;
                        
                        while (len > 0 && block[len - 1] == 0)
                        {
                            len--;
                        }
                        
                        return new String(block, 0, len, StandardCharsets.UTF_8);
                    }
                    
                    else if (tagId == 0x0008 && count == 3) // AccelerationVector
                    {
                        ByteBuffer buf = ByteBuffer.wrap(block).order(ByteOrder.BIG_ENDIAN);
                        
                        double x = readRational(buf);
                        double y = readRational(buf);
                        double z = readRational(buf);
                        return String.format("%.10f %.10f %.10f", x, y, z);
                    }
                    
                    else if (tagId == 0x0009 && count == 2) // FocusDistanceRange
                    {
                        ByteBuffer buf = ByteBuffer.wrap(block).order(ByteOrder.BIG_ENDIAN);
                        
                        double min = readRational(buf);
                        double max = readRational(buf);
                        return String.format("%.2f - %.2f m", min, max);
                    }
                    
                    else
                    {
                        return String.format("Binary Data (%d bytes at offset 0x%04X)", totalBytes, valueOrOffset);
                    }
                }
                
                catch (Exception exc)
                {
                    // Fallback on read error
                }
            }
        }

        return null;
    }

    private double readRational(ByteBuffer buf)
    {
        int num = buf.getInt();
        int den = buf.getInt();
        
        return (den != 0) ? ((double) num / den) : 0.0;
    }

    private String translateCameraType(int val)
    {
        switch (val)
        {
            case 0:
                return "Back Wide";

            case 1:
                return "Back Normal";

            case 2:
                return "Back Telephoto";

            case 3:
                return "Front";

            case 4:
                return "Back Ultra Wide";

            default:
                return String.valueOf(val);
        }
    }

    private String translateImageCaptureType(int val)
    {
        switch (val)
        {
            case 1:
                return "ProRAW";

            case 2:
                return "Portrait";

            case 10:
                return "Photo";

            case 11:
                return "Manual Focus";

            case 12:
                return "Scene";

            default:
                return String.valueOf(val);
        }
    }

    private String lookupTagName(int tagId)
    {
        switch (tagId)
        {
            case 0x0001:
                return "MakerNoteVersion";

            case 0x0003:
                return "RunTime";

            case 0x0007:
                return "AEStable";

            case 0x0008:
                return "AccelerationVector";

            case 0x0009:
                return "FocusDistanceRange";

            case 0x000a:
                return "HDRImageType";

            case 0x000b:
                return "BurstUUID";

            case 0x000f:
                return "QualityHint";

            case 0x0011:
                return "ContentIdentifier";

            case 0x0014:
                return "ImageCaptureType";

            case 0x0017:
                return "LivePhotoVideoIndex";

            case 0x0028:
                return "PhotoIdentifier";

            case 0x002e:
                return "CameraType";

            case 0x0030:
                return "FocusPosition";

            case 0x0033:
                return "ColorTemperature";

            case 0x0038:
                return "AFMeasuredDepth";

            case 0x0063:
                return "ReferenceImageFlag";

            default:
                return String.format("Apple_0x%04x", tagId);
        }
    }
}