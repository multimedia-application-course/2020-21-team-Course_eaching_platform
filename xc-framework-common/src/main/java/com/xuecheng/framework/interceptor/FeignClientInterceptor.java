package com.xuecheng.framework.interceptor;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.Enumeration;

public class FeignClientInterceptor implements RequestInterceptor {
    @Override
    public void apply(RequestTemplate requestTemplate) {
        ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if(requestAttributes!=null){
            // 取出request
            HttpServletRequest request = requestAttributes.getRequest();
            Enumeration<String> headerNames = request.getHeaderNames();
            if(headerNames!=null){
                while (headerNames.hasMoreElements()){
                    String name = headerNames.nextElement();
                    String values = request.getHeader(name);
                    if(name.equals("authorization")){
                        //System.out.println("name="+name+"values="+values);
                        requestTemplate.header(name, values);
                    }
                }
            }
        }
    }
}
