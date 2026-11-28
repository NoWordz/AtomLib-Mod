<p align="left"><strong>English</strong> · <a href="README.zh-CN.md">简体中文</a></p>

<p align="center"><img src="assets/branding/logo-original.png" width="120" alt="AtomLib logo"></p>

<h1 align="center">AtomLib</h1>

<p align="center"><em>Skija drawing and Yoga layout for Minecraft client mods.</em></p>

<p align="center">
  <a href="https://github.com/NoWordz/AtomLib-Mod/releases/latest"><strong>Download</strong></a> ·
  <a href="docs/wiki/getting-started.md">Getting started</a> ·
  <a href="docs/wiki/English.md">Documentation</a> ·
  <a href="RELEASE_NOTES.md">Release notes</a>
</p>

## Install

This source tree targets the **0.8.0 candidate**, which has not been published. The latest standalone AtomLib release is 0.7.0; AtomLib 0.7.1 is available as the companion JARs on the [AtomChat 0.9.5 release](https://github.com/NoWordz/AtomChat-Mod/releases/tag/v0.9.5).

Choose the Forge, Fabric or NeoForge JAR that matches your Minecraft version and put it in `mods/` alongside the mod that requires AtomLib.

| Client OS | JVM architectures | Native libraries |
| --- | --- | --- |
| Windows | x64, ARM64 | Bundled in the JAR |
| macOS, glibc Linux | x64, ARM64 | Downloaded in the background and cached for offline use |

On the first download, macOS and Linux try the Aliyun Maven mirror, then fall back to Maven Central. If preparation fails, open **AtomLib native libraries** from the Minecraft title screen to retry or import the matching offline ZIP from the release. Dedicated servers skip native preparation.

## Features

- **Layout and rendering** with Yoga layout, Skija drawing and Minecraft adapters for backdrop blur and vanilla item icons.
- **Window-local sessions** with independent themes, motion and feedback.
- **Controls and collections** for dialogs, text and numeric editing, and virtual grids and tables.
- **Collection skins** can supply their own item surfaces, table headers and scrollbars while retaining the default appearance for existing hosts.
- **Frontend contracts** that check approved UI design, interactions and feedback using Gradle.

## Develop with AtomLib

Your mod owns its screens, state and lifecycle. Use the local Maven archive from the release and wait for native preparation before creating a Skija or Yoga UI. [Client native integration](docs/wiki/getting-started.md#client-natives)

[Integration examples](examples/) · [Build from source](docs/releasing.md) · [Project status](docs/STATUS.md)

## Acknowledgements

| Contributor or project | Contribution |
| --- | --- |
| [ar_zhe / Module Shop](https://github.com/Ar-Zhe/Module-Shop) | Grid, table and input design references |
| [Muonium Player](https://github.com/zihe114514/MuoniumPlayer-multiversion) | Rendering and GL state capture references |
| [AtomChat](https://github.com/NoWordz/AtomChat-Mod) | UI primitives, themes and interaction design |
| [Skija](https://github.com/HumbleUI/Skija), [Yoga](https://www.yogalayout.dev/), [LWJGL](https://www.lwjgl.org/) | Drawing, layout and native bindings |

## License

AtomLib code is [Apache-2.0](LICENSE). AtomChat adaptations retain the MIT license. Third-party notices are listed in [NOTICE](NOTICE) and [licenses](licenses/).
