# Java RunCat

A cute running cat animation on your Windows taskbar — **Java Enhanced Edition**  
Based on [RunCat365](https://github.com/runcat-dev/RunCat365), with multi-language support, custom animations, and auto-start.

[![Java](https://img.shields.io/badge/Java-17+-orange.svg)](https://www.java.com/)
[![Build](https://img.shields.io/badge/build-maven-blue.svg)](https://maven.apache.org/)
[![License](https://img.shields.io/badge/license-MIT-green.svg)](LICENSE)

---

## ✨ Features

- 🐱 **Running pet animation** in Windows system tray, speed varies with CPU usage
- 🌍 **Multi-language support**: 简体中文 · 繁體中文 · English · 日本語
- 🎨 **Custom animations**: Import your own PNG image sequences
- 🔄 **Built-in animations**: Cat (default), Dog, Horse, Parrot
- 🚀 **Auto-start**: Option to start with Windows
- ⚙️ **Configurable**: Speed, theme, tooltip display
- 💾 **Persistent config**: Settings saved between runs

## 📦 Requirements

- Windows 10 / 11
- Java 17 or higher

## 🚀 Quick Start

### Download

Download the latest `java-runcat-1.0.0.jar` from the [Releases](https://github.com/xxx/java-runcat/releases) page.

### Run

```bash
java -jar java-runcat-1.0.0.jar
```

> **Note:** The app runs in the system tray. Right-click the tray icon to access settings and the exit option.

## 🛠️ Build from Source

```bash
git clone https://github.com/xxx/java-runcat.git
cd java-runcat
mvn clean package
```

The built jar will be at `target/java-runcat-1.0.0.jar`.

## 🎮 Usage

| Action | Description |
|--------|-------------|
| Right-click tray icon | Open context menu |
| Animation → ... | Switch between built-in / custom animations |
| Animation → Custom Animation... | Import your own PNG sequence |
| Language → ... | Switch UI language |
| Settings → Auto Start | Enable/disable Windows startup |
| Settings → Show CPU/Memory | Toggle tooltip info |
| About | Show version & GitHub link |
| Exit | Quit the application |

### Custom Animation Format

Create a folder with sequentially named PNG files (e.g., `frame_0.png`, `frame_1.png`, ...) and import via the "Custom Animation" dialog.

## 📁 Configuration

Config file location: `%USERPROFILE%\.java-runcat\config.json`

```json
{
  "language": "zh_CN",
  "currentAnimation": "cat",
  "autoStart": false,
  "showCpuTooltip": true,
  "showMemoryTooltip": true,
  "speedMultiplier": 1.0,
  "iconTheme": "dark"
}
```

## 🌍 Supported Languages

| Code | Language |
|------|----------|
| `zh_CN` | 简体中文 (Simplified Chinese) |
| `zh_TW` | 繁體中文 (Traditional Chinese) |
| `en` | English |
| `ja` | 日本語 (Japanese) |

## 📸 Screenshots

> *Screenshots will be added once the app is running with real icons.*

## 🔗 Based On

This project is an enhanced Java port of [RunCat365](https://github.com/runcat-dev/RunCat365) by [@runcat-dev](https://github.com/runcat-dev).

## 📄 License

[MIT License](LICENSE)

---

Made with ❤️ by the Java RunCat Team
