package com.carbonara.lighthouse_multithread_java.lighthouse;

import com.carbonara.lighthouse_multithread_java.dto.Institution;
import com.carbonara.lighthouse_multithread_java.dto.LighthouseResultDto;
import com.carbonara.lighthouse_multithread_java.util.ProgressManager;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import static com.carbonara.lighthouse_multithread_java.lighthouse.LighthouseParser.parseLighthouseResult;
import static com.carbonara.lighthouse_multithread_java.lighthouse.LighthouseRunner.runLighthouse;


@AllArgsConstructor
@Slf4j
public class LighthouseWorker implements Runnable {
    private final LinkedBlockingQueue<Institution> queue;   // 작업 대기열
    private final LighthouseMongoService mongoService;      // MongoDB 서비스
    private final AtomicInteger completedCount;             // 완료된 작업 수
    private final int totalTasks;                           // 총 작업 수

    @Override
    public void run() {
        while (!queue.isEmpty()) {

            try {

                // 대기열에서 기관 가져오기
                Institution institution = queue.take();
                if (institution == null) {
                    continue;
                }
                
                // 기관 사이트 링크 가져오기
                String url = institution.getSiteLink();

                // 처리 중인 기관 로그 출력하기
                log.info("🏢 처리 중: {}", institution.getSiteName());
                
                // Lighthouse 실행 결과 가져오기
                String originResult = runLighthouse(url);
                if (originResult == null || originResult.trim().isEmpty()) {
                    log.warn("❌ Lighthouse 실행 결과가 유효하지 않습니다: {}", url);
                    continue;
                }

                // 실행 결과에서 필요한 정보 파싱하기 -> Dto로 저장
                LighthouseResultDto parsedResult = parseLighthouseResult(originResult, url);
                if (parsedResult == null) {
                    log.warn("❌ Lighthouse 결과 파싱 실패: {}", url);
                    continue;
                }

                // 파싱된 결과를 MongoDB에 저장하기
                mongoService.saveLighthouseData(parsedResult, institution);
                log.info("⭐ 저장 완료: {}", institution.getSiteName());

            } catch (Exception e) {
                log.error("🚨 오류 발생: {}", e.getMessage(), e);
            } finally {
                int done = completedCount.incrementAndGet();
                log.info("📊 진행도: {}/{}", done, totalTasks);


                // 진행 상태 업데이트 (인덱스 저장)
                ProgressManager.saveProgress(done);
            }
        }
    }
}
