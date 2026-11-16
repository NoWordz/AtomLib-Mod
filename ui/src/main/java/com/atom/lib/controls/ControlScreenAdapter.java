package com.atom.lib.controls;
import com.atom.lib.input.*;
import java.util.Objects;
/** Receives GUI units, no second pixel-ratio conversion. */
public final class ControlScreenAdapter {
    private final UiSession session;
    private boolean focused=true;
    private char highSurrogate;
    private Object surrogateTarget;
    public ControlScreenAdapter(UiSession session){this.session=Objects.requireNonNull(session);}
    public boolean press(double x,double y,int button){return pointer(PointerEvent.Type.PRESS,x,y,button);}
    public boolean drag(double x,double y,int button,double dx,double dy){return pointer(PointerEvent.Type.MOVE,x,y,button);}
    public boolean release(double x,double y,int button){return pointer(PointerEvent.Type.RELEASE,x,y,button);}
    private boolean pointer(PointerEvent.Type type,double x,double y,int button){return session.pointer(new PointerEvent(type,(float)x,(float)y,button))==InputResult.CONSUMED;}
    public boolean scroll(double x,double y,double dx,double dy){return session.wheel((float)x,(float)y,(float)dx,(float)dy)==InputResult.CONSUMED;}
    public boolean key(KeyEvent event){return focused&&session.key(event)==InputResult.CONSUMED;}
    public void clipboard(TextClipboard clipboard){session.clipboard(clipboard);}
    public boolean text(String text){clearSurrogate();return focused&&session.text(text)==InputResult.CONSUMED;}
    public boolean character(int codePoint){
        if(!focused){clearSurrogate();return false;}
        var target=session.textInputTarget();
        if(codePoint>=0xd800&&codePoint<=0xdbff){highSurrogate=(char)codePoint;surrogateTarget=target;return target!=null;}
        if(codePoint>=0xdc00&&codePoint<=0xdfff){
            char high=highSurrogate;boolean match=high!=0&&target!=null&&target==surrogateTarget;clearSurrogate();
            return match&&session.character(Character.toCodePoint(high,(char)codePoint))==InputResult.CONSUMED;
        }
        clearSurrogate();return session.character(codePoint)==InputResult.CONSUMED;
    }
    private void clearSurrogate(){highSurrogate=0;surrogateTarget=null;}
    public void hover(double x,double y){session.hover((float)x,(float)y);}
    public void windowFocused(boolean value){
        boolean lost=focused&&!value;focused=value;if(lost)clearSurrogate();
        if(lost)try{session.cancel(CancelReason.FOCUS_LOST);}finally{session.resetPhysicalInputTracking();}
    }
    public void removed(){clearSurrogate();session.cancel(CancelReason.REMOVED);}
}
