package com.jiaojiao.codeGenerate.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.jiaojiao.codeGenerate.ai.AiCodeGenTypeRoutingService;
import com.jiaojiao.codeGenerate.ai.AiCodeGenTypeRoutingServiceFactory;
import com.jiaojiao.codeGenerate.constant.AppConstant;
import com.jiaojiao.codeGenerate.core.AiCodeGeneratorFacade;
import com.jiaojiao.codeGenerate.core.builder.VueProjectBuilder;
import com.jiaojiao.codeGenerate.core.handler.StreamHandlerExecutor;
import com.jiaojiao.codeGenerate.exception.BusinessException;
import com.jiaojiao.codeGenerate.exception.ErrorCode;
import com.jiaojiao.codeGenerate.exception.ThrowUtils;
import com.jiaojiao.codeGenerate.manager.CosManager;
import com.jiaojiao.codeGenerate.mapper.UserMapper;
import com.jiaojiao.codeGenerate.model.dto.app.AppAddRequest;
import com.jiaojiao.codeGenerate.model.dto.app.AppQueryRequest;
import com.jiaojiao.codeGenerate.model.entity.App;
import com.jiaojiao.codeGenerate.model.entity.User;
import com.jiaojiao.codeGenerate.model.enums.ChatHistoryMessageTypeEnum;
import com.jiaojiao.codeGenerate.model.enums.CodeGenTypeEnum;
import com.jiaojiao.codeGenerate.model.vo.AppVO;
import com.jiaojiao.codeGenerate.model.vo.UserVO;
import com.jiaojiao.codeGenerate.monitor.MonitorContext;
import com.jiaojiao.codeGenerate.monitor.MonitorContextHolder;
import com.jiaojiao.codeGenerate.service.AppService;
import com.jiaojiao.codeGenerate.service.ChatHistoryService;
import com.jiaojiao.codeGenerate.service.ScreenshotService;
import com.jiaojiao.codeGenerate.service.UserService;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.File;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 应用 服务层实现。
 *
 * @author jiaojiao
 */
@Service
public class AppServiceImpl extends ServiceImpl<UserMapper.AppMapper, App>  implements AppService{
    private static final Logger log = LoggerFactory.getLogger(AppServiceImpl.class);

    @Value("${code.deploy-host:http://localhost}")
    private String deployHost;

    @Resource
    private UserService userService;

    @Resource
    private CosManager cosManager;

    @Resource
    private ScreenshotService screenshotService;

    @Resource
    private AiCodeGeneratorFacade aiCodeGeneratorFacade;

    @Resource
    private ChatHistoryService chatHistoryService;

    @Resource
    private StreamHandlerExecutor streamHandlerExecutor;

    @Resource
    private VueProjectBuilder vueProjectBuilder;

    @Resource
    private AiCodeGenTypeRoutingServiceFactory aiCodeGenTypeRoutingServiceFactory;

    /**
     * 应用对话
     * @param appId 应用ID
     * @param message 提示词
     * @param loginUser 登录用户
     * @return  代码流
     */
    @Override
    public Flux<String> chatToGenCode(Long appId, String message, User loginUser) {
        // 1. 参数校验
        ThrowUtils.throwIf(appId == null || appId < 0, ErrorCode.PARAMS_ERROR, "应用ID不能为空");
        ThrowUtils.throwIf(StrUtil.isBlank(message), ErrorCode.PARAMS_ERROR, "提示词不能为空");
        // 2. 查询应用信息
        App app = this.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");

        // 3. 权限校验（仅本人可以和自己的应用对话）
        if(!app.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权访问该应用");
        }

        // 4. 获取应用的代码生成类型
        String codeGenType = app.getCodeGenType();
        CodeGenTypeEnum codeGenTypeEnum = CodeGenTypeEnum.getEnumByValue(codeGenType);
        if (codeGenTypeEnum == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "应用代码生成类型错误");
        }
        // 5. 在调用AI前，先保存用户消息到数据库中
        chatHistoryService.addChatMessage(appId, message, ChatHistoryMessageTypeEnum.USER.getValue(), loginUser.getId());
        // 6. 设置监控上下文
        // 6. 上下文设置
        MonitorContextHolder.setContext(MonitorContext.builder()
                .userId(loginUser.getId().toString())
                .appId(appId.toString())
                .build());
        // 7. 调用AI生成代码(流式)
        Flux<String> codeStream = aiCodeGeneratorFacade.generateAndSaveCodeStream(message, codeGenTypeEnum, appId);
        // 8. 收集AI响应内容，并且在完成后保存记录到历史对话
        return streamHandlerExecutor.doExecute(codeStream, chatHistoryService, appId, loginUser, codeGenTypeEnum)
                .doFinally(signalType -> {
                    // 9. 完成后清除监控上下文
                    MonitorContextHolder.clearContext();
                });
    }

    @Override
    public Long createApp(AppAddRequest appAddRequest, User loginUser) {
        // 参数校验
        String initPrompt = appAddRequest.getInitPrompt();
        ThrowUtils.throwIf(StrUtil.isBlank(initPrompt), ErrorCode.PARAMS_ERROR, "初始化 prompt 不能为空");
        // 构造入库对象
        App app = new App();
        BeanUtil.copyProperties(appAddRequest, app);
        app.setUserId(loginUser.getId());
        // 应用名称暂时为 initPrompt 前 12 位
        app.setAppName(initPrompt.substring(0, Math.min(initPrompt.length(), 12)));
        // 使用 AI 智能选择代码生成类型
        AiCodeGenTypeRoutingService aiCodeGenTypeRoutingService = aiCodeGenTypeRoutingServiceFactory.createAiCodeGenTypeRoutingService();
        CodeGenTypeEnum selectedCodeGenType = aiCodeGenTypeRoutingService.routeCodeGenType(initPrompt);
        app.setCodeGenType(selectedCodeGenType.getValue());
        // 插入数据库
        boolean result = this.save(app);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        log.info("应用创建成功，ID: {}, 类型: {}", app.getId(), selectedCodeGenType.getValue());
        return app.getId();
    }


    @Override
    public String deployApp(Long appId, User loginUser) {
        // 1. 参数校验
        ThrowUtils.throwIf(appId == null || appId < 0, ErrorCode.PARAMS_ERROR,"应用ID错误");
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR, "用户未登录");
        // 2. 查询应用信息
        App app = this.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        // 3. 权限校验
        if (!app.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限部署该应用");
        }
        // 4. 检查是否已有deploy
        String deployKey = app.getDeployKey();
        // 如果没有，生成6位deployKey（6位字母加数字）
        if (StrUtil.isBlank(deployKey)) {
            deployKey = RandomUtil.randomString(6);
        }
        // 5. 获取代码生成类型， 获取原始代码生成路径(应用访问目录)
        String codeGenType = app.getCodeGenType();
        String sourceDirName = codeGenType + "_" + appId;
        String sourceDirPath = AppConstant.CODE_OUTPUT_ROOT_DIR + File.separator + sourceDirName;
        // 6. 检查路径是否合法
        File sourceDir = new File(sourceDirPath);
        if (!sourceDir.exists() || !sourceDir.isDirectory()) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "应用代码路径不存在，请先生成应用");
        }
        // 7. Vue项目特殊处理：执行构建
        CodeGenTypeEnum codeGenTypeEnum = CodeGenTypeEnum.getEnumByValue(codeGenType);
        if (codeGenTypeEnum == CodeGenTypeEnum.VUE_PROJECT){
            // Vue项目需要构建
            boolean buildSuccess = vueProjectBuilder.buildProject(sourceDirPath);
            ThrowUtils.throwIf(!buildSuccess, ErrorCode.SYSTEM_ERROR, "Vue项目构建失败");
            // 检查dist目录是否存在
            File distDir = new File(sourceDirPath, "dist");
            ThrowUtils.throwIf(!distDir.exists(), ErrorCode.SYSTEM_ERROR, "Vue项目构建完成，但是dist目录不存在");
            sourceDir = distDir;
        }
        // 8. 复制文件到部署目录
        String deployDirPath = AppConstant.CODE_DEPLOY_ROOT_DIR+ File.separator + deployKey;
        try{
            FileUtil.copyContent(sourceDir, new File(deployDirPath), true);
        }catch (Exception e){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "应用部署失败" + e.getMessage());
        }
        // 9. 更新数据库
        App updateApp = new App();
        updateApp.setId(appId);
        updateApp.setDeployKey(deployKey);
        updateApp.setDeployedTime(LocalDateTime.now());
        boolean updateResult = this.updateById(updateApp);
        ThrowUtils.throwIf(!updateResult, ErrorCode.OPERATION_ERROR, "更新应用部署信息失败");
        // 10. 构建应用访问 URL
        String appDeployUrl = String.format("%s/%s/", deployHost, deployKey);
//        // 10. 得到可访问的URL地址 （本地）
//        String appDeployUrl = String.format("%s/%s", AppConstant.CODE_DEPLOY_HOST, deployKey);
        // 11. 异步执行截图服务并更新应用封面
        generateAppScreenshotAsync(appId, appDeployUrl);
        return appDeployUrl;
    }

    /**
     * 异步生成应用截图并更新封面
     * @param appId 应用id
     * @param appDeployUrl 部署地址
     */
    @Override
    public void generateAppScreenshotAsync(Long appId, String appDeployUrl) {
        Thread.startVirtualThread(() -> {
            // 调用截图服务并上传
            String screenshotUrl = screenshotService.generateAndUploadScreenshot(appDeployUrl);
            // 更新数据库封面
            App updateApp = new App();
            updateApp.setId(appId);
            updateApp.setCover(screenshotUrl);
            boolean updated = this.updateById(updateApp);
            ThrowUtils.throwIf(!updated, ErrorCode.OPERATION_ERROR, "更新应用封面字段失败");
        });
    }

    @Override
    public AppVO getAppVO(App app) {
        if (app == null) {
            return null;
        }
        AppVO appVO = new AppVO();
        BeanUtil.copyProperties(app, appVO);
        // 关联查询用户信息
        Long userId = app.getUserId();
        if (userId != null) {
            User user = userService.getById(userId);
            UserVO userVO = userService.getUserVO(user);
            appVO.setUser(userVO);
        }
        return appVO;
    }

    @Override
    public List<AppVO> getAppVOList(List<App> appList) {
        if (CollUtil.isEmpty(appList)) {
            return new ArrayList<>();
        }
        // 批量获取用户信息，避免 N+1 查询问题
        Set<Long> userIds = appList.stream()
                .map(App::getUserId)
                .collect(Collectors.toSet());
        Map<Long, UserVO> userVOMap = userService.listByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, userService::getUserVO));
        return appList.stream().map(app -> {
            AppVO appVO = getAppVO(app);
            UserVO userVO = userVOMap.get(app.getUserId());
            appVO.setUser(userVO);
            return appVO;
        }).collect(Collectors.toList());
    }

    @Override
    public QueryWrapper getQueryWrapper(AppQueryRequest appQueryRequest) {
        if (appQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        Long id = appQueryRequest.getId();
        String appName = appQueryRequest.getAppName();
        String cover = appQueryRequest.getCover();
        String initPrompt = appQueryRequest.getInitPrompt();
        String codeGenType = appQueryRequest.getCodeGenType();
        String deployKey = appQueryRequest.getDeployKey();
        Integer priority = appQueryRequest.getPriority();
        Long userId = appQueryRequest.getUserId();
        String sortField = appQueryRequest.getSortField();
        String sortOrder = appQueryRequest.getSortOrder();
        return QueryWrapper.create()
                .eq("id", id)
                .like("appName", appName)
                .like("cover", cover)
                .like("initPrompt", initPrompt)
                .eq("codeGenType", codeGenType)
                .eq("deployKey", deployKey)
                .eq("priority", priority)
                .eq("userId", userId)
                .orderBy(sortField, "ascend".equals(sortOrder));
    }

    /**
     * 删除应用时关联删除对话历史
     * @param id
     * @return
     */
    @Override
    public boolean removeById(Serializable id){
        if (id == null) {
            return false;
        }
        long appId = Long.parseLong(id.toString());
        if (appId <= 0) {
            return false;
        }
        // 先删除关联对话历史
        try {
            chatHistoryService.deleteByAppId(appId);
        } catch (Exception e) {
            log.error("删除相关应用关联的对话历史失败：{}", e.getMessage());
        }
        // 删除应用
        return super.removeById(id);
    }
}
