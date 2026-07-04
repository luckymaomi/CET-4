package com.kaoshi.admin.user;

import com.kaoshi.admin.role.mapper.AdminRoleMapper;
import com.kaoshi.admin.user.dto.UserCreateRequest;
import com.kaoshi.admin.user.mapper.AdminUserMapper;
import com.kaoshi.common.api.ErrorCode;
import com.kaoshi.common.exception.BusinessException;
import com.kaoshi.common.excel.ExcelWorkbooks;
import com.kaoshi.system.department.domain.Department;
import com.kaoshi.system.department.mapper.DepartmentMapper;
import com.kaoshi.user.domain.Role;
import com.kaoshi.user.domain.UserAccount;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

final class AdminUserExcelService {
    private final AdminUserMapper adminUserMapper;
    private final AdminRoleMapper adminRoleMapper;
    private final DepartmentMapper departmentMapper;

    AdminUserExcelService(AdminUserMapper adminUserMapper, AdminRoleMapper adminRoleMapper, DepartmentMapper departmentMapper) {
        this.adminUserMapper = adminUserMapper;
        this.adminRoleMapper = adminRoleMapper;
        this.departmentMapper = departmentMapper;
    }

    ResponseEntity<byte[]> template() {
        return ExcelWorkbooks.template(
                "用户导入模板.xlsx",
                List.of(
                        new ExcelWorkbooks.SheetData("用户导入", List.of("账号", "姓名", "部门", "角色", "状态"), userTemplateRows()),
                        dictionarySheet()
                )
        );
    }

    List<UserImportRow> readImportRows(MultipartFile file) {
        List<UserImportRow> rows = new ArrayList<>();
        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null || ExcelWorkbooks.text(row, 0).isBlank()) {
                    continue;
                }
                rows.add(new UserImportRow(rowIndex + 1, rowToRequest(row)));
            }
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "读取 Excel 失败");
        }
        return rows;
    }

    ResponseEntity<byte[]> exportExcel() {
        List<List<String>> rows = adminUserMapper.findUsers(null, 10000, 0)
                .stream()
                .map(this::exportRow)
                .toList();
        return ExcelWorkbooks.template(
                "用户导出.xlsx",
                List.of(new ExcelWorkbooks.SheetData("用户导出", List.of("账号", "姓名", "部门", "角色", "状态"), rows), dictionarySheet())
        );
    }

    private ExcelWorkbooks.SheetData dictionarySheet() {
        return new ExcelWorkbooks.SheetData("字典清单", List.of("字段", "可填写值", "编码", "说明"), userDictionaryRows());
    }

    private UserCreateRequest rowToRequest(Row row) {
        String username = ExcelWorkbooks.text(row, 0).trim();
        String displayName = ExcelWorkbooks.text(row, 1).trim();
        String departmentName = ExcelWorkbooks.text(row, 2).trim();
        String roleText = ExcelWorkbooks.text(row, 3).trim();
        String status = normalizeStatus(ExcelWorkbooks.text(row, 4).trim());
        if (!"ACTIVE".equals(status)) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "导入用户只允许创建启用账号，禁用请导入后在页面调整");
        }
        Long departmentId = resolveDepartmentId(departmentName);
        List<Long> roleIds = resolveRoleIds(roleText);
        return new UserCreateRequest(departmentId, username, displayName, roleIds);
    }

    private Long resolveDepartmentId(String value) {
        if (value.isBlank()) {
            return null;
        }
        return departmentMapper.findAll().stream()
                .filter(department -> value.equals(department.getName()) || value.equals(department.getCode()))
                .map(Department::getId)
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.VALIDATION_FAILED, "部门不存在：" + value));
    }

    private List<Long> resolveRoleIds(String value) {
        if (value.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "角色不能为空");
        }
        Map<String, Role> roles = adminRoleMapper.findAll().stream()
                .flatMap(role -> List.of(Map.entry(role.getCode(), role), Map.entry(role.getName(), role)).stream())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (left, ignored) -> left));
        List<Long> roleIds = List.of(value.replace("，", ",").split(",")).stream()
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .map(item -> {
                    Role role = roles.get(item);
                    if (role == null) {
                        throw new BusinessException(ErrorCode.VALIDATION_FAILED, "角色不存在：" + item);
                    }
                    return role.getId();
                })
                .distinct()
                .toList();
        if (roleIds.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "角色不能为空");
        }
        return roleIds;
    }

    private String normalizeStatus(String value) {
        if (value.isBlank() || "启用".equals(value) || "ACTIVE".equalsIgnoreCase(value)) {
            return "ACTIVE";
        }
        if ("禁用".equals(value) || "DISABLED".equalsIgnoreCase(value)) {
            return "DISABLED";
        }
        throw new BusinessException(ErrorCode.VALIDATION_FAILED, "用户状态不合法：" + value);
    }

    private List<List<String>> userDictionaryRows() {
        List<List<String>> rows = new ArrayList<>();
        activeDepartments().forEach(department -> rows.add(List.of(
                "部门",
                department.getName(),
                department.getCode(),
                department.getDescription() == null ? "" : department.getDescription()
        )));
        adminRoleMapper.findAll().forEach(role -> rows.add(List.of(
                "角色",
                role.getName(),
                role.getCode(),
                role.getDescription() == null ? "" : role.getDescription()
        )));
        rows.add(List.of("状态", "启用", "ACTIVE", "导入创建启用账号"));
        rows.add(List.of("状态", "禁用", "DISABLED", "模板列出可用状态；导入创建只允许启用账号"));
        return rows;
    }

    private List<List<String>> userTemplateRows() {
        List<Department> departments = activeDepartments();
        List<Role> roles = adminRoleMapper.findAll();
        String firstDepartment = departments.isEmpty() ? "" : departments.get(0).getName();
        String secondDepartment = departments.size() > 1 ? departments.get(1).getName() : firstDepartment;
        String studentRole = roles.stream()
                .filter(role -> "STUDENT".equals(role.getCode()))
                .findFirst()
                .map(Role::getName)
                .orElse(roles.isEmpty() ? "" : roles.get(0).getName());
        String managerRole = roles.stream()
                .filter(role -> "EXAM_MANAGER".equals(role.getCode()))
                .findFirst()
                .map(Role::getName)
                .orElse(studentRole);
        return List.of(
                List.of("student001", "学生一", firstDepartment, studentRole, "启用"),
                List.of("teacher001", "教师一", secondDepartment, managerRole, "启用")
        );
    }

    private List<Department> activeDepartments() {
        return departmentMapper.findAll().stream()
                .filter(department -> "ACTIVE".equals(department.getStatus()))
                .toList();
    }

    private List<String> exportRow(UserAccount user) {
        return List.of(
                user.getUsername(),
                user.getDisplayName(),
                user.getDepartmentId() == null ? "" : adminUserMapper.findDepartmentName(user.getDepartmentId()),
                String.join(",", adminUserMapper.findRoleCodes(user.getId())),
                "ACTIVE".equals(user.getStatus()) ? "启用" : "禁用"
        );
    }

    record UserImportRow(int rowNumber, UserCreateRequest request) {
    }
}
