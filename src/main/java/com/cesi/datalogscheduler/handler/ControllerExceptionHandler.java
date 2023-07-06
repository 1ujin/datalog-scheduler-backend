package com.cesi.datalogscheduler.handler;

import com.cesi.datalogscheduler.util.ResponseWrapper;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.internal.engine.path.PathImpl;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.util.Set;

@Slf4j
@RestControllerAdvice("com.cesi.datalogscheduler.controller")
public class ControllerExceptionHandler {

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(value = ConstraintViolationException.class)
    public ResponseWrapper handle(ConstraintViolationException e) {
        StringBuilder msg = new StringBuilder();
        Set<ConstraintViolation<?>> constraintViolations = e.getConstraintViolations();
        for (ConstraintViolation<?> constraintViolation : constraintViolations) {
            PathImpl pathImpl = (PathImpl) constraintViolation.getPropertyPath();
            String paramName = pathImpl.getLeafNode().getName();
            String message = constraintViolation.getMessage();
            msg.append(paramName).append("[").append(message).append("]");
        }
        log.error("验证约束失败: " + msg, e);
        return ResponseWrapper.error().setMsg(msg.toString());
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(value = Exception.class)
    public ResponseWrapper handle(Exception e) {
        log.error("未知异常: " + e.getMessage(), e);
        return ResponseWrapper.error();
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    public ResponseWrapper handle(MethodArgumentNotValidException e) {
        BindingResult bindingResult = e.getBindingResult();
        log.error("参数验证失败: " + e.getMessage(), e);
        if (bindingResult.hasErrors()) {
            FieldError fieldError = bindingResult.getFieldError();
            if (fieldError != null) {
                String defaultMessage = fieldError.getDefaultMessage();
                return ResponseWrapper.invalid().setMsg(defaultMessage);
            } else {
                return ResponseWrapper.invalid();
            }
        } else {
            log.error("获取参数错误详情失败");
            return ResponseWrapper.error();
        }
    }
}
