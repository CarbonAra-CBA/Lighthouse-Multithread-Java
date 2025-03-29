# OpenJDK 17을 기반으로 하는 기본 이미지 설정
FROM openjdk:17-jdk-slim

# 작업 디렉토리 설정
WORKDIR /app

# 의존성 설치: curl, wget, gnupg
RUN apt-get update && apt-get install -y \
    curl \
    wget \
    gnupg

# MongoDB 클라이언트 설치 (버전 7.0)
RUN curl -fsSL https://www.mongodb.org/static/pgp/server-7.0.asc | gpg --dearmor -o /usr/share/keyrings/mongodb-keyring.gpg \
    && echo "deb [signed-by=/usr/share/keyrings/mongodb-keyring.gpg] https://repo.mongodb.org/apt/debian bullseye/mongodb-org/7.0 main" | tee /etc/apt/sources.list.d/mongodb-org-7.0.list \
    && apt-get update && apt-get install -y mongodb-org-tools

# Node.js 설치 (버전 20.x)
RUN curl -fsSL https://deb.nodesource.com/setup_20.x | bash - \
    && apt-get install -y nodejs

# Install latest chrome stable package.
RUN wget -q -O - https://dl-ssl.google.com/linux/linux_signing_key.pub | apt-key add -
RUN sh -c 'echo "deb [arch=amd64] http://dl.google.com/linux/chrome/deb/ stable main" >> /etc/apt/sources.list.d/google.list'
RUN apt-get update \
    && apt-get install -y google-chrome-stable --no-install-recommends \
    && apt-get clean

# Install Lighthouse CI
RUN npm install -g @lhci/cli@0.14.0
RUN npm install -g lighthouse

# 애플리케이션 JAR 파일 복사 (SNAPSHOT 대응)
COPY build/libs/lighthouse-multithread-java-*.jar app.jar

# 필요한 JSON 파일 복사
COPY korea_public_website_url_2.json /app/korea_public_website_url_2.json

# 포트 노출
EXPOSE 8080

# 애플리케이션 실행
CMD ["java", "-jar", "app.jar"]