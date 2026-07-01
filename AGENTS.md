# gaokao Agent 工作规约

本文件是 gaokao 仓库的最高工作约束。产品事实看 `spec.md`，当前任务执行状态看 `plan.md`。

## 基础约束

- 始终使用简体中文和项目 owner 交流。
- 阅读本仓库文本文件时按 UTF-8 读取。
- 在 PowerShell 中运行 Node/npm 命令时使用 `npm.cmd`、`npx.cmd`。
- 不回滚 owner 已有改动，除非 owner 明确要求。
- 遇到脏工作区，只处理当前任务相关文件。
- 每次 commit 或 push 前必须先向 owner 确认。
- 不使用阶段性版本命名组织产品事实、计划或回复。
- 命名调整必须逐文件语义修改，并通过类型检查、边界扫描和必要搜索验证。

## 根文档职责

- `AGENTS.md`：agent 工作规则和仓库协作纪律。
- `spec.md`：当前产品事实、业务边界、核心概念和验收标准。
- `plan.md`：当前任务的单文件规格驱动执行合同。
- `plan.example.md`：后续任务计划模板。
- `README.md`：面向新读者的项目入口和产品介绍。
- `LICENSE`：开源协议权威文本。
- `SECURITY.md`：安全报告和敏感数据处理规则。
- `CONTRIBUTING.md`：贡献流程和文档驱动规则。
- `.gitignore`：不进入版本控制的文件边界。
- `.codex/skills/gaokao-development/SKILL.md`：开发、架构、后端、前端、数据模型和生产级交付规则。
- `.codex/skills/gaokao-ui/SKILL.md`：管理端和考试端 UI 规则。
- `.codex/skills/plan/SKILL.md`：`plan.md` 写作和执行规则。

## Skills

命中 skill 描述时，必须先读取对应 `SKILL.md`，再行动。

- 修改 Java、Spring Boot、MySQL、Redis、Vue、TypeScript、接口契约、数据模型、测试、README、spec、AGENTS、skills 或运行配置时，先读 `.codex/skills/gaokao-development/SKILL.md`。
- 修改页面信息架构、管理端表单、考试端作答页、WXML/HTML/CSS、Element Plus 组件、布局、按钮、空状态、表格或视觉样式时，先读 `.codex/skills/gaokao-ui/SKILL.md`。
- 中大型开发、跨模块重构、生产级验收、架构硬化、评测闭环，或任何不能靠一次小补丁完成的任务，先读 `.codex/skills/plan/SKILL.md`，并维护根目录 `plan.md`。

## 事实纪律

- 事实高于一切。
- 所有判断、计划、文档修改和回复必须基于当前仓库文件、命令输出、owner 明确提供的信息或可复现结果。
- 必须区分已确认事实、owner 提供事实、未验证推测、计划事项和已完成并验证事项。
- 不编造功能、数据、接口、页面行为、测试结果、服务能力或业务规则。
- 不把未实现能力写成当前产品事实。
- 文档只写当前事实和当前边界。

## Plan 规则

`plan.md` 是单文件规格驱动执行合同。中大型任务、跨文档修改、产品边界调整、工程实现和验证闭环都必须先维护 `plan.md`。

执行规则：

- 写完计划，再动手。
- checklist 必须随进度更新。
- 新事实推翻计划，先改计划，再继续。
- 不用口头解释替代计划更新。
- 不偷偷偏离计划。
- 提出一个方向后，把 research、设计、实现、测试、文档同步、验证和收口做成同一次完整交付。

`plan.md` 必须至少覆盖：

- 需求文档。
- 当前事实。
- 失败测试。
- 目标。
- 不做范围。
- 设计。
- 实施任务。
- 验证计划。
- 收口。

## 产品纪律

当前产品名是 gaokao。

当前产品主线是现代化考试与考试管理平台。核心业务围绕用户、权限、题库、试题、试卷、考试、作答、评分、成绩和文件附件展开。

## 技术纪律

- 后端默认使用 Java 21、Spring Boot 3、MySQL 8、MyBatis-Plus、Spring Security、JWT 和 Redis。
- 前端默认使用 Vue 3、TypeScript、Vite 和 Element Plus。
- 领域模型必须有显式类型。
- 后端接口必须有请求、响应、错误码和鉴权边界。
- 数据库变更必须有 migration 或明确初始化脚本。
- 页面文件只负责交互和呈现，不承载复杂业务规则。
- 业务规则放在可测试模块中。
- 禁止用 `any` 或无类型对象逃避设计。

## 验证规则

- 文档任务必须检查目标文件是否存在。
- 代码任务收尾前必须运行项目定义的类型检查、测试、构建或等价验证。
- 不能只凭解释声称完成，必须说明验证结果。
- commit 或 push 前必须说明将提交的范围并得到 owner 明确确认。

## 收尾回复

- 最终回复保持简洁。
- 说明改了什么、验证了什么、是否还有风险。
- 不粘贴大段日志、diff 或源码，除非 owner 明确要求。
