# 前端契约锁：真实消费者实施记录

当前为 0.6.0 本地开发候选，未发布、部署或通过完整验收。唯一实施代理顺序推进；未另派审查代理。阶段一已闭合；阶段二仅“小型完整受控场景及独立分发管线”闭合，不能用它替代真实试点。

## 已落地的真实两页增量

AtomChat 的原有设置历史页为已获用户认可的标杆，服务端配置 administration 根页托管标签为复用页。对应版本、安装包和源码凭据见 `2026-11-17-frontend-contract-lock-reference.md`。无需用户再次认可版本。

在 `D:/AtomChat-Main` 新增 SettingsYogaLayout，并在两套映射的 SettingsSectionPage 及共享 ServerConfigPage 中接入选定路线。使用真实 Yoga 结果作为绘制和命中坐标，保留原行折叠、行间隙和小数精度；其他遗留路线未迁移。Lib 新增可选 FRACTIONAL 精度，默认设备像素取整行为保留。

这只是实际页面的纵向布局接入；尚未完成整页配方、受控素材、交互及音效契约登记。不能将整页旧绘制器包成不透明可信函数而声称迁移完成。

AtomChat `versions/atomlib.json` 现使用未发布 0.6.0 候选输入，删除原 0.4.0 公开归档地址，未编造新版本公开地址。本地构建显式提供 Maven 仓库；远程 CI 的候选输入接线尚待实现。未修改该仓原有 gradle.properties 等其他专项改动。迁移前六文件及运行目录独立快照保存在 Lib 的 build/frontend-contract-lock 中。

## 已有效验证（沿用，不冒充本轮重跑）

证据根为 `build/frontend-contract-lock/`。

| 范围 | 结果与证据 |
| --- | --- |
| 阶段二最新完整回归 | UI189 / checker58，零失败、错误、跳过；stage-two-latest-affected-regression.log、business-port-checker-green.log。core42/runtime74 沿用此前集中回归。 |
| 隔离分发消费者 | Java17/21 各19项正反例，consumer-20261117-212500-*；只代表阶段二对应源码。 |
| Yoga 小数精度 | 12项，stage-three-fractional-layout-green.log。 |
| 三端候选 | 三端 build / 本地 Maven / verifyArtifact 通过，stage-three-platform-candidates.log；早于下述输入 API 增量。 |
| AtomChat 实际布局 | 有效行为红灯后2项通过；实际 Maven 接入及3项原有回归合计5项通过，stage-three-real-consumer-wiring.log。 |
| 原有页面与迁移页像素 | production-replay-20261117-221529-xj2kz03a/results.json：旧14帧锚点复现，历史14帧、托管14帧分别与独立冻结旧实现逐像素相同。仅中文、420宽、深色基本矩阵。 |

首次新启动器重放的14帧不一致不是图标改动：实际变化位于 header 时钟，原锚点为17:05，新运行取了22:05。探针专用 Java agent 只替换 ShellHeader 中唯一 LocalTime.now 调用，将业务时钟固定为17:05；要求实际转换标记，未遮图、放宽像素阈值、覆盖原锚点或修改生产时钟。随后独立旧重放复现原14帧才继续比较新页面。失败证据 production-replay-20261117-220547-ef32v31e 保留。

## 最新输入及声音增量

StrictPage 增加类型化文本、数字、布尔编辑数据。只能在真实宿主输入事务中提交已声明编辑能力；业务回调拒绝时不提交，禁用或事件外调用拒绝。数据提交本身明确无声，获准组件可在真实输入阶段请求契约中的焦点反馈，不用伪造业务动作。core 的直接声音请求也必须遵守角色级 SOUND 条件，不能仅检查声音值自身条件。

上述增量已分别取得行为红灯及选定回归绿灯：stage-three-editable-content-red.log、stage-three-sound-role-red.log、stage-three-focus-feedback-red.log；stage-three-edit-feedback-green.log 为 core 契约17项、UI选定9项，零失败、错误、跳过。该结果不是完整 UI/core 回归，也尚未进入当前 Maven 制品。它提供编辑绑定能力，并不代表实际 AtomChat 编辑器已经迁移。

## 2026-11-18 继续时状态

已核对无遗留 Java/Python/Gradle 后台检查。正在对最新输入和声音增量执行完整 core/UI/checker 回归，日志 stage-three-current-regression.log，完成后刷新候选并验证消费者。runtime74 在无相关变更时复用。

剩余：完整真实两页受控模板/配方及入口、真实资源与实现依赖闭包、完整条件/降级/动态/输入/声音场景、实际消费者 CI 与候选输入、最终正反例与制品。独审及实际游戏/声卡听感未做，只能交候选。无必须用户回答的产品决策；禁止提交、push、公开发布、部署、游戏启停仍有效。

## 11-18 实际续作增量

1. 最新编辑/反馈源码先完整执行 core/UI/checker 回归（stage-three-current-regression.log）并更新四模块 Maven。随后加入以下真实边界后，再完整回归 core44、UI193、checker61，共298项，失败/错误/跳过均0，完整 XML 立即保存到 test-results-stage-three-host-mechanism.zip；runtime74 的相关实现和验证配置未变，沿用此前证据。摘要 stage-three-host-mechanism-regression.json。
2. StrictPageHost 固定宿主的页面句柄、实现目录与声音输出；即使字符串身份相同，也不能用另一个注册表替换整页。新增 ContractPage 类型使新页面默认进入严格扫描，无需修改批准配置中的逐页名单；合法纯业务构建通过，直接绘制被 PAGE_DRAWING 拒绝，清单也显示自动识别的严格入口。此接口尚未替代真实两页的完整渲染模板。
3. 有效红灯证实运行时此前会接受不同机制的已批准字节。现在协议2编译自动绑定构建生成的库机制身份，加载不匹配时报 INCOMPATIBLE_PROFILE，保留旧库或审查迁移设计，不自动改用新解释。身份输入为库生产实现/素材/版本依赖与构建配置，规范化文本换行、相对路径；同一份值进入 standalone 和三端。模型探针的无构建绑定包仍只用于隔离验证，不能借此宣称正式协议2接入。当前采取保守拒绝；尚未承诺跨机制兼容别名。
4. 匿名实现的样式依赖补入完整源闭包：直接调用匿名方法的旧用例原本就能识别，不能算红灯；改为先保存 IntSupplier、再经接口调用才真实复现缺口。stage-three-anonymous-deferred-red.log 为有效红灯；完整回归验证缺绑定及改 Palette 常量都被拒绝。
5. 真实素材闭包：检查器支持明确的 classpath: 资源身份，保留资源路径/逃逸/歧义和字节检查。SettingsResourceProbe 对原安装包与候选真实资源核对 sounds.json、3个反馈Ogg、regular/bold字体共6份依赖，正例通过，字体字节漂移按 RESOURCE_CHANGED 拒绝；stage-three-production-resource-closure.log。不是音频设备或完整生产事件绑定验收。
6. 真实页面完整主题/宽度/语言矩阵：production-replay-20261118-113617-_4oo47sl/results.json，2页×2语言×2宽度×2主题，共16组224帧逐像素等同独立旧实现，另先复现14帧原始锚点。仍仅进入/切标签/反向切回，未覆盖全部输入与声音。
7. 三端新增机制资源首次构建因 platform sourcesJar 缺显式生成任务依赖失败（stage-three-host-mechanism-platforms.log）。已在 module-layout 补依赖并重建，stage-three-host-mechanism-platforms-2.log 三端 build/local publish/verifyArtifact 均通过。此为工程接线失败，非负例成功。最新四模块本地发布和核心加载检查 stage-three-host-mechanism-maven.log 通过。
8. 新候选的 Java17/21 隔离消费者各19正反例通过，consumer-20261118-115612-jdk17-zkflq1y3 与 consumer-20261118-115622-jdk21-zmq7str2；对应 stage-three-host-mechanism-consumer-jdk17/21.log。普通构建仍阻断候选、越权绘制、资源/草稿/视觉/陈旧空采样/删插件绕路。
9. 已制作只含当前0.6.0七模块的配对 Maven 输入，三端与core的机制值一致，含最新 StrictPageHost API。身份 stage-three-paired-input-identity.json；archive 为 atomlib-frontend-contract-stage-three-candidate-maven.zip，SHA256=f65508fa2d02a9291ddd254422f595f2b7e2726811241400c812d1431ba5b59e。AtomChat 的3端JAR输入 pin 同步为这次真实产物，不是发布。
10. AtomChat 本地/CI输入工具增加显式本地候选归档，仍核对归档及JAR摘要；先暂存验证，错误或已有目标目录时保留现物，5项正反例通过。workflow 只准备手工URL/SHA或既有配置变量入口，未写远程变量/上传/触发CI；未发布版本无输入时应明确失败，不能猜公开地址。修改 fetch_atomlib_input.py、新测试及 build.yml，未碰其他专项 WIP。

真实消费者新输入第一次 Gradle 显示成功且测试 UP-TO-DATE，但只读检查实际 remapped JAR 发现仍为旧缓存（缺 StrictPageHost 和机制资源）。这次不能算新候选验证。已将唯一已解析0.6.0缓存目录移到 Lib 证据目录保存，校验移动源位于该工程 .gradle/loom-cache 内，未删除源文件/旧0.4参照。重建后 verifyAtomLibInput 与实际5项测试通过（stage-three-paired-real-consumer-refreshed.log），并核对真正 remapped JAR 包含新API与共同机制值，证据 stage-three-actual-remapped-identity.json。最新候选基本生产重放 production-replay-20261118-120108-af6gyj5g 通过原14帧复现、历史14与托管14逐像素相同；已有224帧矩阵保留其当时候选范围，不冒称新JAR完整矩阵实测。完整归档另用真实 fetch_atomlib_input.py 导入成功，stage-three-real-archive-consumer.log。

后续尚未进入上述Maven的源码增量：HostedControl 为已绑定的宿主组件提供原有输入/绘制适配，不把页面作者的任意 Canvas 回调开放为严格API。原生 StrictPageSession 实测按下即提交、释放不重复、禁用取消捕获，stage-three-hosted-control-red/green.log。组件更新新增向后兼容 FrameBindings，传递已声明参数默认值、实际命名状态与每帧不可变副本，避免适配器依靠可变全局外观；有效红灯后 stage-three-host-adapter-snapshot-green.log 相关通过。它们尚未代表两页已迁移；当前准备原有 ToggleSwitch 的显式帧时钟接缝，保留按下语义、原算法和90ms脉冲，不能以库内释放提交控件替换。完成后须刷新受影响回归与候选。

阶段三仍未闭合：两页完整受控配方/组件模板与生产输入声音接线、完整条件场景及真实消费者前端CI门尚待实际完成，不能把以上准备称为整个真实试点。无用户输入阻断，原任务继续。

原有 ToggleSwitch 另补显式 FrameTime 更新和 pulseAt 接缝，旧调用保留原时钟和路径。新测试最初因未隔离 AtomChatConfig 而失败（red.log），属于环境前提失败，不算行为红灯；隔离偏好后 red-2.log 才按 knob 参数和脉冲到期的实际差异失败。新接缝不改变 k/d、90ms 脉冲及“关闭装饰动画仍保留直接操纵 knob”的旧规则。扩大到既有整类时，两个旧用例也缺独立配置前提失败；新测试中以隔离偏好直接复用这两个原测试方法，不改它们的断言、不全局注入配置，相关结果按 green-2.log 实际取得。该接缝还未连入真实页，不能称已完成控件迁移。

## 11-18 续作与宿主隔离

- 用户确认保留旧全局 API 的二进制行为。AtomChat 的布局、绘制和波纹改用自有主题快照及回调。旧宿主之间仍共用旧全局状态。目标实机为 NeoForge「心自安然」，替换前须再次确认。
- 本机 Module-Shop 2.3.0 的 `AtomUi` 字节码和公开源码都调用 `Design.host(ShopDesign.INSTANCE)`。用真实 ShopDesign 在旧 0.6.0 候选上复现 `#4A90E2 → #2B4BFF`，AtomChat 实际色板路径回归复现 17.5 → 0 的圆角串扰。迁移红灯转绿，并补了旧全局回调存在时真实本地波纹仍可见的回归。
- 历史、托管两页共 224 帧，与独立旧实现逐像素一致。先复现 14 帧原锚点，见 `build/frontend-contract-lock/production-replay-20261118-193324-yrpy5tpj/results.json`。首轮帧差来自重放外壳仍用无参圆角 token，已修改为读取同一 AtomChat 快照。该结果覆盖宿主隔离迁移，不覆盖后续严格容器和编辑接入。
- 严格页面新增绑定布局的嵌套容器、固定区与独立滚动，以及折叠时移除命中并取消已按下动作的处理。源码检查允许结构化嵌套，仍拒绝分组回调中的绘制。托管原数字字段已验证可承载原始输入，空值保留给草稿校验。
- 两页完整接入、原有折叠中间帧及标签切换、真实输入音效、生产设计批准与普通 CI 门仍待闭合。所用 AtomChat 工作树以 `d6f7b9b8` 为基线，和原实验工作区分开。

## 11-18 晚间配对验证

- 用户确认沿用原 Screen 的头栏、页签过渡、确认弹窗和输入分层，各个真实入口及其依赖逐项绑定和验证。三端改用稳定命名的 `ScreenTransitionHost`，保留现有调用顺序。合成检查对源码、解析后的调用及编译期常量进行对照，实测可拒绝更换宿主实例、改变常量及漏绑依赖。
- 生产托管视图的实际草稿通过严格会话提交开关和数字字段，一次开关只产生一条声音意图。历史保留天数复用原数字编辑与绘制，严格控件的测试覆盖回车提交、点外侧提交、Esc 取消及编辑时拦住滚动与 Tab。两页的字段定义已可由发行检查器归类，真实托管定义的分组回调插入手算圆角时命中 `PAGE_DRAWING`。
- 三端各 25 项最新选定测试及 AtomLib 公共 388 项零失败、零跳过。AtomChat 与 AtomLib 三端配对制品、依赖摘要及裸值检查通过。此前 Fabric 全量 2147 项零失败、跳过 12 项，覆盖早一批宿主隔离源码。Java 17／21 隔离消费者的协议 2 正反例均通过，覆盖合成方法绑定前的检查器版本。
- 独立旧参考复现 14 帧，再与本轮历史、托管两页的 224 帧逐像素比较。见 `build/frontend-contract-lock/production-replay-20261118-202524-m3q0dkbr/results.json`。重放使用真实页面组件及合成夹具，最终 Screen 宿主走三端构建和接线检查。托管开关到 Fabric SDK 播放边界的正例、漏声音及音高音量交换负例通过，见 `build/frontend-contract-lock/hosting-output-lm6s275d/results.json`。
- 使用「心自安然」现有 Module-Shop 2.3.0 JAR 的同字节副本，与本轮 AtomChat／AtomLib 在同一 JVM 中验证双向切换。商店切换日夜时卡色从 `#FFFFFFFF` 变为 `#FF1A1E27`，AtomChat 独立保持 `#9914161B`。AtomChat 改主题后自身变为 `#99FFFFFF`，商店仍保持夜间值。探针在 `build/frontend-contract-lock/actual-module-shop-neo`。
- 合成提案识别三端各 11 个实际入口，每端仍有 172 条直接调用需要逐项归类或进入已审定实现闭包。位于 `build/frontend-contract-lock/stage3-composition-inventory`。提案未经批准，当前生产入口仍待挂入完整严格会话和正式 CI。
- 配对 Maven 输入在 `build/frontend-contract-lock/atomlib-v0.6.0-host-isolation-contract-increment-maven.zip`，SHA-256 为 `e1e0a36c4e27b1e38e1c9b6bf1db701789e658435865abe5f48672bad882ba11`。宿主隔离独审的四项均已修正并复跑受影响检查。前端锁整体验收和游戏设备验收仍待完成。用户要求等两页完整接入后再确认更换「心自安然」。本轮未替换该实例或启动游戏。

### 11-18 20:47 宿主隔离候选部署

用户授权在两页完整接入前先部署当前配对候选。NeoForge 21.1.255「心自安然」已安装 AtomLib 0.6.0 和 AtomChat 0.9.1。新包读回哈希与候选一致，Module-Shop 2.3.0 哈希一致，旧包已备份。部署清单在 `D:/AtomLib-Backups/2026-11-18-204710-neoforge-host-isolation/deployment.json`。游戏未启动，实机视觉、输入与听感仍待验收。


### 11-18 分组与交互续作

用户已在「心自安然」确认原版 Module-Shop 不再污染已迁移的 AtomChat。续作结束时只读复核实例中的两份配对包及 Module-Shop，三个哈希与部署回执一致。AtomChat 工作树基线为候选分支的 `bc06e327`，未提交本轮 WIP。主题预设未改。

- 兼容 API 的读取、写入和隐式绘制均显示弃用提示。以 0.4.0 编译两个独立测试 JAR 后，在最新 NeoForge 0.6.0 JAR 上复现双向的强调色及波纹串扰。显式默认主题保持稳定，新源码误用隐式入口在弃用警告视为错误时被拒绝。真实旧 AtomChat 与 Module-Shop 的 JVM 结果也已记录。见[复现和边界](2026-11-18-legacy-host-isolation.md)，最新发行格式包证据在 `build/frontend-contract-lock/two-legacy-jars-zs2godwc`。
- `StrictPageSession` 支持在宿主逻辑区域内绘制、按同一树形刷新业务数据以及绑定原卡片材质。原卡片及 divider 由 `SettingsCardGroup` 复用。严格历史页的开关和清空行归到 `HistoryActionComponent`，与原页共用 `HistoryLabelRow`。两页控制器暴露 `mountHistoryContract`／`mountHostingContract`，由实际业务端口提供数据，托管编辑复用宿主剪贴板。
- 历史页测试验证开关、静音、打开原确认窗、取消与确认。托管页验证开关、宿主剪贴板粘贴、草稿不提前保存、一次 `BUTTON` 意图和实际配置的音量 .37。没有本轮设备播放验收。
- 真实原页面组件的导航与交互矩阵各 224 帧逐像素一致，各自先复现旧锚点 14 帧。交互覆盖折叠反转、历史数字输入和撤销或提交、清空确认显示及取消、托管空值与输入。探针固定原字体、头栏及历史光标墙钟，保留原像素容差。首次窄屏托管误点击改为使用实际绘制命中。历史页重放发现聚合浮点舍入把后续头栏从 `306.5855` 推至 `306.58548`，影响 18 像素。用真实的五行高度和折叠进度复现，保留 Yoga 帧并固定组高的计算顺序后转绿。
- 首次登记可以运行候选检查，普通检查要求独立批准来源。Windows 上三端 430 余个生产源文件的路径超过进程命令行长度，现由 Gradle 传候选或普通检查自己的参数文件。三端 `frontendContractCandidate` 真正进入源码分析并拒绝未分类入口。最新 Fabric 普通源码准入在没有已审定来源时拒绝，未发出运行时清单。两页字段及卡片的提案可编译，输出位于 `build/frontend-contract-lock/atomchat-settings-field-draft`。现有 Gradle 启用方式为显式传 `-PatomchatFrontendContract=<配置路径>`。正常 CI 仍未启用整页。
- `group-material-composition-proposal` 中三端分别列出 15 个实际合成入口与 287 条待归类的直接依赖，包含旧 Screen 的方法与新挂载端口。检查器新增非编译期常量字段的精确声明依赖，数组初始化变异会红，同类无关字段变化可通过。该清单是提案。

| 验证命令或范围 | 实际结果 |
| --- | --- |
| Lib `gradlew test publishToTestRepository --offline -PatomlibTestFont=C:/Windows/Fonts/msyh.ttc` | 391 项，无失败、错误或跳过。见 `group-material-root-final.log`。 |
| 三端 Lib `publishMavenJavaPublicationToTestRepository verifyArtifact` | 三端过。见 `group-material-lib-<target>.log`。 |
| 三端 AtomChat `verifyAtomLibInput assemble test` | `SettingsYogaLayoutTest`、`HistoryRetentionComponentTest`、`HostingNumberComponentTest`、`SharedSettingsFrontendTest` 各端共 14 项，无失败或跳过。三端 `verify_jars.py --profile atomlib-consumer` 和裸值扫描过。见 `group-material-atomchat-<target>.log`。 |
| `verify_settings_trial.py --scenario interaction --matrix all` 与默认导航矩阵 | 分别为 `production-replay-20261118-215329-sb752sda`、`production-replay-20261118-215508-oxazfeik`。这是实际原页面组件走合成夹具的离屏证据。 |
| Java 17／21 独立 Maven 消费者协议 2 | 两组各 19 项正反例通过。发生在首登及参数文件之后，结构组材质增量之前，对应 `consumer-20261118-212726-jdk17-mn1w3wiv` 与 `consumer-20261118-212726-jdk21-0_irmgi_`。 |
| 最新七模块输入与配对包 | `atomlib-v0.6.0-fcl-group-material-candidate-maven.zip`，SHA-256 为 `da010e6da463f2a95db4721c2dd1791623daf8bd79de2ccb6607460c23d8c7d8`。由实际消费工具导入并核对三端摘要。 |

当前两个默认 Screen 路由仍需要把分组标题与折叠、固定外壳、滚动位置与编辑焦点接入完整严格会话。需要用实际默认入口重跑动态及声音，再把两页完整定义、绑定和原参照提交明确批准，启用普通 CI。当前工作未达到这一步，整锁独立审查与游戏设备验收未完成。已安装的隔离候选保留。
## 11-19 实际入口与审定草稿

真实默认路由在获审定的运行时资源存在时挂载严格正文。历史页限定 CHAT/history，托管页限定管理员 ROOT 的第 0 页签。三端保留实际 Screen 的固定壳、页签和反转构图、焦点、滚轮及模态优先级。混合入口拆出有名称的路由方法，适用旧入口仍按遗留快照核对。

最初三端 `screen-review-closed-*` 均为 `CANDIDATE_VALID`，但 Fabric 的 3,672 个组合成员包括兄弟页面。后续方法级闭包收窄为每端 217 个根、768 个组合成员、93 个宿主路由。三端本地候选暂无未分类方法或源码诊断。原遗留登记独立，壁纸资源、绘制和业务实现没有进入选定组合闭包。共用按压清理中的一个壁纸 strip 初始化和行类型枚举中的一个常量仍有确切来源。

可审定范围、候选定义、实际绑定及参考帧索引在 `D:/AtomLib/build/frontend-contract-lock/narrow-screen-review/REVIEW.md`。普通 Windows 验证在 `narrow-ordinary-test-only`。该目录的人造场景、测试专用审定输入、运行时资源和标记为 `TEST-ONLY` 的包仅用于验证 Gradle 的传播与打包。所有者仍需审定实际两页的候选定义、应用绑定和独立场景参考。正式两页场景包、Linux CI 环境解析和实机听感尚无验收。

## 11-19 两页实际场景包

所有者评审入口位于 `D:/AtomLib/build/frontend-contract-lock/narrow-screen-review/scene/REVIEW.md`。历史和托管两页共 140 份原实现软件帧与观测，覆盖中文 420 宽深色、英文 280 宽浅色的页签过渡、分组折叠反向、编辑、取消、开关、静音、滚动和历史清理确认。真实默认严格正文每次重新捕获三端 `current.fcs`。两次独立冻结运行都复现原 14 帧锚点，生成相同的 `independent.fcs`，SHA-256 为 `1e847468e18cc9ba5dea644bb41e06fe9709693cd0e52bc4aeec7e4ba44567aa`。

可选场景复用 Gradle test classpath。回放和时钟代理从工具目录移到 AtomChat 的 `shared/src/frontendScene/java`，普通场景任务从实际 test source set 校验 `scene.input.*` 选定夹具和资源。Forge 复用已有的测试 bootstrap。固定帧钟涵盖头栏、历史光标、滚动、弹簧与开关按压的时钟读数，保留零像素与零观测容差。测试代码和代理未进入三端产品 JAR。没有修改两个页面的生产代码或原 768 个组合成员及 93 个宿主路由的批准范围。

三端运行普通 `build check --offline --no-configuration-cache`，给入配对本地 Maven、该端实际 `proposal` 与明确标记的 `TEST-ONLY-reviewed`。Forge 2,137、Fabric 2,152、NeoForge 2,137 项，各 12 跳过，零失败或错误。三端各自 `APPROVED_VALID` 和 `SCENARIOS_VALID`，前者仅针对测试专用审定来源。AtomLib `:frontend-checker:test` 70 项通过。真实 `SettingsContractMount` 反馈量变异从 0.45 改成 0.225，Fabric 场景门报六条 `SOUND_PARAMETER_DRIFT`。变异只编译到临时 classpath。精确命令、哈希、样本帧和输出保存在场景评审目录。

三端 `frontendContractCandidate` 返回候选有效且需审定，Fabric 普通 `check` 去掉独立审定输入触发预期拒绝。随后三端均恢复未启用契约的常规开发归档，测试专用准入资源和回放工具均未打入归档。原工具的无新增参数基本矩阵命令继续可用，并复现原两页各 14 帧。

生产启用须由所有者确认三端完整 `proposal` 内的配置、作者文件、编译设计和冻结参考，然后指定独立 Git 审定版本供 CI 读取。Windows 本地证据已齐，Linux 依赖和渲染环境、独立审查、原游戏窗口及听感仍待验证。

| 检查 | 本地证据 |
| --- | --- |
| 普通两页导航与真实交互重放 | `production-replay-20261119-114530-7oqjsqus/results.json`、`production-replay-20261119-115137-cxwjmxl4/results.json`。每组 224 帧逐像素相同，另各复现原 14 帧锚点。严格挂载按选定帧断言。后者从编译产物快照读取，避免普通构建重编时替换类文件。 |
| 真实入口变异 | `narrow-ordinary-test-only/1.21.1-fabric` 的日志。历史正文转到通用取消路由命中 `COMPOSITION_DISPATCH_BYPASS`，Screen 改画托管固定栏命中 `HOST_DISPATCH_CHANGED`，壁纸旧路由改动命中 `LEGACY_UI_CHANGED`。扰动只修改独立源码副本。 |
| 声音 | `contract-sound-outputs-qxmigv6_/results.json` 三端实际适配源码对 SDK 替身的正负例通过。此前 `hosting-output-lm6s275d/results.json` 一次真实组件按压到 Fabric SDK 输出替身另行通过。普通回归从严格 sink 与旧控制器两处计数，需要一声的操作仍要求一声。 |
| 三端候选 | `frontendContractCandidate` 均为 `CANDIDATE_VALID`、`DESIGN_REVIEW_REQUIRED`。每端 `candidate-report.txt` 有精确类别与成员表。 |
| 三端普通构建 | 本地七模块 Maven 输入，Forge JDK17、Fabric/NeoForge JDK21，`build check --offline --no-configuration-cache` 配测试专用审定源均成功。三端均经过 `APPROVED_VALID`、`SCENARIOS_VALID`、测试和归档，分别 2,137 / 2,152 / 2,137 项，各 12 项跳过、零失败。无审定源、画面漂移、陈旧捕获返回失败。结束后重建了普通开发制品，核对测试清单与测试场景类均已清除。 |
| Lib 集中回归 | `narrow-final-library-regression-with-font.log`。沿用 CI 显式字体参数运行 `:core:test :runtime:test :ui:test :frontend-checker:test`，分别 44 / 74 / 200 / 69 项，零失败和跳过。 |
| CI 构建入口 | AtomChat `.github/workflows/build.yml` 的 YAML、Bash 与五个本地条件分支通过。已审定 PR 目标输入删除或生产版本缺失时拒绝。当前真实生产场景包和已审定正本未激活，远程 CI 未执行。 |

首次普通构建启用默认严格路由后，旧音效测试只观察旧控制器。Forge 普通 JUnit 还将真实分发包的 SRG 调用用于命名测试类路径。回归改为分别观察旧控制器与严格输出，同时保留适配器 SDK 输出探针。一次交互重放遇到并行 Gradle 替换 class 文件，已用稳定目录快照重跑。另外有三份旧测试会写仓库参考图，已恢复原始 30 份 PNG，后续验证使用单独的测试源码与参考视图。

## 11-19 所有者批准与 Git 只读验证

用户明确批准了两页设计基准及已展示的冻结参考，Windows 比对容差为 0。本地 AtomChat 独立 Git 审定版本为 `536e4e7ede3ffada3b51c14f30f882ff393c32e7`，分支为 `codex/atomchat-settings-reviewed-0.6.0`。该版本只包含三端各自 proposal 的配置、作者文件、编译设计及冻结 `.fcs`，12 个输入摘要与已展示的评审包一致。当前 AtomChat 工作树 HEAD 仍是 `bc06e327`。未推送审定分支、修改远端规则、部署或启停游戏。

本机 Windows 三端 `frontendContractCheck` 从该 Git 版本读取输入，均返回 `APPROVED_VALID` 和 `SCENARIOS_VALID`。日志和版本输入摘要在 `D:/AtomLib/build/frontend-contract-lock/narrow-screen-review/scene/owner-approved-git`。WSL Fabric 首轮取得依赖、完成编译后，源码门因结构快照的系统换行差异失败，正在修复及复核。GitHub API 核对 AtomChat main 当前无分支保护，生产检查的远端设置仍需处理。

## 11-19 Windows 分发检查与 NeoForge 候选部署

用户选 Windows x64 作正式零容差像素门，Linux 继续普通构建与测试且不分发。WSL Fabric 观测和声音一致，140 份画面与已批准 Windows 参考有像素差。跨平台 AST 换行已在 checker 修复。干净 Windows 进一步发现 Fabric 52 个及 Forge 1 个生成映射 JAR 只有包元数据差异；先按原批准 SHA 核对原包，再核对全部非目录条目字节。用户批准这 53 项改用 ZIP 内容身份。本地续版 Git 为 `60ccf4326a484d6e656b89aaae7b871fa7e49ee2`，原 `536e4e7e` 是父提交。作者文件、参考捕获及容差沿用原版。checker 74 项通过并拒绝实际修改条目的反例。最新全部七模块 Maven 归档 SHA-256 为 `8972479efd74676712b7fac63dc4b7cb89d84129015e75538745a6342eeafbc1`，位于 `build/frontend-contract-lock/windows-official-ci/dependency-migration`，实际 AtomChat 入口导入校验通过。

用实际 Windows CI 脚本在三个干净本地检出运行，全部读 Git `60ccf43` 通过 `APPROVED_VALID` 和 `SCENARIOS_VALID`，全量分别 2137、2152、2137 项，各 12 跳过、零失败，归档来源戳为干净且准入资源校验通过。矩阵为 Windows 三个正规分发、Linux 三个仅验证的构建。记录、六个配对 JAR 和 XML 摘要位于 `build/frontend-contract-lock/windows-official-ci/dependency-migration/owner-approved-git`。

用户确认替换「心自安然」的配对 JAR，已换新 AtomChat 0.9.1 与 AtomLib 0.6.0，读回 SHA 一致，Module-Shop 原样。备份及部署回执为 `D:/AtomLib-Backups/2026-11-19-133652-neoforge-frontend-contract/deployment.json`。本轮未代启游戏，待用户验两页和设备反馈音。最新代码和批准版本未推送，远端 Windows CI、Maven 输入发布和主分支保护尚未设置。远端现有 AtomChat 候选仍是 bc06e327。

2026-11-19，用户实测当前配对包，确认历史设置、服务端托管、反馈音和 Module Shop 共存主题隔离均通过，并授权提交、推送、执行 GitHub Actions 及发布 AtomLib 0.6.0。
