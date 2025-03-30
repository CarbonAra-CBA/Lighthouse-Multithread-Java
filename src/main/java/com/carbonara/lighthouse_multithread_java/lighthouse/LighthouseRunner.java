package com.carbonara.lighthouse_multithread_java.lighthouse;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
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
            command.add("--output=json");
            command.add("--quiet");
            command.add("--only-categories=performance");
            command.add("--max-wait-for-load=20000"); // 대기 시간을 줄임
            command.add("--disable-storage-reset");
            command.add("--chrome-flags=" + String.join(" ",
                    "--headless",
                    "--disable-gpu",
                    "--no-sandbox",
                    "--disable-dev-shm-usage",
                    "--disk-cache-dir=/tmp/lh-cache",
                    "--memory-pressure-off",
//                    "--disable-software-rasterizer",  // 소프트웨어 래스터라이저 비활성화
//                    "--disable-extensions",  // 확장 프로그램 비활성화
//                    "--disable-plugins",  // 플러그인 비활성화
//                    "--disable-cache",  // 캐시 비활성화
                    "--disable-network-throttling",  // 네트워크 제한 비활성화
                    "--throttling-method=devtools",  // 빠른 성능 테스트를 위한 개발자 도구 방법 사용
                    "--disable-background-networking", // 백그라운드 네트워크 비활성화
                    "--disable-sync",                  // 동기화 비활성화
                    "--disable-component-extensions-with-background-pages", // 백그라운드 페이지 비활성화
                    "--mute-audio"                     // 오디오 비활성화
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
