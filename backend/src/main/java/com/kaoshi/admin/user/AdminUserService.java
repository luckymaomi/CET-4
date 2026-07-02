package com.kaoshi.admin.user;

import com.kaoshi.admin.role.mapper.AdminRoleMapper;
import com.kaoshi.admin.user.dto.AdminUserResponse;
import com.kaoshi.admin.user.dto.UserCreateRequest;
import com.kaoshi.admin.user.dto.UserUpdateRequest;
import com.kaoshi.admin.user.mapper.AdminUserMapper;
import com.kaoshi.common.api.ErrorCode;
import com.kaoshi.common.exception.BusinessException;
import com.kaoshi.common.excel.ExcelImportResult;
import com.kaoshi.common.excel.ExcelWorkbooks;
import com.kaoshi.common.page.PageRequest;
import com.kaoshi.common.page.PageResponse;
import com.kaoshi.system.department.domain.Department;
import com.kaoshi.system.department.mapper.DepartmentMapper;
import com.kaoshi.user.domain.UserAccount;
import com.kaoshi.user.domain.Role;
import com.kaoshi.user.mapper.UserMapper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AdminUserService {
    public static final String DEFAULT_PASSWORD = "123456";

    private final UserMapper userMapper;
    private final AdminUserMapper adminUserMapper;
    private final AdminRoleMapper adminRoleMapper;
    private final DepartmentMapper departmentMapper;
    private final PasswordEncoder passwordEncoder;

    public AdminUserService(
            UserMapper userMapper,
            AdminUserMapper adminUserMapper,
            AdminRoleMapper adminRoleMapper,
            DepartmentMapper departmentMapper,
            PasswordEncoder passwordEncoder
    ) {
        this.userMapper = userMapper;
        this.adminUserMapper = adminUserMapper;
        this.adminRoleMapper = adminRoleMapper;
        this.departmentMapper = departmentMapper;
        this.passwordEncoder = passwordEncoder;
    }

    public PageResponse<AdminUserResponse> page(PageRequest request) {
        long total = adminUserMapper.countUsers(request.keywordLike());
        List<AdminUserResponse> records = adminUserMapper.findUsers(request.keywordLike(), request.size(), request.offset())
                .stream()
                .map(this::toResponse)
                .toList();
        return new PageResponse<>(records, total, request.page(), request.size());
    }

    public AdminUserResponse detail(Long id) {
        return toResponse(findUser(id));
    }

    public ResponseEntity<byte[]> template() {
        return ExcelWorkbooks.template(
                "用户导入模板.xlsx",
                List.of(
                        new ExcelWorkbooks.SheetData(
                                "用户导入",
                                List.of("账号", "姓名", "部门", "角色", "状态"),
                                List.of(
                                        List.of("student001", "学生一", "默认部门", "普通用户", "启用"),
                                        List.of("teacher001", "教师一", "英语教研组", "管理员", "启用")
                                )
                        ),
                        new ExcelWorkbooks.SheetData(
                                "字典清单",
                                List.of("字段", "可用值"),
                                List.of(
                                        List.of("部门", departmentMapper.findAll().stream().map(Department::getName).collect(Collectors.joining("、"))),
                                        List.of("角色", adminRoleMapper.findAll().stream().map(role -> role.getName() + "(" + role.getCode() + ")").collect(Collectors.joining("、"))),
                                        List.of("状态", "启用、禁用")
                                )
                        )
                )
        );
    }

    public ResponseEntity<byte[]> exportExcel() {
        List<List<String>> rows = adminUserMapper.findUsers(null, 10000, 0)
                .stream()
                .map(this::exportRow)
                .toList();
        return ExcelWorkbooks.template(
                "用户导出.xlsx",
                List.of(
                        new ExcelWorkbooks.SheetData("用户导出", List.of("账号", "姓名", "部门", "角色", "状态"), rows),
                        new ExcelWorkbooks.SheetData(
                                "字典清单",
                                List.of("字段", "可用值"),
                                List.of(
                                        List.of("部门", departmentMapper.findAll().stream().map(Department::getName).collect(Collectors.joining("、"))),
                                        List.of("角色", adminRoleMapper.findAll().stream().map(role -> role.getName() + "(" + role.getCode() + ")").collect(Collectors.joining("、"))),
                                        List.of("状态", "启用、禁用")
                                )
                        )
                )
        );
    }

    @Transactional
    public ExcelImportResult importExcel(MultipartFile file) {
        List<String> errors = new ArrayList<>();
        int success = 0;
        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null || ExcelWorkbooks.text(row, 0).isBlank()) {
                    continue;
                }
                try {
                    create(rowToRequest(row));
                    success++;
                } catch (RuntimeException exception) {
                    errors.add("第 " + (rowIndex + 1) + " 行：" + exception.getMessage());
                }
            }
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "读取 Excel 失败");
        }
        return new ExcelImportResult(success, errors.size(), errors);
    }

    @Transactional
    public AdminUserResponse create(UserCreateRequest request) {
        if (adminUserMapper.countByUsername(request.username()) > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "账号已存在");
        }
        ensureDepartmentExists(request.departmentId());
        ensureRolesExist(request.roleIds());

        UserAccount user = new UserAccount();
        user.setDepartmentId(request.departmentId());
        user.setUsername(request.username());
        user.setDisplayName(request.displayName());
        user.setPasswordHash(passwordEncoder.encode(DEFAULT_PASSWORD));
        user.setMustChangePassword(true);
        user.setStatus("ACTIVE");
        userMapper.insert(user);
        replaceRoles(user.getId(), request.roleIds());
        return detail(user.getId());
    }

    @Transactional
    public AdminUserResponse update(Long id, UserUpdateRequest request) {
        ensureDepartmentExists(request.departmentId());
        ensureRolesExist(request.roleIds());
        UserAccount user = findUser(id);
        user.setDepartmentId(request.departmentId());
        user.setDisplayName(request.displayName());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
        replaceRoles(id, request.roleIds());
        return detail(id);
    }

    @Transactional
    public AdminUserResponse changeStatus(Long id, String status) {
        if (!List.of("ACTIVE", "DISABLED").contains(status)) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "用户状态不合法");
        }
        if (id == 1L && "DISABLED".equals(status)) {
            throw new BusinessException(ErrorCode.CONFLICT, "不能禁用种子管理员");
        }
        UserAccount user = findUser(id);
        user.setStatus(status);
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
        return detail(id);
    }

    private void replaceRoles(Long userId, List<Long> roleIds) {
        adminUserMapper.deleteUserRoles(userId);
        roleIds.forEach(roleId -> adminUserMapper.insertUserRole(userId, roleId));
    }

    private UserAccount findUser(Long id) {
        UserAccount user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        return user;
    }

    private void ensureRolesExist(List<Long> roleIds) {
        Set<Long> uniqueRoleIds = new HashSet<>(roleIds);
        long existingRoleCount = adminRoleMapper.countByIds(uniqueRoleIds);
        if (existingRoleCount != uniqueRoleIds.size()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "角色不存在");
        }
    }

    private void ensureDepartmentExists(Long departmentId) {
        if (departmentId != null && adminUserMapper.countActiveDepartmentById(departmentId) == 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "部门不存在或未启用");
        }
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

    private List<String> exportRow(UserAccount user) {
        return List.of(
                user.getUsername(),
                user.getDisplayName(),
                user.getDepartmentId() == null ? "" : adminUserMapper.findDepartmentName(user.getDepartmentId()),
                String.join(",", adminUserMapper.findRoleCodes(user.getId())),
                "ACTIVE".equals(user.getStatus()) ? "启用" : "禁用"
        );
    }

    private AdminUserResponse toResponse(UserAccount user) {
        return new AdminUserResponse(
                user.getId(),
                user.getDepartmentId(),
                user.getDepartmentId() == null ? null : adminUserMapper.findDepartmentName(user.getDepartmentId()),
                user.getUsername(),
                user.getDisplayName(),
                user.getStatus(),
                adminUserMapper.findRoleCodes(user.getId()),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}

