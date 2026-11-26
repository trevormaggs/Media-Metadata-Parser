package tif.tagspecs;

import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import batch.MediaRecord;
import batch.MetadataInspectionEvent;
import common.ByteValueConverter;
import common.binary.ByteArrayReader;
import tif.DirectoryIFD.EntryIFD;
import tif.TifFieldType;
import tif.CameraMarkerNote;
import tif.IFDHandler;
import tif.RationalNumber;
import tif.TagValueTranslator;

public class AppleMakerNote extends CameraMarkerNote
{
    private static final byte[] APPLE_IOS_IDENTIFIER = "Apple iOS\0".getBytes(StandardCharsets.UTF_8);
    private static final int PREAMBLE_LENGTH = 12; // 10 bytes signature + 2 bytes version/padding

    @Override
    public boolean supportsMake(String cameraMake)
    {
        return (cameraMake != null && cameraMake.toLowerCase().contains("apple"));
    }

    /**
     * Parses Apple iOS MakerNote proprietary binary structure from a DNG/TIFF file container.
     * 
     * <p>
     * <strong>Apple MakerNote Binary Layout:</strong>
     * </p>
     * 
     * <table border="1" cellpadding="4" cellspacing="0">
     * <caption>Apple MakerNote Binary Layout</caption>
     * <thead>
     * <tr>
     * <th>Offset / Range</th>
     * <th>Size</th>
     * <th>Description</th>
     * </tr>
     * </thead>
     * <tbody>
     * <tr>
     * <td>Bytes 0–9</td>
     * <td>10 bytes</td>
     * <td>Preamble signature string {@code "Apple iOS\0"}</td>
     * </tr>
     * <tr>
     * <td>Bytes 10–11</td>
     * <td>2 bytes</td>
     * <td>Version padding / alignment bytes</td>
     * </tr>
     * <tr>
     * <td>Bytes 12–13</td>
     * <td>2 bytes</td>
     * <td>Byte order marker {@code 'MM'} (Big-Endian motorola byte ordering)</td>
     * </tr>
     * <tr>
     * <td>Offset 0x0E (14)</td>
     * <td>2 bytes</td>
     * <td>Entry Count (unsigned short specifying total IFD attributes)</td>
     * </tr>
     * <tr>
     * <td>Offset 0x10 onward</td>
     * <td>Variable</td>
     * <td>Flat 12-byte IFD entry blocks (Tag ID, Field Type, Count, Value/Offset). Out-of-line data
     * offsets are absolute relative to the start of the MakerNote buffer ({@code 0x00}).</td>
     * </tr>
     * </tbody>
     * </table>
     *
     * @param entry
     *        the TIFF IFD entry that encapsulates the raw Apple MakerNote byte array to be unpacked
     * @param record
     *        the target media record associated with the inspection batch
     * @return a list of extracted {@link MetadataInspectionEvent} items
     */
    @Override
    public List<MetadataInspectionEvent> processMakerNote(EntryIFD entry, MediaRecord record)
    {
        // 10-byte signature + 2-byte version padding + 2-byte 'MM' marker
        final int headerLength = 14;
        byte[] rawBytes = entry.getByteArray();
        List<MetadataInspectionEvent> events = new ArrayList<>();

        if (rawBytes == null || rawBytes.length < PREAMBLE_LENGTH + 4 || !Arrays.equals(Arrays.copyOf(rawBytes, APPLE_IOS_IDENTIFIER.length), APPLE_IOS_IDENTIFIER))
        {
            return Collections.emptyList();
        }

        try (ByteArrayReader reader = new ByteArrayReader(rawBytes, ByteOrder.BIG_ENDIAN))
        {
            reader.seek(headerLength);

            int entryCount = reader.readUnsignedShort();

            // Iterate through the 12-byte IFD entries starting at offset 0x10
            for (int i = 0; i < entryCount; i++)
            {
                if (reader.remaining() >= 12)
                {
                    String[] infoPair = null;
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

                        else if (fieldType == TifFieldType.TYPE_RATIONAL_S)
                        {
                            RationalNumber[] arr = ByteValueConverter.toRationalArray(block, reader.getByteOrder(), RationalNumber.DataType.SIGNED);
                            rawValue = arr;
                        }

                        else if (fieldType == TifFieldType.TYPE_LONG_S)
                        {
                            int[] arr = ByteValueConverter.toIntegerArray(block, reader.getByteOrder());
                            rawValue = arr;
                        }

                        else if (fieldType == TifFieldType.TYPE_UNDEFINED)
                        {
                            rawValue = block;
                        }

                        else
                        {
                            rawValue = String.format("Binary Data (%d bytes)", totalBytes);
                        }
                    }

                    else
                    {
                        rawValue = "X";
                    }

                    infoPair = translate(tagId, rawValue);

                    System.out.format("0x%04X\t%s\n", tagId, Arrays.toString(infoPair));
                }
            }
        }

        catch (Exception exc)
        {
            // Gracefully handle malformed binary streams
        }

        return events;
    }

    private Long extractBplistIntegerKey(byte[] block, String key)
    {
        byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
        
        for (int i = 0; i < block.length - keyBytes.length; i++)
        {
            boolean match = true;
            
            for (int j = 0; j < keyBytes.length; j++)
            {
                if (block[i + j] != keyBytes[j])
                {
                    match = false;
                    break;
                }
            }
            
            if (match)
            {
                // Look ahead past the key string for the integer object marker (typically 0x10,
                // 0x12, 0x13 for 1, 4, or 8-byte ints)
                int valIdx = i + keyBytes.length;
                
                if (valIdx + 1 < block.length)
                {
                    int marker = block[valIdx] & 0xFF;
                    // Binary plist integer markers: 0x10 (1 byte), 0x11 (2 bytes), 0x12 (4 bytes),
                    // 0x13 (8 bytes)
                    if (marker >= 0x10 && marker <= 0x13)
                    {
                        int bytesLen = 1 << (marker & 0x03);
                        
                        if (valIdx + 1 + bytesLen <= block.length)
                        {
                            long val = 0;
                            
                            for (int b = 0; b < bytesLen; b++)
                            {
                                val = (val << 8) | (block[valIdx + 1 + b] & 0xFF);
                            }
                            
                            return val;
                        }
                    }
                }
            }
        }
        
        return null;
    }

    private String[] translate(int tagId, Object rawData)
    {
        String tagName = null;

        switch (tagId)
        {
            case 0x0001:
                tagName = "MakerNoteVersion";
            break;

            case 0x0002:
                tagName = "AEMatrix";

                if (rawData instanceof byte[])
                {
                    rawData = String.format("Binary Data (%d bytes)", ((byte[]) rawData).length);
                }
            break;

            case 0x0003:
                tagName = "RunTime";

                if (rawData instanceof byte[])
                {
                    final String name = "bplist";
                    byte[] block = (byte[]) rawData;

                    if (block.length > 8 && new String(block, 0, name.length(), StandardCharsets.UTF_8).equals(name))
                    {
                        rawData = "RunTime (Binary PLIST)";
                    }

                    else
                    {
                        rawData = String.format("Binary Data (%d bytes)", block.length);
                    }
                }

            break;

            case 0x0004:
                tagName = "AEStable";

                if (rawData instanceof Number)
                {
                    long val = ((Number) rawData).longValue();
                    rawData = (val == 1 ? "Yes" : (val == 0 ? "No" : rawData));
                }

            break;

            case 0x0005:
                tagName = "AETarget";
            break;

            case 0x0006:
                tagName = "AEAverage";
            break;

            case 0x0007:
                tagName = "AFStable";

                if (rawData instanceof Number)
                {
                    long val = ((Number) rawData).longValue();
                    rawData = (val == 1 ? "Yes" : (val == 0 ? "No" : rawData));
                }

            break;

            case 0x0008:
                tagName = "AccelerationVector";
            break;

            case 0x000A:
                tagName = "HDRImageType";
            break;

            case 0x000B:
                tagName = "BurstUUID";
            break;

            case 0x000C:
                tagName = "FocusDistanceRange";

                if (rawData instanceof RationalNumber[])
                {
                    RationalNumber[] arr = ((RationalNumber[]) rawData);

                    if (arr != null && arr.length >= 2 && arr[0] != null && arr[1] != null)
                    {
                        double low = arr[0].doubleValue();
                        double high = arr[1].doubleValue();
                        rawData = String.format(Locale.ROOT, "%.02f - %.02f m", low, high);
                    }
                }

            break;

            case 0x000F:
                tagName = "OISMode";
            break;

            case 0x0011:
                tagName = "ContentIdentifier";
            break;

            case 0x0014:
                tagName = "ImageCaptureType";

                if (rawData instanceof Number)
                {
                    int val = ((Number) rawData).intValue();

                    switch (val)
                    {
                        case 1:
                            rawData = "ProRAW";
                        break;

                        case 2:
                            rawData = "Portrait";
                        break;

                        case 10:
                            rawData = "Photo";
                        break;

                        case 11:
                            rawData = "Manual Focus";
                        break;

                        case 12:
                            rawData = "Scene";
                        break;

                        default:
                            rawData = String.valueOf(val);
                    }
                }

            break;

            case 0x0015:
                tagName = "ImageUniqueID";
            break;

            case 0x0017:
                tagName = "LivePhotoVideoIndex";
            break;

            case 0x0019:
                tagName = "ImageProcessingFlags";
            break;

            case 0x001A:
                tagName = "QualityHint";
            break;

            case 0x001D:
                tagName = "LuminanceNoiseAmplitude";
            break;

            case 0x001F:
                tagName = "PhotosAppFeatureFlags";
            break;

            case 0x0020:
                tagName = "ImageCaptureRequestID";
            break;

            case 0x0021:
                tagName = "HDRHeadroom";
            break;

            case 0x0023:
                tagName = "AFPerformance";

                if (rawData instanceof int[])
                {
                    int[] arr = (int[]) rawData;

                    if (arr.length >= 2)
                    {
                        // Example unpacking if the second integer is bit-packed:
                        int val1 = arr[0];
                        int sub1 = (arr[1] >> 28) & 0x0F; // Adjust bit shift based on spec
                        int sub2 = arr[1] & 0xFFFF; // Low bits

                        rawData = val1 + " " + sub1 + " " + sub2;
                    }
                }

            break;

            case 0x0025:
                tagName = "SceneFlags";
            break;

            case 0x0026:
                tagName = "SignalToNoiseRatioType";
            break;

            case 0x0027:
                tagName = "SignalToNoiseRatio";
            break;

            case 0x002B:
                tagName = "PhotoIdentifier";
            break;

            case 0x002D:
                tagName = "ColorTemperature";
            break;

            case 0x002E:
                tagName = "CameraType";
            break;

            case 0x002F:
                tagName = "FocusPosition";
            break;

            case 0x0030:
                tagName = "HDRGain";
            break;

            case 0x0038:
                tagName = "AFMeasuredDepth";
            break;

            case 0x003D:
                tagName = "AFConfidence";
            break;

            case 0x003E:
                tagName = "ColorCorrectionMatrix";
            break;

            case 0x003F:
                tagName = "GreenGhostMitigationStatus";
            break;

            case 0x0040:
                tagName = "SemanticStyle";
            break;

            case 0x0041:
                tagName = "SemanticStyleRenderingVer";
            break;

            case 0x0042:
                tagName = "SemanticStylePreset";
            break;

            default:
                tagName = null;
            break;
        }

        String valueStr = TagValueTranslator.toStringValue(rawData, null);

        return new String[]{tagName, valueStr};
    }
    
    /**
     * Parses the Apple binary plist (bplist00) CMTime structure for RunTime (0x0003)
     * and emits individual sub-tag inspection events matching ExifTool.
     */
    private void parseRunTimePlist(byte[] block, MediaRecord record, List<MetadataInspectionEvent> events)
    {
        try
        {
            // Verify bplist00 header signature
            if (block.length > 8 && new String(block, 0, 6, StandardCharsets.UTF_8).equals("bplist"))
            {
                // Apple's CMTime bplist dictionary contains keys: flags, value, timescale, epoch.
                // We perform targeted pattern searches within the binary block for known key signatures 
                // or extract the serialized integer markers reliably.
                
                Long flags = extractBplistIntegerKey(block, "flags");
                Long value = extractBplistIntegerKey(block, "value");
                Long timescale = extractBplistIntegerKey(block, "timescale");
                Long epoch = extractBplistIntegerKey(block, "epoch");

                if (flags != null)
                {
                    events.add(new MetadataInspectionEvent(record, "Apple", "RunTimeFlags", translateRunTimeFlags(flags.intValue())));
                }
                if (value != null)
                {
                    events.add(new MetadataInspectionEvent(record, "Apple", "RunTimeValue", String.valueOf(value)));
                }
                if (timescale != null)
                {
                    events.add(new MetadataInspectionEvent(record, "Apple", "RunTimeScale", String.valueOf(timescale)));
                }
                if (epoch != null)
                {
                    events.add(new MetadataInspectionEvent(record, "Apple", "RunTimeEpoch", String.valueOf(epoch)));
                }
                return;
            }
        }
        catch (Exception e)
        {
            // Fallback if structure varies
        }

        // Fallback generic event if plist parsing encounters an unexpected layout
        events.add(new MetadataInspectionEvent(record, "Apple", "RunTime", String.format("Binary Data (%d bytes)", block.length)));
    }

    private Long extractBplistIntegerKey2(byte[] block, String key)
    {
        byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
        for (int i = 0; i < block.length - keyBytes.length; i++)
        {
            boolean match = true;
            for (int j = 0; j < keyBytes.length; j++)
            {
                if (block[i + j] != keyBytes[j])
                {
                    match = false;
                    break;
                }
            }
            if (match)
            {
                // Look ahead past the key string for the integer object marker (typically 0x10, 0x12, 0x13 for 1, 4, or 8-byte ints)
                int valIdx = i + keyBytes.length;
                if (valIdx + 1 < block.length)
                {
                    int marker = block[valIdx] & 0xFF;
                    // Binary plist integer markers: 0x10 (1 byte), 0x11 (2 bytes), 0x12 (4 bytes), 0x13 (8 bytes)
                    if (marker >= 0x10 && marker <= 0x13)
                    {
                        int bytesLen = 1 << (marker & 0x03);
                        if (valIdx + 1 + bytesLen <= block.length)
                        {
                            long val = 0;
                            for (int b = 0; b < bytesLen; b++)
                            {
                                val = (val << 8) | (block[valIdx + 1 + b] & 0xFF);
                            }
                            return val;
                        }
                    }
                }
            }
        }
        return null;
    }

    private String translateRunTimeFlags(int flags)
    {
        if (flags == 1) return "Valid";
        // Combine or return flag bitmask description matching ExifTool
        return "Valid (" + flags + ")";
    }
}