package com.atom.lib.frontend.contract;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import static com.atom.lib.frontend.contract.ContractCompatibilityException.Reason.*;

/** Frontend interpreter compatibility and build provenance. */
public final class MechanismIdentity {
    public static final String BINDING = "mechanism/atomlib";
    private static final String RESOURCE = "META-INF/atomlib/fcl/mechanism.sha256";
    private static final String INTERPRETER_EPOCH = "atomlib/fcl03/interpreter/1";
    private static final String COMPATIBILITY_BINDING = digest(INTERPRETER_EPOCH);

    private MechanismIdentity() {}

    /** The full build fingerprint, including implementation, platform and native packaging inputs. */
    public static String current() {
        try {
            var resources = MechanismIdentity.class.getClassLoader().getResources(RESOURCE);
            String identity = null;
            while (resources.hasMoreElements()) {
                String next;
                try (var stream = resources.nextElement().openStream()) {
                    byte[] bytes = stream.readNBytes(257);
                    if (bytes.length > 256) throw new IOException("oversized mechanism metadata");
                    next = new String(bytes, StandardCharsets.US_ASCII).strip();
                }
                if (!next.matches("[a-f0-9]{64}")) throw new IOException("invalid mechanism metadata");
                if (identity != null && !identity.equals(next)) {
                    throw new IOException("conflicting library mechanism metadata");
                }
                identity = next;
            }
            if (identity == null) throw new IOException("missing library mechanism metadata");
            return identity;
        } catch (IOException e) {
            throw new ContractCompatibilityException(INVALID_MECHANISM_METADATA, e.getMessage(), e);
        }
    }

    /** Semantic binding for newly compiled FCL03 projects. */
    public static String compatibilityBinding() {
        return COMPATIBILITY_BINDING;
    }

    /** Whether this interpreter supports a verified release's pre-epoch binding. */
    public static boolean supportsLegacyBinding(String binding) {
        // Only the published 0.6.0 and 0.7.0 FCL03 interpreters were reviewed for epoch 1.
        return switch (INTERPRETER_EPOCH) {
            case "atomlib/fcl03/interpreter/1" ->
                    "595aed94fa5c0b076682d6555df481cf7f065da672801e9d841399d317fb57c7".equals(binding)
                    || "685cf4bd479a4fbf89559a529b9f4f168ae1617a7f01eef8c2e3ea3a8a735418".equals(binding);
            default -> false;
        };
    }

    static void require(ContractDesign design) {
        // The standalone model/codec probes, and protocol 1, have no build bindings.
        if (design.buildBindings().isEmpty()) return;

        String expected = design.buildBindings().get(BINDING);
        if (expected == null) {
            throw new ContractCompatibilityException(MISSING_MECHANISM_BINDING,
                    design.profile() + " missing " + BINDING);
        }
        String fingerprint = current();
        if (!COMPATIBILITY_BINDING.equals(expected) && !supportsLegacyBinding(expected)) {
            throw new ContractCompatibilityException(MECHANISM_MISMATCH,
                    design.profile() + " expected mechanism=" + expected + " supported mechanism="
                    + COMPATIBILITY_BINDING + " build fingerprint=" + fingerprint
                    + "; retain the compatible library or review a migrated design");
        }
    }

    private static String digest(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.US_ASCII)));
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError(e);
        }
    }
}
