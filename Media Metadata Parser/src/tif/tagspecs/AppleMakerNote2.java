package tif.tagspecs;

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
import tif.RationalNumber;
import tif.TifFieldType;
import tif.CameraMarkerNote;

public class AppleMakerNote2 extends CameraMarkerNote
{
    private static final byte[] APPLE_IOS_IDENTIFIER = "Apple iOS\0".getBytes(StandardCharsets.UTF_8);
    private static final int PREAMBLE_LENGTH = 12; // 10 bytes signature + 2 bytes version/padding

    @Override
    public boolean supportsMake(String cameraMake)
    {
        return (cameraMake != null && cameraMake.toLowerCase().contains("apple"));
    }

    // Skip the 10-byte signature, 2-byte version padding, and 2-byte 'MM' marker (reaches
    // offset 0x0E)
    // Apple MakerNotes enforce Big-Endian byte ordering
    // Read the Entry Count at offset 0x0E
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

        try (ByteArrayReader reader = new ByteArrayReader(rawBytes))
        {
            reader.setByteOrder(ByteOrder.BIG_ENDIAN);
            reader.seek(headerLength);

            int entryCount = reader.readUnsignedShort();

            // Iterate through the 12-byte IFD entries starting at offset 0x10
            for (int i = 0; i < entryCount; i++)
            {
                if (reader.remaining() >= 12)
                {
                    String str = null;
                    int tagId = reader.readUnsignedShort();
                    TifFieldType fieldType = TifFieldType.getTiffType(reader.readUnsignedShort());
                    long count = reader.readUnsignedInteger();
                    byte[] valueBytes = reader.readBytes(4);
                    long valueOrOffset = ByteValueConverter.toUnsignedInteger(valueBytes, reader.getByteOrder());
                    String tagName = lookupTagName(tagId);

                    if (tagName != null)
                    {
                        str = resolveTagValue(reader, rawBytes, fieldType, count, valueOrOffset);

                        if (str != null && !str.isEmpty())
                        {
                            events.add(new MetadataInspectionEvent(record, "Apple", tagName, str));
                        }
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

    private String resolveTagValue(ByteArrayReader reader, byte[] rawBytes, TifFieldType fieldType, long count, long valueOrOffset)
    {
        long totalBytes = count * fieldType.getFieldSize();

        /*
         * If totalBytes is within the first 4 bytes, the data
         * is stored inline inside the valueOrOffset field.
         */
        if (totalBytes <= IFDHandler.ENTRY_MAX_VALUE_LENGTH)
        {
            return String.valueOf(valueOrOffset);
        }

        /*
         * In this case, valueOrOffset is an absolute offset relative
         * to 0x00 ("Apple iOS\0") outside the current 12 byte block.
         */
        else if (valueOrOffset >= 0 && (valueOrOffset + totalBytes) <= rawBytes.length)
        {
            try
            {
                byte[] block = reader.peek(valueOrOffset, (int) totalBytes);

                if (fieldType == TifFieldType.TYPE_ASCII)
                {
                    return new String(ByteValueConverter.readNullTerminatedString(block, 0, StandardCharsets.UTF_8));
                }

                else if (fieldType == TifFieldType.TYPE_RATIONAL_S)
                {
                    RationalNumber[] arr = ByteValueConverter.toRationalArray(block, reader.getByteOrder(), RationalNumber.DataType.SIGNED);
                    System.out.printf("LOOK: %s\n", Arrays.toString(arr));
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

        else
        {
            // TODO: consider returning some error code?
        }

        return null;
    }

    private String lookupTagName(int tagId)
    {
        switch (tagId)
        {
            case 0x0001:
                return "MakerNoteVersion";

            case 0x0002:
                return "AEMatrix";

            case 0x0003:
                return "RunTime";

            case 0x0004:
                return "AEStable";

            case 0x0005:
                return "AETarget";

            case 0x0006:
                return "AEAverage";

            case 0x0007:
                return "AFStable";

            case 0x0008:
                return "AccelerationVector";

            case 0x000A:
                return "HDRImageType";

            case 0x000B:
                return "BurstUUID";

            case 0x000C:
                return "FocusDistanceRange";

            case 0x000F:
                return "OISMode";

            case 0x0011:
                return "ContentIdentifier";

            case 0x0014:
                return "ImageCaptureType";

            case 0x0015:
                return "ImageUniqueID";

            case 0x0017:
                return "LivePhotoVideoIndex";

            case 0x0019:
                return "ImageProcessingFlags";

            case 0x001A:
                return "QualityHint";

            case 0x001D:
                return "LuminanceNoiseAmplitude";

            case 0x001F:
                return "PhotosAppFeatureFlags";

            case 0x0020:
                return "ImageCaptureRequestID";

            case 0x0021:
                return "HDRHeadroom";

            case 0x0023:
                return "AFPerformance";

            case 0x0025:
                return "SceneFlags";

            case 0x0026:
                return "SignalToNoiseRatioType";

            case 0x0027:
                return "SignalToNoiseRatio";

            case 0x002B:
                return "PhotoIdentifier";

            case 0x002D:
                return "ColorTemperature";

            case 0x002E:
                return "CameraType";

            case 0x002F:
                return "FocusPosition";

            case 0x0030:
                return "HDRGain";

            case 0x0038:
                return "AFMeasuredDepth";

            case 0x003D:
                return "AFConfidence";

            case 0x003E:
                return "ColorCorrectionMatrix";

            case 0x003F:
                return "GreenGhostMitigationStatus";

            case 0x0040:
                return "SemanticStyle";

            case 0x0041:
                return "SemanticStyleRenderingVer";

            case 0x0042:
                return "SemanticStylePreset";

            default:
                // return String.format("Apple_0x%04X", tagId);
                return null;
        }
    }
}