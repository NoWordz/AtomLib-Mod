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
