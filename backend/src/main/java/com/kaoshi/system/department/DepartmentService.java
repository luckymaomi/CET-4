package com.kaoshi.system.department;

import com.kaoshi.common.api.ErrorCode;
import com.kaoshi.common.exception.BusinessException;
import com.kaoshi.system.department.domain.Department;
import com.kaoshi.system.department.dto.DepartmentResponse;
import com.kaoshi.system.department.dto.DepartmentSaveRequest;
import com.kaoshi.system.department.mapper.DepartmentMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DepartmentService {
    private final DepartmentMapper departmentMapper;

    public DepartmentService(DepartmentMapper departmentMapper) {
        this.departmentMapper = departmentMapper;
    }

    public List<DepartmentResponse> tree() {
        List<Department> departments = departmentMapper.findAll();
        Map<Long, List<Department>> children = new HashMap<>();
        for (Department department : departments) {
            children.computeIfAbsent(department.getParentId(), ignored -> new ArrayList<>()).add(department);
        }
        return children.getOrDefault(null, List.of()).stream()
                .map(department -> toResponse(department, children))
                .toList();
    }

    @Transactional
    public DepartmentResponse create(DepartmentSaveRequest request) {
        validateSave(null, request);
        Department department = new Department();
        fill(department, request);
        departmentMapper.insertDepartment(department);
        return toResponse(departmentMapper.findById(department.getId()), Map.of());
    }

    @Transactional
    public DepartmentResponse update(Long id, DepartmentSaveRequest request) {
        Department department = findDepartment(id);
        validateSave(id, request);
        fill(department, request);
        departmentMapper.updateDepartment(department);
        return toResponse(departmentMapper.findById(id), Map.of());
    }

    @Transactional
    public void delete(Long id) {
        findDepartment(id);
        if (departmentMapper.countChildren(id) > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "部门存在下级，不能删除");
        }
        departmentMapper.deleteDepartment(id);
    }

    private void validateSave(Long id, DepartmentSaveRequest request) {
        if (request.name() == null || request.name().isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "部门名称不能为空");
        }
        if (request.code() == null || request.code().isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "部门编码不能为空");
        }
        if (!List.of("ACTIVE", "DISABLED").contains(request.status())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "部门状态不合法");
        }
        if (request.parentId() != null) {
            if (id != null && id.equals(request.parentId())) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED, "上级部门不能选择自己");
            }
            findDepartment(request.parentId());
            if (id != null && isDescendant(request.parentId(), id)) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED, "上级部门不能选择自己的下级部门");
            }
        }
        int duplicate = id == null ? departmentMapper.countByCode(request.code()) : departmentMapper.countByCodeExceptId(request.code(), id);
        if (duplicate > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "部门编码已存在");
        }
    }

    private boolean isDescendant(Long candidateParentId, Long departmentId) {
        Map<Long, Department> departments = new HashMap<>();
        for (Department department : departmentMapper.findAll()) {
            departments.put(department.getId(), department);
        }
        Long currentId = candidateParentId;
        while (currentId != null) {
            if (departmentId.equals(currentId)) {
                return true;
            }
            Department current = departments.get(currentId);
            currentId = current == null ? null : current.getParentId();
        }
        return false;
    }

    private Department findDepartment(Long id) {
        Department department = departmentMapper.findById(id);
        if (department == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "部门不存在");
        }
        return department;
    }

    private void fill(Department department, DepartmentSaveRequest request) {
        department.setParentId(request.parentId());
        department.setName(request.name());
        department.setCode(request.code());
        department.setDescription(request.description());
        department.setStatus(request.status());
    }

    private DepartmentResponse toResponse(Department department, Map<Long, List<Department>> children) {
        return new DepartmentResponse(
                department.getId(),
                department.getParentId(),
                department.getName(),
                department.getCode(),
                department.getDescription(),
                department.getStatus(),
                children.getOrDefault(department.getId(), List.of()).stream()
                        .map(child -> toResponse(child, children))
                        .toList()
        );
    }
}
