package com.tu.tucloudpicturebackend.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 图片表
 * @TableName picture
 */
@TableName(value ="picture")
@Data
public class Picture implements Serializable {

    /**
     * id
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 图片 url
     */
    @TableField(value = "url")
    private String url;

    /**
     * 缩略图 url
     */
    @TableField(value = "thumbnailUrl")
    private String thumbnailUrl;


    /**
     * 图片名称
     */
    @TableField(value = "name")
    private String name;

    /**
     * 简介
     */
    @TableField(value = "introduction")
    private String introduction;

    /**
     * 分类
     */
    @TableField(value = "category")
    private String category;

    /**
     * 标签（JSON 数组）
     */
    @TableField(value = "tags")
    private String tags;

    /**
     * 图片体积
     */
    @TableField(value = "picSize")
    private Long picSize;

    /**
     * 图片宽度
     */
    @TableField(value = "picWidth")
    private Integer picWidth;

    /**
     * 图片高度
     */
    @TableField(value = "picHeight")
    private Integer picHeight;

    /**
     * 图片宽高比例
     */
    @TableField(value = "picScale")
    private Double picScale;

    /**
     * 图片格式
     */
    @TableField(value = "picFormat")
    private String picFormat;

    /**
     * 图片主色调
     */
    @TableField(value = "picColor")
    private String picColor;

    /**
     * 创建用户 id
     */
    @TableField(value = "userId")
    private Long userId;

    /**
     * 空间id（为空表示公共空间）
     */
    @TableField(value = "spaceId")
    private Long spaceId;

    /**
     * 审核状态：0-待审核; 1-通过; 2-拒绝
     */
    @TableField(value = "reviewStatus")
    private Integer reviewStatus;

    /**
     * 审核信息
     */
    @TableField(value = "reviewMessage")
    private String reviewMessage;

    /**
     * 审核人 ID
     */
    @TableField(value = "reviewerId")
    private Long reviewerId;

    /**
     * 审核时间
     */
    @TableField(value = "reviewTime")
    private Date reviewTime;

    /**
     * 创建时间
     */
    @TableField(value = "createTime")
    private Date createTime;

    /**
     * 编辑时间
     */
    @TableField(value = "editTime")
    private Date editTime;

    /**
     * 更新时间
     */
    @TableField(value = "updateTime")
    private Date updateTime;

    /**
     * 是否删除
     */
    @TableLogic
    private Integer isDelete;
}