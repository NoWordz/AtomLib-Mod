package com.atom.lib.controls;

/** Read-only measurements of a control instance; motion values come from the rendered engine state. */
public record ControlObservation(boolean enabled,boolean hovered,boolean pressed,float hover,float press,float position) {}
