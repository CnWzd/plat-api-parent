package com.zdd.plat.admin.internal;

import com.mybatisflex.core.query.QueryWrapper;
import com.zdd.plat.admin.common.cache.TwoLevelCache;
import com.zdd.plat.admin.common.cache.TwoLevelCacheManager;
import com.zdd.plat.admin.common.crypto.CryptoService;
import com.zdd.plat.admin.entity.AppApiAuth;
import com.zdd.plat.admin.entity.AppInfo;
import com.zdd.plat.admin.entity.ApiInfo;
import com.zdd.plat.admin.entity.log.ApiCallLog;
import com.zdd.plat.admin.internal.dto.AppAuthBundle;
import com.zdd.plat.admin.internal.dto.CallLogItem;
import com.zdd.plat.admin.mapper.ApiInfoMapper;
import com.zdd.plat.admin.mapper.AppApiAuthMapper;
import com.zdd.plat.admin.mapper.AppInfoMapper;
import com.zdd.plat.admin.mapper.ApiCallLogMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.zdd.plat.admin.entity.table.ApiInfoTableDef.API_INFO;
import static com.zdd.plat.admin.entity.table.AppApiAuthTableDef.APP_API_AUTH;
import static com.zdd.plat.admin.entity.table.AppInfoTableDef.APP_INFO;

/**
 * 网关支撑服务：
 * <ul>
 *   <li>appKey → 应用身份 + 已授权 API（两级缓存：Caffeine L1 + Redis L2）</li>
 *   <li>批量调用日志落库（日志库）</li>
 *   <li>数据变更时的缓存失效入口</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InternalSupportService {

    private final TwoLevelCacheManager cacheManager;
    private final AppInfoMapper appInfoMapper;
    private final AppApiAuthMapper appApiAuthMapper;
    private final ApiInfoMapper apiInfoMapper;
    private final ApiCallLogMapper callLogMapper;
    private final CryptoService cryptoService;

    private TwoLevelCache<AppAuthBundle> appAuthCache;

    @PostConstruct
    void initCaches() {
        this.appAuthCache = cacheManager.create("appauth", AppAuthBundle.class);
    }

    /**
     * 查询网关鉴权数据包（未命中缓存时回源 MySQL）。
     */
    public AppAuthBundle getAppAuthBundle(String appKey) {
        return appAuthCache.get(appKey, () -> loadBundle(appKey));
    }

    private AppAuthBundle loadBundle(String appKey) {
        AppInfo app = appInfoMapper.selectOneByQuery(QueryWrapper.create()
                .where(APP_INFO.APP_KEY.eq(appKey)));
        if (app == null) {
            return null;
        }

        // 已授权 API：授权状态 APPROVED 且 API 处于上架状态
        Set<Long> authorized = appApiAuthMapper.selectListByQuery(QueryWrapper.create()
                        .select(APP_API_AUTH.API_ID)
                        .where(APP_API_AUTH.APP_ID.eq(app.getId()))
                        .and(APP_API_AUTH.STATUS.eq(1)))
                .stream()
                .map(AppApiAuth::getApiId)
                .collect(Collectors.toSet());

        List<AppAuthBundle.AuthorizedApi> authorizedApis = List.of();
        if (!authorized.isEmpty()) {
            authorizedApis = apiInfoMapper.selectListByQuery(QueryWrapper.create()
                            .where(API_INFO.ID.in(authorized))
                            .and(API_INFO.STATUS.eq(1)))
                    .stream()
                    .map(api -> new AppAuthBundle.AuthorizedApi(
                            api.getId(), api.getApiCode(), api.getMethod(), api.getPath(), api.getAuthType()))
                    .toList();
        }

        return new AppAuthBundle(
                app.getId(),
                app.getAppKey(),
                cryptoService.decrypt(app.getAppSecret()),
                app.getStatus(),
                app.getRateLimitQps(),
                authorizedApis);
    }

    /**
     * 批量写入调用日志（网关攒批上报）。
     */
    public int saveCallLogs(List<CallLogItem> items) {
        if (items == null || items.isEmpty()) {
            return 0;
        }
        List<ApiCallLog> logs = items.stream().map(item -> {
            ApiCallLog log = new ApiCallLog();
            log.setAppId(item.appId());
            log.setAppKey(item.appKey());
            log.setApiId(item.apiId());
            log.setApiCode(item.apiCode());
            log.setMethod(item.method());
            log.setPath(item.path());
            log.setStatusCode(item.statusCode());
            log.setLatencyMs(item.latencyMs());
            log.setClientIp(item.clientIp());
            log.setGatewayResult(item.gatewayResult());
            log.setErrorMsg(item.errorMsg());
            log.setCalledAt(LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(item.calledAtEpochMs()), ZoneId.systemDefault()));
            return log;
        }).toList();
        callLogMapper.insertBatch(logs);
        return logs.size();
    }

    /** 应用身份/密钥/授权变更时失效缓存（L1+L2）。 */
    public void evictAppAuth(String appKey) {
        if (appKey != null) {
            appAuthCache.evict(appKey);
        }
    }
}
