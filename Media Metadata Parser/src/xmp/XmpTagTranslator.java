package xmp;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class XmpTagTranslator
{
    private static final Pattern RATIONAL_PATTERN = Pattern.compile("^(-?\\d+)/(\\d+)$");

    public static String translate(String prefix, String name, String rawValue)
    {
        if (rawValue == null || rawValue.isEmpty())
        {
            return "";
        }

        String key = (prefix + ":" + name).toLowerCase();

        switch (key)
        {
            // --- XMP-tiff Properties ---
            case "tiff:orientation":
                return "1".equals(rawValue) ? "Horizontal (normal)" : rawValue;

            case "tiff:ycbcrpositioning":
                return "1".equals(rawValue) ? "Centered" : "2".equals(rawValue) ? "Co-sited" : rawValue;

            case "tiff:resolutionunit":
                return "2".equals(rawValue) ? "inches" : "3".equals(rawValue) ? "cm" : rawValue;

            // --- XMP-photoshop Properties ---
            case "photoshop:colormode":
                return "3".equals(rawValue) ? "RGB" : "4".equals(rawValue) ? "CMYK" : "1".equals(rawValue) ? "GrayScale" : rawValue;

            // --- XMP-exif Enums ---
            case "exif:exposureprogram":
                return "2".equals(rawValue) ? "Program AE" : rawValue;

            case "exif:meteringmode":
                return "5".equals(rawValue) ? "Multi-segment" : rawValue;

            case "exif:sensingmethod":
                return "2".equals(rawValue) ? "One-chip color area" : rawValue;

            case "exif:scenetype":
                return "1".equals(rawValue) ? "Directly photographed" : rawValue;

            case "exif:exposuremode":
                return "0".equals(rawValue) ? "Auto" : rawValue;

            case "exif:whitebalance":
                return "0".equals(rawValue) ? "Auto" : rawValue;

            case "exif:scenecapturetype":
                return "0".equals(rawValue) ? "Standard" : rawValue;

            // --- GPS Specific Translations ---
            case "exif:gpsaltituderef":
                return "0".equals(rawValue) ? "Above Sea Level" : "1".equals(rawValue) ? "Below Sea Level" : rawValue;

            case "exif:gpsimgdirectionref":
            case "exif:gpsdestbearingref":
                return "T".equalsIgnoreCase(rawValue) ? "True North" : "M".equalsIgnoreCase(rawValue) ? "Magnetic North" : rawValue;

            case "exif:gpsspeedref":
                return "K".equalsIgnoreCase(rawValue) ? "km/h" : rawValue;

            // --- Format Dates to drop the "T" divider ---
            case "exif:datetimeoriginal":
            case "xmp:createdate":
            case "xmp:modifydate":
            case "xmp:metadatadate":
                return rawValue.replace("T", " ");

            default:
                // Fallthrough to handle generic math evaluations
                return evaluateGenericValue(rawValue);
        }
    }

    private static String evaluateGenericValue(String rawValue)
    {
        // Automatically evaluate standard fractions/rationals (e.g., 9/5 -> 1.8)
        Matcher matcher = RATIONAL_PATTERN.matcher(rawValue);

        if (matcher.matches())
        {
            try
            {
                double num = Double.parseDouble(matcher.group(1));
                double den = Double.parseDouble(matcher.group(2));

                if (den == 0)
                {
                    return rawValue;
                }

                double val = num / den;

                // Clean up formatting trailing zeros
                if (val == (long) val)
                {
                    return String.format("%d", (long) val);
                }

                else
                {
                    // Match ExifTool's maximum standard decimal precision dynamic feel
                    return String.valueOf(Math.round(val * 1000000.0) / 1000000.0);
                }
            }

            catch (NumberFormatException exc)
            {
                return rawValue;
            }
        }

        return rawValue;
    }
}