package com.arco2121.jasync.Types.Async;

import com.arco2121.jasync.JAsync.Async;
import com.arco2121.jasync.JAsync.Running.Asyncable;
import com.arco2121.jasync.Types.Annotations.JAsyncable;
import com.arco2121.jasync.Types.Annotations.JAwaitable;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

import java.util.concurrent.Callable;

@Aspect
public class AsyncWrap {

    @Around("@annotation(asyncConfig)")
    public Asyncable<Object> wrapAsync(ProceedingJoinPoint joinPoint, JAsyncable asyncConfig) throws Throwable {

        long time = asyncConfig.delay();

        return Async.async(() -> {
            try {
                if(time > 0) Async.timeout(time);
                return joinPoint.proceed();
            } catch (Throwable e) {
                throw new RuntimeException("Async Failure", e);
            }
        });
    }

    @Around("@annotation(awaitConfig)")
    public Object wrapAwait(ProceedingJoinPoint joinPoint, JAwaitable awaitConfig) throws Throwable {

        long time = awaitConfig.timeout();

        Callable<Object> task = () -> {
            try {
                return joinPoint.proceed();
            } catch (Throwable e) {
                throw new Exception(e);
            }
        };

        return time > 0 ? Async.await(task, time) : Async.await(task);
    }
}