package tif.tagspecs;

/**
 * Extends the baseline {@link Taggable} contract for manufacturer-specific MakerNote tags.
 */
public interface TagExif_MakerNote extends Taggable
{
    /**
     * @return the camera manufacturer identifier (e.g., "CANON", "NIKON", "SONY")
     */
    String getCameraMake();
}