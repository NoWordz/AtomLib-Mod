# AtomLib 0.7.0 跨平台原生库实施方案

**目标**　继续提供 Forge 1.20.1、Fabric 和 NeoForge 1.21.1 三个 JAR。每个 JAR 内置 Windows x64、ARM64 的原生库，macOS、Linux 的 x64、ARM64 在客户端首次需要时准备对应原生包。Android ARM64、x64 保留 Fabric 实验候选，OSS 开通后验证自动识别和下载校验。

**交接**　从 AtomLib `v0.6.0`（`1779a13e`）和 AtomChat `97a108cc` 接手。原有渲染桥工作区改动和历史性能分支有独立用途。发布回执、旧配对包及像素证据归档在 `D:/AtomLib-Backups/2026-11-19-v060-cleanup/`。本方案的第一步可用本地 HTTP 夹具和已固定的上游制品执行。

## 下载与发布决策

| 用途 | 来源与行为 |
| --- | --- |
| CI 获取上游制品 | Maven Central 为源，阿里云中央仓镜像作为构建备用。依赖坐标、版本和 SHA-256 均固定。 |
| 0.7.0 桌面首发 | macOS、Linux 缓存缺失时先取阿里云中央仓的上游 Skija 和对应 Yoga 原生 JAR，失败回退同坐标的 Central。提供离线 ZIP，Windows 继续内置。 |
| Android 实验包 | 用阿里云 OSS 香港地域托管项目自编译的 Yoga 和上游 Android Skija 组成的版本化 ZIP。游戏直接匿名 HTTPS GET，失败可手动导入同一 ZIP。 |
| 桌面运行时下载量增长或镜像频繁限流 | 将已生成的桌面离线 ZIP 一并放入项目 OSS，改为 OSS 优先。已有适用的已备案域名并确认线路收益时，可切到中国内地 OSS 配 CNAME。 |
| 已缓存、离线包和 Windows 内置库 | 直接校验、加载。缓存命中和 Windows 正常运行时零下载请求。 |

### 桌面首发路径

- 固定基础地址 `https://maven.aliyun.com/repository/central/` 和 `https://repo.maven.apache.org/maven2/`。例如 Linux x64 的 Skija 路径为 `io/github/humbleui/skija-linux-x64/0.143.18/skija-linux-x64-0.143.18.jar`。Fabric、NeoForge 对应的 Yoga 路径为 `org/lwjgl/lwjgl-yoga/3.3.3/lwjgl-yoga-3.3.3-natives-linux.jar`，Forge 对应 `3.3.1`。其余平台按表选择固定坐标和分类。
- 同一个依赖按相同路径回退。只提取清单指定的原生文件，校验 JAR 和提取后的文件。离线 ZIP 校验自己的外层摘要，安装到与 JAR 路径相同的缓存。如果本次切到 Central 的 Yoga，已经校验通过的 Skija 直接复用。
- 小范围首发按此方案，网络只在缺失或坏缓存时使用。若出现持续的 `429` 或下载量增长，切换到项目 OSS 的桌面 ZIP。Central 保持构建取材和过渡回退用途。 [阿里云 Maven 镜像](https://developer.aliyun.com/mirror/maven/) · [Sonatype 运行制品说明](https://central.sonatype.org/faq/429-operational-dependencies/)

### Android 无 GitHub 路径

1. 用阿里云 OSS 中国香港地域建一个专用发布 Bucket，先用其公网 HTTPS 域名。已备案域名可绑定中国内地地域 Bucket 的 CNAME 作为后续加速。首发先测香港域名在大陆线路的实际下载，按记录选部署地域。OSS 有存储、请求和外网流量费用。 [OSS 香港地域端点](https://help.aliyun.com/en/oss/user-guide/regions-and-endpoints) · [内地 OSS 默认公网域名限制](https://help.aliyun.com/zh/oss/publicendpointforbidden-error-when-upload-object)
2. 验收通过后将两个架构分别打包，例如 `atomlib-natives-0.7.0-android-arm64.zip` 和 `atomlib-natives-0.7.0-android-x64.zip`。每包包含该架构的 Skija 和已验证的 Yoga 版本。将包上传到 `natives/0.7.0/<sha256>/<文件名>`，旧对象继续服务同版本客户端。客户端下载地址形如 `https://<bucket>.oss-cn-hongkong.aliyuncs.com/natives/0.7.0/<sha256>/atomlib-natives-0.7.0-android-arm64.zip`。离线包与在线对象使用同一 ZIP。
3. 专用 Bucket 保持私有，发布对象单独设为公共读。若账户策略强制禁止公开读取，使用允许公共下载的 CDN 访问私有 OSS 源站。上传权限留在发布端，游戏只做匿名 HTTPS GET。上传后从大陆和境外分别下载完整 ZIP 核对摘要，再把真实 URL 写入发行清单。GitHub Release 可另外留一份备份，Android 默认源和备用离线包都无需访问 GitHub。 [OSS 永久下载链接](https://help.aliyun.com/zh/oss/user-guide/how-to-obtain-the-url-of-a-single-object-or-the-urls-of-multiple-objects) · [OSS 对象访问权限](https://help.aliyun.com/zh/oss/user-guide/permissions-and-access-control-overview)

自动模式优先试本机最近成功的来源，冷缓存按对应平台的固定顺序。连接或响应超时、下载停滞、HTTP 错误或校验失败时换下一来源。允许用户显式选择源或离线模式。语言、时区和 IP 归属地都不参与选源，也不接 IP 定位服务。首次运行和演练可以先用本地 HTTP 夹具。实际 Bucket 和可访问性在发布前确认。

2026-11-19 实测 Maven Central 和阿里云对 Skija 0.143.18 的 macOS、Linux、Android 六份 JAR 及 Yoga 3.3.1、3.3.3 的八份 macOS、Linux JAR 均返回 HTTP 200。本机完整下载的 Skija Linux x64 与 Android ARM64 分别在两源字节一致，对应官方发布摘要。华为云端点在本次 HEAD 探测中返回 403，暂不列为默认候选。此结果确认了制品可取得，国内地域性能以实际线路测量为准。Sonatype 建议运行制品采用项目自己的下载渠道并缓存上游依赖。 [Skija 0.143.18 制品](https://github.com/HumbleUI/Skija/releases/tag/0.143.18) · [Yoga 3.3.1](https://repo.maven.apache.org/maven2/org/lwjgl/lwjgl-yoga/3.3.1/) · [Yoga 3.3.3](https://repo.maven.apache.org/maven2/org/lwjgl/lwjgl-yoga/3.3.3/) · [阿里云 Maven 镜像](https://developer.aliyun.com/mirror/maven/) · [Sonatype 运行制品说明](https://central.sonatype.org/faq/429-operational-dependencies/)

## 平台与版本

| 客户端平台 | Skija 0.143.18 | Yoga | 0.7.0 验收目标 |
| --- | --- | --- | --- |
| Windows x64、ARM64 | 内置上游原生库 | 内置上游原生库 | 断网安装启动，分别以对应 JVM 验证 |
| macOS x64、ARM64 | 上游原生库 | 上游原生库 | CI 冷下载、离线复用、Skija CPU 绘制和 Yoga 布局 |
| Linux x64、ARM64 | 上游原生库 | 上游原生库 | glibc 上验证。x64 由本地验收，ARM64 由 CI 验证原生库 |
| Android ARM64、x64 | 已有上游 Android 库 | Android Bionic `liblwjgl_yoga.so` 3.3.3 | 以 Fabric 候选和启动器实际使用的 Java 21 验证自动识别、OSS 冷下载及 SHA-256 校验 |

Forge 用 LWJGL Yoga 3.3.1，Fabric、NeoForge 用 3.3.3。沿用 Minecraft 或启动器提供的 LWJGL core、GLFW 和 OpenGL。根据 JVM 进程架构匹配原生库，Apple Silicon 上的 x64 JVM 取 x64 包。Android 先于 Linux 识别，优先用已知启动器信号，必要时提供明确的 Android 平台覆盖选项，架构仍从 JVM 核对。未知平台应报告具体原因。 [现有依赖版本](../../../versions/targets.json) · [布局后端选择记录](../../decisions/yoga.md)

## 1. 先跑两项探针

- [ ] **桌面加载**。固定版本，从官方仓库各取一份 Linux x64 和 macOS ARM64 包，读取 ZIP 中的实际原生文件路径，记录依赖、长度和内容摘要。先在干净 Linux JVM 中跑 Skija CPU 光栅和 Yoga 布局，再在 macOS 对应 JVM 重跑。使用 `runtime/src/test/java/com/atom/lib/runtime/NativeLoadingProbe.java` 和 `probes/yoga` 的现有探针，检查是否意外用了 Gradle 或系统已有的原生库。
- [ ] **Android Yoga**。固定一版 Amethyst 和它的 Java 21／LWJGL 3.3.3。该启动器的公开 LWJGL 脚本使用 Android NDK，并显式设 `binding.yoga=false`。先复用它的 NDK 环境和 ABI 参数，试编译 LWJGL 3.3.3 的 `modules/lwjgl/yoga/src/main/c/yoga` 和 `modules/lwjgl/yoga/src/generated/c/org_lwjgl_util_yoga_Yoga.cpp`，产物只取 `liblwjgl_yoga.so`。记录 LWJGL 源提交、任何小补丁、NDK 版本、Android 最低 API 和导出符号。可先参考启动器脚本打开 Yoga 绑定验证，之后把最小构建配方留在 AtomLib。 [Amethyst 的 Android 构建脚本](https://github.com/AngelAuraMC/lwjgl3/blob/wip/rebase_3.3.3/ci_build_android.bash) · [LWJGL 3.3.3 Yoga 源码](https://github.com/LWJGL/lwjgl3/tree/3.3.3/modules/lwjgl/yoga) · [Android NDK CMake](https://developer.android.com/ndk/guides/cmake)

Android 的 `arm64-v8a` 与 `x86_64` 共用一套 NDK 构建配方。用 `llvm-readelf` 检查 ELF 机器类型及动态依赖，用 `llvm-nm` 检查 JNI 导出。现有 `org.lwjgl.util.yoga` 绑定由 `LibYoga` 加载 `lwjgl_yoga`，Forge 若后续加入 Android 实验，需要单独构建其 3.3.1 版本。 [LWJGL 3.3.3 的 LibYoga](https://raw.githubusercontent.com/LWJGL/lwjgl3/3.3.3/modules/lwjgl/yoga/src/generated/java/org/lwjgl/util/yoga/LibYoga.java) · [LWJGL 3.3.1 的 LibYoga](https://raw.githubusercontent.com/LWJGL/lwjgl3/3.3.1/modules/lwjgl/yoga/src/generated/java/org/lwjgl/util/yoga/LibYoga.java)

## 2. 制品、缓存和源切换

- [ ] 在 `versions/native-artifacts.json` 定义平台、依赖版本、上游 JAR 或发布 ZIP 的 URL、完整 SHA-256 与长度、允许提取的文件和各文件摘要。构建时将清单随主 JAR 打包。用 `tools/package_native_bundles.py` 从固定上游 JAR 提取需要的原生文件，打出 `atomlib-natives-0.7.0-<os>-<arch>.zip`。包内放一份 Skija 和按版本分目录的 Yoga，两线共享同一桌面平台包。Android 包只声明已验证的 Yoga 版本。保留上游许可和构建来源，发布时把同一 ZIP 上传到所有已配置的下载点。换下载源时随下一个发布版本更新内置清单，原生文件相同则直接复用缓存。
- [ ] 用 `runtime/src/main/java/com/atom/lib/natives/` 承载平台映射、制品查找、下载与缓存。缓存位于实例数据目录，按平台、依赖版本和原生文件摘要定位，使上游 JAR 和离线 ZIP 可以复用同一缓存。同一进程只启动一次准备任务，跨进程用文件锁。流式下载到临时文件，检查大小、外层制品摘要、条目名称和单文件摘要，验完再原子安装。命中缓存仍核验将要加载的文件，手动导入和联网使用同一验证。部分下载、损坏缓存、路径穿越、重复条目和未允许的重定向都要拒绝。
- [ ] 自动模式开始值建议为单源连接 3 秒、无进度 5 秒、整次准备 60 秒，在主界面可显式重试。使用客户端后台线程和清晰的来源回退原因。首次准备界面显示进度或错误，允许导入离线包。Windows 内置路径、已验证缓存和离线模式应在无网络探针中产生零请求。

验收用临时 HTTP 服务的请求计数与故障注入覆盖慢连接、404、429、断流、校验错误、两实例并发和缓存命中。先通过阿里云镜像安装一遍，再导入同内容离线 ZIP 验证复用，反向顺序也要跑。上线前从一条真实大陆线路和一条境外线路下载完整包并核对摘要。Android 增加一次禁止 AtomLib 访问 GitHub 的冷缓存启动，确认只靠 OSS 即可准备原生库。HTTPS 配置和服务商账号是发布环节的输入，先用本地夹具通过实现与测试。

## 3. 三端接入客户端生命周期

- [ ] 从三个 `AtomLibClient.java` 客户端入口异步启动准备。保护 `runtime/src/main/java/com/atom/lib/AtomLibRuntime.java` 的 Skija 初始化和 `ui/src/main/java/com/atom/lib/layout/internal/YogaNatives.java`、`YogaLayoutEngine.java` 的 Yoga 首次调用。由公共 Java API 给宿主提供未开始、准备中、可用、失败状态及可观察的失败原因，客户端可按状态重试。使用 Skija 已有的目录加载接口和现有 LWJGL Yoga 加载路径，保留宿主显式配置。让临时的准备或网络错误停在普通可重试状态，避免锁死类静态初始化。保留显式运行时入口、已可用的原生路径、正常 Windows 消费者和专服的现有用法。 [Skija 0.143.18 的 Library 加载实现](https://raw.githubusercontent.com/HumbleUI/Skija/0.143.18/shared/java/impl/Library.java)
- [ ] 用 AtomChat 的真实 Screen/HUD 以及一处直接走 Lib API 的消费者验证加载顺序。等待时由 Minecraft 原生界面显示准备状态，渲染线程可继续工作，后续成功能打开原界面。同一 JVM 用同一明确的 Skija/Yoga 组合，升级后从新进程加载新版本。专服测试确认无网络、无原生初始化。
- [ ] 复用 `tools/verify_artifacts.py` 检查三份安装 JAR。确认 Windows x64、ARM64 原生载荷齐全，macOS、Linux、Android 载荷由独立包分发。公开 README 和接入说明补自动下载、缓存位置、离线导入和失败诊断。

## 4. 实机与发布判据

- [ ] 在 GitHub Actions 的 macOS Intel、macOS ARM64、Linux ARM64 和 Windows ARM64 runner 上，分别对 Forge、Fabric 和 NeoForge 的实际安装 JAR 执行 Skija CPU 绘制及 Yoga 布局探针。Forge 使用 Java 17，Fabric 和 NeoForge 使用 Java 21。macOS 和 Linux 各验空缓存下载及离线重启，Windows ARM64 验 JAR 内置库。
- [ ] Linux x64 和 Windows x64 的游戏内验证在本地进行。其他桌面环境的 CI 结果只记录原生库验证，游戏内验证单独记录。
- [ ] OSS 开通后，用 Android 启动器实际使用的 JVM 确认自动识别 Android 及 JVM 架构，然后用空缓存下载对应 ZIP 并核对 SHA-256。记录启动器、JRE、ABI 和下载结果。Android 本轮验收止于识别和下载校验。
- [ ] 调整 `ui/build.gradle`、`runtime/build.gradle` 和 `gradle/render-adapter.gradle` 中的测试原生依赖，使测试可按宿主运行，但安装 JAR 仍由 `gradle/atomlib-dependencies.gradle` 保持 Windows 内置范围。在已支持的架构运行 `./gradlew :core:test :runtime:test :ui:test :frontend-checker:test` 和目标平台的 `hiddenGlTest`。Windows 可用 `gradlew.bat` 与现有测试字体参数，Linux 隐藏窗口用 Xvfb/Mesa。发布流水线实际从每个公开源下载或导入生成好的原生包，匹配同一摘要，再发布宣称支持的范围。

## 11-19 本地候选记录

- 桌面候选在 `build/v070-desktop/candidates/`。三端 JAR 分别通过制品检查，四份离线 ZIP 有固定 SHA-256。构建快照排除了四处既有渲染桥改动。
- WSL Linux x64 用 Java 17 和 21 分别加载 Skija 及两版 Yoga。验证了阿里云冷下载、离线缓存、ZIP 导入、慢镜像回退和两 JVM 共用下载。Fabric 1.21.1 的独立客户端完成主菜单导入。
- AtomChat 三端编译及配对输入校验通过。Fabric 本地验证版实测冷缓存进世界并恢复面板与 HUD。既有前端审定版本拒绝新的 AtomLib 机制标识，正式配对包仍需新审定。
- Android 的两份离线 ZIP 和 Fabric 验证 JAR 在 `build/v070-android/candidates/`。Yoga 3.3.3 的两个 ELF 均核对了 158 个 JNI 导出，ZIP 已过本机导入探针。实际 Android、macOS 和 ARM64 桌面客户端验收及 OSS 地域实测仍待完成。
