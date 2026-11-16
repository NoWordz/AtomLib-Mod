// SPDX-License-Identifier: Apache-2.0
package com.atom.lib.controls;
import com.atom.lib.layout.*;
import java.util.*;
import java.util.function.Consumer;
/** Optional actual controls for local theme/motion preview. Applying/saving a selection remains a host action. */
public final class AppearancePicker implements AutoCloseable {
    public record Selection(String theme,String motion){}
    private final AppearanceSession preview;
    private final SegmentedChoice themes,motions;
    public AppearancePicker(NodeId id,AppearanceSession source,Consumer<Selection> change){
        preview=source.preview();Objects.requireNonNull(change);
        themes=new SegmentedChoice(new NodeId(id.value()+"/themes"),preview.context(),themeModel(),value->{preview.selectTheme(value);sync();change.accept(selection());});
        motions=new SegmentedChoice(new NodeId(id.value()+"/motions"),preview.context(),motionModel(),value->{preview.selectMotion(value);sync();change.accept(selection());});
    }
    private ChoiceModel themeModel(){return new ChoiceModel(preview.themes().stream().map(p->new ChoiceModel.Option(p.id(),p.id(),true)).toList(),preview.themeId());}
    private ChoiceModel motionModel(){return new ChoiceModel(preview.motions().stream().map(p->new ChoiceModel.Option(p.id(),p.id(),true)).toList(),preview.motionId());}
    private void sync(){themes.update(themeModel());motions.update(motionModel());}
    public Selection selection(){return new Selection(preview.themeId(),preview.motionId());}
    public AppearanceSession preview(){return preview;}
    public List<ActionButton> themeControls(){return themes.controls();}
    public List<ActionButton> motionControls(){return motions.controls();}
    public LayoutNode themes(LayoutStyle style){return themes.node(style);}
    public LayoutNode motions(LayoutStyle style){return motions.node(style);}
    /** Explicit confirmation; no config IO. Cancel by closing without calling applyTo(). */
    public void applyTo(AppearanceSession target){var value=selection();target.theme(preview.themes().stream().filter(p->p.id().equals(value.theme())).findFirst().orElseThrow());target.motion(preview.motions().stream().filter(p->p.id().equals(value.motion())).findFirst().orElseThrow());target.selectTheme(value.theme());target.selectMotion(value.motion());}
    @Override public void close(){preview.close();}
}
