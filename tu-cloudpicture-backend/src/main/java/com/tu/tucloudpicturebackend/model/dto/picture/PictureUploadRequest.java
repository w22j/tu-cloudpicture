package com.tu.tucloudpicturebackend.model.dto.picture;

import lombok.Data;

import java.io.Serializable;

@Data
public class PictureUploadRequest implements Serializable {

    private static final long serialVersionUID = 3954073758535272340L;

    /**
     * 图片id 用于修改图片
     */
    private Long id;

    /**
     * 文件地址
     */
    private String fileUrl;

}
