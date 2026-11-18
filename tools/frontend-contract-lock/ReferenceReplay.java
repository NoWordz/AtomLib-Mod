package com.atom.chat.config;

import com.atom.chat.ui.UiStagger;
import com.atom.chat.render.PreviewLanguage;
import io.github.humbleui.skija.*;
import java.lang.reflect.*;
import java.nio.file.*;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.*;

/** Replays the existing production settings composition fixture. No Minecraft window or device. */
public final class ReferenceReplay {
    public static void main(String[] args) throws Exception {
        Path out=Path.of(args[0]);Files.createDirectories(out);
        var test=Class.forName("com.atom.chat.config.SharedSettingsFrontendTest");
        Object owner=test.getDeclaredConstructor().newInstance();
        var temp=test.getDeclaredField("temp");temp.setAccessible(true);temp.set(owner,out.resolve("isolated-config"));
        invoke(test,owner,"setup");
        try(var language=PreviewLanguage.install("zh_cn")) {
            AtomChatConfig.get().animationEnabled=true;AtomChatConfig.get().blurEnabled=false;
            // Packaged fonts are required by the launcher; do not accept system-font fallback.
            for(String resource:List.of("/com/atom/lib/fonts/bundled-default.otf","/com/atom/lib/fonts/bundled-default-bold.otf"))
                if(ReferenceReplay.class.getResource(resource)==null)throw new AssertionError("MISSING_FONT "+resource);
            var paneType=Class.forName("com.atom.chat.config.SharedSettingsFrontendTest$Pane");
            var ctor=paneType.getDeclaredConstructor(boolean.class,int.class,String.class);ctor.setAccessible(true);
            var frame=paneType.getDeclaredMethod("frame",Surface.class,long.class);frame.setAccessible(true);
            var chip=paneType.getDeclaredMethod("chip",int.class);chip.setAccessible(true);
            var close=paneType.getDeclaredMethod("close");close.setAccessible(true);
            Object pane=ctor.newInstance(false,420,"zh_cn");
            List<String> samples=new ArrayList<>();
            try(var surface=Surface.makeRasterN32Premul(460,900)) {
                for(int i=0;i<90;i++) {
                    if(i==35)chip.invoke(pane,1); // original settings CHAT -> HISTORY; never ServerConfigPage
                    if(i==66)chip.invoke(pane,0); // reverse to original behavior chip
                    final int tick=i;byte[][] pixels={null};
                    UiStagger.atFrame(1000+i*16,()->{try{pixels[0]=(byte[])frame.invoke(pane,surface,1000L+tick*16);}catch(Exception e){throw new RuntimeException(e);}});
                    if(Set.of(0,4,12,24,34,35,39,47,59,65,66,70,78,89).contains(i)) {
                        String hash=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(pixels[0]));samples.add(i+"\t"+hash);
                        try(var image=surface.makeImageSnapshot();var data=image.encodeToData()){Files.write(out.resolve("frame-"+i+".png"),data.getBytes());}
                    }
                }
                if(samples.stream().map(x->x.split("\t")[1]).distinct().count()<5)throw new AssertionError("MISSING_DYNAMIC_OUTPUT");
            } finally{close.invoke(pane);}
            Files.write(out.resolve("frames.tsv"),samples);
            System.out.println("ORIGINAL_SETTINGS_SOFTWARE_REPLAY "+samples.size()+" production component/composite frames; no game/device acceptance");
        }finally{invoke(test,owner,"restore");}
    }
    private static void invoke(Class<?> type,Object owner,String name)throws Exception{var method=type.getDeclaredMethod(name);method.setAccessible(true);method.invoke(owner);}
}
