package com.atom.lib.frontend.checker;

import javax.imageio.ImageIO;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Bounded scene exchange. Captures are run-scoped evidence, never approval inputs. */
public final class SceneCaptureIO {
    private SceneCaptureIO(){}
    public record Capture(String invocation,String designDigest,List<ScenarioCheck.Sample> samples){public Capture{samples=List.copyOf(samples);}}
    public static void write(Path file,Capture capture)throws IOException {
        if(capture.samples().size()>1000)throw new IOException("SCENE_CAPTURE_LIMIT");
        try(var out=new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(file)))){
            out.writeInt(0x46435301);out.writeUTF(capture.invocation());out.writeUTF(capture.designDigest());out.writeInt(capture.samples().size());
            for(var sample:capture.samples()){
                out.writeUTF(sample.key());out.writeUTF(sample.page());out.writeUTF(sample.recipe());out.writeUTF(sample.scenario());out.writeLong(sample.nanos());out.writeUTF(sample.phase().name());out.writeBoolean(sample.enabled());
                out.writeInt(sample.states().size());for(var entry:new TreeMap<>(sample.states()).entrySet()){out.writeUTF(entry.getKey());out.writeBoolean(entry.getValue());}
                var image=new ByteArrayOutputStream();if(!ImageIO.write(sample.image(),"PNG",image))throw new IOException("MISSING_IMAGE PNG encoder");byte[] png=image.toByteArray();out.writeInt(png.length);out.write(png);
                out.writeInt(sample.observations().size());for(var entry:new TreeMap<>(sample.observations()).entrySet()){out.writeUTF(entry.getKey());out.writeDouble(entry.getValue());}
                out.writeInt(sample.sounds().size());for(var sound:sample.sounds()){out.writeUTF(sound.event());out.writeUTF(sound.resource());out.writeUTF(sound.channel());out.writeFloat(sound.volume());out.writeFloat(sound.pitch());out.writeLong(sound.millis());}
                out.writeInt(sample.resolvedSounds().size());for(String resource:new TreeSet<>(sample.resolvedSounds()))out.writeUTF(resource);
            }
        }
    }
    public static Capture read(Path file)throws IOException {
        if(Files.size(file)>64L*1024*1024)throw new IOException("SCENE_CAPTURE_LIMIT");
        try(var in=new DataInputStream(new BufferedInputStream(Files.newInputStream(file)))){
            if(in.readInt()!=0x46435301)throw new IOException("UNSUPPORTED_SCENE_CAPTURE");String invocation=in.readUTF(),digest=in.readUTF();int count=count(in,1000);List<ScenarioCheck.Sample> samples=new ArrayList<>();
            for(int i=0;i<count;i++){
                String key=in.readUTF(),page=in.readUTF(),recipe=in.readUTF(),scenario=in.readUTF();long nanos=in.readLong();var phase=ScenarioCheck.Phase.valueOf(in.readUTF());boolean enabled=in.readBoolean();
                Map<String,Boolean> states=new TreeMap<>();for(int j=count(in,1000);j>0;j--)put(states,in.readUTF(),in.readBoolean());
                int length=count(in,16*1024*1024);byte[] png=in.readNBytes(length);if(png.length!=length)throw new EOFException("MISSING_IMAGE truncated PNG");
                java.awt.image.BufferedImage image;
                try(var input=ImageIO.createImageInputStream(new ByteArrayInputStream(png))){var readers=ImageIO.getImageReaders(input);if(!readers.hasNext())throw new IOException("MISSING_IMAGE undecodable PNG");var reader=readers.next();try{reader.setInput(input);if(reader.getWidth(0)>8192||reader.getHeight(0)>8192||(long)reader.getWidth(0)*reader.getHeight(0)>16_000_000)throw new IOException("SCENE_IMAGE_LIMIT");image=reader.read(0);}finally{reader.dispose();}}
                Map<String,Double> observations=new TreeMap<>();for(int j=count(in,10000);j>0;j--)put(observations,in.readUTF(),in.readDouble());
                List<SoundObservation> sounds=new ArrayList<>();for(int j=count(in,10000);j>0;j--)sounds.add(new SoundObservation(in.readUTF(),in.readUTF(),in.readUTF(),in.readFloat(),in.readFloat(),in.readLong()));
                Set<String> resolved=new TreeSet<>();for(int j=count(in,10000);j>0;j--)if(!resolved.add(in.readUTF()))throw new IOException("DUPLICATE_SCENE_RESOURCE");
                samples.add(new ScenarioCheck.Sample(key,page,recipe,scenario,nanos,phase,enabled,states,image,observations,sounds,resolved));
            }
            if(in.read()!=-1)throw new IOException("INVALID_SCENE_CAPTURE trailing bytes");return new Capture(invocation,digest,samples);
        }catch(IllegalArgumentException e){throw new IOException("INVALID_SCENE_CAPTURE "+e.getMessage(),e);}
    }
    private static int count(DataInputStream in,int max)throws IOException{int value=in.readInt();if(value<0||value>max)throw new IOException("SCENE_CAPTURE_LIMIT");return value;}
    private static <T>void put(Map<String,T> map,String key,T value)throws IOException{if(map.putIfAbsent(key,value)!=null)throw new IOException("DUPLICATE_SCENE_FIELD "+key);}
}
