# Incense Terminal

## Product Vision

Incense Terminal 是一个面向程序员和极客用户的番茄钟应用。

核心理念：

Burn Time, Not Notifications.

用户不是在启动一个计时器，而是在点燃一支香。

每一次专注 Session 都是一支正在燃烧的香。

应用整体采用：

* Terminal UI
* ANSI 风格
* Monospace Typography
* ASCII Animation
* 极简交互
* 东方禅意主题

避免：

* Material 风格大卡片
* 复杂图标
* 拟物光影
* 玻璃拟态
* 高频动画

目标关键词：

Zen
Terminal
Focus
Minimal
Hacker
Calm

# Build System

- AGP 9.2.1, Kotlin 2.0.21, Compose BOM 2024.09.00
- 无 KSP / kapt（AGP 9.x 内置 Kotlin 不兼容）
- 无 Room（改用原生 android.database.sqlite.SQLiteOpenHelper）

# Package Structure

```
com.rinko.incenseterminal/
├── IncenseTerminalApp.kt     // Application，持有 DB singleton、repos、currentWorkload StateFlow
├── MainActivity.kt            // setContent { IncenseTerminalTheme { MainScreen() } }
├── core/
│   ├── engine/
│   │   ├── IncenseViewModel.kt    // AndroidViewModel，计时状态机 + session 记录
│   │   ├── WorkloadViewModel.kt   // AndroidViewModel，workload CRUD
│   │   ├── HistoryViewModel.kt    // AndroidViewModel，热力图数据 + 月份导航
│   │   ├── SmokeAnimator.kt       // 烟雾 Frame 定时切换
│   │   └── CeremonyPlayer.kt      // Session 完成后的 ASCII 动画
│   ├── model/
│   │   ├── IncenseState.kt        // data class（burnPhase, totalSticks, sessionNumber, streakDays...）
│   │   ├── IncenseConfig.kt       // data class（durationSeconds, length）
│   │   ├── BurnPhase.kt           // sealed class（Idle, Burning, Paused, Completed）
│   │   ├── EmberPhase.kt          // enum（DOT, HOLLOW, STAR）含 cycleFrom 逻辑
│   │   └── SmokePhase.kt          // enum（A, B, C）
│   ├── renderer/
│   │   ├── IncenseRenderer.kt     // State → AnnotatedString（坐标网格 + ANSI 着色）
│   │   ├── SmokeFrames.kt         // 预定义烟雾字符帧
│   │   └── CeremonyFrames.kt      // 预定义完成仪式 ASCII 帧
│   └── repo/
│       ├── WorkloadRepository.kt      // SQLite 直查 workload 表（getAll, insert, delete, updateDuration）
│       └── FocusSessionRepository.kt  // SQLite 直查 focus_session 表（record, getHeatmapData, getStreakDays 等聚合）
└── data/
    └── AppDatabase.kt               // SQLiteOpenHelper 子类 + 内联 data class（WorkloadRow, FocusSessionRow, DailySummary, HeatmapDay）
```

# Navigation

枚举 `Screen { HOME, WORKLOAD, HISTORY }`，MainScreen 内切换。

```
[ ~ ]  [ # workloads ]  [ ... history ]
```

- **HOME** → IncenseContent（计时主界面）
- **WORKLOAD** → WorkloadScreen（workload 列表 CRUD）
- **HISTORY** → HistoryScreen（热力图 + 日期详情弹窗）

# Data Layer

## 数据库

路径：`/data/data/com.rinko.incenseterminal/databases/incense-terminal.db`
版本：1

### workload 表

| 列名 | 类型 |
|------|------|
| id | INTEGER PK AUTOINCREMENT |
| name | TEXT NOT NULL |
| default_duration_minutes | INTEGER NOT NULL |

### focus_session 表

| 列名 | 类型 |
|------|------|
| id | INTEGER PK AUTOINCREMENT |
| workload_name | TEXT NOT NULL |
| duration_minutes | INTEGER NOT NULL |
| start_timestamp | INTEGER NOT NULL |
| end_timestamp | INTEGER NOT NULL |
| completed | INTEGER NOT NULL (0/1) |

## Repositories

- **WorkloadRepository**：纯 SQL（query insert delete update），不返回 Flow — ViewModel 手动 refresh
- **FocusSessionRepository**：record 写入 + getSessionsForDay / getDailySummary / getHeatmapData / getCompletedSessionCount / getFocusMinutesForRange / getStreakDays 聚合查询

## App State

`IncenseTerminalApp`（Application 子类）持有：

- `database: AppDatabase`（lazy singleton）
- `workloadRepo: WorkloadRepository`
- `sessionRepo: FocusSessionRepository`
- `currentWorkload: StateFlow<WorkloadRow?>`（当前选中的 workload）

# Home Screen Design

## 当前实现

```
$ incense start
session : #001
today   : 0m
streak  : 0

                    (
                   ) )
                  ( (
                    │
                    │
                    │

remaining
24:59

░░░ Start ░░░
```

## Header

左上角 TerminalHeader：

```
$ incense start
session : #${sessionNumber}
today   : ${todayFocusMinutes}m
streak  : ${streakDays}
```

右上角 `< Config >` 按钮（点击打开 ConfigDialog）。

## Config Dialog

两层级联：
1. **MAIN** → "=== config ===" → [Time] [Length] [cancel]
2. **TIME** → 预设选项 (15m / 30m / 45m / 60m) + [Custom] → 自定义输入 `>[     minutes]<` + [save]
3. **LENGTH** → 预设选项 (3 / 5 / 7 / 9 / 12 / 15) → 点击即选

**关键规则**：
- Home Config 只设置 overrideDurationSeconds（覆盖层），不写 DB
- 选中 workload 时清空 override
- `effectiveDurationSeconds()` = override ?? workload.defaultDurationMinutes * 60 ?? 25*60

## IncenseState 聚合字段

- `sessionNumber`（下一个 session 编号）= DB 中已完成 session 数 + 1
- `todayFocusMinutes` = 今日（UTC 0 点起）完成 session 的分钟和
- `weekFocusMinutes` = 近 7 日完成 session 的分钟和
- `streakDays` = 从今日（或昨日，容错）向前回溯的连续活跃天数

在 ViewModel init 时加载，每次 session 完成后 refresh。

## Debug Overlay

右下角浮动 `[ dbg ]` 按钮 → 弹出 debug 面板：
- "10s test" — 快速启动 10 秒 session
- "skip 90%" — 跳到 90% 进度
- "force done" — 立即完成

# Workload Screen

```
$ workload

> algorithm_design    60m    x
  math_analysis       45m    x
  computer_network    30m    x

        [ + new ]
```

- `>` 标记当前选中（Ember 色加亮，不可再点击）
- 未选中的行可点击 → selectWorkload → 自动跳回 HOME
- `x` 删除 workload
- `[ + new ]` → 输入 name + duration（minutes） → `[ add ]`
- 删除当前选中 workload 时自动选中列表第一个

# History Screen

## 热力图设计

```
$ burn log  < 2026-06 >        less □ ░ ▒ ▓ █ more

Mon Tue Wed Thu Fri Sat Sun
    □   □   □   ■   ▓   ▒   □
    □   ░   ■   █   ▓   □   □
    ...

Jun 2026
    □   □   □   ■   ▓   ▒   □
    ░   □   ■   █   □   □   □
    ...

== 2026-06-10 ==
2 sessions, 150m total
algorithm  120m
math       30m
```

**布局规则**：
- 标题行左侧 `$ burn log < yyyy-mm >`，右侧 `less □ ░ ▒ ▓ █ more`（同一 Row）
- **横轴 Mon..Sun**（7 列 × 36dp，header + body 统一左对齐）
- **纵轴三个月**垂直堆叠（prev / selected / next）
- 每月块：月份标签 + 该月所有周行（每行恰 7 格，首日偏移补 null、尾末补 null）
- 点击非 future 单元格 → 弹出详情卡片（`== yyyy-MM-dd ==` 标题，无制表符）
- `< >` 按钮切换月份（±1），跨区间选中自动清除

**字符与颜色**：

| 分钟 | 字符 | 颜色 |
|------|------|------|
| 0 | □ | DimText |
| 1-30 | ░ | Accent |
| 31-60 | ▒ | Success |
| 61-120 | ▓ | Ember |
| 121+ | █ | EmberRed |

future 日：DimText 40% alpha

**详情卡片**：终端风格 Surface（0dp 圆角 + Accent 1dp 边框），显示日期、session 数、总分钟数、各 workload 分钟明细

# Incense State Machine

```
Idle ──[light]──> Burning ──[100%]──> Completed ──[reset(reset)]──> Idle
                    │  ▲
                   [pause] [resume]
                    ▼  │
                  Paused
```

## IncenseViewModel 关键逻辑

- `light(seconds)`：初始化 config + state，启动 smoke + burn timer
- `pause()`：停止 tick，记录 progress，不记录 session
- `resume()`：从 Paused 恢复，继续 tick
- `reset()`：回到 Idle，reset smoke/ceremony/clocks，保留 workloadName
- `recordSession(completed)`：写入 focus_session 表 + refreshAggregateState
- `forceProgress(target)`：debug 用，直接跳转进度

## Aggregate State Refresh

`refreshAggregateState()` 在 init 和每次 session 完成后调用，从 DB 重拉：
- `getCompletedSessionCount()` → sessionNumber = count + 1
- `getFocusMinutesForRange(todayStart, todayEnd)` → todayFocusMinutes
- `getFocusMinutesForRange(weekStart, todayEnd)` → weekFocusMinutes
- `getStreakDays(todayStart)` → streakDays

Streak 容错：今日无 session 但昨日有 → 从昨日开始计（不过早判定断签）。

# Confirmed Implementation Parameters

**Incense Model**：
- `totalSticks` = config.length（默认 9，可配置 3/5/7/9/12/15）
- `remainingSticks` = ((1 - progress) * totalSticks).toInt()
- 香从顶部缩短：Sticks 减少 + 烟雾 + 燃点位置同步

**Smoke Animation**：
- 3 帧 (A/B/C)，每 500ms 切换
- 烟雾字符预定义，非粒子系统

**Ember Animation**：
- DOT(●) → HOLLOW(•) → STAR(*) → HOLLOW → ...
- 300ms 切换
- 颜色：Ember / EmberYellow / EmberRed 循环

**Timer Tick**：100ms 间隔，更新 progress → 重新计算 remainingSeconds/remainingSticks

**Semantic over temporal**：`config.durationSeconds` 是完整持续时间，progress ∈ [0, 1]，remaining = (1 - progress) * total

**Config length** 是香的总段数（visual），与 duration 解耦

# Visual Style Guide

## Typography

全部等宽：`FontFamily.Monospace`（JetBrains Mono > Fira Code > Cascadia Code 回退）

## Color Palette（IncenseColors.kt）

```kotlin
Background  = 0xFF0D1117
PrimaryText = 0xFFC9D1D9
DimText     = 0xFF8B949E
Success     = 0xFF3FB950  // session完成、计时器reset
Warning     = 0xFFD29922
Ember       = 0xFFFF7B72  // 高亮选中
EmberRed    = 0xFFFF4444  // 热力图最高档
EmberYellow = 0xFFFFD700
Smoke       = 0xFFA5D6FF
Accent      = 0xFF58A6FF  // 按钮、链接、选中、边框
```

## ANSI Button 规范

所有按钮使用 `░░░ Label ░░░` 格式：
- Idle: `░░░ Start ░░░`（单按钮）
- Burning: `░░░ Pause ░░░` `░░░ Reset ░░░`（Row）
- Paused: `░░░ Resume ░░░` `░░░ Reset ░░░`（Row）
- Completed: `░░░ Done ░░░`（单按钮，reset）
- 颜色：Accent

## Layout

三层垂直结构：
1. Top Status（header row + config button）
2. Center Incense（ASCII 香 + 倒计时）
3. Bottom Controls（ANSI 按钮）

禁止 Card 容器、圆环进度、Material 预设 spacing — 使用 Monospace 网格对齐。

# Animation Principles

允许：字符变化、颜色变化、轻微闪烁、Frame 切换
禁止：大范围位移、复杂补间、高频重绘
刷新频率：1~4 FPS

目标：像活着的终端，而不是小游戏。

# Compose Architecture

```
StateFlow<IncenseState>
    ↓
IncenseRenderer.render(state) → AnnotatedString (ANSI colored)
    ↓
Text(renderedIncense)
```

所有 ASCII 由 Renderer 输出，禁止 View 层硬编码字符画。

# Product Philosophy

这是一个专注工具。不是游戏、不是收集系统、不是任务管理器。

用户打开它时应该感觉：**我正在点燃一支香**，而不是启动一个计时器。
