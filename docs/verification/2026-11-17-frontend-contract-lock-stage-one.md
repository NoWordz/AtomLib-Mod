# 前端契约锁：阶段一实施记录

> 续作状态：用户对原有设置页的认可已收到并定位到三端安装及源码，阶段一可行性门已闭合，已进入阶段二。下文未满足门槛及待用户选版本为历史记录；当前以[参照定位](2026-11-17-frontend-contract-lock-reference.md)和[阶段二检查点](2026-11-17-frontend-contract-lock-stage-two.md)为准。不是0.6.0完成。

2026-11-17，Asia/Shanghai。依据第二版方案及用户随后明确的实施授权，由本会话唯一实施代理执行；没有新建代理、转交会话或冒充独立审查。

## 当前结论与所有权

阶段一已交付可执行源码分析、规范化数据、批准状态、语义观察、坐标及独立 Maven 消费者探针。**这些是可行性原型，不是正式前端锁或 AtomLib 0.6.0 完成品。** 所有检查报告和 Maven JAR 均标记 `STAGE_ONE_PROBE`。库版本仍为 0.5.0；新检查器使用当前工程版本仅供本地测试仓库，不曾公开发布。

已只读核对 AtomChat 专项状态和当前实施记录：`D:/AtomChat-Main/docs/STATUS.md`、`docs/verification/2026-11-17-shared-settings-frontend-implementation.md`。其设置／服务端共用前端及三端宿主已冻结待该专项独审，未获用户实机认可。FCL 没有修改其生产、构建、实例或文档文件。仅通过位于 AtomLib 的外置 Gradle init task 导出既有生产源码／classpath，输出写在 AtomLib 的 build 目录；没有执行 AtomChat 编译、测试、打包或游戏任务。

四处既有渲染桥 WIP、原方案和审计报告均保留，没有整仓回退、清理或修改渲染桥。审计报告正文没有修改；方案仅更新授权和进度入口。

## 已实现的原型

| 位置 | 实际行为 |
| --- | --- |
| `build-tools/frontend-checker/` | 独立 `atomlib-frontend-checker` Maven 制品，JDK 编译 API 符号分析、命令行入口、薄 Gradle 插件；不合入 Minecraft runtime。 |
| `BoundaryCheck` | 跟随源内 helper、静态导入、完全限定名、方法引用；发现受管绘制调用和已配置宿主入口；按具体方法／初始化块登记遗留 AST；未知外部及抽象动态调用在严格路径上拒绝。 |
| `DesignPackage` | 不执行作者 Java；仅解析受限静态常量，生成排序后的版本化二进制描述并严格读回；拒绝环境调用、方法、初始化块、截断及非规范编码。 |
| `ApprovalCheck`／`ProjectCheck` | 分开读取候选与外部 reviewed 输入；无依据失败、同版本不同内容失败、不支持的 profile 失败；候选不自动晋级，检查接线改变可由独立预检发现。 |
| `ObservationCheck`／`ConditionCheck` | 比较独立数值参照；通过有限命名状态和 AND／OR／NOT 求值推导场景见证，不执行回调；缺场景、非有限数、语义偏差、条件缺少正反见证不算通过；不是生产动态场景执行器。 |
| `FrontendContractPlugin` | 从 Maven buildscript 应用；标准 check、build 与 jar／已识别分发发布任务传播普通检查失败；单独 candidate 任务可产生待批准报告，但不能使普通 build 放行。 |
| `tools/frontend-contract-lock/verify_consumer.py` | 仅复制检查器 Maven 输出、wrapper 和合成测试输入创建隔离消费者；验证普通 build、候选阻断、独立预检和恢复。 |
| `CoordinateProbeTest` | 现有 Yoga + PresentationFrame + Skija 软件光栅实际像素与命中检查，含滚动、页面／节点偏移及 GUI／内容／设备缩放。 |
| `.github/workflows/build.yml` | 将原型单测纳入公共源码作业，Windows 增加本地制品的隔离消费者探针；远程 CI 本轮未执行。 |

插件和扫描器目前是受限原型，不应据此把 AtomChat 或任意其他项目标为“已启用前端锁”。正式角色／族群模型、受控句柄、严格页面构建、运行清单及动态渲染任务尚未实现。

## 探针与实际证据

证据根为 `D:/AtomLib/build/frontend-contract-lock/`；日志文件时间为本轮实际执行时间，测试 XML 见各模块 `build/standalone/_<module>/test-results/test/`。

| 探针／命令 | 本轮结果与范围 |
| --- | --- |
| `gradlew.bat :frontend-checker:test --offline --no-daemon --console=plain` | 先保存 boundary、design、project、preflight、observation、factory、condition 的预期红灯日志，再补实现；最终 26 项，0 失败／错误／跳过。`checker-final4.log`。 |
| `gradlew.bat :core:test :runtime:test :ui:test :frontend-checker:test --offline --no-daemon --console=plain -PatomlibTestFont=C:/Windows/Fonts/msyh.ttc` | 15:35 完成；core 26、runtime 74、ui 182，全部 0 失败／错误／跳过。之后只有 checker 原型和其用例改变，checker 重新完整执行；没有把较早公共结果称为最后一轮全模块重跑。汇总 `test-summary.json`。 |
| `gradlew.bat :ui:test --tests '*CoordinateProbeTest' --offline --no-daemon --console=plain` | 15:29 完成；2 项通过，第一项内部覆盖 27 组缩放组合。先独立按数值推导绘制位置，再读取实际栅格边界，并核对指针命中；重复应用内容缩放的坏绘制会产生不同像素，即使命中数据未变。后续公共全量亦含这 2 项。 |
| `gradlew.bat :frontend-checker:publishMavenJavaPublicationToTestRepository --offline --no-daemon --console=plain` | 仅写本地 `build/test-maven`；JAR、POM、Gradle metadata、sources、javadoc 可用，POM 无 Minecraft／运行时传递依赖。 |
| `python tools/frontend-contract-lock/verify_consumer.py --java-home "C:/Program Files/Java/jdk-17.0.18"` | Java 17 隔离消费者通过；正常 build 绿、合法业务数值可用、越权绘制按 PAGE_DRAWING 红、候选普通 build 红、候选预览单独完成、删插件后外置预检红、恢复绿。最终对应原型变更前后的运行目录分别保留，不冒充远程仓库发布验证。 |
| `python tools/frontend-contract-lock/verify_consumer.py --java-home "C:/Program Files/Java/jdk-21.0.11" --language-level 21` | Java 21 隔离消费者使用真实 Java 21 switch 模式语法，全部同类正反例通过。明确向独立预检传递 release=21，不把语法解析失败当违规成功。 |
| `RealHostBoundaryProbe`，JDK 21，418 个实际 Fabric 生产 Java 源文件 | 真实 Gradle classpath 符号分析成功，原型扩展原生 Screen 工厂识别后为 386 个 UI 相关方法；原始观察不是批准输入。将全部源码复制到 Lib build 内的隔离快照，只修改快照：同文件新增路由分支、实际 `drawArrivingHeader` 外壳新增绘制，均因 LEGACY_UI_CHANGED 失败，且无编译／解析错误；恢复后绿。`real-host-probe-final3.log`／`real-host-probe-final3/results.txt`。 |
| 三端实际源码观察 | 各端从实际 Gradle main source set 获取 418 文件，以对应 JDK 17／21 完成符号解析。加入 Canvas、GuiGraphics、DrawContext 及原生 Screen 工厂后，Forge 392、Fabric 389、NeoForge 393 个 UI 相关方法。对应 `atomchat-*-observed-final3.log`／各目录 `inventory-final3/`；全部为观察，不是覆盖认证或自动批准。 |
| 真实 `MotionSpec` 解释变异 | 在测试临时目录编译原实现与改变系数的副本，保持 ID 和参数不变；旧版独立数值参照通过，新副本按 SEMANTIC_DRIFT 失败。不是改参数后比较，也没有修改主库 MotionSpec。 |

真实源码导出命令：

```powershell
# JDK 21；此任务只读取配置并输出源码／classpath 列表
D:/AtomChat-Main/platforms/1.21.1-fabric/gradlew.bat `
  -p D:/AtomChat-Main/platforms/1.21.1-fabric `
  -I D:/AtomLib/tools/frontend-contract-lock/inspect-atomchat.init.gradle `
  fclObserveInputs -PfclObservationOutput=D:/AtomLib/build/frontend-contract-lock/atomchat-observed `
  -PatomlibMavenRepo=D:/AtomChat-Main/build/atomlib-slim-20261107/atomlib-maven-v040-final `
  --offline --no-daemon --console=plain
```

真实宿主负例命令（JDK 21）：

```powershell
java -Xmx2G -cp "build/standalone/_frontend-checker/classes/java/test;build/standalone/_frontend-checker/classes/java/main" `
  com.atom.lib.frontend.checker.RealHostBoundaryProbe `
  build/frontend-contract-lock/atomchat-observed build/frontend-contract-lock/real-host-probe-final3
```

合成消费者中的 reviewed 目录是**测试夹具**，不代表用户批准了任何真实族群。原始宿主观察输出标为 `OBSERVED_ONLY_NOT_APPROVED`，没有自动生成正式遗留豁免或批准索引。

最后对应的消费者记录：Java 17 为 `consumer-20261117-155103-jdk17-7i67knw2/results.json`，Java 21 为 `consumer-20261117-155028/results.json`。末期仅追加独立 ConditionCheck 和对应单测，没有修改这些消费者已验证的 Gradle／ProjectCheck／BoundaryCheck 路径；沿用其检查范围，不把旧消费者运行说成又执行了一遍。

## 候选标杆盘点（只读，尚未批准）

以下来自 AtomChat 当前冻结候选的真实源码，不是重新设计或直接采用 Lib 同名组件：

| 生产实现 | 必须纳入后续真实契约／场景的已有职责 |
| --- | --- |
| `SettingsGroupFrontend` | 同组行间隙与跨组间隙、折叠高度／内容总高、50ms 指数折叠、fold fade/lift、合并卡片裁剪、PressScale、阴影层范围、stagger 的位移／缩放／透明度；同一 motion 采样给绘制与命中；离屏 hover 继续衰减。 |
| `SettingsGroupPaint` | 分组图标 tight bounds 居中、点阵 trail、标题字体／截断、分隔线、随 expansion 旋转的折叠符号；不能只迁移卡片背景。 |
| `SettingsRowText` | 标题／描述字体角色、实际测宽、宽窄布局切换、基线和换行、紧凑数值框及外部单位；当前候选紧凑80／单位36是待最终参照认可的设计数值，不是新 FCL 默认档位。 |
| `SettingsChipBar` | TAB_MS 指示胶囊、hover、PressScale.bounce、选中字体与 onAccent、截断及命中；页签切换还依赖外层 bodyVariant。 |
| `SettingsFormField` | 单行居中、一至四行自适应后内滚、同源行缓存／advance／光标／选区／命中、Unicode、宽度失效与焦点；业务剪贴板／保存语义仍归宿主。 |
| `PageTransitionComposite` 与实际 Screen Host | 完整 body 离场／进入、冻结快照、变体变化、反转／中断、同帧时钟及逆变换；HeaderChrome／固定 chip／footer 独立合成与输入，不被 body 缩放污染。 |

正常、禁用、失焦、折叠中间态、切换反转、开放主题参数与资源降级需要逐项固定场景；字体、纹理／图标集合和 GL 最终合成也不能由上述源码表代替实测。已有专项结果仅作调查依据，没有冒充本轮重新执行或正式批准。

## 产物与文件清单

- 检查器候选 Maven 输入：[atomlib-frontend-checker-stage-one-maven.zip](/D:/AtomLib/build/frontend-contract-lock/atomlib-frontend-checker-stage-one-maven.zip)。只包含检查器的 Maven 目录结构，不含游戏 JAR 或草稿 UI。
- 本轮范围文件：[implementation-files.json](/D:/AtomLib/build/frontend-contract-lock/implementation-files.json)，列出27个工程文件；共享记忆另行回写，不混进工程制品。
- 最终测试 XML：[test-results-final.zip](/D:/AtomLib/build/frontend-contract-lock/test-results-final.zip)；模块执行时间和统计：[test-summary.json](/D:/AtomLib/build/frontend-contract-lock/test-summary.json)。公共282项在15:35执行，最终checker26项在15:54执行，0失败／错误／跳过；不是同一命令最后同时重跑308项。

## 中间失败与未覆盖

- boundary 最初 7 项有 6 项按缺失行为失败；随后一项合法正例因把基类 Screen 本身误当消费者入口失败，已修正。
- Gradle 消费者首次因 extension 类为 final 而无法实例化失败；已修正，不算越权负例成功。
- Java 21 消费者夹具曾在植入绘制时删掉后续依赖的局部变量，以及独立预检漏传 release=21；两次都因未出现预期规则而由 harness 判失败。不能把编译／语言级别错误记成锁成功；后续修正夹具重跑。
- 并行消费者曾因秒级目录同名而提前退出，没有执行负例；已改用带语言级别的原子唯一临时目录，原始目录与失败记录保留。Forge 首次导出命令的 JVM 参数被 PowerShell 拆成任务名，修正引号后成功；NeoForge 首次只读导出任务不兼容其启用的 configuration cache，加 `--no-configuration-cache` 后成功，不改消费者配置。
- 匿名回调数量用例在修改符号格式前已经通过，属于特征确认，不称为复现并修复的覆盖漏洞；最终明确采用 javac binary name 保持不同匿名／内部类的身份。
- legacy 原型保存完整方法 AST，方法内部普通业务变化也可能要求人工分类；没有声称已完成精确 UI 数据流投影。抽象调用、未知外部依赖和额外 production source set 当前保守阻断；最终支持范围不能靠豁免目录补洞。
- observation 原型验证独立参照与正反见证的检查机制，尚未由真实组件场景执行器生成这些见证。没有证明 AtomChat hover／水波等全部兑现。
- 坐标测试使用实际软件栅格，但不是 Minecraft GUI 输入、最终 GL 合成或实机验收。只读核对真实 `ScreenMetrics` 的 GUI→虚拟坐标关系：`gui * guiScale / uiDensity`，其中 density 已包含内容缩放；不能再次应用旧缩放。
- 未执行三端游戏构建、hidden GL、部署、游戏、用户验收或独审；远程 CI YAML 仅完成接线，没有伪称远程通过。没有第二个代理，不能自审冒充独审。

## 阶段门与后续依赖

**尚不能进入阶段二。** 已证明若干关键技术路线可行，但阶段一的以下门槛仍未满足：

1. AtomChat 专项的候选须先完成其独审／所有者确认，明确可冻结的真实标杆及复用页签。FCL 不接管或修改对方冻结文件，也不以“已授权实现机制”代替批准新设计和新基准。
2. 尚未完成三端 Screen／HUD／模态／header／overlay 的正式适配与覆盖分类；虽已解析三端全部 main Java 源文件，观察到的方法集合仍不能证明未知入口和运行时注册均已纳入。没有用可枚举方法数代替完整入口保证。
3. 首个真实设计语义配置、族群清单、生产场景入口及固定字体／后端参照尚未冻结。当前 scalar 二进制格式和数值参照只验证方法，不能直接作为真实页面族群正本。
4. reviewed 输入的 PR 目标分支选择和正常版本晋级仍需在实际消费者 CI 中接线；原型只证明分离的输入和独立预检能够阻断，不承诺抵抗拥有全部构建与审批权限的修改。

这些不是对 D01—D20 的削减。下一步应先解除真实标杆的所有权／认可依赖、补齐完整接入范围及统一场景入口；阶段一门槛成立后，沿原方案进入 core 契约和 ui 严格构建层。不得用本轮原型或普通 JAR 可构建来宣布 0.6.0 完整验收。

## 续作检查点：批准来源、入口分类及音效补充

用户要求同一代理继续后，完成以下独立工作；本段替代上文仍笼统待落实的对应工程项，但不把真实标杆自动批准。

### 已闭合的工程探针

- **Git reviewed 来源可执行。** `GitReviewedSource` 从显式 tag／remote ref／固定提交读取原始版本，先解析为明确 commit，再通过 Git 原始 blob 读取配置、设计包和被保护的构建接线；不 checkout，不从工作树回退。拒绝 HEAD、缺版本、缺文件、symlink 和逃逸路径。临时目录只是生成输入，不是独立信任副本；无 approved=true 自述。测试通过手写隔离 Git fixture 对象，不对用户仓库执行提交、更新引用或写对象。
- **CLI／Gradle 已接 Git 来源。** CLI 使用 `--reviewed-repo/--reviewed-ref/--reviewed-config`；Gradle 提供对应属性，CI 的 `FCL_REVIEWED_REVISION` 优先于候选 Gradle 属性。PR 应由已审定 CI 从事件的 base commit 提供该变量，普通非 PR 构建提供已认可设计 tag／分发版本；缺依据阻断。Git 版本存在不等于所有者已经认可，真正的首次晋级仍须走审定流程。
- **入口分类和调用图已输出。** 三端 SDK 入口覆盖 Screen 生命周期／输入、返回 Screen 的工厂、HUD 事件、Fabric 泛型 HUD 注册回调、Mixin 注入，以及原生声音输出；应用组合适配显式绑定 `PageTransitionComposite.Host` 的 body／header／variant。输出每个观察方法的类别和符号依赖，不按整文件／目录豁免。新增不画图的 HUD 事件也会被发现并要求分类。
- **音效原型检查可执行。** `SoundObservation/SoundCheck` 检查明确可听／无声预期、事件数量与顺序、资源可解析、通道、音量、音高和采样时机；漏声音、重复播放、改通道／参数／时机或缺资源均失败。平台类型未进入 core，尚未把原型冒充正式运行时音效意图 API。

当前不支持的形式有明确边界：严格路径的反射／动态方法句柄、无法解析的抽象／外部 UI 调用、额外 production source set 或未支持语言必须报错；这些不是静默遗漏，也不因有观察方法数就取得项目完整覆盖认证。完整应用契约绑定和动态场景仍在后续阶段。

### 本轮新增执行证据

2026-11-17 16:28 完成 `gradlew.bat :frontend-checker:test :frontend-checker:publishMavenJavaPublicationToTestRepository --offline --no-daemon --console=plain`：**33 项，0失败／错误／跳过**。日志 `sound-and-coverage-tests.log`。包含 Git 来源3项、HUD 分类1项及音效3项；各新行为保存了预期失败日志，再补实现。不重跑未改动的 core／runtime／ui，沿用15:35的公共282项，并明确时间差。

三端当前源码分别以原对应 JDK 17／21重新做符号分类，每端418个main Java文件，无归因编译错误：Forge483、NeoForge484、Fabric479个观察方法。结果分别位于 `atomchat-{forge,neoforge}-observed/classified-continuation/` 和 `atomchat-observed/classified-continuation/`，包含 `classification.tsv`、`dependencies.tsv` 和方法AST。观察到实际 HUD 注册／事件和 `FeedbackSoundController#play` 声音输出；不是自动批准，也不复用旧374／386的方法数来形容本次范围。

只读核对表明 AtomChat 已由其专项于16:04:28部署新候选，仍未主审和用户实机认可。FCL没有参与部署或修改其冻结文件；本次分类观察当前来源，不将它伪称为先前冻结源码版本。

`python tools/frontend-contract-lock/verify_sound_adapters.py --java-home "C:/Program Files/Java/jdk-17.0.18"` 通过，证据 `sound-adapters-wjn9luit/`：

- 未改动的三端真实 `FeedbackSoundController`、共享 policy／cue 的隔离副本，通过 typed SDK test doubles 到输出边界执行；检查实际发送 ID、音量、音高、同步批处理结束才输出、同动作按钮重复去重、SEND优先、空动作与反馈全局／类型关闭／非有限音量无输出、音量上限。
- 对三端实际 `sounds.json` 的3种反馈映射逐一核对；9份真实 Ogg 均经实际 Minecraft `OggAudioStream`／LWJGL STB 解码，每份得到非空PCM；故意损坏资源因该解码器的 `java.io.IOException: Failed to read Ogg file 30` 被拒绝，不把编译／缺依赖当负例成功。
- 三端实际 SDK `forUI/master` 工厂字节码已只读核对：通道为 MASTER，两个 float 按 pitch、volume 进入；当前参照输出 pitch=1，volume由反馈设置有限值钳制。**没有真实声音设备输出，不能称听感或真实游戏 SoundManager 已验收。** 最初尝试执行真实工厂因 MC registry 未 bootstrap 而失败；没有启动游戏或用反射绕过，改为保留真实字节码依据、实际解码器和明确标注的边界 double。
- 当前参照只做同步 action 合并，没有时间节流和异步取消队列；不能从 endAction 推断存在取消待播音效功能。完整 UI 输入→事件绑定、宿主 SoundEvent 注册／资源重载与 MASTER 用户静音的实际混音仍须在生产场景和实机验证，未计为通过。

### 易漏能力覆盖盘点

| 维度 | 当前源码事实／状态 | 接入要求 |
| --- | --- | --- |
| 指针／tooltip延迟 | 指针命中已在现有 Screen／组件中；选定试点的 tooltip及光标方案尚未确认，不能默认不适用。 | 明确参照是否具备，再锁定出现／消失时机与内容；本轮不新增tooltip系统。 |
| 焦点／快捷键／恢复 | SettingsFormField具备聚焦、失焦、方向键、选择、Enter；Screen有navigationComposerFocus恢复路径。 | 分清编辑焦点与导航焦点，继承实际已用路径；不凭这些源码宣称完整键盘可达性。 |
| 编辑／IME／剪贴板／撤销 | FormField具备复制剪切粘贴、选区、undo/redo及代理对；OS IME组合窗口未验。 | 可编辑插槽需继承已有编辑语义，IME宿主链另测；展示角色明确不适用。 |
| 加载／空／错／禁用 | FormField error/enabled和ServerConfig加载／错误／空内容分支可见。 | 按所选页实际分支建立场景，不给所有展示组件机械添加加载态。 |
| 返回恢复 | Screen有导航／草稿／折叠恢复相关实现，ServerConfig持有总草稿。 | 逐页核对滚动、选择、草稿、折叠的保存和生命周期边界；不移动业务持久化职责。 |
| 退场／中断／反转 | PageTransitionComposite及宿主variant负责完整外层。 | 沿用真实生产链，取消捕获和反向切换单独观察。 |
| 缩放／语言／字体／偏好 | 已有ScreenMetrics、字体角色、语言折行、Animations.enabled、反馈声音开关与音量。 | 固定已用场景；减少动态不自动等价于全动画关闭，不增加未存在的偏好功能。 |

### 明确的外部问题与阶段门

需要用户确认参照版本：**推荐等用户认可16:04:28部署候选中的“设置历史页”后，以其完整前端（含声音）为标杆，服务端托管页作复用试点；或者由用户指定一版此前已认可的设置页。** 两者会影响首个语义配置、数值／声音与视觉基准，代理不能自行混合两版。

在该选择／认可到达前，可以继续工具和覆盖数据准备，但不能把当前候选画面／声音写成正式基准。由于首次真实标杆和生产场景正本尚未审定，仍未进入阶段二；后续角色／族群模型、严格构建及运行清单不是“已实现但待测”。本次按协调请求在此自然断点交检查点，原实施授权继续有效，复用同一代理即可续作。未独审、未实机、未对工作区提交／push、未发布、未部署、未起停游戏。
