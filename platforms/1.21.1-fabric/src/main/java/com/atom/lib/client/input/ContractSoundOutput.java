package com.atom.lib.client.input;

import com.atom.lib.frontend.contract.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

/** UI-only MASTER output. Uses the normal client sound manager and resource-pack resolution. */
public final class ContractSoundOutput implements UiSoundSink {
    public boolean supports(String channel){return "MASTER".equals(channel);}
    public boolean available(String resource){
        var client=MinecraftClient.getInstance();var id=Identifier.tryParse(resource);if(client==null||id==null)return false;
        var event=client.getSoundManager().get(id);return event!=null&&event.getWeight()>0;
    }
    public void play(UiSoundIntent intent){
        if(!supports(intent.channel())||!available(intent.resource()))throw new IllegalStateException("UNAVAILABLE_CONTRACT_SOUND "+intent.resource());
        var id=Identifier.tryParse(intent.resource());
        MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.master(SoundEvent.of(id),intent.pitch(),intent.volume()));
    }
}
