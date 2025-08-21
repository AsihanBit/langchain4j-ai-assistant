package com.aiassist.chat.server.result;


import lombok.Data;

import java.io.Serializable;

/**
 * 后端统一返回
 */
@Data
public class Result<T> implements Serializable {

    private Integer code; // 编码 1成功 0和其它数字失败
    private String msg; // 错误信息
    private T data; // 数据

    public static <T> Result<T> success() {
        Result<T> result = new Result<T>();
        result.setCode(1);
        return result;
    }

    public static <T> Result<T> success(String msg) {
        Result<T> result = new Result<T>();
        result.setCode(1);
        result.setMsg(msg);
        return result;
    }

    public static <T> Result<T> success(T object) {
        Result<T> result = new Result<T>();
        result.setCode(1);
        result.setData(object);
        return result;
    }

    public static Result<?> error(String msg) {
        Result<?> result = new Result<>();
        result.setCode(0);
        result.setMsg(msg);
        return result;
    }

}
