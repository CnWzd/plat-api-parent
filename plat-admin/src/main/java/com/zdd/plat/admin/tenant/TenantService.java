package com.zdd.plat.admin.tenant;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.zdd.plat.admin.common.api.PageQuery;
import com.zdd.plat.admin.common.api.PageResult;
import com.zdd.plat.admin.common.exception.BizException;
import com.zdd.plat.admin.common.query.QueryBuilder;
import com.zdd.plat.admin.entity.SysUser;
import com.zdd.plat.admin.entity.Tenant;
import com.zdd.plat.admin.mapper.SysUserMapper;
import com.zdd.plat.admin.mapper.TenantMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.zdd.plat.admin.entity.table.SysUserTableDef.SYS_USER;
import static com.zdd.plat.admin.entity.table.TenantTableDef.TENANT;

/**
 * 租户管理（运营侧）。
 */
@Service
@RequiredArgsConstructor
public class TenantService {

    private final TenantMapper tenantMapper;
    private final SysUserMapper userMapper;

    public record TenantVO(Long id, String tenantCode, String tenantName, String contactName,
                           String contactPhone, Integer status, LocalDateTime createdAt,
                           long developerCount) {}

    public record TenantCreateRequest(
            @NotBlank(message = "租户编码不能为空")
            @Pattern(regexp = "^[A-Za-z0-9-]{2,32}$", message = "租户编码为 2~32 位字母/数字/横线")
            String tenantCode,

            @NotBlank(message = "租户名称不能为空")
            @Size(max = 128)
            String tenantName,

            @Size(max = 64)
            String contactName,

            @Size(max = 32)
            String contactPhone) {}

    public record TenantUpdateRequest(
            @NotBlank(message = "租户名称不能为空")
            @Size(max = 128)
            String tenantName,

            @Size(max = 64)
            String contactName,

            @Size(max = 32)
            String contactPhone) {}

    public PageResult<TenantVO> page(PageQuery query) {
        QueryWrapper wrapper = QueryBuilder.whereAll(
                        query.keyword() != null && !query.keyword().isBlank()
                                ? TENANT.TENANT_NAME.like(query.keyword())
                                .or(TENANT.TENANT_CODE.like(query.keyword())) : null)
                .orderBy(TENANT.ID.desc());
        Page<Tenant> page = tenantMapper.paginate(
                Page.of(query.pageNoOrDefault(), query.pageSizeOrDefault()), wrapper);
        return PageResult.of(page.getTotalRow(), page.getPageNumber(), page.getPageSize(),
                toVos(page.getRecords()));
    }

    @Transactional
    public TenantVO create(TenantCreateRequest request) {
        Long exists = tenantMapper.selectCountByQuery(QueryWrapper.create()
                .where(TENANT.TENANT_CODE.eq(request.tenantCode())));
        if (exists != null && exists > 0) {
            throw BizException.conflict("租户编码已存在: " + request.tenantCode());
        }
        Tenant tenant = new Tenant();
        tenant.setTenantCode(request.tenantCode());
        tenant.setTenantName(request.tenantName());
        tenant.setContactName(request.contactName());
        tenant.setContactPhone(request.contactPhone());
        tenant.setStatus(1);
        tenantMapper.insert(tenant);
        return toVos(List.of(tenant)).get(0);
    }

    @Transactional
    public TenantVO update(Long id, TenantUpdateRequest request) {
        Tenant tenant = requireTenant(id);
        tenant.setTenantName(request.tenantName());
        tenant.setContactName(request.contactName());
        tenant.setContactPhone(request.contactPhone());
        tenantMapper.update(tenant);
        return toVos(List.of(tenant)).get(0);
    }

    @Transactional
    public TenantVO updateStatus(Long id, int status) {
        if (status != 0 && status != 1) {
            throw BizException.badRequest("非法租户状态: " + status);
        }
        Tenant tenant = requireTenant(id);
        tenant.setStatus(status);
        tenantMapper.update(tenant);
        return toVos(List.of(tenant)).get(0);
    }

    private Tenant requireTenant(Long id) {
        Tenant tenant = tenantMapper.selectOneById(id);
        if (tenant == null) {
            throw BizException.notFound("租户不存在: " + id);
        }
        return tenant;
    }

    /** 批量装配各租户下开发者数量（单次分组查询，避免 N+1）。 */
    private List<TenantVO> toVos(List<Tenant> tenants) {
        if (tenants.isEmpty()) {
            return List.of();
        }
        List<Long> tenantIds = tenants.stream().map(Tenant::getId).toList();
        Map<Long, Long> devCounts = userMapper.selectListByQuery(QueryWrapper.create()
                        .select(SYS_USER.TENANT_ID)
                        .where(SYS_USER.TENANT_ID.in(tenantIds)))
                .stream()
                .collect(Collectors.groupingBy(SysUser::getTenantId, Collectors.counting()));

        return tenants.stream().map(t -> new TenantVO(
                t.getId(), t.getTenantCode(), t.getTenantName(), t.getContactName(),
                t.getContactPhone(), t.getStatus(), t.getCreatedAt(),
                devCounts.getOrDefault(t.getId(), 0L))).toList();
    }
}
