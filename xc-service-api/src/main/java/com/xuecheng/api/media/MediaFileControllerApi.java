package com.xuecheng.api.media;

import com.xuecheng.framework.domain.course.TeachplanMedia;
import com.xuecheng.framework.domain.media.request.QueryMediaFileRequest;
import com.xuecheng.framework.model.response.QueryResponseResult;
import com.xuecheng.framework.model.response.ResponseResult;

public interface MediaFileControllerApi {
    // 查询文件列表
    public QueryResponseResult findList(int page, int size, QueryMediaFileRequest
            queryMediaFileRequest) ;

}
