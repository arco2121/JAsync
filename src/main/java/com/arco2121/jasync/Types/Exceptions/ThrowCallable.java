package com.arco2121.jasync.Types.Exceptions;

@FunctionalInterface
public interface ThrowCallable<V> {
    V call() throws Exception;
}
