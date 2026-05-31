package png;

/**
 * Common interface for PNG textual metadata chunks (tEXt, zTXt, iTXt).
 */
public interface TextualChunk
{
    /**
     * Gets the raw keyword string.
     *
     * @return the keyword
     */
    public String getKeyword();

    /**
     * Provides easy, type-safe retrieval of the resolved keyword enum token.
     *
     * @return the associated {@link TextKeyword} constant, or {@link TextKeyword#OTHER} if unknown
     */
    public TextKeyword getTextKeyword();

    /**
     * Gets the decoded text content.
     *
     * @return the decoded text, or an empty string if missing or could not be decoded
     */
    public String getText();

    /**
     * Checks if this chunk matches a specific predefined keyword.
     *
     * @param keyword
     *        the keyword to search for
     * @return true if found, false otherwise
     */
    public boolean hasKeyword(TextKeyword keyword);
}