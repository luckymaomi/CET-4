# kaoshi 题库资源目录和 CET4 第一卷导入计划

## 需求

- 把当前初始化题库样例从 SQL 硬编码迁到可维护的题库资源目录，并删除旧英语基础样例。
- 新增 `2015 年 12 月英语四级真题第一卷` 示例题库和考试，使用 owner 提供的 docx/pdf 与 `201512cet4-01.mp3`。
- 以后可以继续放入 2015/2016/2017 等多套题，新增题目时优先新增资源文件，不继续把题目硬塞进迁移 SQL。
- 初始化主干只保留 admin、张三、陈欢三个人；E2E 和后端测试锚点改成 CET4 当前事实。

## 当前事实

- 当前数据库初始化把题库、试题、考试、发布快照、作答样本和成绩样本直接写在 `MIGRATION003__exam_business_core.sql` 和 `MIGRATION004__sample_exam_media.sql` 中。
- 当前平台支持题型：`SINGLE_CHOICE`、`MULTIPLE_CHOICE`、`WRITING`。
- 当前平台支持题目附件：图片、音频、视频、文件 URL；考试发布和作答会复制附件快照。
- 当前考试端只有考试总时长倒计时，没有听力/阅读/翻译等分 section 计时模型。
- owner 提供的 CET4 第一卷目录包含：
  - `卷一 2015年12月英语四级真题及答案.docx`
  - `卷一 2015年12月英语四级真题及答案.pdf`
  - `201512cet4-01.mp3`
- Windows 文件属性确认 `201512cet4-01.mp3` 时长为 `00:25:01`，比 docx 中 `Listening Comprehension (30 minutes)` 标注短。
- docx 内容确认：
  - Writing 30 minutes。
  - Listening Comprehension 标注 30 minutes，含 1-25 选择题和 26-35 听写填空。
  - Reading Comprehension 40 minutes，含 36-45 选词、46-55 匹配、56-65 选择。
  - Translation 30 minutes。
  - 答案包含 1-25、26-35、36-45、46-55、56-65，以及写作/翻译参考范文。
- owner 明确纠正：旧英语基础样例不需要迁移，不需要兼容；旧样例、旧候选人样本和旧测试锚点都要删除或改成 CET4 当前事实。

## 生产级验收保护点

- 不把题目内容继续硬编码到 SQL migration。
- 题库资源文件必须结构化、可追加、可 review，资源导入代码和题目内容分离。
- 不保留旧 `英语基础题库`、`英语基础模拟考试`、旧附件和 10 个候选人样本。
- CET4 第一卷题库、试题、音频附件和公开考试初始化后可在管理端和考试端看到。
- 当前不伪造平台未支持的能力：分 section 计时不写成已实现；只用当前题型承载。
- 选择题使用自动评分；写作、翻译和听写填空用 `WRITING` 承载，进入人工阅卷。
- 资源导入应幂等面对空库初始化，不依赖 MySQL 专属语法导致 H2 测试失败。
- 文件职责继续按变化原因拆分：资源 schema、资源导入、题库内容和 SQL schema 分清边界。

## 目标

- 新增 `backend/src/main/resources/question-sets/` 作为题库资源目录。
- 用资源导入器读取 `question-sets/index.json` 中声明的 JSON 文件，并导入分类、题库、试题、附件、考试、草稿快照和发布快照。
- 删除旧英语基础样例硬编码，删除旧样例成绩数据。
- 新增 CET4 第一卷资源 JSON。
- 把 CET4 音频复制到前端静态资源目录，路径稳定为 `/local-assets/cet4/2015-12/set-1/201512cet4-01.mp3`。
- 更新测试或补充测试，验证资源目录导入和 CET4 样例存在。

## 不做范围

- 本轮不新增新的题型枚举，不做填空题专用 UI，不做选词填空组内互斥规则。
- 本轮不实现听力/阅读/翻译分 section 倒计时。
- 本轮不导入所有历史 CET4/CET6 试卷，只导入 owner 指定的 2015 年 12 月英语四级第一卷。
- 本轮不改变考试生命周期、发布快照、作答快照和阅卷锁定语义。

## 设计

- 资源目录：
  - `question-sets/cet4/2015-12/set-1.json`：CET4 第一卷。
- 资源导入：
  - `MIGRATION006__question_set_imports.sql` 只建立资源导入幂等记录表。
  - `QuestionSetResourceImporter` 读取 `question-sets/index.json` 和题库 JSON，`QuestionSetJdbcWriter` 负责写入当前 schema。
  - JSON 中使用外部 code 表达题库、题目、考试和成绩样本之间的引用，导入时在内存中映射成数据库 ID。
  - 导入考试时同时生成草稿题目、发布题目、选项和附件快照，保持当前考试端可直接作答。
  - 旧 SQL migration 保留表结构、菜单和权限初始化，不再承载题库内容。
  - 身份初始化只保留 admin、张三、陈欢，张三和陈欢为考生账号。
- CET4 承载策略：
  - Writing 和 Translation 用 `WRITING`。
  - 1-25、36-45、46-65 用 `SINGLE_CHOICE`。
  - 26-35 听写填空当前用 `WRITING` 承载为 10 个主观题，后续有填空题型后可迁移。
  - CET4 第一卷考试总时长设为 130 分钟，描述中记录 Writing 30、Listening 标注 30/音频 25:01、Reading 40、Translation 30。

## 实施任务

- [x] 读取 kaoshi development skill、plan skill、`AGENTS.md` 和 `spec.md`。
- [x] 确认现有 SQL 硬编码样例、表结构、E2E 依赖和 CET4 原始文件内容。
- [x] 确认 MP3 时长。
- [x] 建立 `question-sets` 资源目录和资源 schema。
- [x] 复制 CET4 音频到稳定前端静态资源路径。
- [x] 实现 Java 资源导入 migration。
- [x] 删除 SQL migration 中题库/试题/考试/成绩样例硬编码。
- [x] 删除旧英语基础样例和旧本地附件依赖。
- [x] 新增 CET4 第一卷资源 JSON。
- [x] 调整后端测试和 E2E，测试锚点改成 CET4 第一卷、张三、陈欢。
- [x] 运行后端相关测试。
- [x] 运行前端类型检查和单测。
- [x] 运行 `python .\start_test.py`。
- [x] 运行 `python .\start_browser_test.py`。
- [x] 收口更新 `plan.md`，写明完成事实、验证结果和风险。

## 验证计划

- `mvn test -Dtest=ExamBusinessFlowTests`
- `npm.cmd run typecheck`
- `npm.cmd run test:unit`
- `python .\start_test.py`
- `python .\start_browser_test.py`

## 收口

- [x] 题库资源目录已成为初始化题目主来源。
- [x] CET4 第一卷已能通过初始化数据库进入题库和考试。
- [x] 旧样例已删除，CET4 当前主干链路和 E2E 通过。
- [x] 常规验证通过：`python .\start_test.py`。
- [x] 真实浏览器验收通过：`python .\start_browser_test.py`，10 条 Playwright 用例通过。
