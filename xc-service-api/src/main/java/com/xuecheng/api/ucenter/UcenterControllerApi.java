package com.xuecheng.api.ucenter;

import com.xuecheng.framework.domain.ucenter.ext.XcUserExt;

// 用户管理中心
public interface UcenterControllerApi {

    // 根据用户账号查询用户信息
    public XcUserExt getUserext(String username);

}
