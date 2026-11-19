<p align="left"><strong>English</strong> · <a href="README.zh-CN.md">简体中文</a></p>

<p align="center"><img src="platforms/1.21.1-fabric/src/main/resources/atomlib.png" width="160" alt="AtomLib logo"></p>

<h1 align="center">AtomLib</h1>

<p align="center"><em>Skija &amp; Yoga for Minecraft.</em></p>

AtomLib is a client-side UI library for Minecraft mods. It combines [Skija](https://github.com/HumbleUI/Skija) drawing, [Yoga](https://www.yogalayout.dev/) layout and Minecraft rendering adapters. Your mod owns its screens, business data and lifecycle.

**v0.6.0** · [Downloads](https://github.com/NoWordz/AtomLib-Mod/releases) · [Getting started](docs/wiki/getting-started.md) · [API guides](docs/wiki/English.md) · [Release notes](RELEASE_NOTES.md)

## Features

- **Window-local appearance** — independent themes, motion policies, feedback and previews through `AtomDefault.createSession`. New sessions are unaffected by legacy global theme changes.
- **Layout and interaction** — Yoga box trees and a shared presentation frame for drawing, clipping and hit testing.
- **Controls and editing** — buttons, sliders, toggles, choices, dialogs, single-line/multiline text and numeric drafts, with grapheme selection, clipboard and undo/redo.
- **Frontend contracts** — grouped strict pages and a Gradle checker for approved design, implementation and replay evidence. See [Design packages](docs/wiki/design-package.md).
- **Virtual collections** — adaptive card grids and fixed-height tables with stable item keys, sortable header intents and inline actions. The host owns loading, sorting and persistence.
- **Text and images** — host fonts or bundled Noto Sans SC subsets, text measurement/drawing, image resources and optional oversized-image preprocessing.
- **Motion and rendering** — explicit frame clocks, springs, interruptible transitions, smooth scrolling, optional backdrop blur and native Minecraft item-icon adapters.

## Compatibility

| Minecraft | Loader build baseline | Java | Bundled client natives |
| --- | --- | --- | --- |
| 1.20.1 | Forge 47.4.10 | 17 | Windows x64, ARM64 |
| 1.21.1 | Fabric Loader 0.16.9 | 21 | Windows x64, ARM64 |
| 1.21.1 | NeoForge 21.1.235 | 21 | Windows x64, ARM64 |

These are build baselines, not a promise of validation on every loader, renderer or device. Windows ARM64 natives are packaged but have not been validated in-game. Linux and macOS client natives are not bundled. AtomLib is a client dependency; dedicated servers do not need to initialize its rendering runtime.

## Install

Download the JAR matching your Minecraft version and loader from [Releases](https://github.com/NoWordz/AtomLib-Mod/releases), then place it in `mods/` alongside a compatible consuming mod. A dependency version range alone does not establish API compatibility.

For mod development, extract `atomlib-input-0.6.0.zip` as a local Maven repository. Coordinates and loader-specific dependency syntax are in [Getting started](docs/wiki/getting-started.md).

## Start a screen

Create and retain one appearance session per window. Build the layout and paint within the same frame snapshot:

```java
var ui = AtomDefault.createSession(runtime);
ui.selectTheme("dusk");
ui.selectMotion("standard");

// Inside the existing render callback, in GUI units:
try (var frame = ui.beginFrame(time, viewport)) {
    // Build/measure the host's tree using ui.context().
    frame.paint(canvas, presentation);
}

// On screen removal; the borrowed runtime remains open:
ui.close();
```

Forward pointer, key down/up, committed text, wheel, window-focus and removal events through the input adapter. See [Sessions, editing and collections](docs/wiki/sessions.md) and the compiled [examples](examples/). Existing `Design.host` and `Design.rippleSink` calls retain their global semantics; hosts must adopt the new session entry to get its isolation guarantees.

## Boundaries

- The deprecated global design APIs retain their binary compatibility and shared state. Use a session or explicit host arguments for isolation.
- Editors support plain left-to-right text and committed text events. In-game Chinese IME submission, focus and composition behaviour still need platform validation. Full bidi and rich-text editing are outside scope.
- Collections use fixed row/card heights. Editable cells, frozen columns, tree tables and complex multiselection are outside scope.
- Background blur requires an explicit backdrop operation and has an opaque fallback. Item icons and blur have render-thread/state/lifetime contracts.
- CPU raster and hidden OpenGL checks do not replace in-game validation of visuals, GUI scales, shader/mod combinations or long-running GPU/native lifetimes.

## Build

Use JDK 17 for common tests and Forge, and JDK 21 for Fabric/NeoForge. The packaging tool reads `JAVA17_HOME` / `JAVA21_HOME` when set. Dependencies must be cached before using Gradle offline.

```powershell
.\gradlew.bat :core:test :runtime:test :ui:test --no-daemon "-PatomlibTestFont=C:/path/to/font.ttf"
python tools/run_packaging.py all --jobs 1
```

[Architecture and ownership](docs/wiki/architecture.md) · [Controls](docs/wiki/controls.md) · [Build and release guide](docs/releasing.md)

## Acknowledgements

| Contributor / project | Contribution |
| --- | --- |
| [ar_zhe / Module Shop](https://github.com/Ar-Zhe/Module-Shop) | Design reference for adaptive grids, tables, input adaptation and interaction boundaries. |
| [Muonium Player](https://github.com/zihe114514/MuoniumPlayer-multiversion) | Rendering ideas and reference for investigating render-state capture and frame cost. |
| [AtomChat](https://github.com/NoWordz/AtomChat-Mod) | Permitted UI primitives, theme palettes and interaction design. |
| [HumbleUI / Skija](https://github.com/HumbleUI/Skija), [Yoga](https://www.yogalayout.dev/), [LWJGL](https://www.lwjgl.org/) | Drawing, layout and native bindings. |

## License

AtomLib-authored code is [Apache-2.0](LICENSE). Allowlisted AtomChat adaptations retain MIT terms. See [NOTICE](NOTICE), [licenses](licenses/) and [provenance](docs/provenance.md) for third-party attribution.

Copyright © 2026 E33EPUS & NoWordz
