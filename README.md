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

kaoshi 面向学校、机构、团队和个人考试场景，把题库建设、试题维护、考试发布、在线作答、自动评分和成绩归档组织成一套清晰、稳定、可扩展的系统。

它覆盖考试平台的核心链路：题目沉淀、试卷组织、考试发布、考生作答和成绩归档。✨

## 🎯 产品方向

- 控制台按“控制台、在线考试、考试管理、系统管理”组织主菜单。
- 在线考试用于考生查看考试任务、准备考试、按考试配置作答、提交试卷和查看成绩。
- 考试管理按成熟考试平台心智组织为“题库管理、试题管理、考试管理”；试卷是考试创建时的内部组卷结果，不作为显性同级菜单。
- 数据模型以用户、部门、权限、题库、试题、考试规则、发布快照、作答快照和成绩为核心。
- 当前快速演进期默认清空重建本地数据，不做旧库迁移兼容。
- 富媒体题目支持真实文件上传，也支持本地音频、png 图片、jpg 图片和文件链接附件能力。
- 系统按前后端分离方式建设，便于后续扩展 H5、桌面 Web 和小程序考试入口。

## ✨ 能做什么

- 🧑‍💼 管理用户、角色、权限和菜单；用户支持 Excel 模板、导入和导出，新用户使用默认密码并在首次登录时强制改密。
- 🧑‍💼 管理部门树，支持在线新建、编辑、删除和树形选择。
- 📚 维护题库、试题、选项、答案、解析、难度和附件；试题 Excel 支持文本题目导入导出，富媒体在网页试题编辑页维护。
- 🚀 保存考试草稿、发布考试和关闭考试；发布时按题库规则生成题目、选项、答案、解析和附件快照，维护总分、及格分、考试时长、限时、可考次数、题目显示方式、题目顺序和部门开放范围。
- ⏱️ 在线作答，支持准备考试、倒计时、逐题显示或整卷一页、题目正文题型标签、按题型答题卡、上一题、下一题、附件展示、提交确认和提交锁定。
- ✅ 自动评分，归档成绩、正确题数、得分、总分、提交记录和逐题复盘。
- 🧪 真实浏览器验收，覆盖登录、管理端操作、考试作答和成绩展示。

## 🧩 产品模块

| 模块 | 能力 |
| --- | --- |
| 身份权限 | 登录、首次改密、当前用户、用户管理、用户 Excel、角色管理、部门管理、角色权限配置 |
| 题库试题 | 题库、分类、单选题、多选题、选项、答案、解析、附件、Excel 导入导出 |
| 考试管理 | 题库管理、试题管理、考试管理、草稿保存、发布快照、考试状态维护、部门开放 |
| 在线作答 | 考试中心、准备考试、逐题或整卷作答、倒计时、答题卡、提交锁定 |
| 成绩归档 | 自动评分、成绩列表、成绩详情、题目明细、正确答案、解析 |
| 工程验收 | 后端测试、前端单测、类型检查、构建、真实 E2E |

## 🛠️ 技术栈

- 后端：Java 21、Spring Boot 3、MyBatis-Plus、Spring Security、JWT
- 数据：MySQL 8、Redis、Flyway
- Excel：Apache POI
- 前端：Vue 3、TypeScript、Vite、Pinia、Vue Router、Element Plus
- 测试：JUnit、MockMvc、Vitest、Playwright
- 部署：Docker Compose

## 🧭 项目状态

当前已完成考试平台基础主链路：

`登录 -> 管理题库/试题/考试 -> 准备考试 -> 在线作答 -> 自动评分 -> 成绩复盘`

本地开发入口 `python .\start_dev.py` 会先清空 Docker 中的 kaoshi 数据，再按当前初始化脚本重建并启动服务。

本地开发、测试和验收入口记录在 [AGENTS.md](./AGENTS.md)。

## License

MIT License
