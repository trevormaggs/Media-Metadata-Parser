package png;

import java.nio.charset.StandardCharsets;
import common.ByteValueConverter;
import common.MetadataConstants;
import common.Utils;
import logger.LogFactory;
import tif.TagHint;

/**
 * Represents a {@code tEXt} chunk in a PNG file, which stores original textual data.
 *
 * This chunk contains a keyword and associated text string, both encoded in Latin-1. It extends
 * {@link PngChunk} to provide decoding of the textual content.
 *
 * @author Trevor Maggs
 * @version 1.0
 * @since 13 August 2025
 */
public class PngChunkTEXT extends PngChunk implements TextualChunk
{
    private static final LogFactory LOGGER = LogFactory.getLogger(PngChunkTEXT.class);
    private final String keyword;
    private final TextKeyword textKeyword;
    private final String text;

    /**
     * Constructs a {@code PngChunkTEXT} instance with the specified metadata.
     *
     * <p>
     * According to the PNG specification:
     * </p>
     *
     * <ul>
     * <li><strong>Keyword</strong>: Latin-1 string (1–79 bytes)</li>
     * <li><strong>Null Separator</strong>: 1 byte</li>
     * <li><strong>Text</strong>: Latin-1 string (0 or more bytes)</li>
     * </ul>
     *
     * @param length
     *        the length of the chunk's data field (excluding type and CRC)
     * @param typeBytes
     *        the raw 4-byte chunk type
     * @param crc32
     *        the CRC value read from the file
     * @param data
     *        raw chunk data
     * @param offsetStart
     *        the absolute physical position in the file where the chunk begins
     */
    public PngChunkTEXT(long length, byte[] typeBytes, int crc32, byte[] data, long offsetStart)
    {
        super(length, typeBytes, crc32, data, offsetStart);

        String[] parts = ByteValueConverter.splitNullDelimitedStrings(payload, StandardCharsets.ISO_8859_1);

        if (parts.length < 2)
        {
            LOGGER.warn("tEXt chunk missing null separator or malformed [" + ByteValueConverter.toHex(payload) + "]");

            this.keyword = "";
            this.textKeyword = TextKeyword.OTHER;
            this.text = "";

            return;
        }

        String parsedKeyword = parts[0];
        String parsedText = parts[1];

        if (parsedKeyword.length() == 0 || parsedKeyword.length() > 79)
        {
            LOGGER.warn("Invalid tEXt keyword length (must be 1–79 characters). Keyword found [" + parsedKeyword + "]");

            this.keyword = "";
            this.textKeyword = TextKeyword.OTHER;
            this.text = "";

            return;
        }

        this.keyword = parsedKeyword;
        this.textKeyword = TextKeyword.fromIdentifierString(parsedKeyword);
        this.text = parsedText;
    }

    /**
     * Returns the decoded keyword from this tEXt chunk.
     *
     * @return the text or null if not yet decoded
     */
    @Override
    public String getKeyword()
    {
        return keyword;
    }

    /**
     * Provides easy, type-safe retrieval of the resolved keyword enum token.
     *
     * @return the associated {@link TextKeyword} constant, or {@link TextKeyword#OTHER} if unknown
     */
    @Override
    public TextKeyword getTextKeyword()
    {
        return textKeyword;
    }

    /**
     * Returns the decoded text from this tEXt chunk.
     *
     * @return the text or an empty string if it was not decoded
     */
    @Override
    public String getText()
    {
        return text;
    }

    /**
     * Checks whether this chunk contains a specific textual keyword.
     *
     * @param keyword
     *        the {@link TextKeyword} to search for
     * @return true if found, false otherwise
     * @throws IllegalArgumentException
     *         if the specified keyword is null
     */
    @Override
    public boolean hasKeyword(TextKeyword keyword)
    {
        if (keyword == null)
        {
            throw new IllegalArgumentException("Keyword cannot be null");
        }

        return this.textKeyword == keyword;
    }

    /**
     * Returns a string representation of the chunk's properties and contents.
     *
     * @return a formatted string describing this chunk
     */
    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        sb.append(super.toString());
        sb.append(String.format(MetadataConstants.FORMATTER, "Keyword", getKeyword()));
        sb.append(String.format(MetadataConstants.FORMATTER, "Text", getText()));

        if (textKeyword.getHint() == TagHint.HINT_DATE)
        {
            String formattedDate = Utils.formatDateString(getText(), Utils.LOCALE_AU);
            sb.append(String.format(MetadataConstants.FORMATTER, "Formatted Date", formattedDate));
        }

        return sb.toString();
    }
}