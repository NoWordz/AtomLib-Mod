package com.atom.lib.controls;

import com.atom.lib.text.UnicodeBoundaries;
import java.util.*;

/** Session-independent plain-text editor. Positions are UTF-16 grapheme boundaries. */
public final class TextEditModel {
    private record State(String text,int anchor,int caret) {}
    private final boolean multiline;
    private String text;
    private int anchor,caret;
    private List<Integer> boundaries;
    private final Deque<State> undo=new ArrayDeque<>(),redo=new ArrayDeque<>();
    private static final int HISTORY_LIMIT=100;
    public TextEditModel(String text,boolean multiline){this.multiline=multiline;reset(text);}
    public String text(){return text;}
    public int anchor(){return anchor;}
    public int caret(){return caret;}
    public boolean multiline(){return multiline;}
    public int selectionStart(){return Math.min(anchor,caret);}
    public int selectionEnd(){return Math.max(anchor,caret);}
    public String selectedText(){return text.substring(selectionStart(),selectionEnd());}
    /** Host replacement starts a fresh undo history; edits use replaceSelection instead. */
    public void reset(String value){
        String normalized=normalize(value);
        text=normalized;boundaries=UnicodeBoundaries.graphemes(text);
        anchor=caret=text.length();undo.clear();redo.clear();
    }
    public void select(int anchor,int caret){
        checkOffset(anchor);checkOffset(caret);
        if(anchor==caret){this.anchor=this.caret=floor(caret);return;}
        this.anchor=anchor<caret?floor(anchor):ceil(anchor);
        this.caret=anchor<caret?ceil(caret):floor(caret);
    }
    public void selectAll(){select(0,text.length());}
    public boolean replaceSelection(String value){
        String normalized=normalize(value);
        return replaceRange(selectionStart(),selectionEnd(),normalized);
    }
    private boolean replaceRange(int start,int end,String normalized){
        String next=text.substring(0,start)+normalized+text.substring(end);
        validate(next);
        if(text.equals(next)){anchor=caret=ceil(start+normalized.length());return false;}
        remember();text=next;boundaries=UnicodeBoundaries.graphemes(text);
        anchor=caret=ceil(start+normalized.length());return true;
    }
    public void move(int direction,boolean extend){
        if(direction==0)return;
        if(!extend&&anchor!=caret){anchor=caret=direction<0?selectionStart():selectionEnd();return;}
        int index=Collections.binarySearch(boundaries,caret);
        int next=boundaries.get(Math.max(0,Math.min(boundaries.size()-1,index+(direction<0?-1:1))));
        moveTo(next,extend);
    }
    public void moveTo(int offset,boolean extend){checkOffset(offset);caret=floor(offset);if(!extend)anchor=caret;}
    public boolean backspace(){
        if(anchor!=caret)return replaceSelection("");
        if(caret==0)return false;
        return replaceRange(boundaries.get(Collections.binarySearch(boundaries,caret)-1),caret,"");
    }
    public boolean deleteForward(){
        if(anchor!=caret)return replaceSelection("");
        if(caret==text.length())return false;
        return replaceRange(caret,boundaries.get(Collections.binarySearch(boundaries,caret)+1),"");
    }
    public boolean undo(){return restoreFrom(undo,redo);}
    public boolean redo(){return restoreFrom(redo,undo);}
    private boolean restoreFrom(Deque<State> source,Deque<State> target){
        if(source.isEmpty())return false;target.push(state());restore(source.pop());return true;
    }
    private State state(){return new State(text,anchor,caret);}
    private void restore(State state){text=state.text;anchor=state.anchor;caret=state.caret;boundaries=UnicodeBoundaries.graphemes(text);}
    private void remember(){undo.push(state());while(undo.size()>HISTORY_LIMIT)undo.removeLast();redo.clear();}
    private int floor(int offset){int i=Collections.binarySearch(boundaries,offset);return boundaries.get(i>=0?i:-i-2);}
    private int ceil(int offset){int i=Collections.binarySearch(boundaries,offset);return boundaries.get(i>=0?i:-i-1);}
    private void checkOffset(int offset){if(offset<0||offset>text.length())throw new IndexOutOfBoundsException("Invalid editor offset: "+offset);}
    private String normalize(String value){
        String result=Objects.requireNonNull(value).replace("\r\n","\n").replace('\r','\n');
        if(!multiline)result=result.replace('\n',' ');
        validate(result);return result;
    }
    private static void validate(String value){
        // Matches the plain TextService contract before mutating history or state.
        if(value.length()>32768)throw new IllegalArgumentException("Text exceeds 32768 UTF-16 units");
        value.codePoints().forEach(cp->{
            byte d=Character.getDirectionality(cp);
            if(cp>=0xd800&&cp<=0xdfff)throw new IllegalArgumentException("Unpaired UTF-16 surrogate");
            if(d==Character.DIRECTIONALITY_RIGHT_TO_LEFT||d==Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC
                ||cp>=0x202a&&cp<=0x202e||cp>=0x2066&&cp<=0x2069)throw new IllegalArgumentException("Bidi text requires a paragraph editor");
            if(Character.isISOControl(cp)&&cp!='\n')throw new IllegalArgumentException("Unsupported control character");
        });
    }
}
