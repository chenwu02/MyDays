# ⏳ MyDays (倒数日) —— 纯粹的时间记录者

![Android Native](https://img.shields.io/badge/Platform-Android-3DDC84?style=flat-square&logo=android)
![Java](https://img.shields.io/badge/Language-Java-ED8B00?style=flat-square&logo=java)
![Privacy](https://img.shields.io/badge/Privacy-100%25_Local-blue?style=flat-square)
![License](https://img.shields.io/badge/License-MIT-green?style=flat-square)

> “先让它跑起来，再让它美起来。” 

MyDays 是一款为 Android 平台打造的极简倒数日/纪念日应用。
在充斥着开屏广告、过度索取隐私权限和臃肿社交功能的工具软件大环境下，MyDays 回归了“记录时间”的本质。它**完全断网运行**，不收集任何用户数据，并为你提供媲美杂志海报的视觉体验。

## ✨ 核心特性

- 🔒 **绝对的隐私安全 (Zero Network)**
  - 纯本地应用，无需联网，无需注册登录。你的纪念日只保存在你的手机里。
- 🎨 **UI 与海报级排版**
  - 主界面采用前沿的材质卡片。
  - 详情页采用大底图+专业级暗角遮罩（Scrim）沉底排版，沉浸感极强。
- ⏰ **严谨的日期计算引擎**
  - 重写了底层日历逻辑，完美规避时区差、夏令时和跨夜导致的“天数算不准” Bug。
  - 智能区分“正数”（...已过）与“倒数”（...还有），支持每年/每月周期重复。
- 🔔 **可靠的后台通知**
  - 基于 Android 官方推荐的 Jetpack WorkManager 打造。
  - 资源占用极低，支持“提前三天”或“当天”精准推送，完美适配 Android 13+ 权限通知规范。


## 🛠 技术栈

- **语言:** Java
- **UI 架构:** 原生 XML
- **本地存储:** SharedPreferences / Gson
- **后台任务:** Jetpack WorkManager
- **开发工具:** Android Studio (最新版)

## 🚀 快速开始

如果你想在本地编译或修改此项目：

1. 克隆本仓库：
   
```bash
   git clone [https://github.com/your-username/MyDays.git](https://github.com/your-username/MyDays.git)
