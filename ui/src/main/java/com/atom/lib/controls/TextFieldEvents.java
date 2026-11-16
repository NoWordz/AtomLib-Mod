package com.atom.lib.controls;

/** Editing and explicit submission are independent; neither callback persists data. */
public interface TextFieldEvents {
    default void changed(String text){}
    default void committed(String text){}
    default void rejected(String reason){}
}
