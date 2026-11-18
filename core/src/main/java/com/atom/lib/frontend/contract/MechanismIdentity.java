package com.atom.lib.frontend.contract;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Build-produced interpreter identity; no Minecraft classes, clocks or environment inputs. */
public final class MechanismIdentity {
    public static final String BINDING="mechanism/atomlib";
    private static final String RESOURCE="META-INF/atomlib/fcl/mechanism.sha256";
    private MechanismIdentity(){}
    public static String current(){
        try{
            var resources=MechanismIdentity.class.getClassLoader().getResources(RESOURCE);String identity=null;
            while(resources.hasMoreElements()){
                String next;try(var stream=resources.nextElement().openStream()){next=new String(stream.readNBytes(256),StandardCharsets.US_ASCII).trim();}
                if(!next.matches("[a-f0-9]{64}"))throw new IOException("invalid mechanism metadata");
                if(identity!=null&&!identity.equals(next))throw new IOException("conflicting library mechanism metadata");
                identity=next;
            }
            if(identity==null)throw new IOException("missing library mechanism metadata");return identity;
        }catch(IOException e){throw new IllegalArgumentException("INCOMPATIBLE_PROFILE "+e.getMessage(),e);}
    }
    static void require(ContractDesign design){
        String expected=design.buildBindings().get(BINDING);
        // Unbound packages remain useful for isolated model probes. Protocol-2 builds always add this binding.
        if(expected!=null){String actual=current();if(!expected.equals(actual))throw new IllegalArgumentException("INCOMPATIBLE_PROFILE "+design.profile()+" expected mechanism="+expected+" actual="+actual+"; retain the compatible library or review a migrated design");}
    }
}
