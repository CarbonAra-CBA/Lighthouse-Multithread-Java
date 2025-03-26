package com.carbonara.lighthouse_multithread_java.util;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

public class MongoDBConnector {
    private static MongoClient mongoClient;

    private MongoDBConnector() {
        // private 생성자로 인스턴스화 방지
    }

    public static synchronized MongoClient getMongoClient(String connectionString) {
        if (mongoClient == null) {
            mongoClient = MongoClients.create(
                    MongoClientSettings.builder()
                            .applyConnectionString(new ConnectionString(connectionString))
                            .build()
            );
            System.out.println("✅ MongoDB 연결 생성 완료");
        }
        return mongoClient;
    }
}
