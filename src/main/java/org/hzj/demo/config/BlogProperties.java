package org.hzj.demo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.util.List;

/**
 * 知识点：@ConfigurationProperties(prefix = "blog")
 * 1. 告诉 Spring Boot：请帮我把配置文件中
 * 所有以 "blog" 为前缀的配置项，
 * 自动“绑定”到这个类的同名字段上。
 * 2. 字段名必须和配置的 "key" 匹配
 * (blog.welcome-message 对应 welcomeMessage)
 * 3. 必须为所有字段提供 public setter 方法！
 * (Spring Boot 需要调用 setter 来注入值)
 *
 * 知识点：这个类本身还不是一个 Bean！
 * 我们还需要在别处“激活”它。
 */
@ConfigurationProperties(prefix = "blog")
public class BlogProperties {

    private String author;
    private String welcomeMessage;
    private List<String> tags; // 连 List 都能自动绑定！

    // --- 必须有 public Getters 和 Setters ---
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }
    public String getWelcomeMessage() { return welcomeMessage; }
    public void setWelcomeMessage(String welcomeMessage) { this.welcomeMessage = welcomeMessage; }
    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }

    @Override
    public String toString() {
        return "BlogProperties{" +
                "author='" + author + '\'' +
                ", welcomeMessage='" + welcomeMessage + '\'' +
                ", tags=" + tags +
                '}';
    }
}