package tif.tagspecs;

import java.util.HashMap;
import java.util.Map;
import logger.LogFactory;
import tif.DirectoryIdentifier;

/**
 * A central repository for mapping numerical Tag IDs and their associated
 * {@link DirectoryIdentifier} to {@link Taggable} enum constants.
 * 
 * <p>
 * This registry handles the contextual nature of the TIFF specification, where the same Tag ID may
 * represent different metadata depending on the directory (IFD) it resides in. For example, ID
 * {@code 0x0001} represents <i>GPSLatitudeRef</i> in a GPS IFD, but <i>InteropIndex</i> in an
 * Interoperability IFD.
 * </p>
 * 
 * <p>
 * To allow multi-page documents and embedded thumbnails without complex runtime lookups, universal
 * main-chain tags (including layout parameters and sub-directory pointers) are broadcasted across
 * all main image file directory scopes (IFD0 through IFD3) during initialisation, facilitating
 * direct and fast O(1) context resolution at parsing time.
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
     * Registers an array of {@link Taggable} constants into the global tag registry.
     *
     * <p>
     * This method maps each tag definition to its respective directory-specific container.
     * Constants targeting the root directory level (such as structural layout parameters and
     * main-chain directory pointers) are automatically broadcasted and replicated across all
     * primary sequential image file directories (IFD0 through IFD3) to facilitate multi-page and
     * thumbnail processing.
     * </p>
     *
     * @param tags
     *        the array of tag constants to initialise and register
     */
    private static void register(Taggable[] tags)
    {
        for (Taggable tag : tags)
        {
            if (tag != null)
            {
                // Supports IFD0 to IFD3 main-chain directories
                if (tag.getDirectoryType() == DirectoryIdentifier.IFD_ROOT_DIRECTORY)
                {
                    // Broadcast across the entire primary structural sequence (IFD0 through IFD3)
                    for (DirectoryIdentifier dir : DirectoryIdentifier.values())
                    {
                        if (dir.isMainChain())
                        {
                            Map<Integer, Taggable> dirMap = TAG_REGISTRY.get(dir);

                            if (dirMap != null)
                            {
                                dirMap.put(tag.getNumberID(), tag);
                            }
                        }
                    }
                }

                else
                {
                    // Non main-chain directories such as EXIF, GPS or Interop
                    DirectoryIdentifier dirType = tag.getDirectoryType();
                    Map<Integer, Taggable> dirMap = TAG_REGISTRY.get(dirType);

                    if (dirMap != null)
                    {
                        dirMap.put(tag.getNumberID(), tag);
                    }
                }
            }
        }
    }

    /**
     * Resolves a TIFF tag ID to its corresponding {@code Taggable} definition within the context of
     * a specific directory.
     *
     * @param id
     *        the unsigned 16-bit tag identifier
     * @param directory
     *        the directory containing the tag
     *
     * @return the matching {@code Taggable} definition, or a {@link TagIFD_Unknown} instance if the
     *         tag is not registered for the specified directory
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