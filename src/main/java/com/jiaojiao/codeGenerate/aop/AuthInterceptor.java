package com.jiaojiao.codeGenerate.aop;

import com.jiaojiao.codeGenerate.annotation.AuthCheck;
import com.jiaojiao.codeGenerate.exception.BusinessException;
import com.jiaojiao.codeGenerate.exception.ErrorCode;
import com.jiaojiao.codeGenerate.model.entity.User;
import com.jiaojiao.codeGenerate.model.enums.UserRoleEnum;
import com.jiaojiao.codeGenerate.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
public class AuthInterceptor {

    @Resource
    private UserService userService;

    /**
     * 执行拦截
     * @param joinPoint
     * @param authCheck
     * @return
     * @throws Throwable
     */
    @Around("@annotation(authCheck)")
    public Object doInterceptor(ProceedingJoinPoint joinPoint, AuthCheck authCheck)  throws Throwable {
        String mustRole = authCheck.mustRole();
        RequestAttributes requestAttributes = RequestContextHolder.currentRequestAttributes();
        HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();
        // 获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        UserRoleEnum mustRoleEnum = UserRoleEnum.getEnumByValue(mustRole);
        // 不需要权限，直接放行
        if (mustRoleEnum == null){
            return joinPoint.proceed();
        }
        // 必须有该权限才通过
        UserRoleEnum userRoleEnum = UserRoleEnum.getEnumByValue(loginUser.getUserRole());
        // 没有权限
        if (userRoleEnum == null){
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 必须是管理员,但当前用户没有管理员权限
        if (UserRoleEnum.ADMIN.equals(mustRoleEnum) && !UserRoleEnum.ADMIN.equals(userRoleEnum)){
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "权限不足");
        }
        // 通过权限校验，放行
        return joinPoint.proceed();
    }
}
