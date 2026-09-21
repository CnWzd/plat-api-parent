package com.zdd.plat.admin.seed;

import com.mybatisflex.core.query.QueryWrapper;
import com.zdd.plat.admin.app.dto.AppCreateRequest;
import com.zdd.plat.admin.app.AppService;
import com.zdd.plat.admin.approval.dto.AppApiAuthVO;
import com.zdd.plat.admin.approval.dto.AuthApplyRequest;
import com.zdd.plat.admin.approval.ApprovalService;
import com.zdd.plat.admin.apimeta.ApiMetaService;
import com.zdd.plat.admin.apimeta.dto.ApiCreateRequest;
import com.zdd.plat.admin.common.config.PlatProperties;
import com.zdd.plat.admin.common.security.UserPrincipal;
import com.zdd.plat.admin.entity.ApiInfo;
import com.zdd.plat.admin.entity.SysUser;
import com.zdd.plat.admin.entity.Tenant;
import com.zdd.plat.admin.mapper.ApiInfoMapper;
import com.zdd.plat.admin.mapper.AppApiAuthMapper;
import com.zdd.plat.admin.mapper.SysUserMapper;
import com.zdd.plat.admin.mapper.TenantMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.zdd.plat.admin.entity.table.ApiInfoTableDef.API_INFO;
import static com.zdd.plat.admin.entity.table.SysUserTableDef.SYS_USER;
import static com.zdd.plat.admin.entity.table.TenantTableDef.TENANT;

/**
 * 演示数据初始化（幂等：仅在对应数据不存在时写入）。
 * <ul>
 *   <li>管理员账号：admin / admin123</li>
 *   <li>开发者账号：demo / demo123（归属演示租户 T001）</li>
 *   <li>演示 API：weather.current / demo.time / demo.echo</li>
 *   <li>演示应用：Demo 应用（已授权 weather.current 与 demo.echo）</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements ApplicationRunner {

    private final TenantMapper tenantMapper;
    private final SysUserMapper userMapper;
    private final ApiInfoMapper apiInfoMapper;
    private final AppApiAuthMapper appApiAuthMapper;
    private final ApiMetaService apiMetaService;
    private final AppService appService;
    private final ApprovalService approvalService;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Tenant demoTenant = ensureTenant();
        SysUser admin = ensureAdmin();
        SysUser demo = ensureDeveloper(demoTenant);
        ensureApis();
        ensureDemoApp(demo, admin);
    }

    private Tenant ensureTenant() {
        Tenant tenant = tenantMapper.selectOneByQuery(QueryWrapper.create()
                .where(TENANT.TENANT_CODE.eq("T001")));
        if (tenant != null) {
            return tenant;
        }
        tenant = new Tenant();
        tenant.setTenantCode("T001");
        tenant.setTenantName("演示租户");
        tenant.setContactName("张三");
        tenant.setContactPhone("13800000000");
        tenant.setStatus(1);
        tenantMapper.insert(tenant);
        log.info("[Seed] 创建演示租户 T001");
        return tenant;
    }

    private SysUser ensureAdmin() {
        SysUser admin = userMapper.selectOneByQuery(QueryWrapper.create()
                .where(SYS_USER.USERNAME.eq("admin")));
        if (admin != null) {
            return admin;
        }
        admin = new SysUser();
        admin.setUsername("admin");
        admin.setPassword(passwordEncoder.encode("admin123"));
        admin.setNickname("平台管理员");
        admin.setRole("ADMIN");
        admin.setStatus(1);
        userMapper.insert(admin);
        log.info("[Seed] 创建平台管理员 admin/admin123");
        return admin;
    }

    private SysUser ensureDeveloper(Tenant tenant) {
        SysUser demo = userMapper.selectOneByQuery(QueryWrapper.create()
                .where(SYS_USER.USERNAME.eq("demo")));
        if (demo != null) {
            return demo;
        }
        demo = new SysUser();
        demo.setUsername("demo");
        demo.setPassword(passwordEncoder.encode("demo123"));
        demo.setNickname("演示开发者");
        demo.setEmail("demo@example.com");
        demo.setRole("DEVELOPER");
        demo.setTenantId(tenant.getId());
        demo.setStatus(1);
        userMapper.insert(demo);
        log.info("[Seed] 创建演示开发者 demo/demo123");
        return demo;
    }

    private void ensureApis() {
        if (apiInfoMapper.selectCountByQuery(QueryWrapper.create().where(API_INFO.STATUS.eq(1))) > 0) {
            return;
        }
        apiMetaService.create(new ApiCreateRequest("weather.current", "实时天气查询", "气象",
                "GET", "/openapi/weather/current", "v1", "按城市查询实时天气（演示数据）", "SIGN"));
        apiMetaService.create(new ApiCreateRequest("demo.time", "服务器时间", "通用",
                "GET", "/openapi/demo/time", "v1", "返回服务器当前时间", "NONE"));
        apiMetaService.create(new ApiCreateRequest("demo.echo", "回显接口", "通用",
                "POST", "/openapi/demo/echo", "v1", "回显请求体，用于连通性测试", "SIGN"));
        // 演示 API 直接置为上架
        apiInfoMapper.selectListByQuery(QueryWrapper.create()).forEach(api -> {
            api.setStatus(1);
            apiInfoMapper.update(api);
        });
        log.info("[Seed] 创建 3 个演示 API 并上架");
    }

    private void ensureDemoApp(SysUser demo, SysUser admin) {
        boolean hasAuth = appApiAuthMapper.selectCountByQuery(QueryWrapper.create()) > 0;
        if (hasAuth) {
            return;
        }
        UserPrincipal demoPrincipal = new UserPrincipal(demo.getId(), demo.getUsername(), demo.getRole(), demo.getTenantId());
        var reveal = appService.createApp(demoPrincipal,
                new AppCreateRequest("Demo 演示应用", null, "端到端链路演示应用"));
        log.info("[Seed] 创建演示应用 appKey={}", reveal.appKey());

        // 为演示应用申请两个 API 并由管理员直接审批通过
        List<ApiInfo> apis = apiInfoMapper.selectListByQuery(QueryWrapper.create()
                .where(API_INFO.API_CODE.in("weather.current", "demo.echo")));
        List<Long> apiIds = apis.stream().map(ApiInfo::getId).toList();
        List<AppApiAuthVO> applied = approvalService.apply(demoPrincipal, reveal.appId(),
                new AuthApplyRequest(apiIds, "演示应用初始化授权"));

        UserPrincipal adminPrincipal = new UserPrincipal(admin.getId(), admin.getUsername(), admin.getRole(), null);
        applied.forEach(auth -> approvalService.approve(adminPrincipal, auth.id()));
        log.info("[Seed] 演示应用已授权 {} 个 API（已审批通过）", applied.size());
    }
}
