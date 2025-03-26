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

@Slf4j
public class Main {

    private static final String CONNECTION_STRING = "mongodb://localhost:27017";                // MongoDB 연결 문자열
    private static final String INPUT_FILE = "korea_public_website_url.json";                   // 입력 파일 이름
    private static final String DB_NAME = "lighthouseDB";                                       // DB명
    private static final int THREAD_COUNT = getNumberOfCores();                                 // 사용할 스레드 수
    private static final AtomicInteger completedCount = new AtomicInteger(0);     // 완료된 작업 수
    private static int totalTasks;                                                              // 총 작업 수

    public static void main(String[] args) {

        // MongoDB 싱글톤 연결 생성
        MongoClient mongoClient = MongoDBConnector.getMongoClient(CONNECTION_STRING);
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
            executorService.execute(new LighthouseWorker(queue, mongoService, completedCount, totalTasks));
        }
        
        // 스레드 풀 종료
        executorService.shutdown();
    }

    // 사용 가능한 코어 수 반환
    private static int getNumberOfCores() {
        return Runtime.getRuntime().availableProcessors();
    }
}
