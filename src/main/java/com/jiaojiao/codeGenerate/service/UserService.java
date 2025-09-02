package com.jiaojiao.codeGenerate.service;

import com.jiaojiao.codeGenerate.model.dto.user.UserQueryRequest;
import com.jiaojiao.codeGenerate.model.vo.LoginUserVO;
import com.jiaojiao.codeGenerate.model.vo.UserVO;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.jiaojiao.codeGenerate.model.entity.User;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

/**
 * 用户 服务层。
 *
 * @author jiaojiao
 */
public interface UserService extends IService<User> {
    /**
     * 用户注册
     *
     * @param userAccount   用户账户
     * @param userPassword  用户密码
     * @param checkPassword 校验密码
     * @return 新用户 id
     */
    long userRegister(String userAccount, String userPassword, String checkPassword);

    /**
     * 用户登录
     *
     * @param userAccount  用户账户
     * @param userPassword 用户密码
     * @param request
     * @return 脱敏后的用户信息
     */
    LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request);

    /**
     * 获取当前登录用户
     * @param request
     * @return
     */
    User getLoginUser(HttpServletRequest request);

    /**
     * 获取登录用户视图(脱敏)
     * @param user 用户信息
     * @return
     */
    UserVO getUserVO(User user);

    /**
     * 获取登录用户视图(脱敏)
     * @param userList 用户信息
     * @return
     */
    List<UserVO> getUserVOList(List<User> userList);

    /**
     * 获取登录用户视图
     * @param user
     * @return
     */
    LoginUserVO getLoginUserVO(User user);

    /**
     * 用户注销
     * @param request
     */
    boolean userLogout(HttpServletRequest request);


    /**
     * 根据查询条件查询参数
     * @param userQueryRequest 查询参数
     * @return
     */
    QueryWrapper getQueryWrapper(UserQueryRequest userQueryRequest);

    /**
     * 加密
     * @param userPassword 用户密码
     * @return 加密后的密码
     */
    String getEncryptPassword(String userPassword);
}
