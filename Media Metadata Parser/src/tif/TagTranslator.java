package tif;

import tif.tagspecs.Taggable;

public class TagTranslator
{

    public static String translate(Taggable tag, Object value)
    {
        if (value == null)
        {
            return "";
        }

        int tagID = tag.getNumberID();

        // Only process tags that need translation
        switch (tagID)
        {
            case 0x0112: // Orientation
                return translateOrientation(value);
            case 0x0128: // ResolutionUnit
                return translateResolutionUnit(value);
            case 0x0103: // Compression
                return translateCompression(value);
            default:
                return String.valueOf(value);
        }
    }

    private static String translateCompression(Object val)
    {
        if (!(val instanceof Number))
        {
            return String.valueOf(val);
        }

        switch (((Number) val).intValue())
        {
            case 1:
                return "Uncompressed";
            case 2:
                return "CCITT 1D";
            case 3:
                return "T4/Group 3 Fax";
            case 4:
                return "T6/Group 4 Fax";
            case 5:
                return "LZW";
            case 6:
                return "JPEG (old-style)";
            case 7:
                return "JPEG";
            case 8:
                return "Adobe Deflate";
            case 32773:
                return "PackBits";
            default:
                return String.valueOf(val);
        }
    }

    private static String translateOrientation(Object val)
    {
        if (!(val instanceof Number))
        {
            return String.valueOf(val);
        }

        switch (((Number) val).intValue())
        {
            case 1:
                return "Horizontal (normal)";
            case 3:
                return "Rotate 180";
            case 6:
                return "Rotate 90 CW";
            case 8:
                return "Rotate 270 CW";
            default:
                return String.valueOf(val);
        }
    }

    private static String translateResolutionUnit(Object val)
    {
        if (!(val instanceof Number))
        {
            return String.valueOf(val);
        }

        switch (((Number) val).intValue())
        {
            case 1:
                return "None";
            case 2:
                return "inches";
            case 3:
                return "cm";
            default:
                return String.valueOf(val);
        }
    }

    public static String getDisplayName(DirectoryIdentifier dir, Taggable tag)
    {
        int id = tag.getNumberID();

        // IFD1 is almost always the thumbnail directory
        if (dir == DirectoryIdentifier.IFD_DIRECTORY_IFD1)
        {
            if (id == 0x0201)
            {
                return "ThumbnailOffset";
            }

            if (id == 0x0202)
            {
                return "ThumbnailLength";
            }
        }

        // Default back to the standard description
        return tag.getDescription();
    }
}