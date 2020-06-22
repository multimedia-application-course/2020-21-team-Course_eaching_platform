package com.xuecheng.api.auth;

import com.xuecheng.framework.domain.ucenter.request.LoginRequest;
import com.xuecheng.framework.domain.ucenter.response.JwtResult;
import com.xuecheng.framework.domain.ucenter.response.LoginResult;
import com.xuecheng.framework.model.response.ResponseResult;

// 用户认证接口
public interface AuthControllerApi {

    //登录
    public LoginResult login(LoginRequest loginRequest);
    // 退出
    public ResponseResult logout();
    //客户端查询jwt令牌内容
    public JwtResult userjwt();

}
