# Incense Terminal（香终端）

<div align="right">

[![English](https://img.shields.io/badge/English-blue?style=for-the-badge)](README.md)
[![简体中文](https://img.shields.io/badge/简体中文-red?style=for-the-badge)](README_CN.md)

</div>

> Burn time, not notifications.（燃烧时间，而非发送提醒。）

一款面向程序员和极客的终端风格番茄钟。

每次专注不是种树、不是填圈、不是转沙漏，而是点燃一支香。

基于 Jetpack Compose、ASCII 动画，贯彻终端优先设计理念。

---

## 预览

```text
$ incense start
session : #183
today   : 155m
streak  : 12

        (
       ) )
      ( (

        *

        │
        │
        │

remaining
24:59

░░░ pause ░░░  ░░░ reset ░░░
```

---

## 三屏

| 页面 | 功能 |
|------|------|
| **Home（首页）** | ASCII 香可视化、倒计时、ANSI 按钮、Config 配置弹窗 |
| **Workloads（任务）** | 任务表格（名称 / 时长 / 今日 / 编辑），增删改弹窗 |
| **History（历史）** | GitHub 热力图（□ ░ ▒ ▓ █），三个月视图，每日详情 |

底部导航：

```
[ ~ ]  [ # workloads ]  [ ... history ]
```

---

## 功能

### 终端 UI

- 等宽字体（JetBrains Mono / Fira Code / Cascadia Code）
- ANSI 风格配色，`#0D1117` 暗色背景
- 无 Material 卡片、无圆角、无阴影
- 按钮全部 ASCII 格式（`░░░ Start ░░░`）

### 香的状态机

```
Idle ──[点香]──> Burning ──[100%]──> Completed ──[重置]──> Idle
                   │  ▲
                 [暂停] [继续]
                   ▼  │
                 Paused
```

- 可配置时长（通过 workload 默认值或 Config 覆盖）
- 可配置香的长度（3~24，自动测量上限，持久化记忆）
- 烟雾动画（3 帧，500ms 切换）
- 燃点闪烁（● → • → *，300ms 切换）
- 完成仪式（4 帧 ASCII 动画 + 动态总结）

### 任务（Workloads）

```
$ workload                                      [ + new ]

Name               Duration  Today  Edit
====               ========  =====  ====
> algorithm_design  60m       2      ···
  math_analysis     45m       1      ···
  computer_network  30m       3      ···
```

- 终端风格弹窗增/删/改
- 上次选中的任务重启后自动恢复
- Config 支持 `#DEFAULT` 选项：使用任务默认时长

### 历史记录

- 热力图 3 个月同屏：当前月 ± 1 月
- 5 级强度：□ (0m) ░ (1–30m) ▒ (31–60m) ▓ (61–120m) █ (121m+)
- 横轴 Mon–Sun，网格严格对齐
- 点击非未来日期弹出 `== yyyy-MM-dd ==` 详情卡

### 统计

- 当前是第几个 session（已完成数 + 1）
- 今日 / 本周专注分钟数（UTC 基准）
- 连续专注天数（streak）
- 每个 workload 今日完成次数

---

## 架构

```
UI (Compose)
  ↓
ViewModel (AndroidViewModel, StateFlow)
  ↓
Repository（原生 SQL 查询）
  ↓
SQLiteOpenHelper (AppDatabase)
```

### 包结构

```
com.rinko.incenseterminal/
├── IncenseTerminalApp.kt      # Application 持有 DB、repos、currentWorkload StateFlow
├── MainActivity.kt
├── core/
│   ├── engine/    # ViewModel + 动画器（计时、烟雾、仪式）
│   ├── model/     # IncenseState, BurnPhase, EmberPhase, SmokePhase
│   ├── renderer/  # IncenseRenderer, SmokeFrames, CeremonyFrames
│   └── repo/      # WorkloadRepository, FocusSessionRepository
├── data/
│   └── AppDatabase.kt         # SQLiteOpenHelper 子类
└── ui/
    ├── screen/    # IncenseScreen, WorkloadScreen, HistoryScreen, MainScreen
    └── theme/     # IncenseColors, Theme, Typography
```

---

## 技术栈

| 类别 | 选型 |
|------|------|
| 语言 | Kotlin 2.0.21 |
| UI | Jetpack Compose (BOM 2024.09.00) |
| 构建 | AGP 9.2.1 |
| 数据库 | 原生 `SQLiteOpenHelper`（无 Room、无 KSP/kapt） |
| 状态管理 | `StateFlow` + `MutableStateFlow` |
| 最低 SDK | 24 |
| 目标 SDK | 35 |

---

## 设计哲学

```
不种树。
不给币。
不升级。
不养电子宠物。
不搞社交动态。

只需：

专注。
燃烧。
重复。
```

---

## 构建

```bash
git clone git@github.com:Rinko2333/incense-terminal.git
cd incense-terminal
```

用 Android Studio 打开，同步 Gradle，运行即可。

---

## 路线图

### 已完成

- [x] 终端风格 UI + ASCII 香可视化
- [x] 状态机（空闲 / 燃烧 / 暂停 / 完成）
- [x] 烟雾 + 燃点动画
- [x] 任务增删改 + 持久化
- [x] 三个月热力图历史
- [x] 连续天数统计
- [x] 香长度动态计算（首启动自动测量）
- [x] 完成仪式

### 计划中

- [ ] 多终端主题（绿屏 CRT / 琥珀屏 CRT）
- [ ] 导出专注数据
- [ ] 桌面小组件
- [ ] 桌面版（Compose Multiplatform）

---

## 许可证

MIT

---

## 灵感来源

- Unix 终端 & ANSI 艺术
- GitHub 贡献图
- 传统焚香计时
- Cal Newport《深度工作》
