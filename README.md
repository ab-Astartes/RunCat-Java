# 🐱 Java RunCat

> Windows 任务栏上的可爱跑步小猫 — Java 增强版

基于 [RunCat365](https://github.com/Kyome22/RunCat365) 概念开发，用 Java 重新实现并增强了多语言、自定义动画、系统仪表盘、桌面宠物等功能。

![Java 17+](https://img.shields.io/badge/Java-17+-orange)
![Platform](https://img.shields.io/badge/Platform-Windows-blue)
![License](https://img.shields.io/badge/License-MIT-green)
![Version](https://img.shields.io/badge/Version-1.2.0-brightgreen)

---

## 📸 功能预览

```
系统托盘动画：           桌面宠物（悬浮窗）：         系统仪表盘（左键点击）：
┌──────────────┐        ┌──────────┐              ┌─────────────────────────┐
│  🐱 → 🐱 → 🐱 │        │   🐱 64px │              │  📊 System Dashboard    │
│  小猫在跑！    │        │  可拖拽    │              │                         │
│  CPU: 23.5%   │        │  可交互    │              │  CPU Usage:  23.5%      │
│  Mem: 61.2%   │        └──────────┘              │  Memory:     61.2%      │
└──────────────┘                                  │  Disk IO:     12 MB/s   │
                                                  │  Network:     1.2 MB/s  │
                                                  │                         │
                                                  │  ▁▂▃▅▇█▆▄▃▂▁▂▃ CPU     │
                                                  │  ▃▄▅▆▇▆▅▄▃▄▅▆▇ Memory  │
                                                  │                         │
                                                  │  8192 MB / 16384 MB     │
                                                  └─────────────────────────┘
```

<img width="334" height="120" alt="image" src="https://github.com/user-attachments/assets/0e7b824c-e751-4964-836e-b04f77710f12" />


```
```
<img width="332" height="496" alt="image" src="https://github.com/user-attachments/assets/14aa64e5-b41b-40ea-808b-46967c0f52ee" />




---

## ✨ 功能特性

### 🐾 桌面宠物（v1.2.0 新增）

在桌面上显示可拖拽的 64×64 高清动画宠物，类似经典 QQ 宠物！

- 🖱️ **拖拽移动** — 按住拖到任意位置
- 🎾 **单击弹跳** — 点击宠物会弹跳
- 📊 **双击开仪表盘** — 快速查看系统状态
- 🎨 **右键菜单** — 调整大小 / 透明度 / 隐藏
- 💾 **位置记忆** — 自动保存位置，重启恢复
- 🔲 **多尺寸** — 64px / 96px / 128px 三档
- 👻 **透明度** — 60% ~ 100% 可调

### 🎬 内置动画（7 种 × 8 帧）

| 动画 | 描述 | 帧数 |
|------|------|------|
| 🐱 Cat | 经典橙色小猫跑步 | 8 帧 |
| 😴 Cat Sleep | 小猫蜷缩睡觉 | 8 帧 |
| 🐕 Dog | 棕色小狗奔跑 | 8 帧 |
| 🐴 Horse | 深棕色骏马 | 8 帧 |
| 🦜 Parrot | 绿色鹦鹉飞翔 | 8 帧 |
| 🐰 Rabbit | 白色兔子蹦跳 | 8 帧 |
| 🐧 Penguin | 黑白企鹅摇摇 | 8 帧 |

所有动画均有 16×16（托盘）+ 64×64（宠物）双分辨率版本。

动画速度与 CPU 使用率联动 —— CPU 越高跑越快！

### 🎨 自定义动画
导入你自己的 PNG 序列帧作为托盘动画，支持拖放导入。自定义动画存放在 `~/.java-runcat/animations/` 目录。

### 🌍 多语言支持（4 语言）

| 语言 | 代码 |
|------|------|
| 简体中文 | zh_CN |
| 繁體中文 | zh_TW |
| English | en |
| 日本語 | ja |

右键菜单 → Language 即可切换，立即生效。所有界面文字（菜单、对话框、动物名、数据标签）均已国际化。

### 📊 系统仪表盘

**左键点击**托盘图标，弹出迷你仪表盘：
- 实时 CPU / 内存使用率（颜色编码：正常蓝→警告橙→危险红）
- 磁盘 IO 速率（读/写 MB/s）
- 网络流量（上传/下载 MB/s）
- 60 秒历史折线图
- 内存详情（已用 / 总量 MB）
- 窗口位置自动记忆

### ⚠️ CPU 过高告警
CPU 超过阈值（默认 90%）时弹出 Windows 系统通知，可自定义：
- 开关：右键菜单 → Settings → CPU High Alert
- 冷却时间：默认 5 分钟内不重复提醒

### 🎨 图标主题
右键菜单 → Settings → Icon Theme，支持：
- **Auto** — 自动跟随 Windows 深色/浅色模式
- **Light** — 始终浅色图标
- **Dark** — 始终深色图标

### ⚡ 速度控制
右键菜单 → Speed 可选 Slow / Normal / Fast 三档。

### 🔒 单实例保护
自动防止重复启动，重复运行时弹窗提示。

### 🖱️ 静默启动
```bash
JavaRunCat.exe --silent    # 无初始通知
JavaRunCat.exe --help      # 查看帮助
```

### 🖥️ 开机自启动
右键菜单 → Settings → Auto Start，写入注册表实现。

---

## 🚀 安装与使用

### 方式一：直接下载 EXE（推荐）

从 [Releases](../../releases) 下载最新版 `JavaRunCat-Windows-x64.zip`，解压后运行 `JavaRunCat.exe`。

> 无需安装 Java，内嵌 JRE。

### 方式二：从源码构建 EXE

```powershell
# 前置：JDK 17+（打包需 JDK 21+），Maven 3.6+

# 一键构建（生成 dist/JavaRunCat/JavaRunCat.exe）
.\build-exe.ps1

# 如需生成 MSI 安装包（需安装 WiX Toolset）
.\build-installer.ps1
```

### 方式三：仅构建 JAR

```bash
mvn clean package
java -jar target/java-runcat-1.2.0.jar
```

### 右键菜单一览

```
📊 System Dashboard          ← 左键点击也可打开
─────────────────────
🎬 Animation
    🐱 Cat ✓
    😴 Cat Sleep
    🐕 Dog
    🐴 Horse
    🦜 Parrot
    🐰 Rabbit
    🐧 Penguin
    ──────────
    📦 Custom Animation...
⚡ Speed
    Slow / Normal / Fast
🐾 Desktop Pet ☑             ← v1.2.0 新增
    Size: 64 / 96 / 128
    Opacity: 60% ~ 100%
🌍 Language
    简体中文 / 繁體中文 / English / 日本語
─────────────────────
⚙ Settings
    ☑ Auto Start
    ☑ Show CPU Usage
    ☑ Show Memory Usage
    ☑ Show Time
    ☑ CPU High Alert
    🎨 Icon Theme (Auto/Light/Dark)
─────────────────────
ℹ About
❌ Exit
```

---

## 📋 版本历史

### v1.2.0 — 桌面宠物
- 🐾 新增桌面宠物窗口（可拖拽、弹跳、右键菜单）
- 🎬 新增兔子和企鹅动画，所有动画升级至 8 帧
- 😴 新增猫咪睡觉动画
- 🖼️ 双分辨率动画系统（16×16 托盘 + 64×64 宠物）
- 🎨 图标主题自动跟随 Windows 深浅色模式
- 📊 仪表盘新增磁盘 IO 和网络流量监控

### v1.1.0 — 增强仪表盘
- 📊 仪表盘新增磁盘 IO / 网络流量监控
- 🕐 托盘 tooltip 显示时间
- 🎨 图标主题自动跟随系统
- 🔧 修复中文菜单乱码（AWT → Swing JPopupMenu）

### v1.0.0 — 首个正式版
- 🐱 4 种内置动画（猫/狗/马/鹦鹉）
- 🌍 4 语言支持
- 📊 系统仪表盘 + CPU/内存折线图
- ⚠️ CPU 过高告警
- 📦 jpackage 打包为独立 EXE
- 🔄 GitHub Actions CI/CD 自动发布

---

## 🛠️ 技术栈

| 层级 | 技术 | 用途 |
|------|------|------|
| 语言 | Java 17 | 核心开发，使用 Swing/AWT 构建系统托盘 UI |
| 构建 | Maven + shade plugin | 依赖管理 + 打 fat JAR |
| 打包 | jpackage (JDK 21) | 生成原生 EXE，内嵌 JRE |
| 图标 | Java 2D API | 程序化生成应用图标 + 动画帧（支持多尺寸缩放） |
| 配置 | Gson | JSON 格式配置持久化 |
| 监控 | com.sun.management.OperatingSystemMXBean | CPU / 内存 / 磁盘 IO / 网络实时采集 |
| 国际化 | ResourceBundle | 4 语言 properties 文件 |
| 自启动 | Windows Registry (RegUtil) | 注册表写入开机启动项 |
| 图标格式 | 自实现 ICO 编码器 | PNG → Windows .ico 转换 |

### 项目结构

```
java-runcat/
├── src/main/java/com/runcat/
│   ├── RunCatApp.java                 # 入口 + 单实例锁 + CLI 参数
│   ├── config/
│   │   └── AppConfig.java             # 配置管理（JSON 持久化）
│   ├── core/
│   │   └── SystemMonitor.java         # CPU/内存/磁盘IO/网络监控 + 告警
│   ├── i18n/
│   │   └── I18nManager.java           # 多语言管理
│   ├── animation/
│   │   └── AnimationManager.java      # 动画帧加载 + HD缓存 + 自定义动画
│   ├── ui/
│   │   ├── TrayIconManager.java       # 系统托盘 + Swing右键菜单
│   │   ├── DashboardWindow.java       # 仪表盘 + CPU/内存/IO/网络折线图
│   │   ├── DesktopPetWindow.java      # 桌面宠物窗口（v1.2.0）
│   │   └── CustomAnimationDialog.java # 自定义动画导入对话框
│   └── util/
│       ├── AutoStartManager.java      # Windows 开机自启
│       ├── AnimationGenerator.java    # 生成内置动画帧（支持多尺寸缩放）
│       ├── IconGenerator.java         # 生成应用图标 PNG
│       └── IcoGenerator.java         # PNG → ICO 转换
├── src/main/resources/
│   ├── animations/                    # 16×16 托盘动画帧
│   ├── animations_hd/                 # 64×64 宠物动画帧（v1.2.0）
│   ├── i18n/                          # 4 语言 + 默认 properties
│   └── icons/                         # 应用图标 PNG + ICO
├── .github/workflows/
│   └── build-release.yml              # CI/CD: 构建 + 自动发布
├── build-exe.ps1                      # 一键构建 EXE 脚本
├── build-installer.ps1                # 构建 MSI 安装包脚本
├── pom.xml                            # Maven 配置
├── LICENSE                            # MIT 许可证
└── README.md
```

---

## 🌿 分支管理规范

| 分支 | 用途 |
|------|------|
| `main` | 稳定发布分支，只接受合并 |
| `feature/1.x.0` | 版本开发分支，功能完成后合并回 main 并打 tag |

**流程：**
1. 新版本功能在 `feature/1.x.0` 分支开发
2. 开发完成 → 合并到 `main`
3. 在 `main` 上打 tag `v1.x.0` 并发布 Release

---

## 📦 构建产物

| 产物 | 路径 | 大小 |
|------|------|------|
| Fat JAR | `target/java-runcat-1.2.0.jar` | ~350 KB |
| 原生 EXE | `dist/JavaRunCat/JavaRunCat.exe` | ~460 KB |
| 完整发布包 | `dist/JavaRunCat/` (含 JRE) | ~150 MB |

---

## 🤝 致谢

- [RunCat365](https://github.com/Kyome22/RunCat365) — 原始概念与灵感来源，C# / WPF 实现
- [Gson](https://github.com/google/gson) — Google JSON 库

---

## 📄 许可证

[MIT License](LICENSE) — 自由使用、修改、分发。
