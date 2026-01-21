package com.arco2121.jasync.Types.Interfaces;

import com.arco2121.jasync.JAsync.Running.Asyncable;

import java.util.List;
import java.util.function.Function;

public interface AsyncCollection {
    
    Asyncable<?> awaitToArray(Function<Integer, ?> generator);
    Asyncable<List<?>> awaitToList();
}
