package com.kaoshi.admin.user;

import com.kaoshi.admin.role.mapper.AdminRoleMapper;
import com.kaoshi.admin.user.dto.AdminUserResponse;
import com.kaoshi.admin.user.dto.UserCreateRequest;
import com.kaoshi.admin.user.dto.UserUpdateRequest;
import com.kaoshi.admin.user.mapper.AdminUserMapper;
import com.kaoshi.common.api.ErrorCode;
import com.kaoshi.common.exception.BusinessException;
import com.kaoshi.common.excel.ExcelImportResult;
import com.kaoshi.common.page.PageRequest;
import com.kaoshi.common.page.PageResponse;
import com.kaoshi.system.department.mapper.DepartmentMapper;
import com.kaoshi.user.domain.UserAccount;
import com.kaoshi.user.mapper.UserMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class AdminUserService {
    public static final String DEFAULT_PASSWORD = "123456";

    private final UserMapper userMapper;
    private final AdminUserMapper adminUserMapper;
    private final AdminRoleMapper adminRoleMapper;
    private final PasswordEncoder passwordEncoder;
    private final AdminUserExcelService excelService;

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
        this.passwordEncoder = passwordEncoder;
        this.excelService = new AdminUserExcelService(adminUserMapper, adminRoleMapper, departmentMapper);
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
        return excelService.template();
    }

    public ResponseEntity<byte[]> exportExcel() {
        return excelService.exportExcel();
    }

    @Transactional
    public ExcelImportResult importExcel(MultipartFile file) {
        List<String> errors = new ArrayList<>();
        int success = 0;
        for (AdminUserExcelService.UserImportRow row : excelService.readImportRows(file)) {
            try {
                create(row.request());
                success++;
            } catch (RuntimeException exception) {
                errors.add("第 " + row.rowNumber() + " 行：" + exception.getMessage());
            }
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
