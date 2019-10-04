package com.yangzheng.web;


import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.mapper.Wrapper;
import com.yangzheng.config.ShiroConfig;
import com.yangzheng.entity.User;
import com.yangzheng.service.IUserService;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.cache.Cache;
import org.apache.shiro.subject.Subject;
import org.crazycake.shiro.RedisCacheManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author Adam
 * @since 2019-10-04
 */
@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    IUserService userService;

    @Autowired
    ShiroConfig shiroConfig;

    @RequestMapping("/insert")
    public boolean insert(){
        User user = new User();
        user.setId(4L);
        user.setUsername("yangzheng4");
        user.setPassword("1234");
        return userService.insert(user);
    }

    @RequestMapping("/selectOne")
    public User selectOne(){
        Wrapper<User> wrapper = new EntityWrapper<>();
        wrapper.eq("username", "yangzheng1");
//        wrapper.eq("id",2);
        return userService.selectOne(wrapper);
    }

    @RequestMapping("/login")
    public User login(@RequestParam(value = "username") String username,
                      @RequestParam(value = "password") String password){
        Subject subject = SecurityUtils.getSubject();
        subject.login(new UsernamePasswordToken(username, password));
        User user = (User) SecurityUtils.getSubject().getPrincipal();
        return user;
    }

    @RequestMapping("/logout")
    public Boolean logout(@RequestParam(value = "username") String username){
        Subject subject = SecurityUtils.getSubject();
        subject.logout();
        System.out.println("username="+username);
        RedisCacheManager redisCacheManager = shiroConfig.redisCacheManager();
        Cache<Object, Object> cache = redisCacheManager.getCache("shiro_redis_cache:");
        cache.remove(username);
        return true;
    }
}
