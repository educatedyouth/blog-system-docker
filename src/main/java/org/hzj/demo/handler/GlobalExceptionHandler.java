package org.hzj.demo.handler;

import org.hzj.demo.exception.ResourceNotFoundException;
import org.hzj.demo.vo.ResultVO;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 知识点：@RestControllerAdvice
 * 这是一个组合注解，等同于 @ControllerAdvice + @ResponseBody。
 * 1. @ControllerAdvice: 告诉 Spring，这个类是一个“全局处理器”，
 * 它会“监视”所有被 @RestController 标记的类。
 * 2. @ResponseBody: 告诉 Spring，这个类中所有方法的返回值
 * 都应该被序列化为 JSON。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    // --- 【【新】】 ---

    /**
     * 捕获“认证失败”异常 (401)
     * (例如：Token 不存在、Token 非法、Token 过期)
     * (由 JwtAuthFilter 或 Spring Security 内部抛出)
     */
    @ExceptionHandler(AuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED) // HTTP 状态码 401
    public ResultVO<Object> handleAuthenticationException(AuthenticationException e) {
        return ResultVO.error(401, "认证失败: " + e.getMessage());
    }

    /**
     * 捕获“权限不足”异常 (403)
     * (例如：一个“普通用户”试图访问“管理员” API)
     * (我们目前还没用到，但必须先加上)
     */
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN) // HTTP 状态码 403
    public ResultVO<Object> handleAccessDeniedException(AccessDeniedException e) {
        return ResultVO.error(403, "权限不足: " + e.getMessage());
    }

    /**
     * 1. 捕获“资源未找到”异常 (404)
     *
     * 知识点：@ExceptionHandler(ResourceNotFoundException.class)
     * 告诉 Spring：“如果任何 Controller 抛出了
     * ResourceNotFoundException 异常，就由这个方法来处理。”
     *
     * 知识点：@ResponseStatus(HttpStatus.NOT_FOUND)
     * 这一步【至关重要】！
     * 它不仅在 JSON (Body) 里返回了 "code: 404"，
     * 它还把“真正”的 HTTP 协议状态码从 200 (OK)
     * 修改为了 404 (Not Found)。
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND) // HTTP 状态码 404
    public ResultVO<Object> handleResourceNotFound(ResourceNotFoundException e) {
        // e.getMessage() 就是我们抛出时写的 "Post not found..."
        // 我们使用自定义的 404 业务码
        return ResultVO.error(404, e.getMessage());
    }

    /**
     * 2. (稍后为“改进点 2b”添加) 捕获“数据校验”异常 (400)
     * (我们先留空，等下回来填)
     */
// ... (handleResourceNotFound 方法) ...

    /**
     * 2. 捕获“数据校验”异常 (400)
     *
     * 知识点：@ExceptionHandler(MethodArgumentNotValidException.class)
     * 当 @Valid 校验失败时，Spring MVC 会自动抛出这个异常。
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST) // HTTP 状态码 400
    public ResultVO<Object> handleValidationExceptions(MethodArgumentNotValidException e) {

        // e.getBindingResult() 包含了所有校验失败的信息
        // 我们可以只获取第一条错误信息返回
        String errorMessage = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .findFirst() // 只取第一条
                .orElse("数据校验失败"); // 兜底

        // 400 是客户端请求参数错误
        return ResultVO.error(400, errorMessage);
    }

    /**
     * 3. 捕获所有“其他”异常 (500)
     *
     * 知识点：@ExceptionHandler(Exception.class)
     * 这是“最终防线”。它会捕获所有【上面没有】处理过的异常
     * (例如 NullPointerException, SQLException 等)。
     *
     * 这样可以确保：无论发生什么，用户永远不会看到
     * Spring Boot 默认的“Whitelabel Error Page”。
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR) // HTTP 状态码 500
    public ResultVO<Object> handleGenericException(Exception e) {
        // 在真实项目中，这里应该记录详细的错误日志
        // log.error("Internal Server Error: ", e);

        // 500 是系统错误码
        return ResultVO.error(500, "服务器内部错误: " + e.getMessage());
    }
}