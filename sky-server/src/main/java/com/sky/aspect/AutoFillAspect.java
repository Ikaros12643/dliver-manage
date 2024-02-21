package com.sky.aspect;

/**
 * @author Ikaros
 */

import com.sky.context.BaseContext;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;

import java.time.LocalDateTime;

/**
 * 自定义切面，实现公共字段自动填充
 */
//@Aspect
@Slf4j
//@Component
public class AutoFillAspect {

    /**
     * 切入点
     */
//    @Pointcut("execution(* com.sky.mapper.*.*(..)) && @annotation(com.sky.annotation.AutoFill)")
    @Pointcut("execution(* com.sky.mapper.*.update*(..))")
    public void autoFillPt(){}

    /**
     * 前置通知，进行公共字段赋值
     */
    @Before("autoFillPt()")
    public void autoFill(JoinPoint joinPoint){
        log.info("开始进行公共字段自动填充...");
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();//方法签名对象
        String MethodName = signature.getMethod().getName();

        //获取当前被拦截的方法的参数
        Object[] args = joinPoint.getArgs();
        if (args == null || args.length == 0){
            return;
        }

        Object entity = args[0];

        //准备要赋值的数据
        LocalDateTime now = LocalDateTime.now();
        Long currentId = BaseContext.getCurrentId();

        //根据得到的方法名的不同


    }

}
