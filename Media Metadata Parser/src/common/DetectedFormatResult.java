package common;

/**
 * Encapsulates the detected media format signature and its corresponding parser instance, if
 * available.
 *
 * @author Trevor Maggs
 * @version 1.0
 * @since 15 August 2026
 */
public final class DetectedFormatResult
{
    private final DigitalSignature signature;
    private final AbstractImageParser<?> parser;

    /**
     * Constructs a result containing the detected format signature and optional parser.
     *
     * @param signature
     *        the detected {@link DigitalSignature}, never {@code null}
     * @param parser
     *        the matching {@link AbstractImageParser}, or {@code null} if no parser is available
     */
    public DetectedFormatResult(DigitalSignature signature, AbstractImageParser<?> parser)
    {
        this.signature = signature;
        this.parser = parser;
    }

    /**
     * Returns the detected media format signature.
     *
     * @return the {@link DigitalSignature}
     */
    public DigitalSignature getSignature()
    {
        return signature;
    }

    /**
     * Returns the image parser instance, if available.
     *
     * @return the {@link AbstractImageParser}, or {@code null} if none exists for the detected
     *         format
     */
    public AbstractImageParser<?> getParser()
    {
        return parser;
    }

    /**
     * Returns whether a parser instance is available.
     *
     * @return {@code true} if a parser is available, otherwise {@code false}
     */
    public boolean hasParser()
    {
        return (parser != null);
    }
}