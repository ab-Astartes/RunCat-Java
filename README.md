# Java RunCat

Windows 任务栏上的动态桌宠与轻量系统监控工具，基于 Java 17 / Swing / AWT 实现。

项目灵感来自 [RunCat365](https://github.com/Kyome22/RunCat365)，当前版本在原始“任务栏跑动宠物”的基础上，补充了桌宠交互、系统仪表盘、进程占用榜、自定义动画管理和多语言支持。

## 当前特性

### 1. 托盘动画与桌宠

- 托盘图标持续播放动画，速度跟随 CPU 负载变化
- 桌宠支持 `64 / 96 / 128` 三档尺寸
- 支持拖拽、单击弹跳、双击打开仪表盘
- 支持透明度调节与位置记忆
- 托盘与桌宠使用独立动画播放状态，避免跳帧和不同步

### 2. 系统监控仪表盘

- 实时显示 `CPU / 内存 / 磁盘 / 网络`
- 显示最近 60 秒 CPU / 内存历史曲线
- 新增四组进程占用榜：
  - `Top CPU`
  - `Top Memory`
  - `Top Disk`
  - `Top Network`
- 适合快速定位“当前哪个程序最吃资源”

说明：
- 进程网络占用当前为近似值，优先保证趋势和排行可用
- 仪表盘默认刷新频率为 `1s`

### 3. 托盘菜单与设置

- 动画切换
- 速度切换
- 语言切换
- 自动启动
- Tooltip 开关
- CPU 高占用提醒
- 图标主题切换
- `Top N` 进程榜数量设置
- 仪表盘刷新频率设置
- 桌宠点击动作设置
- 动画平滑模式开关

### 4. 自定义动画管理

- 从 PNG 序列目录导入
- 自动按自然顺序排序帧文件
- 导入前校验最少帧数
- 预览首帧
- 支持启用、重命名、删除

### 5. 多语言

- `zh_CN`
- `zh_TW`
- `en`
- `ja`

## 项目结构

```text
src/main/java/com/runcat/
  RunCatApp.java
  animation/
  config/
  core/
  i18n/
  ui/
  util/

src/main/resources/
  animations/
  animations_hd/
  i18n/
  icons/
```

## 核心模块

- `RunCatApp`
  - 应用入口
  - 单实例锁
  - 后台更新线程启动

- `SystemMonitor`
  - 系统总量采集
  - 监控历史维护
  - 统一 `MonitorSnapshot` 输出

- `ProcessMetricsCollector`
  - 进程级 CPU / 内存 / 磁盘 / 网络近似排行采集

- `AnimationManager`
  - 内置与自定义动画加载
  - 桌宠 / 托盘独立播放对象创建

- `TrayIconManager`
  - 托盘菜单与 tooltip

- `DesktopPetWindow`
  - 桌宠交互与动画状态切换

- `DashboardWindow`
  - 系统总览、历史图、Top 进程榜

- `CustomAnimationDialog`
  - 自定义动画导入与管理

## 构建

### 仅构建 JAR

```bash
mvn clean package
java -jar target/java-runcat-1.2.0.jar
```

### 构建 EXE

```powershell
.\build-exe.ps1
```

### 构建 MSI

```powershell
.\build-installer.ps1
```

## 运行要求

- Windows
- JDK 17+
- Maven 3.6+

如果要打包 EXE，建议使用带 `jpackage` 的更高版本 JDK。

## 测试

```bash
mvn test
```

当前测试覆盖：

- 进程排行排序逻辑
- 动画独立播放状态
- 自定义动画自然排序

## 已知限制

- 进程网络占用是近似计算，不是底层 socket 级精确统计
- 某些机器上的 Windows 性能计数器或 PowerShell 输出可能影响磁盘/网络榜的可用性
- 如果某项进程指标不可用，仪表盘会单项降级显示，不会让整个程序失效

## 许可证

[MIT](LICENSE)
