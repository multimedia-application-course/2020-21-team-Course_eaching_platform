package com.xuecheng.framework.exception;

import com.google.common.collect.ImmutableMap;
import com.xuecheng.framework.model.response.CommonCode;
import com.xuecheng.framework.model.response.ResponseResult;
import com.xuecheng.framework.model.response.ResultCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

@ControllerAdvice
public class ExceptionCatch {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExceptionCatch.class);
    // 使用EXCEPTIONS存放异常类型和错误代码的映射，ImmutableMap的特点的一旦创建不可改变，并且线程安全
    private static ImmutableMap<Class<? extends Throwable>, ResultCode> EXCEPTIONS;
    // 使用builder来构建一个异常类型和错误代码的异常
    protected static ImmutableMap.Builder<Class<? extends Throwable>,ResultCode> builder =
            ImmutableMap.builder();

    //捕获 CustomException异常
    @ExceptionHandler(CustomException.class)
    @ResponseBody
    public ResponseResult customException(CustomException e) {
        LOGGER.error("catch exception : {}/r/n exception: ",e.getMessage(), e);
        ResultCode resultCode = e.getResultCode();
        return new ResponseResult(resultCode);
    }

    // 捕获不可预知异常
    @ExceptionHandler(Exception.class)
    @ResponseBody
    public ResponseResult exception(Exception exception){
        //记录日志
        LOGGER.error("catch exception:{}",exception.getMessage());

        if(EXCEPTIONS == null){
            EXCEPTIONS = builder.build();    // 构建EXCEPTIONS
        }
        // 从EXCEPTIONS中找异常类型所对应的错误代码
        ResultCode resultCode = EXCEPTIONS.get(exception.getClass());
        if(resultCode != null){
            return new ResponseResult((resultCode));
        }else{
            return new ResponseResult(CommonCode.SERVER_ERROR);
        }
    }

    static {
        builder.put(HttpMessageNotReadableException.class, CommonCode.INVALID_PARAM);
    }

}
