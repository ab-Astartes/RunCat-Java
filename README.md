# Java RunCat

Windows 任务栏上的动态桌宠与轻量系统监控工具，基于 Java 17 / Swing / AWT 实现。

项目灵感来自 [RunCat365](https://github.com/Kyome22/RunCat365)，当前版本在原始"任务栏跑动宠物"的基础上，补充了桌宠交互、系统仪表盘、进程占用榜、自定义动画管理、多语言支持和自动更新。

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
- 四组进程占用榜：
  - `Top CPU`
  - `Top Memory`
  - `Top Disk`
  - `Top Network`
- 仪表盘支持缩放、折叠、隐藏到托盘
- 适合快速定位"当前哪个程序最吃资源"

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
- 图标主题切换（自动 / 浅色 / 深色）
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

### 6. 自动更新

- 启动时静默检查 GitHub Releases 最新版本
- 右键菜单"检查更新"手动触发
- 发现新版本后弹窗确认，一键下载安装
- 更新流程：下载 zip → 解压 → 生成 PowerShell 更新脚本 → 替换 `exe` + `app/` + `runtime/`（如有变更） → 自动重启
- 只替换必要文件，JRE 不变时不重新复制 runtime，减少更新体积
- 更新期间显示进度对话框（下载百分比、解压/安装/重启阶段）
- 保留用户配置（`.java-runcat` 目录不被删除）

## 项目结构

```text
src/main/java/com/runcat/
  RunCatApp.java          # 入口、单实例锁、生命周期
  animation/              # 动画播放与帧管理
  config/                 # AppConfig 用户配置持久化
  core/
    SystemMonitor.java    # 系统总量采集与历史维护
    ProcessMetricsCollector.java  # 进程排行采集
    UpdateChecker.java    # GitHub Releases 自动更新
  i18n/                   # 多语言资源管理
  ui/
    TrayIconManager.java  # 托盘菜单与 tooltip
    DesktopPetWindow.java # 桌宠交互
    DashboardWindow.java  # 仪表盘与进程榜
    CustomAnimationDialog.java  # 自定义动画导入管理
  util/
    AnimationGenerator.java  # 内置动画帧生成
    IconGenerator.java    # 应用图标生成

src/main/resources/
  animations/             # 16x16 托盘动画帧
  animations_hd/          # 64x64 桌宠动画帧
  i18n/                   # 多语言 properties
  icons/                  # 应用图标
```

## 核心模块

- `RunCatApp`
  - 应用入口
  - 单实例锁
  - `--silent` 启动参数（更新后静默重启）
  - 后台监控与更新线程启动

- `SystemMonitor`
  - 系统总量采集
  - 监控历史维护
  - 统一 `MonitorSnapshot` 输出

- `ProcessMetricsCollector`
  - 进程级 CPU / 内存 / 磁盘 / 网络近似排行采集
  - PowerShell `-File` 临时脚本策略，避免 `-Command` 传参转义问题

- `UpdateChecker`
  - GitHub Releases API 版本比较
  - zip 下载与解压
  - PowerShell 更新脚本生成（等待进程退出 → 替换文件 → 重启 → 清理临时文件）
  - AtomicBoolean 防止并发更新

- `AnimationManager`
  - 内置与自定义动画加载
  - 桌宠 / 托盘独立播放对象创建
  - 双分辨率帧缓存（16x16 + 64x64）

- `TrayIconManager`
  - 托盘菜单（JPopupMenu + JWindow invoker，避免 AWT GBK 乱码）
  - 菜单空白处自动关闭（三层机制：PopupMenuListener + WindowDeactivation + MouseInfo 轮询）
  - tooltip 动态更新

- `DesktopPetWindow`
  - 桌宠交互与动画状态切换
  - 拖拽、右键菜单、阴影效果

- `DashboardWindow`
  - 系统总览、历史折线图、Top 进程榜
  - 缩放 / 折叠 / 隐藏控件

- `CustomAnimationDialog`
  - 自定义动画导入与管理

## 构建

### 仅构建 JAR

```bash
mvn clean package
java -jar target/java-runcat-1.2.0.jar
```

### 构建 EXE（独立运行，无需 JDK）

```powershell
.\build-exe.ps1
```

输出：`dist/JavaRunCat/JavaRunCat.exe`（约 148 MB，含嵌入 JRE）

### 构建 MSI 安装包

```powershell
.\build-installer.ps1
```

## 发布新版本

1. 更新 `UpdateChecker.CURRENT_VERSION` 和 `pom.xml` 版本号
2. 更新 `build-exe.ps1` 和 `build-release.yml` 中的 `$VERSION`
3. 提交代码并推送
4. 创建 tag：`git tag v1.3.0 && git push origin v1.3.0`
5. GitHub Actions 自动构建并发布 Release（包含 jar 和 `JavaRunCat-Windows-x64.zip`）
6. 用户端自动检测到新版本并提示更新

## 运行要求

- Windows 10/11
- JDK 17+（开发时）
- Maven 3.6+（构建时）

如果要打包 EXE，建议使用带 `jpackage` 的 JDK 17+。

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
