package com.atom.lib.frontend.contract;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

/** Canonical format used by the build checker and runtime. */
public final class ContractCodec {
    private ContractCodec() {}
    private static final Map<String,Class<?>> TYPES;
    static {
        Map<String,Class<?>> types=new HashMap<>();types.put(ContractDesign.class.getName(),ContractDesign.class);
        for(Class<?> root:List.of(ContractDesign.class,NamedCondition.class))for(Class<?> type:root.getDeclaredClasses())if(type.isRecord()||type.isEnum())types.put(type.getName(),type);
        TYPES=Map.copyOf(types);
    }
    public static byte[] encode(ContractDesign design){DesignResolver.validate(design);try{var bytes=new ByteArrayOutputStream();try(var out=new DataOutputStream(bytes)){out.writeInt(0x46434c03);write(out,design,0);}if(bytes.size()>4_194_304)throw new IOException("package too large");return bytes.toByteArray();}catch(IOException|ReflectiveOperationException e){throw new IllegalArgumentException("INVALID_DESIGN "+e.getMessage(),e);}}
    public static ContractDesign decode(byte[] bytes){
        if(bytes.length>4_194_304)throw new IllegalArgumentException("INVALID_DESIGN package too large");
        try(var in=new DataInputStream(new ByteArrayInputStream(bytes))){
            if(in.readInt()!=0x46434c03)throw new IOException("unsupported format");Object result=read(in,0,new int[]{0});if(!(result instanceof ContractDesign d)||in.read()!=-1)throw new IOException("invalid payload or trailing data");
            DesignResolver.validate(d);if(!Arrays.equals(bytes,encode(d)))throw new IOException("noncanonical encoding");return d;
        }catch(IOException|ReflectiveOperationException|ClassCastException e){throw new IllegalArgumentException("INVALID_DESIGN "+e.getMessage(),e);}
    }
    private static byte[] key(Object value)throws IOException,ReflectiveOperationException{var b=new ByteArrayOutputStream();try(var out=new DataOutputStream(b)){write(out,value,0);}return b.toByteArray();}
    private static List<Object> sorted(Collection<?> values)throws IOException,ReflectiveOperationException{var ordered=new TreeMap<String,Object>();for(Object v:values){String k=HexFormat.of().formatHex(key(v));if(ordered.put(k,v)!=null)throw new IOException("duplicate canonical key");}return new ArrayList<>(ordered.values());}
    private static void write(DataOutputStream out,Object value,int depth)throws IOException,ReflectiveOperationException{
        if(depth>64)throw new IOException("excessive depth");
        if(value==null){out.writeByte(0);return;}if(value instanceof String s){out.writeByte(1);out.writeUTF(s);return;}if(value instanceof Integer n){out.writeByte(2);out.writeInt(n);return;}
        if(value instanceof Double n){out.writeByte(3);out.writeDouble(n);return;}if(value instanceof Boolean b){out.writeByte(4);out.writeBoolean(b);return;}
        Class<?> type=value.getClass();if(value instanceof Enum<?> e){if(!TYPES.containsKey(type.getName()))throw new IOException("unknown enum");out.writeByte(5);out.writeUTF(type.getName());out.writeUTF(e.name());return;}
        if(type.isRecord()&&TYPES.containsKey(type.getName())){out.writeByte(6);out.writeUTF(type.getName());for(var component:type.getRecordComponents())write(out,component.getAccessor().invoke(value),depth+1);return;}
        if(value instanceof Map<?,?> m){out.writeByte(7);out.writeInt(m.size());for(Object k:sorted(m.keySet())){write(out,k,depth+1);write(out,m.get(k),depth+1);}return;}
        if(value instanceof Set<?> s){out.writeByte(8);out.writeInt(s.size());for(Object item:sorted(s))write(out,item,depth+1);return;}
        if(value instanceof List<?> l){out.writeByte(9);out.writeInt(l.size());for(Object item:l)write(out,item,depth+1);return;}
        throw new IOException("unsupported value "+type.getName());
    }
    @SuppressWarnings({"unchecked","rawtypes"})
    private static Object read(DataInputStream in,int depth,int[] count)throws IOException,ReflectiveOperationException{
        if(depth>64||++count[0]>50000)throw new IOException("excessive structure");
        int tag=in.readUnsignedByte();switch(tag){case 0:return null;case 1:return in.readUTF();case 2:return in.readInt();case 3:return in.readDouble();case 4:return in.readBoolean();
            case 5:{Class<?> type=TYPES.get(in.readUTF());if(type==null||!type.isEnum())throw new IOException("unknown enum");return Enum.valueOf((Class)type,in.readUTF());}
            case 6:{Class<?> type=TYPES.get(in.readUTF());if(type==null||!type.isRecord())throw new IOException("unknown record");var components=type.getRecordComponents();Object[] args=new Object[components.length];Class<?>[] types=new Class<?>[components.length];for(int i=0;i<args.length;i++){types[i]=components[i].getType();args[i]=read(in,depth+1,count);}return type.getDeclaredConstructor(types).newInstance(args);}
            case 7,8,9:{int size=in.readInt();if(size<0||size>10000)throw new IOException("collection size");if(tag==7){Map<Object,Object> map=new HashMap<>();for(int i=0;i<size;i++){Object k=read(in,depth+1,count),v=read(in,depth+1,count);if(map.containsKey(k))throw new IOException("duplicate key");map.put(k,v);}return map;}List<Object> values=new ArrayList<>();for(int i=0;i<size;i++)values.add(read(in,depth+1,count));if(tag==9)return values;Set<Object> set=new HashSet<>(values);if(set.size()!=size)throw new IOException("duplicate set member");return set;}
            default:throw new IOException("unknown value tag "+tag);
        }
    }
}
