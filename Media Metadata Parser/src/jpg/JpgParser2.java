package jpg;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import com.adobe.internal.xmp.XMPException;
import common.AbstractImageParser;
import common.DigitalSignature;
import common.ImageRandomAccessReader;
import common.MetadataConstants;
import common.Utils;
import logger.LogFactory;
import tif.DirectoryIFD;
import tif.TifMetadata;
import tif.TifParser;
import xmp.XmpDirectory;
import xmp.XmpHandler;

/**
 * A parser for JPG image files designed to extract metadata from the APP segments, handling
 * multi-segment metadata, specifically ICC data, in addition to single-segment EXIF and XMP data.
 *
 * <p>
 * This parser adheres to the EXIF specification (version 2.32, CIPA DC-008-2019), which mandates
 * that all EXIF metadata must be contained within a single APP1 segment.
 * </p>
 *
 * <p>
 * For ICC profiles, the parser collects and concatenates all APP2 segments containing the
 * {@code ICC_PROFILE} identifier, adhering to the sequential structural rules defined in the ICC
 * specification. For {@code XMP} data, it extracts the primary baseline APP1 segment flagged with
 * the
 * {@code http://ns.adobe.com/xap/1.0/0} identifier.
 * </p>
 *
 * @author Trevor Maggs
 * @version 1.9
 * @since 22 May 2026
 */
public class JpgParser2 extends AbstractImageParser<TifMetadata>
{
    private static final LogFactory LOGGER = LogFactory.getLogger(JpgParser2.class);
    private static final int PADDING_LIMIT = 64;

    public static final byte[] EXIF_IDENTIFIER = "Exif\0\0".getBytes(StandardCharsets.UTF_8);
    public static final byte[] ICC_IDENTIFIER = "ICC_PROFILE\0".getBytes(StandardCharsets.UTF_8);
    public static final byte[] XMP_IDENTIFIER = "http://ns.adobe.com/xap/1.0/0".getBytes(StandardCharsets.UTF_8);
    public static final byte[] EXT_XMP_IDENTIFIER = "http://ns.adobe.com/iim/xmp/extension/0".getBytes(StandardCharsets.UTF_8);

    private final TifMetadata metadata;
    private JpgSegmentData segmentData;
    private boolean dataLoaded = false;

    /**
     * An immutable data carrier for the raw byte arrays of the different metadata segments
     * found in a JPEG file, encapsulating the raw EXIF, XMP, and ICC data payloads.
     */
    private static class JpgSegmentData
    {
        private final byte[] exif;
        private final byte[] xmp;
        private final byte[] icc;

        private JpgSegmentData(byte[] exif, byte[] xmp, byte[] icc)
        {
            this.exif = exif;
            this.xmp = xmp;
            this.icc = icc;
        }

        private Optional<byte[]> getExif()
        {
            return Optional.ofNullable(exif);
        }

        private Optional<byte[]> getXmp()
        {
            return Optional.ofNullable(xmp);
        }

        private Optional<byte[]> getIcc()
        {
            return Optional.ofNullable(icc);
        }
    }

    public JpgParser2(String file) throws IOException
    {
        this(Paths.get(file));
    }

    public JpgParser2(Path fpath) throws IOException
    {
        super(fpath);

        String ext = Utils.getFileExtension(getImageFile());

        if (!ext.equalsIgnoreCase("jpg") && !ext.equalsIgnoreCase("jpeg"))
        {
            LOGGER.warn(formatExtensionErrorMessage());
        }

        this.metadata = new TifMetadata();
    }

    @Override
    public void readMetadata() throws IOException
    {
        if (!dataLoaded)
        {
            validateFileState();

            try (ImageRandomAccessReader reader = new ImageRandomAccessReader(getImageFile()))
            {
                segmentData = readMetadataSegments(reader);
            }

            if (segmentData.getExif().isPresent())
            {
                try
                {
                    TifMetadata parsedTif = TifParser.parseTiffMetadataFromBytes(segmentData.getExif().get());
                    metadata.setByteOrder(parsedTif.getByteOrder());

                    for (DirectoryIFD ifd : parsedTif)
                    {
                        metadata.addDirectory(ifd);
                    }

                    if (parsedTif.hasXmpData())
                    {
                        metadata.addXmpDirectory(parsedTif.getXmpDirectory());
                    }
                }
                catch (IOException exc)
                {
                    LOGGER.error("Corrupt or invalid EXIF payload encountered inside APP1 layout structure", exc);
                }
            }

            if (!metadata.hasXmpData() && segmentData.getXmp().isPresent())
            {
                try
                {
                    XmpDirectory xmpDir = XmpHandler.addXmpDirectory(segmentData.getXmp().get());
                    metadata.addXmpDirectory(xmpDir);
                }
                catch (XMPException exc)
                {
                    LOGGER.error("Unable to parse standalone XMP payload in file [" + getImageFile() + "]", exc);
                }
            }

            dataLoaded = true;
        }
    }

    @Override
    public TifMetadata getMetadata()
    {
        if (!dataLoaded)
        {
            try
            {
                readMetadata();
            }
            catch (IOException exc)
            {
                throw new UncheckedIOException("Unable to parse file [" + getImageFile() + "] due to an error downstream", exc);
            }
        }
        return metadata;
    }

    @Override
    public DigitalSignature getImageFormat()
    {
        return DigitalSignature.JPG;
    }

    @Override
    public String formatDiagnosticString() throws IOException
    {
        if (!dataLoaded)
        {
            readMetadata();
        }

        TifMetadata meta = getMetadata();
        StringBuilder sb = new StringBuilder();

        try
        {
            sb.append("\t\t\tJPG Metadata Summary").append(System.lineSeparator()).append(System.lineSeparator());
            sb.append(super.formatDiagnosticString());

            if (meta.hasMetadata())
            {
                for (DirectoryIFD ifd : meta)
                {
                    sb.append(ifd);
                }
            }
            else
            {
                sb.append("No EXIF metadata found").append(System.lineSeparator());
            }

            sb.append(System.lineSeparator()).append(MetadataConstants.DIVIDER).append(System.lineSeparator());

            if (meta.hasXmpData())
            {
                sb.append(meta.getXmpDirectory());
            }
            else
            {
                sb.append("No XMP metadata found").append(System.lineSeparator());
            }

            sb.append(MetadataConstants.DIVIDER).append(System.lineSeparator());

            if (segmentData.getIcc().isPresent())
            {
                sb.append(String.format("Parser has concatenated all ICC segments, totalling [%d] bytes", segmentData.getIcc().get().length)).append(System.lineSeparator());
            }
            else
            {
                sb.append("No ICC Profile found").append(System.lineSeparator());
            }

            sb.append(MetadataConstants.DIVIDER).append(System.lineSeparator());
        }
        catch (Exception exc)
        {
            LOGGER.error("Diagnostics failed for file [" + getImageFile() + "]", exc);
            sb.append(exc.getMessage()).append(System.lineSeparator());
        }

        return sb.toString();
    }

    public static byte[] stripExifPreamble(byte[] data)
    {
        if (data.length >= EXIF_IDENTIFIER.length && Arrays.equals(Arrays.copyOf(data, EXIF_IDENTIFIER.length), EXIF_IDENTIFIER))
        {
            return Arrays.copyOfRange(data, EXIF_IDENTIFIER.length, data.length);
        }
        return data;
    }

    static JpgSegmentConstants fetchNextSegment(ImageRandomAccessReader reader) throws IOException
    {
        try
        {
            int fillCount = 0;

            while (true)
            {
                int marker = reader.readUnsignedByte();

                if (marker != 0xFF)
                {
                    continue;
                }

                int flag = reader.readUnsignedByte();

                while (flag == 0xFF)
                {
                    fillCount++;

                    if (fillCount > PADDING_LIMIT)
                    {
                        LOGGER.warn("Excessive 0xFF padding bytes detected, possible file corruption");
                        return null;
                    }

                    flag = reader.readUnsignedByte();
                }

                return JpgSegmentConstants.fromBytes(marker, flag);
            }
        }
        catch (EOFException eof)
        {
            return null;
        }
    }

    private JpgSegmentData readMetadataSegments(ImageRandomAccessReader reader) throws IOException
    {
        byte[] exifSegment = null;
        byte[] xmpSegment = null;
        List<byte[]> extXmpSegments = new ArrayList<>();
        List<byte[]> iccSegments = new ArrayList<>();

        while (reader.getCurrentPosition() < reader.length())
        {
            JpgSegmentConstants segment = fetchNextSegment(reader);

            if (segment == null || segment == JpgSegmentConstants.END_OF_IMAGE || segment == JpgSegmentConstants.START_OF_STREAM)
            {
                break;
            }

            if (segment.hasLengthField())
            {
                int length = reader.readUnsignedShort() - 2;

                if (length < 0 || (reader.getCurrentPosition() + length) > reader.length())
                {
                    LOGGER.error(String.format("Malformed segment size [%d] overflows data limit.", length + 2));
                    break;
                }

                if (length == 0)
                {
                    continue;
                }

                if (segment == JpgSegmentConstants.APP1_SEGMENT || segment == JpgSegmentConstants.APP2_SEGMENT)
                {
                    byte[] payload = reader.readBytes(length);

                    if (segment == JpgSegmentConstants.APP1_SEGMENT)
                    {
                        // Only one EXIF segment is allowed
                        if (exifSegment == null)
                        {
                            byte[] strippedPayload = JpgParser2.stripExifPreamble(payload);

                            if (strippedPayload.length < payload.length)
                            {
                                exifSegment = strippedPayload;
                                continue;
                            }
                        }

                        // Only one baseline XMP segment is allowed
                        if (payload.length >= XMP_IDENTIFIER.length && Arrays.equals(Arrays.copyOfRange(payload, 0, XMP_IDENTIFIER.length), XMP_IDENTIFIER))
                        {
                            if (xmpSegment == null)
                            {
                                xmpSegment = Arrays.copyOfRange(payload, XMP_IDENTIFIER.length, payload.length);
                            }

                            continue;
                        }

                        // Handle Extended XMP marker payload boundaries cleanly
                        if (payload.length >= EXT_XMP_IDENTIFIER.length && Arrays.equals(Arrays.copyOfRange(payload, 0, EXT_XMP_IDENTIFIER.length), EXT_XMP_IDENTIFIER))
                        {
                            extXmpSegments.add(Arrays.copyOfRange(payload, EXT_XMP_IDENTIFIER.length, payload.length));
                            continue;
                        }
                    }

                    else if (segment == JpgSegmentConstants.APP2_SEGMENT)
                    {
                        if (payload.length >= ICC_IDENTIFIER.length && Arrays.equals(Arrays.copyOfRange(payload, 0, ICC_IDENTIFIER.length), ICC_IDENTIFIER))
                        {
                            iccSegments.add(payload);
                        }
                    }
                }

                else
                {
                    reader.skip(length);
                }
            }
        }

        // return new JpgSegmentData(exifSegment, xmpSegment, reconstructIccSegments(iccSegments));

        // Pass the standard XMP payload into the extended XMP reassembler
        byte[] fullXmpPayload = reconstructExtendedXmpSegments(xmpSegment, extXmpSegments);

        return new JpgSegmentData(exifSegment, fullXmpPayload, reconstructIccSegments(iccSegments));
    }

    /**
     * Reassembles extended XMP metadata fragments into a single, unified XMP byte array.
     *
     * @param standardXmp
     *        the raw parsed baseline XMP payload (already stripped of its preamble)
     * @param segments
     *        the raw APP1 segments matched to EXT_XMP_IDENTIFIER
     * @return a consolidated byte array containing the stitched XML payload, or the standardXmp if
     *         no extensions exist.
     */
    private byte[] reconstructExtendedXmpSegments(byte[] standardXmp, List<byte[]> segments)
    {
        if (segments.isEmpty() || standardXmp == null)
        {
            return standardXmp; 
        }

        String targetGuid = null;
        
        // 1. Extract the MD5 GUID string target from the Standard XMP baseline text
        String standardXmlText = new String(standardXmp, StandardCharsets.UTF_8);

        // Simple, fast check for xmpNote:HasExtendedXMP="GUID" attribute
        int guidAttrIdx = standardXmlText.indexOf("xmpNote:HasExtendedXMP=");

        if (guidAttrIdx != -1)
        {
            int startQuote = standardXmlText.indexOf('"', guidAttrIdx);

            if (startQuote != -1)
            {
                int endQuote = standardXmlText.indexOf('"', startQuote + 1);

                if (endQuote != -1)
                {
                    targetGuid = standardXmlText.substring(startQuote + 1, endQuote);
                }
            }
        }

        if (targetGuid == null)
        {
            LOGGER.warn("Extended XMP segments detected, but no xmpNote:HasExtendedXMP GUID found in Standard XMP anchor");
            return standardXmp;
        }

        int totalLength = -1;
        List<XmpChunk> validChunks = new ArrayList<>();

        for (byte[] fullPayload : segments)
        {
            // Note: Your main loop passes 'payload' minus EXT_XMP_IDENTIFIER length.
            // So fullPayload here starts directly with the 32-byte ASCII GUID!
            if (fullPayload.length < (32 + 4 + 4))
            {
                LOGGER.error("Malformed Extended XMP segment: too short to contain tracking block headers.");
                continue;
            }

            // Extract the 32-byte GUID string from this chunk
            String chunkGuid = new String(fullPayload, 0, 32, StandardCharsets.UTF_8);

            if (!chunkGuid.equalsIgnoreCase(targetGuid))
            {
                // Skip chunks belonging to secondary or alternate historical data pipelines
                continue;
            }

            // Parse Big-Endian 32-bit unsigned integers safely into long/int
            long chunkTotalLen = ((fullPayload[32] & 0xFFL) << 24) |
                    ((fullPayload[33] & 0xFFL) << 16) |
                    ((fullPayload[34] & 0xFFL) << 8) |
                    (fullPayload[35] & 0xFFL);

            long chunkOffset = ((fullPayload[36] & 0xFFL) << 24) |
                    ((fullPayload[37] & 0xFFL) << 16) |
                    ((fullPayload[38] & 0xFFL) << 8) |
                    (fullPayload[39] & 0xFFL);

            if (totalLength == -1)
            {
                totalLength = (int) chunkTotalLen;
            }

            else if (totalLength != (int) chunkTotalLen)
            {
                LOGGER.error("Mismatched total extended length values across sibling Extended XMP slices.");
                return standardXmp;
            }

            byte[] actualDataBytes = Arrays.copyOfRange(fullPayload, 32 + 4 + 4, fullPayload.length);
            validChunks.add(new XmpChunk((int) chunkOffset, actualDataBytes));
        }

        if (validChunks.isEmpty() || totalLength <= 0)
        {
            return standardXmp;
        }

        // 3. Sort chunks explicitly by their absolute internal byte offset position
        validChunks.sort(Comparator.comparingInt(c -> c.offset));

        // 4. Stitch raw chunks sequentially into our target block destination array
        byte[] extendedBuffer = new byte[totalLength];
        int bytesWritten = 0;

        for (XmpChunk chunk : validChunks)
        {
            if (chunk.offset + chunk.data.length > totalLength)
            {
                LOGGER.error("Extended XMP segment offset slice overflows defined total buffer length metrics.");
                return standardXmp;
            }

            System.arraycopy(chunk.data, 0, extendedBuffer, chunk.offset, chunk.data.length);
            bytesWritten += chunk.data.length;
        }

        if (bytesWritten != totalLength)
        {
            LOGGER.warn(String.format("Extended XMP assembly gap detected. Expected %d bytes, compiled %d bytes.", totalLength, bytesWritten));
            return standardXmp;
        }

        // 5. Concatenate standard XMP and fully resolved Extended XMP together
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream())
        {
            baos.write(standardXmp);
            baos.write(extendedBuffer);

            return baos.toByteArray();
        }

        catch (IOException exc)
        {
            LOGGER.error("Failed final pipeline composition step merging Standard and Extended XMP blocks", exc);
            return standardXmp;
        }
    }

    /** Simple structural helper to facilitate offset-sorting logic. */
    private static class XmpChunk
    {
        private final int offset;
        private final byte[] data;

        private XmpChunk(int offset, byte[] data)
        {
            this.offset = offset;
            this.data = data;
        }
    }

    private byte[] reconstructIccSegments(List<byte[]> segments)
    {
        if (!segments.isEmpty())
        {
            int headerLength = ICC_IDENTIFIER.length + 2;

            for (byte[] seg : segments)
            {
                if (seg.length < headerLength)
                {
                    LOGGER.error("One or more ICC segments are too short to contain header metrics.");
                    return null;
                }
            }

            // Modern Java 8+ Lambda sorting definition
            segments.sort(Comparator.comparingInt(s -> s[ICC_IDENTIFIER.length] & 0xFF));

            int totalCount = segments.get(0)[ICC_IDENTIFIER.length + 1] & 0xFF;

            if (totalCount == 0 || totalCount != segments.size())
            {
                LOGGER.warn(String.format("ICC segment count mismatch. Expected [%d], found [%d].", totalCount, segments.size()));
                return null;
            }

            // Enforce sequence continuity
            for (int i = 0; i < segments.size(); i++)
            {
                int currentSequence = segments.get(i)[ICC_IDENTIFIER.length] & 0xFF;

                if (currentSequence != (i + 1))
                {
                    LOGGER.error(String.format("Corrupted sequence order. Segment index %d points to rank %d", i + 1, currentSequence));
                    return null;
                }
            }

            try (ByteArrayOutputStream baos = new ByteArrayOutputStream())
            {
                for (byte[] seg : segments)
                {
                    baos.write(Arrays.copyOfRange(seg, headerLength, seg.length));
                }

                LOGGER.debug(String.format("Successfully reconstructed ICC profile from [%d] segment(s)", totalCount));
                return baos.toByteArray();
            }

            catch (IOException exc)
            {
                LOGGER.error("Failed to concatenate ICC segments", exc);
            }
        }

        return null;
    }
}