package com.kaoshi.exam;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.io.ByteArrayOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class ExamBusinessFlowTests {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void examLifecycleSavesDraftPublishesAndScoresFromAttemptSnapshot() throws Exception {
        String token = adminToken();
        long bankId = createBank(token, "阅读理解题库");
        long questionId = createSingleChoiceQuestion(token, bankId);

        String draftResponse = createDraftExam(token, bankId, """
                "title": "阅读理解模拟考试",
                "description": "用于端到端测试",
                "qualifyScore": 6,
                "startTime": "2026-01-01T00:00:00",
                "endTime": "2026-12-31T23:59:59",
                "durationMinutes": 20,
                "timeLimit": true,
                "attemptLimit": null,
                "displayMode": "PAGED",
                "questionOrderMode": "FIXED",
                "openType": "PUBLIC"
                """);
        long examId = objectMapper.readTree(draftResponse).at("/data/id").asLong();

        mockMvc.perform(get("/api/admin/exams/{examId}", examId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andExpect(jsonPath("$.data.rules[0].bankId").value(bankId))
                .andExpect(jsonPath("$.data.rules[0].singleCount").value(1))
                .andExpect(jsonPath("$.data.rules[0].singleScore").value(10))
                .andExpect(jsonPath("$.data.questionOrderMode").value("FIXED"));

        mockMvc.perform(post("/api/admin/exams/{examId}/publish", examId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.data.questionCount").value(1))
                .andExpect(jsonPath("$.data.totalScore").value(10));

        mockMvc.perform(post("/api/exam/{examId}/start", examId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.displayMode").value("PAGED"))
                .andExpect(jsonPath("$.data.questions[0].type").value("SINGLE_CHOICE"))
                .andExpect(jsonPath("$.data.questions[0].attachments[0].mediaType").value("IMAGE"))
                .andExpect(jsonPath("$.data.questions[0].attachments[0].fileUrl").value("/assets/quick.png"))
                .andExpect(jsonPath("$.data.questions[0].options.length()").value(3));

        updateQuestionToDifferentCorrectAnswer(token, bankId, questionId);

        mockMvc.perform(post("/api/exam/{examId}/submit", examId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "answers": [
                                    {"questionId": %d, "selectedLabels": ["B"]}
                                  ]
                                }
                                """.formatted(questionId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalScore").value(10))
                .andExpect(jsonPath("$.data.obtainedScore").value(10))
                .andExpect(jsonPath("$.data.correctCount").value(1));

        mockMvc.perform(post("/api/exam/{examId}/submit", examId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "answers": [
                                    {"questionId": %d, "selectedLabels": ["B"]}
                                  ]
                                }
                                """.formatted(questionId)))
                .andExpect(status().isConflict());

        String resultsResponse = mockMvc.perform(get("/api/admin/results")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].examTitle").value("阅读理解模拟考试"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        long resultId = objectMapper.readTree(resultsResponse).at("/data/0/id").asLong();

        mockMvc.perform(get("/api/admin/results/{resultId}", resultId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.questions[0].stem").value("Which word means fast?"))
                .andExpect(jsonPath("$.data.questions[0].selectedLabels[0]").value("B"))
                .andExpect(jsonPath("$.data.questions[0].correctLabels[0]").value("B"))
                .andExpect(jsonPath("$.data.questions[0].analysis").value("Quick means fast."));

        mockMvc.perform(get("/api/exam/results/{resultId}", resultId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.questions[0].correct").value(true));
    }

    @Test
    void seededExamUsesLocalAudioPngAndJpgAttachments() throws Exception {
        String token = adminToken();

        mockMvc.perform(post("/api/exam/{examId}/start", 1)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.questions[*].attachments[*].fileUrl").value(hasItems(
                        "/local-assets/dog-wolf-friendship.mp3",
                        "/local-assets/noun-example.png",
                        "/local-assets/improve-card.jpg",
                        "/local-assets/practice-chart.png"
                )))
                .andExpect(jsonPath("$.data.questions[*].attachments[*].mediaType").value(hasItems("AUDIO", "IMAGE")));
    }

    @Test
    void limitedAttemptExamRejectsRestartAfterSubmittedLimit() throws Exception {
        String token = adminToken();

        String examResponse = createDraftExam(token, 1, """
                "title": "限定次数考试",
                "description": "用于验证可考次数",
                "qualifyScore": 0,
                "startTime": "2026-01-01T00:00:00",
                "endTime": "2026-12-31T23:59:59",
                "durationMinutes": 20,
                "timeLimit": false,
                "attemptLimit": 1,
                "displayMode": "PAGED",
                "questionOrderMode": "FIXED",
                "openType": "PUBLIC"
                """);
        long examId = objectMapper.readTree(examResponse).at("/data/id").asLong();
        publishExam(token, examId);

        mockMvc.perform(post("/api/exam/{examId}/start", examId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/exam/{examId}/submit", examId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "answers": [
                                    {"questionId": 1, "selectedLabels": ["B"]},
                                    {"questionId": 2, "selectedLabels": ["A", "C"]}
                                  ]
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/exam/{examId}/start", examId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict());
    }

    @Test
    void examDisplayAndRandomOrderAreConfiguredByAdminAndPersistedInAttempt() throws Exception {
        String token = adminToken();

        String examResponse = createDraftExam(token, 1, 2, 2, """
                "title": "整卷显示考试",
                "description": "用于验证题目显示配置",
                "qualifyScore": 0,
                "startTime": "2026-01-01T00:00:00",
                "endTime": "2026-12-31T23:59:59",
                "durationMinutes": 20,
                "timeLimit": false,
                "attemptLimit": null,
                "displayMode": "ALL",
                "questionOrderMode": "RANDOM",
                "openType": "PUBLIC"
                """);
        long examId = objectMapper.readTree(examResponse).at("/data/id").asLong();
        publishExam(token, examId);

        String firstStart = mockMvc.perform(post("/api/exam/{examId}/start", examId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.displayMode").value("ALL"))
                .andExpect(jsonPath("$.data.questions.length()").value(4))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String secondStart = mockMvc.perform(post("/api/exam/{examId}/start", examId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(objectMapper.readTree(secondStart).at("/data/attemptId").asLong())
                .isEqualTo(objectMapper.readTree(firstStart).at("/data/attemptId").asLong());
        assertThat(objectMapper.readTree(secondStart).at("/data/questions").toString())
                .isEqualTo(objectMapper.readTree(firstStart).at("/data/questions").toString());
    }

    @Test
    void questionValidationRejectsSingleChoiceWithMultipleCorrectOptions() throws Exception {
        String token = adminToken();

        mockMvc.perform(post("/api/admin/questions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "bankId": 1,
                                  "type": "SINGLE_CHOICE",
                                  "stem": "Invalid single choice",
                                  "analysis": "",
                                  "difficulty": "EASY",
                                  "status": "ACTIVE",
                                  "options": [
                                    {"label": "A", "content": "one", "correct": true},
                                    {"label": "B", "content": "two", "correct": true}
                                  ],
                                  "attachments": []
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40000));
    }

    @Test
    void fileUploadStoresRealMediaAndReturnsAttachmentPayload() throws Exception {
        String token = adminToken();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "listening.mp3",
                "audio/mpeg",
                new byte[]{1, 2, 3, 4}
        );

        mockMvc.perform(multipart("/api/admin/files")
                        .file(file)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fileName").value("listening.mp3"))
                .andExpect(jsonPath("$.data.fileUrl").value(startsWith("/uploads/")))
                .andExpect(jsonPath("$.data.mediaType").value("AUDIO"));
    }

    @Test
    void questionExcelImportSupportsTemplateExportSuccessAndRowErrors() throws Exception {
        String token = adminToken();

        mockMvc.perform(get("/api/admin/questions/import-template")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(result -> assertThat(result.getResponse().getContentType()).contains("spreadsheetml.sheet"));

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "questions.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                questionWorkbook("英语基础题库", "AC")
        );
        mockMvc.perform(multipart("/api/admin/questions/import")
                        .file(file)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.successCount").value(1))
                .andExpect(jsonPath("$.data.failureCount").value(0));

        mockMvc.perform(get("/api/admin/questions?page=1&size=20&keyword=Excel import listening question")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[0].stem").value("Excel import listening question"))
                .andExpect(jsonPath("$.data.records[0].attachments.length()").value(0));

        mockMvc.perform(get("/api/admin/questions/export")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(result -> assertThat(result.getResponse().getContentType()).contains("spreadsheetml.sheet"));

        MockMultipartFile invalidFile = new MockMultipartFile(
                "file",
                "questions-invalid.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                questionWorkbook("英语基础题库", "A,C")
        );
        mockMvc.perform(multipart("/api/admin/questions/import")
                        .file(invalidFile)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.successCount").value(0))
                .andExpect(jsonPath("$.data.failureCount").value(1))
                .andExpect(jsonPath("$.data.errors[0]").value(containsString("请使用 AC")));
    }

    private long createBank(String token, String name) throws Exception {
        String response = mockMvc.perform(post("/api/admin/question-banks")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "categoryId": 1,
                                  "name": "%s",
                                  "description": "用于端到端测试",
                                  "status": "ACTIVE"
                                }
                                """.formatted(name)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value(name))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).at("/data/id").asLong();
    }

    private long createSingleChoiceQuestion(String token, long bankId) throws Exception {
        String response = mockMvc.perform(post("/api/admin/questions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "bankId": %d,
                                  "type": "SINGLE_CHOICE",
                                  "stem": "Which word means fast?",
                                  "analysis": "Quick means fast.",
                                  "difficulty": "EASY",
                                  "status": "ACTIVE",
                                  "options": [
                                    {"label": "A", "content": "slow", "correct": false},
                                    {"label": "B", "content": "quick", "correct": true},
                                    {"label": "C", "content": "late", "correct": false}
                                  ],
                                  "attachments": [
                                    {"fileName": "quick.png", "fileUrl": "/assets/quick.png", "mediaType": "IMAGE"}
                                  ]
                                }
                                """.formatted(bankId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.options.length()").value(3))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).at("/data/id").asLong();
    }

    private void updateQuestionToDifferentCorrectAnswer(String token, long bankId, long questionId) throws Exception {
        mockMvc.perform(put("/api/admin/questions/{questionId}", questionId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "bankId": %d,
                                  "type": "SINGLE_CHOICE",
                                  "stem": "Changed source question",
                                  "analysis": "Changed analysis.",
                                  "difficulty": "HARD",
                                  "status": "ACTIVE",
                                  "options": [
                                    {"label": "A", "content": "quick", "correct": true},
                                    {"label": "B", "content": "slow", "correct": false}
                                  ],
                                  "attachments": []
                                }
                                """.formatted(bankId)))
                .andExpect(status().isOk());
    }

    private String createDraftExam(String token, long bankId, String fields) throws Exception {
        return createDraftExam(token, bankId, 1, 0, fields);
    }

    private String createDraftExam(String token, long bankId, int singleCount, int multipleCount, String fields) throws Exception {
        int singleScore = singleCount == 0 ? 0 : 10;
        int multipleScore = multipleCount == 0 ? 0 : 10;
        return mockMvc.perform(post("/api/admin/exams")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  %s,
                                  "departmentIds": [],
                                  "rules": [
                                    {
                                      "bankId": %d,
                                      "singleCount": %d,
                                      "singleScore": %d,
                                      "multipleCount": %d,
                                      "multipleScore": %d
                                    }
                                  ]
                                }
                                """.formatted(fields, bankId, singleCount, singleScore, multipleCount, multipleScore)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    private void publishExam(String token, long examId) throws Exception {
        mockMvc.perform(post("/api/admin/exams/{examId}/publish", examId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"));
    }

    private byte[] questionWorkbook(String bankName, String answer) throws Exception {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("试题导入");
            Row header = sheet.createRow(0);
            String[] headers = {"题库名称", "题型", "难度", "题干", "选项A", "选项B", "选项C", "选项D", "正确答案", "解析"};
            for (int i = 0; i < headers.length; i++) {
                header.createCell(i).setCellValue(headers[i]);
            }
            Row row = sheet.createRow(1);
            row.createCell(0).setCellValue(bankName);
            row.createCell(1).setCellValue("多选");
            row.createCell(2).setCellValue("困难");
            row.createCell(3).setCellValue("Excel import listening question");
            row.createCell(4).setCellValue("listen");
            row.createCell(5).setCellValue("speak");
            row.createCell(6).setCellValue("sleep");
            row.createCell(7).setCellValue("walk");
            row.createCell(8).setCellValue(answer);
            row.createCell(9).setCellValue("Listening and speaking are language skills.");
            workbook.write(output);
            return output.toByteArray();
        }
    }

    private String adminToken() throws Exception {
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"admin","password":"password"}
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode root = objectMapper.readTree(response);
        String token = root.at("/data/accessToken").asText();
        assertThat(token).isNotBlank();
        return token;
    }
}
