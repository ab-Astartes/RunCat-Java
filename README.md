# 🐱 Java RunCat

> Windows 任务栏上的可爱跑步小猫 — Java 增强版

基于 [RunCat365](https://github.com/Kyome22/RunCat365) 概念开发，用 Java 重新实现并增强了多语言、自定义动画、系统仪表盘等功能。

![Java 17+](https://img.shields.io/badge/Java-17+-orange)
![Platform](https://img.shields.io/badge/Platform-Windows-blue)
![License](https://img.shields.io/badge/License-MIT-green)

---

## 📸 截图预览

```
系统托盘动画：           系统仪表盘（左键点击）：
┌──────────────┐        ┌─────────────────────────┐
│  🐱 → 🐱 → 🐱 │        │  📊 System Dashboard    │
│  小猫在跑！    │        │                         │
│  CPU: 23.5%   │        │  CPU Usage:  23.5%      │
│  Mem: 61.2%   │        │  Memory:     61.2%      │
└──────────────┘        │                         │
                        │  ▁▂▃▅▇█▆▄▃▂▁▂▃ CPU     │
                        │  ▃▄▅▆▇▆▅▄▃▄▅▆▇ Memory  │
                        │                         │
                        │  8192 MB / 16384 MB     │
                        └─────────────────────────┘
```

---

## ✨ 功能特性

### 🎬 内置动画
| 动画 | 描述 | 帧数 |
|------|------|------|
| 🐱 Cat | 默认小猫，橙色跑步 | 5 帧 |
| 🐕 Dog | 棕色小狗 | 5 帧 |
| 🐴 Horse | 深棕色骏马 | 5 帧 |
| 🦜 Parrot | 绿色鹦鹉 | 5 帧 |

动画速度与 CPU 使用率联动 —— CPU 越高跑越快！

### 🎨 自定义动画
导入你自己的 PNG 序列帧作为托盘动画，存放在 `~/.java-runcat/animations/` 目录。

### 🌍 多语言支持
| 语言 | 代码 |
|------|------|
| 简体中文 | zh_CN |
| 繁體中文 | zh_TW |
| English | en |
| 日本語 | ja |

右键菜单 → Language 即可切换，立即生效。

### 📊 系统仪表盘
**左键点击**托盘图标，弹出迷你仪表盘：
- 实时 CPU / 内存使用率（颜色编码：正常蓝→警告橙→危险红）
- 60 秒历史折线图
- 内存详情（已用 / 总量 MB）
- 窗口位置自动记忆

### ⚠️ CPU 过高告警
CPU 超过阈值（默认 90%）时弹出 Windows 系统通知，可自定义：
- 开关：右键菜单 → Settings → CPU High Alert
- 冷却时间：默认 5 分钟内不重复提醒

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

从 [Releases](../../releases) 下载最新版 `JavaRunCat.zip`，解压后运行 `JavaRunCat.exe`。

> 无需安装 Java，内嵌 JRE。

### 方式二：从源码构建 EXE

```powershell
# 前置：JDK 17+，Maven 3.6+

# 一键构建（生成 dist/JavaRunCat/JavaRunCat.exe）
.\build-exe.ps1

# 如需生成 MSI 安装包（需安装 WiX Toolset）
.\build-installer.ps1
```

### 方式三：仅构建 JAR

```bash
mvn clean package
java -jar target/java-runcat-1.0.0.jar
```

### 右键菜单一览

```
📊 System Dashboard          ← 左键点击也可打开
─────────────────────
🎬 Animation
    🐱 Cat ✓
    🐕 Dog
    🐴 Horse
    🦜 Parrot
    ──────────
    📦 Custom Animation...
⚡ Speed
    Slow / Normal / Fast
🌍 Language
    简体中文 / 繁體中文 / English / 日本語
─────────────────────
⚙ Settings
    ☑ Auto Start
    ☑ Show CPU Usage
    ☑ Show Memory Usage
    ☑ CPU High Alert
    🎨 Icon Theme (Light/Dark)
─────────────────────
ℹ About
❌ Exit
```

---

## 🛠️ 技术栈

| 层级 | 技术 | 用途 |
|------|------|------|
| 语言 | Java 17 | 核心开发，使用 Swing AWT 构建系统托盘 UI |
| 构建 | Maven + shade plugin | 依赖管理 + 打 fat JAR |
| 打包 | jpackage (JDK 21) | 生成原生 EXE，内嵌 JRE |
| 图标 | Java 2D API | 程序化生成应用图标 + 动画帧 |
| 配置 | Gson | JSON 格式配置持久化 |
| 监控 | com.sun.management.OperatingSystemMXBean | CPU / 内存实时采集 |
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
│   │   └── SystemMonitor.java         # CPU/内存监控 + 历史记录 + 告警
│   ├── i18n/
│   │   └── I18nManager.java           # 多语言管理
│   ├── animation/
│   │   └── AnimationManager.java      # 动画帧加载 + 自定义动画导入
│   ├── ui/
│   │   ├── TrayIconManager.java       # 系统托盘 + 右键菜单
│   │   ├── DashboardWindow.java       # 左键仪表盘 + 折线图
│   │   └── CustomAnimationDialog.java # 自定义动画导入对话框
│   └── util/
│       ├── AutoStartManager.java      # Windows 开机自启
│       ├── AnimationGenerator.java    # 生成内置动画帧 PNG
│       ├── IconGenerator.java         # 生成应用图标 PNG
│       └── IcoGenerator.java         # PNG → ICO 转换
├── src/main/resources/
│   ├── animations/                    # 内置动画帧 (cat/dog/horse/parrot)
│   ├── i18n/                          # 4 语言 + 默认 properties
│   └── icons/                         # 应用图标 PNG + ICO
├── build-exe.ps1                      # 一键构建 EXE 脚本
├── build-installer.ps1                # 构建 MSI 安装包脚本
├── pom.xml                            # Maven 配置
├── LICENSE                            # MIT 许可证
└── README.md
```

---

## 📦 构建产物

| 产物 | 路径 | 大小 |
|------|------|------|
| Fat JAR | `target/java-runcat-1.0.0.jar` | ~350 KB |
| 原生 EXE | `dist/JavaRunCat/JavaRunCat.exe` | ~460 KB |
| 完整发布包 | `dist/JavaRunCat/` (含 JRE) | ~150 MB |

---

## 🤝 致谢

- [RunCat365](https://github.com/Kyome22/RunCat365) — 原始概念与灵感来源，C# / WPF 实现
- [Gson](https://github.com/google/gson) — Google JSON 库

---

## 📄 许可证

[MIT License](LICENSE) — 自由使用、修改、分发。
