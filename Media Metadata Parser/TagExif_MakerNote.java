package tif.tagspecs;

import java.util.List;
import batch.MediaRecord;
import batch.MetadataInspectionEvent;
import tif.DirectoryIFD.EntryIFD;

public interface TagExif_MakerNote extends Taggable
{
    /**
     * Determines whether this vendor parser supports the given camera make string.
     */
    boolean getCameraMake(String cameraMake);

    /**
     * Parses the MakerNote byte stream into structured inspection events.
     */
    List<MetadataInspectionEvent> processMakerNote(EntryIFD entry, MediaRecord record);
}