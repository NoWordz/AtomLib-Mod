<p align="left"><a href="README.md">English</a> · <strong>简体中文</strong></p>

<p align="center"><img src="assets/branding/logo-original.png" width="120" alt="AtomLib 标志"></p>

<h1 align="center">AtomLib</h1>

<p align="center"><em>为 Minecraft 客户端模组提供 Skija 绘制与 Yoga 布局。</em></p>

<p align="center">
  <a href="https://github.com/NoWordz/AtomLib-Mod/releases/latest"><strong>下载</strong></a> ·
  <a href="docs/wiki/getting-started.md">开始接入</a> ·
  <a href="docs/wiki/Home.md">使用文档</a> ·
  <a href="RELEASE_NOTES.md">更新说明</a>
</p>

## 安装

**0.8.0** 独立发行版提供三端安装 JAR、桌面原生库离线包和供模组开发使用的七模块 Maven ZIP。

选择与 Minecraft 版本及 Forge、Fabric 或 NeoForge 加载器匹配的 JAR，与依赖 AtomLib 的模组一起放入 `mods/`。

| 客户端系统 | JVM 架构 | 原生库来源 |
| --- | --- | --- |
| Windows | x64、ARM64 | 内置于 JAR |
| macOS、glibc Linux | x64、ARM64 | 后台下载，校验后缓存供离线使用 |

macOS 和 Linux 首次下载会先尝试阿里云 Maven 镜像，失败后回退到 Maven Central。准备失败时，可从 Minecraft 主菜单打开 **AtomLib 原生库** 重试，或导入发行页上对应的离线 ZIP。专用服务器跳过原生库准备。

## 主要功能

- **布局与绘制**包括 Yoga 布局、Skija 绘制，以及背景模糊和原版物品图标适配。
- **窗口会话**让各窗口独立管理主题、动效和反馈。
- **控件与集合**提供弹窗、文本与数值编辑，以及虚拟网格与表格。
- **集合皮肤**可自定义条目底面、表头和滚动条，现有宿主继续使用原有外观。
- **前端契约**支持使用 Gradle 校验已审定的界面设计、交互和反馈。

## 模组开发

宿主管理自己的界面、状态和生命周期。使用发行页的本地 Maven 包接入，等原生库就绪后再创建使用 Skija 或 Yoga 的界面。 [客户端原生库接入](docs/wiki/getting-started.md#client-natives)

[接入示例](examples/) · [从源码构建](docs/releasing.md) · [项目状态](docs/STATUS.md)

## 鸣谢

| 贡献者或项目 | 贡献 |
| --- | --- |
| [ar_zhe / Module Shop](https://github.com/Ar-Zhe/Module-Shop) | 网格、表格与输入设计参考 |
| [Muonium Player](https://github.com/zihe114514/MuoniumPlayer-multiversion) | 渲染与 GL 状态捕获参考 |
| [AtomChat](https://github.com/NoWordz/AtomChat-Mod) | UI 原语、主题配色与交互设计 |
| [Skija](https://github.com/HumbleUI/Skija)、[Yoga](https://www.yogalayout.dev/)、[LWJGL](https://www.lwjgl.org/) | 绘制、布局与原生绑定 |

## 许可证

AtomLib 自有代码采用 [Apache-2.0](LICENSE)，AtomChat 改编内容保留 MIT 许可。第三方声明见 [NOTICE](NOTICE) 和 [licenses](licenses/)。
