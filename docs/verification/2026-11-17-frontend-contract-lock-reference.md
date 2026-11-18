# 前端契约锁：原有设置页参照与阶段一结论

2026-11-17。用户明确认可当前游戏**原有设置页**的外观、交互和音效；其完整前端语言就是参照。不是等待服务端配置候选批准，也不重新设计设置页。本记录替代此前“等待用户选时间／版本”的依赖。

## 已定位的安装与源码

只读核对三个实际安装包和最终候选归档，SHA256逐一相同：

| 实例 | AtomChat 0.8.0安装SHA256 |
| --- | --- |
| Forge 1.20.1-main | `63675827e781388c397de55b5102254874363a33ffa50620fcb6b32227e88ce8` |
| Fabric 1.21.1-CCB | `a0d035a08d7d72a122a60074f60739f0cfe03f668e561c8732e7071ceea8f8b5` |
| NeoForge Mechanomania-航空学 | `57ce89258d8df9a392a9bed39490d9b41d7f30049cc559a7f67bef9c87292ca8` |

部署记录原在AtomChat `build/v090-shared-frontend`。调查期间另一专项按用户指令提交并回收该目录，已按其交接定位 `D:/AtomChat-Backups/2026-11-17-closeout/final-candidate-evidence.zip`，从中读取回执及原始JAR，无恢复到对方工作区。源码所在提交为 `d8df2690e0c4ac15f2b5764d9e8b17ff4e190da1`；工作区仍有对方WIP，未改写。

NeoForge选定范围213个class与安装包字节相同；Forge／Fabric的215／213个class经过打包重映射，不能误报字节相同。Fabric重编译所选原有设置组件、外壳、字体、声音与坐标源码，共48个class，反汇编符号指令与当前编译输出一致；结合安装与归档一致的部署链确定来源。没有仅凭版本字符串或文件时间认定源码相同。详细路径、源码快照、资源及字体身份见 `build/frontend-contract-lock/original-settings-reference/installed-reference.json` 与 `replay-results.json`。这些身份记录是调查依据，不是另建SHA门禁。

## 完整前端清单与首个语义配置

首参照为原有 `SettingsSectionPage` 的 CHAT／history，包含原有body、固定chip、HeaderChrome、返回和重置入口及实际模态链。复用试点固定为 `ServerConfigPage` 的administration根页／托管tab0，使用同一已认可设置语言；它只是复用消费者，不作为标杆，不重新接管或修改对方返工。另一专项已提交收口、当前线程idle，仍保留其余WIP；后续真正写消费者时再次核对具体文件所有权。首语义配置命名 `atomchat-settings/1`，绑定下表实现及其现有算法，不以同名令牌或参数等值代替行为等价。

| 维度 | 现有正本与需要保留的行为 |
| --- | --- |
| 布局／尺寸／间距 | SettingsSectionPage的rows、rowHeight／rowRect；SettingsGroupFrontend.layout／advance／contentTop；UiLayout和UiTokens。普通slider／color保留额外value band；历史数字紧凑框及框外单位沿用当前实现；获批连续测量不变成私建档位。 |
| 材质／颜色／层次 | UiCards／UiTokens、PanelBackground、SoftShadow、DotTexture；合并组卡、局部阴影和clip，header及bezel；依赖主题参数派生，不设任意局部颜色入口。 |
| 字体／文本 | FontManager包内regular／bold、SkiaFontRenderer测量绘制；SettingsRowText；原有换行与历史数字words差异，不强制替换成库内相似组件。 |
| 图标 | SettingsGroupIcons／SettingsGroupGlyphs、AppIcons、EditorActionIcons；图形、实际绘制边界、中心与占位均由原实现负责，禁止页面偏移补偿。 |
| hover／高亮 | SettingsGroupFrontend.stepHover、UiMotion.HOVER_MS=90；包括视口外衰减及滚回状态。 |
| press／弹跳 | PressScale、UiSpring、SpringAnim；4u每侧位移预算及6%小控件上限、按压镜像、释放回弹与disabled边界，不能只锁一个scale值。 |
| ripple／嵌套反馈 | ClickRipples及实际surface的onAccent派生；子动作消费与父反馈归属；按原有宿主路径记录次数，禁止重复水波／按压。 |
| 折叠／滚动 | SettingsGroupFrontend的50ms指数时间常数、merged卡片fade／lift／clip／press，stagger命中；ScrollSyncService与原滚动状态。 |
| chip／整页出入场 | SettingsChipBar、UiMotion.TAB_MS=200；PageTransitionComposite冻结离场、bodyVariant、固定header/chip、不被body逆变换污染；UiStagger同帧时钟320ms页面／380ms条目。中间帧、反向与中断属于语义。 |
| 输入／焦点／编辑 | SettingsDetailPage及Screen宿主输入、SettingsFormField已有能力；历史数字实际键盘路径，光标／选区／命中同源。文本编辑框的剪贴板／撤销按实际能力；OS IME未验证，不自称已支持全套IME。 |
| 音效 | FeedbackCue／Policy／三端Controller及sounds.json。有效动作BUTTON、面板PANEL_OPEN、发送SEND；同动作只保留最高优先级，endAction才输出，MASTER通道、pitch=1、配置volume有限值钳制0..1；总开关／分cue关闭及无效动作明确静音。hover和输入聚焦不默认加声音。无异步队列／时间节流／取消API，明确“不适用”，不虚构现有能力。 |
| 状态／返回恢复 | 原开关、数字编辑、清理动作及确认modal、禁用／折叠／切换；返回滚动、选中、草稿、折叠按实际宿主所有权逐项核对，不移走业务持久化。 |
| 偏好／降级 | ScreenMetrics GUI与内容缩放；Animations.enabled；主题、反馈声音开关／音量；模糊可用与不可用两种真实合成。未有专用减少动态API，不等同于全动画关闭；资源缺失不得换未经批准皮肤。 |
| tooltip／指针 | 原有行命中可用；首history场景未发现独立tooltip延时或自定义光标适配，保留待生产宿主场景核对，不能据此全项目宣称“不适用”。不新增tooltip／无障碍／音频引擎。 |

## 固定场景及可重复证据

首阶段一场景：原有CHAT页进入 → history chip → 反向返回CHAT。实际调用SettingsDetailPage.drawSettingsSection／SettingsSectionPage、SettingsChipBar、HeaderChrome及PageTransitionComposite；不手绘近似body或header。复用现有生产组件集成夹具的Host，不声称实例化了Minecraft窗口Screen。

固定输入：zh_cn、panel(20,30,420,820)、460×900软件surface、默认深色主题、动画开启、blur不可用的已存在合成路径、固定渐变外部背景、帧时钟1000ms起每16ms；0/4/12/24/34/35/39/47/59/65/66/70/78/89共14个PNG采样。字体固定为安装AtomLib的 `bundled-default.otf` 与 `bundled-default-bold.otf`；三端字体bytes相同，提取到证据fonts目录，拒绝系统字体代替。

11-18补充已核实的业务输入：原锚点 header 墙钟为17:05，早期启动器未显式固定它，不能将固定动画帧时钟误称已经固定业务时间。后续独立重放使用探针专用 agent，将 ShellHeader 中唯一 LocalTime.now 调用固定到17:05，并要求真实转换标记；生产代码与原图不变，不遮图、不放宽像素阈值。先复现原14帧锚点，再比较候选。详见阶段三记录中的失败与修复证据。

`verify_reference.py`在两个独立JVM重新驱动同一生产入口，14帧逐像素完全相同，且有至少5个不同动态输出。此处冻结的是**现有参照的环境和可重复性**，没有从迁移候选生成期望，没有凭此批准新设计。扩展生产场景要求包括hover离开／press取消／ripple、禁用、清理确认与返回、滚动／折叠、窄宽/中文英文、合法主题／缩放、模糊失败与恢复、声音绑定。它们进入后续完整场景验证，尚未报告SCENARIOS_VALID。

声音沿用阶段一实际controller副本输出边界、九个真实Ogg解码及SDK工厂通道字节码证据。还需生产输入绑定、最终SoundManager输出及设备听感，不能用PNG证明有声音。

## 阶段门判断与所有权

独立构建制品／Java17和21隔离消费者、旧文件新增路由／header变异、批准来源选择、确定性、语义漂移、条件正反与单位像素探针均已有可执行正反证据。三端main源码入口分类和保守未知路径阻断已有实现。当前补齐了已认可参照的定位、实现边界、字体／后端及生产合成入口的稳定性，**阶段一可行性门已通过，可进入阶段二**。

这不是项目已经完整启用：正式族群、严格入口、真实迁移和生产场景矩阵仍须实现。阶段三若需写AtomChat，先协调其同批文件所有权；本轮至今只读对方，所有新工具、源码快照与输出都在AtomLib。游戏包、配置、实例未写，未启停／部署／提交／push／发布。没有独审、没有实机，最终最多交候选。
