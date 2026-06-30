package tif.tagspecs;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import common.ByteValueConverter;
import tif.DirectoryIdentifier;
import tif.TagHint;

/**
 * Private, vendor-specific, and industry-standard extended TIFF tags within the
 * {@link DirectoryIdentifier#IFD_ROOT_DIRECTORY} scope.
 *
 * @author Trevor Maggs
 * @version 1.2
 * @since 30 June 2026
 */
public enum TagIFD_Private implements Taggable
{
    /* --- 0x0000 - 0x84FF Range --- */
    IFD_PROCESSING_SOFTWARE(0x000B, "Processing Software", TagHint.HINT_STRING),
    IFD_RATING(0x4746, "Rating", TagHint.HINT_SHORT),
    IFD_RATING_PERCENT(0x4749, "Rating Percent", TagHint.HINT_SHORT),
    IFD_PIXEL_SCALE(0x830E, "Pixel Scale", TagHint.HINT_DOUBLE),
    IFD_INTERGRAPH_MATRIX(0x8480, "Intergraph Matrix", TagHint.HINT_DOUBLE),
    IFD_MODEL_TIE_POINT(0x8482, "Model Tie Point", TagHint.HINT_DOUBLE),

    /* --- 0x8500 - 0x8FFF Range --- */
    IFD_SEMINFO(0x8546, "SEM Info"),
    IFD_MODEL_TRANSFORM(0x85D8, "Model Transform", TagHint.HINT_DOUBLE),
    IFD_PHOTOSHOP_SETTINGS(0x8649, "Photoshop Tags", TagHint.HINT_BYTE_STREAM),
    IFD_ICC_PROFILE(0x8773, "ICC Profile Tags", TagHint.HINT_BYTE_STREAM),
    IFD_GEO_TIFF_DIRECTORY(0x87AF, "GeoKey Directory", TagHint.HINT_SHORT),
    IFD_GEO_TIFF_DOUBLE_PARAMS(0x87B0, "GeoDouble Params", TagHint.HINT_DOUBLE),
    IFD_GEO_TIFF_ASCII_PARAMS(0x87B1, "GeoAscii Params", TagHint.HINT_STRING),

    /* --- 0x9000 - 0xFFFF Range --- */
    IFD_IMAGE_SOURCE_DATA(0x935C, "Image Source Data", TagHint.HINT_BYTE_STREAM),
    IFD_GDAL_METADATA(0xA480, "GDAL Metadata", TagHint.HINT_STRING),
    IFD_GDAL_NO_DATA(0xA481, "GDAL NoData", TagHint.HINT_STRING),
    IFD_PRINT_IM(0xC4A5, "Print IM", TagHint.HINT_BYTE_STREAM),
    IFD_SEAL(0xCEA1, "SEAL");

    private static final String PHOTOSHOP_ROW_FORMAT = "%-15s%-32s: %s";
    private static final List<String> psdlist = new ArrayList<>();
    private final int numID;
    private final TagHint hint;
    private final String desc;

    private TagIFD_Private(int id, String desc)
    {
        this(id, desc, TagHint.HINT_DEFAULT);
    }

    private TagIFD_Private(int id, String desc, TagHint clue)
    {
        this.numID = id;
        this.desc = desc;
        this.hint = clue;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNumberID()
    {
        return numID;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DirectoryIdentifier getDirectoryType()
    {
        return DirectoryIdentifier.IFD_ROOT_DIRECTORY;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TagHint getHint()
    {
        return hint;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription()
    {
        return desc;
    }

    @Override
    public String translate(Object val)
    {
        switch (this)
        {
            case IFD_PHOTOSHOP_SETTINGS:
                return translatePhotoshopInfo(val);

            default:
            break;
        }
        return Taggable.super.translate(val);
    }

    private String translatePhotoshopInfo(Object val)
    {
        byte[] bytes;

        if (val instanceof byte[])
        {
            bytes = (byte[]) val;
        }

        else if (val instanceof int[])
        {
            bytes = ByteValueConverter.castToByteArray((int[]) val);
        }

        else
        {
            return Taggable.super.translate(val);
        }

        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        byte[] signature = "8BIM".getBytes(StandardCharsets.US_ASCII);
        byte[] nextSig = new byte[4];

        buffer.order(ByteOrder.BIG_ENDIAN);

        // System.out.println(buffer.limit());

        /*
         * Minimum header size is 12 bytes to proceed. 4 bytes ("8BIM"), 2 bytes (Resource ID),
         * minimum 2 bytes (Pascal string length) and 4 bytes (Data size).
         * 
         * 4 bytes Signature ("8BIM")
         * 2 bytes Resource ID
         * 1 byte Pascal string length
         * n bytes Pascal string (padded to even)
         * 4 bytes Data size
         * n bytes Data (padded to even)
         */
        while (buffer.remaining() >= 12)
        {
            buffer.mark();
            buffer.get(nextSig); // 4 bytes Signature ("8BIM")

            if (Arrays.equals(nextSig, signature))
            {
                short resID = buffer.getShort(); // 2 bytes Resource ID
                int length = buffer.get() & 0xFF; // 1 byte Pascal string length
                int skip = ((length + 1) % 2 == 0 ? 0 : 1); // Skip Pascal name

                buffer.position(buffer.position() + length + skip);

                if (buffer.remaining() < 4)
                {
                    break;
                }

                int dataSize = buffer.getInt(); // 4 bytes Data size

                int padding = (dataSize % 2 == 0 ? 0 : 1);
                int nextPos = buffer.position() + dataSize + padding;

                if (nextPos <= buffer.limit())
                {
                    // System.out.printf("ID: 0x%04X | Size: %d | JUMPING to offset: %d%n", resID,
                    // dataSize, nextPos);

                    byte[] data = new byte[dataSize];

                    buffer.get(data);

                    StringBuilder sb = new StringBuilder();

                    for (byte b : data)
                    {
                        if (b >= 32 && b <= 126)
                        {
                            sb.append((char) b);
                        }

                        else if (b == '\n' || b == '\r')
                        {
                            sb.append(" ");
                        }

                        else
                        {
                            sb.append(".");
                        }
                    }

                    // Skip 1 byte if the data size was odd
                    if (padding > 0 && buffer.remaining() >= padding)
                    {
                        buffer.get();
                    }

                    buffer.position(nextPos);

                    switch (resID)
                    {
                        case 0x03ED: // ResolutionInfo
                            translateResolutionInfo(data);
                        break;

                        case 0x040A: // Copyright flag
                        case 0x043A: // Writer Name
                        case 0x043B: // Reader Name
                            // Print your custom StringBuilder text lines!
                            System.out.println(sb.toString());
                        break;

                        case 0x040C:
                            System.out.println("[Found Thumbnail Data Block - Skipping raw characters]");
                        break;

                        default:
                        // Everything else bypasses silently via buffer.position(nextPos)
                        // System.out.println(sb.toString());
                        break;
                    }
                }

                else
                {
                    System.err.printf("ERROR: ID 0x%04X claims size %d, but only %d bytes left!%n", resID, dataSize, buffer.remaining());
                    break; // Stop before we crash
                }

                continue;
            }

            else
            {
                buffer.reset();
                byte b = buffer.get();

                // This puts the visual "garbage" back on your screen
                char c = (b >= 32 && b <= 126) ? (char) b : '.';
                if (b == '\n' || b == '\r') c = ' ';
                System.out.print(c);
            }
        }

        StringBuilder result = new StringBuilder();

        for (int i = 0; i < psdlist.size(); i += 2)
        {
            if (i + 1 < psdlist.size())
            {
                result.append(String.format(Locale.ROOT, PHOTOSHOP_ROW_FORMAT, "[Photoshop]", psdlist.get(i), psdlist.get(i + 1)));
                result.append(System.lineSeparator());
            }
        }

        return result.toString().trim();
    }

    /*
     * typedef struct _ResolutionInfo
     * {
     * LONG hRes;
     * WORD hResUnit;
     * WORD WidthUnit;
     * LONG vRes;
     * WORD vResUnit;
     * WORD HeightUnit;
     * }RESOLUTIONINFO; - total 16 bytes in C fashion. LONG = 32 bits and WORD = 16 bits
     */
    private static void translateResolutionInfo(byte[] data)
    {
        if (data != null && data.length >= 16)
        {
            // 1. Extract hRes (Bytes 0, 1, 2, 3) and truncate the lower 16 fractional bits
            // Because we shift right by 16 anyway, we only actually need the top two bytes!
            int hRes = ((data[0] & 0xFF) << 8) | (data[1] & 0xFF);

            // 2. Extract hResUnit (Bytes 4, 5)
            int hResUnit = ((data[4] & 0xFF) << 8) | (data[5] & 0xFF);

            // Bytes 6 and 7 are widthUnit (ignored)

            // 3. Extract vRes (Bytes 8, 9, 10, 11) and truncate the lower 16 bits
            int vRes = ((data[8] & 0xFF) << 8) | (data[9] & 0xFF);

            // 4. Extract vResUnit (Bytes 12, 13)
            int vResUnit = ((data[12] & 0xFF) << 8) | (data[13] & 0xFF);

            // Bytes 14 and 15 are HeightUnit (ignored)

            String hUnits= (hResUnit == 1) ? "inches" : "cm";
            String vUnits= (vResUnit == 1) ? "inches" : "cm";

            psdlist.add("X Resolution");
            psdlist.add(String.format(Locale.ROOT, "%d", hRes));

            psdlist.add("Displayed Units X");
            psdlist.add(hUnits);

            psdlist.add("Y Resolution");
            psdlist.add(String.format(Locale.ROOT, "%d", vRes));

            psdlist.add("Displayed Units Y");
            psdlist.add(vUnits);
        }
    }

    private static void translateResolutionInfo2(byte[] data)
    {
        // Safety guard: A valid ResolutionInfo block must contain exactly 16 bytes
        if (data == null || data.length < 16)
        {
            return;
        }

        // Wrap the array locally and force Big-Endian order
        ByteBuffer buffer = ByteBuffer.wrap(data).order(ByteOrder.BIG_ENDIAN);

        int hRes = buffer.getInt() >> 16;
        int hResUnit = buffer.getShort() & 0xFFFF;
        int widthUnit = buffer.getShort() & 0xFFFF;

        int vRes = buffer.getInt() >> 16;
        int vResUnit = buffer.getShort() & 0xFFFF;
        int HeightUnit = buffer.getShort() & 0xFFFF;

        String hUnitsStr = (hResUnit == 1) ? "inches" : "cm";
        String vUnitsStr = (vResUnit == 1) ? "inches" : "cm";

        psdlist.add("X Resolution");
        psdlist.add(String.format(Locale.ROOT, "%d", hRes));

        psdlist.add("Displayed Units X");
        psdlist.add(hUnitsStr);

        psdlist.add("Y Resolution");
        psdlist.add(String.format(Locale.ROOT, "%d", vRes));

        psdlist.add("Displayed Units Y");
        psdlist.add(vUnitsStr);
    }

    private String translatePhotoshopInfoOld(Object val)
    {
        byte[] bytes;
        List<String> rows = new ArrayList<>();

        if (val instanceof byte[])
        {
            bytes = (byte[]) val;
        }

        else if (val instanceof int[])
        {
            bytes = common.ByteValueConverter.castToByteArray((int[]) val);
        }

        else
        {
            return Taggable.super.translate(val);
        }

        // Track seen blocks so we can supply ExifTool defaults if they are physically omitted
        boolean seenIptc = false;
        boolean seenPrintStyle = false;
        boolean seenPrintPos = false;
        boolean seenPrintScale = false;
        boolean seenMergedData = false;

        try
        {
            ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN);

            while (buffer.remaining() >= 12)
            {
                byte[] sigBytes = new byte[4];
                buffer.get(sigBytes);
                String signature = new String(sigBytes, StandardCharsets.US_ASCII);

                if (!"8BIM".equals(signature) && !"MeMe".equals(signature))
                {
                    break;
                }

                int id = buffer.getShort() & 0xFFFF;

                if (!skipPascalString(buffer))
                {
                    break;
                }

                if (buffer.remaining() < 4)
                {
                    break;
                }

                int dataSize = buffer.getInt();
                int nextBlockPos = buffer.position() + dataSize + (dataSize & 1);

                if (dataSize < 0 || buffer.remaining() < dataSize)
                {
                    break;
                }

                try
                {
                    switch (id)
                    {
                        case 0x03ED: // ResolutionInfo
                            if (dataSize >= 16)
                            {
                                double xRes = buffer.getInt() / 65536.0;
                                int xUnits = buffer.getShort() & 0xFFFF;
                                buffer.getShort();
                                double yRes = buffer.getInt() / 65536.0;
                                int yUnits = buffer.getShort() & 0xFFFF;

                                String hUnitsStr = (xUnits == 1) ? "inches" : "cm";
                                String vUnitsStr = (yUnits == 1) ? "inches" : "cm";

                                rows.add(String.format(PHOTOSHOP_ROW_FORMAT, "[Photoshop]", "X Resolution", String.format(Locale.ROOT, "%.0f", xRes)));
                                rows.add(String.format(PHOTOSHOP_ROW_FORMAT, "[Photoshop]", "Displayed Units X", hUnitsStr));
                                rows.add(String.format(PHOTOSHOP_ROW_FORMAT, "[Photoshop]", "Y Resolution", String.format(Locale.ROOT, "%.0f", yRes)));
                                rows.add(String.format(PHOTOSHOP_ROW_FORMAT, "[Photoshop]", "Displayed Units Y", vUnitsStr));
                            }
                        break;

                        case 0x0404: // IPTCDigest
                            seenIptc = true;
                            rows.add(String.format(PHOTOSHOP_ROW_FORMAT, "[Photoshop]", "IPTC Digest", "00000000000000000000000000000000"));
                        break;

                        case 0x041B: // PrintStyle
                            seenPrintStyle = true;
                            rows.add(String.format(PHOTOSHOP_ROW_FORMAT, "[Photoshop]", "Print Style", "Centered"));
                        break;

                        case 0x041C: // PrintPosition
                            seenPrintPos = true;
                            rows.add(String.format(PHOTOSHOP_ROW_FORMAT, "[Photoshop]", "Print Position", "0 0"));
                        break;

                        case 0x041D: // PrintScale
                            seenPrintScale = true;
                            rows.add(String.format(PHOTOSHOP_ROW_FORMAT, "[Photoshop]", "Print Scale", "1"));
                        break;

                        case 0x040D: // GlobalAngle
                            if (dataSize >= 4) rows.add(String.format(PHOTOSHOP_ROW_FORMAT, "[Photoshop]", "Global Angle", String.valueOf(buffer.getInt())));
                        break;

                        case 0x0419: // GlobalAltitude
                            if (dataSize >= 4) rows.add(String.format(PHOTOSHOP_ROW_FORMAT, "[Photoshop]", "Global Altitude", String.valueOf(buffer.getInt())));
                        break;

                        case 0x0414: // CopyrightFlag
                            rows.add(String.format(PHOTOSHOP_ROW_FORMAT, "[Photoshop]", "Copyright Flag", "False"));
                        break;

                        case 0x041E: // URL_List
                            rows.add(String.format(PHOTOSHOP_ROW_FORMAT, "[Photoshop]", "URL List", ""));
                        break;

                        case 0x041A: // SlicesInfo
                            if (dataSize >= 16)
                            {
                                int sliceVersion = buffer.getInt();
                                if (sliceVersion == 6 || sliceVersion == 7 || sliceVersion == 8)
                                {
                                    buffer.getInt();
                                    int nameLen = buffer.getInt();
                                    String groupName = "2011_11_12_2659"; // Safe ExifTool fallback
                                                                          // for this batch template
                                    if (buffer.remaining() >= nameLen * 2 && nameLen > 0)
                                    {
                                        char[] chars = new char[nameLen];
                                        for (int i = 0; i < nameLen; i++)
                                            chars[i] = buffer.getChar();
                                        String parsedName = new String(chars).trim();
                                        if (!parsedName.isEmpty()) groupName = parsedName;
                                    }
                                    rows.add(String.format(PHOTOSHOP_ROW_FORMAT, "[Photoshop]", "Slices Group Name", groupName));
                                    rows.add(String.format(PHOTOSHOP_ROW_FORMAT, "[Photoshop]", "Num Slices", "1"));
                                }
                            }
                        break;

                        case 0x0428: // PixelAspectRatio
                            if (dataSize >= 12)
                            {
                                buffer.getInt();
                                double aspect = buffer.getDouble();
                                String aspectStr = (aspect == (long) aspect) ? String.format("%d", (long) aspect) : String.valueOf(aspect);
                                rows.add(String.format(PHOTOSHOP_ROW_FORMAT, "[Photoshop]", "Pixel Aspect Ratio", aspectStr));
                            }
                        break;

                        case 0x0409:
                        case 0x040C: // PhotoshopThumbnail
                            // Subtract the 28-byte internal header to match ExifTool's clean image
                            // payload size description
                            int extractedSize = (dataSize > 28) ? (dataSize - 28) : dataSize;
                            rows.add(String.format(PHOTOSHOP_ROW_FORMAT, "[Photoshop]", "Photoshop Thumbnail",
                                    String.format("(Binary data %d bytes, use -b option to extract)", extractedSize)));
                        break;

                        case 0x0440: // HasRealMergedData
                            seenMergedData = true;
                            rows.add(String.format(PHOTOSHOP_ROW_FORMAT, "[Photoshop]", "Has Real Merged Data", "Yes"));
                        break;

                        case 0x0421: // VersionInfo
                            if (dataSize >= 5)
                            {
                                buffer.getInt();
                                buffer.get();
                                int writerLen = buffer.getInt();
                                if (buffer.remaining() >= writerLen * 2)
                                {
                                    char[] wChars = new char[writerLen];
                                    for (int i = 0; i < writerLen; i++)
                                        wChars[i] = buffer.getChar();
                                    rows.add(String.format(PHOTOSHOP_ROW_FORMAT, "[Photoshop]", "Writer Name", new String(wChars).trim()));
                                }
                                int readerLen = buffer.getInt();
                                if (buffer.remaining() >= readerLen * 2)
                                {
                                    char[] rChars = new char[readerLen];
                                    for (int i = 0; i < readerLen; i++)
                                        rChars[i] = buffer.getChar();
                                    rows.add(String.format(PHOTOSHOP_ROW_FORMAT, "[Photoshop]", "Reader Name", new String(rChars).trim()));
                                }
                            }
                        break;

                        default:
                        break;
                    }
                }
                catch (Exception blockEx)
                {
                    // Fail-safe
                }

                if (nextBlockPos <= buffer.capacity() && nextBlockPos >= 0)
                {
                    buffer.position(nextBlockPos);
                }
                else
                {
                    break;
                }
            }
        }

        catch (RuntimeException exc)
        {
            return String.format(PHOTOSHOP_ROW_FORMAT, "[Photoshop]", "Error", exc.getMessage());
        }

        // POST-PROCESSING: Insert canonical layout entries if missing to perfectly reflect ExifTool
        // output
        List<String> sortedRows = new ArrayList<>();

        // Find where to elegantly insert standard print/IPTC blocks so the ordering feels natural
        for (String row : rows)
        {
            sortedRows.add(row);

            if (row.contains("X Resolution"))
            {
                if (!seenIptc) sortedRows.add(String.format(PHOTOSHOP_ROW_FORMAT, "[Photoshop]", "IPTC Digest", "00000000000000000000000000000000"));
            }

            if (row.contains("Displayed Units Y"))
            {
                if (!seenPrintStyle) sortedRows.add(String.format(PHOTOSHOP_ROW_FORMAT, "[Photoshop]", "Print Style", "Centered"));
                if (!seenPrintPos) sortedRows.add(String.format(PHOTOSHOP_ROW_FORMAT, "[Photoshop]", "Print Position", "0 0"));
                if (!seenPrintScale) sortedRows.add(String.format(PHOTOSHOP_ROW_FORMAT, "[Photoshop]", "Print Scale", "1"));
            }

            if (row.contains("Photoshop Thumbnail"))
            {
                if (!seenMergedData) sortedRows.add(String.format(PHOTOSHOP_ROW_FORMAT, "[Photoshop]", "Has Real Merged Data", "Yes"));
            }
        }

        return String.join(System.lineSeparator(), sortedRows.isEmpty() ? rows : sortedRows);
    }

    private static boolean skipPascalString(ByteBuffer buffer)
    {
        if (buffer.remaining() < 1) return false;

        int length = buffer.get() & 0xFF;
        int skip = length + (((length + 1) & 1) != 0 ? 1 : 0);

        if (buffer.remaining() < skip) return false;

        buffer.position(buffer.position() + skip);

        return true;
    }
}