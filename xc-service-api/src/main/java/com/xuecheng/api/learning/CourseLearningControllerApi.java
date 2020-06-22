package com.xuecheng.api.learning;

import com.xuecheng.framework.domain.learning.response.GetMediaResult;

public interface CourseLearningControllerApi {
    // 获取课程学习地址
    public GetMediaResult getmedia(String courseId, String teachplanId);

}
