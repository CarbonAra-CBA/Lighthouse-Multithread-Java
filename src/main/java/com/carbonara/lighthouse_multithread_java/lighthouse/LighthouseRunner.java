package com.carbonara.lighthouse_multithread_java.lighthouse;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class LighthouseRunner {
    private static final String lighthousePath = "C:\\Users\\k4150\\AppData\\Roaming\\npm\\lighthouse.cmd"; // Lighthouse 실행 경로

    public static String runLighthouse(String url) {
        try {
            List<String> command = new ArrayList<>();
            command.add(lighthousePath);
            command.add(url);
            command.add("--output=json");
            command.add("--quiet");

            // 성능 관련 검사만 수행
            command.add("--only-categories=performance");

            // 스크린샷 관련 검사 생략
            command.add("--skip-audits=screenshot-thumbnails,final-screenshot,full-page-screenshot");

            // 네트워크 및 CPU 속도 제한 설정
            command.add("--throttling.rttMs=40");
            command.add("--throttling.throughputKbps=10240");
            command.add("--throttling.cpuSlowdownMultiplier=1");
            command.add("--throttling.requestLatencyMs=0");
            command.add("--throttling.downloadThroughputKbps=0");
            command.add("--throttling.uploadThroughputKbps=0");

            // 실행할 감사 항목 추가
            command.add("--only-audits=network-requests,resource-summary,network-rtt,network-server-latency,unused-javascript,unused-css-rules,modern-image-formats");

            // Chrome 실행 플래그 추가
            command.add("--chrome-flags=" + String.join(" ",
                    "--headless",
                    "--disable-gpu",
                    "--no-sandbox",
                    "--disable-dev-shm-usage",
                    "--disable-software-rasterizer",
                    "--no-zygote",
                    "--disable-setuid-sandbox",
                    "--disable-accelerated-2d-canvas",
                    "--disable-accelerated-jpeg-decoding",
                    "--disable-accelerated-mjpeg-decode",
                    "--disable-accelerated-video-decode",
                    "--disable-gpu-rasterization",
                    "--disable-zero-copy",
                    "--ignore-certificate-errors"
            ));

            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            // 결과 읽기
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

            process.waitFor();

            return output.toString();

        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }
}
