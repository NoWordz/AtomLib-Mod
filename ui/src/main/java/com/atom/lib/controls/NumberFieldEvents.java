package com.atom.lib.controls;

import java.math.BigDecimal;

/** Changes include invalid drafts; only a valid explicit submission produces a value. */
public interface NumberFieldEvents {
    default void changed(String text){}
    default void committed(BigDecimal value){}
}
