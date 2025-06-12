package com.comprehensive.eureka.admin.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Aspect
@Component
public class ServiceLoggingAspect {

    private static final Logger logger = LoggerFactory.getLogger(ServiceLoggingAspect.class);

    @Around("execution(public * com.comprehensive.eureka.admin.service.*.*(..))")
    public Object logAround(ProceedingJoinPoint pjp) throws Throwable {
        String sig = pjp.getSignature().toShortString();
        String args = Arrays.toString(pjp.getArgs());

        logger.info("▶▶▶ 시작 ▶▶▶ {} 입력값={}", sig, args);

        try {
            Object result = pjp.proceed();
            logger.info("◀◀◀ 종료 ◀◀◀ {} 반환값={}", sig, result);
            return result;
        } catch (Throwable ex) {
            logger.error("✖✖✖ 예외 ✖✖✖ {} 메시지={}", sig, ex.getMessage(), ex);
            throw ex;
        }
    }
}
