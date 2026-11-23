package com.atom.lib.frontend.checker;

import com.atom.lib.frontend.contract.*;
import java.lang.reflect.RecordComponent;
import java.util.*;

/** Stable, field-level review output. Affected pages are a conservative transitive impact bound. */
public final class DesignDiff {
    private DesignDiff() {}
    public static List<String> compare(byte[] reviewed,byte[] candidate,Properties before,Properties after) {
        List<String> changes=new ArrayList<>();
        ContractDesign old=ContractCodec.decode(reviewed),next=ContractCodec.decode(candidate);
        walk("",old,next,changes);
        Set<String> keys=new TreeSet<>(before.stringPropertyNames());keys.addAll(after.stringPropertyNames());
        for(String key:keys)if(!Objects.equals(before.getProperty(key),after.getProperty(key)))
            changes.add("CONFIG_CHANGE "+key+" old="+display(before.getProperty(key))+" new="+display(after.getProperty(key)));
        if(!changes.isEmpty()){
            Set<String> pages=new TreeSet<>(old.pages().keySet());pages.addAll(next.pages().keySet());
            changes.add("IMPACT_SCOPE conservative all declared pages; production entry classification remains authoritative");
            pages.forEach(page->changes.add("AFFECTED_PAGE "+page));
        }
        return List.copyOf(changes);
    }
    private static void walk(String path,Object a,Object b,List<String> out){
        if(Objects.equals(a,b))return;
        if(a instanceof Map<?,?> am&&b instanceof Map<?,?> bm){
            Set<String> keys=new TreeSet<>();am.keySet().forEach(k->keys.add(k.toString()));bm.keySet().forEach(k->keys.add(k.toString()));
            for(String key:keys){Object ak=am.keySet().stream().filter(k->k.toString().equals(key)).findFirst().orElse(null),bk=bm.keySet().stream().filter(k->k.toString().equals(key)).findFirst().orElse(null);walk(path+"/"+key,ak==null?null:am.get(ak),bk==null?null:bm.get(bk),out);}return;
        }
        if(a!=null&&b!=null&&a.getClass()==b.getClass()&&a.getClass().isRecord()){
            try{for(RecordComponent field:a.getClass().getRecordComponents())walk(path+"/"+field.getName(),field.getAccessor().invoke(a),field.getAccessor().invoke(b),out);}
            catch(ReflectiveOperationException e){throw new IllegalArgumentException("INVALID_DESIGN_DIFF",e);}return;
        }
        out.add("DESIGN_CHANGE "+(path.startsWith("/")?path.substring(1):path)+" old="+display(a)+" new="+display(b));
    }
    private static String display(Object value){
        String text=value instanceof Set<?> set?set.stream().map(Object::toString).sorted().toList().toString():String.valueOf(value);
        text=text.replace("\\","\\\\").replace("\n","\\n").replace("\r","\\r");return text.length()>500?text.substring(0,500)+"… [see canonical input]":text;
    }
}
