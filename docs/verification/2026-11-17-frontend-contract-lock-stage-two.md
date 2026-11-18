# 前端契约锁：阶段二实施检查点

2026-11-17。同一唯一实施代理，按用户要求在当前检查结束后交检查点；原实施授权继续有效。当前没有后台检查。没有新代理或独审、部署、游戏启停、提交、push或发布；AtomLib仍0.5.0。

## 参照定位与阶段门

产品认可已收到：当前游戏原有设置页完整前端语言，包含音效，不是返工的ServerConfigPage候选。三实例安装与归档SHA一致、包内字体同源；Fabric所选源码重编译48个class与现有编译输出符号指令相同。两个独立JVM中的原有设置组件／外壳14帧逐像素相同。[参照定位和能力清单](2026-11-17-frontend-contract-lock-reference.md)列出精确包、源码、固定生产组件入口、字体、后端、数据和时钟及未实机边界。

此前阶段一记录的“待用户选择／认可版本”不再有效。独立制品／Gradle消费者、legacy新增路由与外壳、批准来源、确定性、语义漂移、条件正反及单位的可行性正反例已有证据；现补齐参照定位和稳定生产合成入口，**阶段一可行性门已闭合，已进入阶段二**。这不等于项目完整启用或SCENARIOS_VALID；完整场景、消费者CI、实机属于后续验收。

调查期间另一专项按用户指令提交并清理build证据，已依据其交接找到完整归档，没有恢复／改写对方工作区。来源提交 `d8df2690e0c4ac15f2b5764d9e8b17ff4e190da1`；其余WIP仍归对方。真正改消费者文件前仍核对所有权。

## 本阶段实际完成

- core `ContractDesign`：平台无关族群／派生／引用、作用域、完整角色维度决策、配方、页面允许范围、逻辑单位、受控参数、素材用途、实现与语义绑定、内容插槽和必需场景。
- core `DesignResolver`：缺项、循环、隔离串用、未知引用、展示角色带动作、素材用途、参数范围、单位及场景删减检查；有限条件拒绝未知／空条件及恒真恒假。实际动态见证仍需场景执行器。
- core `ContractCodec`：确定规范化二进制，map/set顺序不影响结果；解码核对格式、重复、大小、深度、尾部及重编码等值；运行时不执行作者类。
- core `ContractRegistry`：批准设计与构建清单内容一致性、缺清单阻断、profile支持检查；不可直接构造的页面／配方句柄，禁止不同注册表按同名ID交换；只读定义和参数快照分离。
- checker `ContractAuthorCompiler`：JDK类型检查后只解释字面量、模型record／enum及受控集合构造；拒绝环境读取、任意helper、初始化副作用，不加载或执行作者类。
- checker协议2：从真实作者输入重新编译，旧design.bin不能批准已改变的作者；区分版本复用与候选。`emitRuntime`函数可输出同源设计／清单，**CLI/Gradle资源任务尚未接通**。Gradle运行classpath加入core位置；新Maven消费者尚未重跑，不称分发闭环完成。

声音及其他易漏维度已记入参照和方案16节；未扩出音频／tooltip／无障碍引擎。

## 本次检查及有效红灯

日志根 `build/frontend-contract-lock/`；Gradle命令均含 `--offline --no-daemon --console=plain`。

| 命令／范围 | 真实结果及日志 |
| --- | --- |
| `:core:test --tests '*DesignResolverTest'` | 初始6项中5项按缺失行为失败；补实现后6绿。`core-contract-{red,green}.log`。 |
| `:core:test --tests '*ContractCodecTest'` | 初始3红，补实现后与模型共9绿。`codec-{red,green}.log`。 |
| `:core:test --tests '*ContractRegistryTest'` → `--tests '*frontend.contract.*'` | 初始2红；最终core契约**11项，0失败／错误／跳过**。`registry-{red,green}.log`。 |
| `:frontend-checker:test --tests '*ContractAuthorCompilerTest'` | 有效JDK17红灯2红1特征绿；实现后3绿。`typed-author-red-jdk17.log`、`typed-author-green.log`。 |
| `:frontend-checker:test --tests '*TypedProjectCheckTest'` → 结合作者测试 | 初始合法协议2未接通而红；实现后作者变化、旧版冒用拒绝、新版candidate按预期。最终**4项，0失败／错误／跳过**。`typed-project-{red,green}.log`。 |
| `git diff --check` | 通过，仅既有LF/CRLF提示。 |

后续命令显式 `JAVA_HOME=C:/Program Files/Java/jdk-17.0.18`。最初typed-author一次因环境Java25与Gradle8.8不兼容提前失败，保留 `typed-author-red.log`，不算有效负例。ReferenceReplay初次Path导入冲突编译失败，修正后才执行两个JVM，不算契约负例。定位脚本遇到对方清理后的旧build路径缺失，改读已有归档，没有恢复对方文件。

阶段一公共282、checker33及Java17／21消费者按原执行时间沿用；本次未为进度重跑。**新模型和协议改变后，旧消费者结果不能代表新代码通过。** 本检查点未跑完整公共／全部checker回归，后续集成后按风险执行。检查点XML和命令时间另存证据目录。

## 真实剩余门槛

1. 阶段二：严格页面内容API、组件实现绑定／会话，既有Yoga／UiSession／PresentationFrame链；构建资源接线、设计差异、草稿隔离、真实条件见证。当前模型不是完整前端锁。
2. 阶段三：明确所有权后的真实标杆＋复用页接入；完整输入／模态／外层、声音事件绑定、降级、动态与视觉场景。Lib相似组件不能冒充原有设置页。
3. 阶段四：新Maven制品及JDK17／21隔离消费者、三端适配／打包、CI批准源与动态门禁、全套指定正反例、适用回归及性能。
4. 独审与实机：本轮只有一代理，不能自审冒充独审；最终游戏合成、IME／窗口焦点、声音设备听感和试点用户验收尚未执行，需后续相应安排与授权。

**当前无需用户回答版本或部署时间问题，没有工程来源矛盾待用户裁决。** 检查点后复用同一代理继续阶段二，主代理不接手实现。

本次未重打包阶段二Maven候选；旧 `atomlib-frontend-checker-stage-one-continuation-maven.zip` 仍为阶段一制品，不冒称包含新代码。当前产物为源码、逐次日志、参照快照／PNG、检查点XML。

## 429后原会话恢复记录

服务端429不是工程检查失败，也不是交付成功。已接回原检查进程，没有重新派代理、重启已通过的测试或改写原WIP。

- 已新增 `StrictPage`：页面只能提交获批配方句柄与类型化业务插槽，拒绝跨注册表句柄、缺必需内容、业务图片冒充材质、重复ID和空页面。
- 已新增 `StrictPageSession`：宿主固定实现绑定，降到既有Yoga／UiSession／PresentationFrame，绘制与输入共用呈现帧；每窗口控制状态、当前业务回调、禁用取消和单帧时钟检查，无页面Canvas／Rect／样式参数入口。
- `strict-page-valid-red.log`保留有效红灯，`strict-session-green2.log`为最新相关5项通过记录。最初测试的Java复合var声明及一次wrap／text接口编译错误不是有效负例，已修正后运行；不把这些错误算锁抓住缺陷。
- 恢复前待接回的是 `:frontend-checker:test --tests '*CheckerRuntimeOutputTest'`，已取得结果：**1项按“普通CLI未输出运行清单”预期红灯失败**，日志 `runtime-output-red.log`。它证明CLI／Gradle资源接线尚缺，不是429造成。当前无该进程在后台运行。
- 下一步补CLI输出与旧清单失效、命名空间／版本资源隔离和Gradle接线；随后跑受影响正例／负例及新隔离消费者。新会话并未发生，原代理继续实施。

## 429恢复后的第二检查点

截至本次三端适配编译结束：原CLI有效红灯已补实现并转绿；当前无后台检查，也无必须用户裁决的阻断。阶段二**仍未闭合**，继续实施。

### 实际新增与验证

- CLI协议2新增 `compile-contract`，普通检查成功后才生成 `namespace/version/design.fcl` 与 `manifest.fcl`；失败／待批准会先使本工具上次生成的清单失效，仅按生成文件索引删除输出，不清理源码目录。命名空间／版本限定为安全资源段。
- Gradle `processResources` 接入普通检查，JAR携带同源设计／运行清单，内部生成索引排除打包；检查器classpath及Maven依赖包含公共core。Windows core Javadoc默认GBK失败已通过显式UTF-8配置修正；`stage-two-publish-local-utf8.log`记录本地Maven发布成功，非公开发布。
- Java17与Java21两个全新隔离Maven消费者均完成：普通build绿、运行资源在实际JAR中与输入相同、非法绘制红、候选普通build红／候选任务可执行、删插件外部预检红、恢复绿。输入仅本地Maven与wrapper，无Lib源码路径依赖。证据目录分别为 `consumer-20261117-174453-jdk17-cgu4bouf`、`consumer-20261117-174453-jdk21-zqht2ybc`；两者 `results.json`记录命令、时间和结果。
- `BoundaryCheck`新增原始几何／动效／作者模型／伪造运行许可的能力识别；协议2本地trusted实现必须绑定完整class AST，包含常量和初始化结构。`ImplementationBoundaryTest`先按缺失捕获能力红，补实现后**2项全绿**；保持同绑定只改RADIUS常量会明确 `IMPLEMENTATION_CHANGED`，没有依赖编译错误。
- 正式core音效数据 `SoundValue`、`UiSoundIntent`、`UiSoundSink`、`ContractSoundSession`：由获批配方绑定事件、资源、通道、音量参数、音高、命名条件、优先级及明确无声；每窗口同步动作只在end输出一次，取消／禁用／静音无输出，旧动作不能重播；真实资源不可用是可观察结果。不是音频引擎，没有队列／混音器／时间节流。`contract-sound-red.log`保留有效红灯，补实现后core契约**14项全绿，无跳过**，日志 `contract-sound-green.log`。
- 新增三端 `client/input/ContractSoundOutput.java`，只适配现有MASTER UI输出。查询正常SoundManager事件及有效weight后调用宿主 `forUI/master`，保持实际资源包解析，未改渲染桥。三端实际SDK的 `compileJava`全部通过，日志 `sound-output-{forge,fabric,neoforge}-compile.log`；没有运行游戏或声音设备。

### 当前证据边界与下一步

本次严格页面相关5项结果沿用 `strict-session-green2.log`；运行资源CLI相关1项绿为 `runtime-output-green.log`；源码实现绑定2项绿、core契约14项绿分别是之后的独立执行，不能合并冒充一次全量回归。

17:44两个Maven消费者对应当时的“资源链”制品；之后新增实现绑定检查和音效模型／适配器，尚未重新打包并验证最新完整制品，不能说17:44消费者覆盖这些后续源码。检查点产物 `atomlib-frontend-checker-stage-two-resources-maven.zip`明确取自17:44已测试的隔离输入，状态 `SOURCE_AND_CONTRACT_ONLY`，不是最新完整0.6候选。XML／统计另存 `stage-two-recovery-checkpoint.json` 和 `test-results-stage-two-recovery.zip`，没有为汇报重复测试。

剩余工程：音效适配器实际输出边界执行及参数负例、严格会话与受控音效的真实事件绑定、单位宿主入口、实现／素材及设计差异完整绑定、草稿隔离、正式场景执行与条件见证、真实标杆和复用页接入、动态／视觉／CI及完整制品回归。真实消费者文件仍须核对对方所有权；当前Lib工作无需等待用户输入。现有普通检查报告仅是源码／契约检查，不表示完整项目已启用；正式启用及0.6交付必须等这些后续门槛成立。

独审、游戏最终合成／IME与焦点、音效设备听感及用户试点验收仍未完成。原四桥WIP及AtomChat他人WIP保持；没有提交、push、公开发布、部署或游戏启停。

## 本轮安全停点：集中回归与最新分发输入已保存

本段为上一个恢复检查点之后的实际增量，没有为报告重跑已有效检查。

1. **真实Control提交已绑定受控音效。** `StrictPageSession.ContentView.activate`在实际输入事务中执行业务commit；成功后自动请求配方声音，统一在事务结束输出。缺少有声配方的宿主适配器直接拒绝，防止忘接；拖出取消、禁用及业务返回false都静音。业务回调从当前ContentView读取，更新内容后不继续调用旧闭包。新增红例后，`strict-sound-binding-green.log`通过；后续集中回归也覆盖此路径。
2. **三端新适配器输出边界及有效变异通过。** `verify_contract_sound_outputs.py`读取真实Lib `ContractSoundOutput`源码的隔离副本和真实core意图类型，用明确typed SDK doubles截获资源、MASTER、音量／音高、次数、不可用及空weight事件。三端正例通过；将实际适配源码的音量和音高参数交换后，三份变异均先编译成功，再因 `SOUND_OUTPUT_PARAMETERS`断言失败被捕获。证据 `contract-sound-outputs-cdbl9ljl/results.json`。实际SDK三端编译证据沿用前段；没有把double冒充真实SoundManager或设备输出，原9 Ogg实际解码证据沿用。
3. **正式单位边界通过真实栅格验证。** 新 `GuiCoordinates`明确GUI点和逻辑点，换算为`gui * guiScale / pixelsPerLogicalUnit`；严格会话提供`pointerGui`，使用最后呈现帧density，不重复应用contentScale。`coordinate-boundary-red.log`先证明错误的原样传坐标会失败；修正后27组合绘制／命中及重复缩放像素负例在集中回归通过。参数和命名状态在同帧冻结为不可变快照。
4. **集中公共回归342项全绿、无跳过。** 本次实际执行 `:core:test :runtime:test :ui:test :frontend-checker:test -PatomlibTestFont=C:/Windows/Fonts/msyh.ttc --offline --no-daemon --console=plain`，core40、runtime74、ui188、checker40，失败／错误／跳过均0，日志 `stage-two-integrated-tests.log`。这是本次运行，不是旧282项或若干筛选测试拼接。执行时钟／命令与XML另存本轮摘要。
5. **草稿隔离已实际接线。** `frontendDraft`独立source set可以编译；正常生产源码／资源不可与其重叠，生产JAR检查实际draft类及保留资源入口。`DraftCheckTest`初始2红，补实现后2绿，日志 `draft-check-{red,green-and-local-package}.log`。此2项在上述342项之后独立执行，不冒称342包含它们。Gradle缓存目前明确将依赖完整源集目录的检查任务标记为不兼容configuration cache，不静默跳过检查。
6. **最新检查器／core的Java17、21干净Maven消费者再次验证通过。** 这次重跑由音效模型、实现绑定和草稿接线的实质变化触发，不为进度重复。输入目录 `consumer-20261117-181637-jdk17-n0np5jci`、`consumer-20261117-181637-jdk21-mu0rapbe`，各自results.json记录命令和时间。合法build、包内运行设计／manifest、独立draft编译通过；draft类／资源混入JAR、draft源纳入main、越权绘制、候选普通build及删除插件接入均按预期规则红；恢复后build绿。没有把缺依赖、编译错误或旧报告当负例成功。
7. **四个独立模块的本地Maven输入已构建。** `publishToTestRepository --offline --no-daemon --console=plain`成功，日志 `stage-two-standalone-maven.log`。产物 `atomlib-frontend-contract-stage-two-standalone-maven.zip`包含core／runtime／ui／frontend-checker的0.5.0本地候选输入，含sources／javadoc；不是公开发布或三个Minecraft成品包。最初Windows javadoc默认GBK失败已通过根构建显式UTF-8修正，不算负例。旧17:44资源链ZIP保留，不能与本次最新输入混淆。

**阶段二仍未闭合。** 已有可运行的小型严格页面／输入／声音闭环，但真实AtomChat完整配方与生产场景尚未接入；实现与素材依赖闭包、规范化设计差异、完整条件见证、降级及正式动态／视觉门还未全部完成。普通检查目前仍为SOURCE_AND_CONTRACT范围；合成消费者的APPROVED_VALID不是整个真实项目已经启用或SCENARIOS_VALID。尚未修改AtomChat生产／Gradle文件，不能称真实消费者迁移完成。

当前没有必须用户决定的阻断，也没有后台检查。下一步由同一代理继续补上述阶段二缺口，再进入固定的原设置history标杆＋server托管复用页的真实接入；另一专项当前idle且已交接，仍需保护其未提交实验文件。独审、三端最终打包／游戏最终合成及设备听感保持待验收。此处按用户协调请求保存自然安全停点，原整轮实施授权继续，不转交主代理或新代理。

补充证据范围：342项集中执行后，checker XML被后续DraftCheckTest筛选输出替换；342计数取自当时实际读取的四模块XML汇总，集中成功日志保留。本轮XML ZIP收录当前可用结果，其中checker仅后续2项，不冒充完整342项XML归档；没有为补档重跑未变化测试。

当前无用户输入依赖或后台检查；下一步仍由同一代理补阶段二剩余工程，再做history标杆＋托管复用页接入。阶段二未闭合，独审和实机听感未执行，原实施授权继续有效。


## 继续授权后的依赖闭包与动态场景增量（2026-11-17 20:20）

本段接续用户中断后“继续”的原授权；同一实施代理，未新开代理或会话。恢复时git状态与上一检查点一致，未发现本轮遗留Java／Python后台检查；四处原渲染桥保持未写。最新检查结束与产物另见本段之后的机器摘要，不能将“正在执行”写成通过。

### 已实际补写

- **本地实现传递闭包**：除可信组件本体AST外，扫描方法、字段／常量及类型引用的helper依赖，递归要求完整class绑定。新用例先证明仅冻结Component／Ink而漏Helper／Palette会假绿，再补实现。组件外的已匹配冻结实现可以参与绘制，但页面直接绕入仍按严格入口调用闭包检查。
- **外部实现依赖**：可信入口来自JAR时，绑定整个已解析依赖目录，避免只锁入口class却漏同JAR或另一JAR中的helper；对JAR和输出目录按实际内容校验，相同文件名的歧义明确拒绝。新用例先证明外部实现不绑定也会通过，再验证合法JAR通过、保持入口改内部radius常量报DEPENDENCY_CHANGED；同步改绑定值也不能复用原设计版本。此处目录指调用者交给检查器的完整依赖输入，宿主运行专用依赖目录与正式构建接线仍需继续完成，不能宣称所有运行动态加载已自动可知。
- **素材内容与实际解析**：资源从生产资源根及实际依赖中解析；设计素材不再只有ID。字体／图标／纹理直接资源与PNG元数据受绑定；Minecraft sounds.json中的file／event依赖递归到真实Ogg文件，font JSON的bitmap／ttf／reference受支持，未知provider或外部SVG引用明确UNSUPPORTED。路径逃逸、资源冲突、引用循环、缺文件和内容改变明确失败；资源字节绑定不冒充宿主解码或设备输出。原真实9 Ogg解码证据继续按原范围沿用。
- **规范化身份与差异**：正式包包含compiler-owned buildBindings，承载资源、依赖、实现AST及策略／受保护构建文件的内容身份；更新这些内容必须更新设计版本。新增compile-project生成包含构建绑定的候选包，compile-contract仅用于字面作者模型探针。设计字段逐层输出DESIGN_CHANGE、CONFIG_CHANGE及受影响页面（保守包含全部已声明页面），不再只报候选。编码格式由未发布的FCL02推进到FCL03，旧原型包明确拒绝，旧0.5公共API不受影响；应重生成该未发布原型的reviewed夹具，不能把新制品与旧候选ZIP混用。
- **JDK分析隔离**：完整checker第一次执行因Windows临时JAR被占用而失败，不算通过。查本机JDK17源码定位到javac的autoStart Plugin：即使-proc:none仍会经消费者classpath加载。新增真实JAR自动插件副作用红例后，显式设置空ANNOTATION_PROCESSOR_PATH；插件副作用与清理问题均转绿。没有使用System.gc、跳过清理或改JUnit清理策略遮盖问题。
- **运行时条件**：配方新增按真实命名状态求值的bindings入口；严格会话在创建／更新和提交时使用同帧状态，缺能力不会只停留在声明层。每帧受控enabled来自实际业务内容，不允许页面回调替代条件定义。
- **场景比较与证据交换**：ScenarioCheck验证独立参照及当前采样的页面／配方／场景、时钟、状态、实际像素、数值观察和声音次数参数。角色条件要求启用样本中的成立／不成立，动态能力要求初始／中间／结束的真实变化，交互角色要求实际提交和取消／禁用不提交。SceneCaptureIO是有界PNG／观察／声音数据交换，SceneProcessCheck拒绝旧invocation和不同设计；没有把这些比较器本身称正式Gradle动态门已接通。

### 当前真实原生小场景

`verify_strict_scenario.py`在两个独立JVM中分别执行原UiSession直接控件路径与StrictPageSession路径，使用真实Yoga、Skija软件surface、ActionButton和AtomChatControlSkin。7个采样覆盖initial／hover middle／settled／commit／cancel／disabled／muted，比较逐像素和动作计数／输出次数、音量音高通道及时刻。此处是**合成的小场景**，不以Lib相似皮肤替代已认可AtomChat原有设置页。

场景先抓住两条路径都缺hover的错误参照（DYNAMIC_SAMPLE_MISSING REFERENCE），修正旧路径按其真实API显式调用hover之后，候选按VISUAL_DRIFT middle及DYNAMIC_SAMPLE_MISSING ACTUAL稳定红。根因是StrictPageSession只转发pointer，未接UiSession独立hover入口；补MOVE到hover后正例通过。另将候选实际MOVE丢弃，正常编译后再次按VISUAL_DRIFT middle拒绝。首个探针Path通配导入冲突是编译错误，不计为有效负例；SoundObservation构造参数数目错误亦已单独记录、不计负例。

最新已完成原生记录：`native-scene-20261117-202047-jycfaald/results.json`，包含独立legacy／strict的capture.fcs、每侧7张PNG、comparison.txt和实际命令／时间；其前一版202025采用固定delta，最新已改按真实相邻时间差，避免用相同错误时钟冒充结束态。没有游戏窗口、最终合成或设备听感。

### 阶段门与仍需继续的范围

阶段二暂保持**IN_PROGRESS**：资源／实现闭包、设计差异和一个原生小场景已有实物，但完整运行依赖输入、迁移清单及正式角色／条件场景执行接线尚未整体关闭。阶段三真实AtomChat两页仍未改源码／Gradle，不能把本段的小型闭环当真实消费者迁移。生产完整外壳／滚动／折叠／输入／素材／降级动态与视觉场景、CI普通构建／打包强制传播、最终三端成品、独审／用户实机／听感继续按方案阶段推进。

当前无必须用户决定的阻断。检查点不取消原实施授权；主代理只等待转达，仍由同一代理继续。未提交／push／公开发布／部署／启停游戏；没有改AtomChat同批WIP或四桥原WIP。

本自然停点实际集中回归：`stage-two-closure-regression.log`，core42／runtime74／ui188／checker52，共356项，失败／错误／跳过均0；完整XML已立即保存为`test-results-stage-two-closure.zip`，没有再让筛选结果覆盖证据。`publishToTestRepository`本地四模块成功，`stage-two-closure-maven.log`；最新候选`atomlib-frontend-contract-stage-two-closure-maven.zip`。随后新Java17／21隔离Maven消费者分别为`consumer-20261117-202319-jdk17-c8zfr6do`与`consumer-20261117-202319-jdk21-5pkqsthd`，全部正反例通过。该结果覆盖FCL03／compile-project及当前资源／源码构建链，不是尚未接线的完整场景门。当前无后台检查。


## 正式构建场景门与迁移入口清单接线（20:55安全记录）

- Gradle拆为`frontendContractSourceCheck`（源码／资源同源生成）和`frontendContractCheck`（依赖classes后实际执行场景）；processResources仅依赖前者，check／build／正式归档／发布任务依赖后者，避免classes—资源—场景循环。协议2没有受源绑定的scene.main、独立受保护capture或明确容差时失败；协议1明确PROTOTYPE_SOURCE_ONLY／NOT_ACTIVATED，不冒充完整启用。
- 增加生产runtime配置的真实依赖输入，排除主项目自身输出，外部可信实现的runtime-only JAR同样必须绑定。新增运行专用依赖缺绑定用例；不只检查javac编译输入。
- `SceneProcessCheck`实际启动新JVM／进程，每次新invocation与当前规范化包摘要，超时／非零退出／缺输出／旧执行标识／不同设计均失败。当前捕获不能用旧文件补样；日志保留在capture旁。
- Java17／21新干净消费者`consumer-20261117-204553-jdk17-2pxfyhhb`、`consumer-20261117-204553-jdk21-rn59nwjt`全项通过：完整普通build实际执行SCENARIOS_VALID；真实像素颜色变异VISUAL_DRIFT、旧invocation STALE_SCENE_CAPTURE、空采样MISSING_SAMPLE均使build红；原越权／草稿／候选／删除插件／恢复正反例保留。这两个消费者仍是合成display配置，不是AtomChat真实两页。
- 新原始坐标绕路用例先证明：即使Component源码已冻，页面仍能手算float坐标传给可信paint方法。补`UNSAFE_COMPONENT_INPUT`：协议2严格页面不能经可信方法传原始数值／Canvas／Rect／呈现几何；业务数值通过受限Number内容插槽。框架内部的固定实现仍可使用原绘制与几何算法。
- 普通源码报告输出精确ENTRY STRICT／STRICT_DEPENDENCY／LEGACY／IMPLEMENTATION／UNCOVERED与绑定类，作为迁移范围，不自动登记新legacy。新增命令行缺清单有效红例后转绿。
- 条件见证修正：正例必须来自真正启用的能力，含enabled守卫的合法false分支允许实际禁用实例，同时检查named enabled与真实实例状态一致；其余条件不能仅靠禁用样本充当false。该项先有效红，再checker全量**56项全绿，无失败／错误／跳过**，`stage-two-final-condition-regression.log`。上述消费者发生在最后这条条件修正之前，其合成display配置不涉及该分支，不冒称已测该条件。
- CI已写Windows原生独立旧路径／严格路径／hover变异，以及Java17／21隔离Maven完整场景消费者矩阵；Linux checker测试移除硬编码java.exe。未执行远程CI，不声称远程通过。

当前保存`stage-two-source-scene-checkpoint.json`、`test-results-stage-two-scene-checker.zip`和`atomlib-frontend-contract-stage-two-source-scene-maven.zip`；后者是最新本地0.5.0四模块候选，非MC三端成品。此前356集中回归及其完整XML仍独立保留；没有为进度重跑公共模块。

阶段二仍暂记IN_PROGRESS：在自然收口前继续确认动态见证按具体动效维度取真实进度，而非让hover采样同时替代press／ripple等其他维度；真实生产两页和完整场景属于阶段三，不混作本阶段已接入。没有用户输入依赖，无后台检查，授权继续。

AtomChat所有权复核：应用中唯一相关专项仍idle、docs/STATUS与handoff明确已收口；所选SettingsGroupFrontend／SettingsGroupPaint／SettingsDetailPage／两套SettingsSectionPage／ServerConfigPage没有未提交差异。本轮仍未写入AC。其头像／PNG／ReactionChips等其他WIP保留；本轮不改其gradle.properties。AC AGENTS／UI_CONTRACT中的11/14撤回旧SHA／trust／hook要求为历史约定，本轮11/17用户明确授权的前端契约锁按新范围实施，不恢复旧全仓hook或只读体系，也不继承旧部署授权。


## 协调中断后的立即检查点（2026-11-17 21:10）

按用户要求，本次仅核对／保存状态，未启动新实现或测试、未杀进程。所有已知exec会话已取回完成；Win32 Java／Python／Gradle进程查询未发现遗留，无后台检查，没有当前服务限流或必须用户决定的阻断。

342项之后的实质增量已记前文：资源／实现依赖闭包和内容绑定、FCL03规范化身份与设计差异、JDK插件加载隔离、运行条件、草稿／单位／声音真实提交链、普通构建实际场景门、迁移入口清单与原始坐标绕路拒绝、CI Java17／21矩阵。集中356项完整归档绿后，checker曾独立完整56绿；两个干净Maven消费者各18项正反例绿。不能把不同轮次或旧制品合并冒充最新整批通过。

**最新完成的自然阶段**：每种动效必须有自己的实际进度，不能由hover采样替代press。先有`per-aspect-motion-red.log`有效红例；新增只读`ControlObservation`和严格会话成功呈现帧快照，实际值来自Control而非声明时长。`control-observation-red.log`证明原先缺观察入口；`per-aspect-observation-green.log`本次checker相关7项、UI相关9项全部绿，无跳过。当前XML是筛选范围，不是完整checker／UI。此前完整356的XML、之后完整checker56的XML各自另存，不被当前筛选冒名。

原生探针最初把PRESS要求加给ActionButton，校验器正确拒绝参照：现有AtomChatControlSkin的ActionButton实际只有hover／focus，没有独立press画面。没有给原皮肤补造新动画，也没有放宽PRESS要求；探针改用既有、确有hover／press效果的Toggle。原有设置页标杆保持不动。

最新`native-scene-20261117-210831-eg4il0ls/results.json`于21:08:31—35完成：编译成功，两个独立JVM的旧UiSession路径和StrictPageSession路径各9帧逐像素／真实动作计数／hover与press引擎进度／声音输出参数通过；包含真实禁用点击、取消和静音。丢弃MOVE的变异按VISUAL_DRIFT middle红；丢弃PRESS的变异按DYNAMIC_SAMPLE_MISSING ACTUAL page/button/PRESS红。9帧PNG／capture.fcs／比较文本均落盘。这是合成Lib控件小场景，不是AtomChat两页迁移、Minecraft最终合成或设备听感。

**阶段二仍IN_PROGRESS，未宣布闭合**。最新观察API和逐维度动态规则尚未执行完整checker57／UI189，也尚未刷新并验证包含它们的Maven候选。之前`atomlib-frontend-contract-stage-two-source-scene-maven.zip`不包含这一最后增量，不能作为最新全部源码的已测产物。最新源码快照另存`frontend-contract-lock-interruption-source-20261117-2110.zip`，机器状态与当前XML另见`interruption-checkpoint-20261117-2110.json`／`test-results-interruption-checkpoint.zip`。

下一步：只跑受影响完整checker／UI并刷新本地候选及隔离消费者，再按阶段二门判断；随后落地真实两页组件适配和生产场景。AC所有权／状态已只读复核，所选文件无未提交改动，尚未写AC代码或Gradle。四桥原WIP与其他用户WIP保留。无用户输入依赖；本回合按协调请求可结束，同代理续作原目标，不是取消／暂停。仍0.5.0，未提交／push／公开发布／部署／游戏启停，无独审或设备听感验收。


## 阶段二门已闭合，进入真实消费者阶段（21:29）

最新观察／逐动效增量完整验证：`stage-two-latest-affected-regression.log`本次checker57／UI189均0失败／错误／跳过；完整XML立即保存`test-results-latest-affected-20261117.zip`。core42／runtime74与后续改动无关，沿用原有效记录，未机械重跑。

准备原宿主save(version, values)接线时，又补齐业务端口与视觉组件入口的区别：显式reviewed的businessMethods可以接收版本／数量等业务数值，仍在设计身份内；不能和privileged trusted UI入口混用。业务端口若含实际绘制，沿全项目入口分类或本地调用闭包拒绝。新用例先有效红，`business-port-checker-green.log`后完整checker**58项全绿，无跳过**。此前原始坐标保护继续有效，并未把一切数字都禁止。

刷新Maven后，两端全新隔离输入`consumer-20261117-212500-jdk17-_ln286q9`／`consumer-20261117-212500-jdk21-mnzb2zif`各19项全部通过：合法业务端口、普通build的实际场景、运行清单／资源、非法绘制／草稿／候选／自删插件，以及像素漂移／旧捕获／空采样／业务实现偷画均按指定规则红，恢复绿。不是旧342项或旧制品报告。

据第二版阶段二“小型完整严格场景”门槛，**阶段二已闭合，进入阶段三**：契约／只读句柄、严格页面到Yoga／同帧、实际声音提交、有限条件／逐动效见证、确定性资源／批准索引／状态机、源码及实现／素材依赖、草稿、迁移清单、设计差异及普通构建真实场景传播均已有正反例。闭合范围是通用机制与小场景，不等于真实AtomChat完整试点、远程CI或0.6验收。

最终阶段二候选`atomlib-frontend-contract-stage-two-complete-maven.zip`仍是0.5.0本地四模块输入；摘要`stage-two-complete-checkpoint.json`。候选不含随后阶段三的Yoga亚像素精度扩展，不混淆阶段范围。AC真实两页尚未改；当前正做将原有分组布局转到Yoga的坐标校准，以冻结原有外观／折叠／命中，未设计新皮肤。独审、游戏最终合成与设备听感仍待，未提交／push／公开发布／部署／游戏启停。
