package tif.tagspecs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import batch.MediaRecord;
import batch.MetadataInspectionEvent;
import tif.DirectoryIFD.EntryIFD;
import tif.VendorMakerNote;

public final class MakerNoteRegistry
{
    private static final List<VendorMakerNote> VENDOR_PARSERS;

    static
    {
        List<VendorMakerNote> parsers = new ArrayList<>();
        
        // Clean polymorphic instantiation
        parsers.add(new AppleMakerNote());
        // Future parsers (e.g., parsers.add(new NikonMakerNote());) go here seamlessly
        
        VENDOR_PARSERS = Collections.unmodifiableList(parsers);
    }

    private MakerNoteRegistry()
    {
    }

    public static List<MetadataInspectionEvent> process(String cameraMake, EntryIFD entry, MediaRecord record)
    {
        if (cameraMake == null || cameraMake.trim().isEmpty())
        {
            return Collections.emptyList();
        }

        for (VendorMakerNote parser : VENDOR_PARSERS)
        {
            if (parser.supportsMake(cameraMake))
            {
                return parser.processMakerNote(entry, record);
            }
        }

        return Collections.emptyList();
    }
}