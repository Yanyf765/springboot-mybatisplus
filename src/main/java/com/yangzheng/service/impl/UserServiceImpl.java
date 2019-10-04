package com.yangzheng.service.impl;

import com.baomidou.mybatisplus.mapper.Wrapper;
import com.yangzheng.entity.User;
import com.yangzheng.mapper.UserMapper;
import com.yangzheng.service.IUserService;
import com.baomidou.mybatisplus.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author Adam
 * @since 2019-10-04
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {
    @Autowired
    UserMapper userMapper;

    @Override
    public boolean insert(User entity) {
        return super.insert(entity);
    }

    @Override
    public User selectOne(Wrapper<User> wrapper) {
        return super.selectOne(wrapper);
    }
}
