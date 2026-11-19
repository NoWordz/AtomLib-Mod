# Release Notes

## v0.7.0

- macOS 与 Linux 客户端首次启动会在后台准备原生库，缓存可离线复用。
- 下载失败时，从主菜单打开 **AtomLib 原生库** 重试，或导入对应系统和架构的离线 ZIP。

----

- On macOS and Linux, the first client start prepares native libraries in the background and caches them for offline use.
- If the download fails, open **AtomLib native libraries** from the title screen to retry or import the ZIP for your OS and architecture.

## v0.6.0

**新增**

- 新增前端契约检查，让开发者按已审定的设计验证界面、交互和反馈音。
- 严格页面支持嵌套分组、独立滚动与折叠、数值编辑及反馈音。
- 新增显式主题和波纹入口，采用新入口的宿主可与旧模组隔离外观和点击反馈。

**说明**

- 旧全局入口仍可运行，两个继续使用全局入口的宿主仍共享状态。
- 在 NeoForge 中配对验收了 AtomChat 0.9.1 候选的历史设置、服务端托管、反馈音及与 Module Shop 共存的主题隔离。
- 请选择 Minecraft 1.20.1 Forge 或 1.21.1 Fabric、NeoForge 对应的 JAR，Maven ZIP 供开发者使用。

----

**Added**

- Added frontend contract checks so developers can verify screens, interactions and feedback sounds against a reviewed design.
- Strict pages support nested groups, independent scrolling and folding, numeric editing and sound feedback.
- Explicit theme and ripple APIs let adopting hosts keep their appearance and click feedback separate from legacy mods.

**Notes**

- The legacy global APIs remain compatible, and hosts that both use them still share state.
- Tested the paired AtomChat 0.9.1 candidate on NeoForge, including history settings, server hosting, feedback sounds and theme isolation alongside Module Shop.
- Download the JAR for Minecraft 1.20.1 Forge or 1.21.1 Fabric or NeoForge. The Maven ZIP is for mod developers.

## v0.5.0

**新增**

- 新界面可独立选择和预览主题、动效，多个宿主与窗口之间互不覆盖。
- 提供单行、多行、数值输入，以及可视区域网格、列表表格和行内操作。
- 提供平滑滚动、控件反馈、页面过渡与区域形变工具。

**更改**

- 保留十二个主题 ID，改良典雅与玻璃配色，并补充接入示例、中英文 README 和鸣谢表。

**说明**

- 安装包分别对应 Minecraft 1.20.1 Forge、1.21.1 Fabric 和 1.21.1 NeoForge。
- 宿主需采用新会话入口获得隔离；旧全局主题调用保留原有语义。
- 游戏内中文输入法提交、视觉与长期 GPU 生命周期仍待实机验证。

----

**Added**

- New screens can independently choose and preview themes and motion across hosts and windows.
- Added single-line, multiline and numeric inputs, virtual grids/tables and inline actions.
- Added smooth scrolling, control feedback, page transitions and region morph tools.

**Changed**

- Retained twelve theme IDs, refined Elegant and glass colours, and added integration examples, English/Chinese READMEs and acknowledgements.

**Notes**

- Installation JARs target Minecraft 1.20.1 Forge, 1.21.1 Fabric and 1.21.1 NeoForge respectively.
- Hosts must use the new session entry for isolation; legacy global theme calls retain their original semantics.
- In-game Chinese IME submission, appearance and long-running GPU lifecycle still require game validation.

## v0.4.0

**性能**

- Skija 绘制路径改用精简的 GL 状态快照，降低每帧状态查询开销。

**配置**

- 可用 `-Datomlib.glscope.slim=false` 回退到完整快照边界。
- 可用 `-Datomlib.glscope.verify=true` 校验被省略的状态族。

----

**Performance**

- The Skija drawing path now uses a slim GL state snapshot, reducing per-frame state queries.

**Config**

- Use `-Datomlib.glscope.slim=false` to fall back to the full snapshot boundary.
- Use `-Datomlib.glscope.verify=true` to verify omitted state families.
