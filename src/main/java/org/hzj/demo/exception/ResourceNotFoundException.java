package org.hzj.demo.exception;

/**
 * 自定义一个“资源未找到”异常。
 * * 知识点：为什么继承 RuntimeException？
 * 1. RuntimeException (非受检异常) 不需要
 * 在方法签名上显式 "throws ...",
 * 这让我们的 Service 和 Controller 代码更简洁。
 * 2. 这是一个业务逻辑异常，调用者（Controller）
 * 通常不需要“捕获”它，而是任其抛出，
 * 由我们即将创建的“全局异常处理器”统一捕获。
 */
// 1. "extends RuntimeException"
// 这句话的意思是：ResourceNotFoundException 是 RuntimeException 的“子类”（孩子）。
// RuntimeException 是 ResourceNotFoundException 的“父类”（父母）。
// 孩子会“继承”父母的所有非私有特性。
public class ResourceNotFoundException extends RuntimeException {

    // 2. "public ResourceNotFoundException(String message)"
    // 这是“孩子”的“构造函数”（Constructor）。
    // 构造函数是 "new" 一个对象时被调用的方法。
    // 当我们写 new ResourceNotFoundException("文章未找到") 时，
    // "文章未找到" 这个字符串就被传入了这里的 message 变量。
    public ResourceNotFoundException(String message) {

        // 3. "super(message);"
        // 这就是核心！
        // "super" 关键字在这里的意思是：“调用我父类的构造函数”。
        //
        // 所以，super(message) 的意思是：
        // “把我（孩子）刚刚收到的这个 message 字符串，
        //  原封不动地传递给我父类（RuntimeException）的构造函数。”
        //
        // 它实际上是在调用父类 RuntimeException(String message) 这个构造函数。
        super(message);
    }
}