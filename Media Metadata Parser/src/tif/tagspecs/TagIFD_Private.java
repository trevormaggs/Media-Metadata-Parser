package tif.tagspecs;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Arrays;
import common.ByteValueConverter;
import tif.DirectoryIdentifier;
import tif.TagHint;

/**
 * Private, vendor-specific, and industry-standard extended TIFF tags within the
 * {@link DirectoryIdentifier#IFD_ROOT_DIRECTORY} scope.
 *
 * @author Trevor Maggs
 * @version 1.3
 * @since 01 July 2026
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

    @Override
    public int getNumberID()
    {
        return numID;
    }

    @Override
    public DirectoryIdentifier getDirectoryType()
    {
        return DirectoryIdentifier.IFD_ROOT_DIRECTORY;
    }

    @Override
    public TagHint getHint()
    {
        return hint;
    }

    @Override
    public String getDescription()
    {
        return desc;
    }

    @Override
    public String translate(Object val)
    {
        if (this == IFD_PHOTOSHOP_SETTINGS)
        {
            return translatePhotoshopInfo(val);
        }

        return Taggable.super.translate(val);
    }

    public static void displayPhotoshopTags()
    {
        for (int i = 0; i < psdlist.size(); i += 2)
        {
            if (i + 1 < psdlist.size())
            {
                System.out.printf(Locale.ROOT, PHOTOSHOP_ROW_FORMAT, "[Photoshop]", psdlist.get(i), psdlist.get(i + 1));
                System.out.println();
            }
        }

        psdlist.clear();
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

        psdlist.clear();

        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        byte[] signature = "8BIM".getBytes(StandardCharsets.US_ASCII);
        byte[] nextSig = new byte[4];

        buffer.order(ByteOrder.BIG_ENDIAN);

        while (buffer.remaining() >= 12)
        {
            buffer.mark();
            buffer.get(nextSig);

            if (Arrays.equals(nextSig, signature))
            {
                short resID = buffer.getShort();
                int length = buffer.get() & 0xFF;
                int skip = ((length + 1) % 2 == 0 ? 0 : 1);

                buffer.position(buffer.position() + length + skip);

                if (buffer.remaining() < 4)
                {
                    break;
                }

                int dataSize = buffer.getInt();
                int padding = (dataSize % 2 == 0 ? 0 : 1);
                int nextPos = buffer.position() + dataSize + padding;

                if (nextPos > buffer.limit() || dataSize < 1)
                {
                    break;
                }

                else
                {
                    byte[] data = new byte[dataSize];
                    buffer.get(data);

                    // System.out.printf("LOOK: 0x%04X\n", resID);

                    switch (resID)
                    {
                        case 0x03ED: // ResolutionInfo
                            translateResolutionInfo(data);
                        break;

                        case 0x03F3: // PrintFlags
                            translatePrintFlags(data);
                        break;

                        case 0x0404: // IPTCDigest
                            psdlist.add("IPTC Digest");
                            psdlist.add("00000000000000000000000000000000");
                        break;

                        case 0x0409: // Thumbnail (Photoshop 4.0 BGR)
                        case 0x040C: // Thumbnail (Photoshop 5.0+ RGB)
                            translateThumbnailInfo(data);
                        break;

                        case 0x040A: // Copyright flag
                            psdlist.add("Copyright Flag");
                            psdlist.add(data.length > 0 && data[0] == 1 ? "True" : "False");
                        break;

                        case 0x040D: // GlobalAngle
                            translateGlobalAngle(data);
                        break;

                        case 0x0419: // GlobalAltitude
                            translateGlobalAltitude(data);
                        break;

                        case 0x041A: // SlicesInfo
                            translateSlicesInfo(data);
                        break;

                        case 0x041B: // Legacy PrintStyle Fallback
                        // translatePrintStyleFallback(data);
                        break;

                        case 0x041C: // Legacy PrintPosition Fallback
                        // translatePrintPositionFallback(data);
                        break;

                        case 0x041D: // Legacy PrintScale Fallback
                        // translatePrintScaleFallback(data);
                        break;

                        case 0x0421: // VersionInfo
                            translateVersionInfo(data);
                        break;

                        case 0x0426: // Modern PrintScaleInfo (Unified Layout Block)
                            translatePrintScaleInfo(data);
                        break;

                        case 0x0428: // PixelAspectRatio
                            translatePixelAspectRatio(data);
                        break;

                        case 0x2710: // PrintFlagsInfo
                            translatePrintFlagsInfo(data);
                        break;

                        default:
                        break;
                    }

                    buffer.position(nextPos);
                }
            }

            else
            {
                buffer.reset();
                buffer.get();
            }
        }

        return "";
    }

    /*
     * 0x0426 (1062) — PrintScaleInfo: This block unified print scale and placement layouts into a
     * single structural block. It is a 14-byte continuous block:
     *
     * 2 bytes (short): Style (0 = Center, 1 = Scale to fit, 2 = User Defined)
     * 4 bytes (float): X Position (Offset mapping)
     * 4 bytes (float): Y Position (Offset mapping)
     * 4 bytes (float): Scale Factor
     */
    private static void translateResolutionInfo(byte[] data)
    {
        if (data == null || data.length < 16) return;

        /*
         * For hRes and vRes, we are not interested in the fractional part, so we only obtain the
         * upper 2 bytes of the original 32-bit Fixed point number (16-bit integer part followed by
         * a 16-bit fractional part). Hence the use of toUnsignedShort(data, 0, ...).
         */

        int hRes = ByteValueConverter.toUnsignedShort(data, 0, ByteOrder.BIG_ENDIAN);
        int hResUnit = ByteValueConverter.toUnsignedShort(data, 4, ByteOrder.BIG_ENDIAN);
        int vRes = ByteValueConverter.toUnsignedShort(data, 8, ByteOrder.BIG_ENDIAN);
        int vResUnit = ByteValueConverter.toUnsignedShort(data, 12, ByteOrder.BIG_ENDIAN);

        psdlist.add("X Resolution");
        psdlist.add(String.valueOf(hRes));
        psdlist.add("Displayed Units X");
        psdlist.add(hResUnit == 1 ? "inches" : "cm");

        psdlist.add("Y Resolution");
        psdlist.add(String.valueOf(vRes));
        psdlist.add("Displayed Units Y");
        psdlist.add(vResUnit == 1 ? "inches" : "cm");
    }

    private static void translatePrintFlags(byte[] data)
    {
        if (data != null && data.length >= 9)
        {
            String[] labels = {
                    "Print Labels",
                    "Print Crop Marks",
                    "Print Color Bars",
                    "Print Registration Marks",
                    "Print Negative",
                    "Print Flip",
                    "Print Interpolate",
                    "Print Caption",
                    "Print Flags"
            };

            for (int i = 0; i < labels.length; i++)
            {
                String status = (data[i] == 1) ? "True" : "False";

                psdlist.add(labels[i]);
                psdlist.add(status);
            }
        }
    }

    /*
     * 4-byte int — Format (1 = JPEG-compressed, 0 = Uncompressed Raw)
     * 4-byte int — Width (in pixels)
     * 4-byte int — Height (in pixels)
     * 4-byte int — Widthbytes (Padded calculation for scanning row size)
     * 4-byte int — Total Size
     * 4-byte int — Compressed Size
     * 2-byte short — Bits per Pixel (Always 24)
     * 2-byte short — Number of Planes (Always 1)
     * Variable Bytes — Binary Preview Data
     */
    private static void translateThumbnailInfo(byte[] data)
    {
        if (data != null && data.length >= 28)
        {
            int format = ByteValueConverter.toInteger(data, 0, ByteOrder.BIG_ENDIAN);
            int width = ByteValueConverter.toInteger(data, 4, ByteOrder.BIG_ENDIAN);
            int height = ByteValueConverter.toInteger(data, 8, ByteOrder.BIG_ENDIAN);
            int widthBytes = ByteValueConverter.toInteger(data, 12, ByteOrder.BIG_ENDIAN);
            int totalSize = ByteValueConverter.toInteger(data, 16, ByteOrder.BIG_ENDIAN);
            int compressedSize = ByteValueConverter.toInteger(data, 20, ByteOrder.BIG_ENDIAN);
            short bitsPerPixel = ByteValueConverter.toShort(data, 24, ByteOrder.BIG_ENDIAN);
            short numberOfPlanes = ByteValueConverter.toShort(data, 26, ByteOrder.BIG_ENDIAN);

            psdlist.add("Thumbnail Format");
            psdlist.add(format == 1 ? "JPEG-compressed" : "Uncompressed Raw");

            psdlist.add("Thumbnail Width");
            psdlist.add(String.valueOf(width));

            psdlist.add("Thumbnail Height");
            psdlist.add(String.valueOf(height));

            psdlist.add("Thumbnail Widthbytes");
            psdlist.add(String.valueOf(widthBytes));

            psdlist.add("Thumbnail Total Size");
            psdlist.add(String.valueOf(totalSize));

            psdlist.add("Thumbnail Compressed Size");
            psdlist.add(String.valueOf(compressedSize));

            psdlist.add("Thumbnail Bits Per Pixel");
            psdlist.add(String.valueOf(bitsPerPixel));

            psdlist.add("Thumbnail Planes");
            psdlist.add(String.valueOf(numberOfPlanes));
        }
    }

    private static void translateGlobalAngle(byte[] data)
    {
        if (data != null && data.length >= 4)
        {
            int angle = ByteValueConverter.toInteger(data, 0, ByteOrder.BIG_ENDIAN);

            psdlist.add("Global Angle");
            psdlist.add(angle + "°");
        }
    }

    private static void translateGlobalAltitude(byte[] data)
    {
        if (data != null && data.length >= 4)
        {
            int altitude = ByteValueConverter.toInteger(data, 0, ByteOrder.BIG_ENDIAN);

            psdlist.add("Global Altitude");
            psdlist.add(altitude + "°");
        }
    }

    private static void translateSlicesInfo(byte[] data)
    {
        if (data != null && data.length >= 24)
        {
            /*
             * int version = ByteValueConverter.toInteger(data, 0, ByteOrder.BIG_ENDIAN);
             * int top = ByteValueConverter.toInteger(data, 4, ByteOrder.BIG_ENDIAN);
             * int left = ByteValueConverter.toInteger(data, 8, ByteOrder.BIG_ENDIAN);
             * int bottom = ByteValueConverter.toInteger(data, 12, ByteOrder.BIG_ENDIAN);
             * int right = ByteValueConverter.toInteger(data, 16, ByteOrder.BIG_ENDIAN);
             */
            int nameLength = ByteValueConverter.toInteger(data, 20, ByteOrder.BIG_ENDIAN);

            /*
             * Because the actual string starts from Byte 24, we need to multiply
             * the length by two to support UTF-16 formats (2 bytes per character),
             * then extract the UTF-16BE string from offset 24.
             */
            int stringLength = nameLength * 2;

            if (data.length >= stringLength + 24)
            {
                String slicesGroupName = new String(data, 24, stringLength, StandardCharsets.UTF_16BE).trim();

                psdlist.add("Slices Group Name");
                psdlist.add(slicesGroupName.isEmpty() ? "None" : slicesGroupName);

                /* NumSlices (4 bytes) starts after the first string */
                int nextOffset = 24 + stringLength;

                if (data.length >= nextOffset + 4)
                {
                    int numSlices = ByteValueConverter.toInteger(data, nextOffset, ByteOrder.BIG_ENDIAN);

                    psdlist.add("Number of Slices");
                    psdlist.add(String.valueOf(numSlices));
                }
            }
        }
    }

    private static void translatePrintStyleFallback(byte[] data)
    {
        if (data == null || data.length < 2 || psdlist.contains("Print Style")) return;

        int colorHandling = ByteValueConverter.toUnsignedShort(data, 0, ByteOrder.BIG_ENDIAN);
        psdlist.add("Print Style");
        psdlist.add(colorHandling == 1 ? "Centered" : "Normal");
    }

    private static void translatePrintPositionFallback(byte[] data)
    {
        if (data == null || data.length < 8 || psdlist.contains("Print Position")) return;

        float xPosition = ByteValueConverter.toFloat(data, 0, ByteOrder.BIG_ENDIAN);
        float yPosition = ByteValueConverter.toFloat(data, 4, ByteOrder.BIG_ENDIAN);

        psdlist.add("Print Position");
        psdlist.add(String.format(Locale.ROOT, "%.0f %.0f", xPosition, yPosition));
    }

    private static void translatePrintScaleFallback(byte[] data)
    {
        if (data == null || data.length < 14 || psdlist.contains("Print Scale")) return;

        float scale = ByteValueConverter.toFloat(data, 10, ByteOrder.BIG_ENDIAN);
        psdlist.add("Print Scale");
        psdlist.add(String.format(Locale.ROOT, "%g", scale).replace(".00000", ""));
    }

    /*
     * 4 bytes (int): File Version (1)
     * 
     * 1 byte (boolean): hasRealMergedData (1 or 0)
     * 
     * Variable bytes: Writer Name String.
     * 
     * Structure: A 4-byte string character length counter, followed by that many characters of
     * standard UTF-16BE data.
     * 
     * Variable bytes: Reader Name String.
     * 
     * Structure: A 4-byte string character length counter, followed by that many characters of
     * standard UTF-16BE data.
     * 
     * 4 bytes (int): Application File Version.
     */
    private static void translateVersionInfo(byte[] data)
    {
        if (data != null && data.length >= 13)
        {
            // 1. Version (4 bytes)
            int version = ByteValueConverter.toInteger(data, 0, ByteOrder.BIG_ENDIAN);

            // 2. Has Real Merged Data (1 byte)
            boolean hasMergedData = (data[4] == 1);

            psdlist.add("Has Real Merged Data");
            psdlist.add(hasMergedData ? "Yes" : "No");

            int offset = 5;

            // 3. Extract Writer Name
            if (data.length >= offset + 4)
            {
                int writerLen = ByteValueConverter.toInteger(data, offset, ByteOrder.BIG_ENDIAN);

                offset += 4;

                int byteLen = writerLen * 2; // UTF-16BE uses 2 bytes per character

                if (data.length >= offset + byteLen)
                {
                    String writerName = new String(data, offset, byteLen, StandardCharsets.UTF_16BE).trim();

                    psdlist.add("Writer Name");
                    psdlist.add(writerName.isEmpty() ? "Unknown" : writerName);

                    offset += byteLen;
                }
            }

            // 4. Extract Reader Name
            if (data.length >= offset + 4)
            {
                int readerLen = ByteValueConverter.toInteger(data, offset, ByteOrder.BIG_ENDIAN);

                offset += 4;

                int byteLen = readerLen * 2;

                if (data.length >= offset + byteLen)
                {
                    String readerName = new String(data, offset, byteLen, StandardCharsets.UTF_16BE).trim();

                    psdlist.add("Reader Name");
                    psdlist.add(readerName.isEmpty() ? "Unknown" : readerName);

                    offset += byteLen;
                }
            }

            // 5. Final Application File Version (4 bytes)
            if (data.length >= offset + 4)
            {
                int fileVersion = ByteValueConverter.toInteger(data, offset, ByteOrder.BIG_ENDIAN);

                String majorVersion;
                
                switch (fileVersion)
                {
                    case 1:
                        majorVersion = "Photoshop 7.0 or earlier";
                    break;

                    case 2:
                        majorVersion = "Photoshop CS (8.0)";
                    break;

                    case 3:
                        majorVersion = "Photoshop CS2 (9.0)";
                    break;

                    case 4:
                        majorVersion = "Photoshop CS3 (10.0)";
                    break; // (Matches babygemma.tif!)

                    case 5:
                        majorVersion = "Photoshop CS4 (11.0)";
                    break;

                    case 6:
                        majorVersion = "Photoshop CS5 (12.0)";
                    break;

                    case 7:
                        majorVersion = "Photoshop CS6 (13.0)";
                    break;

                    case 8:
                        majorVersion = "Photoshop CC (14.0)";
                    break;

                    // Later versions continue to increment or stick to a baseline version behavior
                    default:
                        majorVersion = "Photoshop (Internal Version " + fileVersion + ")";
                    break;
                }

                psdlist.add("Application File Version");
                psdlist.add(majorVersion);
            }
        }
    }

    private static void translatePrintScaleInfo(byte[] data)
    {
        if (data != null && data.length >= 14)
        {
            int styleType = ByteValueConverter.toUnsignedShort(data, 0, ByteOrder.BIG_ENDIAN);
            float xPos = ByteValueConverter.toFloat(data, 2, ByteOrder.BIG_ENDIAN);
            float yPos = ByteValueConverter.toFloat(data, 6, ByteOrder.BIG_ENDIAN);
            float scale = ByteValueConverter.toFloat(data, 10, ByteOrder.BIG_ENDIAN);
            String styleStr = (styleType == 0 ? "Centered" : (styleType == 1 ? "Size to Fit" : "User Defined"));

            psdlist.add("Print Style");
            psdlist.add(styleStr);
            psdlist.add("Print Position");
            psdlist.add(String.format(Locale.ROOT, "%.0f %.0f", xPos, yPos));
            psdlist.add("Print Scale");
            psdlist.add(String.format(Locale.ROOT, "%g", scale).replace(".00000", ""));
        }
    }

    /*
     * Byte Offset Data Type Field Name Description
     * 
     * 0 – 34-byte int Version Always equal to 1 or 2.
     * 
     * 4 – 118-byte double AspectRatio A 64-bit IEEE-754 double-precision floating-point value
     * specifying the aspect
     */
    private static void translatePixelAspectRatio(byte[] data)
    {
        if (data != null && data.length >= 12)
        {
            // Skip the 4-byte version/flag header in data (offsets 0-3) and read the 8-byte double
            // (offsets 4-11)
            double aspect = ByteValueConverter.toDouble(data, 4, ByteOrder.BIG_ENDIAN);

            // Formats whole integers without trailing zeros (e.g., 2 instead of 2.0)
            String aspectStr = (aspect == (long) aspect) ? String.format(Locale.ROOT, "%d", (long) aspect) : String.valueOf(aspect);

            psdlist.add("Pixel Aspect Ratio");
            psdlist.add(aspectStr);
        }
    }

    private static void translatePrintFlagsInfo(byte[] data)
    {
        // Lower boundary gate to 10 bytes captures legacy truncated structures safely
        if (data != null && data.length >= 10)
        {
            short version = ByteValueConverter.toShort(data, 0, ByteOrder.BIG_ENDIAN);
            long bleedBits = ByteValueConverter.toLong(data, 2, ByteOrder.BIG_ENDIAN);
            double bleedWidth = Double.longBitsToDouble(bleedBits);

            bleedWidth = bleedWidth + 0.0;

            psdlist.add("Print Flags Info Version");
            psdlist.add(String.valueOf(version));

            short bleedScale = 0;
            String unitLabel = "points (default)"; // Fallback structure configuration label

            // Safely extract bleedScale only if the 2 bytes at offsets 10-11 are in the stream
            // payload
            if (data.length >= 12)
            {
                bleedScale = ByteValueConverter.toShort(data, 10, ByteOrder.BIG_ENDIAN);

                switch (bleedScale)
                {
                    case 0:
                        unitLabel = "points (default)";
                    break;

                    case 1:
                        unitLabel = "inches";
                    break;

                    case 2:
                        unitLabel = "cm";
                    break;

                    case 3:
                        unitLabel = "points";
                    break;

                    case 4:
                        unitLabel = "picas";
                    break;

                    case 5:
                        unitLabel = "columns";
                    break;

                    default:
                        unitLabel = "unknown (" + bleedScale + ")";
                    break;
                }
            }

            // Only parse out centerCropMarks if the block has those extra trailing bytes
            if (data.length >= 14)
            {
                boolean centerCropMarks = (data[12] == 1);
                psdlist.add("Center Crop Marks");
                psdlist.add(centerCropMarks ? "True" : "False");
            }

            psdlist.add("Print Bleed Width");
            psdlist.add(String.format(Locale.ROOT, "%.2f %s", bleedWidth, unitLabel));
        }
    }
}