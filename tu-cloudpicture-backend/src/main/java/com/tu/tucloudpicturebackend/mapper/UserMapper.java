package com.tu.tucloudpicturebackend.mapper;

import com.tu.tucloudpicturebackend.model.entity.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.mapstruct.Mapper;

/**
* @author W2J
* @description 针对表【user(用户表)】的数据库操作Mapper
* @createDate 2025-03-06 15:20:44
* @Entity com.tu.tucloudpicturebackend.model.entity.User
*/
@Mapper
public interface UserMapper extends BaseMapper<User> {

}




