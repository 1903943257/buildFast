package com.jiaojiao.codeGenerate.service;

import com.jiaojiao.codeGenerate.model.dto.app.AppAddRequest;
import com.jiaojiao.codeGenerate.model.dto.app.AppQueryRequest;
import com.jiaojiao.codeGenerate.model.entity.User;
import com.jiaojiao.codeGenerate.model.vo.AppVO;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.jiaojiao.codeGenerate.model.entity.App;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * 应用 服务层。
 *
 * @author jiaojiao
 */
public interface AppService extends IService<App> {

    /**
     * 通过对话生成应用代码
     * @param appId
     * @param message
     * @param loginUser
     * @return
     */
    Flux<String> chatToGenCode(Long appId, String message, User loginUser);

    /**
     * 创建应用
     * @param appAddRequest
     * @param loginUser
     * @return
     */
    Long createApp(AppAddRequest appAddRequest, User loginUser);

    /**
     * 应用部署
     * @param appId 应用Id
     * @param loginUser 登录用户
     * @return 可访问的部署地址
     */
    String deployApp(Long appId, User loginUser);

    /**
     * 异步生成应用截图
     * @param appId 应用Id
     * @param appDeployUrl 应用部署地址
     */
    void generateAppScreenshotAsync(Long appId, String appDeployUrl);

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
