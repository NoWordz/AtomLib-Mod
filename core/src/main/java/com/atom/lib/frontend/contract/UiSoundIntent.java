package com.atom.lib.frontend.contract;

/** One controlled UI feedback request, independent of Minecraft and audio devices. */
public record UiSoundIntent(String event,String resource,String channel,float volume,float pitch,long action,long frameMillis) {
    public UiSoundIntent {
        if(event==null||event.isBlank()||resource==null||channel==null||!Float.isFinite(volume)||volume<0||volume>1||!Float.isFinite(pitch)||pitch<=0||action<0||frameMillis<0)throw new IllegalArgumentException("INVALID_SOUND_INTENT");
    }
}
