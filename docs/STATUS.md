# AtomLib status

**当前桌面发行版为 0.7.0。** [GitHub Release](https://github.com/NoWordz/AtomLib-Mod/releases/tag/v0.7.0) · [接入文档](wiki/sessions.md)

## 0.7.0 交付状态

- Windows x64 和 ARM64 使用内置原生库。macOS 和 glibc Linux 的 x64 与 ARM64 客户端按 JVM 架构在后台下载 Skija 和 Yoga，校验后支持缓存复用和离线 ZIP 导入。
- 发行附件包括 Forge 1.20.1、Fabric 和 NeoForge 1.21.1 三份安装 JAR、四份桌面离线 ZIP、七模块 Maven ZIP 和校验文件。发行来自 `codex/v070-cross-platform-natives`。
- 2026-11-19 的 CI 在 macOS x64、macOS ARM64、Linux ARM64 和 Windows ARM64 验过三份安装 JAR 的原生库，整轮 12 项通过。覆盖原生加载、Skija 回读和 Yoga 布局。 [Actions](https://github.com/NoWordz/AtomLib-Mod/actions/runs/44515980874)
- WSL Prism 的 Linux x64 Fabric 实例已验证空缓存后台下载、两份原生库的 SHA-256，以及 AtomChat 测试包进存档后的界面和交互。偶发闪烁暂未复现定位。Windows x64 完成原生探针和隐藏窗口 OpenGL 验证，完整游戏验收未记录。
- Android 的两个 ABI 离线候选已归档。OSS 开通后，验收范围为启动器 JVM 下的操作系统及 ABI 识别、正确 ZIP 的自动下载和 SHA-256 校验。
- AtomChat 0.9.1 三端配套已完成本地编译和候选输入校验，Fabric 测试包已在 WSL 运行。正式配对制品待重新审定前端契约。 [实施方案](superpowers/plans/2026-11-19-v070-cross-platform-natives.md)

## 0.6.0 交付状态

0.6.0 于 2026-11-19 发布。同日用户在 NeoForge「心自安然」确认历史设置、服务端托管、反馈音和 Module Shop 共存时的主题隔离均正常。三端 Windows 本地干净检出各通过 140 个零容差场景和全量测试。 [GitHub Release](https://github.com/NoWordz/AtomLib-Mod/releases/tag/v0.6.0) · [验收记录](verification/2026-11-18-frontend-contract-lock-stage-three.md#11-19-windows-分发检查与-neoforge-候选部署)

## 0.6.0 续作

1. 保留旧版宿主的二进制兼容。为低层绘制提供显式主题和反馈入口，AtomChat 的绘制、布局和输入统一读取自己的外观状态。两个仍使用全局兼容 API 的旧宿主可能互相覆盖。
2. 从阶段三继续，复用既有标杆和配对重放工具。完成设置历史页和服务端托管页的容器、编辑、输入、声音和正式生产检查。验证真实入口完整覆盖和预期负例，再生成三端配对候选。 [前端契约交付检查点](verification/2026-11-18-frontend-contract-lock-delivery-checkpoint.md#5-最小剩余工作)
3. 离屏复现已确认 Module-Shop 2.3.0 安装主题后，修复前的共享强调色从 `#4A90E2` 变成了 `#2B4BFF`。NeoForge「心自安然」用于游戏内验证双向主题切换、两页交互和听感。

2026-11-18，用户在「心自安然」确认 Module-Shop 2.3.0 与配对安装的 AtomLib 0.6.0、AtomChat 0.9.1 共存时不再污染迁移后的 AtomChat。旧宿主保留二进制兼容和共享语义，两个未迁移宿主仍可互相覆盖。部署备份与安装哈希已记录。 [兼容边界与复现](verification/2026-11-18-legacy-host-isolation.md)

本轮续作将旧全局状态的读取、隐式 token 和隐式卡片绘制也标为弃用，保留旧字节码及最后写入生效的行为。两个独立旧 JAR 的颜色及波纹串扰可重现，备份 AtomChat 0.6.0 与真实 Module-Shop 同 JVM 的强调色改写也已重现。新宿主用隔离会话或显式参数。见[兼容边界和复现命令](verification/2026-11-18-legacy-host-isolation.md)。

2026-11-19，历史和管理员托管两页的默认 Screen 路由已接严格正文，继续使用原头栏、页签、输入顺序、模态和转场。三端候选各 768 个精确组合成员及 93 个宿主路由，遗留兄弟页面分别登记。两组真实页面软件重放各 224 帧相同。后续补齐 140 份原实现冻结样本，Forge、Fabric、NeoForge 的本地普通 `build check` 在测试专用审定输入下通过实际场景门。剩余正式审定版本、目标 Linux CI、独立审查和实机窗口及听感。 [两页场景与准入记录](verification/2026-11-18-frontend-contract-lock-stage-three.md#11-19-两页实际场景包)

性能联审修复仍位于 `codex/atomlib-performance` 的 `5e91ab1`，双向反哺另行安排。

## 2026-11-18 前端契约锁收口，未完成

用户不再等待继续实施，本轮保存现有成果并停止续作。唯一实施代理已关闭，没有本任务后台检查。这里的提交是本地开发检查点，不是完整 0.6.0、正式发布或可直接交用户使用的成品。

- 已有：族群及角色契约、批准／候选区分、源码与资源／实现依赖检查、严格会话、单位与音效绑定、构建检查器及 Maven 分发。阶段二仅小型完整场景闭合。
- 真实 AtomChat：原有设置历史页和托管页只接入所选路由的 Yoga 行布局；托管开关严格组件已通过单独集成验证，但尚未由真实页面路由调用。
- 核心未完成：分组／容器及完整两页模板、所有编辑／交互／音效的正式绑定、完整场景及生产 CI 门。不能承诺现在设计新页面时所有越界与遗漏都会被拦住。
- 未验收：完整独立审查、指定游戏窗口、设备听感。没有推送、发布、部署或启动游戏。

沿用 11-18 12:29 的 core44 / UI196 / checker61 共301项通过记录；后续相关增量另有三端构建、实际组件测试及 Java17／21 各19项隔离消费者正反例。不是本轮重跑，也不是完整实机认证。源码快照核对与当前任务文件一致；原四处渲染桥 WIP 不纳入本轮提交。

最新候选是七模块 Maven 输入，含三端 Lib JAR，不含按本轮修改重建的 AtomChat 游戏包；整个 ZIP 不能放入 mods。候选、源码快照及验证历史归档在本机 D:/AtomLib-Backups/2026-11-18-fcl-closeout/；原 build 路径清理后需从归档按原路径取回。

历史交接见[交付检查点](verification/2026-11-18-frontend-contract-lock-delivery-checkpoint.md)和[第二版方案](superpowers/plans/2026-11-17-v060-frontend-contract-lock.md)。当前续作从[阶段三记录](verification/2026-11-18-frontend-contract-lock-stage-three.md#11-18-晚间配对验证)接手，继续使用已认可的原设置页参照。

## 已发布 0.5.0 的能力

提供按窗口隔离的外观会话、同帧主题快照、独立动效、预览选择与局部反馈；新增单行/多行/数值编辑、三端输入和剪贴板适配，以及固定高度虚拟网格/表格。宿主负责页面路由、数据、排序执行与持久化。

新会话不读取或安装旧的全局 Design 状态；旧 0.4 公共符号及全局兼容路径保留，旧调用不自动获得隔离。README 和中文 README 均有鸣谢表，列入 ar_zhe / Module Shop 与 Muonium Player。

## 0.5.0 历史本地验证

| 检查 | 结果与范围 |
| --- | --- |
| 公共模块 | core 26、runtime 74、ui 180 项通过，无跳过；最后的列命中改动另跑相关 7 项通过。 |
| 三端分发 | Forge 1.20.1、Fabric/NeoForge 1.21.1 构建与制品检查通过。 |
| 隐藏窗口 GL | 每目标 79 项及着色器失败回退 1 项通过；修正后复跑受影响的 UI/布局用例。 |
| 独立消费者 | 六个隔离 Maven 消费者及三个纯 Maven 目标通过；含会话隔离探针和缺 ICU 的 Paragraph 负控。 |
| 旧 API | 对照 141 个公共模块类的 1207 个公开符号，无缺失；有限的 0.4 编译二进制调用在 0.5 运行通过。 |

上述结果不等于全部宿主或真实游戏运行认证。CI 状态请查看对应提交的 [Actions](https://github.com/NoWordz/AtomLib-Mod/actions)。

## 0.5.0 未覆盖范围

真实游戏中文输入法提交与组字、视觉动效、GUI 缩放、加载器/第三方渲染组合、长期 GPU/原生资源生命周期与 Windows ARM64 仍需实机验证。Linux/macOS 客户端原生依赖未打包；完整 bidi、富文本编辑和可选 Windows 候选窗定位未实现。

测试与发行库不要求改写宿主工程或游戏实例。来源与第三方许可见 [provenance](provenance.md) 和 [NOTICE](../NOTICE)。
