package com.carbonara.lighthouse_multithread_java.dto;

import lombok.*;
import org.bson.Document;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class Institution {
    private String institutionType;
    private String institutionCategory;
    private String institutionSubcategory;
    private String siteName;
    private String siteType;
    private String siteLink;

    public Document toDocument() {
        return new Document()
                .append("institutionType", institutionType)
                .append("institutionCategory", institutionCategory)
                .append("institutionSubcategory", institutionSubcategory)
                .append("siteName", siteName)
                .append("siteType", siteType)
                .append("siteLink", siteLink);
    }
}

