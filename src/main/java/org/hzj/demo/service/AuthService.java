package org.hzj.demo.service;

import org.hzj.demo.exception.ResourceNotFoundException; // (借用一下)
import org.hzj.demo.model.User;
import org.hzj.demo.repository.UserRepository;
import org.hzj.demo.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
public class AuthService implements UserDetailsService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil; // 2. 注入 JWT 工具类

    // Redis Key 的前缀
    private static final String CODE_KEY_PREFIX = "login_code:";
    // 验证码 5 分钟过期
    private static final long CODE_EXPIRATION_MINUTES = 5;

    // 3. 【【实现接口方法】】
    /**
     * 这是 Spring Security 在“认证”时
     * 自动调用的【唯一】方法
     * @param phone (它叫 "username"，但我们存的是手机号)
     * @return UserDetails (Spring Security 自己的 User 对象)
     * @throws UsernameNotFoundException
     */
    public UserDetails loadUserByUsername(String phone) throws UsernameNotFoundException {

        // 1. 这里的 "User"
        //    (因为我们的 import)
        //    正确地指向了 "org.hzj.demo.model.User"
        User myUser = userRepository.findByPhone(phone)
                .orElseThrow(() ->
                        new UsernameNotFoundException("用户未找到, 手机号: " + phone)
                );

        // 2. 这里的 "new org.springframework.security.core.userdetails.User"
        //    我们【【必须】】使用“全名”
        //    来告诉 Java 我们要 new 的是“另一个” User
        return new org.springframework.security.core.userdetails.User(
                myUser.getPhone(),
                myUser.getPassword(),
                new java.util.ArrayList<>()
        );
    }

    /**
     * 1. 发送验证码
     */
    public void sendCode(String phone) {
        // 1. (防刷) TODO: 60秒内是否发过？(我们先省略)

        // 2. 生成一个 6 位随机验证码
        String code = generateSmsCode();

        // 3. 定义 Redis Key
        String key = CODE_KEY_PREFIX + phone;

        // 4. 【核心】把验证码存入 Redis，并设置 5 分钟过期
        //    (对应 SET login_code:138... "123456" EX 300)
        stringRedisTemplate.opsForValue().set(
                key,
                code,
                CODE_EXPIRATION_MINUTES,
                TimeUnit.MINUTES
        );

        // 5. (模拟) 调用短信网关
        //    在真实项目中，这里会调用阿里云/腾讯云的短信 SDK
        System.out.println("====== [AuthService] 向手机号 " + phone + " 发送验证码: " + code + " ======");
    }

    /**
     * 2. 通过短信验证码登录
     * 【修改】返回值现在是 JWT Token 字符串
     */
    public String loginBySms(String phone, String code) {
        // 1. (校验) 验证码是否过期？
        String key = CODE_KEY_PREFIX + phone;
        String codeInRedis = stringRedisTemplate.opsForValue().get(key);
        if (codeInRedis == null) {
            throw new ResourceNotFoundException("验证码已过期或未发送");
        }

        // 2. (校验) 验证码是否正确？
        if (!codeInRedis.equals(code)) {
            throw new ResourceNotFoundException("验证码错误");
        }

        // 3. 【认证成功！】

        // 4. (清理) 验证码是一次性的，用完就删
        stringRedisTemplate.delete(key);

        // 5. 【核心】查找或创建用户
        User user = findOrCreateUserByPhone(phone);

        // 6. 【【重大修改】】
        //    我们不再返回一个“欢迎字符串”
        //    return "登录成功, 欢迎您: " + user.getUsername();

        //    而是调用 JwtUtil 【签发一个 JWT Token】
        return jwtUtil.generateToken(user);
    }

    /**
     * 辅助方法：查找或创建用户
     */
    private User findOrCreateUserByPhone(String phone) {
        // 1. 尝试按手机号查找
        Optional<User> userOpt = userRepository.findByPhone(phone);

        if (userOpt.isPresent()) {
            // (情况 A) 找到了，是老用户
            return userOpt.get();
        } else {
            // (情况 B) 没找到，是新用户，自动注册
            System.out.println("====== [AuthService] 新用户自动注册: " + phone + " ======");
            User newUser = new User();
            newUser.setPhone(phone);
            // (给一个默认用户名，比如“用户”+手机号后4位)
            newUser.setUsername("用户" + phone.substring(phone.length() - 4));
            // (密码可以先设一个随机值或 null)
            newUser.setPassword("N/A");

            return userRepository.save(newUser);
        }
    }

    /**
     * 辅助方法：生成 6 位随机数字
     */
    private String generateSmsCode() {
        // (用 SecureRandom 保证随机性)
        SecureRandom random = new SecureRandom();
        int code = 100000 + random.nextInt(900000); // 100000 - 999999
        return String.valueOf(code);
    }
    /**
     * 【【新】】
     * 3. 通过 OAuth (GitHub) 登录
     * (这个逻辑和 findOrCreateUserByPhone 几乎一样)
     *
     * @param oauthId (例如 GitHub 的 123456)
     * @param username (例如 "octocat")
     * @param avatarUrl (头像 URL)
     * @return 我们的 JWT Token
     */
    public String loginByOauth(String oauthId, String username, String avatarUrl) {

        // 1. 我们用 "phone" 字段来存储 "github_123456" 这样的唯一 ID
        String phoneKey = "github_" + oauthId;

        Optional<User> userOpt = userRepository.findByPhone(phoneKey);

        User user;
        if (userOpt.isPresent()) {
            // (老用户)
            user = userOpt.get();
            // (也许更新一下他的用户名和头像？)
            user.setUsername(username);
            // user.setAvatar(avatarUrl); // (我们的 User 实体还没这个字段)
            userRepository.save(user);
        } else {
            // (新用户，自动注册)
            System.out.println("====== [AuthService] GitHub 新用户自动注册: " + username + " ======");
            user = new User();
            user.setPhone(phoneKey); // (用 phone 字段存 GitHub ID)
            user.setUsername(username);
            user.setPassword("OAUTH_LOGIN"); // (OAuth 登录，密码无意义)
            user = userRepository.save(user);
        }

        // 2. 【签发 JWT】
        return jwtUtil.generateToken(user);
    }
}