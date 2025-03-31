package com.tu.tucloudpicturebackend.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tu.tucloudpicturebackend.common.ErrorCode;
import com.tu.tucloudpicturebackend.exception.BusinessException;
import com.tu.tucloudpicturebackend.mapper.SpaceMapper;
import com.tu.tucloudpicturebackend.model.dto.space.SpaceAddRequest;
import com.tu.tucloudpicturebackend.model.dto.space.SpaceQueryRequest;
import com.tu.tucloudpicturebackend.model.entity.Picture;
import com.tu.tucloudpicturebackend.model.entity.Space;
import com.tu.tucloudpicturebackend.model.entity.User;
import com.tu.tucloudpicturebackend.model.enums.SpaceLevelEnum;
import com.tu.tucloudpicturebackend.model.vo.SpaceVO;
import com.tu.tucloudpicturebackend.model.vo.UserVO;
import com.tu.tucloudpicturebackend.service.PictureService;
import com.tu.tucloudpicturebackend.service.SpaceService;
import com.tu.tucloudpicturebackend.service.UserService;
import com.tu.tucloudpicturebackend.utils.ThrowsUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;


@Service
public class SpaceServiceImpl extends ServiceImpl<SpaceMapper, Space>
        implements SpaceService {

    @Resource
    private UserService userService;

    @Resource
    private TransactionTemplate transactionTemplate;

    @Resource
    private PictureService pictureService;

    private static final Map<Long, Object> lockMap = new ConcurrentHashMap<Long, Object>();

    @Override
    public long addSpace(SpaceAddRequest spaceAddRequest, User loginUser) {
        Space space = new Space();
        BeanUtils.copyProperties(spaceAddRequest, space);
        if (StrUtil.isBlank(space.getSpaceName())) {
            space.setSpaceName("默认空间");
        }
        if (space.getSpaceLevel() == null) {
            space.setSpaceLevel(SpaceLevelEnum.COMMON.getValue());
        }
        // 校验新增时参数
        this.validSpace(space, true);
        // 填充空间级别相关参数
        this.fillSpaceBySpaceLevel(space);
        Long userId = loginUser.getId();
        space.setUserId(userId);
        // 非管理员只能创建普通级别的空间
        if (!userService.isAdmin(loginUser) && !space.getSpaceLevel().equals(SpaceLevelEnum.COMMON.getValue())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限创建指定级别空间");
        }
        // 保证一个用户只创建一个空间（锁 + 事务）
        // 根据用户id设本地锁 （锁用完最好释放）
        Object lock = lockMap.computeIfAbsent(userId, v -> new Object());
        synchronized (lock) {
            Long newUserId = null;
            try {
                newUserId = transactionTemplate.execute(status -> {
                    boolean exists = this.lambdaQuery()
                            .eq(Space::getUserId, userId)
                            .exists();
                    ThrowsUtils.throwIf(exists, ErrorCode.OPERATION_ERROR, "每个用户只能创建一个空间");
                    boolean result = this.save(space);
                    ThrowsUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "新增空间操作失败");
                    return space.getId();
                });
            } finally { // 释放锁，防止占用资源
                lockMap.remove(userId);
            }
            return Optional.ofNullable(newUserId).orElse(-1L);
        }
    }

    @Override
    public void validSpace(Space space, boolean add) {
        ThrowsUtils.throwIf(space == null, ErrorCode.PARAMS_ERROR);
        // 从对象中取值
        String spaceName = space.getSpaceName();
        Integer spaceLevel = space.getSpaceLevel();
        SpaceLevelEnum spaceLevelEnum = SpaceLevelEnum.getEnumByValue(spaceLevel);
        // 创建时校验
        if (add) {
            if (StrUtil.isBlank(spaceName)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间名称不能为空");
            }
            if (spaceLevel == null) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间级别不能为空");
            }
        }
        // 修改时 空间名称进行校验
        if (StrUtil.isNotBlank(spaceName) && spaceName.length() > 30) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间名称过长");
        }
        // 修改时 空间级别进行校验
        if (spaceLevel != null && spaceLevelEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间级别不存在");
        }
    }

    @Override
    public SpaceVO getSpaceVO(Space space, HttpServletRequest request) {
        // 对象转封装类
        SpaceVO spaceVO = SpaceVO.objToVo(space);
        // 关联查询用户信息
        Long userId = space.getUserId();
        if (userId != null && userId > 0) {
            User user = userService.getById(userId);
            UserVO userVO = userService.getUserVO(user);
            spaceVO.setUser(userVO);
        }
        return spaceVO;
    }

    @Override
    public Page<SpaceVO> getSpaceVOPage(Page<Space> page, HttpServletRequest request) {
        List<Space> records = page.getRecords();
        Page<SpaceVO> spaceVOPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        if (CollUtil.isEmpty(records)) {
            return spaceVOPage;
        }
        // 获取所有用户id
        Set<Long> userIds = records.stream().map(Space::getUserId).collect(Collectors.toSet());
        // 根据userIds查询所有用户信息
        Map<Long, List<UserVO>> userVOIdMap = userService.listByIds(userIds).stream().map(UserVO::objToVo).collect(Collectors.groupingBy(UserVO::getId));
        List<SpaceVO> pictureVOList = records.stream().map(SpaceVO::objToVo).collect(Collectors.toList());
        // 塞值
        for (SpaceVO spaceVO : pictureVOList) {
            spaceVO.setUser(userVOIdMap.get(spaceVO.getUserId()) != null ? userVOIdMap.get(spaceVO.getUserId()).get(0) : null);
        }
        spaceVOPage.setRecords(pictureVOList);
        return spaceVOPage;
    }

    @Override
    public QueryWrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest) {
        QueryWrapper<Space> queryWrapper = new QueryWrapper<>();
        if (spaceQueryRequest == null) {
            return queryWrapper;
        }
        // 从对象中取值
        Long id = spaceQueryRequest.getId();
        Long userId = spaceQueryRequest.getUserId();
        String spaceName = spaceQueryRequest.getSpaceName();
        Integer spaceLevel = spaceQueryRequest.getSpaceLevel();
        String sortField = spaceQueryRequest.getSortField();
        String sortOrder = spaceQueryRequest.getSortOrder();
        // 拼接查询条件
        queryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjUtil.isNotEmpty(userId), "userId", userId);
        queryWrapper.like(StrUtil.isNotBlank(spaceName), "spaceName", spaceName);
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceLevel), "spaceLevel", spaceLevel);
        // 排序
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);
        return queryWrapper;

    }

    @Override
    public void fillSpaceBySpaceLevel(Space space) {
        // 根据空间级别自动填充限额
        SpaceLevelEnum spaceLevelEnum = SpaceLevelEnum.getEnumByValue(space.getSpaceLevel());
        if (spaceLevelEnum != null) {
            if (space.getMaxSize() == null) {
                space.setMaxSize(spaceLevelEnum.getMaxSize());
            }
            if (space.getMaxCount() == null) {
                space.setMaxCount(spaceLevelEnum.getMaxCount());
            }
        }
    }

    @Override
    public void checkoutAuth(User loginUser, Space space) {
        // 仅本人或管理员可编辑
        if (!space.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
    }

    @Override
    public void deleteSpace(long id, User loginUser) {
        // 判断是否存在
        Space oldSpace = this.getById(id);
        ThrowsUtils.throwIf(oldSpace == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可删除
        this.checkoutAuth(loginUser, oldSpace);
        transactionTemplate.execute(status -> {
            // 操作数据库
            boolean result = this.removeById(id);
            // 同时删除空间中所有的图片
            List<Picture> pictureList = pictureService.lambdaQuery()
                    .eq(Picture::getSpaceId, id)
                    .list();
            if (CollUtil.isNotEmpty(pictureList)) {
                List<Long> pictureIds = pictureList.stream().map(Picture::getId).collect(Collectors.toList());
                // 删除该空间内相关数据
                boolean removeByIds = pictureService.removeByIds(pictureIds);
                ThrowsUtils.throwIf(!removeByIds, ErrorCode.OPERATION_ERROR);
            }
            ThrowsUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
            return true;
        });
    }
}




