# 单词记忆（dancijiyi）

> 面向英语学习者的离线优先单词记忆 App：用间隔重复把「记不住」变成「忘不掉」，并配套桌面小组件随时复习。

## 文档

- [需求文档：用户角色、功能清单、页面结构](docs/requirements.md)
- [数据模型说明：Word、Room 表结构、DAO 契约](docs/data-model.md)

## 功能清单

### 已搭建（v0.1 骨架）

- [x] `app` 主应用模块：单 Activity + Material 3 主题 + 空白占位首页
- [x] `widget` P1 桌面小组件模块：示例单词卡片（AppWidgetProvider + RemoteViews）
- [x] Word 数据实体：单词、释义、添加时间、复习时间、记忆阶段、是否掌握
- [x] Room 实体、`WordDao`、`AppDatabase` 与仓库层
- [x] Repository + ViewModel + Coroutines/Flow 的 MVVM 数据链路
- [x] Room schema 导出：`app/schemas/com.dancijiyi.data.AppDatabase/1.json`
- [x] 需求文档、数据模型说明与页面结构注释

### 规划中

- [ ] 单词本管理：新增、编辑、删除、搜索、标签分类
- [ ] 间隔重复复习：基于 SM-2 / 艾宾浩斯遗忘曲线的复习队列
- [ ] 每日学习：今日新词、待复习词、拼写测验与选择题
- [ ] 学习统计：连续打卡、掌握度、复习热力图
- [ ] 桌面小组件：展示真实「今日单词」与待复习数量，支持点击跳转
- [ ] 数据能力：导入/导出、备份恢复、深色模式与无障碍适配

## 数据模型（Word）

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | `Long` | 主键，自增 |
| `word` | `String` | 单词，大小写不敏感唯一 |
| `meaning` | `String` | 释义 |
| `addedAt` | `Long` | 添加时间，epoch millis |
| `reviewAt` | `Long` | 下次复习时间，epoch millis |
| `memoryStage` | `Int` | 记忆阶段，0..5 |
| `mastered` | `Boolean` | 是否掌握；true 不进入复习队列 |

表名 `words`，索引：`word`（唯一）、`mastered + reviewAt`（到期查询）、`addedAt`（首页排序）。详见 [数据模型说明](docs/data-model.md)。

## 技术栈

| 类别 | 选型 |
| --- | --- |
| 语言 | Kotlin 2.2.10 |
| 构建 | Gradle 9.6.0 + Android Gradle Plugin 9.5.1（Kotlin DSL） |
| UI | Android View + Material 3 + ViewBinding，单 Activity 架构 |
| Jetpack | ViewModel、Lifecycle、Room、KSP、AppCompat |
| 异步 | Kotlin Coroutines + Flow / StateFlow |
| 持久化 | Room 2.7.2（`room-runtime` / `room-ktx` / `room-compiler`） |
| 桌面小组件 | 原生 AppWidgetProvider + RemoteViews（`widget` 模块） |
| 兼容性 | minSdk 26，targetSdk / compileSdk 37 |

架构采用 **MVVM + Repository**：UI 只依赖 ViewModel，ViewModel 通过 Repository 访问 Room，数据以 `Flow` 暴露；`widget` 作为独立 library 模块，后续通过 ContentProvider / DataStore 与主应用共享今日单词，避免直接耦合 Room 实现。

## 模块结构

```text
dancijiyi/
├── app/                         # 单词记忆主 App
│   ├── src/main/java/com/dancijiyi/
│   │   ├── MainActivity.kt      # 空白占位首页
│   │   ├── data/                # Room 实体、DAO、数据库、Repository
│   │   └── ui/                  # WordViewModel
│   ├── src/main/res/            # 主题、布局、图标
│   └── schemas/                 # Room schema 导出
├── widget/                      # P1 桌面小组件伴生模块（Android Library）
│   └── src/main/
│       ├── java/.../WordWidgetProvider.kt
│       ├── res/layout/widget_word.xml
│       └── res/xml/word_widget_info.xml
├── docs/
│   ├── requirements.md          # 需求文档与页面结构注释
│   └── data-model.md            # 数据模型与 Room 设计说明
├── build.gradle.kts
├── settings.gradle.kts
└── gradle.properties
```

## 构建与运行

环境要求：

- Android Studio（建议最新稳定版）
- JDK 17 及以上（本项目在 JDK 25 下完成编译验证）
- Android SDK Platform 37、Build-Tools 36.0.0

```bash
# 1. 配置 SDK 路径（仅本地生效，不提交）
#    local.properties
#    sdk.dir=/path/to/Android/Sdk
#    Windows 示例：sdk.dir=D\:\\Program\\Android\\SDK

# 2. 编译 Debug APK（首次构建会下载依赖）
./gradlew :app:assembleDebug
# Windows
gradlew.bat :app:assembleDebug

# 3. 安装到设备/模拟器
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

启动后进入「单词记忆」占位首页。添加桌面小组件：长按桌面空白处 → 小组件 → 找到「单词记忆 · 今日单词」。

## 仓库

- GitHub：<https://github.com/mxmxbkbk/dancijiyi>
- 初始提交：`feat: 初始化项目仓库，搭建app与widget模块骨架`
- 数据模型提交：`feat: 设计单词数据模型，搭建Room数据库基础结构，补充需求文档`
