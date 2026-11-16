package com.atom.lib.controls;

import com.atom.lib.design.atomchat.AtomChatControlSkin;
import com.atom.lib.design.atomchat.AtomChatFonts;
import com.atom.lib.geometry.Rect;
import com.atom.lib.input.*;
import com.atom.lib.layout.*;
import com.atom.lib.text.*;
import io.github.humbleui.skija.*;
import java.util.*;
import java.util.function.BooleanSupplier;

/** A plain-text field. Printable input arrives only through character/text, never key(). */
public sealed class TextField extends Control permits TextArea,NumberField {
    private final TextEditModel edit;
    private final TextFieldEvents events;
    private boolean enabled=true,enterHeld;
    private boolean rejectedInput;
    private String placeholder="";
    private TextInputStyle explicitStyle;
    private float scrollX,scrollY;
    private record InputFrame(TextLayout layout,Rect area,float scrollX,float scrollY) {}
    private InputFrame displayedInput,pendingInput;
    public TextField(NodeId id,ControlContext context,String value,TextFieldEvents events){this(id,context,value,false,events);}
    protected TextField(NodeId id,ControlContext context,String value,boolean multiline,TextFieldEvents events){
        super(id,context);edit=new TextEditModel(value,multiline);this.events=Objects.requireNonNull(events);
    }
    public TextEditModel edit(){return edit;}
    public String value(){return edit.text();}
    /** Explicit host replacement; does not emit an editing or submission callback. */
    public void value(String value){edit.reset(value);rejectedInput=false;scrollX=scrollY=0;}
    public boolean rejectedInput(){return rejectedInput;}
    public void placeholder(String value){
        String next=Objects.requireNonNull(value);
        new TextRequest(next,style().font(),style().fontSize(),style().muted());
        placeholder=next;
    }
    public void inputStyle(TextInputStyle value){explicitStyle=Objects.requireNonNull(value);}
    public void enabled(boolean value){if(enabled==value)return;enabled=value;if(!value)terminate(CancelReason.DISABLED);}
    public float horizontalScroll(){return scrollX;}
    public float verticalScroll(){return scrollY;}
    private TextInputStyle style(){
        if(explicitStyle!=null)return explicitStyle;
        var base=TextInputStyle.defaults();
        var appearance=context.skin().appearanceStyle();
        if(appearance.isPresent()||context.skin() instanceof AtomChatControlSkin){
            var s=appearance.orElseGet(()->((AtomChatControlSkin)context.skin()).style());
            return new TextInputStyle(AtomChatFonts.BODY,base.fontSize(),base.padding(),base.singleLineHeight(),base.multilineHeight(),
                s.cornerRadius(),s.cardColor(),s.textColor(),s.textSecondaryColor(),s.accentColor(),base.error());
        }
        return base;
    }
    public LayoutNode node(LayoutStyle outer){
        var s=style();float h=edit.multiline()?s.multilineHeight():s.singleLineHeight();
        var geometry=outer.size(outer.width(),outer.height().equals(Length.auto())?Length.points(h):outer.height());
        return LayoutNode.leaf(id().value(),geometry,c->new MeasuredSize(c.width().constrain(220),c.height().constrain(h),s.fontSize()),s.hashCode());
    }
    Set<NodeId> slots(){return Set.of(id());}
    boolean enabled(){return enabled;}
    boolean interacting(){return pressed||enterHeld;}
    boolean press(PointerEvent event,PresentationFrame frame){
        if(event.button()!=0||displayedInput==null)return false;
        pressed=true;int offset=hit(event,frame);edit.select(offset,offset);return true;
    }
    void drag(PointerEvent event,PresentationFrame frame){
        if(pressed&&displayedInput!=null)edit.select(edit.anchor(),hit(event,frame));
    }
    void release(PointerEvent event,PresentationFrame frame){if(pressed)drag(event,frame);pressed=false;}
    void cancel(CancelReason reason){pressed=false;enterHeld=false;}
    private int hit(PointerEvent event,PresentationFrame frame){
        var d=displayedInput;float scale=frame.renderScale(id());
        float x=event.x()/scale-d.area.x()+d.scrollX,y=event.y()/scale-d.area.y()+d.scrollY;
        return Math.min(edit.text().length(),d.layout.hitTest(x,y));
    }
    private boolean change(BooleanSupplier mutation){
        boolean changed;
        try{changed=mutation.getAsBoolean();}
        catch(IllegalArgumentException rejected){rejectedInput=true;events.rejected(rejected.getMessage());return false;}
        rejectedInput=false;
        if(changed)try{changed(edit.text());}catch(RuntimeException|Error failure){abort(failure);throw failure;}
        return changed;
    }
    protected void changed(String text){events.changed(text);}
    protected boolean validSubmission(){return true;}
    protected void committed(String text){events.committed(text);}
    protected boolean invalid(){return rejectedInput;}
    /** Submit explicitly. Invalid numeric drafts remain visible and return false. */
    public boolean submit(){
        if(!enabled||!validSubmission())return false;
        committed(edit.text());return true;
    }
    @Override boolean character(int codePoint){return text(new String(Character.toChars(codePoint)));}
    @Override boolean text(String value){
        if(!available())return false;
        change(()->edit.replaceSelection(value));return true;
    }
    boolean key(KeyEvent e){
        if(!available())return false;
        if(e.key()==KeyEvent.Key.ENTER){
            if(!e.pressed()){enterHeld=false;return true;}
            if(enterHeld)return true;enterHeld=true;
            if(edit.multiline()&&!e.control())change(()->edit.replaceSelection("\n"));else submit();
            return true;
        }
        if(!e.pressed())return false;
        if(e.control()&&!e.alt()){
            switch(e.key()){
                case A -> {edit.selectAll();return true;}
                case C -> {if(edit.selectionStart()!=edit.selectionEnd())clipboard().write(edit.selectedText());return true;}
                case X -> {if(edit.selectionStart()!=edit.selectionEnd()){clipboard().write(edit.selectedText());change(()->edit.replaceSelection(""));}return true;}
                case V -> {String value=clipboard().read();change(()->edit.replaceSelection(value));return true;}
                case Z -> {change(e.shift()?edit::redo:edit::undo);return true;}
                case Y -> {change(edit::redo);return true;}
                default -> {}
            }
        }
        switch(e.key()){
            case LEFT -> {if(e.control())word(-1,e.shift());else edit.move(-1,e.shift());return true;}
            case RIGHT -> {if(e.control())word(1,e.shift());else edit.move(1,e.shift());return true;}
            case HOME,END -> {edge(e.key()==KeyEvent.Key.HOME,e.control(),e.shift());return true;}
            case UP,DOWN -> {if(!edit.multiline())return false;vertical(e.key()==KeyEvent.Key.UP?-1:1,e.shift());return true;}
            case BACKSPACE -> {change(edit::backspace);return true;}
            case DELETE -> {change(edit::deleteForward);return true;}
            default -> {return false;}
        }
    }
    private TextClipboard clipboard(){return owner==null?localClipboard:owner.clipboard();}
    private final TextClipboard localClipboard=TextClipboard.memory();
    private void moveTo(int offset,boolean extend){edit.select(extend?edit.anchor():offset,offset);}
    private void word(int direction,boolean extend){
        int originalAnchor=edit.anchor(),offset=edit.caret();String text=edit.text();
        // Move only through model graphemes; whitespace separates plain-text words.
        if(direction<0){
            while(offset>0&&Character.isWhitespace(text.codePointBefore(offset))){edit.select(offset,offset);edit.move(-1,false);offset=edit.caret();}
            while(offset>0&&!Character.isWhitespace(text.codePointBefore(offset))){edit.select(offset,offset);edit.move(-1,false);offset=edit.caret();}
        }else{
            while(offset<text.length()&&!Character.isWhitespace(text.codePointAt(offset))){edit.select(offset,offset);edit.move(1,false);offset=edit.caret();}
            while(offset<text.length()&&Character.isWhitespace(text.codePointAt(offset))){edit.select(offset,offset);edit.move(1,false);offset=edit.caret();}
        }
        // Restore the original selection anchor when extending.
        edit.select(extend?originalAnchor:offset,offset);
    }
    private void edge(boolean start,boolean document,boolean extend){
        int target=start?0:edit.text().length();
        if(!document&&edit.multiline()){
            var lines=currentLayout().lines();var line=lineAt(lines,edit.caret());target=start?line.start():line.end();
        }
        moveTo(target,extend);
    }
    private void vertical(int direction,boolean extend){
        var layout=currentLayout();var lines=layout.lines();if(lines.isEmpty())return;
        var line=lineAt(lines,edit.caret());int index=lines.indexOf(line);
        var next=lines.get(Math.max(0,Math.min(lines.size()-1,index+direction)));
        moveTo(Math.min(edit.text().length(),layout.hitTest(caretX(line,edit.caret()),next.top()+next.height()/2)),extend);
    }
    private TextLayout currentLayout(){
        float width=displayedInput==null?220:displayedInput.area.width();return layout(width,style());
    }
    private TextLayout layout(float width,TextInputStyle s){
        String value=edit.text().isEmpty()?" ":edit.text();
        return context.text().layout(new TextRequest(value,s.font(),s.fontSize(),s.foreground()),
            new Constraints(edit.multiline()?Constraints.Axis.atMost(width):Constraints.Axis.undefined(),Constraints.Axis.undefined()));
    }
    private static TextLayout.Line lineAt(List<TextLayout.Line> lines,int offset){
        var result=lines.get(0);
        for(var line:lines){if(line.start()>offset)break;result=line;}
        return result;
    }
    private static float caretX(TextLayout.Line line,int offset){
        float result=0;for(var caret:line.carets()){if(caret.offset()>offset)break;result=caret.x();}return result;
    }
    void paint(Canvas canvas,PresentationFrame frame,ControlVisual visual){
        var s=style();var r=frame.box(id()).border();
        var area=new Rect(r.x()+s.padding(),r.y()+s.padding(),Math.max(0,r.width()-2*s.padding()),Math.max(0,r.height()-2*s.padding()));
        var layout=layout(area.width(),s);var line=lineAt(layout.lines(),edit.caret());
        float cx=edit.text().isEmpty()?0:caretX(line,edit.caret());
        scrollX=Math.max(0,Math.max(cx-area.width()+1,Math.min(scrollX,cx)));
        scrollX=Math.min(scrollX,Math.max(0,layout.size().width()-area.width()+1));
        scrollY=edit.multiline()?Math.max(0,Math.max(line.top()+line.height()-area.height(),Math.min(scrollY,line.top()))):0;
        float originY=edit.multiline()?area.y():area.y()+Math.max(0,(area.height()-line.height())/2);
        var textArea=new Rect(area.x(),originY,area.width(),area.height());
        int saved=canvas.getSaveCount();canvas.save();
        try(var paint=new Paint().setAntiAlias(true)){
            canvas.drawRRect(io.github.humbleui.types.RRect.makeXYWH(r.x(),r.y(),r.width(),r.height(),s.radius()),paint.setColor(s.background()));
            if(visual.focused()||invalid()){
                canvas.drawRRect(io.github.humbleui.types.RRect.makeXYWH(r.x()+.5f,r.y()+.5f,Math.max(0,r.width()-1),Math.max(0,r.height()-1),s.radius()),
                    paint.setMode(PaintMode.STROKE).setStrokeWidth(1).setColor(invalid()?s.error():s.accent()));
                paint.setMode(PaintMode.FILL);
            }
            canvas.clipRect(io.github.humbleui.types.Rect.makeXYWH(area.x(),area.y(),area.width(),area.height()));
            if(!enabled)canvas.saveLayerAlpha(null,115);
            if(edit.text().isEmpty()&&!placeholder.isEmpty()){
                var hint=context.text().layout(new TextRequest(placeholder,s.font(),s.fontSize(),s.muted(),edit.multiline()?Integer.MAX_VALUE:1,false),
                    new Constraints(Constraints.Axis.atMost(area.width()),Constraints.Axis.atMost(area.height())));
                context.text().draw(canvas,hint,area.x(),originY);
            }else if(!edit.text().isEmpty()){
                for(var selection:layout.selection(edit.anchor(),edit.caret()))canvas.drawRect(io.github.humbleui.types.Rect.makeXYWH(area.x()+selection.x()-scrollX,originY+selection.y()-scrollY,selection.width(),selection.height()),paint.setColor((s.accent()&0x00ffffff)|0x55000000));
                context.text().draw(canvas,layout,area.x()-scrollX,originY-scrollY);
            }
            if(visual.focused()&&enabled)canvas.drawRect(io.github.humbleui.types.Rect.makeXYWH(area.x()+cx-scrollX,originY+line.top()-scrollY,1,line.height()),paint.setColor(s.foreground()));
            pendingInput=new InputFrame(layout,textArea,scrollX,scrollY);
        }finally{canvas.restoreToCount(saved);}
    }
    /** UiSession calls this only after the entire frame paints successfully. */
    void publishInputFrame(){displayedInput=pendingInput;}
}
