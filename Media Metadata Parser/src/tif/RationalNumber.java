package tif;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.Objects;

/**
 * This class represents a rational number, typically used in the TIFF/EXIF image format. TIFF
 * rational numbers are defined as pairs of 32-bit integers, encompassing numerator and denominator.
 * 
 * <p>
 * This class stores the numerator and denominator as 64-bit values to safely handle 32-bit unsigned
 * inputs from TIFF/EXIF specifications. All instances are automatically normalised (reduced to the
 * lowest terms with a positive divisor) upon creation to ensure mathematical equality and
 * consistent hash codes.
 * </p>
 * 
 * @author Trevor Maggs
 * @version 1.2
 * @since 27 November 2025
 */
public class RationalNumber extends Number
{
    private static final DecimalFormatSymbols ROOT_SYMBOLS = new DecimalFormatSymbols(Locale.ROOT);

    public final long numerator;
    public final long divisor;
    public final boolean unsignedType;

    public enum DataType
    {
        SIGNED,
        UNSIGNED;
    }

    /**
     * Internal constructor used to create normalised rational numbers.
     *
     * <p>
     * The divisor is always stored as a positive value. If necessary, the sign is moved to the
     * numerator. When {@code normalise} is {@code true}, the fraction is reduced to its lowest
     * terms using the greatest common divisor (GCD).
     * </p>
     *
     * @param num
     *        the numerator
     * @param div
     *        the divisor
     * @param type
     *        the original signed/unsigned interpretation
     * @param normalise
     *        whether the fraction should be reduced via GCD
     *
     * @throws ArithmeticException
     *         if {@code div} is zero and {@code num} is non-zero
     */
    private RationalNumber(long num, long div, DataType type, boolean normalise)
    {
        if (div == 0 && num != 0)
        {
            throw new ArithmeticException("Denominator cannot be zero for non-zero numerator");
        }

        if (div < 0)
        {
            num = -num;
            div = -div;
        }

        if (normalise && num != 0)
        {
            long common = gcd(Math.abs(num), div);

            if (common != 0)
            {
                num /= common;
                div /= common;
            }
        }

        this.unsignedType = (type == DataType.UNSIGNED);
        this.numerator = num;
        this.divisor = div;
    }

    /**
     * Constructs an instance to handle both signed and unsigned 32-bit integers. The input values
     * are masked to 64-bit values if {@code DataType.UNSIGNED} is specified. The resulting rational
     * number is automatically normalised.
     *
     * @param num
     *        a numerator in either 32-bit signed or unsigned format
     * @param div
     *        a divisor in either 32-bit signed or unsigned format
     * @param type
     *        specifies whether the values should be interpreted as {@code DataType.UNSIGNED} or
     *        {@code DataType.SIGNED}
     */
    public RationalNumber(int num, int div, DataType type)
    {
        this(type == DataType.UNSIGNED ? (num & 0xFFFFFFFFL) : num, type == DataType.UNSIGNED ? (div & 0xFFFFFFFFL) : div, type, true);
    }

    /**
     * Constructs a signed instance, interpreting inputs as standard 32-bit signed integers. The
     * resulting rational number is automatically normalised.
     *
     * @param num
     *        a numerator in 32-bit signed format
     * @param div
     *        a non-zero divisor in 32-bit signed format
     */
    public RationalNumber(int num, int div)
    {
        this(num, div, DataType.SIGNED);
    }

    /**
     * Simplifies or reduces a fraction by finding the greatest common divisor (GCD).
     *
     * @param numerator
     *        the top number of the fraction
     * @param divisor
     *        the bottom number of the fraction
     * @param type
     *        a flag indicating the signed/unsigned type for the resulting instance
     * @return an instance of the {@code RationalNumber} class representing the simplified fraction
     */
    public static RationalNumber simplify(long numerator, long divisor, DataType type)
    {
        return new RationalNumber(numerator, divisor, type, true);
    }

    /**
     * Returns a new instance with the value negated (multiplied by -1). The resulting instance is
     * always treated as {@code DataType.SIGNED}, and the fraction is normalised.
     *
     * @return a valid instance with a negated value
     */
    public RationalNumber negate()
    {
        return simplify(-numerator, divisor, DataType.SIGNED);
    }

    /**
     * Returns the integer representation of the rational number. It performs the conversion by
     * dividing the numerator by the denominator and truncating the result toward zero.
     * 
     * <p>
     * <strong>Note:</strong> This operation may result in a loss of precision.
     * </p>
     * 
     * @return the integer value after computation (truncated)
     */
    @Override
    public int intValue()
    {
        return (int) getFraction();
    }

    /**
     * Returns the long representation of the rational number. It performs the conversion by
     * dividing the numerator by the denominator and truncating the result toward zero.
     * 
     * <p>
     * <strong>Note:</strong> This operation may result in a loss of precision.
     * </p>
     * 
     * @return the long value after computation (truncated)
     */
    @Override
    public long longValue()
    {
        return (long) getFraction();
    }

    /**
     * Returns the single-precision floating-point representation of the rational number. It obtains
     * a double-precision value during computation to maintain as much precision from the original
     * numerator and denominator as possible before casting to {@code float}.
     *
     * @return the floating-point value after computation
     */
    @Override
    public float floatValue()
    {
        return (float) getFraction();
    }

    /**
     * Returns the double-precision floating-point representation of the rational number.
     *
     * @return the double value after computation
     */
    @Override
    public double doubleValue()
    {
        return getFraction();
    }

    /**
     * Generates a structural string representation of the fraction alongside a 4-digit decimal
     * fallback.
     * 
     * @return a formatted string showing the fractional components or an error message if invalid
     */
    @Override
    public String toString()
    {
        if (divisor == 0)
        {
            return String.format(Locale.ROOT, "Invalid rational number detected (%d/%d)", numerator, divisor);
        }

        if (divisor == 1)
        {
            return String.valueOf(numerator);
        }

        return String.format(Locale.ROOT, "%.4f", getFraction());
    }

    public String toString2()
    {
        if (divisor == 0)
        {
            return String.format(Locale.ROOT, "Invalid rational number detected (%d/%d)", numerator, divisor);
        }

        String decimalString = String.format(Locale.ROOT, "%.4f", getFraction());

        if (numerator % divisor == 0)
        {
            return decimalString;
        }

        else
        {
            return String.format(Locale.ROOT, "%d/%d (%s)", numerator, divisor, decimalString);
        }
    }

    /**
     * Compares this rational number to the specified object.
     * 
     * <p>
     * Two {@code RationalNumber} instances are considered equal if their normalised numerator,
     * normalised divisor, and {@code unsignedType} flag are completely identical.
     * </p>
     * 
     * @param obj
     *        the object to compare against
     * @return {@code true} if the specified object is mathematically equal to this rational number
     *         and
     *         shares the same data type interpretation; {@code false} otherwise
     */
    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }

        if (!(obj instanceof RationalNumber))
        {
            return false;
        }

        RationalNumber other = (RationalNumber) obj;

        return (numerator == other.numerator && divisor == other.divisor && unsignedType == other.unsignedType);
    }

    /**
     * Returns the hash code for this rational number.
     * 
     * <p>
     * The hash code is generated from the normalised numerator, normalised divisor, and the
     * {@code unsignedType} flag. This ensures that mathematically equal rational numbers (e.g., 1/2
     * and 2/4) evaluate to identical hash codes.
     * </p>
     * 
     * @return a hash code value for this object
     */
    @Override
    public int hashCode()
    {
        return Objects.hash(numerator, divisor, unsignedType);
    }

    /**
     * Checks if the rational number represents a whole number (an integer value).
     *
     * @return {@code true} if the fraction reduces cleanly to a whole number with no remainder,
     *         {@code false} if the value contains a decimal remainder or has an undefined
     *         denominator
     */
    public boolean hasIntegerValue()
    {
        return divisor != 0 && (divisor == 1 || numerator % divisor == 0);
    }

    /**
     * Returns a clean, human-readable string representation of the rational number, favouring a
     * simple integer or clean decimal string over a fractional notation where appropriate.
     * 
     * <p>
     * Formatting rules applied:
     * </p>
     * <ul>
     * <li>When decimal output is enabled, whole numbers are formatted with one trailing decimal
     * (e.g., "8.0").</li>
     * <li>Complex decimals are trimmed to a maximum of 4 decimal places without trailing zeros
     * (e.g., "1.6", "1.2891").</li>
     * <li>Falls back to standard fractional format if decimals are disallowed.</li>
     * </ul>
     * 
     * @param decimalAllowed
     *        if true, allows a short decimal representation instead of a fraction string
     * @return the simple string representation
     */
    public String toSimpleString(boolean decimalAllowed)
    {
        if (hasIntegerValue())
        {
            return decimalAllowed ? String.format(Locale.ROOT, "%.1f", doubleValue()) : Long.toString(longValue());
        }

        if (decimalAllowed)
        {
            DecimalFormat df = new DecimalFormat("0.####", ROOT_SYMBOLS);

            return df.format(doubleValue());
        }

        return toString();
    }

    public String toSimpleString2(boolean decimalAllowed)
    {
        if (hasIntegerValue())
        {
            return decimalAllowed ? String.format(Locale.ROOT, "%.1f", doubleValue()) : Long.toString(longValue());
        }

        if (decimalAllowed)
        {
            // Allows up to 4 decimal places, but drops unnecessary trailing zeros
            DecimalFormat df = new DecimalFormat("0.####", ROOT_SYMBOLS);
            return df.format(doubleValue());
        }

        return toString();
    }

    /**
     * Computes the greatest common divisor in a recursive fashion, based on the Euclidean
     * algorithm.
     *
     * @param a
     *        the first value
     * @param b
     *        the second value
     * @return the greatest common divisor of a and b
     */
    private static long gcd(long a, long b)
    {
        if (b == 0)
        {
            return a;
        }

        return gcd(b, a % b);
    }

    /**
     * Computes and returns the rational number as a double-precision floating-point value.
     *
     * @return the floating-point result of numerator divided by divisor, or special floating-point
     *         constants if the denominator is zero
     */
    private double getFraction()
    {
        if (divisor == 0)
        {
            return (numerator == 0) ? Double.NaN : (numerator > 0 ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY);
        }

        return (double) numerator / (double) divisor;
    }
}