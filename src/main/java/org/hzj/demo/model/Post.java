package org.hzj.demo.model;

import jakarta.persistence.*; // 注意是 jakarta.persistence
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.CreationTimestamp;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 知识点：@Entity
 * 告诉 JPA："这个类是一个实体类，它对应数据库中的一张表"。
 * 默认情况下，类名 (Post) 对应表名 (post)。
 * * 知识点：@Table(name = "blog_posts")
 * 我们可以用 @Table 自定义表名。
 */
@Entity
@Table(name = "blog_posts")
public class Post implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    // 2. 【新字段】
    //    @Transient 告诉 JPA 忽略此字段
    //    它不会被保存到数据库，它只是一个“临时数据持有者”
    @Transient
    private Long viewCount;

    // 3. 【新字段】的 Getter 和 Setter
    public Long getViewCount() {
        return viewCount;
    }

    public void setViewCount(Long viewCount) {
        this.viewCount = viewCount;
    }

    /**
     * 知识点：@Id
     * 声明这个字段是主键。
     *
     * 知识点：@GeneratedValue(strategy = GenerationType.IDENTITY)
     * 声明主键的生成策略。IDENTITY 表示使用数据库的“自增”特性（如 MySQL 的 AUTO_INCREMENT）。
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false, length = 200)
    private Long id;

    /**
     * 知识点：@Column
     * 用于定义字段的属性。
     * nullable = false: 对应数据库的 NOT NULL 约束。
     * length = 200: 对应 VARCHAR(200)。
     */
    @Column(nullable = false, length = 200)
    @NotEmpty(message = "文章标题(title)不能为空") // 失败时的提示信息
    @Size(min = 3, max = 200, message = "文章标题(title)长度必须在 3 到 200 之间")
    private String title;

    /**
     * 知识点：@Lob (Large Object)
     * @Column(columnDefinition = "TEXT") 也可以。
     * 告诉 JPA 这是一个大文本字段，应映射为 TEXT 类型，而不是默认的 VARCHAR(255)。
     */
    @Lob
    @Column(nullable = false)
    @NotEmpty(message = "文章内容(content)不能为空")
    private String content;

    /**
     * 知识点：@CreationTimestamp
     * 由 Hibernate 提供，当数据第一次被插入时，自动将当前时间戳填充到这个字段。
     */
    @CreationTimestamp
    @Column(nullable = false, updatable = false) // 不允许更新
    private LocalDateTime createTime;

    // --- 构造函数, Getter 和 Setter ---
    // JPA 需要一个无参构造函数
    public Post() {
    }

    // (为了方便，省略了其他构造函数)

    // 必须为所有字段提供 Getter 和 Setter，JPA 需要它们来读写数据
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}