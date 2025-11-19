package org.hzj.demo.repository;

import org.hzj.demo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * 【核心】
     * Spring Data JPA 的“魔法”：
     * 你只需要按规范定义这个方法名，JPA 就会自动为你
     * 生成 "SELECT * FROM users WHERE phone = ?" 的 SQL 实现。
     */
    Optional<User> findByPhone(String phone);
}
