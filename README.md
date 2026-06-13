![HEAD](https://socialify.git.ci/TYOPXN360/PixelLauncherEnhanced/image?description=1&font=Jost&forks=1&issues=1&logo=https%3A%2F%2Fi.postimg.cc%2FmgfNTCbc%2Fpixel-launcher-enhanced.png&name=1&owner=1&pattern=Formal+Invitation&pulls=1&stargazers=1&theme=Auto)

<p align="center">
Pixel Launcher Enhanced 是一个 Xposed 模块，可以为你的启动器解锁各种实用功能。虽然名字叫 Pixel Launcher Enhanced，但它同样完整支持 Launcher3。从自定义外观到增加更多功能，这个模块能让你的启动器体验更好。
</p>
<br>
<p align="center">
  <a href="https://github.com/TYOPXN360/PixelLauncherEnhanced/releases"><img src="https://img.shields.io/github/downloads/TYOPXN360/PixelLauncherEnhanced/total?color=%233DDC84&logo=android&logoColor=%23fff&style=for-the-badge" alt="下载量"></a>
  <a href="https://github.com/TYOPXN360/PixelLauncherEnhanced"><img alt="仓库大小" src="https://img.shields.io/github/repo-size/TYOPXN360/PixelLauncherEnhanced?style=for-the-badge"></a>
</p>

# v1.1.5 更新说明

> 本次升级将模块从旧版 Xposed 框架迁移到了全新的 libxposed API 101。全部升级工作由 AI 辅助完成，已通过功能测试。

### 主要变化

- **框架升级**：从旧版 Xposed API 迁移到 libxposed API 101，适配最新的 Android 系统
- **兼容性提升**：模块现在可以在新版 LSPosed 框架下正常运行

### 修复的问题

- 修复部分功能设置开关不生效的问题
- 修复「清除所有」按钮导致启动器崩溃的问题
- 修复桌面图标标签和应用抽屉图标标签消失的问题
- 修复应用抽屉主题色图标显示异常的问题
- 修复在桌面弹出菜单中显示入口的功能
- 修复启动器反复卡死（ANR）的问题
- 修复 Release 版本因混淆导致功能失效的问题

### 已知问题

- 少数功能因启动器版本更新导致方法名变化，暂时不可用（如搜索栏、手势状态等），不影响主要使用

> ⚠️ **本仓库仅适用于 Android 17 金丝雀版（Canary）或 Beta 版本的启动器。**

---

# 🌟 功能列表

> ✅ 完全支持
> ⚠️ 部分支持
> 🚫 不支持

<details>
<summary>图标</summary>

| 功能 | Pixel Launcher | Launcher3 |
|------|:---:|:---:|
| 强制主题图标 | ✅ | ✅ |
| 移除快捷方式角标 | ✅ | ✅ |
| 图标大小 | ✅ | ✅ |
| 文字大小 | ✅ | ✅ |
| 自定义主题图标颜色 | ✅ | ⚠️ |

</details>

<details>
<summary>主屏幕</summary>

| 功能 | Pixel Launcher | Launcher3 |
|------|:---:|:---:|
| 锁定布局 | ✅ | ✅ |
| 双击锁屏 | ✅ | ✅ |
| 壁纸缩放 | ✅ | ✅ |
| 隐藏状态栏 | ✅ | ✅ |
| 隐藏顶部阴影 | ✅ | ✅ |
| 深色状态栏图标 | ✅ | ✅ |
| 深色页面指示器 | ✅ | ✅ |
| 桌面图标标签 | ✅ | ✅ |
| 桌面列数 | ✅ | ✅ |
| 桌面行数 | ✅ | ✅ |
| 隐藏「一览」 | ✅ | ✅ |
| 隐藏桌面搜索栏 | ✅ | ✅ |
| 搜索栏透明度 | ✅ | 🚫 |
| 底栏间距 | ✅ | ✅ |

</details>

<details>
<summary>应用抽屉</summary>

| 功能 | Pixel Launcher | Launcher3 |
|------|:---:|:---:|
| 主题色图标 | ✅ | ✅ |
| 搜索栏开关 | ✅ | ✅ |
| 应用抽屉图标标签 | ✅ | ✅ |
| 隐藏应用 | ✅ | ✅ |
| 搜索隐藏应用 | ✅ | ✅ |
| 应用抽屉背景透明度 | ✅ | ✅ |
| 应用抽屉列数 | ✅ | ✅ |
| 行高倍数 | ✅ | ✅ |

</details>

<details>
<summary>最近任务</summary>

| 功能 | Pixel Launcher | Launcher3 |
|------|:---:|:---:|
| 清除所有按钮 | ✅ | ✅ |
| 移除截图按钮 | ✅ | ✅ |
| 禁用最近任务实时卡片 | ✅ | ✅ |
| 最近任务背景透明度 | ✅ | ✅ |
| 小窗模式手势 | ✅ | ✅ |

</details>

<details>
<summary>其他</summary>

| 功能 | Pixel Launcher | Launcher3 |
|------|:---:|:---:|
| 隐藏手势指示条 | ✅ | ✅ |
| 隐藏导航栏空间 | ✅ | ✅ |
| 桌面弹出菜单显示入口 | ✅ | ✅ |
| 防止壁纸变暗导致重启 | ✅ | ✅ |
| 开发者选项 | ✅ | 🚫 |
| 重启启动器 | ✅ | ✅ |

</details>

# 🛠 使用要求

- **Root 权限**：部分功能需要 Root 才能正常使用
- **Xposed 框架**：需要安装支持 libxposed API 101 的 Xposed 框架（如 LSPosed）

# 🔧 安装方法

1. 下载并安装 [Pixel Launcher Enhanced APK](https://github.com/TYOPXN360/PixelLauncherEnhanced/releases)
2. 为模块授予 Root 权限
3. 在 Xposed 管理器中启用模块，并勾选目标启动器
4. 强制停止启动器，等待重启
5. 享受新功能 🎉

# 📝 注意事项

本模块仅兼容以下启动器：

- Pixel Launcher `(com.google.android.apps.nexuslauncher)`
- Launcher3 `(com.android.launcher3)`

以下功能需要 Root 权限：

- 双击锁屏
- 重启启动器

# 🙏 致谢

- 感谢 [Mahmud0808](https://github.com/Mahmud0808) 开发的原始模块
- 本次升级由 AI 辅助完成，已通过功能测试，确保功能可用性
