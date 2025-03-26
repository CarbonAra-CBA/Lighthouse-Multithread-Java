package com.carbonara.lighthouse_multithread_java;

import com.carbonara.lighthouse_multithread_java.dto.Institution;
import com.carbonara.lighthouse_multithread_java.lighthouse.LighthouseMongoService;
import com.carbonara.lighthouse_multithread_java.lighthouse.LighthouseWorker;
import com.carbonara.lighthouse_multithread_java.util.MongoDBConnector;
import com.carbonara.lighthouse_multithread_java.util.UrlManager;
import com.mongodb.client.MongoClient;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;


@Slf4j
public class Main {

    private static final String CONNECTION_STRING = "mongodb://localhost:27017";
    private static final String INPUT_FILE = "korea_public_website_url.json";
    private static final String DB_NAME = "lighthouseDB";
    private static final int THREAD_COUNT = getNumberOfCores();
    private static final AtomicInteger completedCount = new AtomicInteger(0);
    private static int totalTasks;

    public static void main(String[] args) {

        // 🌐 MongoDB 싱글톤 연결 생성
        MongoClient mongoClient = MongoDBConnector.getMongoClient(CONNECTION_STRING);
        LighthouseMongoService mongoService = LighthouseMongoService.getInstance(mongoClient, DB_NAME);

        List<Institution> validInstitutions = UrlManager.filterValidInstitutions(INPUT_FILE);
        totalTasks = validInstitutions.size();

        LinkedBlockingQueue<Institution> queue = new LinkedBlockingQueue<>(validInstitutions);

        List<List<Long>> threadTaskTimes = new ArrayList<>();

        ExecutorService executorService = new ThreadPoolExecutor(
                THREAD_COUNT,               // 코어 스레드 개수
                THREAD_COUNT * 2,           // 최대 스레드 개수 (CPU 2배까지 확장)
                60L, TimeUnit.SECONDS,      // 유휴 스레드가 종료되기까지의 시간
                new LinkedBlockingQueue<>() // 무제한 큐 (데드락 방지)
        );

        for (int i = 0; i < THREAD_COUNT; i++) {
            executorService.execute(new LighthouseWorker(queue, mongoService, completedCount, totalTasks, threadTaskTimes, i));
        }
        executorService.shutdown();
    }

    private static int getNumberOfCores() {
        return Runtime.getRuntime().availableProcessors();
    }
}
