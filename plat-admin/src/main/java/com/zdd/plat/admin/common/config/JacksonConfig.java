package com.zdd.plat.admin.common.config;

import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * JSON 序列化规范：
 * <ul>
 *   <li>Long → String：防止前端 JS Number 精度丢失（雪花/自增 ID 安全上限 2^53）</li>
 *   <li>LocalDateTime → "yyyy-MM-dd HH:mm:ss"</li>
 * </ul>
 */
@Configuration
public class JacksonConfig {

    public static final DateTimeFormatter DATETIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    public static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Bean
    public ObjectMapper objectMapper() {
        SimpleModule longModule = new SimpleModule();
        longModule.addSerializer(Long.class, ToStringSerializer.instance);
        longModule.addSerializer(Long.TYPE, ToStringSerializer.instance);

        JavaTimeModule timeModule = new JavaTimeModule();
        timeModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(DATETIME));
        timeModule.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(DATETIME));
        timeModule.addSerializer(LocalDate.class, new com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer(DATE));
        timeModule.addDeserializer(LocalDate.class, new com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer(DATE));

        return new ObjectMapper()
                .registerModule(longModule)
                .registerModule(timeModule);
    }
}
