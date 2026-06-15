package tif;

import tif.tagspecs.TagIFD_Baseline;
import tif.tagspecs.Taggable;

public final class TagMapper
{
    private TagMapper()
    {
    }
    
    public static String getDisplayName(DirectoryIdentifier dir, Taggable tag)
    {
        if (dir == DirectoryIdentifier.IFD_DIRECTORY_IFD1)
        {
            if (tag == TagIFD_Baseline.IFD_JPEG_INTERCHANGE_FORMAT)
            {
                return "ThumbnailOffset";
            }

            else if (tag == TagIFD_Baseline.IFD_JPEG_INTERCHANGE_FORMAT_LENGTH)
            {
                return "ThumbnailLength";
            }
        }

        return tag.getDescription();
    }

    public static String translate(Taggable tag, Object rawValue)
    {
        return (tag == null ? "" : tag.translate(rawValue));
    }
}