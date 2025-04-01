package com.carbonara.lighthouse_multithread_java.lighthouse;

import com.carbonara.lighthouse_multithread_java.dto.Institution;
import com.carbonara.lighthouse_multithread_java.dto.LighthouseResultDto;
import com.carbonara.lighthouse_multithread_java.util.ProgressManager;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
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
            Institution institution = null;
            try {
                log.info("📌 작업 대기열에서 기관 가져오는 중...");
                institution = queue.take(); // 다음 기관 가져오기

                if (institution == null) {
                    log.warn("⚠️ 기관 정보가 null입니다. 다음 작업으로 넘어갑니다.");
                    continue;
                }

                String url = institution.getSiteLink();
                log.info("🌍 Lighthouse 실행 시작 - 기관명: {} | URL: {}", institution.getSiteName(), url);

                // Lighthouse 실행
                String originResult = runLighthouse(url);
                if (originResult == null) {
                    log.warn("❌ Lighthouse 실행 결과가 null - URL: {}", url);
                    continue;
                }
                else if (originResult.trim().isEmpty()) {
                    log.warn("❌ Lighthouse 실행 결과가 빈 문자열 - URL: {}", url);
                    continue;
                }
                log.info("📥 Lighthouse 실행 완료 - 결과 길이: {} bytes | URL: {}", originResult.length(), url);

                // 결과 파싱
                log.info("🛠️ Lighthouse 결과 파싱 시작 - URL: {}", url);
                LighthouseResultDto parsedResult = parseLighthouseResult(originResult, url);

                if (parsedResult == null) {
                    log.warn("❌ Lighthouse 결과 파싱 실패 - URL: {}", url);

                    // 파싱 오류가 발생했을 때 도메인 유효성 검사
                    if (!isDomainValid(url)) {
                        log.warn("❌ 도메인 유효하지 않음 - URL: {}", url);
                    }
                    continue;
                }
                log.info("✅ Lighthouse 결과 파싱 완료 - URL: {}", url);

                // MongoDB 저장
                log.info("💾 MongoDB 저장 시작 - 기관명: {}", institution.getSiteName());
                mongoService.saveLighthouseData(parsedResult, institution);
                log.info("⭐ 저장 완료 - 기관명: {}", institution.getSiteName());

            } catch (Exception e) {
                log.error("🚨 오류 발생 - 기관명: {} | 원인: {}",
                        institution != null ? institution.getSiteName() : "알 수 없음",
                        e.getMessage(), e);
            } finally {
                int done = completedCount.incrementAndGet();
                log.info("📊 진행도 업데이트: {}/{}", done, totalTasks);

                // 진행 상태 업데이트
                ProgressManager.saveProgress(done);
            }
        }
    }

    // 도메인 유효성 검사
    private boolean isDomainValid(String url) {
        try {
            InetAddress.getByName(new URL(url).getHost()); // 도메인 확인
            return true;
        } catch (UnknownHostException e) {
            return false; // 도메인이 존재하지 않음
        } catch (Exception e) {
            log.error("❌ 도메인 검사 중 오류 발생 - URL: {}", url, e);
            return false;
        }
    }
}
