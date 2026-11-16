package com.atom.lib.controls;
/** Skin timing in seconds. */
public record ControlMotion(double hover,double toggle,double modal,double hoverExit){
    public ControlMotion(double hover,double toggle,double modal){this(hover,toggle,modal,hover);}
    public ControlMotion{for(double d:new double[]{hover,toggle,modal,hoverExit})if(!Double.isFinite(d)||d<=0)throw new IllegalArgumentException("Invalid motion duration");}
}
