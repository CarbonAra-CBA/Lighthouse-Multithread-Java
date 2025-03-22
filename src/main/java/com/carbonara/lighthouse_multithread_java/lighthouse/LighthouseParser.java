package com.carbonara.lighthouse_multithread_java.lighthouse;

import com.carbonara.lighthouse_multithread_java.dto.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class LighthouseParser {
    public static LighthouseResultDto parseLighthouseResult(String json, String url) {

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode root = objectMapper.readTree(json);
            JsonNode audits = root.path("audits");

            // 네트워크 요청 데이터 추출
            List<NetworkRequestDto> networkRequests = new ArrayList<>();
            JsonNode networkRequestsNode = audits
                    .path("network-requests")
                    .path("details")
                    .path("items");
            if (networkRequestsNode.isArray()) {
                for (JsonNode node : networkRequestsNode) {
                    networkRequests.add(new NetworkRequestDto(
                            node.path("url").asText(),
                            node.path("resourceType").asText(),
                            node.path("resourceSize").asLong(),
                            node.path("transferSize").asLong(),
                            node.path("statusCode").asInt(),
                            node.path("protocol").asText()
                    ));
                }
            }

            // 리소스 요약 데이터 추출
            List<ResourceSummaryDto> resourceSummary = new ArrayList<>();
            JsonNode resourceSummaryNode = audits
                    .path("resource-summary")
                    .path("details")
                    .path("items");
            if (resourceSummaryNode.isArray()) {
                for (JsonNode node : resourceSummaryNode) {
                    resourceSummary.add(new ResourceSummaryDto(
                            node.path("resourceType").asText(),
                            node.path("requestCount").asInt(),
                            node.path("transferSize").asLong()
                    ));
                }
            }

            // 미사용 데이터 추출
            UnusedDataDto unusedData = new UnusedDataDto(
                    url,
                    extractUnusedData(audits.path("unused-javascript")),
                    extractUnusedData(audits.path("unused-css-rules")),
                    extractUnusedData(audits.path("modern-image-formats"))
            );

            // 최종 결과 DTO 반환
            return new LighthouseResultDto(url, networkRequests, resourceSummary, unusedData);

        } catch (Exception e) {
            e.printStackTrace();
            log.error("❌ Lighthouse 결과 JSON 파싱 중 오류 발생.");
            return null;
        }
    }

    // 미사용 데이터 추출 메서드
    private static UnusedMetricDto extractUnusedData(JsonNode auditNode) {
        if (auditNode == null || auditNode.isMissingNode()) {
            return new UnusedMetricDto("", 0.0); // 기본값 반환
        }
        return new UnusedMetricDto(
                auditNode.path("displayValue").asText(""), // displayValue 추출
                auditNode.path("numericValue").asDouble(0.0) // numericValue 추출
        );
    }
}
