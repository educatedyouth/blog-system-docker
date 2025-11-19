package org.hzj.demo.model;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "users") // 数据库中的表名
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 我们用手机号作为登录的唯一凭据
    // unique = true 告诉 JPA 这是一个唯一索引
    @Column(nullable = false, unique = true, length = 255)
    private String phone;

    @Column(nullable = false, length = 100)
    private String username;

    // (我们暂时不用密码，但一个完整的 User 对象应该有它)
    @Column(length = 255)
    private String password;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createTime;

    // --- 构造函数, Getters, Setters ---
    public User() {}

    // (Getters 和 Setters... 省略篇幅，请确保为所有字段生成它们)
    // ...
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}