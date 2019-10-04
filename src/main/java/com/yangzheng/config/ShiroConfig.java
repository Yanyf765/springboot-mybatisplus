package com.yangzheng.config;

import com.yangzheng.filter.KickoutSessionControlFilter;
import com.yangzheng.realm.UserRealm;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.session.mgt.eis.JavaUuidSessionIdGenerator;
import org.apache.shiro.spring.LifecycleBeanPostProcessor;
import org.apache.shiro.spring.web.ShiroFilterFactoryBean;
import org.apache.shiro.web.mgt.DefaultWebSecurityManager;
import org.apache.shiro.web.servlet.SimpleCookie;
import org.apache.shiro.web.session.mgt.DefaultWebSessionManager;
import org.crazycake.shiro.RedisCacheManager;
import org.crazycake.shiro.RedisManager;
import org.crazycake.shiro.RedisSessionDAO;
import org.springframework.aop.framework.autoproxy.DefaultAdvisorAutoProxyCreator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

import java.util.LinkedHashMap;
import javax.servlet.Filter;

@Configuration
public class ShiroConfig {

    @Bean
    public RedisManager redisManager(){
        RedisManager redisManager = new RedisManager();     // crazycake 实现
//        RedisClusterManager redisClusterManager = new RedisClusterManager();
        redisManager.setHost("localhost:6379");
        redisManager.setTimeout(180000);
        return redisManager;
    }

    @Bean
    public JavaUuidSessionIdGenerator sessionIdGenerator(){
        return new JavaUuidSessionIdGenerator();
    }

    @Bean
    public RedisSessionDAO sessionDAO(){
        RedisSessionDAO sessionDAO = new RedisSessionDAO(); // crazycake 实现
        sessionDAO.setRedisManager(redisManager());
        sessionDAO.setSessionIdGenerator(sessionIdGenerator()); //  Session ID 生成器
        return sessionDAO;
    }

    @Bean
    public SimpleCookie cookie(){
        SimpleCookie cookie = new SimpleCookie("SHAREJSESSIONID"); //  cookie的name,对应的默认是 JSESSIONID
        cookie.setHttpOnly(true);
        cookie.setPath("/");        //  path为 / 用于多个系统共享JSESSIONID
        return cookie;
    }

    @Bean
    public DefaultWebSessionManager sessionManager(){
        DefaultWebSessionManager sessionManager = new DefaultWebSessionManager();
        sessionManager.setGlobalSessionTimeout(-1000L);    // 设置session超时
        sessionManager.setDeleteInvalidSessions(true);      // 删除无效session
        sessionManager.setSessionIdCookie(cookie());            // 设置JSESSIONID
        sessionManager.setSessionDAO(sessionDAO());         // 设置sessionDAO
        return sessionManager;
    }

    /**
     * 1. 配置SecurityManager
     * @return
     */
    @Bean
    public DefaultWebSecurityManager securityManager(){
        DefaultWebSecurityManager securityManager = new DefaultWebSecurityManager();
        securityManager.setRealm(realm());  // 设置realm
        securityManager.setSessionManager(sessionManager());    // 设置sessionManager
        securityManager.setCacheManager(redisCacheManager()); // 配置缓存的话，退出登录的时候crazycake会报错，要求放在session里面的实体类必须有个id标识
        return securityManager;
    }

    /**
     * 2. 配置缓存
     * @return
     */
//    @Bean
//    public CacheManager cacheManager(){
//        EhCacheManager ehCacheManager = new EhCacheManager();
//        ehCacheManager.setCacheManagerConfigFile("classpath:ehcache.xml");
//        return ehCacheManager;
//    }

    @Bean
    public RedisCacheManager redisCacheManager(){
        RedisCacheManager cacheManager = new RedisCacheManager();   // crazycake 实现
        cacheManager.setRedisManager(redisManager());
        return cacheManager;
    }

    /**
     * 3. 配置Realm
     * @return
     */
    @Bean
    public AuthorizingRealm realm(){
        return new UserRealm();
    }

    /**
     * 4. 配置LifecycleBeanPostProcessor，可以来自动的调用配置在Spring IOC容器中 Shiro Bean 的生命周期方法
     * @return
     */
    @Bean
    public LifecycleBeanPostProcessor lifecycleBeanPostProcessor(){
        return new LifecycleBeanPostProcessor();
    }

    /**
     * 5. 启用IOC容器中使用Shiro的注解，但是必须配置第四步才可以使用
     * @return
     */
    @Bean
    @DependsOn("lifecycleBeanPostProcessor")
    public DefaultAdvisorAutoProxyCreator defaultAdvisorAutoProxyCreator(){
        return new DefaultAdvisorAutoProxyCreator();
    }

    /**
     * 6. 配置ShiroFilter
     * @return
     */
    @Bean
    public ShiroFilterFactoryBean shiroFilterFactoryBean(){
        LinkedHashMap<String, String> map = new LinkedHashMap<>();
        // 静态资源
        map.put("/css/**", "anon");
        map.put("/js/**", "anon");

        // 公共路径
        map.put("/login", "anon");
        map.put("/register", "anon");
        map.put("/user/login", "kickout,anon");
        map.put("/user/logout", "anon");
        //map.put("/*", "anon");

        // 登出,项目中没有/logout路径,因为shiro是过滤器,而SpringMVC是Servlet,Shiro会先执行
        map.put("/logout", "logout");

        // 授权
//        map.put("/user/**", "authc,roles[user]");
        map.put("/admin/**", "authc,roles[admin]");

        // everything else requires authentication:
        map.put("/**", "authc");

        ShiroFilterFactoryBean factoryBean = new ShiroFilterFactoryBean();
        // 配置SecurityManager
        factoryBean.setSecurityManager(securityManager());
        // 配置权限路径
        factoryBean.setFilterChainDefinitionMap(map);

        //自定义拦截器限制并发人数,参考博客：
        LinkedHashMap<String, Filter> filtersMap = new LinkedHashMap<>();
        //限制同一帐号同时在线的个数
        filtersMap.put("kickout", kickoutSessionControlFilter());
        //统计登录人数
        factoryBean.setFilters(filtersMap);

        // 配置登录url
        factoryBean.setLoginUrl("/");
        // 配置无权限路径
        factoryBean.setUnauthorizedUrl("/unauthorized");
        return factoryBean;
    }

    /**
     * 并发登录控制
     * @return
     */
    @Bean
    public KickoutSessionControlFilter kickoutSessionControlFilter(){
        KickoutSessionControlFilter kickoutSessionControlFilter = new KickoutSessionControlFilter();
        //用于根据会话ID，获取会话进行踢出操作的；
        kickoutSessionControlFilter.setSessionManager(sessionManager());
        //使用cacheManager获取相应的cache来缓存用户登录的会话；用于保存用户—会话之间的关系的；
        kickoutSessionControlFilter.setRedisManager(redisManager());
        kickoutSessionControlFilter.setCacheManager(redisCacheManager());
        //是否踢出后来登录的，默认是false；即后者登录的用户踢出前者登录的用户；
        kickoutSessionControlFilter.setKickoutAfter(true);
        //同一个用户最大的会话数，默认1；比如2的意思是同一个用户允许最多同时两个人登录；
        kickoutSessionControlFilter.setMaxSession(1);
        //被踢出后重定向到的地址；
        kickoutSessionControlFilter.setKickoutUrl("/login?kickout=1");
        return kickoutSessionControlFilter;
    }

}
