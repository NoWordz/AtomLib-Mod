package com.atom.lib.client.input;

import com.atom.lib.controls.*;
import com.atom.lib.input.KeyEvent;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;
import java.util.Objects;

/**
 * Screen callback bridge. Receives GUI coordinates and committed Unicode, not IME preedit.
 * The host must forward callbacks; candidate-window placement is not provided here.
 */
public final class ControlInputAdapter {
    private final ControlScreenAdapter input;
    public ControlInputAdapter(UiSession session){
        input=new ControlScreenAdapter(Objects.requireNonNull(session));
        input.clipboard(new TextClipboard(){
            public String read(){return Minecraft.getInstance().keyboardHandler.getClipboard();}
            public void write(String value){Minecraft.getInstance().keyboardHandler.setClipboard(value);}
        });
    }
    public boolean press(double x,double y,int button){return input.press(x,y,button);}
    public boolean drag(double x,double y,int button,double dx,double dy){return input.drag(x,y,button,dx,dy);}
    public boolean release(double x,double y,int button){return input.release(x,y,button);}
    public boolean scroll(double x,double y,double dx,double dy){return input.scroll(x,y,dx,dy);}
    public void hover(double x,double y){input.hover(x,y);}
    public void windowFocused(boolean value){input.windowFocused(value);}
    public void removed(){input.removed();}
    public boolean keyPressed(int key,int scanCode,int modifiers){return input.key(event(key,true,modifiers));}
    public boolean keyReleased(int key,int scanCode,int modifiers){return input.key(event(key,false,modifiers));}
    /** Minecraft char callbacks may split a supplementary scalar into two UTF-16 units. */
    public boolean character(char value,int modifiers){
        boolean shortcut=(modifiers&(GLFW.GLFW_MOD_CONTROL|GLFW.GLFW_MOD_SUPER))!=0&&(modifiers&GLFW.GLFW_MOD_ALT)==0;
        return !shortcut&&input.character(value);
    }
    /** GLFW code-point callback, when a host can forward it directly. */
    public boolean character(int codePoint){return input.character(codePoint);}
    /** One committed text transaction, for a host with a whole-string submission callback. */
    public boolean text(String committedText){return input.text(committedText);}
    public static KeyEvent event(int key,boolean pressed,int modifiers){
        var mapped=switch(key){
            case GLFW.GLFW_KEY_TAB -> KeyEvent.Key.TAB;
            case GLFW.GLFW_KEY_ESCAPE -> KeyEvent.Key.ESCAPE;
            case GLFW.GLFW_KEY_LEFT -> KeyEvent.Key.LEFT;
            case GLFW.GLFW_KEY_RIGHT -> KeyEvent.Key.RIGHT;
            case GLFW.GLFW_KEY_UP -> KeyEvent.Key.UP;
            case GLFW.GLFW_KEY_DOWN -> KeyEvent.Key.DOWN;
            case GLFW.GLFW_KEY_HOME -> KeyEvent.Key.HOME;
            case GLFW.GLFW_KEY_END -> KeyEvent.Key.END;
            case GLFW.GLFW_KEY_ENTER,GLFW.GLFW_KEY_KP_ENTER -> KeyEvent.Key.ENTER;
            case GLFW.GLFW_KEY_SPACE -> KeyEvent.Key.SPACE;
            case GLFW.GLFW_KEY_BACKSPACE -> KeyEvent.Key.BACKSPACE;
            case GLFW.GLFW_KEY_DELETE -> KeyEvent.Key.DELETE;
            case GLFW.GLFW_KEY_A -> KeyEvent.Key.A;
            case GLFW.GLFW_KEY_C -> KeyEvent.Key.C;
            case GLFW.GLFW_KEY_V -> KeyEvent.Key.V;
            case GLFW.GLFW_KEY_X -> KeyEvent.Key.X;
            case GLFW.GLFW_KEY_Z -> KeyEvent.Key.Z;
            case GLFW.GLFW_KEY_Y -> KeyEvent.Key.Y;
            default -> KeyEvent.Key.OTHER;
        };
        boolean alt=(modifiers&GLFW.GLFW_MOD_ALT)!=0;
        boolean control=(modifiers&GLFW.GLFW_MOD_CONTROL)!=0;
        boolean command=System.getProperty("os.name","").startsWith("Mac")&&(modifiers&GLFW.GLFW_MOD_SUPER)!=0;
        // AltGr may carry CTRL+ALT and must remain a printable character path.
        return new KeyEvent(mapped,pressed,(modifiers&GLFW.GLFW_MOD_SHIFT)!=0,(control||command)&&!alt,alt);
    }
}
