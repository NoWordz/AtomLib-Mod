package com.atom.lib.controls;

import com.atom.lib.layout.NodeId;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;

/** Decimal draft editor. Invalid/intermediate drafts never clamp or replace the last committed value. */
public final class NumberField extends TextField {
    public enum Validation { VALID, INCOMPLETE, INVALID, OUT_OF_RANGE }
    private final BigDecimal min,max;
    private final NumberFieldEvents events;
    private BigDecimal committedValue;
    public NumberField(NodeId id,ControlContext context,BigDecimal value,BigDecimal min,BigDecimal max,NumberFieldEvents events){
        super(id,context,Objects.requireNonNull(value).toPlainString(),new TextFieldEvents(){});
        this.min=Objects.requireNonNull(min);this.max=Objects.requireNonNull(max);this.events=Objects.requireNonNull(events);
        if(min.compareTo(max)>0)throw new IllegalArgumentException("Reversed numeric range");
        if(value.compareTo(min)<0||value.compareTo(max)>0)throw new IllegalArgumentException("Initial value outside numeric range");
        committedValue=value;
    }
    public Validation validation(){
        String draft=value();
        if(draft.isEmpty()||draft.matches("[+-]?\\.?")||draft.matches("[+-]?(?:\\d+(?:\\.\\d*)?|\\.\\d+)[eE][+-]?"))return Validation.INCOMPLETE;
        try{
            var value=new BigDecimal(draft);
            return value.compareTo(min)<0||value.compareTo(max)>0?Validation.OUT_OF_RANGE:Validation.VALID;
        }catch(NumberFormatException ex){return Validation.INVALID;}
    }
    public Optional<BigDecimal> parsedValue(){return validation()==Validation.VALID?Optional.of(new BigDecimal(value())):Optional.empty();}
    public BigDecimal committedValue(){return committedValue;}
    @Override protected void changed(String text){events.changed(text);}
    @Override protected boolean validSubmission(){return validation()==Validation.VALID;}
    @Override protected boolean invalid(){return super.invalid()||validation()!=Validation.VALID;}
    @Override protected void committed(String text){
        var next=new BigDecimal(text);committedValue=next;events.committed(next);
    }
}
