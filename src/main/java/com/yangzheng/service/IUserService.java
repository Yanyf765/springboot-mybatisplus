package com.yangzheng.service;

import com.baomidou.mybatisplus.mapper.Wrapper;
import com.yangzheng.entity.User;
import com.baomidou.mybatisplus.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author Adam
 * @since 2019-10-04
 */
public interface IUserService extends IService<User> {
   boolean insert(User user);

   User selectOne(Wrapper<User> wrapper);
}
