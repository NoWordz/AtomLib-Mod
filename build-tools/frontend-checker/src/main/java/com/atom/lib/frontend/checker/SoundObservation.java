package com.atom.lib.frontend.checker;

/** Platform-neutral output boundary evidence, not an audio renderer. */
public record SoundObservation(String event,String resource,String channel,float volume,float pitch,long millis) {
    public SoundObservation {
        if(event==null || resource==null || channel==null || event.isBlank() || resource.isBlank() || channel.isBlank()
            || !Float.isFinite(volume) || volume<0 || !Float.isFinite(pitch) || pitch<=0 || millis<0) throw new IllegalArgumentException("invalid sound observation");
    }
}
