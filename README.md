# 单词记忆（dancijiyi）

> 面向英语学习者的离线优先单词记忆 App：用间隔重复把「记不住」变成「忘不掉」，并配套桌面小组件随时复习。

## 文档

- [需求文档：用户角色、功能清单、页面结构](docs/requirements.md)
- [数据模型说明：Word、Room 表结构、DAO 契约](docs/data-model.md)

## 功能清单

### 已搭建（v0.5）

- [x] 单 Activity + Navigation 导航，首页 / 详情 / 导入 / 复习四页可互相跳转
- [x] 四个页面 UI 骨架：Material 3、ViewBinding、空状态、加载状态
- [x] 单词基础 CRUD：新增、列表查询、详情查看、编辑、删除、手动掌握
- [x] Room 本地持久化：列表走 `observeAll()`，详情走 `observeById()`，写入走 Repository
- [x] 列表筛选：全部 / 待复习 / 已掌握
- [x] 词库导入：本地 txt/csv、UTF-8、权限申请与拒绝降级、协程解析、100 条/批入库、结果弹窗
- [x] 基础异常捕获：数据库异常转换为 Snackbar 提示，不直接崩溃
- [x] 每页独立 ViewModel，协程调用入口与页面状态已接入
- [x] `widget` P1 桌面小组件模块：示例单词卡片（AppWidgetProvider + RemoteViews）
- [x] Word 数据实体、Room schema、需求文档与数据模型说明

### 规划中

- [ ] 搜索、标签分类与批量管理
- [ ] 间隔重复复习：基于 SM-2 / 艾宾浩斯遗忘曲线的复习队列
- [ ] 每日学习：今日新词、待复习词、拼写测验与选择题
- [ ] 学习统计：连续打卡、掌握度、复习热力图
- [ ] 桌面小组件：展示真实「今日单词」与待复习数量，支持点击跳转
- [ ] 数据能力：导出、备份恢复、深色模式与无障碍适配

## 页面导航

```text
首页 · 单词列表（startDestination）
├── 点击单词 ──────> 单词详情（查看 / 编辑 / 删除 / 掌握）
├── 空状态 / 顶栏 ─> 导入词库（选择 txt/csv → 预览 → 批量导入）
└── 空状态 / 顶栏 ─> 复习测试（页面骨架）
```

列表、详情与导入已接入 Room/文件读取：新增、修改、删除、掌握与导入后均通过 `Flow` 自动刷新；复习页面仍只保留 UI 骨架，未实现调度算法。

## 本地持久化与导入

- 数据库：`dancijiyi.db`，表 `words`，Room 版本 1。
- 列表页：`WordRepository.observeAll()` → `WordListViewModel` → `WordListUiState` → RecyclerView。
- 详情页：`observeById(wordId)`，编辑/删除/掌握状态通过 Repository 写回。
- 导入页：系统文件选择器读取 txt/csv，`VocabularyParser` 解析后按 100 条/批写入 Room。
- 文件读取、解析、批量写入分别在 IO / Default / IO 协程执行，不阻塞主线程。
- Android 12 及以下申请 `READ_EXTERNAL_STORAGE`；拒绝时弹窗提示，并可降级到系统文件选择器。Android 13+ 直接使用 SAF。
- 写入失败、唯一索引冲突、导入错误均通过 Snackbar 或导入结果弹窗提示；未知单词显示空状态而不是崩溃。
- 新增/编辑统一走 `WordEditorDialog`，删除前二次确认。

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
| UI | Android View + Material 3 + ViewBinding + Navigation，单 Activity 架构 |
| Jetpack | ViewModel、Lifecycle、Navigation、Room、KSP、AppCompat |
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
│   │   ├── MainActivity.kt      # 单 Activity + NavHost + 统一 AppBar
│   │   ├── data/                # Room、Repository、AppContainer、importer（文件读取/解析）
│   │   └── ui/
│   │       ├── wordlist/        # 列表页、Adapter、筛选、ViewModel
│   │       ├── detail/          # 详情页、编辑/删除/掌握、ViewModel
│   │       ├── importer/        # 文件选择、权限、解析预览、批量导入
│   │       ├── review/          # 复习测试页（骨架）
│   │       └── common/          # ScreenState、格式化、对话框、ViewModelFactory
│   ├── src/main/res/
│   │   ├── navigation/          # nav_graph.xml
│   │   ├── layout/              # 4 个页面、空/加载状态、列表项
│   │   └── values/              # 主题、字符串、颜色
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

启动后进入「单词记忆」首页，点击「添加单词」创建词条，点击列表项进入详情，可编辑、删除或标记掌握；顶栏「导入」可选择 txt/csv 词库并批量写入。数据保存在本地 Room，重启后仍存在。复习页仍为页面骨架。

## 仓库

- GitHub：<https://github.com/mxmxbkbk/dancijiyi>
- 初始提交：`feat: 初始化项目仓库，搭建app与widget模块骨架`
- 数据模型提交：`feat: 设计单词数据模型，搭建Room数据库基础结构，补充需求文档`
- 导航骨架提交：`feat: 搭建主App导航框架，完成4个基础页面UI骨架`
- CRUD 提交：`feat: 实现单词基础CRUD，对接Room持久化存储`
- 导入提交：`feat: 实现csv/txt词库批量导入，权限处理与异步解析`
