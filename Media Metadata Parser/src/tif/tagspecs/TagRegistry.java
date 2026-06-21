package tif.tagspecs;

import java.util.HashMap;
import java.util.Map;
import logger.LogFactory;
import tif.DirectoryIdentifier;

/**
 * Central registry that maps TIFF tag IDs and directory types to their corresponding
 * {@link Taggable} definitions.
 *
 * <p>
 * TIFF metadata is context-sensitive. The same numeric tag ID can have different meanings depending
 * on the Image File Directory (IFD) in which it appears. For example, tag {@code 0x0001} represents
 * <i>GPSLatitudeRef</i> in a GPS IFD, but <i>InteropIndex</i> in an Interoperability IFD.
 * </p>
 *
 * <p>
 * During initialisation, tags associated with the root TIFF directory (IFD0) are automatically
 * registered across all main-chain IFDs (IFD0 through IFD3), allowing resolution of shared TIFF
 * tags in multi-page images and embedded thumbnails.
 * </p>
 *
 * @author Trevor Maggs
 * @version 1.1
 * @since 18 June 2026
 */
public final class TagRegistry
{
    private static final LogFactory LOGGER = LogFactory.getLogger(TagRegistry.class);
    private static final Map<DirectoryIdentifier, Map<Integer, Taggable>> TAG_REGISTRY = new HashMap<>();

    static
    {
        for (DirectoryIdentifier dir : DirectoryIdentifier.values())
        {
            TAG_REGISTRY.put(dir, new HashMap<>());
        }

        /*
         * Populate the registry with all supported tag sets. Note: Tag sets
         * are registered based on their internal DirectoryIdentifier.
         */
        register(TagIFD_Baseline.values());
        register(TagIFD_Extension.values());
        register(TagIFD_Exif.values());
        register(TagIFD_GPS.values());
        register(TagIFD_Private.values());
        register(TagExif_Interop.values());
        register(TagIFD_Pointer.values());
        register(TagIFD_DNG.values());

        if (LogFactory.isDebugEnabled())
        {
            for (Map.Entry<DirectoryIdentifier, Map<Integer, Taggable>> entry : TAG_REGISTRY.entrySet())
            {
                for (Taggable tag : entry.getValue().values())
                {
                    LOGGER.debug(String.format("Registered: %-10s | 0x%04X | %s", entry.getKey(), tag.getNumberID(), tag));
                }
            }
        }
    }

    /**
     * Default constructor is unsupported and will always throw an exception.
     *
     * @throws UnsupportedOperationException
     *         to indicate that instantiation is not supported
     */
    private TagRegistry()
    {
        throw new UnsupportedOperationException("Not intended for instantiation");
    }

    /**
     * Registers tag definitions in the global registry.
     *
     * <p>
     * Tags associated with the root TIFF directory are automatically registered across all
     * main-chain IFDs (IFD0 through IFD3), since these directories share the same baseline tag set.
     * All other tags are registered only within their declared directory scope.
     * </p>
     */
    private static void register(Taggable[] tags)
    {
        for (Taggable tag : tags)
        {
            DirectoryIdentifier dirType = tag.getDirectoryType();

            if (dirType == DirectoryIdentifier.IFD_ROOT_DIRECTORY)
            {
                for (DirectoryIdentifier dir : DirectoryIdentifier.values())
                {
                    if (dir.isMainChain())
                    {
                        TAG_REGISTRY.get(dir).put(tag.getNumberID(), tag);
                    }
                }
            }

            else
            {
                TAG_REGISTRY.get(dirType).put(tag.getNumberID(), tag);
            }
        }
    }

    /**
     * Resolves a TIFF tag identifier within the context of a specific directory.
     *
     * <p>
     * Resolution is performed against the directory-specific registry first. For SubIFDs, a
     * secondary lookup is performed against the root IFD registry because many baseline TIFF tags
     * are legally reused inside DNG and TIFF SubIFD structures.
     * </p>
     *
     * @param id
     *        the unsigned 16-bit tag identifier
     * @param directory
     *        the directory containing the tag
     * @return the matching {@code Taggable} definition, or a {@link TagIFD_Unknown} instance if the
     *         tag is not registered
     */
    public static Taggable resolve(int id, DirectoryIdentifier directory)
    {
        Map<Integer, Taggable> dirMap = TAG_REGISTRY.get(directory);

        if (dirMap != null)
        {
            Taggable tag = dirMap.get(id);

            if (tag != null)
            {
                return tag;
            }
        }

        /*
         * Some tags defined for the root IFD may also appear inside a SubIFD. If the tag is not
         * registered in the SubIFD namespace, perform a fallback lookup using the root IFD
         * registry.
         */
        if (directory == DirectoryIdentifier.IFD_SUBIFD_DIRECTORY)
        {
            Map<Integer, Taggable> rootMap = TAG_REGISTRY.get(DirectoryIdentifier.IFD_ROOT_DIRECTORY);

            Taggable tag = (rootMap != null) ? rootMap.get(id) : null;

            if (tag != null)
            {
                return tag;
            }
        }

        return new TagIFD_Unknown(id, directory);
    }
}