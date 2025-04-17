package com.tu.tucloudpicturebackend.manager.websocket;

import cn.hutool.core.util.StrUtil;
import com.tu.tucloudpicturebackend.constant.SpaceUserPermissionConstant;
import com.tu.tucloudpicturebackend.manager.auth.SpaceUserAuthManager;
import com.tu.tucloudpicturebackend.model.entity.Picture;
import com.tu.tucloudpicturebackend.model.entity.Space;
import com.tu.tucloudpicturebackend.model.entity.User;
import com.tu.tucloudpicturebackend.model.enums.SpaceTypeEnum;
import com.tu.tucloudpicturebackend.service.PictureService;
import com.tu.tucloudpicturebackend.service.SpaceService;
import com.tu.tucloudpicturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

/**
 * WebSocket 拦截器，建立连接前先进行校验
 */
@Component
@Slf4j
public class WsHandshakeInterceptor implements HandshakeInterceptor {

    @Resource
    private UserService userService;

    @Resource
    private PictureService pictureService;

    @Resource
    private SpaceService spaceService;

    @Resource
    private SpaceUserAuthManager spaceUserAuthManager;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        if (request instanceof ServletServerHttpRequest) {
            HttpServletRequest servletRequest = ((ServletServerHttpRequest) request).getServletRequest();
            // 获取请求参数
            String pictureId = servletRequest.getParameter("pictureId"); // Todo 不知是否获取到
            if (StrUtil.isBlank(pictureId)) {
                log.error("缺少图片参数，拒绝握手");
                return false;
            }
            User loginUser = userService.getLoginUser(servletRequest);
            if (loginUser == null) {
                log.error("用户未登录，拒绝握手");
                return false;
            }
            Picture picture = pictureService.getById(pictureId);
            if (picture == null) {
                log.error("图片不存在，拒绝握手");
                return false;
            }
            Long spaceId = picture.getSpaceId();
            // 团队空间判断
            Space space = null;
            if (spaceId != null) {
                // 判断空间是否存在
                space = spaceService.getById(spaceId);
                if (space == null) {
                    log.error("空间不存在，拒绝握手");
                    return false;
                }
                // 判断是否为团队空间
                if (space.getSpaceType() != SpaceTypeEnum.TEAM.getValue()) {
                    log.error("不是团队空间，拒绝握手");
                    return false;
                }
            }
            // TODO spaceId为空时为公共空间，目前公共空间用户是不能进行团队操作的，为了预防以后管理员对公共空间图片进行操作 可以先预留口子

            // 判断当前登录用户是否有编辑的权限
            List<String> permissionList = spaceUserAuthManager.getPermissionList(space, loginUser);
            if (!permissionList.contains(SpaceUserPermissionConstant.PICTURE_EDIT)) {
                log.error("没有图片编辑权限，拒绝握手");
                return false;
            }
            // 设置attributes
            attributes.put("user", loginUser);
            attributes.put("userId", loginUser.getId());
            attributes.put("pictureId", Long.valueOf(pictureId));
        }
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception exception) {

    }
}
