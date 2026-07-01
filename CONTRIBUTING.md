# Contributing

欢迎为 gaokao 贡献代码、文档、测试和设计建议。

## 工作流程

1. 先阅读 `AGENTS.md`、`spec.md` 和当前 `plan.md`。
2. 中大型任务先更新 `plan.md`。
3. 按计划完成 research、设计、实现、测试、文档同步和验证。
4. 提交前运行项目定义的验证命令。
5. commit 和 push 前必须得到项目 owner 明确确认。

## 文档规则

- `spec.md` 只记录当前产品事实。
- `plan.md` 记录当前任务执行合同。
- `README.md` 面向新读者介绍项目。
- 不把未实现能力写成当前事实。

## 代码规则

- 后端代码保持清晰的 controller、service、domain、repository、security 和 migration 边界。
- 前端代码保持页面、组件、API client、状态和业务规则边界。
- 数据库变更必须有 migration 或初始化脚本。
- 接口必须有稳定请求、响应和错误结构。
- 测试覆盖核心业务规则、权限边界和失败路径。

## 提交规则

提交信息使用简洁中文，说明本次改动的真实意图。

示例：

```text
初始化项目协作骨架
新增题库领域模型
实现考试发布接口
```
