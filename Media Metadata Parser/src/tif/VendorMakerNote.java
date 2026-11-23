package tif;

import java.util.List;
import batch.MediaRecord;
import batch.MetadataInspectionEvent;
import tif.DirectoryIFD.EntryIFD;

public abstract class VendorMakerNote
{
    /**
     * Determines whether this parser supports the given camera make string.
     */
    public abstract boolean supportsMake(String cameraMake);

    /**
     * Parses the MakerNote binary block into structured inspection events.
     */
    public abstract List<MetadataInspectionEvent> processMakerNote(EntryIFD entry, MediaRecord record);
}