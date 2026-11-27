package tif;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import batch.MediaRecord;
import batch.MetadataInspectionEvent;
import tif.DirectoryIFD.EntryIFD;
import tif.tagspecs.AppleMakerNote;
import tif.tagspecs.CanonMakerNote;
import tif.tagspecs.NikonMakerNote;

public abstract class CameraMarkerNote
{
    private static final List<CameraMarkerNote> CAMERA_MAKER;

    static
    {
        List<CameraMarkerNote> vendors = new ArrayList<>();

        vendors.add(new AppleMakerNote());
        vendors.add(new CanonMakerNote());
        vendors.add(new NikonMakerNote());
        // vendors.add(new SamsungMakerNote());
        // vendors.add(new SonyMakerNote());

        CAMERA_MAKER = Collections.unmodifiableList(vendors);
    }

    public static List<MetadataInspectionEvent> process(String cameraMake, EntryIFD entry, MediaRecord record)
    {
        if (cameraMake != null && !cameraMake.isEmpty())
        {
            for (CameraMarkerNote vendor : CAMERA_MAKER)
            {
                if (vendor.supportsMake(cameraMake))
                {
                    return vendor.processMakerNote(entry, record);
                }
            }
        }

        return Collections.emptyList();
    }

    /**
     * Determines whether this parser supports the given camera make string.
     * 
     * @param cameraMake
     *        the camera manufacturer make string extracted from the image container
     * @return {@code true} if the make agrees with the camera type (case-insensitive), otherwise
     *         {@code false}
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