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
import java.util.Collections;
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
 * specification. For {@code XMP} data, it aggregates sequential APP1 segments flagged with the
 * {@code http://ns.adobe.com/xap/1.0/0} identifier, concatenating their raw payloads to reassemble
 * the underlying XML metadata stream.
 * </p>
 *
 * @author Trevor Maggs
 * @version 1.9
 * @since 22 May 2026
 */
public class JpgParserOld2 extends AbstractImageParser<TifMetadata>
{
    private static final LogFactory LOGGER = LogFactory.getLogger(JpgParserOld2.class);
    private static final int PADDING_LIMIT = 64;
    public static final byte[] EXIF_IDENTIFIER = "Exif\0\0".getBytes(StandardCharsets.UTF_8);
    public static final byte[] ICC_IDENTIFIER = "ICC_PROFILE\0".getBytes(StandardCharsets.UTF_8);
    public static final byte[] XMP_IDENTIFIER = "http://ns.adobe.com/xap/1.0/\0".getBytes(StandardCharsets.UTF_8);
    private final TifMetadata metadata;
    private JpgSegmentData segmentData;
    private boolean dataLoaded = false;

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

        @SuppressWarnings("unused")
        private boolean hasMetadata()
        {
            return ((exif != null && exif.length > 0) ||
                    (xmp != null && xmp.length > 0) ||
                    (icc != null && icc.length > 0));
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
    public JpgParserOld2(String file) throws IOException
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
    public JpgParserOld2(Path fpath) throws IOException
    {
        super(fpath);

        String ext = Utils.getFileExtension(getImageFile());

        if (!ext.equalsIgnoreCase("jpg") && !ext.equalsIgnoreCase("jpeg"))
        {
            LOGGER.warn(formatExtensionErrorMessage());
        }

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
                    LOGGER.error("Corrupt or invalid EXIF payload encountered inside APP1 layout structure; leaving structure empty", exc);
                }
            }

            if (segmentData.getXmp().isPresent() && !metadata.hasXmpData())
            {
                try
                {
                    XmpDirectory xmpDir = XmpHandler.addXmpDirectory(segmentData.getXmp().get());
                    metadata.addXmpDirectory(xmpDir);
                }

                catch (XMPException exc)
                {
                    LOGGER.error("Unable to parse standalone XMP payload in file [" + getImageFile() + "] due to an error", exc);
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
                throw new UncheckedIOException("Lazy execution of readMetadata() failed downstream", exc);
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
                sb.append("No EXIF metadata found.").append(System.lineSeparator());
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
                sb.append("Parser has concatenated all ICC segments, ");
                sb.append(String.format("totalling [%d] bytes of ICC Data.", segmentData.getIcc().get().length)).append(System.lineSeparator());
            }

            else
            {
                sb.append("No ICC Profile found.").append(System.lineSeparator());
            }

            sb.append(MetadataConstants.DIVIDER).append(System.lineSeparator());
        }

        catch (Exception exc)
        {
            LOGGER.error("Diagnostics failed for file [" + getImageFile() + "]", exc);

            sb.append("Error generating diagnostics [")
                    .append(exc.getClass().getSimpleName())
                    .append("]: ")
                    .append(exc.getMessage())
                    .append(System.lineSeparator());
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
        if (data.length >= JpgParserOld2.EXIF_IDENTIFIER.length && Arrays.equals(Arrays.copyOf(data, JpgParserOld2.EXIF_IDENTIFIER.length), JpgParserOld2.EXIF_IDENTIFIER))
        {
            return Arrays.copyOfRange(data, JpgParserOld2.EXIF_IDENTIFIER.length, data.length);
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

                if (LOGGER.isDebugEnabled())
                {
                    if (!(flag >= JpgSegmentConstants.RST0.getFlag() && flag <= JpgSegmentConstants.RST7.getFlag()) && flag != JpgSegmentConstants.UNKNOWN.getFlag())
                    {
                        LOGGER.debug(String.format("Segment flag [%s] detected", JpgSegmentConstants.fromBytes(marker, flag)));
                    }
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
        List<byte[]> iccSegments = new ArrayList<>();
        List<byte[]> xmpSegments = new ArrayList<>();

        while (reader.getCurrentPosition() < reader.length())
        {
            JpgSegmentConstants segment = fetchNextSegment(reader);

            if (segment == null || segment == JpgSegmentConstants.END_OF_IMAGE || (segment == JpgSegmentConstants.START_OF_STREAM))
            {
                break;
            }

            if (segment.hasLengthField())
            {
                int length = reader.readUnsignedShort() - 2;

                if (length < 0 || (reader.getCurrentPosition() + length) > reader.length())
                {
                    LOGGER.error(String.format("Malformed segment size [%d] at marker location 0x%04X overflows data boundary limit.", length + 2, reader.getCurrentPosition() - 2));
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
                        if (exifSegment == null)
                        {
                            byte[] strippedPayload = JpgParserOld2.stripExifPreamble(payload);

                            if (strippedPayload.length < payload.length)
                            {
                                exifSegment = strippedPayload;
                                LOGGER.debug(String.format("Valid EXIF APP1 segment found. Length [%d]", exifSegment.length));

                                continue;
                            }
                        }

                        if (payload.length >= XMP_IDENTIFIER.length && Arrays.equals(Arrays.copyOfRange(payload, 0, XMP_IDENTIFIER.length), XMP_IDENTIFIER))
                        {
                            xmpSegments.add(Arrays.copyOfRange(payload, XMP_IDENTIFIER.length, payload.length));
                            LOGGER.debug(String.format("Valid XMP APP1 segment found. Length [%d]", payload.length));

                            continue;
                        }

                        LOGGER.debug(String.format("Non-EXIF/XMP APP1 segment skipped. Length [%d]", payload.length));
                    }

                    else if (segment == JpgSegmentConstants.APP2_SEGMENT)
                    {
                        if (payload.length >= ICC_IDENTIFIER.length && Arrays.equals(Arrays.copyOfRange(payload, 0, ICC_IDENTIFIER.length), ICC_IDENTIFIER))
                        {
                            iccSegments.add(payload);
                            LOGGER.debug(String.format("Valid ICC APP2 segment found. Length [%d]", payload.length));

                            continue;
                        }

                        LOGGER.debug(String.format("Non-ICC APP2 segment skipped. Length [%d]", payload.length));
                    }
                }

                else
                {
                    reader.skip(length);
                }
            }
        }

        return new JpgSegmentData(exifSegment, reconstructXmpSegments(xmpSegments), reconstructIccSegments(iccSegments));
    }

    /**
     * Reassembles XMP metadata fragments into a single, cohesive byte array for parsing.
     *
     * @param segments
     *        a list of isolated XMP payload byte arrays, with their identification preambles
     *        already stripped
     * 
     * @return the concatenated byte array, or returns null if no segments are available
     */
    private byte[] reconstructXmpSegments(List<byte[]> segments)
    {
        if (!segments.isEmpty())
        {
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream())
            {
                for (byte[] seg : segments)
                {
                    baos.write(seg);
                }

                LOGGER.debug(String.format("Successfully reconstructed XMP metadata from [%d] segment(s)", segments.size()));
                return baos.toByteArray();
            }

            catch (IOException exc)
            {
                LOGGER.error("Failed to concatenate XMP segments", exc);
            }
        }

        return null;
    }

    /**
     * Reconstruction of a complete ICC metadata block by concatenating multiple ICC profile segments.
     * Segments are ordered by their sequence number as specified in the header.
     *
     * @param segments
     *        the list of raw ICC segments
     * @return the concatenated byte array, or returns null if no valid segments are available
     */
    private byte[] reconstructIccSegments(List<byte[]> segments)
    {
        int headerLength = ICC_IDENTIFIER.length + 2;

        if (segments.isEmpty())
        {
            return null;
        }

        for (byte[] seg : segments)
        {
            if (seg.length < headerLength)
            {
                LOGGER.error("One or more ICC segments are too short to contain the required header information");
                return null;
            }
        }

        int totalCount = segments.get(0)[ICC_IDENTIFIER.length + 1] & 0xFF;

        if (totalCount == 0 || totalCount != segments.size())
        {
            LOGGER.warn(String.format("ICC segment count mismatch. Expected [%d] segments, but found [%d]. Profile may be corrupted or incomplete.", totalCount, segments.size()));
            return null;
        }

        for (byte[] seg : segments)
        {
            if ((seg[ICC_IDENTIFIER.length + 1] & 0xFF) != totalCount)
            {
                LOGGER.error("Inconsistent total segment count (M) found across ICC segments. Profile is corrupted");
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

        return null;
    }
}