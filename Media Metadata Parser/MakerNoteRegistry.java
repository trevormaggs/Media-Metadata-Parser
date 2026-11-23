package tif.tagspecs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import batch.MediaRecord;
import batch.MetadataInspectionEvent;
import tif.DirectoryIFD.EntryIFD;

public final class MakerNoteRegistry
{
    private static final List<TagExif_MakerNote> VENDOR_PARSERS;

    static
    {
        List<TagExif_MakerNote> parsers = new ArrayList<>();
        parsers.add(AppleMakerNote.MAKER_NOTE_VERSION);
        //parsers.add(CanonMakerNote.MAKER_NOTE_VERSION);
        //parsers.add(NikonMakerNote.MAKER_NOTE_VERSION);
        //parsers.add(SonyMakerNote.MAKER_NOTE_VERSION);
        
        VENDOR_PARSERS = Collections.unmodifiableList(parsers);
    }

    private MakerNoteRegistry()
    {
    }

    /**
     * Polymorphically finds the correct vendor parser and processes the MakerNote.
     */
    public static List<MetadataInspectionEvent> process(String cameraMake, EntryIFD entry, MediaRecord record)
    {
        if (cameraMake == null || cameraMake.trim().isEmpty())
        {
            return Collections.emptyList();
        }

        String normalizedMake = cameraMake.trim().toLowerCase();

        for (TagExif_MakerNote parser : VENDOR_PARSERS)
        {
            if (parser.getCameraMake(normalizedMake))
            {
                return parser.processMakerNote(entry, record);
            }
        }

        return Collections.emptyList();
    }
}