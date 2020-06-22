package com.xuecheng.api.course;

import com.xuecheng.framework.domain.course.CoursePic;
import com.xuecheng.framework.domain.course.Teachplan;
import com.xuecheng.framework.domain.course.TeachplanMedia;
import com.xuecheng.framework.domain.course.ext.CourseInfo;
import com.xuecheng.framework.domain.course.ext.CourseView;
import com.xuecheng.framework.domain.course.ext.TeachplanNode;
import com.xuecheng.framework.domain.course.request.CourseListRequest;
import com.xuecheng.framework.domain.course.response.CoursePublishResult;
import com.xuecheng.framework.model.response.QueryResponseResult;
import com.xuecheng.framework.model.response.ResponseResult;
import org.springframework.web.bind.annotation.PathVariable;

public interface CourseControllerApi {
    // 课程计划查询
    public TeachplanNode findTeachplanList(String courseId);
    // 添加课程计划
    public ResponseResult addTeachplan(Teachplan teachplan);
    // 添加课程图片
    public ResponseResult addCoursePic(String courseId,String pic);
    // 查询课程图片
    public CoursePic findCoursePic(String courseId);
    // 删除课程图片
    public ResponseResult deleteCoursePic(String courseId);
    // 课程视图查询
    public CourseView courseview(String id);
    // 课程预览
    public CoursePublishResult preview(String id);
    // 发布课程
    public CoursePublishResult publish(@PathVariable String id);
    // 保存媒资信息
    public ResponseResult savemedia(TeachplanMedia teachplanMedia);
    // 课程查询
    public QueryResponseResult<CourseInfo> findCourseList(int page,
                                                          int size,
                                                          CourseListRequest courseListRequest);
}
