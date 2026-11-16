# AtomLib status

**Version 0.5.0** · [Release](https://github.com/NoWordz/AtomLib-Mod/releases/tag/v0.5.0) · [Integration guide](wiki/sessions.md)

## 当前能力

提供按窗口隔离的外观会话、同帧主题快照、独立动效、预览选择与局部反馈；新增单行/多行/数值编辑、三端输入和剪贴板适配，以及固定高度虚拟网格/表格。宿主负责页面路由、数据、排序执行与持久化。

新会话不读取或安装旧的全局 Design 状态；旧 0.4 公共符号及全局兼容路径保留，旧调用不自动获得隔离。README 和中文 README 均有鸣谢表，列入 ar_zhe / Module Shop 与 Muonium Player。

## 已完成的本地验证

| 检查 | 结果与范围 |
| --- | --- |
| 公共模块 | core 26、runtime 74、ui 180 项通过，无跳过；最后的列命中改动另跑相关 7 项通过。 |
| 三端分发 | Forge 1.20.1、Fabric/NeoForge 1.21.1 构建与制品检查通过。 |
| 隐藏窗口 GL | 每目标 79 项及着色器失败回退 1 项通过；修正后复跑受影响的 UI/布局用例。 |
| 独立消费者 | 六个隔离 Maven 消费者及三个纯 Maven 目标通过；含会话隔离探针和缺 ICU 的 Paragraph 负控。 |
| 旧 API | 对照 141 个公共模块类的 1207 个公开符号，无缺失；有限的 0.4 编译二进制调用在 0.5 运行通过。 |

上述结果不等于全部宿主或真实游戏运行认证。CI 状态请查看对应提交的 [Actions](https://github.com/NoWordz/AtomLib-Mod/actions)。

## 未覆盖范围

真实游戏中文输入法提交与组字、视觉动效、GUI 缩放、加载器/第三方渲染组合、长期 GPU/原生资源生命周期与 Windows ARM64 仍需实机验证。Linux/macOS 客户端原生依赖未打包；完整 bidi、富文本编辑和可选 Windows 候选窗定位未实现。

测试与发行库不要求改写宿主工程或游戏实例。来源与第三方许可见 [provenance](provenance.md) 和 [NOTICE](../NOTICE)。
