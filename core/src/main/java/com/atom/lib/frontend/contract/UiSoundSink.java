package com.atom.lib.frontend.contract;

/** Implemented by the host: validates actual event resources, then dispatches to its normal sound output. */
public interface UiSoundSink {
    boolean supports(String channel);
    boolean available(String resource);
    void play(UiSoundIntent intent);
}
