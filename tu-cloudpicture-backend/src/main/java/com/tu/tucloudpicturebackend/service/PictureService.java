package com.tu.tucloudpicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tu.tucloudpicturebackend.model.dto.picture.PictureQueryRequest;
import com.tu.tucloudpicturebackend.model.dto.picture.PictureReviewRequest;
import com.tu.tucloudpicturebackend.model.dto.picture.PictureUploadByBatchRequest;
import com.tu.tucloudpicturebackend.model.dto.picture.PictureUploadRequest;
import com.tu.tucloudpicturebackend.model.entity.Picture;
import com.tu.tucloudpicturebackend.model.entity.User;
import com.tu.tucloudpicturebackend.model.vo.PictureVO;

import javax.servlet.http.HttpServletRequest;

public interface PictureService extends IService<Picture> {

    void validPicture(Picture picture);

    PictureVO uploadPicture(Object inputSource, PictureUploadRequest pictureUploadRequest, User loginUser);

    /**
     * 转vo包装类（单条）
     * @param picture
     * @param request
     * @return
     */
    PictureVO getPictureVO(Picture picture, HttpServletRequest request);

    /**
     * 转vo包装类（分页）
     * @param page
     * @param request
     * @return
     */
    Page<PictureVO> getPictureVOPage(Page<Picture> page, HttpServletRequest request);

    /**
     * 获取查询条件
     * @param pictureQueryRequest
     * @return
     */
    QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest);

    /**
     * 审核图片
     * @param pictureReviewRequest
     * @param loginUser
     */
    void doPictureReview(PictureReviewRequest pictureReviewRequest, User loginUser);

    /**
     * 设置审核参数
     * @param picture
     * @param loginUser
     */
    void fillReviewParams(Picture picture, User loginUser);

    /**
     * 批量抓取和上传图片
     * @param pictureUploadByBatchRequest
     * @param loginUser
     * @return
     */
    int uploadPictureByBatch(PictureUploadByBatchRequest pictureUploadByBatchRequest, User loginUser);

    /**
     * 空间中图片的权限校验
     * @param loginUser
     * @param picture
     */
    void checkPictureAuth(User loginUser, Picture picture);

    /**
     * 删除图片
     * @param id
     * @param loginUser
     */
    void deletePicture(long id, User loginUser);
}
