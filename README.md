# DeepSeek 余额小组件

在 Android 桌面上查看 DeepSeek API 账户余额的小组件，基于 Jetpack Glance 实现。

## 功能

- 显示账户总余额与更新时间，点按卡片立即刷新
- 余额低于自定义阈值时数字变红
- 背景自动跟随系统深色 / 浅色模式
- 右下角表情包紧贴卡片边缘，可在设置页从相册更换
- Android 12+ 长按组件，从系统菜单的「编辑 / 自定义」直接进入设置页
- 余额数字使用 JetBrains Mono 字体（位图渲染，中文回退系统字体）
- 组件尺寸自适应（`SizeMode.Exact`）

## 构建

需要 JDK 17+ 与 Android SDK（compileSdk 35、minSdk 26）。

```bash
./gradlew assembleDebug
```

若所在网络访问 Gradle 官方发行地址较慢，可把 `gradle/wrapper/gradle-wrapper.properties`
中的 `distributionUrl` 换成镜像，例如：

```
distributionUrl=https\://mirrors.cloud.tencent.com/gradle/gradle-8.9-bin.zip
```

## 使用

1. 安装并打开应用，填入 DeepSeek API Key（在 platform.deepseek.com 创建），按需设置低额提醒阈值
2. 回到桌面，长按空白处 → 小组件 / 小部件 → 添加「DeepSeek 余额」
3. 点按组件刷新余额；长按组件 → 编辑进入设置；设置页可更换右下角表情包

## 隐私

API Key 仅保存在应用私有的 SharedPreferences 中，只用于请求
`https://api.deepseek.com/user/balance`，不会发送给任何其他服务。

## 已知限制

- 余额目前只在点按时刷新；余额变动后短时高频轮询的自动刷新在计划中
- 小组件内部无法响应长按手势，设置入口依赖 Android 12+ 的桌面长按菜单
- 非官方项目，与 DeepSeek 无关

## 许可

- 代码以 MIT 许可发布，见 [LICENSE](LICENSE)
- 内置 JetBrains Mono 字体按 SIL Open Font License 1.1 分发，协议见 [app/JetBrainsMono-OFL.txt](app/JetBrainsMono-OFL.txt)
- `app/src/main/res/drawable-nodpi/widget_mascot.png` 为作者提供的素材，如需再分发请自行确认授权
