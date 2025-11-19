package org.hzj.demo.config;

import org.hzj.demo.filter.JwtAuthFilter;
import org.hzj.demo.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration      // 声明这是一个“配置类”
@EnableWebSecurity  // “启动”Spring Security 的 Web 安全功能
public class SecurityConfig {

    @Autowired
    private JwtAuthFilter jwtAuthFilter; // 我们的“保镖”

    /**
     * 【【核心：配置“过滤器链”】】
     * @param http HttpSecurity (Spring Security 的配置入口)
     * @return
     * @throws Exception
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        // 1. 【关闭】CSRF
        //    (因为我们用 JWT，是“无状态”的，
        //     不需要 Spring 默认的 Session-Cookie CSRF 防护)
        http.csrf(csrf -> csrf.disable());

        // 2. 【核心】设置 Session 管理策略为“无状态”(Stateless)
        //    (我们“不”创建 HttpSession，
        //     这告诉 Spring Security：“不要用你的 Session 那一套了！”)
        http.sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        );

        // 3. 【【核心：配置“授权规则”】】
        http.authorizeHttpRequests(auth -> auth
                // --- “放行” (Permit All) ---
                // (1) 登录/注册 API
                .requestMatchers("/api/v1/auth/**").permitAll()

                // (2) 我们的“前端页面” (必须放行)
                .requestMatchers("/index.html").permitAll()

                // (3) “只读”的 GET API (我们允许“游客”查看文章)
                .requestMatchers(HttpMethod.GET, "/api/v1/posts/**").permitAll()
                // 【【【 新增：放行 Actuator 所有接口 】】】
                // (注意：在生产环境中，这里通常需要 "hasRole('ADMIN')" 权限)
                .requestMatchers("/actuator/**").permitAll()
                // --- “保护” (Authenticated) ---
                // (4) “其他所有”的请求
                //    (例如 POST /posts, PUT /posts, DELETE /posts)
                //    都必须“已认证”(Authenticated)
                .anyRequest().authenticated()
        );

        // 4. 【【核心：添加“保镖”】】
        //    把我们的 "JwtAuthFilter"（保镖），
        //    添加到 Spring Security 默认的
        //    "UsernamePasswordAuthenticationFilter" (登录过滤器)
        //    【之前】。
        http.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        // 5. (下一步) 我们还需要配置“异常处理器”... (先省略)
        return http.build();
    }
}