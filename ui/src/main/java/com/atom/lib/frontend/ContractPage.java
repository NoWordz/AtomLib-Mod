package com.atom.lib.frontend;

import com.atom.lib.frontend.contract.ContractRegistry;

/** Discoverable business-only page definition. The host supplies its approved page handle. */
public abstract class ContractPage {
    public abstract StrictPage build(ContractRegistry.PageHandle page);
}
