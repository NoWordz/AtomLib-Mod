package com.atom.lib.frontend.contract;

/** A valid contract could not be admitted by this library's interpreter. */
public final class ContractCompatibilityException extends IllegalArgumentException {
    private static final long serialVersionUID = 1L;

    /** Reason for a registry admission or library metadata failure. */
    public enum Reason {
        UNSUPPORTED_PROFILE,
        MECHANISM_MISMATCH,
        MISSING_MECHANISM_BINDING,
        INVALID_MECHANISM_METADATA
    }

    private final Reason reason;

    ContractCompatibilityException(Reason reason, String detail) {
        this(reason, detail, null);
    }

    ContractCompatibilityException(Reason reason, String detail, Throwable cause) {
        super("INCOMPATIBLE_PROFILE " + detail, cause);
        this.reason = reason;
    }

    /** Machine-readable reason. Other design and manifest errors have their own diagnostics. */
    public Reason reason() {
        return reason;
    }
}
