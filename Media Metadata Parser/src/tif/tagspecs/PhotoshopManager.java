package tif.tagspecs;

import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;
import common.ByteValueConverter;
import common.PropertyDisplay;
import common.binary.ByteArrayReader;

/**
 * Utility class that converts Photoshop Image Resource Block (IRB) metadata into ExifTool-style
 * human-readable representations.
 *
 * <p>
 * This class formats halftoning layouts, image resolution metrics, alpha channels, print settings,
 * and other Photoshop-specific metadata into descriptive text suitable for display.
 * </p>
 *
 * @author Trevor Maggs
 * @version 1.5
 * @since 7 July 2026
 */
public final class PhotoshopManager
{
    private PhotoshopManager()
    {
        throw new UnsupportedOperationException("Not intended for instantiation");
    }

    /**
     * Decodes raw Photoshop Image Resource Block (IRB) metadata into human-readable properties.
     *
     * @param val
     *        the raw Photoshop resource value
     * @param display
     *        the callback receiving each decoded property
     */
    public static void decodePhotoshopProperties(Object val, PropertyDisplay display)
    {
        byte[] bytes;
        byte[] signature = "8BIM".getBytes(StandardCharsets.US_ASCII);

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
            return;
        }

        try (ByteArrayReader reader = new ByteArrayReader(bytes, ByteOrder.BIG_ENDIAN))
        {
            /*
             * Minimum of 12 bytes is required, including 4 bytes signature,
             * 2 bytes resource ID, min 2 bytes pascal name string, 4 bytes data size.
             */
            while (reader.hasRemaining(12))
            {
                byte[] nextSig = reader.peek(reader.getCurrentPosition(), 4);

                if (Arrays.equals(nextSig, signature))
                {
                    reader.readBytes(4); // Skip signature

                    short resID = reader.readShort(); // Resource ID - 2 bytes
                    int length = reader.readUnsignedByte(); // Length - 1 byte
                    int skip = (length % 2 == 0 ? 1 : 0); // Just to ensure even length to skip

                    // Validate the remaining data contains the Pascal name string
                    if (reader.remaining() < ((long) length + skip + 4))
                    {
                        break;
                    }

                    // Skip the Pascal string so the next field starts on an even boundary
                    reader.readBytes(length + skip);

                    reader.mark();

                    int dataSize = reader.readInteger(); // Data size - 4 bytes

                    if (dataSize < 0 || reader.remaining() < (long) dataSize)
                    {
                        reader.reset();
                        break;
                    }

                    switch (resID)
                    {
                        case 0x03EC: // Legacy
                        case 0x03F5:
                            translateColorHalftoningInfo(reader, display);
                        break;

                        case 0x03ED:
                            translateResolutionInfo(reader, display);
                        break;

                        case 0x03EE:
                            translateAlphaChannelNames(reader, dataSize, display);
                        break;

                        case 0x03F3:
                            translatePrintFlags(reader, display);
                        break;

                        case 0x03F8:
                            translateColorTransferFunctions(reader, display);
                        break;

                        case 0x03FE:
                            translateQuickMaskInfo(reader, display);
                        break;

                        case 0x0409:
                        case 0x040C:
                            translateThumbnailInfo(reader, display);
                        break;

                        case 0x040D:
                            translateGlobalAngle(reader, display);
                        break;

                        case 0x0419:
                            translateGlobalAltitude(reader, display);
                        break;

                        case 0x041A:
                            translateSlicesInfo(reader, display);
                        break;

                        case 0x041B:
                            translatePrintStyleFallback(reader, display);
                        break;

                        case 0x041C:
                            translatePrintPositionFallback(reader, display);
                        break;

                        case 0x041D:
                            translatePrintScaleFallback(reader, display);
                        break;

                        case 0x0421:
                            translateVersionInfo(reader, display);
                        break;

                        case 0x0426:
                            translatePrintScaleInfo(reader, display);
                        break;

                        case 0x0428:
                            translatePixelAspectRatio(reader, display);
                        break;

                        case 0x2710:
                            translatePrintFlagsInfo(reader, display);
                        break;

                        case 0x040A:
                            display.accept("Copyright Flag", dataSize > 0 && reader.peek(reader.getCurrentPosition()) == 1 ? "True" : "False");
                        break;

                        case 0x0408:
                            translateGridGuidesInfo(reader, display);
                        break;

                        case 0x041E:
                        case 0x040B:
                            translateURL(reader, resID, display);
                        break;

                        case 0x0414:
                            if (reader.hasRemaining(Integer.BYTES))
                            {
                                display.accept("Base Value for IDs", String.valueOf(reader.readUnsignedInteger()));
                            }
                        break;

                        default:
                        break;
                    }

                    // Return to the checkpoint before skipping the resource block in full
                    reader.reset();

                    // Add trailing padding to ensure the resource block
                    // ends on an even byte boundary
                    int padding = (dataSize % 2 != 0 ? 1 : 0);

                    try
                    {
                        reader.seek(reader.getCurrentPosition() + 4 + (long) dataSize + padding);
                    }

                    catch (IndexOutOfBoundsException exc)
                    {
                        break;
                    }
                }

                else
                {
                    // If signature is not found, move one byte and try again
                    reader.seek(reader.getCurrentPosition() + 1);
                }
            }
        }
    }

    /**
     * Parses the Colour Halftoning Information Image Resource Block (0x03F5) by decoding
     * Photoshop's 18-byte fixed-point channel structure, following the parsing logic used by Phil
     * Harvey's ExifTool. Both frequency and angle values are extracted as 4-byte 16.16 fixed-point
     * numbers.
     * *
     * <p>
     * Structure of each channel (18 bytes):
     * </p>
     * *
     * <ul>
     * <li>4 bytes: int32 (fixed-point 16.16 frequency)</li>
     * <li>2 bytes: int16 (frequency unit: 1 = LPI, 2 = LPC)</li>
     * <li>4 bytes: int32 (fixed-point 16.16 angle)</li>
     * <li>2 bytes: int16 (angle unit: 1 = Degrees)</li>
     * <li>2 bytes: int16 (shape ID enum)</li>
     * <li>4 bytes: padding/flags</li>
     * </ul>
     * 
     * @param reader
     *        the binary reader for the resource data block
     * 
     * @param display
     *        the target streaming handler receiving property pairs
     */
    private static void translateColorHalftoningInfo(ByteArrayReader reader, PropertyDisplay display)
    {
        if (reader.hasRemaining(72))
        {
            final double Fixed_Point_Multiplier = 1.0 / 65536.0;
            final String[] Channels = {"Cyan", "Magenta", "Yellow", "Black"};
            final String[] Shapes = {"Round", "Ellipse", "Line", "Square", "Cross", "Unknown", "Diamond"};

            for (String channel : Channels)
            {
                double frequency = reader.readInteger() * Fixed_Point_Multiplier;
                short freqUnit = reader.readShort();
                double angle = reader.readInteger() * Fixed_Point_Multiplier;
                reader.readShort(); // Skip angle unit

                int shapeId = reader.readShort();
                String shapeName = (shapeId >= 0 && shapeId < Shapes.length) ? Shapes[shapeId] : "Unknown (" + shapeId + ")";

                reader.readInteger(); // Skip reserved padding/flags

                display.accept(channel + " Halftone Frequency", String.format(Locale.ROOT, "%.2f %s", frequency, (freqUnit == 2 ? "LPC" : "LPI")));
                display.accept(channel + " Halftone Angle", String.format(Locale.ROOT, "%.1f°", angle));
                display.accept(channel + " Halftone Shape", shapeName);
            }
        }
    }

    /**
     * Parses Photoshop resolution information (0x03ED).
     *
     * @param reader
     *        the binary reader for the data block
     * @param display
     *        the target streaming handler receiving property pairs
     */
    private static void translateResolutionInfo(ByteArrayReader reader, PropertyDisplay display)
    {
        if (reader.hasRemaining(16))
        {
            display.accept("X Resolution", String.valueOf(reader.readUnsignedShort()));
            reader.readBytes(2);
            display.accept("Displayed Units X", reader.readUnsignedShort() == 1 ? "inches" : "cm");

            display.accept("Y Resolution", String.valueOf(reader.readUnsignedShort()));
            reader.readBytes(2);
            display.accept("Displayed Units Y", reader.readUnsignedShort() == 1 ? "inches" : "cm");
        }
    }

    /**
     * Parses Alpha Channel Names (0x03EE).
     *
     * <p>
     * Alpha channel names are stored as a contiguous sequence of unpadded Pascal strings.
     * </p>
     *
     * @param reader
     *        the binary reader for the data block
     * @param dataSize
     *        the exact byte length of the resource data
     * @param display
     *        the target streaming handler receiving property pairs
     */
    private static void translateAlphaChannelNames(ByteArrayReader reader, int dataSize, PropertyDisplay display)
    {
        if (reader.hasRemaining(dataSize))
        {
            int index = 1;
            long endPosition = reader.getCurrentPosition() + dataSize;

            while (reader.getCurrentPosition() < endPosition)
            {
                int length = reader.readUnsignedByte();

                if (length > 0 && reader.getCurrentPosition() + length <= endPosition)
                {
                    String name = new String(reader.readBytes(length), StandardCharsets.US_ASCII).trim();
                    display.accept("Alpha Channel #" + index++, name.isEmpty() ? "Untitled" : name);
                }

                else
                {
                    break;
                }
            }
        }
    }

    /**
     * Parses the legacy 9-byte Print Flags resource (0x03F3).
     *
     * @param reader
     *        the binary reader for the data block
     * @param display
     *        the target streaming handler receiving property pairs
     */
    private static void translatePrintFlags(ByteArrayReader reader, PropertyDisplay display)
    {
        if (reader.hasRemaining(9))
        {
            final String[] Labels = {
                    "Print Labels", "Print Crop Marks", "Print Color Bars",
                    "Print Registration Marks", "Print Negative", "Print Flip",
                    "Print Interpolate", "Print Caption", "Print Flags"
            };

            for (String label : Labels)
            {
                display.accept(label, (reader.readByte() == 1) ? "True" : "False");
            }
        }
    }

    /**
     * Parses Color Transfer Functions (0x03F8). Processes the four channel transfer curves (Cyan,
     * Magenta, Yellow, and Black).
     * 
     * Structure per curve (28 bytes total, 112 bytes aggregate payload):
     * 
     * <ul>
     * <li>26 bytes: short[13] (curve mapping values from 0% to 100% density scaled 0-1000)</li>
     * <li>2 bytes: int16 (reserved/padding)</li>
     * </ul>
     * 
     * @param reader
     *        the binary reader for the data block
     * @param display
     *        the target streaming handler receiving property pairs
     */
    private static void translateColorTransferFunctions(ByteArrayReader reader, PropertyDisplay display)
    {
        if (reader.hasRemaining(112))
        {
            final String[] Channels = {"Cyan", "Magenta", "Yellow", "Black"};

            for (String channel : Channels)
            {
                StringBuilder curveValues = new StringBuilder();

                // Extract all 13 curve mapping density check-points
                for (int i = 0; i < 13; i++)
                {
                    short pointValue = reader.readShort();

                    curveValues.append(pointValue);

                    if (i < 12)
                    {
                        curveValues.append(", ");
                    }
                }

                reader.readShort(); // Skip the reserved trailing field
                display.accept(channel + " Transfer Function", "[" + curveValues.toString() + "]");
            }
        }
    }

    /**
     * Parses Quick Mask Information (0x03FE).
     *
     * <p>
     * Reads a 2-byte channel identifier followed by a 1-byte flag indicating whether the quick mask
     * is initially empty.
     * </p>
     *
     * @param reader
     *        the binary reader for the data block
     * @param display
     *        the target streaming handler receiving property pairs
     */
    private static void translateQuickMaskInfo(ByteArrayReader reader, PropertyDisplay display)
    {
        if (reader.hasRemaining(3))
        {
            short channelId = reader.readShort();
            int isInitiallyEmpty = reader.readUnsignedByte();

            display.accept("Quick Mask Channel ID", String.valueOf(channelId));
            display.accept("Quick Mask Initially Empty", isInitiallyEmpty == 1 ? "True" : "False");
        }
    }

    /**
     * Parses Photoshop thumbnail information (0x0409 and 0x040C).
     *
     * @param reader
     *        the binary reader for the data block
     * @param display
     *        the target streaming handler receiving property pairs
     */
    private static void translateThumbnailInfo(ByteArrayReader reader, PropertyDisplay display)
    {
        if (reader.hasRemaining(28))
        {
            display.accept("Thumbnail Format", reader.readInteger() == 1 ? "JPEG-compressed" : "Uncompressed Raw");
            display.accept("Thumbnail Width", String.valueOf(reader.readInteger()));
            display.accept("Thumbnail Height", String.valueOf(reader.readInteger()));
            display.accept("Thumbnail Width Bytes", String.valueOf(reader.readInteger()));
            display.accept("Thumbnail Total Size", String.valueOf(reader.readInteger()));
            display.accept("Thumbnail Compressed Size", String.valueOf(reader.readInteger()));
            display.accept("Thumbnail Bits Per Pixel", String.valueOf(reader.readShort()));
            display.accept("Thumbnail Planes", String.valueOf(reader.readShort()));
        }
    }

    /**
     * Parses the global lighting angle (0x040D).
     *
     * @param reader
     *        the binary reader for the data block
     * @param display
     *        the target streaming handler receiving property pairs
     */
    private static void translateGlobalAngle(ByteArrayReader reader, PropertyDisplay display)
    {
        if (reader.hasRemaining(Integer.BYTES))
        {
            display.accept("Global Angle", reader.readInteger() + "°");
        }
    }

    /**
     * Parses the global lighting altitude (0x0419).
     *
     * @param reader
     *        the binary reader for the data block
     * @param display
     *        the target streaming handler receiving property pairs
     */
    private static void translateGlobalAltitude(ByteArrayReader reader, PropertyDisplay display)
    {
        if (reader.hasRemaining(Integer.BYTES))
        {
            display.accept("Global Altitude", reader.readInteger() + "°");
        }
    }

    /**
     * Parses slice information (0x041A).
     *
     * @param reader
     *        the binary reader for the data block
     * @param display
     *        the target streaming handler receiving property pairs
     */
    private static void translateSlicesInfo(ByteArrayReader reader, PropertyDisplay display)
    {
        if (reader.hasRemaining(24))
        {
            reader.readBytes(20);

            int stringLength = reader.readInteger() * 2;

            if (stringLength >= 0 && reader.hasRemaining(stringLength))
            {
                byte[] stringBytes = reader.readBytes(stringLength);
                String slicesGroupName = new String(stringBytes, StandardCharsets.UTF_16BE).trim();
                display.accept("Slices Group Name", slicesGroupName.isEmpty() ? "None" : slicesGroupName);
            }

            if (reader.hasRemaining(Integer.BYTES))
            {
                display.accept("Number of Slices", String.valueOf(reader.readInteger()));
            }
        }
    }

    /**
     * Parses legacy print style information (0x041B).
     *
     * @param reader
     *        the binary reader for the data block
     * @param display
     *        the target streaming handler receiving property pairs
     */
    private static void translatePrintStyleFallback(ByteArrayReader reader, PropertyDisplay display)
    {
        if (!reader.hasRemaining(Short.BYTES))
        {
            return;
        }

        display.accept("Print Style", reader.readUnsignedShort() == 1 ? "Centered" : "Normal");
    }

    /**
     * Parses legacy print position information (0x041C).
     *
     * @param reader
     *        the binary reader for the data block
     * @param display
     *        the target streaming handler receiving property pairs
     */
    private static void translatePrintPositionFallback(ByteArrayReader reader, PropertyDisplay display)
    {
        if (!reader.hasRemaining(8))
        {
            return;
        }

        display.accept("Print Position", String.format(Locale.ROOT, "%.0f %.0f", reader.readFloat(), reader.readFloat()));
    }

    /**
     * Parses legacy print scale information (0x041D).
     *
     * @param reader
     *        the binary reader for the data block
     * @param display
     *        the target streaming handler receiving property pairs
     */
    private static void translatePrintScaleFallback(ByteArrayReader reader, PropertyDisplay display)
    {
        if (!reader.hasRemaining(14))
        {
            return;
        }

        reader.readBytes(10);
        display.accept("Print Scale", String.format(Locale.ROOT, "%g", reader.readFloat()).replace(".00000", ""));
    }

    /**
     * Parses Photoshop version information (0x0421).
     *
     * @param reader
     *        the binary reader for the data block
     * @param display
     *        the target streaming handler receiving property pairs
     */
    private static void translateVersionInfo(ByteArrayReader reader, PropertyDisplay display)
    {
        final String[] Photoshop_Versions = {
                "Photoshop 7.0 or earlier", "Photoshop CS (8.0)", "Photoshop CS2 (9.0)",
                "Photoshop CS3 (10.0)", "Photoshop CS4 (11.0)", "Photoshop CS5 (12.0)",
                "Photoshop CS6 (13.0)", "Photoshop CC (14.0)"
        };

        if (reader.hasRemaining(13))
        {
            reader.readInteger();

            display.accept("Has Real Merged Data", reader.readByte() != 0 ? "Yes" : "No");
            display.accept("Writer Name", readUnicodeString(reader));
            display.accept("Reader Name", readUnicodeString(reader));

            if (reader.hasRemaining(Integer.BYTES))
            {
                int version = reader.readInteger();

                if (version >= 1 && version <= Photoshop_Versions.length)
                {
                    display.accept("Application File Version", Photoshop_Versions[version - 1]);
                }

                else
                {
                    display.accept("Application File Version", "Photoshop (Internal Version " + version + ")");
                }
            }
        }
    }

    /**
     * Parses Photoshop print scale information (0x0426).
     *
     * @param reader
     *        the binary reader for the data block
     * @param display
     *        the target streaming handler receiving property pairs
     */
    private static void translatePrintScaleInfo(ByteArrayReader reader, PropertyDisplay display)
    {
        if (reader.hasRemaining(14))
        {
            int styleType = reader.readUnsignedShort();
            float xPos = reader.readFloat();
            float yPos = reader.readFloat();
            float scale = reader.readFloat();
            String styleStr = (styleType == 0 ? "Centered" : (styleType == 1 ? "Size to Fit" : "User Defined"));

            display.accept("Print Style", styleStr);
            display.accept("Print Position", String.format(Locale.ROOT, "%.0f %.0f", xPos, yPos));
            display.accept("Print Scale", String.format(Locale.ROOT, "%g", scale).replace(".00000", ""));
        }
    }

    /**
     * Parses pixel aspect ratio information (0x0428).
     *
     * @param reader
     *        the binary reader for the data block
     * @param display
     *        the target streaming handler receiving property pairs
     */
    private static void translatePixelAspectRatio(ByteArrayReader reader, PropertyDisplay display)
    {
        if (reader.hasRemaining(12))
        {
            reader.readBytes(4);
            double aspect = reader.readDouble();
            String aspectStr = (aspect == (long) aspect) ? String.format(Locale.ROOT, "%d", (long) aspect) : String.valueOf(aspect);

            display.accept("Pixel Aspect Ratio", aspectStr);
        }
    }

    /**
     * Parses print flag information (0x2710).
     *
     * @param reader
     *        the binary reader for the data block
     * @param display
     *        the target streaming handler receiving property pairs
     */
    private static void translatePrintFlagsInfo(ByteArrayReader reader, PropertyDisplay display)
    {
        if (reader.hasRemaining(10))
        {
            short version = reader.readShort();
            boolean centerCropMarks = (reader.readByte() == 1);
            reader.readByte(); // Skip the 1-byte padding field

            /*
             * Bleed width is stored as a 4-byte value. It is interpreted
             * as an unsigned integer to match ExifTool's output.
             */
            double bleedWidth = (double) reader.readUnsignedInteger();
            short bleedScale = reader.readShort();
            String unitLabel;

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

            display.accept("Print Flags Info Version", String.valueOf(version));
            display.accept("Center Crop Marks", centerCropMarks ? "True" : "False");
            display.accept("Print Bleed Width", String.format(Locale.ROOT, "%.2f %s", bleedWidth, unitLabel));
        }
    }

    /**
     * Parses Grid and Guides information (0x0408).
     *
     * <p>
     * Decodes the 16-byte header followed by an array of 8-byte guide records. The parsing logic is
     * based on the parsing logic used by Phil Harvey's ExifTool.
     * </p>
     *
     * <p>
     * Structure of each guide:
     * </p>
     *
     * <ul>
     * <li>4-byte 16.16 fixed-point coordinate</li>
     * <li>1-byte orientation (0 = Vertical, 1 = Horizontal)</li>
     * <li>3 bytes of padding</li>
     * </ul>
     *
     * @param reader
     *        the binary reader for the data block
     * @param display
     *        the target streaming handler receiving property pairs
     */
    private static void translateGridGuidesInfo(ByteArrayReader reader, PropertyDisplay display)
    {
        if (reader.hasRemaining(16))
        {
            final double Fixed_Point_Multiplier = 1.0 / 65536.0;
            int version = reader.readInteger();
            double gridCycleX = reader.readInteger() * Fixed_Point_Multiplier;
            int gridSubdivs = reader.readInteger();
            int guideCount = reader.readInteger();

            display.accept("Grid Guides Version", String.valueOf(version));
            display.accept("Grid Spacing Interval", String.format(Locale.ROOT, "%.2f", gridCycleX));
            display.accept("Grid Subdivisions", String.valueOf(gridSubdivs));
            display.accept("Manual Guides Count", String.valueOf(guideCount));

            // Extract each manual guide line matching ExifTool's loop layout
            for (int i = 1; i <= guideCount; i++)
            {
                if (reader.hasRemaining(8))
                {
                    double location = reader.readInteger() * Fixed_Point_Multiplier;
                    byte direction = reader.readByte();
                    reader.readBytes(3); // Skip the 3 trailing alignment bytes

                    String dirString = (direction == 0) ? "Vertical" : "Horizontal";
                    display.accept(String.format(Locale.ROOT, "Manual Guide #%d", i), String.format(Locale.ROOT, "%s at %.2f px", dirString, location));
                }
                else
                {
                    break;
                }
            }
        }
    }

    /**
     * Parses URL Information (0x040B) and URL List (0x041E) resources.
     *
     * <p>
     * Resource 0x040B contains a single Pascal string. Resource 0x041E begins with a 4-byte item
     * count followed by a sequence of Pascal strings.
     * </p>
     *
     * @param reader
     *        the binary reader for the data block
     * @param resID
     *        the Photoshop resource identifier
     * @param display
     *        the target streaming handler receiving property pairs
     */
    private static void translateURL(ByteArrayReader reader, short resID, PropertyDisplay display)
    {
        int index = 1;
        int totalStrings = 1;

        // 0x041E contains a 4-byte integer tracking the total number of items
        if (resID == 0x041E)
        {
            if (!reader.hasRemaining(Integer.BYTES))
            {
                return;
            }

            totalStrings = reader.readInteger();
        }

        for (int i = 0; i < totalStrings; i++)
        {
            if (reader.hasRemaining(1))
            {
                int length = reader.readUnsignedByte();

                if (length > 0 && reader.hasRemaining(length))
                {
                    String url = new String(reader.readBytes(length), StandardCharsets.US_ASCII).trim();
                    String label = (resID == 0x041E) ? "URL #" + index++ : "URL";

                    display.accept(label, url.isEmpty() ? "Untitled" : url);
                }

                else
                {
                    break;
                }
            }
        }
    }

    /**
     * Reads a UTF-16BE encoded string prefixed by its character count.
     *
     * @param reader
     *        the binary reader for the data block
     * @return the decoded string, or {@code "Unknown"} if it cannot be read
     */
    private static String readUnicodeString(ByteArrayReader reader)
    {
        String value = "";

        if (reader.hasRemaining(Integer.BYTES))
        {
            int length = reader.readInteger();

            if (length >= 0 && length <= 10000)
            {
                int byteLength = length * 2;

                if (reader.hasRemaining(byteLength))
                {
                    byte[] stringBytes = reader.readBytes(byteLength);
                    value = new String(stringBytes, StandardCharsets.UTF_16BE).trim();
                }
            }
        }

        return value.isEmpty() ? "Unknown" : value;
    }
}