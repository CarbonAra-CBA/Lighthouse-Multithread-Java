package com.carbonara.lighthouse_multithread_java.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.bson.Document;


@Getter
@Setter
@ToString
@AllArgsConstructor
public class UnusedDataDto {
    private String url;
    private UnusedMetricDto unusedJavascript;
    private UnusedMetricDto unusedCssRules;
    private UnusedMetricDto modernImageFormats;

}
