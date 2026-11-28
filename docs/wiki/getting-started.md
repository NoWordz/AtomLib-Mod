# Getting started

Download the platform JAR that matches your loader and Minecraft version and
add it to the host's `mods/` folder. [AtomLib v0.8.0 release](https://github.com/NoWordz/AtomLib-Mod/releases/tag/v0.8.0)

For development, extract the seven-module Maven archive and set the Maven
repository URL below to its extraction directory. If building from source, use
`build/test-maven` directly. [Download atomlib-input-0.8.0.zip](https://github.com/NoWordz/AtomLib-Mod/releases/download/v0.8.0/atomlib-input-0.8.0.zip)

```groovy
repositories { maven { url = uri('/absolute/path/to/extracted-atomlib-input') }
             mavenCentral() }
dependencies {
    // Fabric: modImplementation; Forge/NeoForge: implementation
    implementation 'com.atom.lib:atomlib-1.20.1-forge:0.8.0'
}
```

## Client natives

Open the runtime from client code after `NativePreparation.status().state()`
reports `AVAILABLE`. Use `NativePreparationScreen.whenReady(parent, factory)`
to defer creating a screen until Skija and Yoga are ready.

On macOS and Linux, set the JVM property `atomlib.natives.source` to `mirror`,
`central` or `offline` to override the default `auto` mode. In offline mode
with an empty cache, import the matching release ZIP as-is from **AtomLib
native libraries** in the title screen.

## Runtime and rendering

Register a font supplied by the host for priority, and call `registerDefaults()`
so text renders CJK out of the box
through the bundled subsetted Noto Sans SC (GB2312-complete hanzi, kana,
Latin-1 supplement, punctuation and kaomoji symbols; emoji, Hangul words and
GBK extensions are intentionally excluded). Host fonts win: the bundled faces
join the fallback chain behind whatever the host set. The charset contract and
the regeneration workflow live in `tools/default-font-charset.txt` and
`tools/subset_default_font.py`.

```java
try (var runtime = AtomLibRuntime.open("example");
     var fonts = new FontRegistry(runtime);
     var text = new TextService(fonts);
     var layout = new YogaLayoutEngine()) {
    var body = new FontId("host.body");
    fonts.register(body, FontSource.file(hostFontPath));
    fonts.registerDefaults(); // appended behind your own fallbacks

    LayoutTree tree = buildHostTree(text, body);
    var frame = PresentationFrame.create(
        tree, layout.layout(tree, viewport), viewport,
        Map.of(), new PresentationFrame.Offset(0, 0));
}
```

`YogaLayoutEngine` is single-threaded: create, lay out, paint and close it on
the same thread. `LayoutTree` uses stable `NodeId` identities; changing a
node's children or measurement callback updates only the affected native
subtree. `PresentationFrame` is immutable and supplies border, content and hit
rectangles plus clipping for painting and hit testing.

For Minecraft rendering, call `MinecraftRenderBridge.get()` from the render
thread. Flush pending `GuiGraphics`/`DrawContext` work before `drawFrame`, and
run vanilla content in `vanillaPass` between Skija passes. In CPU previews use
`RenderSession` and `Surface.makeRasterN32Premul` directly.

The two integration examples in `examples/a` and `examples/b` show the two
supported paths. Example A uses the optional design package (`UiTokens`,
`SkiaDraw`, `UiCards`). Example B builds a Yoga layout and paints it with a raw
Skija `Canvas`. The optional local controls and settings shell are documented
separately in [controls.md](controls.md).

The common Java bytecode targets 17. Forge runs on 17; Fabric and NeoForge run
on 21. Hosts must keep Minecraft's existing LWJGL version and must not compile
or bundle a second Skija runtime into their own artifact.
