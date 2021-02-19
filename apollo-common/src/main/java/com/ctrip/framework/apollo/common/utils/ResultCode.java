package com.ctrip.framework.apollo.common.utils;


import org.springframework.util.ObjectUtils;

/**
 * the enum for microservice's result
 * <p>
 * *****************************************************************
 * Name               Action            Time          Description  *
 * Ning.Zhang       Initialize        2/14/2021        Initialize  *
 * *****************************************************************
 */
public enum ResultCode {
    SUCCESS(0L, true, "SUCCESS"),
    ERROR(1L, false, "ERROR");

    private final Long code;
    private final String description;
    private final Boolean success;

    ResultCode(final long code,
               final Boolean success,
               final String description) {
        this.code = code;
        this.success = success;
        this.description = description;
    }

    public Long getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public Boolean getSuccess() {
        return success;
    }

    public static ResultCode get(final Long code) {
        if (null == code) {
            return null;
        }

        for (final ResultCode item : ResultCode.values()) {
            if (item.getCode().equals(code)) {
                return item;
            }
        }
        return null;
    }

    public static ResultCode get(final String description) {
        if (ObjectUtils.isEmpty(description)) {
            return null;
        }

        for (final ResultCode item : ResultCode.values()) {
            if (item.getDescription().equals(description)) {
                return item;
            }
        }
        return null;
    }
}