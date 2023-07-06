package com.cesi.datalogscheduler.handler;

import com.cesi.datalogscheduler.util.ResponseWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

@Slf4j
@RestControllerAdvice("com.cesi.datalogscheduler.controller") // 将包括属于这些基本软件包或其子软件包的控制器
public class ResponseBodyHandler implements ResponseBodyAdvice<Object> {
    @Override
    public boolean supports(MethodParameter returnType, Class converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType, Class selectedConverterType, ServerHttpRequest request, ServerHttpResponse response) {
        if (body instanceof ResponseWrapper) {
            return body;
        } else if (body == null) {
            return ResponseWrapper.ok();
        }
        return ResponseWrapper.ok().setData(body);
    }
}
