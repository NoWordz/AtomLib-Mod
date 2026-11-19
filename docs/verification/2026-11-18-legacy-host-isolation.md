# AtomLib 0.6 宿主隔离的兼容边界

两个独立测试宿主分别使用部署备份中的 NeoForge AtomLib 0.4.0 编译，打成各自的 JAR。运行时只加载 AtomLib 0.6.0 UI JAR 和同版本 Skija，两个宿主不重编。第一个宿主读取的强调色从 `FFCC3344` 变为第二个宿主注册的 `FF2244EE`，第一个宿主触发的波纹送到第二个宿主。倒换注册顺序后，颜色和波纹都改由第一个宿主决定。显式默认主题两次读取均为 `FF4A90E2`。

同一探针用 0.6.0 编译从未注册主题的新消费者。`UiTokens.accentFill()` 在 `-Xlint:deprecation -Werror` 下因弃用报错，`UiTokens.accentFill(Design.defaultHost())` 编译通过。检查只涉及公开 Java API，无需启动 Minecraft。真实 Module-Shop 2.3.0 与迁移后的 AtomChat 同 JVM 双向切换已有单独证据，参见[配对验证](2026-11-18-frontend-contract-lock-stage-three.md#11-18-晚间配对验证)。

用户已在「心自安然」确认 Module-Shop 不再污染迁移后的 AtomChat。另用备份的 AtomChat 0.6.0、实际 Module-Shop JAR 和已部署的 AtomLib 0.6.0 复现旧调用串扰，强调色从 `FF4A90E2` 变为 `FF2563D8`。这两条使用实际宿主的证据与上面的独立测试 JAR 结果一致。旧包结果在 `build/frontend-contract-lock/appearance-default-probe-807ecb00/legacy-chat-new-lib.out`。

## 复现

在 AtomLib 根目录执行。参数填写本地旧发行包和依赖的实际位置。

```powershell
.\gradlew.bat :ui:jar --offline --no-daemon
python tools/frontend-contract-lock/verify_legacy_appearance.py --java-home "<JDK 17>" --old "<AtomLib 0.4.0 NeoForge JAR>" --new build/standalone/_ui/libs/ui-0.6.0.jar --skija "<skija-shared-0.143.18.jar>"
```

2026-11-18 最新 NeoForge 平台包结果在 `build/frontend-contract-lock/two-legacy-jars-zs2godwc`，含生成的两个宿主 JAR、输出及编译诊断。旧 API 延续最后写入生效；全版本升级无法给没有宿主标识的旧调用推断正确的隔离范围。保留兼容行为时，只有使用显式入口的宿主能获得隔离保证。

两个未重编宿主共享同一份已加载的 `Design`，旧读写方法也没有宿主参数。按调用栈或线程猜宿主无法覆盖共享 helper、回调与延迟绘制。分离类加载器或重写旧 mod 字节码需要加载器介入，还会改变旧 API 的共享语义和类型身份。0.6.0 因此保留旧链接行为，在新代码的隐式读写处给出弃用诊断。新宿主可显式传入 `Design.defaultHost()` 或自己的 `DesignHost`，点击反馈由该宿主提供。
