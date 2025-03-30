package com.carbonara.lighthouse_multithread_java;

import com.carbonara.lighthouse_multithread_java.dto.Institution;
import com.carbonara.lighthouse_multithread_java.lighthouse.LighthouseMongoService;
import com.carbonara.lighthouse_multithread_java.lighthouse.LighthouseWorker;
import com.carbonara.lighthouse_multithread_java.util.MongoDBConnector;
import com.carbonara.lighthouse_multithread_java.util.UrlManager;
import com.mongodb.client.MongoClient;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
public class Main {

    private static final String CONNECTION_STRING = "mongodb://localhost:27017";                // MongoDB 연결 문자열
    private static final String INPUT_FILE = "korea_public_website_url_2.json";                   // 입력 파일 이름
    private static final String DB_NAME = "lighthouseDB";                                       // DB명
    private static final int THREAD_COUNT = getNumberOfCores();                                 // 사용할 스레드 수
    private static final AtomicInteger completedCount = new AtomicInteger(0);     // 완료된 작업 수
    private static final AtomicLong totalExecutionTime = new AtomicLong(0);       // 총 실행 시간
    private static int totalTasks;

    public static void main(String[] args) {
        long startTime = System.currentTimeMillis(); // 실행 시작 시간

        // MongoDB 싱글톤 연결 생성
        MongoClient mongoClient = MongoDBConnector.getMongoClient(CONNECTION_STRING);
        if (mongoClient == null) {
            log.error("MongoDB 연결 실패");
            return;
        }
        LighthouseMongoService mongoService = LighthouseMongoService.getInstance(mongoClient, DB_NAME);

        // 유효한 URL이며 공공기관에 해당하는 기관 목록 추출
        List<Institution> validInstitutions = UrlManager.filterValidInstitutions(INPUT_FILE);
        totalTasks = validInstitutions.size();

        // 작업 대기열 생성
        LinkedBlockingQueue<Institution> queue = new LinkedBlockingQueue<>(validInstitutions);

        // 스레드 풀 생성 -> 스레드 관리
        ExecutorService executorService = new ThreadPoolExecutor(
                THREAD_COUNT,               // 코어 스레드 개수
                THREAD_COUNT * 2,           // 최대 스레드 개수 (CPU 2배까지 확장)
                60L, TimeUnit.SECONDS,      // 유휴 스레드가 종료되기까지의 시간
                new LinkedBlockingQueue<>() // 무제한 큐 (데드락 방지)
        );

        // 각 스레드에 작업 할당
        for (int i = 0; i < THREAD_COUNT; i++) {
            executorService.submit(() -> {
                try {
                    long threadStartTime = System.currentTimeMillis();
                    new LighthouseWorker(queue, mongoService, completedCount, totalTasks).run();
                    long elapsedTime = System.currentTimeMillis() - threadStartTime;
                    totalExecutionTime.addAndGet(elapsedTime);
                } catch (Exception e) {
                    log.error("작업 수행 중 오류 발생", e);
                }
            });
        }

        // 스레드 풀 종료
        executorService.shutdown();

        try {
            executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.error("스레드 종료 대기 중 오류 발생", e);
        }

        long endTime = System.currentTimeMillis(); // 실행 종료 시간
        long totalElapsedTime = endTime - startTime;
        double averageExecutionTime = (double) totalExecutionTime.get() / totalTasks;

        log.info("\uD83D\uDD56 총 실행 시간: {}ms", totalElapsedTime);
        log.info("\uD83D\uDD56 평균 작업 실행 시간: {}ms", averageExecutionTime);
    }

    // 사용 가능한 코어 수 반환
    private static int getNumberOfCores() {
        return Runtime.getRuntime().availableProcessors();
    }
}
