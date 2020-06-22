package com.xuecheng.learning.service;

import com.xuecheng.framework.domain.course.TeachplanMediaPub;
import com.xuecheng.framework.domain.learning.XcLearningCourse;
import com.xuecheng.framework.domain.learning.response.GetMediaResult;
import com.xuecheng.framework.domain.learning.response.LearningCode;
import com.xuecheng.framework.domain.task.XcTask;
import com.xuecheng.framework.domain.task.XcTaskHis;
import com.xuecheng.framework.exception.ExceptionCast;
import com.xuecheng.framework.model.response.CommonCode;
import com.xuecheng.framework.model.response.ResponseResult;
import com.xuecheng.learning.client.CourseSearchClient;
import com.xuecheng.learning.dao.XcLearningCourseRepository;
import com.xuecheng.learning.dao.XcTaskHisRepository;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.Optional;

@Service
public class LearningService {

    @Autowired
    private CourseSearchClient courseSearchClient;
    @Autowired
    private XcLearningCourseRepository xcLearningCourseRepository;
    @Autowired
    private XcTaskHisRepository xcTaskHisRepository;

    // 获取课程学习地址
    public GetMediaResult getMedia(String courseId, String teachplanId) {
        TeachplanMediaPub teachplanMediaPub = courseSearchClient.getmedia(teachplanId);
        if(teachplanMediaPub == null || StringUtils.isEmpty(teachplanMediaPub.getMediaUrl())){
            //获取视频播放地址出错
            ExceptionCast.cast(LearningCode.LEARNING_GETMEDIA_ERROR);
        }
        return new GetMediaResult(CommonCode.SUCCESS,teachplanMediaPub.getMediaUrl());
    }

    // 添加选课
    @Transactional
    public ResponseResult addcourse(String userId, String courseId, String valid, Date
            startTime, Date endTime, XcTask xcTask){
        if (org.apache.commons.lang3.StringUtils.isEmpty(courseId)) {
            ExceptionCast.cast(LearningCode.LEARNING_GETMEDIA_ERROR);
        }
        if (org.apache.commons.lang3.StringUtils.isEmpty(userId)) {
            ExceptionCast.cast(LearningCode.CHOOSECOURSE_USERISNULL);
        }
        if(xcTask == null || org.apache.commons.lang3.StringUtils.isEmpty(xcTask.getId())){
            ExceptionCast.cast(LearningCode.CHOOSECOURSE_TASKISNULL);
        }

        XcLearningCourse xcLearningCourse = xcLearningCourseRepository.findByUserIdAndCourseId(userId, courseId);
        if (xcLearningCourse == null) {//没有选课记录则添加
            xcLearningCourse = new XcLearningCourse();
            xcLearningCourse.setUserId(userId);
            xcLearningCourse.setCourseId(courseId);
            xcLearningCourse.setValid(valid);
            xcLearningCourse.setStartTime(startTime);
            xcLearningCourse.setEndTime(endTime);
            xcLearningCourse.setStatus("501001");
            xcLearningCourseRepository.save(xcLearningCourse);
        } else {//有选课记录则更新日期
            xcLearningCourse.setValid(valid);
            xcLearningCourse.setStartTime(startTime);
            xcLearningCourse.setEndTime(endTime);
            xcLearningCourse.setStatus("501001");
            xcLearningCourseRepository.save(xcLearningCourse);
        }
        // 向历史任务表添加记录
        Optional<XcTaskHis> optional = xcTaskHisRepository.findById(xcTask.getId());
        if(!optional.isPresent()){
            // 添加历史任务
            XcTaskHis xcTaskHis = new XcTaskHis();
            BeanUtils.copyProperties(xcTask,xcTaskHis);
            xcTaskHisRepository.save(xcTaskHis);
        }
        return new ResponseResult(CommonCode.SUCCESS);
    }
}
