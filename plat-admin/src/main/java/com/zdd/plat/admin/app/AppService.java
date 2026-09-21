package com.zdd.plat.admin.app;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.zdd.plat.admin.app.dto.AppConsolePatchRequest;
import com.zdd.plat.admin.app.dto.AppCreateRequest;
import com.zdd.plat.admin.app.dto.AppUpdateRequest;
import com.zdd.plat.admin.app.dto.AppVO;
import com.zdd.plat.admin.app.dto.SecretRevealVO;
import com.zdd.plat.admin.common.api.PageQuery;
import com.zdd.plat.admin.common.api.PageResult;
import com.zdd.plat.admin.common.crypto.CryptoService;
import com.zdd.plat.admin.common.exception.BizException;
import com.zdd.plat.admin.common.query.QueryBuilder;
import com.zdd.plat.admin.common.security.UserPrincipal;
import com.zdd.plat.admin.common.util.KeyGenerator;
import com.zdd.plat.admin.entity.AppApiAuth;
import com.zdd.plat.admin.entity.AppInfo;
import com.zdd.plat.admin.internal.InternalSupportService;
import com.zdd.plat.admin.mapper.AppApiAuthMapper;
import com.zdd.plat.admin.mapper.AppInfoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.zdd.plat.admin.entity.table.AppApiAuthTableDef.APP_API_AUTH;
import static com.zdd.plat.admin.entity.table.AppInfoTableDef.APP_INFO;

/**
 * 应用与密钥管理。
 * <p>SK 采用 AES-GCM 加密落库，明文仅在创建/重置时返回一次。</p>
 */
@Service
@RequiredArgsConstructor
public class AppService {

    private final AppInfoMapper appInfoMapper;
    private final AppApiAuthMapper appApiAuthMapper;
    private final CryptoService cryptoService;
    private final InternalSupportService internalSupportService;

    // ------------------------------------------------------------------
    // 开发者侧
    // ------------------------------------------------------------------

    public PageResult<AppVO> pageMyApps(UserPrincipal principal, PageQuery query) {
        QueryWrapper wrapper = QueryBuilder.whereAll(
                        APP_INFO.OWNER_USER_ID.eq(principal.userId()),
                        query.keyword() != null && !query.keyword().isBlank()
                                ? APP_INFO.APP_NAME.like(query.keyword()) : null)
                .orderBy(APP_INFO.ID.desc());
        Page<AppInfo> page = appInfoMapper.paginate(
                Page.of(query.pageNoOrDefault(), query.pageSizeOrDefault()), wrapper);
        return PageResult.of(page.getTotalRow(), page.getPageNumber(), page.getPageSize(),
                toVos(page.getRecords()));
    }

    @Transactional
    public SecretRevealVO createApp(UserPrincipal principal, AppCreateRequest request) {
        AppInfo app = new AppInfo();
        app.setAppName(request.appName());
        app.setAppKey(KeyGenerator.newAppKey());
        String secret = KeyGenerator.newAppSecret();
        app.setAppSecret(cryptoService.encrypt(secret));
        app.setTenantId(principal.tenantId());
        app.setOwnerUserId(principal.userId());
        app.setRateLimitQps(100);
        app.setCallbackUrl(request.callbackUrl());
        app.setRemark(request.remark());
        app.setStatus(1);
        appInfoMapper.insert(app);
        return reveal(app, secret);
    }

    public AppVO getMyApp(UserPrincipal principal, Long appId) {
        AppInfo app = requireOwnedApp(principal, appId);
        List<AppVO> vos = toVos(List.of(app));
        return vos.get(0);
    }

    @Transactional
    public AppVO updateMyApp(UserPrincipal principal, Long appId, AppUpdateRequest request) {
        AppInfo app = requireOwnedApp(principal, appId);
        app.setAppName(request.appName());
        app.setCallbackUrl(request.callbackUrl());
        app.setRemark(request.remark());
        appInfoMapper.update(app);
        return toVos(List.of(app)).get(0);
    }

    @Transactional
    public SecretRevealVO resetSecret(UserPrincipal principal, Long appId) {
        AppInfo app = requireOwnedApp(principal, appId);
        String secret = KeyGenerator.newAppSecret();
        app.setAppSecret(cryptoService.encrypt(secret));
        appInfoMapper.update(app);
        internalSupportService.evictAppAuth(app.getAppKey());
        return reveal(app, secret);
    }

    // ------------------------------------------------------------------
    // 运营侧
    // ------------------------------------------------------------------

    public PageResult<AppVO> pageAllApps(PageQuery query) {
        String kw = query.keyword();
        QueryWrapper wrapper = QueryBuilder.whereAll(
                        kw != null && !kw.isBlank()
                                ? APP_INFO.APP_NAME.like(kw).or(APP_INFO.APP_KEY.like(kw)) : null)
                .orderBy(APP_INFO.ID.desc());
        Page<AppInfo> page = appInfoMapper.paginate(
                Page.of(query.pageNoOrDefault(), query.pageSizeOrDefault()), wrapper);
        return PageResult.of(page.getTotalRow(), page.getPageNumber(), page.getPageSize(),
                toVos(page.getRecords()));
    }

    @Transactional
    public AppVO updateAppStatus(Long appId, int status) {
        if (status != 0 && status != 1 && status != 2) {
            throw BizException.badRequest("非法应用状态: " + status);
        }
        AppInfo app = requireApp(appId);
        app.setStatus(status);
        appInfoMapper.update(app);
        internalSupportService.evictAppAuth(app.getAppKey());
        return toVos(List.of(app)).get(0);
    }

    @Transactional
    public AppVO updateRateLimit(Long appId, AppConsolePatchRequest request) {
        AppInfo app = requireApp(appId);
        app.setRateLimitQps(request.rateLimitQps());
        appInfoMapper.update(app);
        internalSupportService.evictAppAuth(app.getAppKey());
        return toVos(List.of(app)).get(0);
    }

    // ------------------------------------------------------------------
    // 内部方法
    // ------------------------------------------------------------------

    private AppInfo requireApp(Long appId) {
        AppInfo app = appInfoMapper.selectOneById(appId);
        if (app == null) {
            throw BizException.notFound("应用不存在: " + appId);
        }
        return app;
    }

    private AppInfo requireOwnedApp(UserPrincipal principal, Long appId) {
        AppInfo app = requireApp(appId);
        if (!app.getOwnerUserId().equals(principal.userId()) && !principal.isAdmin()) {
            throw BizException.forbidden("只能操作自己的应用");
        }
        return app;
    }

    private SecretRevealVO reveal(AppInfo app, String plainSecret) {
        return new SecretRevealVO(app.getId(), app.getAppName(), app.getAppKey(), plainSecret,
                "SecretKey 仅此一次完整展示，请立即妥善保存；关闭后平台不再提供明文查询。", LocalDateTime.now());
    }

    private List<AppVO> toVos(List<AppInfo> apps) {
        if (apps.isEmpty()) {
            return List.of();
        }
        List<Long> appIds = apps.stream().map(AppInfo::getId).toList();
        Map<Long, List<AppApiAuth>> grouped = appApiAuthMapper.selectListByQuery(QueryWrapper.create()
                        .where(APP_API_AUTH.APP_ID.in(appIds)))
                .stream()
                .collect(Collectors.groupingBy(AppApiAuth::getAppId));

        return apps.stream().map(app -> {
            List<AppApiAuth> auths = grouped.getOrDefault(app.getId(), List.of());
            long approved = auths.stream().filter(a -> a.getStatus() == 1).count();
            long pending = auths.stream().filter(a -> a.getStatus() == 0).count();
            return new AppVO(
                    app.getId(), app.getAppName(), app.getAppKey(), maskSecret(app.getAppKey()),
                    app.getStatus(), app.getRateLimitQps(), app.getCallbackUrl(), app.getRemark(),
                    app.getTenantId(), app.getCreatedAt(), approved, pending);
        }).toList();
    }

    /** SK 不可逆展示：仅展示与 AK 同长度的掩码（DB 中为密文，无法还原）。 */
    private String maskSecret(String appKey) {
        return "*".repeat(appKey.length() + 3);
    }
}
