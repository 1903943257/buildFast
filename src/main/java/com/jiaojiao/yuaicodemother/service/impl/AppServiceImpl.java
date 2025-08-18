package com.jiaojiao.yuaicodemother.service.impl;

import com.jiaojiao.yuaicodemother.mapper.UserMapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.jiaojiao.yuaicodemother.model.entity.App;
import com.jiaojiao.yuaicodemother.service.AppService;
import org.springframework.stereotype.Service;

/**
 * 应用 服务层实现。
 *
 * @author jiaojiao
 */
@Service
public class AppServiceImpl extends ServiceImpl<UserMapper.AppMapper, App>  implements AppService{

}
