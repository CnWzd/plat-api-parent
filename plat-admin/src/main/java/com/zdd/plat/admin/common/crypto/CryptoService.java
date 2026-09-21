package com.zdd.plat.admin.common.crypto;

import com.zdd.plat.admin.common.config.PlatProperties;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AppSecret 加解密（AES-256-GCM）。
 * <p>密钥由配置项派生（SHA-256 摘要归一为 32 字节）；每次加密使用随机 12 字节 IV，
 * 密文格式 Base64(iv || ciphertext+tag)。</p>
 * <p>SK 必须可逆（网关 HMAC 验签需要原始密钥），故采用对称加密而非 BCrypt。</p>
 */
@Component
public class CryptoService {

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12;
    private static final int TAG_BITS = 128;

    private final SecretKeySpec key;
    private final SecureRandom random = new SecureRandom();

    public CryptoService(PlatProperties properties) {
        try {
            byte[] raw = properties.cryptoOrDefault().secretKey().getBytes(StandardCharsets.UTF_8);
            this.key = new SecretKeySpec(MessageDigest.getInstance("SHA-256").digest(raw), "AES");
        } catch (Exception e) {
            throw new IllegalStateException("初始化加密密钥失败", e);
        }
    }

    public String encrypt(String plainText) {
        try {
            byte[] iv = new byte[IV_LENGTH];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            byte[] out = new byte[IV_LENGTH + cipherText.length];
            System.arraycopy(iv, 0, out, 0, IV_LENGTH);
            System.arraycopy(cipherText, 0, out, IV_LENGTH, cipherText.length);
            return Base64.getEncoder().encodeToString(out);
        } catch (Exception e) {
            throw new IllegalStateException("加密失败", e);
        }
    }

    public String decrypt(String encoded) {
        try {
            byte[] all = Base64.getDecoder().decode(encoded);
            byte[] iv = new byte[IV_LENGTH];
            System.arraycopy(all, 0, iv, 0, IV_LENGTH);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            byte[] plain = cipher.doFinal(all, IV_LENGTH, all.length - IV_LENGTH);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("解密失败（密钥是否变更？）", e);
        }
    }
}
