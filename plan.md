# gaokao 初始化 Plan

## 1. 需求文档

本次任务初始化 gaokao 的 AI 协作骨架和项目根文档，让后续开发可以围绕统一产品事实、技术方向、执行计划、安全边界和贡献规则展开。

业务完成标准：

- 项目根目录拥有清晰的产品介绍、产品事实、执行计划、计划模板、协作规约、安全说明、贡献指南、开源协议和忽略规则。
- `.codex/skills` 拥有开发、UI 和计划三个项目级 skill。
- 文档统一表达 gaokao 是现代化考试与考试管理平台。
- 技术方向统一为 Java 21、Spring Boot 3、MySQL 8、MyBatis-Plus、Spring Security/JWT、Redis、Vue 3、TypeScript、Vite 和 Element Plus。

## 2. 当前事实

- 目标目录是 `C:\Users\Administrator\Desktop\gaokao`。
- owner 已确认项目名为 `gaokao`。
- owner 已确认先初始化 AI 辅助骨架和根文档。
- 本机已安装 Java 21、Maven、MySQL、DBeaver、Postman 和 VS Code Java/Spring 插件。
- 当前任务只创建文档和 skill，不生成后端、前端或数据库实现代码。

## 3. 失败测试

- 根目录缺少 `README.md`、`AGENTS.md`、`spec.md`、`plan.md`、`plan.example.md`、`LICENSE`、`SECURITY.md`、`CONTRIBUTING.md` 或 `.gitignore`。
- `.codex/skills` 缺少开发、UI 或 plan skill。
- 文档中项目名不统一。
- 文档把尚未实现的接口、页面、数据库或部署写成当前能力。
- 文档没有记录当前技术方向和执行纪律。

## 4. 目标

- 创建 gaokao 根文档和项目级 skills。
- README 提供简单、有吸引力的产品介绍。
- spec 记录当前产品定位、核心概念、技术方向和初始化验收标准。
- AGENTS 记录协作纪律、根文档职责、skill 使用规则和验证规则。
- SECURITY、CONTRIBUTING、LICENSE、.gitignore 完整可用。

## 5. 不做范围

- 本次不创建 Spring Boot 后端代码。
- 本次不创建 Vue 前端代码。
- 本次不创建数据库 migration。
- 本次不初始化远程仓库或执行 commit/push。

## 6. 设计

根文档负责项目事实和协作边界；skills 负责后续开发时的专项规则。

文档边界：

- `README.md` 面向新读者，介绍产品和技术栈。
- `spec.md` 记录当前产品事实。
- `AGENTS.md` 约束 AI 协作行为。
- `plan.md` 记录当前任务执行合同。
- `plan.example.md` 作为未来计划模板。
- `.codex/skills/*/SKILL.md` 作为后续开发、UI 和计划执行的专项规则。

## 7. 实施任务

- [x] T001 创建目录和 `.codex/skills` 结构。
- [x] T002 创建 README、spec、AGENTS、plan 和 plan.example。
- [x] T003 创建 LICENSE、SECURITY、CONTRIBUTING 和 `.gitignore`。
- [x] T004 创建 gaokao-development、gaokao-ui 和 plan skills。
- [x] T005 检查文件存在、项目命名和未实现能力表述。
- [x] T006 收口记录完成事实和验证结果。

## 8. 验证计划

执行文件检查：

```powershell
Test-Path README.md, AGENTS.md, spec.md, plan.md, plan.example.md, LICENSE, SECURITY.md, CONTRIBUTING.md, .gitignore
Test-Path .codex/skills/gaokao-development/SKILL.md, .codex/skills/gaokao-ui/SKILL.md, .codex/skills/plan/SKILL.md
```

执行内容扫描：

```powershell
rg -n "gaokao" README.md AGENTS.md spec.md plan.md plan.example.md SECURITY.md CONTRIBUTING.md .codex/skills
```

## 9. 收口

已完成。

完成事实：

- 创建 `C:\Users\Administrator\Desktop\gaokao` 项目目录。
- 创建根文档：`README.md`、`AGENTS.md`、`spec.md`、`plan.md`、`plan.example.md`、`LICENSE`、`SECURITY.md`、`CONTRIBUTING.md` 和 `.gitignore`。
- 创建项目级 skills：`gaokao-development`、`gaokao-ui` 和 `plan`。
- README 已写入 gaokao 的简短宣传介绍、产品方向、技术栈和工程原则。
- spec 已记录产品定位、技术方向、核心用户、核心概念和初始化验收标准。
- AGENTS 已记录协作纪律、根文档职责、skill 规则、事实纪律、计划规则、产品纪律、技术纪律和验证规则。
- LICENSE 使用 MIT License。

验证结果：

- 文件存在检查通过。
- `.codex/skills` 三个 skill 文件存在。
- 内容扫描未发现旧项目名或对照式定位表述。
- 技术方向扫描确认 Java 21、Spring Boot 3、MySQL 8、Vue 3、TypeScript、Vite 和 Element Plus 已在关键文档中一致出现。

未验证内容：

- 尚未初始化 Git 仓库。
- 尚未创建后端、前端、数据库和部署代码。

剩余风险：

- 后续进入工程实现前，需要围绕第一条实现主线重新更新 `plan.md`。
