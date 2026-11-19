# Changelog

## v0.6.0

**新增**

- 新增前端契约及 Gradle 检查器，可对照独立审定版本校验组件、实现依赖、资源、交互和声音。
- 严格页面支持嵌套分组、独立滚动与折叠、数值编辑及命名反馈音。
- 新增显式宿主样式和波纹回调入口，可在底层绘制中隔离外观与反馈。

**更改**

- 弃用隐式全局外观与波纹入口，保留现有宿主的二进制兼容和原有共享语义。

**说明**

- 与 AtomChat 0.9.1 候选配对验收历史设置和服务端托管，NeoForge 实机确认反馈音正常且与 Module Shop 共存时主题隔离。
- 分发包包括 Minecraft 1.20.1 Forge、1.21.1 Fabric 和 NeoForge 的 JAR，以及开发者用的本地 Maven ZIP。

----

**Added**

- Added frontend contracts and a Gradle checker for comparing components, implementation dependencies, resources, interactions and sound against an independently reviewed revision.
- Strict pages support nested groups, independent scrolling and folding, numeric editing and named feedback sounds.
- Added explicit host styling and ripple callbacks for isolated appearance and feedback in low-level drawing.

**Changed**

- Deprecated implicit global appearance and ripple entries while preserving binary compatibility and their shared semantics.

**Notes**

- Tested the paired AtomChat 0.9.1 candidate's history settings and server hosting in NeoForge, including feedback sounds and theme isolation alongside Module Shop.
- Distributed Minecraft 1.20.1 Forge and 1.21.1 Fabric and NeoForge JARs, plus a local Maven ZIP for developers.

## v0.5.0

**新增**

- 新增按窗口隔离的外观会话、主题快照、动效策略与独立预览选择器。
- 新增单行、多行与数值输入，支持字素选区、剪贴板、撤销及无效草稿反馈。
- 新增固定高度的虚拟自适应网格与列表表格，提供稳定条目标识、排序意图和行内动作。
- 新增可中断过渡、页面平移缩放、区域形变、平滑滚动及局部水波反馈。

**更改**

- 设置壳与控件在新会话中共用同帧主题，保留旧全局入口的兼容语义。
- 保留十二个主题 ID，改良典雅配色与玻璃主题的次级文字颜色。
- 补充三端输入适配、隔离会话示例、中英文 README 及鸣谢名单。

----

**Added**

- Added window-local appearance sessions, theme snapshots, motion policies and independent preview choices.
- Added single-line, multiline and numeric editors with grapheme selection, clipboard, undo and invalid-draft feedback.
- Added fixed-height virtual grids and tables with stable item keys, sort intents and inline actions.
- Added interruptible transitions, page slide/zoom, region morphing, smooth scrolling and local ripple feedback.

**Changed**

- New-session settings chrome and controls share one frame theme while legacy global APIs retain their compatibility semantics.
- Retained all twelve theme IDs and refined Elegant and glass secondary colours.
- Added platform input adapters, isolated-session examples, English/Chinese READMEs and an acknowledgement table.

## v0.4.0

**性能**

- 为 Skija 专用绘制路径提供精简的 GL 状态快照，每帧原生状态查询由约 854 次降至约 495 次。

**新增**

- GL 状态快照的 JFR 事件增加原生调用计数与瘦身边界标记。

**配置**

- 新增 `-Datomlib.glscope.slim=false`，回退到完整快照边界。
- 新增 `-Datomlib.glscope.verify=true`，校验被省略的状态族。

----

**Performance**

- Added a slim GL state snapshot for the Skija-only drawing path, cutting native state queries from about 854 to about 495 per frame.

**Added**

- The GL state snapshot JFR event now reports native call counts and the slim boundary flag.

**Config**

- Added `-Datomlib.glscope.slim=false` to fall back to the full snapshot boundary.
- Added `-Datomlib.glscope.verify=true` to verify the omitted state families.
