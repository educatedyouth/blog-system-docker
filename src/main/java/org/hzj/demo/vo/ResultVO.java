package org.hzj.demo.vo;

/**
 * (VO = View Object)
 * 大厂规范：定义一个统一的、通用的 JSON 响应结构。
 * 未来所有 Controller 的方法，都将返回这个 ResultVO 对象。
 * * 我们将这个类设计为“泛型类” (Generic Type)，
 * 用 <T> 来代表“真正的数据”的类型。
 */
public class ResultVO<T> {

    /** 响应状态码 (不是 HTTP 状态码)，而是一个业务内部的状态码 */
    private Integer code;

    /** 响应消息 (例如 "success", "文章未找到") */
    private String message;

    /** 响应的真实数据 (例如 Post, List<Post>, null) */
    private T data;

    // --- 私有化构造函数，强制使用静态方法创建 ---
    private ResultVO() {
    }

    private ResultVO(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    // --- 提供一系列“静态工厂方法”，用于快速创建 ResultVO 对象 ---

    /**
     * 1. 成功 - 并且需要返回数据
     * @param data 要返回的数据
     * @return ResultVO 实例
     */
    public static <T> ResultVO<T> success(T data) {
        // 约定：成功的 code 统一为 200
        return new ResultVO<>(200, "success", data);
    }

    /**
     * 2. 成功 - 但不需要返回数据 (例如 POST, PUT, DELETE 成功)
     * @return ResultVO 实例
     */
    public static <T> ResultVO<T> success() {
        // data 字段默认为 null
        return new ResultVO<>(200, "success", null);
    }

    /**
     * 3. 失败 - (用于业务异常，例如“用户名已存在”)
     * @param code 业务错误码 (例如 50001)
     * @param message 错误信息
     * @return ResultVO 实例
     */
    public static <T> ResultVO<T> error(Integer code, String message) {
        return new ResultVO<>(code, message, null);
    }

    /**
     * 4. 失败 - (用于系统异常)
     * @param message 错误信息
     * @return ResultVO 实例
     */
    public static <T> ResultVO<T> error(String message) {
        // 约定：系统级错误的 code 统一为 500
        return new ResultVO<>(500, message, null);
    }


    // --- 必须提供 Getter ---
    // (因为 Jackson 库在序列化时，需要调用 public getter 来获取私有字段)
    public Integer getCode() { return code; }
    public String getMessage() { return message; }
    public T getData() { return data; }

    // (Setter 可以不提供，我们希望这个对象一旦创建就不被修改)
}