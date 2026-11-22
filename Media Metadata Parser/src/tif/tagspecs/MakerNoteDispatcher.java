package tif.tagspecs;

import java.util.Collections;
import java.util.List;

/**
 * Dispatches raw MakerNote byte stream payloads to the appropriate manufacturer-specific
 * parser implementation based on the camera make.
 */
public final class MakerNoteDispatcher
{
    private MakerNoteDispatcher()
    {
        // Utility class
    }

    /**
     * Parses the raw MakerNote payload based on the camera manufacturer.
     *
     * @param cameraMake
     *        the camera make string extracted from IFD0 (e.g., "Apple", "Canon")
     * @param makerNoteBytes
     *        the raw binary byte stream of the MakerNote tag (0x927C)
     * @return a list of parsed entries, or an empty list if unhandled
     */
    public static List<ParsedMakerNoteEntry> parse(String cameraMake, byte[] makerNoteBytes)
    {
        if (cameraMake == null || makerNoteBytes == null || makerNoteBytes.length == 0)
        {
            return Collections.emptyList();
        }

        String normalizedMake = cameraMake.trim().toLowerCase();

        if (normalizedMake.contains("apple"))
        {
            return AppleMakerNoteParser.parse(makerNoteBytes);
        }

        else if (normalizedMake.contains("nikon"))
        {
            // return NikonMakerNoteParser.parse(makerNoteBytes);
        }

        else if (normalizedMake.contains("sony"))
        {
            // return SonyMakerNoteParser.parse(makerNoteBytes);
        }

        else if (normalizedMake.contains("canon"))
        {
            // return CanonMakerNoteParser.parse(makerNoteBytes);
        }

        return Collections.emptyList();
    }

    /**
     * Data transfer container for an unpacked MakerNote property entry.
     */
    public static final class ParsedMakerNoteEntry
    {
        private final String groupName;
        private final String propertyName;
        private final Object propertyValue;

        public ParsedMakerNoteEntry(String groupName, String propertyName, Object propertyValue)
        {
            this.groupName = groupName;
            this.propertyName = propertyName;
            this.propertyValue = propertyValue;
        }

        public String getGroupName()
        {
            return groupName;
        }

        public String getPropertyName()
        {
            return propertyName;
        }

        public Object getPropertyValue()
        {
            return propertyValue;
        }
    }
}