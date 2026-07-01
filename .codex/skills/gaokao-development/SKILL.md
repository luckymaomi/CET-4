---
name: gaokao-development
description: 维护 gaokao 考试平台时使用。适用于修改 Java、Spring Boot、MySQL、Redis、Vue、TypeScript、接口契约、数据模型、权限、考试作答、评分、成绩、测试、README、spec、AGENTS、skills 或运行配置；要求先 research，再写 plan，再按生产级验收闭环交付。
---

# gaokao Development

每次接手都当作新项目。

先看事实。再做判断。最后行动。

事实来自当前 `spec.md`、`AGENTS.md`、`plan.md`、`README.md`、后端代码、前端代码、数据库脚本、测试、配置、git 状态、命令结果和 owner 明确输入。

## 铁律

- 先 research，再计划，再实现。
- 没有完成核心语义调查，不动局部代码。
- 用户输入是线索，仓库事实决定行动。
- 抓根本逻辑，不追表面症状。
- 不把未实现能力写成当前事实。
- 不交半成品。
- 架构、类型、测试、文档和验证必须作为同一次交付的一部分。

## 当前产品主干

gaokao 是现代化考试与考试管理平台。

核心领域：

- 用户。
- 角色。
- 权限。
- 题库。
- 试题。
- 试卷。
- 考试。
- 作答。
- 评分。
- 成绩。
- 文件附件。

默认技术方向：

- Java 21。
- Spring Boot 3。
- MySQL 8。
- MyBatis-Plus。
- Spring Security。
- JWT。
- Redis。
- Vue 3。
- TypeScript。
- Vite。
- Element Plus。
- Docker Compose。

## 后端纪律

- Controller 只做参数接入、鉴权上下文接入和响应转换。
- Service 承载业务流程。
- Domain/Entity 承载领域结构。
- Mapper/Repository 承载数据访问。
- Security 承载认证、授权和上下文解析。
- Migration 或初始化脚本承载数据库结构。
- 接口必须有请求、响应、错误码和权限边界。
- 考试提交必须幂等。
- 作答提交后必须锁定。
- 评分必须可复现。
- 成绩必须可追溯。

## 前端纪律

- Vue 页面只做页面状态和用户交互。
- API client 统一封装请求。
- 表单校验和业务校验分层。
- 管理端和考试端路径分离。
- 考试端倒计时、提交、锁定和异常恢复必须有明确状态。

## 验证

收尾前运行项目定义的验证命令。项目尚未定义命令时，至少运行相关语言的类型检查、测试或构建命令，并说明未验证内容。
