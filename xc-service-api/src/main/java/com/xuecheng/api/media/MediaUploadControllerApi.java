package com.xuecheng.api.media;

import com.xuecheng.framework.domain.media.response.CheckChunkResult;
import com.xuecheng.framework.model.response.ResponseResult;
import org.springframework.web.multipart.MultipartFile;

public interface MediaUploadControllerApi {

    // 文件上传注册
    public ResponseResult register(String fileMd5,
                                   String fileName,
                                   Long fileSize,
                                   String mimetype,
                                   String fileExt);
    // 分块检查
    public CheckChunkResult checkchunk(String fileMd5,
                                       Integer chunk,
                                       Integer chunkSize);
    // 上传分块
    public ResponseResult uploadchunk(MultipartFile file,
                                      String fileMd5,
                                      Integer chunk);
    // 合并文件
    public ResponseResult mergechunks(String fileMd5,
                                      String fileName,
                                      Long fileSize,
                                      String mimetype,
                                      String fileExt);
}
