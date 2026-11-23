# AtomChat design package

0.7.2 的集合皮肤扩展保留原有外观和解释器 epoch 1。语义代际与构建追溯指纹的区别、升级规则及旧绑定支持范围见 [API compatibility](../api-contracts.md#frontend-contract-compatibility)；集合扩展接口见 [控件文档](controls.md#072-集合皮肤)。

## 0.5 安全入口

新界面优先使用 `AtomDefault.createSession(runtime)`；每个窗口拥有自己的 theme/motion 选择、控件、焦点与反馈。`beginFrame` 固定快照，SettingsShell 和控件共同消费实例皮肤；`frame.card/panel` 用于直绘与明确的模糊降级。主题与动效可独立预览，选择器不读写宿主配置。完整示例、材质与兼容边界见 [sessions](sessions.md)。

下方为低层/旧入口说明；`skin(palette)` 仍可使用，但完整帧材质与隔离便利入口是 `createSession`。

## AtomDefault 低层皮肤入口

`com.atom.lib.design.atomdefault.AtomDefault` 提供独立皮肤入口：`skin()` 使用现有默认外观；`skin(AtomDefault.presets().get(i))` 创建使用指定配色的新实例。每个宿主持有自己的 `ControlContext`、皮肤与 `TextService`；切换颜色时在该皮肤上调用 `updateStyle(palette.controlStyle(radius, halfScreen, cardTint))`。原 `AtomChatControlSkin(AtomChatControlStyle)` 与 `design.atomchat` API 保留，旧全局 `Design.host` / `Design.rippleSink` 仍有全局状态，不应作为多宿主隔离入口；旧入口弃用期限尚未确定。

十二个 `AtomDefaultPalette` 为不可变值，包含强调、面板、主次文字、卡片、边框色与面板透明度、是否模糊的建议值。配色源自 AtomChat，许可证及文件版本见 [来源基线](../atomdefault-baseline-2026-11-02.md)。聊天气泡色、胶囊、业务配置与动作未映射；圆角不跟随预设变化。面板透明度、边框色与模糊票据目前只提供给宿主，皮肤不会自动应用或捕获背景；宿主须明确处理。默认皮肤工厂没有选择任意一套预设。预设皮肤的模态底色跟随面板色，固定旧构造器仍使用旧模态色；其余组件和效果尚未全部主题化。

明暗控件离屏图及隔离测试只是实现证据；字体角色、ripple 的跨宿主完整行为、模态各状态、实际游戏和用户观感仍待验证。

> 本地候选新增 AtomChatFonts 与 AtomChatControlSkin／AtomChatControlStyle，为可选 Slider／Toggle／ConfirmationDialog 提供 AC 材质。新路径使用实例不可变 style 与显式 UiCards/UiTokens 参数，不安装或临时替换全局 Design.host。`Design.defaultHost()`只返回不可变默认值。旧全局路径仍兼容，未由本次声称完成所有旧消费者的多mod改造。见 [完整合同](controls.md)。

> 模态外层限宽与留白由 AtomChatControlSkin.modalDialogStyle(Viewport)提供，UiSession 仅通用居中；非 AC 皮肤可使用自己的尺寸策略。本轮不实现背景模糊，新增模糊讨论文档仅随完整源码快照保留。

`com.atom.lib.design.atomchat` is an optional layer on top of the render
bridge. It is the look AtomChat ships: frosted cards, 20-unit corner radius,
blue accent, hairline rims. Import it when you want that appearance; skip it
and draw with your own tokens if you do not.

| Class | What it gives you |
|---|---|
| `UiTokens` | Sizes, radii, shadow constants, colour derivation (`cardFill`, `accentFill`, `cardHover`, `onAccent`, `hairline`) |
| `SkiaDraw` | Rounded rect, chrome shadow, edge highlight, ring, rounded/cover image, blur backdrop, clip, colour lerp |
| `UiCards` | Card surface, float surface, notification backdrop |
| `AppIcons` | Icon path data |

For direct drawing, pass one immutable theme snapshot to both measurement and paint:

```java
var theme = Design.defaultHost();
float radius = UiTokens.cardRadius(theme);
UiCards.drawCard(canvas, x, y, width, height, radius, hover, theme);
```

Pass a host-owned `Design.RippleSink` as the final argument for click feedback.
The overload shown above needs no feedback registration. A window that uses
the full settings widgets can instead use `AtomDefault.createSession(runtime)`.

The compatibility methods on `Design`, plus the `UiTokens` and `UiCards`
overloads that use them implicitly, are deprecated in 0.6. Compiled legacy
callers still share the last registered theme and callback, including a caller
that only reads the defaults. New Java source can turn these warnings into
errors with `-Xlint:deprecation -Werror`. Pure operations such as `UiTokens.s`,
`Design.isLight` and the explicit overloads need no registration.

The old descriptors carry no host or window identity. Two unchanged legacy
mods can paint different windows on the same render thread, or use the same
helper library. Partitioning by thread or caller class would change observable
behavior without reliably identifying the window. An owner can isolate a legacy
renderer by migrating its calls to explicit context. Transparent isolation for
two unchanged legacy mods would require changing the legacy contract or
intercepting calls with an authoritative host identity supplied by the loader.
See the [two-JAR probe](../verification/2026-11-18-legacy-host-isolation.md).

Not included on purpose: layout, motion timing, message bubbles, screen chrome,
composers and any screen-level composition. These classes provide tokens and
drawing helpers, not widgets; the generic component, overlay and shell layer
from earlier extraction rounds has been deleted.
