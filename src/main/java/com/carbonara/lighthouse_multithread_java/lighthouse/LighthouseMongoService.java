package com.carbonara.lighthouse_multithread_java.lighthouse;

import com.carbonara.lighthouse_multithread_java.dto.Institution;
import com.carbonara.lighthouse_multithread_java.dto.LighthouseResultDto;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class LighthouseMongoService {
    private static LighthouseMongoService instance;
    private final MongoCollection<Document> resourceCollection;
    private final MongoCollection<Document> trafficCollection;
    private final MongoCollection<Document> unusedCollection;
    private final MongoCollection<Document> errorCollection;

    private LighthouseMongoService(MongoClient mongoClient, String databaseName) {
        MongoDatabase database = mongoClient.getDatabase(databaseName);
        this.resourceCollection = database.getCollection("resource_data");
        this.trafficCollection = database.getCollection("traffic_data");
        this.unusedCollection = database.getCollection("unused_data");
        this.errorCollection = database.getCollection("error_logs");
    }

    public static synchronized LighthouseMongoService getInstance(MongoClient mongoClient, String databaseName) {
        if (instance == null) {
            instance = new LighthouseMongoService(mongoClient, databaseName);
            System.out.println("✅ LighthouseMongoService 인스턴스 생성 완료");
        }
        return instance;
    }

    public void saveLighthouseData(LighthouseResultDto result, Institution institution) {
        try {
            if (result.getNetworkRequests().isEmpty()) {
                System.err.println("⚠️ 네트워크 요청 데이터 없음: " + result.getUrl());
                errorCollection.insertOne(new Document()
                        .append("url", result.getUrl())
                        .append("error", "Network requests empty")
                        .append("type", "empty_network_requests")
                        .append("timestamp", new Date()));
                return;
            }

            List<Document> networkRequestDocs = result.getNetworkRequests().stream()
                    .map(nr -> nr.toDocument())
                    .collect(Collectors.toList());

            List<Document> resourceSummaryDocs = result.getResourceSummary().stream()
                    .map(rs -> rs.toDocument())
                    .collect(Collectors.toList());

            Document resourceDoc = new Document()
                    .append("url", result.getUrl())
                    .append("network_request", networkRequestDocs)
                    .append("institution", institution.toDocument())
                    .append("timestamp", new Date());
            resourceCollection.insertOne(resourceDoc);

            Document trafficDoc = new Document()
                    .append("url", result.getUrl())
                    .append("resource_summary", resourceSummaryDocs)
                    .append("institution", institution.toDocument())
                    .append("timestamp", new Date());
            trafficCollection.insertOne(trafficDoc);

            Document unusedDoc = new Document()
                    .append("url", result.getUrl())
                    .append("unused_data", result.getUnusedData().toDocument())
                    .append("institution", institution.toDocument())
                    .append("timestamp", new Date());
            unusedCollection.insertOne(unusedDoc);

            System.out.println("✅ 데이터 저장 완료: " + result.getUrl());

        } catch (Exception e) {
            System.err.println("❌ MongoDB 저장 중 오류 발생: " + result.getUrl());
            e.printStackTrace();
            errorCollection.insertOne(new Document()
                    .append("url", result.getUrl())
                    .append("error", e.getMessage())
                    .append("type", "mongodb_error")
                    .append("timestamp", new Date()));
        }
    }
}
