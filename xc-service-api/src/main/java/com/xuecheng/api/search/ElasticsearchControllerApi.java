package com.xuecheng.api.search;

import com.xuecheng.framework.domain.course.CoursePub;
import com.xuecheng.framework.domain.course.TeachplanMediaPub;
import com.xuecheng.framework.domain.search.CourseSearchParam;
import com.xuecheng.framework.model.response.QueryResponseResult;

import java.io.IOException;
import java.util.Map;

public interface ElasticsearchControllerApi {

    // 搜索课程信息
    public QueryResponseResult<CoursePub> list(int page, int size, CourseSearchParam courseSearchParam);
    // 根据课程id查询课程信息
    public Map<String,CoursePub> getall(String id);
    // 根据课程计划搜索媒资信息
    public TeachplanMediaPub getmedia(String teachplanId);

}

