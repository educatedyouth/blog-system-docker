package org.hzj.demo.service;

import org.hzj.demo.exception.ResourceNotFoundException;
import org.hzj.demo.model.Post;
import org.hzj.demo.repository.PostRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.events.Event;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 知识点：@Service
 * 告诉 Spring 容器："这是一个业务逻辑 Bean"，请管理它。
 */
@Service
public class PostService {

    /**
     * 知识点：依赖注入 (Dependency Injection)
     * 我们需要 PostRepository 来帮我们操作数据库。
     *
     * 知识点：@Autowired (自动装配)
     * 告诉 Spring："请在你的容器中找到一个 PostRepository 类型的 Bean
     * (就是我们上一步定义的接口的实现类)，并自动把它赋值给这个字段。"
     *
     * (大厂更推荐使用 "构造函数注入"，这里为保持简单先用 @Autowired)
     */
    @Autowired
    private PostRepository postRepository;

    // 注入“手动挡” Redis 模板
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    // (可选，但推荐) 定义 ZSet 在 Redis 中的 Key
    private static final String POST_VIEW_COUNT_KEY = "post:view_counts";

    /**
     * (GET /posts) 获取所有文章
     *
     * 知识点：@Cacheable(cacheNames = "post_list")
     * 1. cacheNames = "post_list": 我们给这个缓存“目录”起名叫 "post_list"
     * 2. key: 我们没有指定 key。此时 Spring 会自动生成一个 key
     * (比如 "SimpleKey []")，代表“所有文章”。
     * 3. 第一次调用此方法，会查询数据库，并将 List<Post> 存入 Redis。
     * 4. 第二次（及以后）调用，会【直接】从 Redis 返回 List<Post>，
     * 【不会】执行此方法体内的 postRepository.findAll()。
     */
    @Cacheable(cacheNames = "post_list")
    public List<Post> getAllPosts() {
        // (为了验证，我们在日志里加一句话)
        System.out.println("====== [Service] 正在查询数据库：getAllPosts() ... ======");
        return postRepository.findAll();
    }

    /**
     * (GET /posts/{id}) 获取单篇文章
     *
     * 知识点：@Cacheable(cacheNames = "post", key = "#id")
     * 1. cacheNames = "post": 我们给“单篇文章”的缓存目录起名叫 "post"。
     * 2. key = "#id": 这是“SpEL 表达式”。
     * #id 的意思是：“把这个方法的 'id' 参数的值，作为缓存的 key”。
     * 3. 结果：
     * getPostById(1L) -> Redis key 是 "post::1"
     * getPostById(2L) -> Redis key 是 "post::2"
     */
    @Cacheable(cacheNames = "post", key = "#id")
    public Optional<Post> getPostById(Long id) {
        System.out.println("====== [Service] 正在查询数据库：getPostById(" + id + ") ... ======");
        // 1. 先从数据库（或缓存）获取
        return postRepository.findById(id);
    }

    /**
     * 【新功能】增加文章点击率
     * 这是一个“手动”操作，不使用 @Cacheable
     */
    public Long incrementViewCount(Long postId) {
        System.out.println("====== [Service] 正在增加 Redis ZSet score：post " + postId + " ... ======");
        try {
            // opsForZSet() -> 获取操作 ZSet 的“命令集”
            // incrementScore(ZSET的Key, 成员, 增加的分数)
            // 这对应 redis-cli 命令: ZINCRBY post:view_counts 1 "post_id_string"
            Double newScore = stringRedisTemplate.opsForZSet().incrementScore(
                    POST_VIEW_COUNT_KEY, // "post:view_counts"
                    postId.toString(),     // "成员"：Post ID (必须是字符串)
                    1.0                    // "分数"：每次点击 +1
            );
            return newScore != null ? newScore.longValue() : 0L;
        } catch (Exception e) {
            // 在真实项目中，缓存的失败不应阻断主流程
            // log.error("增加 Redis 点击率失败: ", e);
            System.err.println("增加 Redis 点击率失败: " + e.getMessage());
            return 0L; // 出错时返回 0
        }
    }

    /**
     * (POST /posts) 创建新文章
     *
     * 知识点：@CacheEvict(cacheNames = "post_list", allEntries = true)
     * 1. 当我们【创建】了一篇新文章时，"post_list" (所有文章)
     * 这个缓存就“脏”了 (数据过时了)。
     * 2. 我们必须【清除】它，下次 `getAllPosts()` 就会重新从数据库读取。
     * 3. allEntries = true: "把 'post_list' 目录下的【所有】缓存都删了"。
     */
    @CacheEvict(cacheNames = "post_list", allEntries = true)
    public Post createPost(Post post) {
        System.out.println("====== [Service] 正在写入数据库：createPost() ... ======");
        return postRepository.save(post);
    }

    /**
     * (PUT /posts/{id}) 更新文章
     *
     * 知识点：@Caching (一个注解干两件事)
     * 我们需要干两件事：
     * 1. 【更新】 "post::id" 这个单篇缓存 (用 @CachePut)
     * 2. 【清除】 "post_list" 这个列表缓存 (用 @CacheEvict)
     */
    @Caching(
            put = {
                    // @CachePut 会【总是】执行方法体，并把【返回值】
                    // 存入 "post::id" 这个 key 中。
                    @CachePut(cacheNames = "post", key = "#id")
            },
            evict = {
                    // 同时，清除 "post_list" 缓存
                    @CacheEvict(cacheNames = "post_list", allEntries = true)
            }
    )
    public Post updatePost(Long id, Post postDetails) {
        System.out.println("====== [Service] 正在更新数据库：updatePost(" + id + ") ... ======");
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("更新失败, 文章未找到, ID: " + id));

        post.setTitle(postDetails.getTitle());
        post.setContent(postDetails.getContent());

        return postRepository.save(post); // 返回值 (post) 会被放入 "post::id" 缓存
    }

    /**
     * (DELETE /posts/{id}) 删除文章
     *
     * 知识点：@Caching
     * 我们也需要干两件事：
     * 1. 【清除】 "post::id" 这个单篇缓存
     * 2. 【清除】 "post_list" 这个列表缓存
     */
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = "post", key = "#id"),
                    @CacheEvict(cacheNames = "post_list", allEntries = true)
            }
    )
    public void deletePost(Long id) {
        System.out.println("====== [Service] 正在删除数据库：deletePost(" + id + ") ... ======");
        if (!postRepository.existsById(id)) {
            throw new ResourceNotFoundException("删除失败, 文章未找到");
        }
        postRepository.deleteById(id);
    }

    /**
     * 【新功能】获取 Top N 排行榜
     * @param topN 要获取前 N 个
     * @return 排序后的 Post 列表
     */
    public List<Post> getTopViewedPosts(int topN) {
        System.out.println("====== [Service] 正在查询 Redis ZSet：getTopViewedPosts(" + topN + ") ... ======");

        // 1. (Redis) 从 ZSet 获取“Top N”的 ID 和分数
        // opsForZSet().reverseRangeWithScores(key, start, end)
        // 对应 redis-cli 命令: ZREVRANGE post:view_counts 0 4 WITHSCORES
        // (start=0, end=topN-1)
        Set<ZSetOperations.TypedTuple<String>> tuples =
                stringRedisTemplate.opsForZSet().reverseRangeWithScores(
                        POST_VIEW_COUNT_KEY,
                        0,
                        topN - 1
                );

        // 如果 Redis 里没数据，返回空列表
        if (tuples == null || tuples.isEmpty()) {
            return List.of(); // Java 9+ 的 List.of()
        }

        // 2. (Java) 提取 ID
        // 我们需要一个【有序】的 ID 列表，以保持排行榜的顺序
        List<Long> postIdsInOrder = tuples.stream()
                .map(tuple -> Long.parseLong(tuple.getValue())) // .getValue() 获取的是成员 (Post ID)
                // .map(tuple -> tuple.getScore()) // (如果需要分数，可以这样取)
                .collect(Collectors.toList());

        // 把分数存入MAP
        Map<Long, Long> scoreMap = tuples.stream().collect(Collectors.toMap(
                tuple -> Long.parseLong(tuple.getValue()),
                tuple -> Math.round(tuple.getScore())
        ));

        // 3. (DB) 根据 ID 列表，一次性从数据库查出所有 Post
        // postRepository.findAllById() 返回的 List 是【无序】的！
        // (JPA 不保证返回的顺序和
        List<Post> postsFromDb = postRepository.findAllById(postIdsInOrder);

        // 4. (Java) 【关键】按 Redis 的顺序重新排序
        // 我们必须把“无序”的 DB 结果，按照 Redis 返回的“有序”ID 列表重新排序

        // (为了高效排序，先把 DB 结果转为 Map)
        Map<Long, Post> postMap = postsFromDb.stream()
                .collect(Collectors.toMap(Post::getId, post -> post));

        // (遍历“有序”的 ID 列表，从 Map 中取出 Post，放入最终结果)
        List<Post> sortedPosts = postIdsInOrder.stream()
                .map(new Function<Long, Post>() {
                    @Override
                    public Post apply(Long aLong) {
                        Post post = postMap.get(aLong);
                        long longValue2 = scoreMap.get(aLong);
                        post.setViewCount(longValue2);
                        return post;
                    }
                })
                .filter(post -> post != null) // 过滤掉可能在 Redis 有但在 DB 已删除的数据
                .collect(Collectors.toList());

        return sortedPosts;
    }
}