package com.gsecp.user.service;

import com.gsecp.common.BusinessException;
import com.gsecp.common.ResultCode;
import com.gsecp.config.JwtProperties;
import com.gsecp.security.JwtTokenProvider;
import com.gsecp.security.UserPrincipal;
import com.gsecp.user.dto.AuthDtos;
import com.gsecp.user.entity.SysUser;
import com.gsecp.user.repository.SysUserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuthService {

    private final SysUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final JwtProperties jwtProperties;

    public AuthService(SysUserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider tokenProvider,
                       JwtProperties jwtProperties) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.jwtProperties = jwtProperties;
    }

    public AuthDtos.LoginResponse login(AuthDtos.LoginRequest request) {
        SysUser user = userRepository.findByUsername(request.username())
                .filter(u -> !Integer.valueOf(1).equals(u.getIsDeleted()))
                .orElseThrow(() -> new BusinessException(ResultCode.UNAUTHORIZED, "用户名或密码错误"));

        if (!"ACTIVE".equals(user.getStatus())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "账号已被禁用");
        }
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "用户名或密码错误");
        }

        List<String> roles = userRepository.findRoleCodesByUserId(user.getId());
        if (roles.isEmpty()) {
            throw new BusinessException(ResultCode.FORBIDDEN, "账号未分配角色，请联系管理员");
        }

        String token = tokenProvider.generate(user.getId(), user.getUsername(), roles);
        AuthDtos.UserInfo userInfo = new AuthDtos.UserInfo(
                user.getId(), user.getUsername(), user.getRealName(), user.getUserNo(), roles);
        return new AuthDtos.LoginResponse(token, "Bearer", jwtProperties.expirationMs() / 1000, userInfo);
    }

    public AuthDtos.UserInfo currentUser(UserPrincipal principal) {
        return userRepository.findByUsername(principal.getUsername())
                .map(user -> new AuthDtos.UserInfo(
                        user.getId(), user.getUsername(), user.getRealName(),
                        user.getUserNo(), principal.getRoles()))
                .orElseThrow(() -> new BusinessException(ResultCode.UNAUTHORIZED, "登录状态已失效"));
    }
}
