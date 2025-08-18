package com.jiaojiao.yuaicodemother.mapper;

import com.jiaojiao.yuaicodemother.model.entity.App;
import com.mybatisflex.core.BaseMapper;
import com.jiaojiao.yuaicodemother.model.entity.User;

/**
 * 用户 映射层。
 *
 * @author jiaojiao
 */
public interface UserMapper extends BaseMapper<User> {

    /**
     * 应用 映射层。
     *
     * @author jiaojiao
     */
    interface AppMapper extends BaseMapper<App> {

    }
}
