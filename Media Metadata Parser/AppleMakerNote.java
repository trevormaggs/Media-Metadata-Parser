package tif.tagspecs;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import batch.MediaRecord;
import batch.MetadataInspectionEvent;
import tif.DirectoryIFD.EntryIFD;
import tif.DirectoryIdentifier;
import tif.TagHint;

public enum AppleMakerNote implements TagExif_MakerNote
{
    MAKER_NOTE_VERSION(0x0001, "MakerNote Version", TagHint.HINT_SHORT),
    RUN_TIME_FLAGS(0x0003, "Run Time Flags", TagHint.HINT_INTEGER),
    RUN_TIME_VALUE(0x0004, "Run Time Value", TagHint.HINT_INTEGER),
    COLOR_TEMPERATURE(0x0005, "Color Temperature", TagHint.HINT_INTEGER),
    CAMERA_TYPE(0x0006, "Camera Type", TagHint.HINT_STRING);

    private final int numID;
    private final String desc;
    private final TagHint hint;

    AppleMakerNote(int id, String desc, TagHint hint)
    {
        this.numID = id;
        this.desc = desc;
        this.hint = hint;
    }

    @Override
    public int getNumberID()
    {
        return numID;
    }

    @Override
    public String getDescription()
    {
        return desc;
    }

    @Override
    public TagHint getHint()
    {
        return hint;
    }

    @Override
    public DirectoryIdentifier getDirectoryType()
    {
        return DirectoryIdentifier.IFD_DIRECTORY_UNKNOWN;
    }

    @Override
    public String translate(Object val)
    {
        if (this == COLOR_TEMPERATURE)
        {
            int kelvin = Taggable.convertToInt(val);

            if (kelvin > 0)
            {
                return String.format(Locale.US, "%d K", kelvin);
            }
        }

        return TagExif_MakerNote.super.translate(val);
    }

    @Override
    public boolean getCameraMake(String cameraMake)
    {
        return (cameraMake != null && cameraMake.contains("apple"));
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

                    AppleMakerNote matchedTag = null;

                    for (AppleMakerNote tag : AppleMakerNote.values())
                    {
                        if (tag.getNumberID() == tagId)
                        {
                            matchedTag = tag;
                            break;
                        }
                    }

                    if (matchedTag != null)
                    {
                        Object rawValue = extractValue(buffer, dataType, dataCount, valueOrOffset);
                        String translatedValue = matchedTag.translate(rawValue);

                        if (translatedValue != null && !translatedValue.isEmpty())
                        {
                            events.add(new MetadataInspectionEvent(record, "Apple", matchedTag.getDescription(), translatedValue));
                        }
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

    private Object extractValue(ByteBuffer buffer, int dataType, long dataCount, int valueOrOffset)
    {
        switch (dataType)
        {
            case 3:
                return valueOrOffset & 0xFFFF;

            case 4:
                return valueOrOffset;

            default:
                return valueOrOffset;
        }
    }
}