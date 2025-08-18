package com.jiaojiao.yuaicodemother.service;

import com.jiaojiao.yuaicodemother.model.dto.app.AppQueryRequest;
import com.jiaojiao.yuaicodemother.model.vo.AppVO;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.jiaojiao.yuaicodemother.model.entity.App;

import java.util.List;

/**
 * 应用 服务层。
 *
 * @author jiaojiao
 */
public interface AppService extends IService<App> {

    /**
     * 获取应用封装类
     * @param app
     * @return
     */
    AppVO getAppVO(App app);

    /**
     * 获取应用封装列表
     * @param appList
     * @return
     */
    List<AppVO> getAppVOList(List<App> appList);

    /**
     * 构造应用查询条件
     * @param appQueryRequest
     * @return
     */
    QueryWrapper getQueryWrapper(AppQueryRequest appQueryRequest);



}
