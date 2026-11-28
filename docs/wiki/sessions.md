# Isolated sessions, editors and collections (0.5.0)

Create one `AppearanceSession` per screen/window. It owns its controls, focus, theme and feedback, and borrows the `RuntimeHandle`. The runtime overload registers the bundled font roles and owns only the fonts/text created for that window. The `TextService` overload borrows the supplied service; register `AtomChatFonts` in that service's registry first.

```java
var ui = AtomDefault.createSession(runtime); // retain between frames
ui.selectTheme("dusk");
ui.selectMotion("light"); // independent: standard / light / off
var input = new ControlScreenAdapter(ui.input());
ui.input().setControls(List.of(new TextField(
    new NodeId("name"), ui.context(), "", new TextFieldEvents() {
        @Override public void changed(String draft) { /* local draft */ }
        @Override public void committed(String value) { /* host action */ }
    })));

// Inside the existing MinecraftRenderBridge draw callback, after GUI-unit scaling:
try (var frame = ui.beginFrame(time, viewport)) {
    // Build/measure the existing Yoga tree using ui.context(), then:
    frame.paint(canvas, presentation);
}
// Screen removal: ui.close(). The runtime remains available to other windows.
```

New sessions never read or install `Design.host` / `Design.rippleSink`. Theme and motion selections made while a frame is open take effect at the next frame boundary. A frame must close before the next begins; use try-with-resources even when painting fails. Do not directly mutate the borrowed skin during a frame. `SettingsShell` captures the safe skin's appearance during layout; its chrome, groups and controls then use the same palette. Legacy `SettingsPageStyle` behaviour remains for legacy skins.

`createSession(text, customSkin)` borrows a separate host-owned skin instance. New default methods `beginAppearance(theme, motion)` and `appearanceStyle()` let custom skins consume the snapshot and share chrome roles; existing implementations need no new methods and retain their own painting. The library does not rewrite custom drawing code or make a host's shared/global skin safe.

`ThemeCatalog` is immutable and native-free. `with(palette)` returns a new catalog; `ui.themeCatalog(catalog)` replaces only that session's directory. `ui.theme(palette)` and `ui.motion(spec)` register local custom descriptions; choose them explicitly by ID. Existing twelve theme IDs remain. Elegant and glass secondary colours have been refined; the library never rewrites saved host colours.

`frame.panel(canvas, bounds, radius, backdropOperation)` applies material opacity once. A glass theme invokes the host's actual backdrop operation; return false when unavailable for an opaque fallback. Selecting glass does not capture the world automatically. `frame.card` uses the same snapshot and a session-local feedback sink. `ui.feedbackLayer().pulse(...)` provides bounded, explicit-clock ripples; no process-global sink is involved.

## Motion and preview

`Transition` finishes at the requested duration and reverses from its current value. `RegionMotion` samples a rectangle for host remeasurement. `PageMotion` transforms an existing presentation frame for optional slide/zoom; it does not own navigation. Apply the transformed/clipped frame to painting and input together. `PageEntrance` also accepts a maximum stagger delay. These tools use `FrameTime`; repeated sampling of a frame does not advance it again, and `off` snaps to the final state.

Safe-session controls have bounded hover/press scaling. `ScrollViewport` can use frame-driven smoothing and scrollbar fade; drag and keyboard positioning remain immediate. Low-level users opt in with `advance(time, motion)` and use `wheelBy(delta)`. `SmoothScroll` is also available for host-owned scrolling.

`ui.preview()` creates independent state and borrows text services. Cancel by closing it. `AppearancePicker` builds real `ActionButton` choices for theme/motion; mount its controls in the desired view, paint using `picker.preview()` and apply explicitly with `applyTo(target)`. It never writes configuration. Theme and motion choices may be embedded separately with `themeControls()` / `motionControls()` and their corresponding layout nodes.

## Plain-text and numeric drafts

`TextField`, `TextArea` and `NumberField` work through the existing `UiSession` focus, modal and lifecycle paths. Selection/navigation/deletion operate on grapheme boundaries. Editors support clipboard, undo/redo, selection and local errors; a numeric draft can be empty, incomplete or outside its declared range without silent clamping. Change and valid-submit callbacks are separate. The default clipboard is window-local memory: connect the platform clipboard explicitly via `ControlScreenAdapter.clipboard(...)`.

Forward key down/up (including A/C/V/X/Z/Y, Backspace and Delete), char/text commits, pointer movement, wheel, focus and removal. Key events never insert printable characters. Use either `character` events or a committed `text(String)` event for a platform submission, not both for the same submission. The adapter pairs UTF-16 surrogate callbacks and discards an unfinished pair across focus changes. See the compiled three-platform `ControlsRecipe` and shared `AppearanceRecipe` in `examples/a`.

This is a plain left-to-right editor. Full bidi, styled editing and custom IME candidate rendering are outside scope. Host/game Chinese IME submission and focus changes require real-game validation on each target; offline tests do not establish that result. The optional Windows candidate-window enhancement is not implemented.

## Virtual grids and tables

```java
VirtualData<String, Product> data = /* synchronous host snapshot: count/itemAt/key */;
var grid = VirtualCollection.grid(new NodeId("products"), data, 160, 96, 12);
grid.events(event -> { /* event.key(), action(), column(), kind() */ });
grid.actions((key, card) -> Map.of("open", new Rect(card.right()-36, card.y(), 36, 28)));

// Each frame, in GUI units:
grid.advance(time, frame.motion());
var visible = grid.layout(viewport, contentBounds);
grid.paint(canvas, visible, ui.context(), (c, item, card) -> {
    // Draw host content and actions using item.actions(); no second hit geometry.
});
```

Tables use `VirtualCollection.table(id, data, rowHeight, columns)` with `TableColumn(id, label, width, sortable)`. Pass `ControlContext` to paint labelled headers. Sort clicks emit a column/direction intent and indicator; only the host changes the data order with `data(nextSnapshot)`. Body/item callbacks identify the stable key. Fixed-height items use visible rows plus one prefetch row. Grid columns adapt to width, including one narrow column.

Route collection `pointer`, `hover`, `wheel` and `cancel` from the owning screen, before overlapping page content, and cancel on focus loss/removal. Scrollbars use the displayed frame. Child actions win over row/card clicks, gaps and clipped areas never hit, and failed paint keeps the previous successful hit frame. A replacement cancels captures/hover; selection and scroll anchors follow stable keys. Keys must be unique throughout the snapshot. Override `indexOf` for efficient anchor/selection lookup on large datasets; the default scans only during replacement. Keep separate collection instances per window and close each on removal. Data loading, sorting, filtering, persistence and commercial logic remain host responsibilities. Variable-height rows, frozen columns, editable cells and complex multiselection are outside this version.

## Preview settings assets

AtomLib 0.8.0 adds an opt-in package under `com.atom.lib.design.preview`. Use `PreviewThemes.catalog()` on an individual appearance session started on `minimal`; select a palette explicitly. `PreviewThemes.find(id)` also yields its capsule background and text colors. The existing `AtomDefault.catalog()` retains its own presets.

Create a `PreviewScrollFade` per view and compute a `Window` from the viewport and displayed scroll offset. Apply the same window to content, any backdrop mask and late feedback. The edge fades only while content continues past it. A view that fits keeps normal clipping without a separate fade layer.

`PreviewPageTransition` draws slide or zoom over 240 ms. Call `paint` with a stable ID for each page or tab and paint the header outside the body. Supply the effective rectangular host clip if it is narrower than the body. The page selected by the host receives input through `presented(layout)`, which uses the body as last drawn. Disabled motion switches immediately. Reset the transition before replacing the graphics context. Close both visual effects with the view.

Example A exercises the `PreviewSettingsPage` through its `previewSettingsExample` Gradle task. It uses the locally published platform Maven coordinate and writes PNGs to `build/reports/preview-settings`. The replay uses the same page code for scrolling, clicking a color, switching tabs and interrupting a transition.
