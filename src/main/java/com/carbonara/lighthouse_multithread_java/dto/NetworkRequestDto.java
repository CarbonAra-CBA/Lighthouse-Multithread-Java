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
public class NetworkRequestDto {
    private String url;
    private String resourceType;
    private long resourceSize;
    private long transferSize;
    private int statusCode;
    private String protocol;

}


