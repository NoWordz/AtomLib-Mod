package com.atom.lib.controls;
import com.atom.lib.layout.*;
import com.atom.lib.geometry.Viewport;
import com.atom.lib.text.TextService;
import io.github.humbleui.skija.Canvas;
import java.util.Optional;
public interface ControlSkin {
    /** Override to supply collection chrome without adopting the default design package. */
    default CollectionSkin collectionSkin() {
        return new com.atom.lib.design.atomchat.AtomChatCollectionSkin(appearanceStyle()
                .orElseGet(com.atom.lib.design.atomchat.AtomChatControlStyle::defaults));
    }
    /** Optional new-frame hook; old custom skins remain binary compatible. Supply a separate mutable skin per session. */
    default void beginAppearance(com.atom.lib.design.atomdefault.ThemeSnapshot theme,com.atom.lib.motion.MotionSpec motion){}
    /** Whole-frame chrome/input appearance, when this skin participates in the new snapshot contract. */
    default Optional<com.atom.lib.design.atomchat.AtomChatControlStyle> appearanceStyle(){return Optional.empty();}
    /** Empty retains the original duration-based toggle movement. */
    default Optional<ControlSpring> toggleSpring(){return Optional.empty();}
    /** The optional knob position can overshoot; the visual's colour weights remain bounded. */
    default void paintToggle(Canvas c,PresentationFrame f,NodeId id,ToggleModel m,ControlVisual v,TextService text,ControlPresentation p,float knobPosition){paintToggle(c,f,id,m,v,text,p);}
    default LayoutNode actionNode(NodeId id,ActionModel model,LayoutStyle outer,TextService text,float iconSize){throw new UnsupportedOperationException("This skin does not support image slots");}
    default void paintAction(Canvas canvas,PresentationFrame frame,NodeId id,ActionModel model,ControlVisual visual,TextService text,ControlIcon icon,float iconSize){throw new UnsupportedOperationException("This skin does not support image slots");}
    default LayoutNode actionNode(NodeId id,ActionModel m,LayoutStyle outer,TextService text){throw new UnsupportedOperationException("This skin does not support action controls");}
    default void paintAction(Canvas c,PresentationFrame f,NodeId id,ActionModel m,ControlVisual v,TextService text){throw new UnsupportedOperationException("This skin does not support action controls");}
    default LayoutNode sliderNode(NodeId id,SliderModel model,String value,LayoutStyle style,TextService text,ControlPresentation p){return sliderNode(id,model,value,style,text);}
    default void paintSlider(Canvas c,PresentationFrame f,NodeId id,SliderModel m,float value,String label,ControlVisual v,TextService text,ControlPresentation p){paintSlider(c,f,id,m,value,label,v,text);}
    default LayoutNode toggleNode(NodeId id,ToggleModel model,LayoutStyle style,TextService text,ControlPresentation p){return toggleNode(id,model,style,text);}
    default void paintToggle(Canvas c,PresentationFrame f,NodeId id,ToggleModel m,ControlVisual v,TextService text,ControlPresentation p){paintToggle(c,f,id,m,v,text);}
    default LayoutNode modalTitle(NodeId id,String title,TextService text){throw new UnsupportedOperationException("This skin does not support composable modals");}
    default void paintModalSurface(Canvas canvas,PresentationFrame frame,NodeId id,String title,TextService text){throw new UnsupportedOperationException("This skin does not support composable modals");}
    ControlMotion motion();
    LayoutStyle modalDialogStyle(Viewport viewport);
    LayoutNode sliderNode(NodeId id,SliderModel model,String valueText,LayoutStyle outerStyle,TextService text);
    void paintSlider(Canvas canvas,PresentationFrame frame,NodeId id,SliderModel model,float previewValue,String valueText,ControlVisual visual,TextService text);
    LayoutNode toggleNode(NodeId id,ToggleModel model,LayoutStyle outerStyle,TextService text);
    void paintToggle(Canvas canvas,PresentationFrame frame,NodeId id,ToggleModel model,ControlVisual visual,TextService text);
    LayoutNode dialogNode(NodeId id,ConfirmationModel model,LayoutStyle outerStyle,TextService text);
    void paintDialog(Canvas canvas,PresentationFrame frame,NodeId id,ConfirmationModel model,NodeId focusedAction,ControlVisual visual,TextService text);
}
