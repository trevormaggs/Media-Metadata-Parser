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

/**
 * Decodes Apple iOS and macOS proprietary MakerNote binary metadata structures embedded within
 * TIFF/DNG image containers, achieving exact functional parity with Phil Harvey's ExifTool. Apple
 * does not officially document the MakerNote binary specification; however, the structures
 * implemented herein are reverse-engineered and aligned with Phil Harvey's ExifTool Perl
 * implementation.
 * 
 * 
 * <p>
 * <strong>Implementation References</strong>
 * </p>
 * 
 * <p>
 * The implementation is based on reverse-engineering Apple's undocumented MakerNote structures,
 * using Phil Harvey's ExifTool as the primary reference. The Apple tag definitions are derived from
 * the <a href="https://exiftool.org/TagNames/Apple.html">ExifTool Apple Tags documentation</a>.
 * </p>
 * 
 * <p>
 * Selected decoding algorithms have been adapted from Phil Harvey's Perl source code in ExifTool's
 * {@code Apple.pm} and associated modules, and reimplemented in Java using the existing TIFF and
 * binary-processing infrastructure of this project.
 * </p>
 * 
 * <p>
 * ExifTool is also used as a reference implementation for validating extracted metadata values
 * against actual image files.
 * </p>
 * 
 * <p>
 * <strong>Apple MakerNote Binary Specification</strong>
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
 * <td>Preamble signature string {@code "Apple iOS\0"} (header) identifying the MakerNote
 * payload.</td>
 * </tr>
 * <tr>
 * <td>Bytes 10–11</td>
 * <td>2 bytes</td>
 * <td>Version padding and alignment bytes.</td>
 * </tr>
 * <tr>
 * <td>Bytes 12–13</td>
 * <td>2 bytes</td>
 * <td>Endianness marker {@code 'MM'}. Always big-Endian order (Motorola byte ordering).</td>
 * </tr>
 * <tr>
 * <td>Offset 0x0E (14)</td>
 * <td>2 bytes</td>
 * <td>Entry Count read as unsigned short specifying the total number of MakerNote entries
 * contained.</td>
 * </tr>
 * <tr>
 * <td>Offset 0x10 onward</td>
 * <td>Variable</td>
 * <td>Flat 12-byte IFD entry blocks structured as:
 * <ul>
 * <li><strong>Tag ID:</strong> 2-byte unsigned short identifier.</li>
 * <li><strong>Field Type:</strong> 2-byte TIFF field type descriptor ({@link TifFieldType}).</li>
 * <li><strong>Count:</strong> 4-byte unsigned integer element count.</li>
 * <li><strong>Value/Offset:</strong> 4-byte field containing either inline data (if &le; 4 bytes)
 * or an absolute byte offset relative to the start of the MakerNote buffer ({@code 0x00}).</li>
 * </ul>
 * </td>
 * </tr>
 * </tbody>
 * </table>
 * 
 * <p>
 * <strong>Binary Property List ({@code bplist00}) Architecture Reference</strong>
 * </p>
 * <p>
 * Complex telemetry tags like {@code RunTime} (0x0003) and {@code AEMatrix} (0x0002) serialize data
 * using Apple's binary property list format ({@code bplist00}).
 * </p>
 * 
 * <ul>
 * <li><strong>Macro Structural Layout:</strong>
 * 
 * <pre>
 *     +-----------------------+
 *     | Header (8 bytes)      | -&gt; "bplist00" magic signature
 *     +-----------------------+
 *     | Object Table          | -&gt; Raw data markers, dictionaries, and numeric values
 *     +-----------------------+
 *     | Offset Table          | -&gt; Fixed-size byte offsets pointing to objects in the table
 *     +-----------------------+
 *     | Trailer (32 bytes)    | -&gt; Metadata block fixed at the very end of the array
 *     +-----------------------+
 * </pre>
 * 
 * </li>
 * <li><strong>Detailed Trailer Structure (32 bytes from the end of the block):</strong>
 * <p>
 * <table border="1" cellpadding="4" cellspacing="0">
 * <caption>bplist00 Trailer Layout</caption>
 * <thead>
 * <tr>
 * <th>Offset from Trailer Start</th>
 * <th>Size</th>
 * <th>Field Name</th>
 * <th>Description</th>
 * </tr>
 * </thead>
 * <tbody>
 * <tr>
 * <td>Bytes 0–5</td>
 * <td>6 bytes</td>
 * <td>Null Padding / Marker</td>
 * <td>Reserved / alignment bytes</td>
 * </tr>
 * <tr>
 * <td>Byte 6</td>
 * <td>1 byte</td>
 * <td>{@code offsetIntSize}</td>
 * <td>Size in bytes of each entry in the offset table (1, 2, or 4)</td>
 * </tr>
 * <tr>
 * <td>Byte 7</td>
 * <td>1 byte</td>
 * <td>{@code objectRefSize}</td>
 * <td>Size in bytes of object references inside dictionaries/arrays</td>
 * </tr>
 * <tr>
 * <td>Bytes 8–15</td>
 * <td>8 bytes</td>
 * <td>{@code numObjects}</td>
 * <td>Total count of objects in the object table</td>
 * </tr>
 * <tr>
 * <td>Bytes 16–23</td>
 * <td>8 bytes</td>
 * <td>{@code topLevelObject}</td>
 * <td>Index of the root dictionary/object</td>
 * </tr>
 * <tr>
 * <td>Bytes 24–31</td>
 * <td>8 bytes</td>
 * <td>{@code offsetTableOffset}</td>
 * <td>Absolute byte offset where the offset table begins</td>
 * </tr>
 * </tbody>
 * </table>
 * </p>
 * </li>
 * <li><strong>Object Table Marker Ranges:</strong>
 * <p>
 * <table border="1" cellpadding="4" cellspacing="0">
 * <caption>bplist00 Marker Ranges</caption>
 * <thead>
 * <tr>
 * <th>Marker Range</th>
 * <th>Data Type</th>
 * <th>Description</th>
 * </tr>
 * </thead>
 * <tbody>
 * <tr>
 * <td>0x00</td>
 * <td>Null / Booleans</td>
 * <td>0x00 = Null, 0x08 = False, 0x09 = True</td>
 * </tr>
 * <tr>
 * <td>0x10 – 0x13</td>
 * <td>Integers</td>
 * <td>1, 2, 4, or 8-byte integers (targeted by parser logic)</td>
 * </tr>
 * <tr>
 * <td>0x20 – 0x23</td>
 * <td>Reals / Floats</td>
 * <td>4-byte or 8-byte floating-point numbers</td>
 * </tr>
 * <tr>
 * <td>0x40 – 0x4F</td>
 * <td>Data / Byte Buffers</td>
 * <td>Raw binary data blobs (e.g., thumbnail bytes)</td>
 * </tr>
 * <tr>
 * <td>0x50 – 0x5F</td>
 * <td>ASCII Strings</td>
 * <td>Standard text strings</td>
 * </tr>
 * <tr>
 * <td>0x60 – 0x6F</td>
 * <td>Unicode Strings</td>
 * <td>UTF-16 text strings</td>
 * </tr>
 * <tr>
 * <td>0xA0 – 0xAF</td>
 * <td>Arrays</td>
 * <td>Lists of object references</td>
 * </tr>
 * <tr>
 * <td>0xD0 – 0xDF</td>
 * <td>Dictionaries</td>
 * <td>Key-value mapping structures</td>
 * </tr>
 * </tbody>
 * </table>
 * </p>
 * </li>
 * </ul>
 * 
 * @author Trevor Maggs
 * @version 1.0
 * @since 27 November 2026
 */
public class AppleMakerNote extends CameraMarkerNote
{
    private static final byte[] APPLE_IOS_IDENTIFIER = "Apple iOS\0".getBytes(StandardCharsets.UTF_8);
    private static final int PREAMBLE_LENGTH = 12; // 10 bytes signature + 2 bytes version/padding

    /**
     * Determines whether this metadata handler supports the specified camera manufacturer make.
     * 
     * @param cameraMake
     *        the camera manufacturer make string extracted from the image container
     * @return {@code true} if the make contains "apple" (case-insensitive), otherwise {@code false}
     */
    @Override
    public boolean supportsMake(String cameraMake)
    {
        return (cameraMake != null && cameraMake.toLowerCase().contains("apple"));
    }

    /**
     * Parses Apple iOS MakerNote proprietary binary structure from a DNG/TIFF file container.
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

                        else if (fieldType == TifFieldType.TYPE_RATIONAL_S)
                        {
                            rawValue = ByteValueConverter.toRationalArray(block, reader.getByteOrder(), RationalNumber.DataType.SIGNED);
                        }

                        else if (fieldType == TifFieldType.TYPE_LONG_S)
                        {
                            rawValue = ByteValueConverter.toIntegerArray(block, reader.getByteOrder());
                        }

                        else if (fieldType == TifFieldType.TYPE_UNDEFINED)
                        {
                            rawValue = block;
                        }
                    }

                    /*
                     * Because tag 0x0003 (RunTime bplist) requires multiple results for emitting
                     * events, therefore its efficient to handle here.
                     */
                    if (tagId == 0x0003 && rawValue instanceof byte[])
                    {
                        processBinaryPropertyList((byte[]) rawValue, record, events);
                        continue;
                    }

                    String[] infoPair = translate(tagId, rawValue);

                    if (infoPair != null && infoPair.length >= 2 && infoPair[0] != null)
                    {
                        events.add(new MetadataInspectionEvent(record, "Apple", infoPair[0], infoPair[1]));
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

    /**
     * Parses an Apple binary property list ({@code bplist00}) byte block, useful for decoding the
     * RunTime MakerNote tag (0x0003), extracting the object offset table and scanning integer
     * objects to identify RunTime components.
     *
     * <p>
     * The decoding approach is based on the binary property list processing implemented in Phil
     * Harvey's ExifTool.
     * </p>
     *
     * @param block
     *        the raw binary property list byte array
     * @param record
     *        the target media record associated with the inspection batch
     * @param events
     *        the completed list of metadata inspection events to append to
     */
    private void processBinaryPropertyList(byte[] block, MediaRecord record, List<MetadataInspectionEvent> events)
    {
        final String bplist = "bplist00";
        long epoch = -1, flags = -1, timescale = -1, value = -1;

        if (block != null && block.length > 32 && new String(block, 0, bplist.length(), StandardCharsets.UTF_8).equals(bplist))
        {
            int trailerStart = block.length - 32;
            long offsetTableOffset = convertToLong(block, trailerStart + 24, 8);
            long numObjects = convertToLong(block, trailerStart + 8, 8);
            int offsetIntSize = block[trailerStart + 6] & 0xFF;
            long[] objectOffsets = new long[(int) numObjects];

            for (int i = 0; i < numObjects; i++)
            {
                objectOffsets[i] = convertToLong(block, (int) offsetTableOffset + (i * offsetIntSize), offsetIntSize);
            }

            // Scan object table for integers
            for (long offset : objectOffsets)
            {
                int pos = (int) offset;

                if (pos < block.length)
                {
                    // Integer types: 0x10 (1 byte), 0x11 (2 bytes), 0x12 (4 bytes), 0x13 (8 bytes)
                    int marker = block[pos] & 0xFF;
                    int markerType = marker & 0xF0;

                    // 0x10 indicates Integer values only
                    if (markerType == 0x10)
                    {
                        int byteCount = 1 << (marker & 0x03);

                        if (pos + 1 + byteCount <= block.length)
                        {
                            long val = convertToLong(block, pos + 1, byteCount);

                            if (val == 1000000000L)
                            {
                                timescale = val;
                            }

                            else if (val == 0 && epoch == -1)
                            {
                                epoch = val;
                            }

                            else if ((val == 0 || val == 1) && flags == -1 && timescale != -1)
                            {
                                flags = val;
                            }

                            else if (val > 10000)
                            {
                                value = val;
                            }
                        }
                    }
                }
            }

            long valNum = (value != -1 ? value : 0L);
            long ts = (timescale != -1 ? timescale : 1000000000L);
            long flagNum = (flags != -1 ? flags : 1L);
            String flagDesc = translateRunTimeFlags((int) flagNum);
            long epochNum = (epoch != -1 ? epoch : 0L);
            double seconds = (double) valNum / ts;

            events.add(new MetadataInspectionEvent(record, "Apple", "RunTimeValue", valNum));
            events.add(new MetadataInspectionEvent(record, "Apple", "RunTimeScale", ts));
            events.add(new MetadataInspectionEvent(record, "Apple", "RunTimeFlags", flagDesc));
            events.add(new MetadataInspectionEvent(record, "Apple", "RunTimeEpoch", epochNum));
            events.add(new MetadataInspectionEvent(record, "Apple", "RunTime", String.format(Locale.ROOT, "%.3fs", seconds)));

            return;
        }

        events.add(new MetadataInspectionEvent(record, "Apple", "RunTime", String.format("Binary Data (%d bytes)", block.length)));
    }

    /**
     * Translates a numeric RunTime flag bitmask into a comma-separated description of active states
     * (e.g., Valid, Rounded, Infinity).
     * 
     * @param flags
     *        the raw integer flag bitmask
     * @return a human-readable description of the active flag states
     */
    private String translateRunTimeFlags(int flags)
    {
        if (flags != 0)
        {
            List<String> flagName = new ArrayList<>();

            if ((flags & 0x01) != 0)
            {
                flagName.add("Valid");
            }

            if ((flags & 0x02) != 0)
            {
                flagName.add("Rounded");
            }

            if ((flags & 0x04) != 0)
            {
                flagName.add("+Infinity");
            }

            if ((flags & 0x08) != 0)
            {
                flagName.add("-Infinity");
            }

            if ((flags & 0x10) != 0)
            {
                flagName.add("Indefinite");
            }

            return (flagName.isEmpty() ? "Unknown (" + flags + ")" : String.join(", ", flagName));
        }

        return "Not Valid";
    }

    /**
     * Translates a given MakerNote tag ID and its raw unpacked value into a standardized metadata
     * property name and formatted string value pair.
     * 
     * @param tagId
     *        the 16-bit unsigned TIFF tag identifier
     * @param rawData
     *        the raw extracted object data for the tag
     * @return a two-element String array containing {@code [tagName, formattedValue]}, with a
     *         {@code null} tag name for unsupported tags
     */
    private String[] translate(int tagId, Object rawData)
    {
        String tagName;

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
            break;

            case 0x0004:
                tagName = "AEStable";
                rawData = translateAStable(rawData);
            break;

            case 0x0005:
                tagName = "AETarget";
            break;

            case 0x0006:
                tagName = "AEAverage";
            break;

            case 0x0007:
                tagName = "AFStable";
                rawData = translateAStable(rawData);
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
                rawData = translateImageCaptureType(rawData);
            break;

            case 0x0015:
                tagName = "ImageUniqueID";
            break;

            case 0x0017:
                tagName = "LivePhotoVideoIndex";
            break;

            case 0x0019:
                tagName = "ImageProcessingFlags";
                rawData = translateImageProcessingFlags(rawData);
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
                rawData = translateAFPerformance(rawData);
            break;

            case 0x0025:
                tagName = "SceneFlags";
                rawData = translateSceneFlags(rawData);
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
                rawData = translateCameraType(rawData);
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
     * Translates auto-exposure or auto-focus stability numeric codes into "Yes" or "No" strings.
     * 
     * @param rawData
     *        the raw stability indicator object
     * @return "Yes" if the value equals 1, "No" otherwise (or string representation if non-numeric)
     */
    private String translateAStable(Object rawData)
    {
        String value = rawData.toString();

        if (rawData instanceof Number)
        {
            value = (((Number) rawData).longValue() == 1 ? "Yes" : "No");
        }

        return value;
    }

    /**
     * Translates numeric image capture type codes into descriptive capture mode labels
     * (e.g., ProRAW, Portrait, Photo).
     * 
     * @param rawData
     *        the raw image capture type numeric value
     * @return the descriptive capture type string, or the numeric value string if unrecognized
     */
    private String translateImageCaptureType(Object rawData)
    {
        String value = null;

        if (rawData instanceof Number)
        {
            int val = ((Number) rawData).intValue();

            switch (val)
            {
                case 1:
                    value = "ProRAW";
                break;

                case 2:
                    value = "Portrait";
                break;

                case 10:
                    value = "Photo";
                break;

                case 11:
                    value = "Manual Focus";
                break;

                case 12:
                    value = "Scene";
                break;

                default:
                    value = String.valueOf(val);
            }
        }

        return value;
    }

    /**
     * Unpacks a raw integer bitmask into a comma-separated list of active bit indices
     * enclosed in square brackets (e.g., {@code [1], [16]}), matching ExifTool behavior.
     * 
     * @param rawData
     *        the raw numeric bitmask value
     * @return a comma-separated string of bracketed active bit indices, or "0" if none set
     */
    private String translateImageProcessingFlags(Object rawData)
    {
        if (rawData instanceof Number)
        {
            long flags = ((Number) rawData).longValue();
            List<String> activeFlags = new ArrayList<>();

            // Loop through all 32 bit positions just like ExifTool's bitmask evaluator
            for (int i = 0; i < 32; i++)
            {
                if ((flags & (1L << i)) != 0)
                {
                    activeFlags.add("[" + i + "]");
                }
            }

            return activeFlags.isEmpty() ? "0" : String.join(", ", activeFlags);
        }

        return rawData != null ? rawData.toString() : null;
    }

    /**
     * Decodes the Apple AFPerformance value into its constituent subfields, following the algorithm
     * implemented in Phil Harvey's ExifTool.
     *
     * <p>
     * The implementation is based on the corresponding Perl code in ExifTool'<a href=
     * "https://github.com/exiftool/exiftool/blob/master/lib/Image/ExifTool/Apple.pm">github.com/exiftool/exiftool/blob/master/lib/Image/ExifTool/Apple.pm</a>
     * </p>
     * 
     * @param rawData
     *        the original data extracted from the MakerNote segment
     * @return a formatted string containing the unpacked subfield values,
     *         or the original value if it cannot be decoded
     */
    private Object translateAFPerformance(Object rawData)
    {
        if (rawData instanceof int[])
        {
            int[] arr = (int[]) rawData;

            if (arr.length >= 2)
            {
                int val1 = arr[0];
                int sub1 = (arr[1] >> 28) & 0x0F;
                int sub2 = arr[1] & 0x0FFFFFFF; // Aligned with ExifTool's 28-bit mask

                rawData = val1 + " " + sub1 + " " + sub2;
            }
        }

        return rawData;
    }

    /**
     * Translates SceneFlags bitmask values using 1-based indexing to match ExifTool's exact output
     * format.
     * 
     * @param rawData
     *        the raw numeric SceneFlags value
     * @return a comma-separated string of 1-based bracketed active bit indices
     */
    private String translateSceneFlags(Object rawData)
    {
        if (rawData instanceof Number)
        {
            long flags = ((Number) rawData).longValue();
            List<String> activeFlags = new ArrayList<>();

            // ExifTool evaluates SceneFlags using 1-based bit indexing (i + 1)
            for (int i = 0; i < 32; i++)
            {
                if ((flags & (1L << i)) != 0)
                {
                    activeFlags.add("[" + (i + 1) + "]");
                }
            }

            return activeFlags.isEmpty() ? "0" : String.join(", ", activeFlags);
        }

        return rawData != null ? rawData.toString() : null;
    }

    /**
     * Translates camera type integer codes into descriptive lens or position labels
     * (e.g., Back Wide, Front, Back Ultra Wide).
     * 
     * @param rawData
     *        the raw camera type numeric value
     * @return the descriptive camera position string, or the numeric value string if unrecognized
     */
    private String translateCameraType(Object rawData)
    {
        String value = null;

        if (rawData instanceof Number)
        {
            int val = ((Number) rawData).intValue();

            switch (val)
            {
                case 0:
                    return "Back Wide Angle";

                case 1:
                    return "Back Normal";

                case 6:
                    return "Front";

                default:
                    return String.valueOf(val);
            }
        }

        return value;
    }

    /**
     * Utility method to convert a specified byte slice from a source byte array into an unsigned
     * integer represented as a Java {@code long}.
     * 
     * @param src
     *        the source byte array
     * @param offset
     *        the starting byte offset within the source array
     * @param length
     *        the number of bytes to read (1, 2, 4, or 8)
     * @return the converted integer value as a {@code long}
     */
    private static long convertToLong(byte[] src, int offset, int length)
    {
        long result = 0;

        for (int i = 0; i < length; i++)
        {
            if ((offset + i) < src.length)
            {
                result = (result << 8) | (src[offset + i] & 0xFF);
            }
        }

        return result;
    }
}