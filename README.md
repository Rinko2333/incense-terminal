# Incense Terminal

<div align="right">

[![English](https://img.shields.io/badge/English-blue?style=for-the-badge)](README.md)
[![简体中文](https://img.shields.io/badge/简体中文-red?style=for-the-badge)](README_CN.md)

</div>

> Burn time.

A terminal-inspired focus timer for programmers and hackers.

Each focus session is lighting an electronic incense stick. No trees, no coins, no gamification — just you and time.

Built with Jetpack Compose, ASCII animation, and a terminal-first design philosophy.

---

## Preview

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

## Screens

| Screen | Description |
|--------|-------------|
| **Home** | ASCII incense visualization, countdown timer, ANSI buttons, Config dialog |
| **Workloads** | CRUD table with Name / Duration / Today / Edit columns |
| **History** | GitHub-style heatmap (░ ▒ ▓ █), 3-month view, daily detail popup |

Navigation via terminal-style tab bar:

```
[ ~ ]  [ # workloads ]  [ ... history ]
```

---

## Features

### Terminal UI

- Monospace typography (JetBrains Mono / Fira Code / Cascadia Code)
- ANSI-inspired color palette on `#0D1117` background
- No Material cards, no rounded corners, no shadows
- ASCII-only buttons (`░░░ Start ░░░`)

### Incense State Machine

```
Idle ──[light]──> Burning ──[100%]──> Completed ──[reset]──> Idle
                    │  ▲
                   [pause] [resume]
                    ▼  │
                  Paused
```

- Configurable duration (overridden via workload or Config dialog)
- Configurable stick count (3–24 dynamic, persisted)
- Animated smoke (3 frames, 500ms)
- Flickering ember (● → • → *, 300ms)
- Completion ceremony (4 ASCII frames + summary)

### Workloads

```
$ workload                        [ + new ]

Name               Duration  Today  Edit
====               ========  =====  ====
> algorithm_design  60m       2      ···
  math_analysis     45m       1      ···
  computer_network  30m       3      ···
```

- Add / edit / delete via terminal-style dialogs
- Last selected workload persisted across restarts
- Config supports `#DEFAULT` duration from workload

### Burn History

- 3-month heatmap: selected month ± 1
- 5 intensity levels: □ (>0) ░ (>30m) ▒ (>60m) ▓ (>90m) █ (>120m)
- Mon–Sun header, aligned grid
- Tap any non-future day for `== yyyy-MM-dd ==` detail card

### Statistics

- Session number (next session = completed count + 1)
- Today / week focus minutes (UTC-based)
- Consecutive streak days
- Per-workload today counts

---

## Architecture

```
UI (Compose)
  ↓
ViewModel (AndroidViewModel, StateFlow)
  ↓
Repository (raw SQL queries)
  ↓
SQLiteOpenHelper (AppDatabase)
```

### Package Structure

```
com.rinko.incenseterminal/
├── IncenseTerminalApp.kt
├── MainActivity.kt
├── core/
│   ├── engine/    (ViewModel + animators)
│   ├── model/     (IncenseState, BurnPhase, EmberPhase, SmokePhase)
│   ├── renderer/  (IncenseRenderer, SmokeFrames, CeremonyFrames)
│   └── repo/      (WorkloadRepository, FocusSessionRepository)
├── data/
│   └── AppDatabase.kt
└── ui/
    ├── screen/    (IncenseScreen, WorkloadScreen, HistoryScreen, MainScreen)
    └── theme/     (IncenseColors, Theme, Typography)
```

---

## Tech Stack

| Category | Choice |
|----------|--------|
| Language | Kotlin 2.0.21 |
| UI | Jetpack Compose (BOM 2024.09.00) |
| Build | AGP 9.2.1 |
| Database | Native `SQLiteOpenHelper` (no Room, no KSP/kapt) |
| State | `StateFlow` + `MutableStateFlow` |
| Min SDK | 24 |
| Target SDK | 35 |

---

## Philosophy

```
No trees.
No coins.
No levels.
No virtual pets.
No social feeds.

Just:

Focus.
Burn.
Repeat.
```

---

## Building

```bash
git clone git@github.com:Rinko2333/incense-terminal.git
cd incense-terminal
```

Open in Android Studio, sync Gradle, and run.

---

## Roadmap

### Done

- [x] Terminal UI with ASCII incense
- [x] State machine (Idle / Burning / Paused / Completed)
- [x] Smoke + ember animation
- [x] Workload CRUD with persistence
- [x] 3-month heatmap history
- [x] Streak tracking
- [x] Dynamic stick count (auto-measured)
- [x] Completion ceremony

### Planned

- [ ] Multiple terminal themes (green/amber CRT)
- [ ] Export study statistics
- [ ] Widget support
- [ ] Desktop version (Compose Multiplatform)

---

## License

MIT

---

## Inspiration

- Unix terminals & ANSI art
- GitHub contribution graphs
- Traditional incense burning
- Cal Newport's Deep Work

If you have ideas for UI or features, pull requests are welcome.
