package tif;

import java.util.List;
import batch.MediaRecord;
import batch.MetadataInspectionEvent;
import tif.DirectoryIFD.EntryIFD;

public abstract class CameraMarkerNote
{
    /**
     * Determines whether this parser supports the given camera make string.
     * 
     * @param cameraMake
     *        the camera manufacturer make string extracted from the image container
     * @return {@code true} if the make agrees with the camera type (case-insensitive), otherwise {@code false}  
     */
    public abstract boolean supportsMake(String cameraMake);

    /**
     * Parses the MakerNote binary block into structured inspection events.
     * 
     * @param entry
     *        the TIFF IFD entry that encapsulates the raw MakerNote byte array to be unpacked
     * @param record
     *        the target media record associated with the inspection batch
     * @return a list of {@link MetadataInspectionEvent} items 
     */
    public abstract List<MetadataInspectionEvent> processMakerNote(EntryIFD entry, MediaRecord record);
}