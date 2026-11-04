package pt.peopleware.cienciaid;

import java.util.List;

/**
 * Result of {@link CienciaId#parse(String)}. When the status is {@link Status#INVALID}, every field
 * other than the status, the reason and the (empty) warnings is {@code null}.
 */
public final class ParseResult {
    private final Status status;
    private final Reason reason;
    private final List<Warning> warnings;
    private final String internalValidation;
    private final Source source;
    private final String uniqueId;
    private final Long uniqueIdDecimal;
    private final String checkDigit;
    private final String indicatedDomain;

    ParseResult(Status status, Reason reason, List<Warning> warnings, String internalValidation,
                Source source, String uniqueId, Long uniqueIdDecimal, String checkDigit,
                String indicatedDomain) {
        this.status = status;
        this.reason = reason;
        this.warnings = warnings;
        this.internalValidation = internalValidation;
        this.source = source;
        this.uniqueId = uniqueId;
        this.uniqueIdDecimal = uniqueIdDecimal;
        this.checkDigit = checkDigit;
        this.indicatedDomain = indicatedDomain;
    }

    public Status getStatus() {
        return status;
    }

    /** Why the identifier is invalid; {@code null} unless the status is {@link Status#INVALID}. */
    public Reason getReason() {
        return reason;
    }

    /** Unmodifiable list of warnings; empty when there are none. */
    public List<Warning> getWarnings() {
        return warnings;
    }

    /** The first two digits. */
    public String getInternalValidation() {
        return internalValidation;
    }

    public Source getSource() {
        return source;
    }

    /** Digits 4 to 11 (eight hexadecimal digits). */
    public String getUniqueId() {
        return uniqueId;
    }

    /** {@link #getUniqueId()} as an unsigned 32-bit integer (held in a {@code Long}). */
    public Long getUniqueIdDecimal() {
        return uniqueIdDecimal;
    }

    /** The twelfth digit. */
    public String getCheckDigit() {
        return checkDigit;
    }

    /** The lower-case domain prefix found in the input, or {@code null}. */
    public String getIndicatedDomain() {
        return indicatedDomain;
    }
}
