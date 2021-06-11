package com.spldeolin.allison1875.inspector.javabean;

import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * @author Deolin 2020-02-22
 */
@Data
public class LawlessDto {

    @JsonProperty("源码位置")
    private String sourceCode;

    /**
     * - type qualifier
     * - field qualifier
     * - method qualifier
     * - or else null
     */
    @JsonProperty("全限定名")
    private String qualifier;

    @JsonProperty("规约号")
    private String statuteNo;

    @JsonProperty("详细信息")
    private String message;

    @JsonProperty("作者")
    private String author;

    @JsonProperty("修复者")
    private String fixer;

    @JsonProperty("修复时间")
    private LocalDateTime fixedAt;

}
