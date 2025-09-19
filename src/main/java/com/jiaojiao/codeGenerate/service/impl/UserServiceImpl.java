package com.jiaojiao.codeGenerate.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import com.jiaojiao.codeGenerate.exception.BusinessException;
import com.jiaojiao.codeGenerate.exception.ErrorCode;
import com.jiaojiao.codeGenerate.mapper.UserMapper;
import com.jiaojiao.codeGenerate.model.dto.user.UserQueryRequest;
import com.jiaojiao.codeGenerate.model.entity.User;
import com.jiaojiao.codeGenerate.model.enums.UserRoleEnum;
import com.jiaojiao.codeGenerate.model.vo.LoginUserVO;
import com.jiaojiao.codeGenerate.model.vo.UserVO;
import com.jiaojiao.codeGenerate.service.UserService;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.stream.Collectors;

import static com.jiaojiao.codeGenerate.constant.UserConstant.USER_LOGIN_STATE;

/**
 * 用户 服务层实现。
 *
 * @author jiaojiao
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User>  implements UserService {

    /**
     * 用户注册
     * @param userAccount   用户账户
     * @param userPassword  用户密码
     * @param checkPassword 校验密码
     * @return 新用户id
     */
    @Override
    public long userRegister(String userAccount, String userPassword, String checkPassword) {
        // 1. 校验参数
        if(StrUtil.hasBlank(userAccount,userPassword,checkPassword)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if(userAccount.length()<4){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号长度过短");
        }
        if (userPassword.length()<8 || checkPassword.length()<8){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码长度过短");
        }
        if (!userPassword.equals(checkPassword)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次输入的密码不一致");
        }

        // 2. 查询用户是否已经存在
        QueryWrapper queryWrapper = new QueryWrapper();
        queryWrapper.eq("userAccount", userAccount);
        long count = this.mapper.selectCountByQuery(queryWrapper);
        if (count > 0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号重复");
        }

        // 3. 加密密码
        String encryptPassword = getEncryptPassword(userPassword);

        // 4. 创建用户，插入数据库
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(encryptPassword);
        user.setUserName("焦焦");
        user.setUserRole(UserRoleEnum.USER.getValue());
        boolean saveResult = this.save(user);
        if (!saveResult){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "注册失败，数据库错误");
        }
        return user.getId();
    }

    /**
     * 用户登录
     *
     * @param userAccount  用户账户
     * @param userPassword 用户密码
     * @param request
     * @return 脱敏后的用户信息
     */
    @Override
    public LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        // 1. 校验参数
        if(StrUtil.hasBlank(userAccount,userPassword)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if(userAccount.length()<4){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号长度过短");
        }
        if (userPassword.length()<8){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码长度过短");
        }

        // 2. 加密
        String encryptPassword = getEncryptPassword(userPassword);
        // 3. 查询用户是否存在
        QueryWrapper queryWrapper = new QueryWrapper();
        queryWrapper.eq("userAccount", userAccount);
        queryWrapper.eq("userPassword", encryptPassword);
        User user = this.mapper.selectOneByQuery(queryWrapper);
        if (user == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不存在或密码错误");
        }
        // 4. 记录用户的登录态
        request.getSession().setAttribute(USER_LOGIN_STATE, user);
        // 5. 返回脱敏后的用户信息
        return this.getLoginUserVO(user);
    }

    /**
     * 获取当前登录用户
     * @param request  请求对象
     * @return  当前登录用户
     */
    @Override
    public User getLoginUser(HttpServletRequest request) {
        // 先判断用户是否登录
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if (currentUser == null || currentUser.getId() == null){
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        // 从数据库查询当前用户信息
        long userId = currentUser.getId();
        currentUser = this.getById(userId);
        if (currentUser == null){
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        return currentUser;
    }

    /**
     * 获取登录用户视图(脱敏)
     * @param user 用户信息
     * @return  用户视图
     */
    @Override
    public UserVO getUserVO(User user) {
        if (user == null){
            return null;
        }
        UserVO userVO = new UserVO();
        BeanUtil.copyProperties(user, userVO);
        return userVO;
    }

    /**
     * 获取登录用户列表(脱敏)
     * @param userList 用户信息
     * @return  用户视图列表
     */
    @Override
    public List<UserVO> getUserVOList(List<User> userList) {
        if (CollectionUtil.isEmpty(userList)){
            return new ArrayList<>();
        }
        return userList.stream()
                .map(this::getUserVO)
                .collect(Collectors.toList());
    }


    /**
     * 获取登录用户视图
     * @param user  用户实体
     * @return  登录用户视图
     */
    @Override
    public LoginUserVO getLoginUserVO(User user) {
        if (user == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不存在");
        }
        LoginUserVO loginUserVO = new LoginUserVO();
        BeanUtil.copyProperties(user, loginUserVO);
        return loginUserVO;
    }

    /**
     * 用户注销
     * @param request  请求对象
     */
    @Override
    public boolean userLogout(HttpServletRequest request) {
        // 1. 打印当前会话的核心信息：会话ID、是否新会话、会话创建/过期时间
        HttpSession session = request.getSession();
        log.info("=== 退出登录会话信息 ===");
        log.info("会话ID（JSESSIONID）：{}", session.getId()); // 关键：确认登录和退出的会话ID是否一致
        log.info("是否新会话（新创建则说明旧会话已失效）：{}", session.isNew());
        log.info("会话创建时间：{}", new Date(session.getCreationTime()));
        log.info("会话最后访问时间：{}", new Date(session.getLastAccessedTime()));
        log.info("会话超时时间（秒）：{}", session.getMaxInactiveInterval());

        // 2. 打印会话中所有属性（查看是否有 USER_LOGIN_STATE，或是否有意外属性）
        Enumeration<String> attributeNames = session.getAttributeNames();
        log.info("=== 会话中所有属性 ===");
        while (attributeNames.hasMoreElements()) {
            String attrName = attributeNames.nextElement();
            Object attrValue = session.getAttribute(attrName);
            log.info("属性名：{}，属性值：{}（类型：{}）",
                    attrName, attrValue, attrValue != null ? attrValue.getClass().getName() : "null");
        }
        log.info("当前类中 USER_LOGIN_STATE 的值：{}", USER_LOGIN_STATE);
        log.info("当前类中 USER_LOGIN_STATE 的值是否等于 'user_login'：{}", USER_LOGIN_STATE.equals("user_login"));

        // 先判断用户是否登录
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        if (userObj == null){
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "用户未登录");
        }
        // 移除登录态
        request.getSession().removeAttribute(USER_LOGIN_STATE);
        return true;
    }

    /**
     * 根据查询条件查询参数
     * @param userQueryRequest 查询参数
     * @return 查询参数
     */
    @Override
    public QueryWrapper getQueryWrapper(UserQueryRequest userQueryRequest) {
        if (userQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        Long id = userQueryRequest.getId();
        String userAccount = userQueryRequest.getUserAccount();
        String userName = userQueryRequest.getUserName();
        String userProfile = userQueryRequest.getUserProfile();
        String userRole = userQueryRequest.getUserRole();
        String sortField = userQueryRequest.getSortField();
        String sortOrder = userQueryRequest.getSortOrder();
        return QueryWrapper.create()
                .eq("id", id)
                .eq("userRole", userRole)
                .like("userAccount", userAccount)
                .like("userName", userName)
                .like("userProfile", userProfile)
                .orderBy(sortField, "ascend".equals(sortOrder));
    }


    /**
     * 获取加密后的密码
     * @param userPassword  用户密码
     * @return  加密后的密码
     */
    @Override
    public String getEncryptPassword(String userPassword) {
        // 盐值，混淆空间
        final String SALT = "yupi";
        // MD5加密
        return DigestUtils.md5DigestAsHex((userPassword + SALT).getBytes(StandardCharsets.UTF_8));
    }
}
