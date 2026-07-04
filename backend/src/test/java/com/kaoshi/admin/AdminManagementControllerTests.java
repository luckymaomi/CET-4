package com.kaoshi.admin;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class AdminManagementControllerTests {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void adminApisRequireLogin() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40100));
    }

    @Test
    void userManagementSupportsPageCreateUpdateAndStatus() throws Exception {
        String token = adminToken();

        mockMvc.perform(get("/api/admin/users?page=1&size=10&keyword=admin")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[0].username").value("admin"))
                .andExpect(jsonPath("$.data.total").value(1));

        String createResponse = mockMvc.perform(post("/api/admin/users")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "departmentId": 2,
                                  "username": "teacher",
                                  "displayName": "考务老师",
                                  "roleIds": [2]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("teacher"))
                .andExpect(jsonPath("$.data.departmentName").value("四级考生"))
                .andExpect(jsonPath("$.data.roles[0]").value("EXAM_MANAGER"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        long userId = objectMapper.readTree(createResponse).at("/data/id").asLong();

        mockMvc.perform(put("/api/admin/users/{id}", userId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "departmentId": 1,
                                  "displayName": "考务主管",
                                  "roleIds": [2, 3]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.displayName").value("考务主管"))
                .andExpect(jsonPath("$.data.departmentName").value("默认组织"))
                .andExpect(jsonPath("$.data.roles.length()").value(2));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"teacher","password":"123456"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.user.mustChangePassword").value(true));

        mockMvc.perform(patch("/api/admin/users/{id}/status", userId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status": "DISABLED"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DISABLED"));

        mockMvc.perform(get("/api/admin/users/import-template")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    assertThat(result.getResponse().getContentType()).contains("spreadsheetml.sheet");
                    try (Workbook workbook = new XSSFWorkbook(new java.io.ByteArrayInputStream(result.getResponse().getContentAsByteArray()))) {
                        assertThat(workbook.getNumberOfSheets()).isEqualTo(2);
                        assertThat(workbook.getSheetAt(0).getSheetName()).isEqualTo("用户导入");
                        Sheet dictionary = workbook.getSheetAt(1);
                        assertThat(dictionary.getSheetName()).isEqualTo("字典清单");
                        assertThat(dictionary.getRow(0).getCell(0).getStringCellValue()).isEqualTo("字段");
                        assertThat(dictionary.getRow(0).getCell(1).getStringCellValue()).isEqualTo("可填写值");
                        assertThat(dictionary.getRow(1).getCell(0).getStringCellValue()).isEqualTo("部门");
                        assertThat(dictionary.getRow(1).getCell(1).getStringCellValue()).isEqualTo("默认组织");
                        assertThat(sheetContainsDictionaryRow(dictionary, "角色", "ADMIN")).isTrue();
                        assertThat(sheetContainsDictionaryRow(dictionary, "状态", "ACTIVE")).isTrue();
                        assertThat(dictionary.getLastRowNum()).isGreaterThan(4);
                    }
                });

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "users.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                userWorkbook("student001")
        );
        mockMvc.perform(multipart("/api/admin/users/import")
                        .file(file)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.successCount").value(1))
                .andExpect(jsonPath("$.data.failureCount").value(0));

        MockMultipartFile duplicateFile = new MockMultipartFile(
                "file",
                "users-duplicate.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                userWorkbook("student001")
        );
        mockMvc.perform(multipart("/api/admin/users/import")
                        .file(duplicateFile)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.successCount").value(0))
                .andExpect(jsonPath("$.data.failureCount").value(1))
                .andExpect(jsonPath("$.data.errors[0]").value(org.hamcrest.Matchers.containsString("账号已存在")));

        mockMvc.perform(get("/api/admin/users/export")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(result -> assertThat(result.getResponse().getContentType()).contains("spreadsheetml.sheet"));
    }

    @Test
    void roleManagementSupportsCreateUpdatePermissionsAndMenus() throws Exception {
        String token = adminToken();

        String createResponse = mockMvc.perform(post("/api/admin/roles")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "AUDITOR",
                                  "name": "审计员",
                                  "description": "查看权限和菜单",
                                  "permissionIds": [1, 6],
                                  "menuIds": [1, 2, 3]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code").value("AUDITOR"))
                .andExpect(jsonPath("$.data.permissions.length()").value(2))
                .andExpect(jsonPath("$.data.permissions[0].name").value("系统管理"))
                .andExpect(jsonPath("$.data.menus.length()").value(3))
                .andExpect(jsonPath("$.data.menus[0].title").value("工作台"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        long roleId = objectMapper.readTree(createResponse).at("/data/id").asLong();

        mockMvc.perform(put("/api/admin/roles/{id}", roleId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "AUDITOR",
                                  "name": "审计管理员",
                                  "description": "查看权限、菜单和成绩",
                                  "permissionIds": [1, 6],
                                  "menuIds": [1, 2, 3, 11]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("审计管理员"))
                .andExpect(jsonPath("$.data.menus.length()").value(4));

        mockMvc.perform(get("/api/admin/roles")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(4));
    }

    @Test
    void permissionAndMenuListsAreAvailableForAdmin() throws Exception {
        String token = adminToken();

        mockMvc.perform(get("/api/admin/permissions")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].code").value("system:admin"));

        mockMvc.perform(get("/api/admin/menus")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[1].title").value("角色管理"))
                .andExpect(jsonPath("$.data[2].title").value("部门管理"))
                .andExpect(jsonPath("$.data[3].title").value("用户管理"));
    }

    @Test
    void departmentManagementSupportsTreeCrud() throws Exception {
        String token = adminToken();

        mockMvc.perform(get("/api/admin/departments")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("默认组织"))
                .andExpect(jsonPath("$.data[0].children[0].name").value("四级考生"));

        String parentResponse = mockMvc.perform(post("/api/admin/departments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "parentId": 1,
                                  "name": "浏览器验收部",
                                  "code": "E2E_DEPT",
                                  "description": "用于部门管理测试",
                                  "status": "ACTIVE"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("浏览器验收部"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        long parentId = objectMapper.readTree(parentResponse).at("/data/id").asLong();

        mockMvc.perform(post("/api/admin/departments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "parentId": %d,
                                  "name": "浏览器验收小组",
                                  "code": "E2E_DEPT_CHILD",
                                  "description": "用于部门层级测试",
                                  "status": "ACTIVE"
                                }
                                """.formatted(parentId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.parentId").value(parentId));

        mockMvc.perform(delete("/api/admin/departments/{id}", parentId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict());

        mockMvc.perform(delete("/api/admin/departments/{id}", parentId + 1)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        String departmentTreeResponse = mockMvc.perform(get("/api/admin/departments")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertThat(departmentTreeResponse).contains("E2E_DEPT");

    }

    private byte[] userWorkbook(String username) throws Exception {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("用户导入");
            Row header = sheet.createRow(0);
            String[] headers = {"账号", "姓名", "部门", "角色", "状态"};
            for (int i = 0; i < headers.length; i++) {
                header.createCell(i).setCellValue(headers[i]);
            }
            Row row = sheet.createRow(1);
            row.createCell(0).setCellValue(username);
            row.createCell(1).setCellValue("Excel 学员");
            row.createCell(2).setCellValue("四级考生");
            row.createCell(3).setCellValue("EXAM_MANAGER");
            row.createCell(4).setCellValue("启用");
            workbook.write(output);
            return output.toByteArray();
        }
    }

    private boolean sheetContainsDictionaryRow(Sheet sheet, String field, String code) {
        for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) {
                continue;
            }
            if (field.equals(row.getCell(0).getStringCellValue()) && code.equals(row.getCell(2).getStringCellValue())) {
                return true;
            }
        }
        return false;
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

