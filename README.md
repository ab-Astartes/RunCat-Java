# 🐱 Java RunCat

A cute running cat animation on your Windows taskbar — Java Enhanced Edition.

Based on the [RunCat365](https://github.com/Kyome22/RunCat365) concept, enhanced with multi-language support, custom animations, and a system dashboard.

![Java 17+](https://img.shields.io/badge/Java-17+-orange)
![Platform](https://img.shields.io/badge/Platform-Windows-blue)
![License](https://img.shields.io/badge/License-MIT-green)

## ✨ Features

- 🐱 **4 Built-in Animations**: Cat, Dog, Horse, Parrot (5 frames each)
- 🌍 **Multi-language**: 简体中文 / 繁體中文 / English / 日本語
- 📊 **System Dashboard**: Left-click tray icon → CPU/memory chart with 60s history
- ⚠️ **CPU Alert**: System notification when CPU exceeds threshold (default 90%)
- 🎨 **Custom Animations**: Import your own PNG sequence
- 🖥️ **Auto Start**: Launch with Windows
- ⚡ **Speed Control**: Slow / Normal / Fast animation speed
- 🔒 **Single Instance**: Prevents duplicate processes
- 🖱️ **Silent Mode**: `--silent` flag for quiet startup
- 🎯 **Native EXE**: jpackage packaging with embedded JRE + custom icon

## 🚀 Quick Start

### Option 1: Run JAR directly
```bash
# Build
mvn clean package

# Run
java -jar target/java-runcat-1.0.0.jar
```

### Option 2: Build as Windows Application (EXE)
```powershell
# One-click build: generates native EXE with embedded JRE
.\build-exe.ps1

# Output: dist\JavaRunCat\JavaRunCat.exe
```

### Option 3: Create Installer (MSI/EXE)
```powershell
# Requires: WiX Toolset (for MSI) 
.\build-installer.ps1
```

### CLI Arguments
```
java-runcat [--silent] [--help]
  --silent, -s   Start without notification
  --help,    -h  Show help
```

## 📊 Dashboard

Left-click the tray icon to open the system dashboard:

- Real-time CPU usage with color-coded indicator
- Memory usage with MB detail
- 60-second history charts for both CPU and memory
- Window position persists between sessions

## ⚠️ CPU Alert

When CPU usage exceeds the configurable threshold (default 90%), a Windows notification is shown. This can be toggled in the right-click menu under **Settings → CPU High Alert**.

## 🎨 Custom Animations

1. Right-click tray icon → **Animation → Custom Animation...**
2. Select a folder containing PNG files (numbered sequence)
3. Name your animation and apply

Custom animations are stored in `~/.java-runcat/animations/`

## 🛠️ Development

### Prerequisites
- JDK 17+ (for jpackage: JDK 17+)
- Maven 3.6+

### Project Structure
```
java-runcat/
├── src/main/java/com/runcat/
│   ├── RunCatApp.java              # Entry point + single instance lock
│   ├── config/AppConfig.java       # Configuration with JSON persistence
│   ├── core/SystemMonitor.java     # CPU/memory monitor with history + alerts
│   ├── i18n/I18nManager.java       # Multi-language support
│   ├── animation/AnimationManager.java  # Animation frame management
│   ├── ui/
│   │   ├── TrayIconManager.java    # System tray + right-click menu
│   │   ├── DashboardWindow.java    # Left-click dashboard with charts
│   │   └── CustomAnimationDialog.java  # Custom animation import
│   └── util/
│       ├── AutoStartManager.java   # Windows auto-start registry
│       ├── AnimationGenerator.java # Generate built-in animation frames
│       ├── IconGenerator.java      # Generate app icon (PNG)
│       └── IcoGenerator.java      # PNG → ICO converter
├── src/main/resources/
│   ├── animations/                  # Built-in animation PNGs
│   ├── i18n/                        # Language properties files
│   └── icons/                       # App icon (PNG + ICO)
├── build-exe.ps1                   # One-click EXE build script
├── build-installer.ps1             # Installer creation script
├── pom.xml                         # Maven build (shade plugin)
└── README.md
```

### Build Commands
```bash
# Compile only
mvn compile

# Build JAR
mvn clean package

# Generate icons + animations
java -cp target/classes com.runcat.util.IconGenerator src/main/resources/icons
java -cp target/classes com.runcat.util.AnimationGenerator

# Build native EXE (jpackage)
jpackage --type app-image --name JavaRunCat --input target \
  --main-jar java-runcat-1.0.0.jar --main-class com.runcat.RunCatApp \
  --app-version 1.0.0 --vendor "JavaRunCat Team" \
  --icon src/main/resources/icons/app-icon.ico --dest dist
```

## 📦 Output

| Format | Path | Size |
|--------|------|------|
| Fat JAR | `target/java-runcat-1.0.0.jar` | ~350 KB |
| Native EXE | `dist/JavaRunCat/JavaRunCat.exe` | ~460 KB |
| Full Package | `dist/JavaRunCat/` | ~150 MB (with JRE) |

## 📄 License

MIT License - see [LICENSE](LICENSE)
