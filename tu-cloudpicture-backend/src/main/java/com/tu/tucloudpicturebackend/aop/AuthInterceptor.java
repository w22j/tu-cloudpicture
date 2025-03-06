package com.tu.tucloudpicturebackend.aop;

import com.tu.tucloudpicturebackend.annotation.AuthCheck;
import com.tu.tucloudpicturebackend.common.ErrorCode;
import com.tu.tucloudpicturebackend.exception.BusinessException;
import com.tu.tucloudpicturebackend.model.entity.User;
import com.tu.tucloudpicturebackend.model.enums.UserRoleEnum;
import com.tu.tucloudpicturebackend.service.UserService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

@Aspect
@Component
public class AuthInterceptor {

    @Resource
    private UserService userService;

    /**
     * 拦截方法
     *
     * @param proceedingJoinPoint 切入点
     * @param authCheck           权限校验
     */
    @Around("@annotation(authCheck)")
    public Object doInterceptor(ProceedingJoinPoint proceedingJoinPoint, AuthCheck authCheck) throws Throwable {
        String mustRole = authCheck.mustRole();
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();
        // 获取当前登录用户信息
        User loginUser = userService.getLoginUser(request);
        // 判断方法上的注解权限
        UserRoleEnum mustRoleEnum = UserRoleEnum.getEnumByValue(mustRole);
        // 无需权限，放行
        if (mustRoleEnum == null) {
            return proceedingJoinPoint.proceed();
        }
        // 需要权限的操作
        UserRoleEnum userRoleEnum = UserRoleEnum.getEnumByValue(loginUser.getUserRole());
        // 未登录
        if (userRoleEnum == null) {
            throw new BusinessException(ErrorCode.NO_LOGIN_AUTH_ERROR);
        }
        // 已登录，判断权限是否一致， 登录用户是否具备管理员权限，不具备则抛出异常
        if (mustRole.equals(UserRoleEnum.ADMIN.getValue()) && !loginUser.getUserRole().equals(UserRoleEnum.ADMIN.getValue())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        return proceedingJoinPoint.proceed();
    }
}
