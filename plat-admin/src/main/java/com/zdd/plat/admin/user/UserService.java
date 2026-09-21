package com.zdd.plat.admin.user;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.zdd.plat.admin.common.api.PageQuery;
import com.zdd.plat.admin.common.api.PageResult;
import com.zdd.plat.admin.common.exception.BizException;
import com.zdd.plat.admin.common.query.QueryBuilder;
import com.zdd.plat.admin.common.security.UserPrincipal;
import com.zdd.plat.admin.common.util.KeyGenerator;
import com.zdd.plat.admin.entity.SysUser;
import com.zdd.plat.admin.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static com.zdd.plat.admin.entity.table.SysUserTableDef.SYS_USER;

/**
 * 用户管理（运营侧）：分页检索、启禁用、重置密码。
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final SysUserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public record UserVO(Long id, String username, String nickname, String email, String phone,
                         String role, Long tenantId, Integer status, LocalDateTime createdAt) {}

    public record PasswordResetVO(Long userId, String username, String newPassword) {}

    public PageResult<UserVO> page(PageQuery query, String role) {
        QueryWrapper wrapper = QueryBuilder.whereAll(
                        role != null && !role.isBlank() ? SYS_USER.ROLE.eq(role) : null,
                        query.keyword() != null && !query.keyword().isBlank()
                                ? SYS_USER.USERNAME.like(query.keyword())
                                .or(SYS_USER.NICKNAME.like(query.keyword())) : null)
                .orderBy(SYS_USER.ID.desc());
        Page<SysUser> page = userMapper.paginate(
                Page.of(query.pageNoOrDefault(), query.pageSizeOrDefault()), wrapper);
        List<UserVO> vos = page.getRecords().stream().map(u -> new UserVO(
                u.getId(), u.getUsername(), u.getNickname(), u.getEmail(), u.getPhone(),
                u.getRole(), u.getTenantId(), u.getStatus(), u.getCreatedAt())).toList();
        return PageResult.of(page.getTotalRow(), page.getPageNumber(), page.getPageSize(), vos);
    }

    @Transactional
    public UserVO updateStatus(UserPrincipal operator, Long userId, int status) {
        if (status != 0 && status != 1) {
            throw BizException.badRequest("非法用户状态: " + status);
        }
        SysUser user = requireUser(userId);
        if (user.getId().equals(operator.userId())) {
            throw BizException.badRequest("不能变更自己的账号状态");
        }
        user.setStatus(status);
        userMapper.update(user);
        return toVo(user);
    }

    /** 重置密码：生成随机密码并一次性返回。 */
    @Transactional
    public PasswordResetVO resetPassword(Long userId) {
        SysUser user = requireUser(userId);
        String newPassword = "Pd-" + KeyGenerator.newAppSecret().substring(3, 15);
        user.setPassword(passwordEncoder.encode(newPassword));
        userMapper.update(user);
        return new PasswordResetVO(user.getId(), user.getUsername(), newPassword);
    }

    private SysUser requireUser(Long userId) {
        SysUser user = userMapper.selectOneById(userId);
        if (user == null) {
            throw BizException.notFound("用户不存在: " + userId);
        }
        return user;
    }

    private UserVO toVo(SysUser user) {
        return new UserVO(user.getId(), user.getUsername(), user.getNickname(), user.getEmail(),
                user.getPhone(), user.getRole(), user.getTenantId(), user.getStatus(), user.getCreatedAt());
    }
}
