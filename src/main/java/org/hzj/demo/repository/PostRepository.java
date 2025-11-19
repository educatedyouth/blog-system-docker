package org.hzj.demo.repository;
import org.hzj.demo.model.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 知识点：@Repository
 * 告诉 Spring 容器："这是一个数据访问 Bean"，请管理它（并处理相关的数据库异常）。
 *
 * 知识点：JpaRepository<Post, Long>
 * 这是 Spring Data JPA 的“魔法”。
 * 1. 你只需要定义一个接口，继承 JpaRepository。
 * 2. 泛型 <Post, Long>：
 * - 第一个参数 (Post): 这个 Repository 是用来操作哪个实体类的。
 * - 第二个参数 (Long): 这个实体类的主键 (id) 是什么类型的。
 *
 * 3. 你什么代码都不用写，Spring Data JPA 会在运行时自动为你实现这个接口，
 * 并提供一整套现成的 CRUD 方法，例如：
 * - save(Post post):      保存 (新增或更新)
 * - findById(Long id):   根据 ID 查找
 * - findAll():           查找所有
 * - deleteById(Long id): 根据 ID 删除
 * - ...等等
 */
@Repository
public interface PostRepository extends JpaRepository<Post, Long> {
    // 未来我们还可以在这里定义自定义查询，比如：
    // List<Post> findByTitleContaining(String title);
}