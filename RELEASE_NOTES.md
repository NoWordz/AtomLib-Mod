# Release Notes

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
