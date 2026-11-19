# 数据模型说明：Word 与 Room 数据库

| 项目 | 内容 |
| --- | --- |
| 文档版本 | v0.2 |
| 最后更新 | 2026-11-12 |
| 对应代码 | `app/src/main/java/com/dancijiyi/data/` |
| schema | `app/schemas/com.dancijiyi.data.AppDatabase/1.json` |

## 1. 设计目标

- 用一张 `words` 表承载单词本的核心数据，满足首页、详情、导入、复习四个页面的读写需求。
- 字段命名与需求文档中的「单词、释义、添加时间、复习时间、记忆阶段、是否掌握」一一对应。
- 用数据库索引保证去重与到期查询效率。
- 数据层独立于 UI，后续页面通过 Repository / ViewModel 访问，不直接依赖 DAO。
- v1 即导出 schema，为后续迁移保留基线。

## 2. Word 实体

### 2.1 字段定义

| Kotlin 字段 | 类型 | SQLite 类型 | 可空 | 默认值 | 含义 |
| --- | --- | --- | --- | --- | --- |
| `id` | `Long` | `INTEGER` | 否 | 自增 | 主键，Room `autoGenerate = true` |
| `word` | `String` | `TEXT COLLATE NOCASE` | 否 | 无 | 单词，大小写不敏感唯一 |
| `meaning` | `String` | `TEXT` | 否 | 无 | 释义，可包含多义项 |
| `addedAt` | `Long` | `INTEGER` | 否 | 当前时间 | 添加时间，epoch millis，创建后不变 |
| `reviewAt` | `Long` | `INTEGER` | 否 | 当前时间 | 下次复习时间，epoch millis |
| `memoryStage` | `Int` | `INTEGER` | 否 | `0` | 记忆阶段，取值 0..5 |
| `mastered` | `Boolean` | `INTEGER` | 否 | `false` | 是否掌握；true 时不进入待复习队列 |

> 下表中的「默认值」是 Kotlin 构造参数的默认值，不是 SQLite 的 `DEFAULT` 约束；Room schema 不会生成列级 SQL 默认值，所有写入都由实体携带具体值。

### 2.2 记忆阶段

| 值 | 常量 | 名称 | 含义 | 建议复习间隔（后续算法） |
| --- | --- | --- | --- | --- |
| 0 | `STAGE_NEW` | 新词 | 尚未学习，默认立即到期 | 立即 / 10 分钟 |
| 1 | `STAGE_FAMILIAR` | 初识 | 见过并理解释义 | 1 天 |
| 2 | `STAGE_SHORT_TERM` | 短期 | 能主动回忆 | 3 天 |
| 3 | `STAGE_MID_TERM` | 中期 | 间隔复习后仍能回忆 | 7 天 |
| 4 | `STAGE_LONG_TERM` | 长期 | 较长时间未复习仍能回忆 | 15 天 |
| 5 | `STAGE_MASTERED` | 熟练 | 接近掌握 | 30 天 |

说明：

- 间隔为产品建议值，真正的调度算法（SM-2 / 艾宾浩斯变体）在后续版本实现。
- `memoryStage` 只表示记忆深度，不等于 `mastered`。阶段 5 可以由用户或策略进一步标记为掌握。
- 数据库不添加 `CHECK` 约束，业务层在写入前将阶段限制在 `STAGE_RANGE` 内。

### 2.3 掌握状态

- `mastered = true`：用户已掌握，`observeDue` 不会返回该词。
- `mastered = false`：未掌握，只要 `reviewAt <= now` 就进入待复习队列。
- 取消掌握后，单词重新参与到期判断；`reviewAt` 可保留上次值，由业务层决定是否重置为当前时间。

### 2.4 时间语义

- 所有时间字段统一使用 **epoch milliseconds（UTC）**。
- UI 展示时转换为设备本地时区，不在数据库中存字符串时间。
- `addedAt` 是创建时间，`update(word)` 不应修改它。
- `reviewAt` 是「下一次复习时间」，不是「上次复习时间」。
- 新词默认 `reviewAt = addedAt = 当前时间`，因此创建后立即可复习。

## 3. 表结构与索引

### 3.1 表名与 DDL

表名：`words`。Room 导出的等价 DDL：

```sql
CREATE TABLE IF NOT EXISTS `words` (
    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    `word` TEXT NOT NULL COLLATE NOCASE,
    `meaning` TEXT NOT NULL,
    `addedAt` INTEGER NOT NULL,
    `reviewAt` INTEGER NOT NULL,
    `memoryStage` INTEGER NOT NULL,
    `mastered` INTEGER NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS `index_words_word`
    ON `words` (`word`);

CREATE INDEX IF NOT EXISTS `index_words_mastered_reviewAt`
    ON `words` (`mastered`, `reviewAt`);

CREATE INDEX IF NOT EXISTS `index_words_addedAt`
    ON `words` (`addedAt`);
```

### 3.2 索引设计

| 索引 | 字段 | 类型 | 用途 |
| --- | --- | --- | --- |
| `index_words_word` | `word` | 唯一 | 防止重复单词；配合 `NOCASE` 实现大小写不敏感去重 |
| `index_words_mastered_reviewAt` | `mastered, reviewAt` | 普通 | 复习队列查询 `mastered = 0 AND reviewAt <= now` |
| `index_words_addedAt` | `addedAt` | 普通 | 首页按添加时间倒序 |

去重说明：

- `NOCASE` 只对 ASCII 字母做大小写折叠，适合英文单词；`Apple` 与 `apple` 会被视为重复。
- 中文或其他 Unicode 大小写场景在后续需要额外的规范化字段。
- Room 的 `OnConflictStrategy.IGNORE` 在唯一索引冲突时返回 `-1`，不会抛异常。

## 4. DAO 契约

`WordDao` 只包含数据访问方法，不包含 UI 状态与业务算法。

### 4.1 查询

| 方法 | 语义 | 返回 |
| --- | --- | --- |
| `observeAll()` | 全部单词，`addedAt DESC, id DESC` | `Flow<List<Word>>` |
| `observeById(id)` | 按主键查询；不存在时发射 `null` | `Flow<Word?>` |
| `observeDue(now)` | `mastered = 0 AND reviewAt <= now`，按 `reviewAt ASC, id ASC` | `Flow<List<Word>>` |
| `observeTotalCount()` | 全部数量 | `Flow<Int>` |
| `observeDueCount(now)` | 到期数量 | `Flow<Int>` |
| `count()` | 一次性数量查询 | `Int` |

### 4.2 写入

| 方法 | 语义 | 返回 |
| --- | --- | --- |
| `insert(word)` | 插入单词；`word` 冲突时忽略 | 新 rowId，冲突为 `-1` |
| `insertAll(words)` | 批量插入，适用于导入 | rowId 列表，冲突项为 `-1` |
| `update(word)` | 按主键更新整行 | 受影响行数 |
| `updateReviewState(id, reviewAt, memoryStage, mastered)` | 复习后只更新调度字段 | 受影响行数 |
| `delete(word)` | 按实体删除 | 受影响行数 |
| `deleteById(id)` | 按主键删除 | 受影响行数 |

### 4.3 线程与响应式

- `Flow` 查询由 Room 在后台线程执行，数据表变化后自动重新发射。
- `suspend` 写入方法由调用方在协程中执行，Room 负责事务与线程切换。
- 不在主线程使用 `allowMainThreadQueries()`。

## 5. AppDatabase

```kotlin
@Database(
    entities = [Word::class],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun wordDao(): WordDao
}
```

设计要点：

- 数据库名：`dancijiyi.db`。
- 版本：`1`，作为首个正式 schema 基线。
- `exportSchema = true`，schema 由 KSP 输出到 `app/schemas/com.dancijiyi.data.AppDatabase/1.json`。
- 单例通过 `@Volatile` + `synchronized` 双重检查实现，避免重复创建数据库连接。
- 使用 `context.applicationContext`，避免持有 Activity。
- 不启用 `fallbackToDestructiveMigration()`；后续版本必须提供 `Migration`。
- 数据库在第一次访问 `getInstance()` 时惰性创建。

## 6. 数据访问链路

```text
[首页/详情]（已接入 Room）
            │
            ▼
  各页面 ViewModel（列表 / 详情 / 导入 / 复习，协程入口已预留）
            │
            ▼
      WordRepository       （数据访问编排）
            │
            ▼
         WordDao           （Room 注解接口）
            │
            ▼
      AppDatabase          （RoomDatabase 单例）
            │
            ▼
        SQLite (`words`)
```

小组件的数据链路（后续）：

```text
[桌面小组件] ──只读──> ContentProvider / DataStore ──> 今日单词 + 待复习数
                              ▲
                              │ 数据变化时主动通知
                       App 写入/复习/导入
```

小组件不直接持有 `AppDatabase`，避免跨进程访问 Room 实例。

## 7. 示例数据

```text
Word(
    id = 1,
    word = "abandon",
    meaning = "v. 放弃；抛弃",
    addedAt = 1762912800000,      // 2026-11-12 10:00:00 +08:00
    reviewAt = 1762999200000,     // 次日 10:00 到期
    memoryStage = 2,              // 短期记忆
    mastered = false
)
```

对应数据库行：

| id | word | meaning | addedAt | reviewAt | memoryStage | mastered |
| --- | --- | --- | --- | --- | --- | --- |
| 1 | abandon | v. 放弃；抛弃 | 1762912800000 | 1762999200000 | 2 | 0 |

## 8. 校验与边界

| 场景 | 处理 |
| --- | --- |
| 单词或释义为空白 | 业务层拒绝写入；DAO 不做隐式 trim |
| 首尾空格 | 调用方（ViewModel / 导入解析器）负责 `trim()` |
| `Apple` 与 `apple` | 唯一索引冲突，`insert` 返回 `-1` |
| 重复导入 | 默认跳过；`insertAll` 对应位置返回 `-1` |
| `memoryStage` 越界 | 业务层限制到 0..5；数据库不强制 |
| `reviewAt` 已过期 | 进入待复习队列 |
| `mastered = true` | 不进入待复习队列 |
| 删除单词 | 单表删除，无级联关系；后续复习日志需单独处理 |
| 时区变化 | 存储不变；展示层按当前时区格式化 |
| 批量导入 1 万条 | 分批次调用 `insertAll`，每批一个事务，避免长事务 |

## 9. 迁移与演进

### 9.1 v1 基线

- 当前 schema 版本为 1。
- `1.json` 已纳入版本控制；任何实体变更都必须同时提交新的 schema 文件。

### 9.2 后续可能新增字段

| 字段 | 用途 | 迁移方式 |
| --- | --- | --- |
| `phonetic` | 音标 | `ALTER TABLE words ADD COLUMN phonetic TEXT` |
| `example` | 例句 | `ALTER TABLE words ADD COLUMN example TEXT` |
| `tags` | 标签 | 新建 `tags` / `word_tags` 关联表 |
| `lastReviewedAt` | 上次复习时间 | `ALTER TABLE words ADD COLUMN lastReviewedAt INTEGER` |
| `correctCount` / `wrongCount` | 复习统计 | 新增列或独立 `review_logs` 表 |

原则：

- 不删除旧列、不重命名列，除非提供完整迁移。
- 每次 schema 版本 +1，并提供 `Migration(old, new)`。
- 生产环境禁止破坏性迁移。
- 新增索引时同步更新 schema 与本文档。

## 10. 与页面需求的对应

| 页面 | 使用的方法/字段 |
| --- | --- |
| 首页 · 单词列表 | `observeAll()`、`insert()`；数量与筛选在 ViewModel 内计算 |
| 单词详情 | `observeById()`、`update()`、`delete()`；全部字段 |
| 导入词库 | `insertAll()`、`word` 唯一索引、返回的 rowId；支持批量冲突统计 |
| 导出词库 | `getAll()` 一次性读取全部单词，按 `word COLLATE NOCASE` 排序 |
| 复习测试 | 预留 `observeDue()`、`updateReviewState()`、`memoryStage/reviewAt/mastered` |
| 桌面小组件 | 只读的到期数量与今日单词（后续通过 ContentProvider / DataStore） |

当前列表、详情、词库导入与导出已接入 Room 并完成基础业务；复习调度与小组件真实数据仍未接入。
