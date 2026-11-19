<p align="left"><a href="README.md">English</a> · <strong>简体中文</strong></p>

<p align="center"><img src="platforms/1.21.1-fabric/src/main/resources/atomlib.png" width="160" alt="AtomLib 标志"></p>

<h1 align="center">AtomLib</h1>

<p align="center"><em>为 Minecraft 带来 Skija 绘制与 Yoga 布局。</em></p>

AtomLib 是面向 Minecraft 模组的客户端 UI 库，组合了 [Skija](https://github.com/HumbleUI/Skija) 绘制、[Yoga](https://www.yogalayout.dev/) 布局与 Minecraft 渲染适配。页面、业务数据和生命周期仍由宿主模组管理。

**v0.7.0** · [下载](https://github.com/NoWordz/AtomLib-Mod/releases/tag/v0.7.0) · [开始接入](docs/wiki/getting-started.md) · [使用文档](docs/wiki/Home.md) · [更新说明](RELEASE_NOTES.md)

## 主要能力

- **按窗口隔离外观**：通过 `AtomDefault.createSession` 独立持有主题、动效、反馈与预览；旧全局主题被修改时，新会话不受影响。
- **布局与交互**：用 Yoga 构建盒模型树，让绘制、裁剪和命中共享同一呈现帧。
- **控件与编辑**：提供按钮、滑条、开关、选项、弹窗、单行/多行文本和数值草稿，支持字素选区、剪贴板及撤销/重做。
- **前端契约**：提供分组式严格页面和 Gradle 检查器，校验已审定的设计、实现和回放。见[设计包](docs/wiki/design-package.md)。
- **虚拟集合**：提供自适应卡片网格、固定行高表格、稳定条目 ID、排序表头意图与行内动作；加载、排序和持久化归宿主。
- **文字与图片**：支持宿主字体或内置 Noto Sans SC 子集、文字量测与绘制、图片资源管理及可选的超大图片预处理。
- **动效与渲染**：提供显式帧时钟、弹簧、可中断过渡、平滑滚动、可选背景模糊和原版物品图标适配。

## 兼容目标

| Minecraft | 加载器构建基线 | Java | 内置客户端原生依赖 |
| --- | --- | --- | --- |
| 1.20.1 | Forge 47.4.10 | 17 | Windows x64、ARM64 |
| 1.21.1 | Fabric Loader 0.16.9 | 21 | Windows x64、ARM64 |
| 1.21.1 | NeoForge 21.1.235 | 21 | Windows x64、ARM64 |

上表列出每个 JAR 内置的原生库。Windows ARM64 尚待游戏内验证。专用服务器跳过原生库准备。

macOS 和使用 glibc 的 Linux 客户端按 JVM 架构选择 x64 或 ARM64，在后台下载对应的 Skija 和 Yoga。冷缓存先尝试阿里云 Maven 镜像，失败后回退到 Maven Central。后续启动会直接核验并复用游戏目录下 `atomlib/natives/` 中的缓存。

准备失败时，从 Minecraft 主菜单打开 **AtomLib 原生库** 重试，或直接导入对应的 `atomlib-natives-0.7.0-<os>-<arch>.zip`。可用 JVM 参数 `-Datomlib.natives.source=mirror`、`central` 或 `offline` 指定来源，默认为 `auto`。Windows 可离线使用内置原生库。

模组可用 `NativePreparation.status()` 查询状态，在调用 Skija 或 Yoga 前确认已就绪。Minecraft 界面可用 `NativePreparationScreen.whenReady(parent, factory)` 延迟构造。Android 仍处于实验阶段，需单独验证启动器及渲染后端。

## 安装

从 [Releases](https://github.com/NoWordz/AtomLib-Mod/releases) 下载匹配 Minecraft 版本与加载器的 JAR，与兼容的宿主模组一起放入 `mods/`。依赖版本范围符合要求，不等于 API 必然兼容。

模组开发者可将发行附件 `atomlib-input-0.7.0.zip` 解压为本地 Maven 仓库。坐标与各加载器的依赖写法见[开始接入](docs/wiki/getting-started.md)。

## 接入一个界面

每个窗口创建并保留一个外观会话，在同一帧快照中构建布局与绘制：

```java
var ui = AtomDefault.createSession(runtime);
ui.selectTheme("dusk");
ui.selectMotion("standard");

// 位于现有渲染回调中，坐标使用 GUI 单位：
try (var frame = ui.beginFrame(time, viewport)) {
    // 使用 ui.context() 构建并量测宿主布局树。
    frame.paint(canvas, presentation);
}

// 界面移除时关闭；借用的 runtime 仍然可用：
ui.close();
```

通过输入适配器转发指针、按键按下/释放、已提交文字、滚轮、窗口焦点和移除事件。完整合同见[会话、编辑与集合](docs/wiki/sessions.md)和可编译的[示例](examples/)。旧 `Design.host`、`Design.rippleSink` 保留全局语义；宿主需采用新会话入口，才能获得该入口的隔离保证。

## 使用边界

- 已弃用的全局外观方法保留二进制兼容和共享状态。宿主可用独立会话或显式宿主参数获得隔离。
- 编辑器支持普通从左到右文字与文字提交事件。游戏内中文输入法提交、焦点和组字行为仍需逐平台验证；完整双向文字与富文本编辑不在范围内。
- 集合使用固定行高或卡片高度；不提供单元格编辑、冻结列、树形表格或复杂多选。
- 背景模糊需要显式调用实际背景处理，不可用时回退为实底；物品图标与模糊都需遵守渲染线程、状态和资源生命周期合同。
- CPU 离屏与隐藏窗口 OpenGL 检查，不能替代游戏内视觉、GUI 缩放、光影/模组组合和长期 GPU/原生资源验证。

## 从源码构建

公共模块测试与 Forge 使用 JDK 17，Fabric/NeoForge 使用 JDK 21。打包工具支持通过 `JAVA17_HOME`、`JAVA21_HOME` 指定对应 JDK；Gradle 离线构建要求依赖已缓存。

```powershell
.\gradlew.bat :core:test :runtime:test :ui:test --no-daemon "-PatomlibTestFont=C:/path/to/font.ttf"
python tools/run_packaging.py all --jobs 1
```

[架构与所有权](docs/wiki/architecture.md) · [控件](docs/wiki/controls.md) · [构建与发版](docs/releasing.md)

## 鸣谢

| 贡献者 / 项目 | 贡献 |
| --- | --- |
| [ar_zhe / Module Shop](https://github.com/Ar-Zhe/Module-Shop) | 自适应网格、表格、输入适配与交互边界的设计参考。 |
| [Muonium Player](https://github.com/zihe114514/MuoniumPlayer-multiversion) | 渲染思路，以及渲染状态捕获与帧成本研究的参考。 |
| [AtomChat](https://github.com/NoWordz/AtomChat-Mod) | 获准采用的 UI 原语、主题配色与交互设计。 |
| [HumbleUI / Skija](https://github.com/HumbleUI/Skija)、[Yoga](https://www.yogalayout.dev/)、[LWJGL](https://www.lwjgl.org/) | 绘制、布局与原生绑定。 |

## 许可证

AtomLib 自有代码使用 [Apache-2.0](LICENSE)，准入清单内的 AtomChat 改编内容保留 MIT 条款。第三方声明与来源见 [NOTICE](NOTICE)、[licenses](licenses/) 和[来源记录](docs/provenance.md)。

Copyright © 2026 E33EPUS & NoWordz
