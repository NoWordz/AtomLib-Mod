package com.atom.lib.frontend.checker;

import java.util.Set;
import java.util.Arrays;

/** Never writes reviewed inputs or promotes candidates. */
public final class ApprovalCheck {
    private ApprovalCheck() {}
    public static String assess(byte[] reviewed, byte[] candidate, Set<String> supportedProfiles) {
        if(candidate.length>=4 && candidate[3]==3) {
            var proposed=com.atom.lib.frontend.contract.ContractCodec.decode(candidate);
            if(!supportedProfiles.contains(proposed.profile()))return "INCOMPATIBLE_PROFILE";
            if(reviewed==null)return "MISSING_EVIDENCE";
            var approved=com.atom.lib.frontend.contract.ContractCodec.decode(reviewed);
            if(!approved.namespace().equals(proposed.namespace()))return "INVALID_NAMESPACE";
            if(Arrays.equals(reviewed,candidate))return "APPROVED_VALID";
            if(approved.version().equals(proposed.version()))return "INVALID_REUSED_VERSION";
            return "CANDIDATE_VALID";
        }
        var proposed=DesignPackage.read(candidate);
        if(!supportedProfiles.contains(proposed.get("profile"))) return "INCOMPATIBLE_PROFILE";
        if(reviewed==null) return "MISSING_EVIDENCE";
        var approved=DesignPackage.read(reviewed);
        if(!approved.get("namespace").equals(proposed.get("namespace"))) return "INVALID_NAMESPACE";
        if(Arrays.equals(reviewed,candidate)) return "APPROVED_VALID";
        if(approved.get("version").equals(proposed.get("version"))) return "INVALID_REUSED_VERSION";
        return "CANDIDATE_VALID";
    }
}
