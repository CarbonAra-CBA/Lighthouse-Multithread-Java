package com.carbonara.lighthouse_multithread_java.lighthouse;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class LighthouseRunner {
    private static final String lighthousePath = "C:\\Users\\k4150\\AppData\\Roaming\\npm\\lighthouse.cmd"; // Lighthouse 실행 경로

    public static String runLighthouse(String url) {
        long startTime = System.currentTimeMillis(); // 시작 시간 기록

        try {
            List<String> command = new ArrayList<>();
            command.add(lighthousePath);
            command.add(url);
            command.add("--output=json");
            command.add("--quiet");
            command.add("--only-categories=performance");
            command.add("--max-wait-for-load=50000");
            command.add("--disable-storage-reset");
            command.add("--chrome-flags=" + String.join(" ",
                    "--headless",
                    "--remote-debugging-port=9222",
                    "--disable-gpu",
                    "--no-sandbox",
                    "--disable-dev-shm-usage",
                    "--single-process",
                    "--disk-cache-dir=/tmp/lh-cache",
                    "--memory-pressure-off"
            ));

            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            StringBuilder output = new StringBuilder();
            boolean jsonStarted = false;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.info("\uD83D\uDEA8 Lighthouse 출력: " + line); // 각 라인 출력

                    if (!jsonStarted && line.trim().startsWith("{")) {
                        jsonStarted = true;
                    }
                    if (jsonStarted) {
                        output.append(line).append("\n");
                    }
                }
            }
            process.waitFor();

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
