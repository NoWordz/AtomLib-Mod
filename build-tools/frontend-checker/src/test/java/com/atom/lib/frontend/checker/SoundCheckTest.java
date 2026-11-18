package com.atom.lib.frontend.checker;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class SoundCheckTest {
    private final SoundObservation click=new SoundObservation("release","atomchat:feedback_button","MASTER",.6f,1f,25);
    @Test void omissionsDuplicatesAndUnresolvedResourcesFail() {
        assertTrue(SoundCheck.compare(false,List.of(click),List.of(click),Set.of(click.resource())).isEmpty());
        assertTrue(SoundCheck.compare(false,List.of(click),List.of(),Set.of(click.resource())).stream().anyMatch(s->s.startsWith("SOUND_EVENT_COUNT ")));
        assertFalse(SoundCheck.compare(false,List.of(click),List.of(click,click),Set.of(click.resource())).isEmpty());
        assertTrue(SoundCheck.compare(false,List.of(click),List.of(click),Set.of()).stream().anyMatch(s->s.startsWith("SOUND_RESOURCE_MISSING ")));
    }
    @Test void checksActualChannelVolumePitchEventAndTiming() {
        for(var wrong:List.of(new SoundObservation("press",click.resource(),"MASTER",.6f,1,25),new SoundObservation("release",click.resource(),"BLOCKS",.6f,1,25),new SoundObservation("release",click.resource(),"MASTER",.5f,1,25),new SoundObservation("release",click.resource(),"MASTER",.6f,1.1f,25),new SoundObservation("release",click.resource(),"MASTER",.6f,1,26))) {
            assertTrue(SoundCheck.compare(false,List.of(click),List.of(wrong),Set.of(click.resource())).stream().anyMatch(s->s.startsWith("SOUND_PARAMETER_DRIFT ")));
        }
    }
    @Test void silenceMustBeExplicitAndMustProduceNoEvents() {
        assertTrue(SoundCheck.compare(true,List.of(),List.of(),Set.of()).isEmpty());
        assertFalse(SoundCheck.compare(false,List.of(),List.of(),Set.of()).isEmpty());
        assertFalse(SoundCheck.compare(true,List.of(),List.of(click),Set.of(click.resource())).isEmpty());
        assertFalse(SoundCheck.compare(true,List.of(click),List.of(),Set.of(click.resource())).isEmpty());
    }
}
