package tif;

import java.nio.ByteOrder;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import common.DigitalSignature;
import tif.tagspecs.TagIFD_Baseline;
import tif.tagspecs.TagIFD_Exif;
import util.SmartDateParser;
import xmp.XmpDirectory;
import xmp.XmpProperty;

/**
 * A container for storing information on extracted TIFF-based metadata, including EXIF and XMP
 * segments.
 *
 * <p>
 * This class provides access to Image File Directories (IFDs), such as the primary IFD and the EXIF
 * sub-IFD, typically found in TIFF and JPEG files.
 * </p>
 *
 * @author Trevor Maggs
 * @version 1.2
 * @since 13 August 2025
 */
public class TifMetadata implements TifMetadataProvider
{
    private final Map<DirectoryIdentifier, DirectoryIFD> ifdMap;
    private DigitalSignature imageFormat;
    private ByteOrder byteOrder;
    private XmpDirectory xmpDir;

    /**
     * Constructs an empty metadata container configured for TIFF parsing by default.
     */
    public TifMetadata()
    {
        this(DigitalSignature.TIF);
    }

    /**
     * Constructs a metadata container for a specific image format layout.
     *
     * @param imageFormat
     *        the true structural format identity
     */
    public TifMetadata(DigitalSignature imageFormat)
    {
        this.ifdMap = new HashMap<>();
        this.imageFormat = imageFormat;
    }

    /**
     * Constructs a new metadata container with a specific format identity and byte order.
     *
     * @param imageFormat
     *        the true structural format identity
     * @param order
     *        either {@code ByteOrder.BIG_ENDIAN} or {@code ByteOrder.LITTLE_ENDIAN}
     * @throws NullPointerException
     *         if {@code order} is {@code null}
     */
    public TifMetadata(DigitalSignature imageFormat, ByteOrder order)
    {
        this(imageFormat);
        setByteOrder(Objects.requireNonNull(order, "Byte order cannot be null"));
    }

    /**
     * Sets the image format associated with this metadata container.
     *
     * @param format
     *        the detected image format
     */
    void setImageFormat(DigitalSignature format)
    {
        imageFormat = format;
    }

    /**
     * Adds a new {@link DirectoryIFD} to the container.
     *
     * @param directory
     *        the directory to add
     * 
     * @throws NullPointerException
     *         if the directory is null
     * @throws IllegalStateException
     *         if the byte order is not yet determined
     */
    @Override
    public void addDirectory(DirectoryIFD directory)
    {
        if (directory == null)
        {
            throw new NullPointerException("Directory cannot be null");
        }

        if (byteOrder == null)
        {
            throw new IllegalStateException("ByteOrder is undefined. Please ensure the TIFF header is processed first");
        }

        ifdMap.put(directory.getDirectoryType(), directory);
    }

    /**
     * Removes a {@link DirectoryIFD} from the container.
     *
     * @param dir
     *        the {@link DirectoryIFD} to remove
     * @return {@code true} if the directory was successfully removed
     *
     * @throws NullPointerException
     *         if the specified directory is null
     */
    @Override
    public boolean removeDirectory(DirectoryIFD dir)
    {
        Objects.requireNonNull(dir, "Directory cannot be null");

        return (ifdMap.remove(dir.getDirectoryType()) != null);
    }

    /**
     * Checks if the metadata container is empty.
     *
     * @return {@code true} if no directories are present
     */
    @Override
    public boolean isEmpty()
    {
        return (ifdMap.isEmpty() && xmpDir == null);
    }

    /**
     * Returns the byte order, indicating how data values will be interpreted correctly.
     *
     * @return either {@link java.nio.ByteOrder#BIG_ENDIAN} or
     *         {@link java.nio.ByteOrder#LITTLE_ENDIAN}
     */
    @Override
    public ByteOrder getByteOrder()
    {
        return byteOrder;
    }

    /**
     * Returns the image format associated with this metadata container.
     *
     * @return the detected {@link DigitalSignature}
     */
    @Override
    public DigitalSignature getImageFormat()
    {
        return imageFormat;
    }

    /**
     * Returns an iterator over all stored directories.
     *
     * @return an iterator of {@link DirectoryIFD} instances
     */
    @Override
    public Iterator<DirectoryIFD> iterator()
    {
        return ifdMap.values().iterator();
    }

    /**
     * Retrieves a {@link DirectoryIFD} from the container by identity.
     *
     * @param key
     *        the {@link DirectoryIdentifier} of the directory to retrieve
     * @return the {@link DirectoryIFD} associated with the key, or {@code null} if not found
     */
    @Override
    public DirectoryIFD getDirectory(DirectoryIdentifier key)
    {
        return ifdMap.get(key);
    }

    /**
     * Sets the specified byte order used to interpret multi-byte raw data correctly.
     *
     * @param order
     *        either {@code ByteOrder.BIG_ENDIAN} or {@code ByteOrder.LITTLE_ENDIAN}
     */
    @Override
    public void setByteOrder(ByteOrder order)
    {
        byteOrder = order;
    }

    /**
     * Resets this metadata container to its initial state.
     *
     * <p>
     * All stored directories and XMP metadata are removed, the byte order is cleared, and the image
     * format is reset to {@link DigitalSignature#TIF}.
     * </p>
     */
    @Override
    public void clear()
    {
        ifdMap.clear();
        xmpDir = null;
        byteOrder = null;
        imageFormat = DigitalSignature.TIF;
    }

    /**
     * Adds a new {@link XmpDirectory} directory to this container.
     *
     * @param dir
     *        the {@link XmpDirectory} to be added
     *
     * @throws NullPointerException
     *         if the specified directory is null
     */
    @Override
    public void addXmpDirectory(XmpDirectory dir)
    {
        Objects.requireNonNull(dir, "XMP Directory cannot be null");

        xmpDir = dir;
    }

    /**
     * Retrieves the parsed XMP metadata directory.
     *
     * @return the {@link XmpDirectory}, or {@code null} if no XMP metadata is present
     */
    @Override
    public XmpDirectory getXmpDirectory()
    {
        return xmpDir;
    }

    /**
     * Checks if the collection contains any metadata.
     *
     * @return {@code true} if the container is not empty
     */
    @Override
    public boolean hasMetadata()
    {
        return !isEmpty();
    }

    /**
     * Indicates whether the container holds EXIF metadata.
     *
     * @return {@code true} if EXIF metadata is present, otherwise {@code false}
     */
    @Override
    public boolean hasExifData()
    {
        return isDirectoryPresent(DirectoryIdentifier.IFD_EXIF_SUBIFD_DIRECTORY);
    }

    /**
     * Checks if the collection contains an XMP directory.
     *
     * @return {@code true} if XMP metadata is present
     */
    @Override
    public boolean hasXmpData()
    {
        return (xmpDir != null);
    }

    /**
     * Extracts the most authoritative creation date available, returning a {@link ZonedDateTime}
     * derived from a prioritised metadata hierarchy.
     *
     * <p>
     * This method employs a "waterfall" strategy to identify the earliest point of origin,
     * evaluating tags in the following order:
     * </p>
     *
     * <ol>
     * <li>EXIF Sub-IFD: {@code DateTimeOriginal} (Tag 0x9003)</li>
     * <li>Main IFD0: {@code DateTimeOriginal} or {@code DateTime}</li>
     * <li>XMP EXIF Schema: {@code DateTimeOriginal}</li>
     * <li>XMP General Schema: {@code CreateDate}</li>
     * </ol>
     *
     * @return the extracted {@link ZonedDateTime}, or {@code null} if no valid timestamp is
     *         detected across the supported metadata directories
     */
    @Override
    public ZonedDateTime extractZonedDateTime()
    {
        DirectoryIFD exifDir = getDirectory(DirectoryIdentifier.IFD_EXIF_SUBIFD_DIRECTORY);

        if (exifDir != null && exifDir.hasTag(TagIFD_Exif.EXIF_DATE_TIME_ORIGINAL))
        {
            return exifDir.getZonedDateTime(TagIFD_Exif.EXIF_DATE_TIME_ORIGINAL);
        }

        DirectoryIFD mainDir = getDirectory(DirectoryIdentifier.IFD_DIRECTORY_IFD0);

        if (mainDir != null)
        {
            // TODO: Does IFD0 hold EXIF_DATE_TIME_ORIGINAL? Unlikely?
            if (mainDir.hasTag(TagIFD_Exif.EXIF_DATE_TIME_ORIGINAL))
            {
                return mainDir.getZonedDateTime(TagIFD_Exif.EXIF_DATE_TIME_ORIGINAL);
            }

            if (mainDir.hasTag(TagIFD_Baseline.IFD_DATE_TIME))
            {
                return mainDir.getZonedDateTime(TagIFD_Baseline.IFD_DATE_TIME);
            }
        }

        if (hasXmpData())
        {
            Optional<String> optXmp = xmpDir.getValueByPath(XmpProperty.EXIF_DATE_TIME_ORIGINAL);

            if (optXmp.isPresent())
            {
                ZonedDateTime zdt = SmartDateParser.convertToZonedDateTime(optXmp.get());

                if (zdt != null)
                {
                    return zdt;
                }
            }

            optXmp = xmpDir.getValueByPath(XmpProperty.XMP_CREATEDATE);

            if (optXmp.isPresent())
            {
                ZonedDateTime zdt = SmartDateParser.convertToZonedDateTime(optXmp.get());

                if (zdt != null)
                {
                    return zdt;
                }
            }
        }

        return null;
    }

    /**
     * Checks whether a directory of the specified type is present.
     *
     * @param dir
     *        the directory identifier to test
     *
     * @return {@code true} if the directory exists in this container, otherwise {@code false}
     */
    public boolean isDirectoryPresent(DirectoryIdentifier dir)
    {
        return ifdMap.containsKey(dir);
    }
}