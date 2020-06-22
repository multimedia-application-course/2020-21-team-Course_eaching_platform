package com.xuecheng.learning.client;

import com.xuecheng.framework.domain.course.TeachplanMediaPub;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(value = "xc-service-search")
public interface CourseSearchClient {

    // 根据课程计划搜索媒资信息
    @GetMapping(value="/search/course/getmedia/{teachplanId}")
    public TeachplanMediaPub getmedia(@PathVariable("teachplanId") String teachplanId);
}
