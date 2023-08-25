package com.github.wlxxyw.maven.exception;

import lombok.Getter;

import static com.github.wlxxyw.utils.StrTemplate.format;

/**
 * 自定义异常
 */
@Getter
public class MavenException extends RuntimeException {
    private final int code;

    public MavenException(int code, String message) {
        super(message);
        this.code = code;
    }

    public MavenException(int code, String message,Throwable t) {
        super(message,t);
        this.code = code;
    }

    public MavenException(int code, String template, Object... param) {
        super(format(template, param), findThrowable(param));
        this.code = code;
    }

    private static Throwable findThrowable(Object... param) {
        if (null == param || param.length == 0) return null;
        for (int i = param.length; i > 0; i--) {
            if (param[i - 1] instanceof Throwable) return (Throwable) param[i - 1];
        }
        return null;
    }
}