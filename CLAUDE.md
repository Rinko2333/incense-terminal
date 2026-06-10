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

# Visual Style Guide

## Typography

Primary Font:

JetBrains Mono

Fallback:

Fira Code
Cascadia Code

全部界面使用等宽字体。

## Color Palette

Background

#0D1117

Primary Text

#C9D1D9

Dim Text

#8B949E

Success

#3FB950

Warning

#D29922

Ember

#FF7B72

Smoke

#A5D6FF

Accent

#58A6FF

## Layout

屏幕分为三个区域：

Top Status

Center Incense

Bottom Controls

示例：

SESSION #183

today: 2h 35m
streak: 12

```
    (
   ) )
  ( (
    │
    │
    │
```

remaining
24:59

[start] [pause]

# ASCII Rendering Engine

禁止使用图片资源。

禁止使用SVG资源。

所有视觉元素由：

Text
AnnotatedString
ANSI Color
Monospace Layout

构成。

# Incense Model

每个 Session 对应一支香。

长度与时间成比例。

25分钟：

│
│
│

50分钟：

│
│
│
│
│
│

90分钟：

│
│
│
│
│
│
│
│
│

# Burn States

State 1

```
    (
   ) )
  ( (
    │
    │
    │
```

State 2

```
   (
  ) )
   │
   │
```

State 3

```
  (
   │
```

State 4

```
  ·
```

State 5

(empty)

香长度根据 Progress 实时缩短。

# Smoke Animation System

不要使用粒子系统。

烟雾由预定义 Frame 构成。

Frame A

(
) )
( (

Frame B

)
( (
) )

Frame C

( (
)
(

每 500ms 切换。

形成轻微漂浮感。

# Ember Animation

燃点位置：

香顶端。

使用字符：

●
•
*

循环变化：

● → • → * → •

颜色：

橙色
红色
黄色

刷新间隔：

300ms

# Timer Visualization

禁止圆环。

禁止进度条。

禁止仪表盘。

时间通过：

香长度
燃点位置
剩余时间数字

共同表达。

# Session Complete

完成后播放 ASCII Ceremony

Frame 1

```
  *
```

Frame 2

```
* * *
```

Frame 3

* ✦  *

Frame 4

focus complete

随后显示：

session complete

+25m focus

streak +1

# Pause State

暂停时：

烟雾停止

燃点熄灭

字符：

x

示例：

```
    x
    │
    │
```

状态文字：

paused

# Home Screen

SESSION #183

today: 2h 35m
week: 11h 20m
streak: 12

[ incense ]

remaining
24:59

[start]

# History Screen

burn log

183  25m  completed
182  25m  completed
181  15m  interrupted
180  50m  completed

# Achievements

100 sessions

first flame

10 hour burn

100 hour burn

7 day streak

30 day streak

全部采用 Terminal 风格文本展示。

# Animation Principles

允许：

字符变化
颜色变化
轻微闪烁
Frame 切换

禁止：

大范围位移
复杂补间
高频重绘

刷新频率：

1~4 FPS

目标：

像活着的终端

而不是小游戏。

# Compose Architecture

UI

StateFlow<AppState>

↓

ASCII Renderer

↓

AnnotatedString

↓

Text()

所有 ASCII 由 Renderer 输出。

禁止在 View 层硬编码字符画。

# Future Expansion

不同香型

Sandalwood
Agarwood
Osmanthus

不同香炉

Wood
Bronze
Jade

不同终端主题

Green CRT
Amber CRT
GitHub Dark
Tokyo Night

主题切换不改变交互逻辑。

# Product Philosophy

这是一个专注工具。

不是游戏。

不是收集系统。

不是任务管理器。

用户打开它时应该感觉：

我正在点燃一支香。

而不是启动一个计时器。
