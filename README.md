# kaoshi

<p align="center">
  <strong>现代化考试与考试管理平台</strong>
</p>

<p align="center">
  <a href="https://github.com/agentjz/kaoshi"><img alt="GitHub Repo" src="https://img.shields.io/badge/GitHub-agentjz%2Fkaoshi-181717?logo=github"></a>
  <img alt="Java 21" src="https://img.shields.io/badge/Java-21-007396?logo=openjdk">
  <img alt="Spring Boot 3" src="https://img.shields.io/badge/Spring%20Boot-3-6DB33F?logo=springboot&logoColor=white">
  <img alt="Vue 3" src="https://img.shields.io/badge/Vue-3-42B883?logo=vuedotjs&logoColor=white">
  <img alt="TypeScript" src="https://img.shields.io/badge/TypeScript-5-3178C6?logo=typescript&logoColor=white">
  <img alt="License MIT" src="https://img.shields.io/badge/License-MIT-blue">
</p>

kaoshi 面向学校、机构、团队和个人考试场景，把题库建设、试卷编排、考试发布、在线作答、自动评分和成绩归档组织成一套清晰、稳定、可扩展的系统。

它覆盖考试平台的核心链路：题目沉淀、试卷组织、考试发布、考生作答和成绩归档。✨

## 🎯 产品方向

- 管理端用于维护用户、角色、题库、试题、试卷、考试、成绩和系统配置。
- 考试端用于考生登录、查看考试任务、进入考试、计时作答、提交试卷和查看结果。
- 数据模型以题库、试题、试卷、考试、作答、成绩和用户权限为核心。
- 富媒体题目支持图片、音频等附件能力，并通过统一文件服务接入。
- 系统按前后端分离方式建设，便于后续扩展 H5、桌面 Web 和小程序考试入口。

## ✨ 能做什么

- 🧑‍💼 管理用户、角色、权限和菜单，支撑清晰的后台权限边界。
- 📚 维护题库、试题、选项、答案、解析、分值、难度和附件。
- 🧾 编排试卷，保存题目顺序、分值快照、总分和限时。
- 🚀 发布考试，维护考试时间、状态、规则和可见范围。
- ⏱️ 在线作答，支持倒计时、答题卡、附件展示、提交确认和提交锁定。
- ✅ 自动评分，归档成绩、正确题数、得分、总分和提交记录。
- 🧪 真实浏览器验收，覆盖登录、管理端操作、考试作答和成绩展示。

## 🧩 产品模块

| 模块 | 能力 |
| --- | --- |
| 身份权限 | 登录、当前用户、用户管理、角色管理、权限清单、菜单清单 |
| 题库试题 | 题库、分类、单选题、多选题、选项、答案、解析、附件 |
| 试卷考试 | 试卷管理、选题组卷、考试发布、考试状态维护 |
| 在线作答 | 考试中心、开始考试、倒计时、答题卡、提交锁定 |
| 成绩归档 | 自动评分、成绩列表、提交结果、题目明细 |
| 工程验收 | 后端测试、前端单测、类型检查、构建、真实 E2E |

## 🛠️ 技术栈

- 后端：Java 21、Spring Boot 3、MyBatis-Plus、Spring Security、JWT
- 数据：MySQL 8、Redis、Flyway
- 前端：Vue 3、TypeScript、Vite、Pinia、Vue Router、Element Plus
- 测试：JUnit、MockMvc、Vitest、Playwright
- 部署：Docker Compose

## 🧭 项目状态

当前已完成考试平台基础主链路：

`登录 -> 管理题库/试题/试卷/考试 -> 考生作答 -> 自动评分 -> 成绩归档`

本地开发、测试和验收入口记录在 [AGENTS.md](./AGENTS.md)。

## License

MIT License
