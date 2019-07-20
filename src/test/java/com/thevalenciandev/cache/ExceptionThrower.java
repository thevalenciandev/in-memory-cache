package com.thevalenciandev.cache;

import java.util.concurrent.Callable;

public class ExceptionThrower {

    public static Throwable exceptionFrom(Callable<?> function) {

        try {
            function.call();
            return null;
        } catch (Throwable t) {
            return t;
        }
    }

}