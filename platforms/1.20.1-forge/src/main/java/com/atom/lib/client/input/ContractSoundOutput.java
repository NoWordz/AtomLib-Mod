package com.atom.lib.client.input;

import com.atom.lib.frontend.contract.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

/** UI-only MASTER output. Uses the normal client sound manager and resource-pack resolution. */
public final class ContractSoundOutput implements UiSoundSink {
    public boolean supports(String channel){return "MASTER".equals(channel);}
    public boolean available(String resource){
        var client=Minecraft.getInstance();var id=ResourceLocation.tryParse(resource);if(client==null||id==null)return false;
        var event=client.getSoundManager().getSoundEvent(id);return event!=null&&event.getWeight()>0;
    }
    public void play(UiSoundIntent intent){
        if(!supports(intent.channel())||!available(intent.resource()))throw new IllegalStateException("UNAVAILABLE_CONTRACT_SOUND "+intent.resource());
        var id=ResourceLocation.tryParse(intent.resource());
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvent.createVariableRangeEvent(id),intent.pitch(),intent.volume()));
    }
}
