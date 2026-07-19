package common;

/**
 * Represents a generic collection or directory of entries of a specific type. A {@code Directory}
 * provides basic operations for adding, removing, checking for the presence of entries, and
 * querying the size and emptiness of the collection.
 *
 * <p>
 * This interface extends {@link Iterable}, allowing implementations to be used in enhanced
 * for-loops.
 * </p>
 *
 * @param <T>
 *        the type of entries contained in this directory
 */
public interface Directory<T> extends Iterable<T>
{
    /**
     * Adds the specified entry to this directory.
     *
     * <p>
     * The handling of duplicate entries is implementation-specific.
     * </p>
     *
     * @param entry
     *        the entry to add
     */
    void add(T entry);

    /**
     * Removes a single instance of the specified entry from this directory, if present.
     *
     * @param entry
     *        the entry to be removed
     * @return {@code true} if an entry was removed, otherwise {@code false}
     */
    boolean remove(T entry);

    /**
     * Returns {@code true} if this directory contains the specified entry.
     *
     * @param entry
     *        the entry whose presence in this directory is to be checked
     * @return {@code true} if this directory contains the specified entry
     */
    boolean contains(T entry);

    /**
     * Returns the number of entries in this directory.
     *
     * @return the number of entries
     */
    int size();

    /**
     * Returns {@code true} if this directory contains no entries.
     *
     * @return {@code true} if this directory is empty
     */
    boolean isEmpty();
}