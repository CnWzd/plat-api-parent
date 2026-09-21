package com.zdd.plat.admin.common.util;

import java.security.SecureRandom;

/**
 * AK/SK 生成器（SecureRandom，URL 安全字符集）。
 */
public final class KeyGenerator {

    private static final char[] ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyz".toCharArray();
    private static final SecureRandom RANDOM = new SecureRandom();

    private KeyGenerator() {
    }

    /** 生成 AppKey：ak_ + 24 位随机小写十六进制。 */
    public static String newAppKey() {
        return "ak_" + randomHex(24);
    }

    /** 生成 AppSecret：sk_ + 40 位随机小写十六进制。 */
    public static String newAppSecret() {
        return "sk_" + randomHex(40);
    }

    private static String randomHex(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(ALPHABET[RANDOM.nextInt(16)]);
        }
        return sb.toString();
    }
}
