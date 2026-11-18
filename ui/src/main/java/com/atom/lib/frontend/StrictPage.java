package com.atom.lib.frontend;

import com.atom.lib.frontend.contract.*;
import java.util.*;

/** Page authors supply business content and approved recipe handles, never geometry or paint. */
public final class StrictPage {
    public sealed interface Item permits Text,Number,Image,Action,BooleanValue,TextInput,NumberInput,BooleanInput {}
    public record Text(String value) implements Item {public Text{Objects.requireNonNull(value);}}
    public record Number(double value) implements Item {public Number{if(!Double.isFinite(value))throw new IllegalArgumentException("Invalid business number");}}
    public record Image(String contentHandle) implements Item {public Image{Objects.requireNonNull(contentHandle);}}
    public record BooleanValue(boolean value) implements Item {}
    public record TextInput(String value,java.util.function.Predicate<String> commit) implements Item {public TextInput{Objects.requireNonNull(value);Objects.requireNonNull(commit);}}
    public record NumberInput(double value,java.util.function.DoublePredicate commit) implements Item {public NumberInput{if(!Double.isFinite(value))throw new IllegalArgumentException("Invalid business number");Objects.requireNonNull(commit);}}
    public record BooleanInput(boolean value,java.util.function.Predicate<Boolean> commit) implements Item {public BooleanInput{Objects.requireNonNull(commit);}}
    public record Action(java.util.function.BooleanSupplier commit) implements Item {
        public Action{Objects.requireNonNull(commit);}
        public Action(Runnable action){this(accepted(action));}
        private static java.util.function.BooleanSupplier accepted(Runnable action){Objects.requireNonNull(action);return ()->{action.run();return true;};}
    }
    public record Content(Map<String,Item> slots,boolean enabled) {public Content{slots=Map.copyOf(slots);}}
    public record Entry(String id,ContractRegistry.RecipeHandle recipe,Content content) {}
    private final ContractRegistry.PageHandle page;
    private final List<Entry> entries;
    private StrictPage(ContractRegistry.PageHandle page,List<Entry> entries){this.page=page;this.entries=List.copyOf(entries);}
    public ContractRegistry.PageHandle contract(){return page;}
    public List<Entry> entries(){return entries;}
    public static Builder builder(ContractRegistry.PageHandle page){return new Builder(Objects.requireNonNull(page));}
    static void validate(ContractRegistry.RecipeHandle recipe,Content content){
        Objects.requireNonNull(content);
        if(!recipe.slots().keySet().containsAll(content.slots().keySet()))throw new IllegalArgumentException("CONTENT_PURPOSE recipe="+recipe.id()+" expected="+recipe.slots().keySet()+" actual="+content.slots().keySet());
        recipe.slots().forEach((name,slot)->{Item item=content.slots().get(name);if(item==null){if(slot.required())throw new IllegalArgumentException("MISSING_CONTENT recipe="+recipe.id()+" slot="+name);return;}
            boolean valid=switch(slot.kind()){case TEXT->item instanceof Text||item instanceof TextInput;case BUSINESS_IMAGE->item instanceof Image;case ACTION->item instanceof Action;case NUMBER->item instanceof Number||item instanceof NumberInput;case BOOLEAN->item instanceof BooleanValue||item instanceof BooleanInput;};
            if(!valid)throw new IllegalArgumentException("CONTENT_PURPOSE recipe="+recipe.id()+" slot="+name+" expected="+slot.kind()+" actual="+item.getClass().getSimpleName());
        });
    }
    public static final class Builder {
        private final ContractRegistry.PageHandle page;private final List<Entry> entries=new ArrayList<>();private final Set<String> ids=new HashSet<>();
        private Builder(ContractRegistry.PageHandle page){this.page=page;}
        public Builder component(String id,ContractRegistry.RecipeHandle recipe,Content content){
            if(id==null||!id.matches("[a-zA-Z0-9_.-]+")||ids.contains(id))throw new IllegalArgumentException("DUPLICATE_OR_RESERVED_COMPONENT page="+page.id()+" id="+id);
            page.require(recipe);validate(recipe,content);ids.add(id);entries.add(new Entry(id,recipe,content));return this;
        }
        public StrictPage build(){if(entries.isEmpty())throw new IllegalArgumentException("MISSING_EVIDENCE page="+page.id()+" has no component instances");return new StrictPage(page,entries);}
    }
}
