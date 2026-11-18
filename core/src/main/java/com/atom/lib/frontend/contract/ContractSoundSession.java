package com.atom.lib.frontend.contract;

import java.util.*;

/** Synchronous action arbitration only. No timing queue, mixer, engine or global state. */
public final class ContractSoundSession {
    public enum Result { SILENT,PLAYED,UNSUPPORTED_CHANNEL,RESOURCE_UNAVAILABLE }
    private final UiSoundSink sink;private final ContractRegistry.PageHandle page;
    private long lastAction=-1,action,frameMillis;private boolean active;private ContractRegistry.BoundSound pending;
    public ContractSoundSession(ContractRegistry.PageHandle page,UiSoundSink sink){this.page=Objects.requireNonNull(page);this.sink=Objects.requireNonNull(sink);}
    public void begin(long action,long frameMillis){if(active||action<=lastAction||frameMillis<0)throw new IllegalStateException("INVALID_ACTION_SEQUENCE");lastAction=action;this.action=action;this.frameMillis=frameMillis;active=true;pending=null;}
    public void request(ContractRegistry.RecipeHandle recipe,String event,Map<String,Double> parameters,Map<String,Boolean> states){if(!active)throw new IllegalStateException("NO_ACTIVE_ACTION");page.require(recipe);var next=recipe.sound(event,parameters,states);if(next.isPresent()&&(pending==null||next.get().priority()>pending.priority()))pending=next.get();}
    public Result end(){if(!active)throw new IllegalStateException("NO_ACTIVE_ACTION");active=false;var selected=pending;pending=null;if(selected==null)return Result.SILENT;
        if(!sink.supports(selected.channel()))return Result.UNSUPPORTED_CHANNEL;if(!sink.available(selected.resource()))return Result.RESOURCE_UNAVAILABLE;
        sink.play(new UiSoundIntent(selected.event(),selected.resource(),selected.channel(),selected.volume(),selected.pitch(),action,frameMillis));return Result.PLAYED;
    }
    public void cancel(){active=false;pending=null;}
}
