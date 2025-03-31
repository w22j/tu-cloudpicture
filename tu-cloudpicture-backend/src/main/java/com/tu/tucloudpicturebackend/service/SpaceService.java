package com.tu.tucloudpicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tu.tucloudpicturebackend.model.dto.space.SpaceAddRequest;
import com.tu.tucloudpicturebackend.model.dto.space.SpaceQueryRequest;
import com.tu.tucloudpicturebackend.model.entity.Space;
import com.tu.tucloudpicturebackend.model.entity.User;
import com.tu.tucloudpicturebackend.model.vo.SpaceVO;

import javax.servlet.http.HttpServletRequest;


public interface SpaceService extends IService<Space> {

    /**
     * 创建空间
     *
     * @param spaceAddRequest
     * @param loginUser
     * @return
     */
    long addSpace(SpaceAddRequest spaceAddRequest, User loginUser);

    /**
     * 校验
     * @param space
     * @param add 是否为创建时校验
     */
    void validSpace(Space space, boolean add);
    
    /**
     * 转vo包装类（单条）
     * @param space
     * @param request
     * @return
     */
    SpaceVO getSpaceVO(Space space, HttpServletRequest request);

    /**
     * 转vo包装类（分页）
     * @param page
     * @param request
     * @return
     */
    Page<SpaceVO> getSpaceVOPage(Page<Space> page, HttpServletRequest request);

    /**
     * 获取查询条件
     * @param spaceQueryRequest
     * @return
     */
    QueryWrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest);

    /**
     * 根据空间级别自动填充空间参数
     * @param space
     */
    void fillSpaceBySpaceLevel(Space space);

    /**
     * 校验空间权限
     * @param loginUser
     * @param oldSpace
     */
    void checkoutAuth(User loginUser, Space oldSpace);

    /**
     * 删除空间
     * @param id 空间id
     * @param loginUser
     */
    void deleteSpace(long id, User loginUser);
}
