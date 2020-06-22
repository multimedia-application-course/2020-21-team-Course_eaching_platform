package com.xuecheng.manage_course.dao;

import com.xuecheng.framework.domain.course.ext.TeachplanNode;
import com.xuecheng.framework.domain.course.ext.TeachplanNode1;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TeachplanMapper1 {
    // 课程计划查询
    public TeachplanNode1 selectList(String courseId);
}
