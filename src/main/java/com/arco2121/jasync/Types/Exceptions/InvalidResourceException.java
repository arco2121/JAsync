package com.arco2121.jasync.Types.Exceptions;

import java.util.concurrent.ExecutionException;

public class InvalidResourceException extends ExecutionException {
    public InvalidResourceException(String message) {
        super(message);
    }
}
