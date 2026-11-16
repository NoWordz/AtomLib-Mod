package com.atom.example.verification;
import com.atom.lib.*;
import com.atom.lib.controls.*;
import com.atom.lib.design.atomdefault.*;
import com.atom.lib.geometry.*;
import com.atom.lib.motion.*;
/** Runs against the published platform coordinate, without Minecraft or a project dependency. */
public final class SessionConsumerProbe {
    public static void main(String[] args){
        try(var runtime=AtomLibRuntime.open("independent-consumer");var a=AtomDefault.createSession(runtime);var b=AtomDefault.createSession(runtime)){
            a.selectTheme("dusk");b.selectTheme("minimal");a.selectMotion("off");
            var time=new FrameTime(1,0,0);var viewport=new Viewport(240,160,1);
            try(var af=a.beginFrame(time,viewport);var bf=b.beginFrame(time,viewport)){
                if(af.theme().controls().panelBackgroundColor()==bf.theme().controls().panelBackgroundColor())throw new AssertionError("Host theme collision");
                if(af.motion().enabled()||!bf.motion().enabled())throw new AssertionError("Host motion collision");
            }
            try(var preview=a.preview()){preview.selectTheme("raven");if(!a.themeId().equals("dusk"))throw new AssertionError("Preview leaked");}
            var draft=new TextEditModel("A👩‍💻é",false);draft.select(draft.text().length(),draft.text().length());draft.backspace();draft.backspace();if(!draft.text().equals("A"))throw new AssertionError("Split grapheme");
            a.close();if(runtime.isClosed()||b.isClosed())throw new AssertionError("Closing one view closed its host");
            try(var frame=b.beginFrame(new FrameTime(2,1,0),viewport)){if(!frame.theme().palette().id().equals("minimal"))throw new AssertionError("Sibling changed");}
        }
        System.out.println("Session consumer passed");
    }
}
