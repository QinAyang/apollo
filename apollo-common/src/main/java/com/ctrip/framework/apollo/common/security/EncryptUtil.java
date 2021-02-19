package com.ctrip.framework.apollo.common.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ObjectUtils;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * the util of encrypt/decrypt
 * <p>
 *
 * @author Ning.Zhang(Pegasus)
 * *****************************************************************
 * Name               Action            Time          Description  *
 * Ning.Zhang       Initialize        2/14/2021        Initialize  *
 * *****************************************************************
 */
public final class EncryptUtil {
    private static final Logger logger = LoggerFactory.getLogger(EncryptUtil.class);
    public static final String PREFIX = "ENC(";
    public static final String SUFFIX = ")";
    private static final Pattern PATTERN = Pattern.compile(PREFIX.replace("(", "\\(") +
            "(.+?)" +
            SUFFIX.replace(")", "\\)"));

    public static void decrypt(final Map<String, String> configurations,
                               final RsaKey rsaKey,
                               final List<Exception> exceptionList) {
        if (null == configurations || configurations.isEmpty() || null == rsaKey) {
            return;
        }
        configurations.replaceAll((k, v) -> decrypt(v, rsaKey, exceptionList));
    }

    public static String decrypt(String value,
                                 final RsaKey rsaKey,
                                 final List<Exception> exceptionList) {
        if (ObjectUtils.isEmpty(value) || null == rsaKey) {
            return value;
        }
        Matcher matcher = PATTERN.matcher(value);
        while (matcher.find()) {
            String orig = matcher.group(1);
            String decry;
            try {
                decry = RsaTool.decrypt(matcher.group(1), rsaKey.getPrivateKey());
            } catch (final Exception exception) {
                exceptionList.add(exception);
                logger.error(String.format("Decrypt meet error, caused: %s", exception.getMessage()), exception);
                return value;
            }
            value = value.replace(PREFIX.concat(orig).concat(SUFFIX), decry);
            matcher = PATTERN.matcher(value);
        }
        return value;
    }

    public static String addTag(String value) {
        if (!value.startsWith(PREFIX) && !value.endsWith(SUFFIX)) {
            value = PREFIX.concat(value).concat(SUFFIX);
        }
        return value;
    }

    public static String removeTag(String value) {
        if (value.startsWith(PREFIX) && value.endsWith(SUFFIX)) {
            value = value.substring(PREFIX.length(), value.length() - 1);
        }
        return value;
    }
}