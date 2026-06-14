package tif;

/**
 * Structural type hints mapping low-level binary TIFF/Exif fields to high-level Java data types.
 * 
 * <p>
 * These hints instruct the binary parsing stream and text-formatting engines how to handle, unpack,
 * and represent complex primitive structures, character matrices, and binary streams extracted from
 * Image File Directories (IFDs).
 * </p>
 * 
 * @author Trevor Maggs
 * @version 1.2
 */
public enum TagHint
{
    HINT_BYTE("8-bit unsigned byte"),
    HINT_SBYTE("8-bit signed byte"),
    HINT_SHORT("16-bit unsigned short"),
    HINT_SSHORT("16-bit signed short"),
    HINT_INTEGER("32-bit unsigned integer (LONG)"),
    HINT_SINTEGER("32-bit signed integer (SLONG)"),
    HINT_RATIONAL("2 unsigned LONGs (Fraction)"),
    HINT_SRATIONAL("2 signed LONGs (Signed Fraction)"),
    HINT_FLOAT("Single precision floating point"),
    HINT_DOUBLE("Double precision floating point"),
    HINT_STRING("Java String (Null-terminated ASCII)"),
    HINT_VERSION("Fixed 4-character version string"),
    HINT_DATE("Java Date (Standard ASCII format)"),
    HINT_MASK("Masked string"),
    HINT_UCS2("Universal character set - UTF-16"),
    HINT_ENCODED_STRING("Encoded String (Prefixed character set header)"),
    HINT_BYTE_STREAM("Opaque binary byte sequence"),
    HINT_UNDEFINED("Likely an unstructured byte array"),
    HINT_DEFAULT("Default structural type logic"),
    HINT_UNKNOWN("Hint configuration is unknown");

    private final String description;

    private TagHint(String desc)
    {
        this.description = desc;
    }

    /**
     * Retrieves the structural engineering description of the formatting type hint.
     * 
     * @return the text {@link String} descriptor
     */
    public String getDescription()
    {
        return description;
    }
}