package jpg;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.adobe.internal.xmp.XMPException;
import common.AbstractImageParser;
import common.ByteValueConverter;
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
 * multi-segment metadata, specifically ICC and XMP data, in addition to the single-segment EXIF
 * data.
 *
 * <p>
 * This parser adheres to the EXIF specification (version 2.32, CIPA DC-008-2019), which mandates
 * that all EXIF metadata must be contained within a single APP1 segment. This parser will search
 * for and process the first APP1 segment it finds that contains the {@code Exif} identifier.
 * </p>
 *
 * <p>
 * For ICC profiles, the parser collects and concatenates all APP2 segments containing the
 * {@code ICC_PROFILE} identifier, adhering to the sequential structural rules defined in the ICC
 * specification. For {@code XMP} data, it aggregates standard APP1 segments flagged with the
 * {@code http://ns.adobe.com/xap/1.0/\0} identifier.
 * </p>
 *
 * @author Trevor Maggs
 * @version 1.9
 * @since 22 May 2026
 */
public class JpgParser extends AbstractImageParser<TifMetadata>
{
    private static final LogFactory LOGGER = LogFactory.getLogger(JpgParser.class);
    private static final int EXTENDED_XMP_LENGTH = 40;
    private static final int PADDING_LIMIT = 64;
    public static final byte[] EXIF_IDENTIFIER = "Exif\0\0".getBytes(StandardCharsets.UTF_8);
    public static final byte[] ICC_IDENTIFIER = "ICC_PROFILE\0".getBytes(StandardCharsets.UTF_8);
    public static final byte[] XMP_IDENTIFIER = "http://ns.adobe.com/xap/1.0/\0".getBytes(StandardCharsets.UTF_8);
    public static final byte[] EXT_XMP_IDENTIFIER = "http://ns.adobe.com/iim/xmp/extension/\0".getBytes(StandardCharsets.UTF_8);
    private final TifMetadata metadata;
    private JpgSegmentData segmentData;
    private boolean dataLoaded;

    /**
     * A simple immutable data carrier for the raw byte arrays of the different metadata segments
     * found in a JPEG file, encapsulating the raw EXIF, ICC, and XMP data payloads.
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

    /**
     * Constructs a new instance from a file path string.
     *
     * @param file
     *        the path to the JPG file as a string
     *
     * @throws IOException
     *         if the file cannot be opened or read
     */
    public JpgParser(String file) throws IOException
    {
        this(Paths.get(file));
    }

    /**
     * Constructs a new instance with the specified file path.
     *
     * @param fpath
     *        the path to the JPG file as an encapsulated object
     *
     * @throws IOException
     *         if the file cannot be opened or read
     */
    public JpgParser(Path fpath) throws IOException
    {
        super(fpath);

        String ext = Utils.getFileExtension(getImageFile());

        if (!ext.equalsIgnoreCase("jpg") && !ext.equalsIgnoreCase("jpeg"))
        {
            LOGGER.warn(formatExtensionErrorMessage());
        }

        this.dataLoaded = false;
        this.metadata = new TifMetadata();
    }

    /**
     * Reads the JPEG file structure to extract and isolate metadata segment streams. This method
     * parses raw underlying APP markers, processes structural EXIF blocks, populates internal
     * metadata records, and reassembles fragmented ICC and XMP structures.
     *
     * @throws IOException
     *         if a file reading or parsing error occurs
     */
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

    /**
     * Retrieves the extracted metadata from the JPG image file. If the metadata has not been
     * explicitly loaded yet using {@link #readMetadata()}, it will trigger lazy loading.
     *
     * @return a {@link TifMetadata} container populated with all successfully extracted directory
     *         properties and XMP markers
     *
     * @throws UncheckedIOException
     *         if an unrecoverable structural issue occurs during lazy context instantiation
     */
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

    /**
     * Returns the detected {@code JPG} format.
     *
     * @return a {@link DigitalSignature} enum constant representing this image format
     */
    @Override
    public DigitalSignature getImageFormat()
    {
        return DigitalSignature.JPG;
    }

    /**
     * Generates a human-readable diagnostic string containing metadata details.
     *
     * @return a formatted string suitable for diagnostics, logging, or inspection
     *
     * @throws IOException
     *         if lower level filesystem attributes are inaccessible
     */
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

    /**
     * Removes the 6-byte {@code Exif\0\0} signature/header required by the JPEG standard before
     * passing the payload to a TIFF parser.
     *
     * @param data
     *        the raw EXIF chunk payload
     * @return the corrected EXIF data byte array
     */
    public static byte[] stripExifPreamble(byte[] data)
    {
        if (data.length >= EXIF_IDENTIFIER.length && Arrays.equals(Arrays.copyOf(data, EXIF_IDENTIFIER.length), EXIF_IDENTIFIER))
        {
            return Arrays.copyOfRange(data, EXIF_IDENTIFIER.length, data.length);
        }

        return data;
    }

    /**
     * Reads the next JPEG segment marker from the input stream. Note that this method has
     * package-private visibility.
     *
     * <p>
     * Markers are identified by a {@code 0xFF} byte followed by a non-zero flag. As per the
     * specification, any number of {@code 0xFF} fill bytes may precede the actual flag; this method
     * safely discards such padding.
     * </p>
     *
     * @param reader
     *        the input stream of the JPEG file, positioned at the current read cursor
     * @return a {@link JpgSegmentConstants} representing the detected marker, or {@code null} if an
     *         {@link EOFException} occurs or excessive padding suggests corruption
     *
     * @throws IOException
     *         if an I/O error occurs while reading from the stream
     */
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

    /**
     * Reads all supported metadata segments, including EXIF, ICC and XMP, if present, from the JPEG
     * file stream.
     *
     * @param reader
     *        the input JPEG stream
     * @return a JpgSegmentData record containing the byte arrays for any found segments
     *
     * @throws IOException
     *         if an I/O error occurs
     */
    private JpgSegmentData readMetadataSegments(ImageRandomAccessReader reader) throws IOException
    {
        byte[] exifSegment = null;
        byte[] standardXmp = null;
        List<byte[]> iccSegments = new ArrayList<>();
        List<byte[]> extendedXmpSegments = new ArrayList<>();

        while (reader.getCurrentPosition() < reader.length())
        {
            JpgSegmentConstants segment = fetchNextSegment(reader);

            /*
             * SOS (Start of Scan) marks the beginning of the compressed image data.
             * Usually, no metadata exists after this point except the EOI marker.
             */
            if (segment == null || segment == JpgSegmentConstants.END_OF_IMAGE || segment == JpgSegmentConstants.START_OF_STREAM)
            {
                break;
            }

            if (segment.hasLengthField())
            {
                int length = reader.readUnsignedShort() - 2;

                if (length < 0 || (reader.getCurrentPosition() + length) > reader.length())
                {
                    LOGGER.error(String.format("Malformed segment size [%d] overflows data limit", length + 2));
                    break;
                }

                // Length must be between 2 and 65535 bytes (unsigned short)
                if (length > 0)
                {
                    // Decision point: Read or Skip?
                    if (segment == JpgSegmentConstants.APP1_SEGMENT || segment == JpgSegmentConstants.APP2_SEGMENT)
                    {
                        byte[] payload = reader.readBytes(length);

                        if (segment == JpgSegmentConstants.APP1_SEGMENT)
                        {
                            if (exifSegment == null)
                            {
                                byte[] strippedPayload = JpgParser.stripExifPreamble(payload);

                                if (strippedPayload.length < payload.length)
                                {
                                    exifSegment = strippedPayload;
                                    continue;
                                }
                            }

                            // Match standard XMP
                            if (payload.length >= XMP_IDENTIFIER.length && Arrays.equals(Arrays.copyOfRange(payload, 0, XMP_IDENTIFIER.length), XMP_IDENTIFIER))
                            {
                                if (standardXmp == null)
                                {
                                    standardXmp = Arrays.copyOfRange(payload, XMP_IDENTIFIER.length, payload.length);
                                }

                                continue;
                            }

                            // Attach Extended XMP segments
                            if (payload.length >= EXT_XMP_IDENTIFIER.length && Arrays.equals(Arrays.copyOfRange(payload, 0, EXT_XMP_IDENTIFIER.length), EXT_XMP_IDENTIFIER))
                            {
                                extendedXmpSegments.add(Arrays.copyOfRange(payload, EXT_XMP_IDENTIFIER.length, payload.length));
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

                        else
                        {
                            LOGGER.debug(String.format("Unhandled segment [0xFF%02X] skipped. Length [%d]", segment.getFlag(), length));
                        }
                    }

                    else
                    {
                        reader.skip(length);
                    }
                }
            }
        }

        byte[] cleanedIcc = reconstructIccSegments(iccSegments);
        byte[] unifiedXmp = reconstructExtendedXmpSegments(standardXmp, extendedXmpSegments);

        return new JpgSegmentData(exifSegment, unifiedXmp, cleanedIcc);
    }

    /**
     * Reconstructs split Extended XMP metadata fragments from a JPEG stream by validating them
     * against an anchor GUID and sequentially stitching them back together.
     * 
     * <p>
     * <strong>References</strong><br>
     * For full technical requirements on multi-segment layout design, see the section
     * {@code Extended XMP in JPEG} inside the <strong>Adobe XMP Specification Part 3: Storage in
     * Files</strong> documentation provided by Adobe Systems Incorporated.
     * </p>
     *
     * <p>
     * <strong>Technical Summary</strong><br>
     * The JPEG format limits individual marker segments (like APP1) to 65,535 bytes due to a
     * 16-bit length restriction. Large metadata packages (e.g., depth maps, edit histories)
     * must be sliced across multiple sequential segments. This method implements the rules
     * defined in the <em>Adobe XMP Specification Part 3 (Storage in Files)</em> to locate,
     * validate, order, and merge those segments.
     * </p>
     * 
     * <p>
     * <strong>The Binary Stream Layout</strong><br>
     * Each raw byte payload chunk in the segments list must follow this header packet format:
     * </p>
     *
     * <pre>
     * +--------------------------+-------------------------------+-------------------------------+------------------------+
     * | Bytes 0 - 31 (32 Bytes)  | Bytes 32 - 35 (4 Bytes)       | Bytes 36 - 39 (4 Bytes)       | Bytes 40+ (Variable)   |
     * +--------------------------+-------------------------------+-------------------------------+------------------------+
     * | MD5 GUID Hex Character   | Total Extended Payload Length | Slice Byte Offset             | Raw Extended XML Data  |
     * | String (Matching Anchor) | Big-Endian Unsigned 32-bit    | Big-Endian Unsigned 32-bit    | Fragment Block Payload |
     * +--------------------------+-------------------------------+-------------------------------+------------------------+
     * </pre>
     * <p>
     * <strong>Stream Geometry Example</strong><br>
     * Below illustrates how {@code chunkTotalLength} remains constant across sibling slices to
     * define the final master buffer scale, while {@code chunkOffset} increments sequentially to
     * track memory mapping:
     * </p>
     *
     * <pre>
     * +---------+--------------------+--------------------+----------------+
     * | Segment |  chunkTotalLength  |    chunkOffset     |   dataLength   |
     * |         |   (Bytes 32–35)    |   (Bytes 36–39)    | (Payload Size) |
     * +---------+--------------------+--------------------+----------------+
     * | Chunk 1 | 153,600 (150 KB)   | 0                  | 65,500 bytes   |
     * | Chunk 2 | 153,600 (150 KB)   | 65,500             | 65,500 bytes   |
     * | Chunk 3 | 153,600 (150 KB)   | 131,000            | 22,600 bytes   |
     * +---------+--------------------+--------------------+----------------+
     * </pre>
     *
     * @param standardXmp
     *        the baseline Standard XMP XML block containing the master anchor token link
     * @param segments
     *        a list of raw byte segments extracted from trailing JPEG APP1 blocks, stripped of the
     *        namespace prefix string
     * @return a consolidated byte array combining the Standard and full Extended XMP packets, or
     *         the original fallback standardXmp array if validation checks fail
     */
    private byte[] reconstructExtendedXmpSegments(byte[] standardXmp, List<byte[]> segments)
    {
        if (segments.isEmpty() || standardXmp == null)
        {
            return standardXmp;
        }

        int MAX_ALLOWED_XMP_SIZE = 100 * 1024 * 1024;
        List<byte[]> segList = new ArrayList<>();
        String decodedXmlText = new String(standardXmp, StandardCharsets.UTF_8);
        Matcher m = Pattern.compile("xmpNote:HasExtendedXMP=\"([0-9A-Fa-f]{32})\"").matcher(decodedXmlText);

        if (!m.find())
        {
            LOGGER.warn("Extended XMP segments detected, but no xmpNote:HasExtendedXMP GUID found in Standard XMP anchor");
            return standardXmp;
        }

        String targetGuid = m.group(1);

        for (byte[] segData : segments)
        {
            /*
             * EXTENDED_XMP_LENGTH is GUID (32 bytes) + Total Length (4 bytes) + Offset (4 bytes) =
             * 40 bytes. See document "XMP SPECIFICATION PART 3 - STORAGE IN FILES" on Page 20
             * "Extended XMP in JPEG" for details.
             */
            if (segData.length < EXTENDED_XMP_LENGTH)
            {
                LOGGER.error("Extended XMP segment is too short. Likely a malformed entry");
                continue;
            }

            String chunkGuid = new String(segData, 0, 32, StandardCharsets.UTF_8);

            if (chunkGuid.equalsIgnoreCase(targetGuid))
            {
                segList.add(segData);
            }
        }

        if (segList.isEmpty())
        {
            return standardXmp;
        }

        // Sort fragments sequentially by their internal 4-Byte Big-Endian Offset (Bytes 36-39)
        segList.sort(new Comparator<byte[]>()
        {
            @Override
            public int compare(byte[] o1, byte[] o2)
            {
                long offset1 = ByteValueConverter.toUnsignedInteger(o1, 36, ByteOrder.BIG_ENDIAN);
                long offset2 = ByteValueConverter.toUnsignedInteger(o2, 36, ByteOrder.BIG_ENDIAN);

                return Long.compare(offset1, offset2);
            }
        });

        int totalLength = -1;
        int totalBytesCopied = 0;
        byte[] extendedBuffer = null;

        for (byte[] segData : segList)
        {
            long chunkTotalLength = ByteValueConverter.toUnsignedInteger(segData, 32, ByteOrder.BIG_ENDIAN);
            long chunkOffset = ByteValueConverter.toUnsignedInteger(segData, 36, ByteOrder.BIG_ENDIAN);

            if (totalLength == -1)
            {
                totalLength = (int) chunkTotalLength;

                /* Prevents a potential OutOfMemoryError exception */
                if (totalLength < 1 || totalLength > MAX_ALLOWED_XMP_SIZE)
                {
                    LOGGER.error(String.format("Chunk total length [%d] in bytes out of bounds", totalLength));
                    return standardXmp;
                }

                extendedBuffer = new byte[totalLength];
            }

            else if (totalLength != (int) chunkTotalLength)
            {
                LOGGER.error("Mismatched total extended length values across sibling Extended XMP slices");
                return standardXmp;
            }

            int dataLength = segData.length - EXTENDED_XMP_LENGTH;

            // Strict checking to prevent out-of-bounds corruption or continuous tracking gaps
            if (chunkOffset != totalBytesCopied)
            {
                LOGGER.error(String.format("Malformed layout geometry: Expected offset %d, but found %d", totalBytesCopied, chunkOffset));
                return standardXmp;
            }

            else if (chunkOffset + dataLength > totalLength)
            {
                LOGGER.error("Malformed layout geometry: Segment slice overflows total defined buffer space");
                return standardXmp;
            }

            System.arraycopy(segData, EXTENDED_XMP_LENGTH, extendedBuffer, (int) chunkOffset, dataLength);
            totalBytesCopied += dataLength;
        }

        if (extendedBuffer == null || totalBytesCopied != totalLength)
        {
            LOGGER.error("Extended XMP reconstruction failed: data gaps or sizing mismatch detected.");
            return standardXmp;
        }

        byte[] combinedResult = new byte[standardXmp.length + totalLength];

        System.arraycopy(standardXmp, 0, combinedResult, 0, standardXmp.length);
        System.arraycopy(extendedBuffer, 0, combinedResult, standardXmp.length, totalLength);

        return combinedResult;
    }

    /**
     * Reconstruction of a complete ICC metadata block by concatenating multiple ICC profile
     * segments. Segments are ordered by their sequence number as specified in the header.
     *
     * @param segments
     *        the list of raw ICC segments
     * @return the concatenated byte array, or returns null if no valid segments are available
     */
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

            Collections.sort(segments, new Comparator<byte[]>()
            {
                @Override
                public int compare(byte[] s1, byte[] s2)
                {
                    return Integer.compare(s1[ICC_IDENTIFIER.length] & 0xFF, s2[ICC_IDENTIFIER.length] & 0xFF);
                }
            });

            int totalCount = segments.get(0)[ICC_IDENTIFIER.length + 1] & 0xFF;

            if (totalCount == 0 || totalCount != segments.size())
            {
                LOGGER.warn(String.format("ICC segment count mismatch. Expected [%d], found [%d].", totalCount, segments.size()));
                return null;
            }

            // Enforce sequence sequence continuity
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