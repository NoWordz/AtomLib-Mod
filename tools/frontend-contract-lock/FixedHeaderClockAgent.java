package com.atom.lib.frontend.probe;

import java.lang.instrument.*;
import java.security.ProtectionDomain;
import java.time.LocalTime;
import org.objectweb.asm.*;

/** Probe-only business clock input for the unchanged production header. Never included in mod artifacts. */
public final class FixedHeaderClockAgent {
    private FixedHeaderClockAgent(){}
    public static LocalTime now(){return LocalTime.of(17,5);}
    public static void premain(String argument,Instrumentation instrumentation){
        instrumentation.addTransformer(new ClassFileTransformer(){
            @Override public byte[] transform(ClassLoader loader,String name,Class<?> redefining,ProtectionDomain domain,byte[] input)throws IllegalClassFormatException{
                if(!name.equals("com/atom/chat/ui/ShellHeader"))return null;
                var reader=new ClassReader(input);var writer=new ClassWriter(reader,0);int[] replaced={0};
                reader.accept(new ClassVisitor(Opcodes.ASM9,writer){
                    @Override public MethodVisitor visitMethod(int access,String method,String descriptor,String signature,String[] exceptions){
                        return new MethodVisitor(Opcodes.ASM9,super.visitMethod(access,method,descriptor,signature,exceptions)){
                            @Override public void visitMethodInsn(int opcode,String owner,String method,String descriptor,boolean isInterface){
                                if(opcode==Opcodes.INVOKESTATIC&&owner.equals("java/time/LocalTime")&&method.equals("now")&&descriptor.equals("()Ljava/time/LocalTime;")){
                                    replaced[0]++;super.visitMethodInsn(opcode,"com/atom/lib/frontend/probe/FixedHeaderClockAgent",method,descriptor,false);
                                }else super.visitMethodInsn(opcode,owner,method,descriptor,isInterface);
                            }
                        };
                    }
                },0);
                if(replaced[0]!=1)throw new IllegalClassFormatException("FCL_HEADER_CLOCK_UNBOUND count="+replaced[0]);
                System.out.println("FCL_HEADER_CLOCK_BOUND count=1 value=17:05");return writer.toByteArray();
            }
        });
    }
}
