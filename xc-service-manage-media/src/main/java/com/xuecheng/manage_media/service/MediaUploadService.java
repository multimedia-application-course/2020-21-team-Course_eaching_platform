package com.xuecheng.manage_media.service;

import com.alibaba.fastjson.JSON;
import com.xuecheng.framework.domain.media.MediaFile;
import com.xuecheng.framework.domain.media.response.CheckChunkResult;
import com.xuecheng.framework.domain.media.response.MediaCode;
import com.xuecheng.framework.exception.ExceptionCast;
import com.xuecheng.framework.model.response.CommonCode;
import com.xuecheng.framework.model.response.ResponseResult;
import com.xuecheng.manage_media.config.RabbitMQConfig;
import com.xuecheng.manage_media.dao.MediaFileRepository;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.util.*;

@Service
public class MediaUploadService {

    @Autowired
    private MediaFileRepository mediaFileRepository;

    // 上传文件根目录
    @Value("${xc-service-manage-media.upload-location}")
    String uploadPath;
    // mq RoutingKey
    @Value("${xc-service-manage-media.mq.routingkey-media-video}")
    String routingkey_media_video;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    // 上传文件注册
    public ResponseResult register(String fileMd5, String fileName, Long fileSize, String mimetype, String fileExt) {
        // 检测文件是否上传
        // 文件路径
        String fileFolderPath = getFileFolderPath(fileMd5);

        // 1、得到文件路径
        String filePath = getFilePath(fileMd5, fileExt);
        File file = new File(filePath);

        // 2、查询数据库是否存在
        Optional<MediaFile> optional = mediaFileRepository.findById(fileMd5);
        // 文件存在直接返回
        if(file.exists() && optional.isPresent()){
            ExceptionCast.cast(MediaCode.UPLOAD_FILE_REGISTER_EXIST);
        }
        // 文件不存在
        File fileFolder = new File(fileFolderPath);
        if(!fileFolder.exists()){
            fileFolder.mkdirs();
        }

        return new ResponseResult(CommonCode.SUCCESS);
    }
    //得到文件所在目录
    private String getFileFolderPath(String fileMd5){
        return uploadPath + fileMd5.substring(0,1) + "/" + fileMd5.substring(1,2) +"/" + fileMd5 + "/";
    }
    // 得到文件路径
    private String getFilePath(String fileMd5, String fileExt){
        return uploadPath + fileMd5.substring(0,1) + "/" + fileMd5.substring(1,2) +"/" + fileMd5 + "/" + fileMd5 + "." + fileExt;
    }


    // 检测分块文件
    public CheckChunkResult checkchunk(String fileMd5, Integer chunk, Integer chunkSize) {
        // 得到块文件路径
        String chunkFileFolderPath = getChunkFileFolderPath(fileMd5);
        File chunkFile = new File((chunkFileFolderPath + chunk));
        if(chunkFile.exists()){
            // 块文件存在
            return new CheckChunkResult(MediaCode.CHUNK_FILE_EXIST_CHECK,true);
        }else{
            return new CheckChunkResult(MediaCode.CHUNK_FILE_EXIST_CHECK,false);
        }
    }

    // 得到块文件目录
    private String getChunkFileFolderPath(String fileMd5){
        return uploadPath + fileMd5.substring(0,1) + "/" + fileMd5.substring(1,2) +"/" + fileMd5 + "/chunks/";
    }


    // 上传分块
    public ResponseResult uploadchunk(MultipartFile file, String fileMd5, Integer chunk) {
        // 检测分块目录，不存在则创建
        String chunkFileFolderPath = getChunkFileFolderPath(fileMd5);
        // 得到分块文件路径
        String chunkFilePath = chunkFileFolderPath + chunk;
        File chunkFileFolder = new File(chunkFileFolderPath);
        if(!chunkFileFolder.exists()){
            chunkFileFolder.mkdirs();
        }

        // 上传块文件
        InputStream inputStream = null;
        FileOutputStream outputStream = null;

        try {
            inputStream = file.getInputStream();
            outputStream = new FileOutputStream(new File(chunkFilePath));
            IOUtils.copy(inputStream, outputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            try {
                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                outputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        return new ResponseResult(CommonCode.SUCCESS);

    }


    // 合并文件
    public ResponseResult mergechunks(String fileMd5, String fileName, Long fileSize, String mimetype, String fileExt) {
        // 1、合并所有分块
        String chunkFileFolderPath = getChunkFileFolderPath(fileMd5);
        File chunkFileFolder = new File(chunkFileFolderPath);
        // 分块文件列表
        File[] files = chunkFileFolder.listFiles();
        List<File> fileList = Arrays.asList(files);
        // 创建合并文件
        String filePath = getFilePath(fileMd5, fileExt);
        File mergeFile = new File(filePath);
        // 执行合并
        mergeFile = mergeFile(mergeFile, fileList);
        if(mergeFile == null){
            ExceptionCast.cast(MediaCode.MERGE_FILE_FAIL);
        }
        // 2、检验文件Md5是否与前端传入Md5一样
        boolean checkFileMd5 = checkFileMd5(mergeFile, fileMd5);
        if(!checkFileMd5){
            // 校验文件失败
            ExceptionCast.cast(MediaCode.MERGE_FILE_CHECKFAIL);
        }
        // 3、文件信息写入mongodb
        MediaFile mediaFile = new MediaFile();
        mediaFile.setFileId(fileMd5);
        mediaFile.setFileName(fileMd5+"."+fileExt);
        mediaFile.setFileOriginalName(fileName);
        //文件路径保存相对路径
        String FileFolderRelativePath = fileMd5.substring(0,1) + "/" + fileMd5.substring(1,2) +"/" + fileMd5 + "/";
        mediaFile.setFilePath(FileFolderRelativePath);
        mediaFile.setFileSize(fileSize);
        mediaFile.setUploadTime(new Date());
        mediaFile.setMimeType(mimetype);
        mediaFile.setFileType(fileExt);
        //状态为上传成功
        mediaFile.setFileStatus("301002");
        mediaFileRepository.save(mediaFile);

        // 向mq发送视频处理消息
        sendProcessVideoMsg(mediaFile.getFileId());
        return new ResponseResult(CommonCode.SUCCESS);
    }

    // 合并文件
    private File mergeFile(File mergeFile, List<File> chunkFileList){
        try {
            // 合并文件存在则删除
            if(mergeFile.exists()){
                mergeFile.delete();
            }else{
                // 创建新文件
                mergeFile.createNewFile();
            }
            // 对块文件进行排序
            Collections.sort(chunkFileList, new Comparator<File>() {
                @Override
                public int compare(File o1, File o2) {
                    if(Integer.parseInt(o1.getName()) >Integer.parseInt(o2.getName())){
                        return 1;
                    }
                    return -1;
                }
            });
            // 创建写对象
            RandomAccessFile raf_write = new RandomAccessFile(mergeFile,"rw");
            byte[] b = new byte[1024];
            for (File chunkFile : chunkFileList) {
                RandomAccessFile raf_read = new RandomAccessFile(chunkFile,"r");
                int len = -1;
                while ((len = raf_read.read(b))!=-1){
                    raf_write.write(b,0,len);
                }
                raf_read.close();
            }
            raf_write.close();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return mergeFile;
    }

    // 校验文件
    private boolean checkFileMd5(File mergeFile, String md5){
        try {
            // 创建文件输入流
            FileInputStream inputStream = new FileInputStream(mergeFile);
            // 得到文件的Md5
            String md5Hex = DigestUtils.md5Hex(inputStream);

            if(md5.equalsIgnoreCase(md5Hex)){
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return false;
    }

    // 向mq发送消息
    public ResponseResult sendProcessVideoMsg(String mediaId){
        Optional<MediaFile> optional = mediaFileRepository.findById(mediaId);
        if(!optional.isPresent()){
            return new ResponseResult(CommonCode.FAIL);
        }
        // 构建消息内容
        HashMap<String, String> msgMap = new HashMap<>();
        msgMap.put("mediaId", mediaId);
        String msg = JSON.toJSONString(msgMap);
        // 向mq 发送视频处理消息
        try {
            rabbitTemplate.convertAndSend(RabbitMQConfig.EX_MEDIA_PROCESSTASK, routingkey_media_video, msg);
        } catch (AmqpException e) {
            e.printStackTrace();
            return new ResponseResult(CommonCode.FAIL);
        }

        return new ResponseResult(CommonCode.SUCCESS);
    }

}
