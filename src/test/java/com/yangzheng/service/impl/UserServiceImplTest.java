package com.yangzheng.service.impl;

import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.mapper.Wrapper;
import com.yangzheng.SpringbootMybatisplusApplicationTests;
import com.yangzheng.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.Assert.*;

@Slf4j
public class UserServiceImplTest extends SpringbootMybatisplusApplicationTests {

    @Autowired
    UserServiceImpl userService;

    @Test
    public void selectOne() {
        Wrapper<User> wrapper = new EntityWrapper<>();
        wrapper.eq("id",2);
        User user = userService.selectOne(wrapper);
        log.info("user={}",user);
    }
}