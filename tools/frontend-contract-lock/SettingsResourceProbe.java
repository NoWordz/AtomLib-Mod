package com.atom.lib.frontend.probe;

import com.atom.lib.frontend.checker.ResourceCheck;
import com.atom.lib.frontend.contract.*;
import com.atom.lib.frontend.contract.ContractDesign.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.zip.ZipFile;

/** Actual installed-reference assets against the candidate resolver, not a new approval source. */
public final class SettingsResourceProbe {
    public static void main(String[] args)throws Exception{
        Path reference=Path.of(args[0]),candidate=Path.of(args[1]);Properties pins=new Properties();Map<String,Asset> assets=new TreeMap<>();
        for(String cue:List.of("button","panel_open","send"))assets.put(cue,new Asset(AssetKind.SOUND,"atomchat:feedback_"+cue,"user-approved original settings sound"));
        for(String font:List.of("bundled-default.otf","bundled-default-bold.otf"))assets.put(font,new Asset(AssetKind.FONT,"classpath:com/atom/lib/fonts/"+font,"installed original AtomLib font"));
        for(Path jar:List.of(reference,Path.of(args[2])))try(var zip=new ZipFile(jar.toFile())){
            for(var entry:Collections.list(zip.entries())){
                String name=entry.getName();
                if(name.equals("assets/atomchat/sounds.json")||name.startsWith("assets/atomchat/sounds/feedback_")&&name.endsWith(".ogg")||name.matches("com/atom/lib/fonts/bundled-default(?:-bold)?\\.otf"))
                    pins.setProperty("resource."+name,HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(zip.getInputStream(entry).readAllBytes())));
            }
        }
        if(pins.size()!=6)throw new AssertionError("MISSING_REFERENCE_RESOURCES expected6 actual="+pins.size());
        // Asset resolution does not require a page fixture: inspect follows actual declared resources only.
        var design=new ContractDesign(1,"atomchat-settings","candidate","atomchat-settings/1",Map.of(),Set.of(),Map.of(),Map.of(),Map.of(),Map.of(),Map.of(),assets,Map.of());
        var good=ResourceCheck.inspect(design,List.of(),List.of(candidate,Path.of(args[3])),pins);
        if(!good.diagnostics().isEmpty()||good.resources().size()!=6)throw new AssertionError(good);
        pins.setProperty("resource.com/atom/lib/fonts/bundled-default.otf","0".repeat(64));
        var changed=ResourceCheck.inspect(design,List.of(),List.of(candidate,Path.of(args[3])),pins);
        if(changed.diagnostics().stream().noneMatch(s->s.startsWith("RESOURCE_CHANGED com/atom/lib/fonts/bundled-default.otf")))throw new AssertionError(changed);
        System.out.println("REAL_SETTINGS_RESOURCE_CLOSURE valid=6 changed-font=RESOURCE_CHANGED; no device/listening claim");
    }
}
