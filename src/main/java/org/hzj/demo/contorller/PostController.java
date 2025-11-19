package org.hzj.demo.contorller;
// 11.03
//import org.hzj.demo.model.Post;
//import org.hzj.demo.service.PostService;
//import jakarta.validation.Valid; // JSR 303 Validation
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.web.bind.annotation.*; // 导入所有 Web 注解
//
//import java.util.List;
//
///**
// * 知识点：@RestController
// * (阶段一复习) 声明这是一个 RESTful API 控制器。
// *
// * 知识点：@RequestMapping("/api/v1")
// * 在类上添加 @RequestMapping，意味着这个类中所有方法的 URL 都会
// * 自动带上 "/api/v1" 这个前缀。
// * 例如：@GetMapping("/posts") 的完整路径是 /api/v1/posts。
// * 这是一个很好的实践，用于 API 版本管理。
// */
//@RestController
//@RequestMapping("/api/v1")
//public class PostController {
//
//    @Autowired
//    private PostService postService; // 注入业务层
//
//    // --- API 实现 ---
//
//    /**
//     * 1. (POST) 创建一篇新文章
//     * * 知识点：@PostMapping("/posts")
//     * 映射 HTTP POST 请求到 /api/v1/posts。
//     * POST 通常用于“创建”新资源。
//     *
//     * 知识点：@RequestBody
//     * 这是 Spring MVC 最重要的注解之一。
//     * 它告诉 Spring："请读取 HTTP 请求的 Body (请求体)，
//     * 并使用 Jackson 库将它从 JSON 格式反序列化为 Post 类型的 Java 对象。"
//     *
//     * 知识点：@Valid
//     * (这个现在还看不出效果，因为 Post 实体类里没加校验注解，
//     * 我们在“知识点 6”会优化它，这里先加上)
//     * 告诉 Spring："请对这个 post 对象进行数据校验。"
//     */
//    @PostMapping("/posts")
//    public Post createPost(@Valid @RequestBody Post post) {
//        // 注意：这里我们直接返回了 Post 实体，
//        // 这在“知识点 6”中会被优化为返回统一的 Result 对象。
//        return postService.createPost(post);
//    }
//
//    /**
//     * 2. (GET) 获取所有文章
//     * * 知识点：@GetMapping("/posts")
//     * 映射 HTTP GET 请求到 /api/v1/posts。
//     * GET 用于“读取”资源。
//     */
//    @GetMapping("/posts")
//    public List<Post> getAllPosts() {
//        return postService.getAllPosts();
//    }
//
//    /**
//     * 3. (GET) 根据 ID 获取单篇文章
//     *
//     * 知识点：@GetMapping("/posts/{id}")
//     * 这里的 {id} 是一个“路径变量”。
//     * 它可以匹配 /api/v1/posts/1, /api/v1/posts/23 ...
//     *
//     * 知识点：@PathVariable("id") Long id
//     * 告诉 Spring："请从 URL 路径中提取 {id} 部分，
//     * 并将它转换为 Long 类型，赋值给 id 这个方法参数。"
//     */
//    @GetMapping("/posts/{id}")
//    public Post getPostById(@PathVariable("id") Long id) {
//        // 我们在 Service 层返回了 Optional<Post>
//        // .orElseThrow() 是一个简单粗暴的处理：如果找不到，就抛出异常。
//        // (我们会在“知识点 7”中优雅地处理这个异常)
//        return postService.getPostById(id)
//                .orElseThrow(() -> new RuntimeException("Post not found with id: " + id));
//    }
//
//    /**
//     * 4. (PUT) 更新一篇文章
//     *
//     * 知识点：@PutMapping("/posts/{id}")
//     * 映射 HTTP PUT 请求到 /api/v1/posts/{id}。
//     * PUT 通常用于“完整更新”一个资源。
//     */
//    @PutMapping("/posts/{id}")
//    public Post updatePost(@PathVariable Long id, @Valid @RequestBody Post postDetails) {
//        // (如果 @PathVariable 的名字和方法参数名一致，可以省略 ("id"))
//        return postService.updatePost(id, postDetails);
//    }
//
//    /**
//     * 5. (DELETE) 删除一篇文章
//     *
//     * 知识点：@DeleteMapping("/posts/{id}")
//     * 映射 HTTP DELETE 请求到 /api/v1/posts/{id}。
//     * DELETE 用于“删除”一个资源。
//     */
//    @DeleteMapping("/posts/{id}")
//    public String deletePost(@PathVariable Long id) {
//        postService.deletePost(id);
//        return "Post deleted successfully with id: " + id;
//    }
//
//    /**
//     * 6. (GET with Param) 搜索文章 (演示 @RequestParam)
//     *
//     * 知识点：@RequestParam
//     * 用于获取 URL 中的“查询参数”（? 后面的键值对）。
//     * * 访问示例: /api/v1/posts/search?query=java
//     * - query 变量会被赋值为 "java"
//     * * 访问示例: /api/v1/posts/search
//     * - 因为设置了 defaultValue="default", query 变量会被赋值为 "default"
//     * * @RequestParam(value = "query", required = false, defaultValue = "default") String query
//     * - value = "query":      参数名叫 "query"
//     * - required = false:   这个参数不是必须的
//     * - defaultValue = "default": 如果不传，就用这个默认值
//     */
//    @GetMapping("/posts/search")
//    public String searchPosts(@RequestParam(value = "query", required = false, defaultValue = "default") String query) {
//        // (这里我们只是演示，不去实现 service 层的搜索逻辑)
//        return "Searching for posts with query: " + query;
//    }
//}

// 11.04

import org.hzj.demo.config.BlogProperties;
import org.hzj.demo.exception.ResourceNotFoundException;
import org.hzj.demo.model.Post;
import org.hzj.demo.repository.PostRepository;
import org.hzj.demo.service.PostService;
import org.hzj.demo.vo.ResultVO; // 1. 导入 ResultVO
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity; // 2. (等下会用到) 导入 ResponseEntity
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class PostController {

    @Autowired
    private PostService postService;
    @Autowired
    private PostRepository postRepository;

    // 2. 注入我们新创建的配置 Bean
    @Autowired
    private BlogProperties blogProperties;

    // 3. 创建一个新 API 来显示配置
    @GetMapping("/info")
    public ResultVO<String> getBlogInfo() {
        // 使用配置类，代码非常“干净”，类型安全
        return ResultVO.success(blogProperties.getWelcomeMessage());
    }

    @GetMapping("/info/all")
    public ResultVO<BlogProperties> getAllBlogInfo() {
        // 我们可以把整个配置对象返回
        return ResultVO.success(blogProperties);
    }

    /**
     * 1. (POST) 创建一篇新文章
     * 修改：返回值从 Post 改为 ResultVO<Post>
     */
    @PostMapping("/posts")
    public ResultVO<Post> createPost(@Valid @RequestBody Post post) {
        Post createdPost = postService.createPost(post);
        // 使用静态方法 ResultVO.success(data) 来包装数据
        return ResultVO.success(createdPost);
    }

    /**
     * 2. (GET) 获取所有文章
     * 修改：返回值从 List<Post> 改为 ResultVO<List<Post>>
     */
    @GetMapping("/posts")
    public ResultVO<List<Post>> getAllPosts() {
        List<Post> posts = postService.getAllPosts();
        return ResultVO.success(posts);
    }

    /**
     * 3. (GET) 根据 ID 获取单篇文章
     * 修改：返回值从 Post 改为 ResultVO<Post>
     * 注意：我们之前那个 .orElseThrow() 异常还没处理，
     * 别急，这正是我们“改进点2”要解决的。
     */
    @GetMapping("/posts/{id}")
    public ResultVO<Post> getPostById(@PathVariable("id") Long id) {

        // 1. 【首先】获取文章
        // 这一步会触发 @Cacheable，
        // 它要么从缓存（极快），要么从数据库（较慢）获取 Post
        Post post = postService.getPostById(id)
                .orElseThrow(() -> new ResourceNotFoundException("文章未找到, ID: " + id));

        // 2. 【然后】增加点击率
        // 关键：如果 .orElseThrow() 抛出了异常（404），
        // 下面这行代码【不会】被执行。
        // 这就保证了我们【不会】给“不存在的文章”增加点击率！
        // 这一步是“手动挡”，它【每次】都会执行 Redis ZINCRBY。
        Long newViewCount = postService.incrementViewCount(id);


        // 3. 【拼装】把“热”数据，塞进“冷”数据
        post.setViewCount(newViewCount);

        // 4. 【最后】返回“融合”后的数据
        return ResultVO.success(post);
    }

    // ... in PostService.java

    /**
     * 4. (PUT) 更新一篇文章
     *
     * 知识点：@PutMapping("/posts/{id}")
     * 映射 HTTP PUT 请求到 /api/v1/posts/{id}。
     * PUT 通常用于“完整更新”一个资源。
     */
    @PutMapping("/posts/{id}")
    public Post updatePost(@PathVariable Long id, @Valid @RequestBody Post postDetails) {
        // (如果 @PathVariable 的名字和方法参数名一致，可以省略 ("id"))
        return postService.updatePost(id, postDetails);
    }

    /**
     * 5. (DELETE) 删除一篇文章
     *
     * 知识点：@DeleteMapping("/posts/{id}")
     * 映射 HTTP DELETE 请求到 /api/v1/posts/{id}。
     * DELETE 用于“删除”一个资源。
     */
    @DeleteMapping("/posts/{id}")
    public String deletePost(@PathVariable Long id) {
        postService.deletePost(id);
        return "Post deleted successfully with id: " + id;
    }

    /**
     * 6. (GET with Param) 搜索文章 (演示)
     * 修改：返回值从 String 改为 ResultVO<String>
     */
    @GetMapping("/posts/search")
    public ResultVO<String> searchPosts(@RequestParam(value = "query", required = false, defaultValue = "default") String query) {
        String result = "Searching for posts with query: " + query;
        return ResultVO.success(result);
    }

    /**
     * 【新 API】获取 Top 5 热榜
     */
    @GetMapping("/posts/top")
    public ResultVO<List<Post>> getTop5Posts() {
        // 我们硬编码 Top 5
        List<Post> topPosts = postService.getTopViewedPosts(5);
        return ResultVO.success(topPosts);
    }
}