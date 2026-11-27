package tif.tagspecs;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import batch.MediaRecord;
import batch.MetadataInspectionEvent;
import tif.CameraMarkerNote;
import tif.DirectoryIFD.EntryIFD;

public class NikonMakerNote extends CameraMarkerNote
{
    private static final byte[] NIKON_IDENTIFIER = "Nikon\0".getBytes(StandardCharsets.UTF_8);

    @Override
    public boolean supportsMake(String cameraMake)
    {
        return cameraMake != null && cameraMake.toLowerCase().contains("nikon");
    }

    @Override
    public List<MetadataInspectionEvent> processMakerNote(EntryIFD entry, MediaRecord record)
    {
        byte[] rawBytes = entry.getByteArray();
        List<MetadataInspectionEvent> events = new ArrayList<>();

        if (rawBytes == null || rawBytes.length < NIKON_IDENTIFIER.length)
        {
            return Collections.emptyList();
        }

        // Verify Nikon signature and parse nested TIFF header offsets...
        return events;
    }
}