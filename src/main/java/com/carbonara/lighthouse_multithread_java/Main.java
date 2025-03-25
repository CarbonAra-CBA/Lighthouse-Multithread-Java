package com.carbonara.lighthouse_multithread_java;

import com.carbonara.lighthouse_multithread_java.dto.Institution;
import com.carbonara.lighthouse_multithread_java.lighthouse.LighthouseMongoService;
import com.carbonara.lighthouse_multithread_java.lighthouse.LighthouseWorker;
import com.carbonara.lighthouse_multithread_java.util.MongoDBConnector;
import com.carbonara.lighthouse_multithread_java.util.UrlManager;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import static com.carbonara.lighthouse_multithread_java.util.MongoDBConnector.createMongoClient;

@Slf4j
public class Main {

    private static final String CONNECTION_STRING = "mongodb://localhost:27017";
    private static final String INPUT_FILE = "korea_public_website_url.json";
    private static final int THREAD_COUNT = 3;
    private static final int THREAD_COUNT = getNumberOfCores();
    private static final AtomicInteger completedCount = new AtomicInteger(0);
    private static int totalTasks;

    public static void main(String[] args) {

        MongoClient mongoClient = createMongoClient(CONNECTION_STRING);
        LighthouseMongoService mongoService = new LighthouseMongoService(mongoClient, "lighthouseDB");

        List<Institution> validInstitutions = UrlManager.filterValidInstitutions(INPUT_FILE);
        totalTasks = validInstitutions.size();

        LinkedBlockingQueue<Institution> queue = new LinkedBlockingQueue<>(validInstitutions);
        ExecutorService executorService = Executors.newFixedThreadPool(THREAD_COUNT);

        for (int i = 0; i < THREAD_COUNT; i++) {
            executorService.execute(new LighthouseWorker(queue, mongoService, completedCount, totalTasks));
        }

        executorService.shutdown();
    }

    private static int getNumberOfCores() {
        return Runtime.getRuntime().availableProcessors();
    }
}
