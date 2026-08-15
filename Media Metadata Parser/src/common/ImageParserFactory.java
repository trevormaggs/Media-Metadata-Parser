package common;

import java.io.IOException;
import java.nio.file.Path;
import heif.HeifParser;
import jpg.JpgParser;
import png.PngParser;
import tif.TifParser;
import webp.WebpParser;

/**
 * A static factory class responsible for returning appropriate image parser instances based on the
 * detected image format from file signature bytes.
 *
 * <p>
 * Supported image parsers include:
 * </p>
 *
 * <ul>
 * <li>JPEG (JPG)</li>
 * <li>PNG</li>
 * <li>TIFF</li>
 * <li>HEIC (High Efficiency Image Format)</li>
 * <li>WebP</li>
 * </ul>
 *
 * <p>
 * This factory encapsulates single-pass format detection via {@link DigitalSignature} and delegates
 * instantiation of the correct parser implementation and encapsulates the result in a
 * {@link DetectedFormatResult}.
 * </p>
 *
 * @author Trevor Maggs
 * @version 1.4
 * @since 15 August 2026
 */
public final class ImageParserFactory
{
    /**
     * Prevents direct instantiation.
     *
     * @throws UnsupportedOperationException
     *         to indicate that direct instantiation is not supported
     */
    private ImageParserFactory()
    {
        throw new UnsupportedOperationException("Instantiation not allowed");
    }

    /**
     * Inspects the specified file to detect its signature and returns an immutable
     * {@link DetectedFormatResult} containing the detected signature and the corresponding parser,
     * if supported.
     *
     * @param fpath
     *        the file path of the media file to inspect
     * @return a {@link DetectedFormatResult} containing the detected {@link DigitalSignature} and
     *         corresponding {@link AbstractImageParser}, if available
     *
     * @throws IOException
     *         if an I/O error occurs while reading the file signature
     * @throws UnsupportedOperationException
     *         if the file signature is unrecognised
     */
    public static DetectedFormatResult inspect(Path fpath) throws IOException
    {
        DigitalSignature signature = DigitalSignature.detectFormat(fpath);

        if (signature == DigitalSignature.UNKNOWN)
        {
            throw new UnsupportedOperationException("No parser available for unrecognised image format in file [" + fpath.getFileName() + "]");
        }

        AbstractImageParser<?> parser;

        switch (signature)
        {
            case JPG:
                parser = new JpgParser(fpath);
            break;

            case TIF:
                parser = new TifParser(fpath);
            break;

            case PNG:
                parser = new PngParser(fpath);
            break;

            case HEIF:
                parser = new HeifParser(fpath);
            break;

            case WEBP:
                parser = new WebpParser(fpath);
            break;

            default:
                // Defensive handling for future DigitalSignature enum values without a parser.
                parser = null;
            break;
        }

        return new DetectedFormatResult(signature, parser);
    }
}