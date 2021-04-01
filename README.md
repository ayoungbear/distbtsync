# Distbtsync
Hellow! 

这是一款在分布式场景中用于协调同步的轻量级SDK，提供了分布式锁实现、集成 Spring 框架提供分布式同步注解等功能，方便我们在分布式场景下进行同步控制等操作。

## 集成
- JDK 1.8+
- Spring 5.1+
- Spring Boot 2.1+

1). 引入 jar 包

distbtsync-x.y.z.jar

2). 使用 maven
```xml
<dependency>
    <groupId>ayoungbear</groupId>
    <artifactId>distbtsync</artifactId>
    <version>x.y.z</version>
</dependency>
```
### 集成 Spring
可参考基于 Spring Boot 的 demo 工程：[distbtsync-demo](https://github.com/ayoungbear/distbtsync/tree/master/distbtsync-demo)

该工程提供了一些使用和配置样例，还有针对分布式并发场景的模拟测试。
1. 修改服务端口，本地启动多个服务模拟分布式场景。
2. 调用测试接口触发模拟测试分布式并发场景（自动广播）。
3. 调用验证接口验证分布式并发控制结果。

## Distributed lock
提供了分布式锁的相关实现，并扩展了功能。

### Redis lock
基于 [redis](https://github.com/redis/redis)、Pub/Sub、LUA、AQS 实现的可重入分布式锁 [RedisBasedLock](https://github.com/ayoungbear/distbtsync/blob/master/src/main/java/com/ayoungbear/distbtsync/redis/lock/RedisBasedLock.java)，有公平（相对）和非公平两种模式，提供了阻塞、超时、可中断等加锁方式以及其他便利方法。

该分布式锁不依赖于其他组件，可以根据项目所使用的 redis 客户端进行灵活配置，甚至可以自行实现而不依赖已有客户端，只需要实现定义的基础操作接口 [RedisLockCommands](https://github.com/ayoungbear/distbtsync/blob/master/src/main/java/com/ayoungbear/distbtsync/redis/lock/RedisLockCommands.java)。
接口需实现的功能有：
1. 脚本执行，用于实现原子操作。
2. 发布订阅，用于解锁消息通知。

已提供了一些目前流行的 redis 客户端的相关接口实现类，例如 [lettuce](https://github.com/lettuce-io/lettuce-core)、[jedis](https://github.com/redis/jedis)、[spring-data-redis](https://github.com/spring-projects/spring-data-redis) 等。

How to use 如何使用：

1). 获取分布式锁
```java
// 使用jedis客户端 (redis.clients.jedis.JedisCluster)
RedisLockCommands commands = new JedisClusterCommandsAdapter(jedisCluster); 

// 使用lettuce客户端 (io.lettuce.core.cluster.RedisClusterClient)
RedisLockCommands commands = new LettuceClusterClientCommandsAdapter(redisClusterClient); 

// 使用RedisConnection (org.springframework.data.redis.connection.RedisConnectionFactory)
RedisLockCommands commands = new RedisConnectionCommandsAdapter(redisConnectionFactory); 

// 使用RedisTemplate (org.springframework.data.redis.core.StringRedisTemplate)
RedisLockCommands commands = new RedisTemplateCommandsAdapter(stringRedisTemplate); 

// 创建分布式锁对象
RedisLock lock = new RedisBasedLock((String) key, commands); 
```
2). 使用分布式锁
```java
// 持续阻塞直到加锁成功
lock.lock(); 
// 10s内阻塞加锁超时则退出返回false，加锁成功则设置锁过期时间为60s并返回true
lock.tryLockTimed(10000, 60000, TimeUnit.MILLISECONDS); 
// 可中断模式下加锁，持续阻塞直到加锁成功或者被中断
lock.lockInterruptibly(); 
...
// 解锁，未持有锁则抛出异常
lock.unlock(); 
// 尝试释放锁，如果锁归自己持有则解锁返回true，否则返回false
lock.releaseLock(); 
// 强制解锁
lock.forceUnlock(); 
```
更多信息请参考 code 或 apidocs 。

PS：已对该分布式锁进行了单元测试与稳定性测试（例如模拟分布式场景 1000 个线程并发计数等），根据目前多项测试情况看，性能上是要优于目前流行的 [redisson](https://github.com/redisson/redisson) 分布式锁的，但是锁的功能和类型没有 redisson lock 丰富。

## Spring
在 Spring framwork 的基础上提供了同步相关的便利功能，如分布式同步标记注解，以及一些便利模板类等。

### Redis Sync

基于 redis 分布式锁、Spring AOP 实现的同步标记注解功能组件。

注解支持方法或者类/接口级别的标记，用来实现分布式并发场景下方法级别的同步调用，还支持基于 Spring 容器和方法调用上下文的 SpEL（Spring Expression Language）表达式解析，实现 redis 分布式锁所用键值与过期时间等属性的灵活配置，让分布式场景下的同步方法调用控制更加方便。

How to use 如何使用：

1). 开启 redis 同步注解功能

可在配置类上使用 [@EnableRedisSync](https://github.com/ayoungbear/distbtsync/blob/master/src/main/java/com/ayoungbear/distbtsync/spring/redis/EnableRedisSync.java) 注解来启用该功能，例如：
```java
@Configuration
@EnableRedisSync
public class MyRedisSyncConfiguration {
   // various @Bean definitions
}
```
如果是 Spring Boot 还可以使用自动配置 [distbtsync-redis-spring-boot-starter](https://github.com/ayoungbear/distbtsync/tree/master/distbtsync-redis-spring-boot-starter)，maven 引用：
```xml
<dependency>
    <groupId>ayoungbear</groupId>
    <artifactId>distbtsync-redis-spring-boot-starter</artifactId>
    <version>x.y.z</version>
</dependency>
```

2). 使用 redis 同步注解功能

在需要同步的方法上标记 [@RedisSync](https://github.com/ayoungbear/distbtsync/blob/master/src/main/java/com/ayoungbear/distbtsync/spring/redis/RedisSync.java) 注解，表示该方法需要同步执行，也可以在类或者接口上标记，此时会作用于类下的非私有方法或接口方法。

注解可指定 redis 分布式锁所需要的相关信息，包括使用的 key、过期时间等，可通过表达式根据上下文指定。如果未指定将使用默认的配置。该注解隐式支持“继承”，可利用此特点配置组合注解或者模板注解（例如 @RequestMapping 与 @GetMapping）等。

相关属性说明：
- name：同步使用的标识，即为 redis 分布式锁所使用的键值 key。默认使用的锁 key 为方法的全限定名。
- leaseTime：redis 分布式锁的过期时间，小于等于 0 表示不设置过期时间，锁状态将会一直存在直到解锁。默认的过期时间（ms）可通过 ayoungbear.distbtsync.spring.redis.defaultLeaseTime 项进行配置，默认无过期时间。
- waitTime：加锁时的阻塞等待超时时间，小于 0 表示持续阻塞直到加锁成功，等于 0 表示只会尝试加锁一次，大于 0 表示最大的阻塞等待时间。默认的等待超时时间（ms）可通过 ayoungbear.distbtsync.spring.redis.defaultWaitTime 项进行配置，默认为持续阻塞直到加锁成功。
- timeUnit：时间单位，默认为毫秒（ms）。
- handlerQualifier：指定同步失败异常处理器 [SyncMethodFailureHandler](https://github.com/ayoungbear/distbtsync/blob/master/src/main/java/com/ayoungbear/distbtsync/spring/SyncMethodFailureHandler.java)  的限定名，实例需要注册到 Spring 上下文中，同步失败时将会根据此名从上下文获取指定的处理器来进行相应的异常处理。

在需要同步调用的方法上标记该注解：
```java
// 不指定则使用默认值 key="public void x.x.x.x.syncAdd(java.lang.String)"
@RedisSync
public void syncMethod(String param){...};

// 直接设置值 key="myKey"
@RedisSync("myKey")
public void syncMethod(String param){...};

// 通过占位符的方式设置值，假设配置 syncMethodKey=myKey key="myKey"
@RedisSync("${syncMethodKey}")
public void syncMethod(String param){...};

// 设置为前缀+调用方法名 key="prefix_syncMethod"
@RedisSync("prefix_#{#methodName}")
public void syncMethod(String param){...};

// 与方法参数有关，假设 param 为 “name” 则 key="param_name"
@RedisSync("param_#{#param}")
public void syncMethod(String param){...};

// 方法参数为对象，和对象中的属性有关，假设 pojo.id 为 "233" 则 key="pojoId_233"
// 如果有相应 get/set 方法，表达式还可以写成 "pojoId_#{#pojo.getId()}"
@RedisSync("pojoId_#{#pojo.id}")
public void syncMethod(Pojo pojo){...};

// 直接设置过期时间为 60000ms（60s），等待超时的时间为 4000ms（4s）
@RedisSync(leaseTime = "60000", waitTime = "4000")
public void syncMethod(String param){...};

// 通过占位符的方式设置过期和超时时间，解析后的结果必须是数值
@RedisSync(leaseTime = "${myLeaseTime}", waitTime = "${myWaitTime}")
public void syncMethod(String param){...};

// 通过参数指定过期和超时时间
@RedisSync(leaseTime = "#{#leaseTime}", waitTime = "#{#waitTime}")
public void syncMethod(long leaseTime, long waitTime){...};
```
另外还支持自定义配置的功能，例如自定义同步器、自定义表达式解析器、自定义默认的异常处理器，该功能可通过 [SyncMethodFailureHandler](https://github.com/ayoungbear/distbtsync/blob/master/src/main/java/com/ayoungbear/distbtsync/spring/redis/RedisSyncConfigurer.java) 来配置实现。例如，可通过自定义同步器的方式来改变底层所使用的分布式锁实现，比如可更换为使用 redisson 的分布式锁（demo 工程有相关实现样例）。

更多信息请参考 code 或 apidocs 。关于 SpEL 表达式的使用请参考 Spring 官方文档。

## License

- [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0)

## 其他

- 作者 wechat：yzx4007
- github ：https://github.com/ayoungbear/distbtsync
- demo ：https://github.com/ayoungbear/distbtsync/tree/master/distbtsync-demo


