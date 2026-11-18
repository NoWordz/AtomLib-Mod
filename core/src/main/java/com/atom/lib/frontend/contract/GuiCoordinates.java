package com.atom.lib.frontend.contract;

/** MC GUI callbacks and already-composed logical density. Content scaling is included exactly once. */
public record GuiCoordinates(double guiScale,double pixelsPerLogicalUnit) {
    public GuiCoordinates{if(!Double.isFinite(guiScale)||guiScale<=0||!Double.isFinite(pixelsPerLogicalUnit)||pixelsPerLogicalUnit<=0)throw new IllegalArgumentException("INVALID_COORDINATE_SCALE");}
    public record GuiPoint(double x,double y){public GuiPoint{if(!Double.isFinite(x)||!Double.isFinite(y))throw new IllegalArgumentException("INVALID_GUI_POINT");}}
    public record LogicalPoint(float x,float y){public LogicalPoint{if(!Float.isFinite(x)||!Float.isFinite(y))throw new IllegalArgumentException("INVALID_LOGICAL_POINT");}}
    public LogicalPoint toLogical(GuiPoint point){return new LogicalPoint((float)(point.x()*guiScale/pixelsPerLogicalUnit),(float)(point.y()*guiScale/pixelsPerLogicalUnit));}
}
