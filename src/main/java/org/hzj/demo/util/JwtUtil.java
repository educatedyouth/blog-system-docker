package org.hzj.demo.util;

import org.hzj.demo.config.JwtProperties;
import org.hzj.demo.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * JWT 工具类
 * (必须是 @Component，才能被 Spring 注入)
 */
@Component
public class JwtUtil {

    private final JwtProperties jwtProperties;
    private final SecretKey key; // 加密后的秘钥

    @Autowired
    public JwtUtil(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;

        // 1. 将 application.yml 中配置的 Base64 字符串秘钥
        //    转换回“字节数组”
        byte[] keyBytes = Decoders.BASE64.decode(this.jwtProperties.getSecret());

        // 2. 使用 HMAC-SHA 算法创建一个“安全秘钥”对象
        //    (这是 jjwt 库推荐的做法)
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * 【核心】1. 生成 Token
     * @param user 我们登录的“用户”对象
     * @return Token 字符串
     */
    public String generateToken(User user) {
        // “Claims” (声明) 是 Token 的“正文” (Payload)
        // 我们可以把任何“非敏感”信息放进去
        Map<String, Object> claims = new HashMap<>();
        // (例如，我们可以把 "role" 放进去，
        //  但现在我们的 User 还没有 role 字段)
        // claims.put("role", user.getRole());
        claims.put("userId", user.getId());

        return createToken(claims, user.getPhone());
    }

    private String createToken(Map<String, Object> claims, String subject) {
        // subject (主题) 是一个标准声明，我们用它来存“用户名”(手机号)

        Date now = new Date(System.currentTimeMillis());
        Date expirationDate = new Date(now.getTime() + jwtProperties.getExpirationMs());

        return Jwts.builder()
                .setClaims(claims)       // 设置自定义声明
                .setSubject(subject)     // 设置主题 (手机号)
                .setIssuedAt(now)        // 设置签发时间
                .setExpiration(expirationDate) // 设置过期时间 (1 小时)
                .signWith(key, SignatureAlgorithm.HS256) // 设置签名 (秘钥 + 算法)
                .compact(); // 压缩成最终的 eyJ... 字符串
    }

    // --- 【【以下方法将在下一步 (5.3) 被使用】】 ---

    /**
     * 【核心】2. 解析 Token (获取所有声明)
     * (如果签名无效或 Token 过期，这里会抛出异常)
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 3. 从 Token 中获取“主题”(手机号)
     */
    public String extractSubject(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * 4. 检查 Token 是否过期
     */
    public Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /** (辅助工具) */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }
}