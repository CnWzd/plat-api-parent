package com.zdd.plat.admin.approval;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.zdd.plat.admin.approval.dto.AppApiAuthVO;
import com.zdd.plat.admin.approval.dto.ApprovalDecisionRequest;
import com.zdd.plat.admin.approval.dto.AuthApplyRequest;
import com.zdd.plat.admin.common.api.PageQuery;
import com.zdd.plat.admin.common.api.PageResult;
import com.zdd.plat.admin.common.exception.BizException;
import com.zdd.plat.admin.common.query.QueryBuilder;
import com.zdd.plat.admin.common.security.UserPrincipal;
import com.zdd.plat.admin.entity.AppApiAuth;
import com.zdd.plat.admin.entity.AppInfo;
import com.zdd.plat.admin.entity.ApiInfo;
import com.zdd.plat.admin.entity.SysUser;
import com.zdd.plat.admin.internal.InternalSupportService;
import com.zdd.plat.admin.mapper.AppApiAuthMapper;
import com.zdd.plat.admin.mapper.AppInfoMapper;
import com.zdd.plat.admin.mapper.ApiInfoMapper;
import com.zdd.plat.admin.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.zdd.plat.admin.entity.table.AppApiAuthTableDef.APP_API_AUTH;

/**
 * 授权审批流：开发者申请 -> PENDING -> 管理员 APPROVED / REJECTED。
 * 审批结果变更实时失效网关鉴权缓存。
 */
@Service
@RequiredArgsConstructor
public class ApprovalService {

    private final AppApiAuthMapper authMapper;
    private final AppInfoMapper appInfoMapper;
    private final ApiInfoMapper apiInfoMapper;
    private final SysUserMapper userMapper;
    private final InternalSupportService internalSupportService;

    // ------------------------------------------------------------------
    // 开发者侧
    // ------------------------------------------------------------------

    /** 应用下的全部授权记录。 */
    public List<AppApiAuthVO> listByApp(UserPrincipal principal, Long appId) {
        requireOwnedApp(principal, appId);
        List<AppApiAuth> auths = authMapper.selectListByQuery(QueryWrapper.create()
                .where(APP_API_AUTH.APP_ID.eq(appId))
                .orderBy(APP_API_AUTH.ID.desc()));
        return assemble(auths);
    }

    /** 批量提交授权申请（跳过已 APPROVED / PENDING 的组合）。 */
    @Transactional
    public List<AppApiAuthVO> apply(UserPrincipal principal, Long appId, AuthApplyRequest request) {
        AppInfo app = requireOwnedApp(principal, appId);
        if (app.getStatus() != 1) {
            throw BizException.badRequest("应用当前状态不可申请接口授权");
        }

        List<ApiInfo> apis = apiInfoMapper.selectListByIds(request.apiIds());
        if (apis.size() != request.apiIds().size()) {
            throw BizException.badRequest("存在无效的 API ID");
        }
        for (ApiInfo api : apis) {
            if (api.getStatus() != 1) {
                throw BizException.badRequest("API 未上架，无法申请: " + api.getApiCode());
            }
        }

        List<AppApiAuth> existing = authMapper.selectListByQuery(QueryWrapper.create()
                .where(APP_API_AUTH.APP_ID.eq(appId))
                .and(APP_API_AUTH.API_ID.in(request.apiIds())));
        Set<Long> occupied = existing.stream()
                .filter(a -> a.getStatus() == 0 || a.getStatus() == 1)
                .map(AppApiAuth::getApiId)
                .collect(Collectors.toSet());

        List<AppApiAuth> created = new ArrayList<>();
        for (ApiInfo api : apis) {
            if (occupied.contains(api.getId())) {
                continue;
            }
            // 历史上被驳回的记录：删除旧记录重新申请，避免唯一键冲突
            existing.stream()
                    .filter(a -> a.getApiId().equals(api.getId()) && a.getStatus() == 2)
                    .findFirst()
                    .ifPresent(authMapper::delete);

            AppApiAuth auth = new AppApiAuth();
            auth.setAppId(appId);
            auth.setApiId(api.getId());
            auth.setStatus(0);
            auth.setApplyRemark(request.applyRemark());
            auth.setAppliedAt(LocalDateTime.now());
            authMapper.insert(auth);
            created.add(auth);
        }

        if (created.isEmpty()) {
            throw BizException.badRequest("所选 API 均已授权或已在审批中");
        }
        return assemble(created);
    }

    // ------------------------------------------------------------------
    // 运营侧
    // ------------------------------------------------------------------

    /** 审批工作台：按状态分页（默认 PENDING）。 */
    public PageResult<AppApiAuthVO> consolePage(PageQuery query, Integer status) {
        QueryWrapper wrapper = QueryBuilder.whereAll(
                        status != null ? APP_API_AUTH.STATUS.eq(status) : null)
                .orderBy(APP_API_AUTH.ID.desc());
        Page<AppApiAuth> page = authMapper.paginate(
                Page.of(query.pageNoOrDefault(), query.pageSizeOrDefault()), wrapper);
        return PageResult.of(page.getTotalRow(), page.getPageNumber(), page.getPageSize(),
                assemble(page.getRecords()));
    }

    @Transactional
    public AppApiAuthVO approve(UserPrincipal admin, Long authId) {
        AppApiAuth auth = requireAuth(authId);
        if (auth.getStatus() != 0) {
            throw BizException.badRequest("该申请已处理，无需重复审批");
        }
        auth.setStatus(1);
        auth.setApprovedBy(admin.userId());
        auth.setApprovedAt(LocalDateTime.now());
        authMapper.update(auth);
        evictRelatedCache(auth.getAppId());
        return assemble(List.of(auth)).get(0);
    }

    @Transactional
    public AppApiAuthVO reject(UserPrincipal admin, Long authId, ApprovalDecisionRequest request) {
        AppApiAuth auth = requireAuth(authId);
        if (auth.getStatus() != 0) {
            throw BizException.badRequest("该申请已处理，无需重复审批");
        }
        auth.setStatus(2);
        auth.setRejectReason(request.reason());
        auth.setApprovedBy(admin.userId());
        auth.setApprovedAt(LocalDateTime.now());
        authMapper.update(auth);
        return assemble(List.of(auth)).get(0);
    }

    // ------------------------------------------------------------------
    // 内部方法
    // ------------------------------------------------------------------

    private AppInfo requireOwnedApp(UserPrincipal principal, Long appId) {
        AppInfo app = appInfoMapper.selectOneById(appId);
        if (app == null) {
            throw BizException.notFound("应用不存在: " + appId);
        }
        if (!app.getOwnerUserId().equals(principal.userId()) && !principal.isAdmin()) {
            throw BizException.forbidden("只能操作自己的应用");
        }
        return app;
    }

    private AppApiAuth requireAuth(Long authId) {
        AppApiAuth auth = authMapper.selectOneById(authId);
        if (auth == null) {
            throw BizException.notFound("授权记录不存在: " + authId);
        }
        return auth;
    }

    private void evictRelatedCache(Long appId) {
        AppInfo app = appInfoMapper.selectOneById(appId);
        if (app != null) {
            internalSupportService.evictAppAuth(app.getAppKey());
        }
    }

    /** 批量装配关联实体（应用 / API / 申请人），避免 N+1：每组实体各一次批量查询。 */
    private List<AppApiAuthVO> assemble(List<AppApiAuth> auths) {
        if (auths.isEmpty()) {
            return List.of();
        }
        Map<Long, AppInfo> apps = batchLoad(auths.stream().map(AppApiAuth::getAppId).collect(Collectors.toSet()),
                appInfoMapper::selectListByIds, AppInfo::getId);
        Map<Long, ApiInfo> apis = batchLoad(auths.stream().map(AppApiAuth::getApiId).collect(Collectors.toSet()),
                apiInfoMapper::selectListByIds, ApiInfo::getId);
        Set<Long> userIds = apps.values().stream().map(AppInfo::getOwnerUserId).collect(Collectors.toSet());
        Map<Long, SysUser> users = batchLoad(userIds, userMapper::selectListByIds, SysUser::getId);

        return auths.stream().map(auth -> {
            AppInfo app = apps.get(auth.getAppId());
            ApiInfo api = apis.get(auth.getApiId());
            String owner = app == null ? null
                    : Optional.ofNullable(users.get(app.getOwnerUserId())).map(SysUser::getUsername).orElse(null);
            return new AppApiAuthVO(
                    auth.getId(), auth.getAppId(),
                    app == null ? null : app.getAppName(),
                    app == null ? null : app.getAppKey(),
                    auth.getApiId(),
                    api == null ? null : api.getApiCode(),
                    api == null ? null : api.getApiName(),
                    api == null ? null : api.getMethod(),
                    api == null ? null : api.getPath(),
                    auth.getStatus(), auth.getApplyRemark(), auth.getRejectReason(),
                    auth.getAppliedAt(), auth.getApprovedAt(), owner);
        }).toList();
    }

    private <T> Map<Long, T> batchLoad(Set<Long> ids,
                                       Function<Set<Long>, List<T>> loader,
                                       Function<T, Long> keyGetter) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        return loader.apply(ids).stream().collect(Collectors.toMap(keyGetter, Function.identity()));
    }
}
