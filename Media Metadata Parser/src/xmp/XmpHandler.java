package xmp;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.adobe.internal.xmp.XMPException;
import com.adobe.internal.xmp.XMPIterator;
import com.adobe.internal.xmp.XMPMeta;
import com.adobe.internal.xmp.XMPMetaFactory;
import com.adobe.internal.xmp.properties.XMPPropertyInfo;
import common.ImageHandler;
import logger.LogFactory;
import xmp.XmpDirectory.XmpRecord;

/**
 * Extracts standard XMP metadata packets from image containers (JPEG, TIFF, HEIF) using the Adobe
 * XMPCore library.
 *
 * <pre>
 *  -- For developmental testing --
 *
 * <u>Some examples of exiftool usages</u>
 *
 * exiftool -XMP:All -a -u -g1 pool19.JPG
 * ---- XMP-x ----
 * XMP Toolkit : Image::ExifTool 13.29
 * ---- XMP-rdf ----
 * About : uuid:faf5bdd5-ba3d-11da-ad31-d33d75182f1b
 * ---- XMP-dc ----
 * Creator : Gemma Emily Maggs
 * Description : Trevor
 * Title : Trevor
 * ---- XMP-exif ----
 * Date/Time Original : 2011:10:07 22:59:20
 * ---- XMP-xmp ----
 * Create Date : 2011:10:07 22:59:20
 * Modify Date : 2011:10:07 22:59:20
 *
 * exiftool -XMP:Description="Construction Progress" XMPimage.png
 * </pre>
 *
 * 
 * @author Trevor
 * @version 1.9
 * @since 9 November 2025
 */
public class XmpHandler implements ImageHandler
{
    private static final LogFactory LOGGER = LogFactory.getLogger(XmpHandler.class);
    private static final Pattern REGEX_DIGIT = Pattern.compile("\\[\\d+\\]");
    private final byte[] rawPayload;
    private final XmpDirectory xmpDir;
    private boolean isParsed;

    /**
     * Parses XMP metadata from a byte array and returns a structured directory.
     *
     * @param input
     *        byte array containing the XMP XML packet
     * @return a populated {@link XmpDirectory}, or null if no properties were found
     * 
     * @throws XMPException
     *         if the data is malformed
     */
    public static XmpDirectory addXmpDirectory(byte[] input) throws XMPException
    {
        try (XmpHandler handler = new XmpHandler(input))
        {
            if (handler.parseMetadata())
            {
                LOGGER.debug(String.format("XMP Data Found. [%d bytes] processed", input.length));
                return handler.getXmpDirectory();
            }
        }

        return null;
    }

    /**
     * Constructs a new handler and registers the raw input payload data source.
     *
     * @param input
     *        raw XMP data payload, typically a single XML packet
     * @throws IllegalArgumentException
     *         if input is null or empty
     */
    public XmpHandler(byte[] input)
    {
        if (input == null || input.length == 0)
        {
            throw new IllegalArgumentException("XMP Data payload cannot be null or empty");
        }

        this.isParsed = false;
        this.rawPayload = input;
        this.xmpDir = new XmpDirectory();
    }

    /**
     * Complies with interface contract. Nothing to deallocate in this handler.
     * 
     * This handler operates purely in-memory on raw byte arrays and does not hold underlying native
     * filesystem OS resource descriptors.
     */
    @Override
    public void close()
    {
    }

    /**
     * Executes the extraction parsing logic across the registered payload asset block.
     * 
     * <p>
     * This method employs a lazy-evaluation lock to ensure that the underlying raw byte array is
     * parsed at most once. Subsequent invocations skip processing and immediately return the
     * structural state of the directory.
     * </p>
     *
     * @return true if the XMP directory was successfully populated and contains records. Or false
     *         if parsing failed or if the payload yielded no metadata properties
     */
    @Override
    public boolean parseMetadata()
    {
        if (!isParsed)
        {
            isParsed = true;

            try
            {
                readPropertyData(this.rawPayload);
            }

            catch (XMPException exc)
            {
                LOGGER.error("Adobe XMPCore engine failed to decode stream packet", exc);
                return false;
            }
        }

        return !xmpDir.isEmpty();
    }

    /**
     * Returns the directory containing all extracted XMP records.
     *
     * @return an instance of {@link XmpDirectory}
     */
    public XmpDirectory getXmpDirectory()
    {
        return xmpDir;
    }

    /**
     * Uses Adobe XMPCore to iterate through the RDF tree and populate the directory.
     * 
     * @param data
     *        an array of bytes containing raw XMP data
     *
     * @throws XMPException
     *         if parsing fails
     */
    private void readPropertyData(byte[] data) throws XMPException
    {
        XMPMeta xmpMeta = XMPMetaFactory.parseFromBuffer(data);

        if (xmpMeta != null)
        {
            String nsTracker = "";
            XMPIterator iter = xmpMeta.iterator();

            while (iter.hasNext())
            {
                Object obj = iter.next();

                if (!(obj instanceof XMPPropertyInfo))
                {
                    continue;
                }

                XMPPropertyInfo info = (XMPPropertyInfo) obj;
                String ns = info.getNamespace();
                String path = info.getPath();
                String value = info.getValue();

                // Continuous contextual state updating ensures zero namespace drops
                if (ns != null && !ns.isEmpty())
                {
                    nsTracker = ns;
                }

                // Handle structural nodes (containers/arrays) that don't have immediate values
                if (path == null || value == null || value.isEmpty())
                {
                    continue;
                }

                String finalNs = (ns != null && !ns.isEmpty()) ? ns : nsTracker;

                // Strip array indices [1] to simplify path-based lookup
                Matcher matcher = REGEX_DIGIT.matcher(path);
                String cleanedPath = matcher.replaceAll("");

                xmpDir.add(new XmpRecord(finalNs, cleanedPath, value));
            }

            LOGGER.debug("Registered [" + xmpDir.size() + "] XMP records");
        }

        else
        {
            LOGGER.warn("XMPMetaFactory failed to produce metadata object");
        }
    }
}