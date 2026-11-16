package com.atom.lib.controls;

/** Host clipboard port. Each session defaults to an independent in-memory clipboard. */
public interface TextClipboard {
    String read();
    void write(String text);
    static TextClipboard memory(){return new TextClipboard(){
        private String value="";
        public String read(){return value;}
        public void write(String text){value=java.util.Objects.requireNonNull(text);}
    };}
}
