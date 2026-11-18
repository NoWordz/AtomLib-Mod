package com.atom.lib.frontend;

import com.atom.lib.frontend.contract.*;
import com.atom.lib.frontend.contract.ContractDesign.Binding;
import java.util.*;

/** Host composition root: page authors cannot replace the registry, implementation catalog or output adapter. */
public final class StrictPageHost {
    private final ContractRegistry.PageHandle contract;
    private final Map<Binding,StrictPageSession.Implementation> implementations;
    private final UiSoundSink sound;
    public StrictPageHost(ContractRegistry.PageHandle contract,Map<Binding,StrictPageSession.Implementation> implementations,UiSoundSink sound){
        this.contract=Objects.requireNonNull(contract);this.implementations=Map.copyOf(implementations);this.sound=sound;
    }
    public StrictPageSession create(ContractPage page){return mount(Objects.requireNonNull(page).build(contract));}
    public StrictPageSession mount(StrictPage page){
        contract.requirePage(Objects.requireNonNull(page).contract());
        return new StrictPageSession(page,implementations,sound);
    }
}
