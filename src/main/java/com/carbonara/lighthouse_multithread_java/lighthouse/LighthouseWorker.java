package com.carbonara.lighthouse_multithread_java.lighthouse;

import com.carbonara.lighthouse_multithread_java.dto.Institution;
import com.carbonara.lighthouse_multithread_java.dto.LighthouseResultDto;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import static com.carbonara.lighthouse_multithread_java.lighthouse.LighthouseParser.parseLighthouseResult;
import static com.carbonara.lighthouse_multithread_java.lighthouse.LighthouseRunner.runLighthouse;


@AllArgsConstructor
@Slf4j
public class LighthouseWorker implements Runnable {
    private final LinkedBlockingQueue<Institution> queue;
    private final LighthouseMongoService mongoService;
    private final AtomicInteger completedCount;
    private final int totalTasks;

    @Override
    public void run() {
        while (!queue.isEmpty()) {
            try {
                Institution institution = queue.poll();
                if (institution == null) {
                    continue;
                }

                String url = institution.getSiteLink();
                log.info("🏢 Processing: {}", institution.getSiteName());

                String originResult = runLighthouse(url);
                if (originResult == null || originResult.trim().isEmpty()) {
                    log.warn("❌ Lighthouse 실행 결과가 유효하지 않습니다: {}", url);
                    continue;
                }

                LighthouseResultDto parsedResult = parseLighthouseResult(originResult, url);
                if (parsedResult == null) {
                    log.warn("❌ Lighthouse 결과 파싱 실패: {}", url);
                    continue;
                }

                mongoService.saveLighthouseData(parsedResult, institution);
                log.info("✅ 저장 완료: {}", institution.getSiteName());

            } catch (Exception e) {
                log.error("🚨 오류 발생: {}", e.getMessage(), e);
            } finally {
                int done = completedCount.incrementAndGet();
                log.info("📊 진행도: {}/{}", done, totalTasks);
            }
        }
    }
}
