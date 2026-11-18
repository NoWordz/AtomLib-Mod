package com.atom.chat.config;

import com.atom.chat.ui.UiStagger;
import com.atom.chat.render.PreviewLanguage;
import com.atom.chat.theme.ThemeService;
import io.github.humbleui.skija.*;
import java.lang.reflect.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/** Current or independently frozen production fixture; caller chooses an explicit classpath. No game window. */
public final class SettingsTrialReplay {
    public static void main(String[] args)throws Exception {
        Path out=Path.of(args[0]);Files.createDirectories(out);boolean hosting=args[1].equals("hosting");String locale=args[2];int width=Integer.parseInt(args[3]);boolean light=args[4].equals("light");
        var test=Class.forName("com.atom.chat.config.SharedSettingsFrontendTest");var ctor=test.getDeclaredConstructor();ctor.setAccessible(true);Object owner=ctor.newInstance();
        var temp=test.getDeclaredField("temp");temp.setAccessible(true);temp.set(owner,out.resolve("isolated-config"));invoke(test,owner,"setup");
        try(var language=PreviewLanguage.install(locale)){
            if(light)ThemeService.MINIMAL.applyTo(AtomChatConfig.get());AtomChatConfig.get().animationEnabled=true;AtomChatConfig.get().blurEnabled=false;
            for(String resource:List.of("/com/atom/lib/fonts/bundled-default.otf","/com/atom/lib/fonts/bundled-default-bold.otf"))if(SettingsTrialReplay.class.getResource(resource)==null)throw new AssertionError("MISSING_FONT "+resource);
            var type=Class.forName("com.atom.chat.config.SharedSettingsFrontendTest$Pane");var create=type.getDeclaredConstructor(boolean.class,int.class,String.class);create.setAccessible(true);Object pane=create.newInstance(hosting,width,locale);
            var frame=type.getDeclaredMethod("frame",Surface.class,long.class);frame.setAccessible(true);var chip=type.getDeclaredMethod("chip",int.class);chip.setAccessible(true);var close=type.getDeclaredMethod("close");close.setAccessible(true);
            List<String> frames=new ArrayList<>();
            try(var surface=Surface.makeRasterN32Premul(width+40,900)){
                for(int i=0;i<90;i++){
                    if(i==35)chip.invoke(pane,1);if(i==66)chip.invoke(pane,0);final int tick=i;
                    UiStagger.atFrame(1000+i*16,()->{try{frame.invoke(pane,surface,1000L+tick*16);}catch(Exception e){throw new RuntimeException(e);}});
                    if(Set.of(0,4,12,24,34,35,39,47,59,65,66,70,78,89).contains(i))try(var image=surface.makeImageSnapshot();var png=image.encodeToData(EncodedImageFormat.PNG)){Files.write(out.resolve("frame-"+i+".png"),png.getBytes());frames.add(Integer.toString(i));}
                }
            }finally{close.invoke(pane);}
            Files.write(out.resolve("frames.txt"),frames);Files.writeString(out.resolve("conditions.txt"),"PRODUCTION_COMPONENT_SOFTWARE_ONLY\npage="+args[1]+"\nlocale="+locale+"\nwidth="+width+"\nlight="+light+"\nanimation=true\nblur=false\nfont=packaged regular+bold\n");System.out.println("PRODUCTION_TRIAL_CAPTURE frames="+frames.size()+" page="+args[1]);
        }finally{invoke(test,owner,"restore");}
    }
    private static void invoke(Class<?> type,Object owner,String name)throws Exception{var method=type.getDeclaredMethod(name);method.setAccessible(true);method.invoke(owner);}
}
