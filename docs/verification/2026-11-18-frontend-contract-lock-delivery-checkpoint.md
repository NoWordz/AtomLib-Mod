# 前端契约锁：交付状态核对

## 11-18 用户要求收口后的状态

用户已明确不再等待继续实施，随后要求提交、更新状态和将临时文件移入回收站。本轮按未完成的本地开发检查点保存，停止自动续作；唯一实施代理已关闭，不推送、发布、部署或启停游戏。

后文是12:48已保存的历史检查结果，包含当时“原任务继续”的叙述；当前收口状态以本段和 docs/STATUS.md 为准。核心功能未全部完成，尤其真实两页完整契约接入与生产检查门；不是只缺打包或用户验收。

候选、源码快照和验证历史保存在 D:/AtomLib-Backups/2026-11-18-fcl-closeout/。candidate/ 保存最后七模块 Maven 输入，source-checkpoint/ 保存125个任务文件的快照，verification-and-history.zip 按原 build/frontend-contract-lock 路径保存验证记录。部分重复 Maven 解包目录可由保留的输入 ZIP 重建，不保留重复副本。临时目录仅送回收站，清单和回收回执在同一归档目录。

收口时核对了112个Lib及13个AtomChat任务文件与交付源码快照逐字节一致；本轮仅修改状态文档，没有重跑业务测试。原四处Lib渲染桥及AtomChat其他WIP保留，不混入本轮提交。具体提交号在归档回执中保存，不将本检查点误写为完整0.6.0。

2026-11-18 12:48（Asia/Shanghai）。按用户最新要求，只核对并保存，不新增实现或扩大测试。已取回中断前的最后一项生产重放结果，未结束的检查为空。未提交、push、公开发布、部署或启停游戏。

## 1. 当前实际完成层级

**框架尚未完整完成，不能作为完整 0.6.0 前端契约锁交付用户使用。**

阶段一可行性门已闭合；阶段二的小型完整场景、独立检查器及分发管线已闭合；阶段三仍进行中。已有契约模型、编译/批准与候选区分、源码/资源/实现依赖检查、严格页面会话、宿主适配、声音意图及三端输出。严格会话当前仍以平铺组件为主；真实分组、折叠、滚动、固定外壳及模态尚未完成整体组合与试点迁移。

可交接的是工程候选源码、构建输入和验证证据；不能称为已启用完整锁的游戏成品。最后讨论的嵌套组件/容器组合仍只是分析，未写入源码。

## 2. 当前产物与源码对应

最新已验证归档：`build/frontend-contract-lock/atomlib-frontend-contract-stage-three-hosted-candidate-maven.zip`，SHA256：

`70265096999d6b299cb85b4d6544911f22cdce77fc73f4a840fec6f867ab9593`

它是 **Maven 构建输入归档**，含 core/runtime/ui/frontend-checker 四模块和三端 Lib 坐标，共七模块；其中三端的主 JAR 是可安装格式的 Lib 候选，但整个 ZIP 不能放进 mods。它不含本轮 AtomChat 修改源码或由这些修改重打的 AtomChat 游戏包。

只读核对七个主 JAR 均与已构建身份清单一致；各模块 sources JAR 中与当前生产源码/资源对应的271份文件没有缺失或字节差异。共同机制身份为 `e2b1200403fd51e9f7ec4248ad4e7e3c45987882099e07d1eafe92fc0643975e`。详见 `build/frontend-contract-lock/delivery-state-audit-20261118.json` 及 `stage-three-hosted-paired-input-identity.json`。没有为核对重启构建。

该输入已经由实际 AtomChat 获取工具成功导入隔离目录；未写入游戏实例。它适合继续开发和内部验证，**不满足直接作为完整前端契约锁交用户使用的条件**。

## 3. 有效验证与失败边界

- 12:29 完整集中回归：core44 / UI196 / checker61，共301项，失败、错误、跳过均0；命令及日志 `stage-three-current-adapter-regression-maven.log`，完整 XML `test-results-stage-three-current-adapter.zip`。其后新增的 HostedControl 适配器取回方法及真实托管组件，由后续三端编译和实际 AtomChat 两项组件测试覆盖；未把301冒称为最后一行源码后的再次集中执行。runtime74沿用相关实现未变的既有结果。
- 最新七模块本地 Maven 与三端 build/local publish/verifyArtifact 通过：`stage-three-toggle-adapter-maven.log`、`stage-three-toggle-adapter-platforms.log`。
- 最新 Java17/21 隔离消费者各19项正反例通过：`consumer-20261118-123758-jdk17-ju59ryom`、`consumer-20261118-123808-jdk21-ovhgfo1x`。
- 实际 AtomChat 使用新候选编译，HostingToggleComponentTest 两项通过，无跳过：按下提交/声音/静音/禁用，以及原行像素与外观隔离。`stage-three-actual-hosted-toggle-green.log`、`stage-three-actual-hosted-toggle-proxy-green.log`。旧/新开关时钟与外观选定回归另有成功日志；部分早期 XML 已被后续筛选运行替换，不并入301归档。
- 真实控件输入 → 严格反馈 → 正式 Fabric 适配器字节码 → typed SDK 播放边界通过；漏声音、交换最终音量/音高两项变异均按预期断言失败。证据 `hosting-output-afc581zg/results.json`。SDK 播放端为替身，未运行真实游戏混音或声卡，不是听感验收。
- 最新真实两页基本重放 `production-replay-20261118-124316-pbcqtg31`：先复现原14帧锚点，历史14、托管14分别与独立冻结旧实现逐像素相同。较早候选另有2页×2语言×2宽度×2主题共224帧通过，保留其对应范围；不冒称最新版已重跑全部矩阵。
- 真实6份声音/字体资源闭包与字体漂移拒绝通过。三端实际适配编译、资源解码及早期输出变异证据按对应代码范围沿用。

已修复并保留记录的失败：platform sourcesJar 缺任务依赖；同版本 Fabric 旧 remap 缓存造成假 UP-TO-DATE；新/旧测试未隔离配置；坐标用例最初缺 viewport clip；新组件方法命名与 Adapter.text 冲突；SDK 替身最初缺 SoundInstance 精确方法描述符。以上均不算预期负例成功，后续修正结果另存。当前没有未结束的失败检查。

尚未执行：完整真实两页的所有条件/编辑/模态/恢复/退场与中断场景、正式消费者完整前端 CI 门及远程 CI、最终独立审查、指定游戏窗口与实际设备听感。

## 4. AtomChat 真正改动范围

生产源码已经改动：

| 文件 | 实际变化 |
| --- | --- |
| `platforms/1.21.1-fabric/.../settings/SettingsSectionPage.java` | 仅 CHAT/history 选定路由接 Yoga 行布局。 |
| `layers/mapping/official/.../settings/SettingsSectionPage.java` | 同上，覆盖另两端的共享映射来源。 |
| `shared/.../config/ServerConfigPage.java` | 仅 administration/ROOT/托管标签接 Yoga 行布局；未改为完整严格模板。 |
| `shared/.../settings/contract/SettingsYogaLayout.java` | 新增真实 Yoga 行/间隙/折叠测量适配。 |
| `shared/.../ui/ToggleSwitch.java` | 增加显式帧时钟、脉冲和不可变外观接缝；原调用路径保留。 |
| `shared/.../settings/contract/HostingToggleComponent.java` | 新增原托管开关行的严格宿主组件，已编译并由集成测试实例化；**尚未由 ServerConfigPage 的真实路由调用**。 |

另改 AtomLib 输入版本及三端真实摘要、候选获取工具/测试、CI输入准备；新增上述组件、时钟和布局测试。没有整体替换原页面。真实设置页和服务端页的改动不是“只写了隔离副本”，但完整受控组件测试也不能冒充整页已接入。

未覆盖另一专项的头像/PNG/ReactionChips 等 WIP；Lib原四处渲染桥仍保留11-16原修改时间，未写它们。

## 5. 最小剩余工作

1. 完成严格组件的分组/容器组合与原有模板适配，接通两页全部内容、数字编辑、分组折叠、滚动、固定header及实际模态；不是单纯补几项测试或再打一个包。
2. 把完整真实前端清单编为正式候选定义与入口/合成绑定，接入实际路由；建立声音在真实宿主输入中的归属、完整条件场景与普通构建/CI阻断，并完成该批正反例及最终配对产物。保留已明确的产品认可，不再请用户重新选版本。
3. 完成独立审查及经授权的实机/听感验收。未满足前只能交候选。

目前没有必须用户决定的产品分歧；主要剩余工作是实施，不能把它隐去称为“只等用户验收”。

## 6. 安全停止点

已取回最后生产重放结果。只读进程检查未发现仍运行的 Java/Python/Gradle 检查；没有本任务后台进程会继续改文件。没有杀进程，没有开启新长检查。唯一实施代理在此检查点停止本回合操作，供主流程转达和评估；原任务未被标为完成、取消或暂停。
