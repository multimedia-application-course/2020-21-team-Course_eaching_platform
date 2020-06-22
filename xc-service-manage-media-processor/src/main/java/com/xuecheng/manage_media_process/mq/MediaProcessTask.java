package com.xuecheng.manage_media_process.mq;

import com.alibaba.fastjson.JSON;
import com.xuecheng.framework.domain.media.MediaFile;
import com.xuecheng.framework.domain.media.MediaFileProcess_m3u8;
import com.xuecheng.framework.utils.HlsVideoUtil;
import com.xuecheng.framework.utils.Mp4VideoUtil;
import com.xuecheng.manage_media_process.dao.MediaFileRepository;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class MediaProcessTask {

    //ffmpeg绝对路径
    @Value("${xc-service-manage-media.ffmpeg-path}")
    String ffmpeg_path;
    //上传文件根目录
    @Value("${xc-service-manage-media.video-location}")
    String serverPath;

    @Autowired
    private MediaFileRepository mediaFileRepository;

    @RabbitListener(queues ="${xc-service-manage-media.mq.queue-media-video-processor}", containerFactory = "customContainerFactory")
    public void receiveMediaProcessTasking(String msg){
        // 解析消息内容
        Map msgMap = JSON.parseObject(msg, Map.class);
        String mediaId = (String) msgMap.get("mediaId");
        // 那mediaId从数据库查询文件信息
        Optional<MediaFile> optional = mediaFileRepository.findById(mediaId);
        if(!optional.isPresent()){
            return;
        }
        MediaFile mediaFile = optional.get();

        //媒资文件类型
        String fileType = mediaFile.getFileType();
        if(fileType == null || !fileType.equals("avi")){//目前只处理avi文件
            mediaFile.setProcessStatus("303004");//处理状态为无需处理
            mediaFileRepository.save(mediaFile);
            return ;
        }else{
            mediaFile.setProcessStatus("303001");//处理状态为未处理
            mediaFileRepository.save(mediaFile);
        }

        // 使用工具类讲avi文件生成MP4
        String video_path = serverPath + mediaFile.getFilePath() + mediaFile.getFileName();
        String mp4_name = mediaFile.getFileId() + ".mp4";
        String mp4folder_path = serverPath + mediaFile.getFilePath();
        Mp4VideoUtil mp4VideoUtil = new Mp4VideoUtil(ffmpeg_path, video_path, mp4_name, mp4folder_path);
        String result = mp4VideoUtil.generateMp4();
        if(result == null || !result.equals("success")){
            // 处理失败
            mediaFile.setProcessStatus("303003");//处理状态为处理失败
            MediaFileProcess_m3u8 mediaFileProcess_m3u8 = new MediaFileProcess_m3u8();
            mediaFileProcess_m3u8.setErrormsg(result);
            mediaFile.setMediaFileProcess_m3u8(mediaFileProcess_m3u8);
            mediaFileRepository.save(mediaFile);
            return ;
        }

        // 将MP4生成m3u8和ts文件
        video_path = serverPath + mediaFile.getFilePath()+mp4_name;//此地址为mp4的地址
        String m3u8_name = mediaFile.getFileId()+".m3u8";
        String m3u8folder_path = serverPath + mediaFile.getFilePath()+"hls/";
        HlsVideoUtil hlsVideoUtil = new HlsVideoUtil(ffmpeg_path, video_path, m3u8_name, m3u8folder_path);
        result = hlsVideoUtil.generateM3u8();
        if(result == null || !result.equals("success")){
            //操作失败写入处理日志
            mediaFile.setProcessStatus("303003");//处理状态为处理失败
            MediaFileProcess_m3u8 mediaFileProcess_m3u8 = new MediaFileProcess_m3u8();
            mediaFileProcess_m3u8.setErrormsg(result);
            mediaFile.setMediaFileProcess_m3u8(mediaFileProcess_m3u8);
            mediaFileRepository.save(mediaFile);
            return ;
        }
        // 处理成功
        // 获取m3u8列表
        List<String> ts_list = hlsVideoUtil.get_ts_list();
        //更新处理状态为成功
        mediaFile.setProcessStatus("303002");//处理状态为处理成功
        MediaFileProcess_m3u8 mediaFileProcess_m3u8 = new MediaFileProcess_m3u8();
        mediaFileProcess_m3u8.setTslist(ts_list);
        mediaFile.setMediaFileProcess_m3u8(mediaFileProcess_m3u8);
        // m3u8文件url
        mediaFile.setFileUrl(mediaFile.getFilePath() +"hls/"+m3u8_name );
        mediaFileRepository.save(mediaFile);

    }
}
