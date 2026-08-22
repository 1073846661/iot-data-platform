package com.iot.platform.aspect;

import com.iot.platform.annotation.Log;
import com.iot.platform.entity.OperationLog;
import com.iot.platform.mapper.OperationLogMapper;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Aspect
@Component
public class LogAspect {
    @Autowired
    private OperationLogMapper operationLogMapper;

    @Around("@annotation(log)")
    public Object logMethod(ProceedingJoinPoint joinPoint, Log log) throws Throwable{
        /*
        1.记录开始时间 System.currentTimeMillis()
        2.joinPoint.proceed() 执行原方法，拿到返回值
        3.算耗时 = 结束时间 - 开始时间
        4.把方法名、参数、返回值、耗时组装成 OperationLog 对象，调用 mapper 插入数据库*/
        long start = System.currentTimeMillis();
        Object result = joinPoint.proceed();
        long time = System.currentTimeMillis() - start;

        OperationLog operationLog = new OperationLog();
        operationLog.setMethodName(joinPoint.getSignature().getName());
        operationLog.setParams(Arrays.toString(joinPoint.getArgs()));
        operationLog.setReturnValue(String.valueOf(result));
        operationLog.setExecutionTime(time);
        operationLogMapper.insert(operationLog);
        return result;
    }

}
