package com.xuecheng.search.controller;

import com.xuecheng.api.search.ElasticsearchControllerApi;
import com.xuecheng.framework.domain.course.CoursePub;
import com.xuecheng.framework.domain.course.TeachplanMediaPub;
import com.xuecheng.framework.domain.search.CourseSearchParam;
import com.xuecheng.framework.model.response.QueryResponseResult;
import com.xuecheng.framework.model.response.QueryResult;
import com.xuecheng.search.service.EsCourseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/search/course")
public class ElasticsearchController implements ElasticsearchControllerApi {

    @Autowired
    private EsCourseService esCourseService;

    // 搜索课程查询
    @Override
    @GetMapping(value="/list/{page}/{size}")
    public QueryResponseResult<CoursePub> list(@PathVariable("page") int page, @PathVariable("size") int size, CourseSearchParam courseSearchParam) {
        return esCourseService.list(page, size, courseSearchParam);
    }

    // 学习页面查询课程计划
    @Override
    @GetMapping("/getall/{id}")
    public Map<String, CoursePub> getall(@PathVariable("id") String id) {
        return esCourseService.getall(id);
    }

    // 根据课程计划搜索媒资信息
    @Override
    @GetMapping(value="/getmedia/{teachplanId}")
    public TeachplanMediaPub getmedia(@PathVariable("teachplanId") String teachplanId) {
        String[] teachplanIds = {teachplanId};
        QueryResponseResult<TeachplanMediaPub> queryResponseResult = esCourseService.getmedia(teachplanIds);
        QueryResult<TeachplanMediaPub> queryResult = queryResponseResult.getQueryResult();
        if(queryResult != null){
            List<TeachplanMediaPub> list = queryResult.getList();
            if(list != null &&list.size() > 0){
                return list.get(0);
            }
        }
        return new TeachplanMediaPub();
    }
}
