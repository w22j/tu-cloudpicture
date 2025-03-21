package com.tu.tucloudpicturebackend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tu.tucloudpicturebackend.common.ErrorCode;
import com.tu.tucloudpicturebackend.exception.BusinessException;
import com.tu.tucloudpicturebackend.manager.upload.FilePictureUpload;
import com.tu.tucloudpicturebackend.manager.upload.PictureUploadTemplate;
import com.tu.tucloudpicturebackend.manager.upload.UrlPictureUpload;
import com.tu.tucloudpicturebackend.mapper.PictureMapper;
import com.tu.tucloudpicturebackend.model.dto.file.UploadPictureResult;
import com.tu.tucloudpicturebackend.model.dto.picture.PictureQueryRequest;
import com.tu.tucloudpicturebackend.model.dto.picture.PictureReviewRequest;
import com.tu.tucloudpicturebackend.model.dto.picture.PictureUploadRequest;
import com.tu.tucloudpicturebackend.model.entity.Picture;
import com.tu.tucloudpicturebackend.model.entity.User;
import com.tu.tucloudpicturebackend.model.enums.PictureReviewStatusEnum;
import com.tu.tucloudpicturebackend.model.vo.PictureVO;
import com.tu.tucloudpicturebackend.model.vo.UserVO;
import com.tu.tucloudpicturebackend.service.PictureService;
import com.tu.tucloudpicturebackend.service.UserService;
import com.tu.tucloudpicturebackend.utils.ThrowsUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PictureServiceImpl extends ServiceImpl<PictureMapper, Picture>
        implements PictureService {

    @Resource
    private FilePictureUpload filePictureUpload;

    @Resource
    private UrlPictureUpload urlPictureUpload;

    @Resource
    private UserService userService;

    @Override
    public void validPicture(Picture picture) {
        ThrowsUtils.throwIf(picture == null, ErrorCode.PARAMS_ERROR);
        // 从对象中取值
        Long id = picture.getId();
        String url = picture.getUrl();
        String introduction = picture.getIntroduction();
        // 修改数据时，id 不能为空，有参数则校验
        ThrowsUtils.throwIf(ObjUtil.isNull(id), ErrorCode.PARAMS_ERROR, "id 不能为空");
        if (StrUtil.isNotBlank(url)) {
            ThrowsUtils.throwIf(url.length() > 1024, ErrorCode.PARAMS_ERROR, "url 过长");
        }
        if (StrUtil.isNotBlank(introduction)) {
            ThrowsUtils.throwIf(introduction.length() > 800, ErrorCode.PARAMS_ERROR, "简介过长");
        }
    }

    @Override
    public PictureVO uploadPicture(Object inputSource, PictureUploadRequest pictureUploadRequest, User loginUser) {
        // 判断当前是否登录
        ThrowsUtils.throwIf(loginUser == null, ErrorCode.NO_LOGIN_AUTH_ERROR);
        Long pictureId = null;
        // 判断是新增还是更新
        if (pictureUploadRequest != null) {
            pictureId = pictureUploadRequest.getId();
        }
        // 判断是否存在
        if (pictureId != null) {
            Picture picture = getById(pictureId);
            ThrowsUtils.throwIf(picture == null, ErrorCode.OPERATION_ERROR, "图片数据不存在");
            // 仅本人或者管理员才能修改上传的图片
            if (!picture.getUserId().equals(loginUser.getId()) && userService.isAdmin(loginUser)) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "仅有本人或管理员才能修改上传的图片");
            }
        }
        String prefix = String.format("public/%s", loginUser.getId());

        // 根据输入源选择不同的模板方法
        PictureUploadTemplate pictureUploadTemplate = filePictureUpload;
        if (inputSource instanceof String) {
            pictureUploadTemplate = urlPictureUpload;
        }
        UploadPictureResult uploadPictureResult = pictureUploadTemplate.uploadPicture(inputSource, prefix);

        // 拷贝对象属性
        Picture picture = BeanUtil.copyProperties(uploadPictureResult, Picture.class);
        picture.setName(uploadPictureResult.getPicName());
        picture.setUserId(loginUser.getId());
        // 设置审核参数
        fillReviewParams(picture, loginUser);
        // 更新需要修改新的图片id和编辑时间
        if (pictureId != null) {
            picture.setId(pictureId);
            picture.setEditTime(new Date());
        }
        boolean result = saveOrUpdate(picture);
        ThrowsUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "图片上传失败");
        return PictureVO.objToVo(picture);
    }

    @Override
    public PictureVO getPictureVO(Picture picture, HttpServletRequest request) {
        // 对象转封装类
        PictureVO pictureVO = PictureVO.objToVo(picture);
        // 关联查询用户信息
        Long userId = picture.getUserId();
        if (userId != null && userId > 0) {
            User user = userService.getById(userId);
            UserVO userVO = userService.getUserVO(user);
            pictureVO.setUser(userVO);
        }
        return pictureVO;
    }

    @Override
    public Page<PictureVO> getPictureVOPage(Page<Picture> page, HttpServletRequest request) {
        List<Picture> records = page.getRecords();
        Page<PictureVO> pictureVOPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        if (CollUtil.isEmpty(records)) {
            return pictureVOPage;
        }
        // 获取所有用户id
        Set<Long> userIds = records.stream().map(Picture::getUserId).collect(Collectors.toSet());
        // 根据userIds查询所有用户信息
        Map<Long, List<UserVO>> userVOIdMap = userService.listByIds(userIds).stream().map(UserVO::objToVo).collect(Collectors.groupingBy(UserVO::getId));
        List<PictureVO> pictureVOList = records.stream().map(PictureVO::objToVo).collect(Collectors.toList());
        // 塞值
        for (PictureVO pictureVO : pictureVOList) {
            pictureVO.setUser(userVOIdMap.get(pictureVO.getUserId()) != null ? userVOIdMap.get(pictureVO.getUserId()).get(0) : null);
        }
        pictureVOPage.setRecords(pictureVOList);
        return pictureVOPage;
    }


    @Override
    public QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest) {
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        if (pictureQueryRequest == null) {
            return queryWrapper;
        }
        Long id = pictureQueryRequest.getId();
        String name = pictureQueryRequest.getName();
        String introduction = pictureQueryRequest.getIntroduction();
        String category = pictureQueryRequest.getCategory();
        List<String> tags = pictureQueryRequest.getTags();
        Long picSize = pictureQueryRequest.getPicSize();
        Integer picWidth = pictureQueryRequest.getPicWidth();
        Integer picHeight = pictureQueryRequest.getPicHeight();
        Double picScale = pictureQueryRequest.getPicScale();
        String picFormat = pictureQueryRequest.getPicFormat();
        String searchText = pictureQueryRequest.getSearchText();
        Long userId = pictureQueryRequest.getUserId();
        Integer reviewStatus = pictureQueryRequest.getReviewStatus();
        String reviewMessage = pictureQueryRequest.getReviewMessage();
        Long reviewerId = pictureQueryRequest.getReviewerId();
        String sortField = pictureQueryRequest.getSortField();
        String sortOrder = pictureQueryRequest.getSortOrder();
        // 搜索字段
        if (StrUtil.isNotBlank(searchText)) {
            queryWrapper.and(qw -> qw.like("name", searchText).or().like("introduction", searchText));
        }
        queryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjUtil.isNotEmpty(userId), "userId", userId);
        queryWrapper.like(StrUtil.isNotBlank(name), "name", name);
        queryWrapper.like(StrUtil.isNotBlank(introduction), "introduction", introduction);
        queryWrapper.like(StrUtil.isNotBlank(picFormat), "picFormat", picFormat);
        queryWrapper.like(StrUtil.isNotBlank(reviewMessage), "reviewMessage", reviewMessage);
        queryWrapper.eq(StrUtil.isNotBlank(category), "category", category);
        queryWrapper.eq(ObjUtil.isNotEmpty(picWidth), "picWidth", picWidth);
        queryWrapper.eq(ObjUtil.isNotEmpty(picHeight), "picHeight", picHeight);
        queryWrapper.eq(ObjUtil.isNotEmpty(picSize), "picSize", picSize);
        queryWrapper.eq(ObjUtil.isNotEmpty(picScale), "picScale", picScale);
        queryWrapper.eq(ObjUtil.isNotEmpty(reviewStatus), "reviewStatus", reviewStatus);
        queryWrapper.eq(ObjUtil.isNotEmpty(reviewerId), "reviewerId", reviewerId);
        // 标签json数组查询
        if (CollUtil.isNotEmpty(tags)) {
            for (String tag : tags) {
                queryWrapper.like("tags", "\"" + tag + "\"");
            }
        }
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("asc"), sortField);
        return queryWrapper;
    }

    @Override
    public void doPictureReview(PictureReviewRequest pictureReviewRequest, User loginUser) {
        ThrowsUtils.throwIf(pictureReviewRequest == null , ErrorCode.PARAMS_ERROR, "请求参数为空");
        Long pictureId = pictureReviewRequest.getId();
        PictureReviewStatusEnum reviewStatusEnum = PictureReviewStatusEnum.getEnumByValue(pictureReviewRequest.getReviewStatus());
        if (pictureId == null || reviewStatusEnum == null || pictureReviewRequest.getReviewStatus().equals(PictureReviewStatusEnum.REVIEWING.getValue())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 判断图片是否存在
        Picture oldPicture = getById(pictureId);
        ThrowsUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");
        // 判断审核状态是否重复
        if (oldPicture.getReviewStatus().equals(pictureReviewRequest.getReviewStatus())) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "请勿重复审核");
        }
        Picture updatePicture = new Picture();
        BeanUtil.copyProperties(pictureReviewRequest, updatePicture);
        updatePicture.setReviewTime(new Date());
        updatePicture.setReviewerId(loginUser.getId());
        boolean result = updateById(updatePicture);
        ThrowsUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "审核失败");
    }

    @Override
    public void fillReviewParams(Picture picture, User loginUser) {
        // 初始化审核信息 (管理员上传默认自动过审)
        if (userService.isAdmin(loginUser)) {
            picture.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
            picture.setReviewMessage("管理员自动过审");
            picture.setReviewerId(loginUser.getId());
            picture.setReviewTime(new Date());
        } else { // 待审核
            picture.setReviewStatus(PictureReviewStatusEnum.REVIEWING.getValue());
        }
    }
}




