package com.carbonara.lighthouse_multithread_java.lighthouse;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class LighthouseRunner {
    private static final String LIGHTHOUSE_PATH = System.getenv("LIGHTHOUSE_PATH"); // Lighthouse 실행 경로

    public static String runLighthouse(String url) {
        long startTime = System.currentTimeMillis(); // 시작 시간 기록

        try {
            List<String> command = new ArrayList<>();
            command.add(LIGHTHOUSE_PATH);
            command.add(url);
            command.add("--output=json"); // JSON 형식으로 결과 출력
            command.add("--quiet"); // 실행 중 로그 출력 최소화
            command.add("--only-categories=performance"); // 성능 카테고리만 분석
            command.add("--max-wait-for-load=7000"); // 최대 대기 시간 설정 (밀리초)
            command.add("--chrome-flags=" + String.join(" ",
                    "--headless", // 헤드리스 모드로 실행 (UI 없이)
                    "--disable-gpu", // GPU 비활성화
                    "--no-sandbox", // 샌드박스 모드 비활성화
                    "--disable-dev-shm-usage", // /dev/shm 사용 비활성화
                    "--disk-cache-dir=/tmp/lh-cache" // 디스크 캐시 디렉토리 설정
            ));


            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            StringBuilder output = new StringBuilder();
            boolean jsonStarted = false;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {

                    if (!jsonStarted && line.trim().startsWith("{")) {
                        jsonStarted = true;
                    }
                    if (jsonStarted) {
                        output.append(line).append("\n");
                    }
                }
            }
            try {
                process.waitFor();
            } catch (InterruptedException e) {
                log.error("Lighthouse 프로세스 대기 중 오류 발생: " + e.getMessage());
                Thread.currentThread().interrupt(); // 인터럽트 상태를 복원
            }

            long endTime = System.currentTimeMillis(); // 종료 시간 기록
            long elapsedTime = endTime - startTime; // 소요 시간 계산
            log.info("\uD83D\uDCA1 Lighthouse 실행 완료. 소요 시간: " + elapsedTime + "ms");


            return output.toString();

        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }
}
