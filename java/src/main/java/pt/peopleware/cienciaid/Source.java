package pt.peopleware.cienciaid;

/** The issuing source of an identifier, taken from its third digit. */
public final class Source {
    private final String code;
    private final boolean known;
    private final String name;
    private final String domain;

    Source(String code, boolean known, String name, String domain) {
        this.code = code;
        this.known = known;
        this.name = name;
        this.domain = domain;
    }

    /** The source code (one hexadecimal digit). */
    public String getCode() {
        return code;
    }

    /** Whether this library knows the source. */
    public boolean isKnown() {
        return known;
    }

    /** The source name, or {@code "Unknown"}. */
    public String getName() {
        return name;
    }

    /** The source domain, or {@code null} for an unknown source. */
    public String getDomain() {
        return domain;
    }
}
