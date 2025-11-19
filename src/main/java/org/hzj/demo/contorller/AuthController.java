package org.hzj.demo.contorller;

import jakarta.servlet.http.HttpServletResponse;
import org.hzj.demo.dto.SendCodeRequest;
import org.hzj.demo.dto.SmsLoginRequest;
import org.hzj.demo.service.AuthService;
import org.hzj.demo.vo.ResultVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth") // 认证的 API 统一前缀
public class AuthController {

    @Autowired
    private AuthService authService;

    /**
     * 1. 发送短信验证码 API
     */
    @PostMapping("/send-code")
    public ResultVO<Object> sendCode(@RequestBody SendCodeRequest request) {
        authService.sendCode(request.getPhone());
        return ResultVO.success(); // (只返回成功，不返回数据)
    }

    /**
     * 2. 短信登录 API
     */
    @PostMapping("/login-by-sms")
    public ResultVO<String> loginBySms(@RequestBody SmsLoginRequest request) {
        String loginResult = authService.loginBySms(request.getPhone(), request.getCode());
        // (目前 loginResult 是 "登录成功..." 字符串,
        //  下一步它将变成 JWT Token)
        return ResultVO.success(loginResult);
    }
    // 1. 注入我们刚配置的“入场券”
    @Value("${github.client-id}")
    private String githubClientId;
    @Value("${github.client-secret}")
    private String githubClientSecret;

    // --- 【API 1：获取“GitHub 授权 URL”】 ---
    @GetMapping("/github/url")
    public ResultVO<String> getGitHubAuthUrl() {
        // (安全) state 必须与 session/redis 关联，我们 demo 暂时简化
        String state = UUID.randomUUID().toString().substring(0, 6);

        // 2. 拼装 GitHub 的 URL
        String url = "https://github.com/login/oauth/authorize?" +
                "client_id=" + githubClientId +
                "&scope=read:user" + // (新版 scope 叫 read:user, user:email)
                "&state=" + state;

        System.out.println("====== [AuthController] 生成的 GitHub 授权 URL: " + url + " ======");
        return ResultVO.success(url);
    }

    // --- 【API 2：“回调” (Callback)】 ---
    @GetMapping("/github/callback")
    public void githubCallback(
            @RequestParam("code") String code,   // GitHub 给的“临时授权码”
            @RequestParam("state") String state, // (我们应该校验 state)
            HttpServletResponse httpResponse // (用于重定向回前端)
    ) throws Exception {

        System.out.println("====== [AuthController] GitHub 回调... code=" + code.substring(0, 4) + "... ======");

        // 1. 【【核心：用 Code 换 AccessToken】】
        RestTemplate restTemplate = new RestTemplate();
        String tokenUrl = "https://github.com/login/oauth/access_token";

        // (GitHub 奇葩之处：它需要 JSON 格式的 Body)
        Map<String, String> tokenRequest = new HashMap<>();
        tokenRequest.put("client_id", githubClientId);
        tokenRequest.put("client_secret", githubClientSecret);
        tokenRequest.put("code", code);

        // (GitHub 奇葩之处 2: 它需要 'Accept: application/json' 头)
        HttpHeaders tokenHeaders = new HttpHeaders();
        tokenHeaders.set("Accept", "application/json");

        HttpEntity<Map<String, String>> tokenEntity = new HttpEntity<>(tokenRequest, tokenHeaders);

        // (执行“服务器 To 服务器”的 POST 请求)
        ResponseEntity<Map> tokenResponse = restTemplate.exchange(
                tokenUrl, HttpMethod.POST, tokenEntity, Map.class
        );

        String accessToken = (String) tokenResponse.getBody().get("access_token");
        System.out.println("====== [AuthController] 换取到 AccessToken: " + accessToken.substring(0, 6) + "... ======");

        // 2. 【【核心：用 AccessToken 换 UserInfo】】
        String userUrl = "https://api.github.com/user";

        HttpHeaders userHeaders = new HttpHeaders();
        // (GitHub 奇葩之处 3: 它的 Token 叫 "token"，而不是 "Bearer")
        userHeaders.set("Authorization", "token " + accessToken);

        HttpEntity<String> userEntity = new HttpEntity<>(userHeaders);

        // (执行“服务器 To 服务器”的 GET 请求)
        ResponseEntity<Map> userResponse = restTemplate.exchange(
                userUrl, HttpMethod.GET, userEntity, Map.class
        );

        String githubLoginName = (String) userResponse.getBody().get("login");
        String githubAvatarUrl = (String) userResponse.getBody().get("avatar_url");
        String githubId = userResponse.getBody().get("id").toString();

        System.out.println("====== [AuthController] 换取到用户信息: " + githubLoginName + " ======");

        // 3. 【【登录或注册，并签发我们自己的 JWT】】
        //    (我们用 githubId (唯一) 作为“手机号”，
        //     用 githubLoginName 作为“用户名”)
        //    (这会触发 AuthService 里的“自动注册”逻辑)
        String ourJwtToken = authService.loginByOauth(
                githubId,
                githubLoginName,
                githubAvatarUrl
        );

        // 4. 【重定向回“真正”的前端】
        //    我们把“我们自己”的 Token，通过 URL 参数带回去
        httpResponse.sendRedirect("http://localhost:8081/index.html?token=" + ourJwtToken);
    }
}