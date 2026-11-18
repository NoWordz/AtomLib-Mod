package com.atom.lib.frontend.checker;

import java.util.*;

/** Bounded JSON data reader for resource dependency descriptors; never evaluates expressions. */
final class ResourceJson {
    private final String input; private int at,nodes;
    private ResourceJson(String input){this.input=input;}
    static Object parse(String input){if(input.length()>4*1024*1024)throw new IllegalArgumentException("RESOURCE_DESCRIPTOR_LIMIT");var p=new ResourceJson(input);Object value=p.value(0);p.space();if(p.at!=input.length())p.bad();return value;}
    private Object value(int depth){
        if(depth>64||++nodes>50000)throw new IllegalArgumentException("RESOURCE_DESCRIPTOR_LIMIT");space();if(at==input.length())return bad();char c=input.charAt(at);
        if(c=='"')return string();
        if(c=='{'){at++;Map<String,Object> map=new LinkedHashMap<>();space();if(take('}'))return map;do{space();String key=string();space();if(!take(':'))return bad();if(map.containsKey(key))return bad();map.put(key,value(depth+1));space();if(take('}'))return map;}while(take(','));return bad();}
        if(c=='['){at++;List<Object> list=new ArrayList<>();space();if(take(']'))return list;do{list.add(value(depth+1));space();if(take(']'))return list;}while(take(','));return bad();}
        for(String literal:List.of("true","false","null"))if(input.startsWith(literal,at)){at+=literal.length();return literal.equals("null")?null:Boolean.valueOf(literal);}
        int begin=at;while(at<input.length()&&"0123456789.eE+-".indexOf(input.charAt(at))>=0)at++;String number=input.substring(begin,at);
        if(!number.matches("-?(0|[1-9][0-9]*)(\\.[0-9]+)?([eE][+-]?[0-9]+)?"))return bad();double n=Double.parseDouble(number);if(!Double.isFinite(n))return bad();return n;
    }
    private String string(){
        if(!take('"'))return bad();StringBuilder out=new StringBuilder();while(at<input.length()){
            char c=input.charAt(at++);if(c=='"')return out.toString();if(c<32)return bad();if(c!='\\'){out.append(c);continue;}
            if(at==input.length())return bad();char e=input.charAt(at++);switch(e){case '"','\\','/'->out.append(e);case 'b'->out.append('\b');case 'f'->out.append('\f');case 'n'->out.append('\n');case 'r'->out.append('\r');case 't'->out.append('\t');case 'u'->{if(at+4>input.length())return bad();String hex=input.substring(at,at+4);if(!hex.matches("[0-9a-fA-F]{4}"))return bad();out.append((char)Integer.parseInt(hex,16));at+=4;}default->{return bad();}}
        }return bad();
    }
    private boolean take(char c){if(at<input.length()&&input.charAt(at)==c){at++;return true;}return false;}
    private void space(){while(at<input.length()&&" \t\r\n".indexOf(input.charAt(at))>=0)at++;}
    private <T>T bad(){throw new IllegalArgumentException("INVALID_RESOURCE_JSON offset="+at);}
}
