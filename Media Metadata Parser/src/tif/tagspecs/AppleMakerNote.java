package tif.tagspecs;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import batch.MediaRecord;
import batch.MetadataInspectionEvent;
import tif.DirectoryIFD.EntryIFD;
import tif.VendorMakerNote;

public class AppleMakerNote extends VendorMakerNote
{
    @Override
    public boolean supportsMake(String cameraMake)
    {
        return (cameraMake != null && cameraMake.trim().toLowerCase().contains("apple"));
    }

    @Override
    public List<MetadataInspectionEvent> processMakerNote(EntryIFD entry, MediaRecord record)
    {
        byte[] bytes = entry.getByteArray();
        
        if (bytes == null || bytes.length < 14)
        {
            return Collections.emptyList();
        }

        List<MetadataInspectionEvent> events = new ArrayList<>();

        try
        {
            ByteBuffer buffer = ByteBuffer.wrap(bytes);
            buffer.order(ByteOrder.BIG_ENDIAN);

            if (buffer.remaining() >= 2)
            {
                int numEntries = buffer.getShort() & 0xFFFF;

                for (int i = 0; i < numEntries && buffer.remaining() >= 12; i++)
                {
                    int tagId = buffer.getShort() & 0xFFFF;
                    int dataType = buffer.getShort() & 0xFFFF;
                    long dataCount = buffer.getInt() & 0xFFFFFFFFL;
                    int valueOrOffset = buffer.getInt();

                    String tagName = lookupTagName(tagId);

                    if (tagName != null)
                    {
                        events.add(new MetadataInspectionEvent(record, "Apple", tagName, String.valueOf(valueOrOffset)));
                    }
                }
            }
        }

        catch (Exception exc)
        {
            // Gracefully handle malformed binary streams
        }

        return events;
    }

    private String lookupTagName(int tagId)
    {
        switch (tagId)
        {
            case 0x0001:
                return "MakerNote Version";

            case 0x0003:
                return "Run Time Flags";

            case 0x0004:
                return "Run Time Value";

            case 0x0005:
                return "Color Temperature";

            case 0x0006:
                return "Camera Type";

            default:
                return null;
        }
    }
}