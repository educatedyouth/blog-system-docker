package org.hzj.demo.filter;

import org.hzj.demo.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT 认证过滤器
 * 【【“保镖”】】
 * * 1. 继承 OncePerRequestFilter (确保每次请求只执行一次)
 * 2. 必须是 @Component (才能被 @Autowired)
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil; // 我们的“Token 工厂” (解码/验证)

    @Autowired
    private UserDetailsService userDetailsService; // 我们的“用户加载器” (AuthService)

    /**
     * 【【核心过滤逻辑】】
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain // “过滤器链”
    ) throws ServletException, IOException {

        // 1. 【获取】"Authorization" 请求头
        final String authHeader = request.getHeader("Authorization");

        // 2. 【检查】
        //    - authHeader 是 null 吗？(没带 Token)
        //    - authHeader 是 "Bearer " 开头吗？(格式不对)
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            // 如果是，直接“放行”。
            // 为什么？因为“公共 API”(如/auth/login)不需要 Token。
            // 别担心，如果它试图访问“受保护”的 API，
            // Spring Security 的“下一个”过滤器会(因为没认证)拦住它。
            filterChain.doFilter(request, response);
            return;
        }

        // 3. 【提取】Token
        //    ("Bearer eyJ...") -> ("eyJ...")
        final String jwt = authHeader.substring(7);

        // 4. 【【解码】】
        //    (这里就是 5.2 阶段“死代码”被“激活”的地方)
        final String userPhone = jwtUtil.extractSubject(jwt);

        // 5. 【检查“是否已认证”】
        //    (SecurityContextHolder 是 Spring Security 的“全局上下文”)
        //    如果 userPhone 不是 null，并且“上下文”中【还未】设置认证
        if (userPhone != null &&
                SecurityContextHolder.getContext().getAuthentication() == null) {

            // 6. 【加载】用户
            //    (这将调用我们的 AuthService.loadUserByUsername(...))
            UserDetails userDetails = this.userDetailsService.loadUserByUsername(userPhone);

            // 7. 【验证】Token
            //    (检查 Token 是否过期，并且 Token 里的
            //     "subject" 是否真的和 UserDetails 里的 "username" 匹配)
            if (!jwtUtil.isTokenExpired(jwt) &&
                    jwtUtil.extractSubject(jwt).equals(userDetails.getUsername())) {

                // 8. 【【认证成功！】】
                //    创建一个“认证凭证” (Token)
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                userDetails, // "当事人" (Principal)
                                null,        // "凭证" (Credentials)，我们用 Token，不需要密码
                                userDetails.getAuthorities() // "权限" (Authorities)
                        );

                authToken.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );

                // 9. 【【关键】】
                //    把这个“凭证”放入“安全上下文”
                //    Spring Security 在此之后，
                //    就“知道”这个请求是“已认证”的了。
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        // 10. 【放行】
        //     无论认证成功与否，都放行给“下一个”过滤器。
        //     (如果第 9 步成功了，它就被“标记”为已认证)
        //     (如果第 7 步失败了，它还是“未认证”，
        //      会被 Spring Security 的“下一个”过滤器拦住)
        filterChain.doFilter(request, response);
    }
}