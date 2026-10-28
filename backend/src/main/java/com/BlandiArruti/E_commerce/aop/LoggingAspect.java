package com.BlandiArruti.E_commerce.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class LoggingAspect {

    @Pointcut("execution(* com.BlandiArruti.E_commerce.*.service.*Service.*(..))")
    public void serviceMethods() {}

    @Around("serviceMethods()")
    public Object logExecucion(ProceedingJoinPoint joinPoint) throws Throwable {
        String metodo = joinPoint.getSignature().toShortString();
        log.info("[SERVICE] Iniciando: {}", metodo);
        long inicio = System.currentTimeMillis();
        try {
            Object resultado = joinPoint.proceed();
            long duracion = System.currentTimeMillis() - inicio;
            log.info("[SERVICE] Completado: {} — {}ms", metodo, duracion);
            return resultado;
        } catch (Exception ex) {
            log.warn("[SERVICE] Excepción en {}: {}", metodo, ex.getMessage());
            throw ex;
        }
    }
}
