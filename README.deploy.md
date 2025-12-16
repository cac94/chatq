# ChatQ 배포 가이드

## 🚀 자동 배포 (권장)

### 전체 빌드 및 배포
```powershell
.\deploy.ps1
```

### 빌드 후 바로 실행
```powershell
.\deploy.ps1 -RunServer
```

### 부분 빌드 옵션
```powershell
# 프론트엔드만 다시 빌드
.\deploy.ps1 -SkipBackend

# 백엔드만 다시 빌드
.\deploy.ps1 -SkipFrontend
```

## 📦 배포 프로세스

### 자동화 스크립트가 수행하는 작업:

1. **프론트엔드 빌드**
   - `chatq` 폴더에서 `npm run build` 실행
   - React 앱을 정적 파일(HTML, CSS, JS)로 빌드
   - `dist` 폴더에 결과물 생성

2. **정적 파일 복사**
   - `chatq/dist` → `chatq-server/src/main/resources/static`
   - Spring Boot가 정적 파일도 서빙할 수 있도록 설정

3. **백엔드 빌드**
   - `chatq-server` 폴더에서 Maven 빌드
   - 정적 파일이 포함된 실행 가능한 JAR 파일 생성
   - `target/chatq-server-*.jar` 생성

4. **서버 실행** (선택적)
   - `-RunServer` 옵션 사용 시 JAR 파일 실행
   - 포트 8080에서 서버 시작

## 🌐 배포 후 구조

```
포트 8080 (Spring Boot)
├─ /api/**        → REST API
├─ /index.html    → React 앱 (메인 페이지)
└─ /assets/**     → React 정적 파일 (JS, CSS, 이미지 등)
```

- **개발 환경**: Vite 프록시 사용 (프론트엔드: 5174, 백엔드: 8080)
- **프로덕션**: Spring Boot 하나로 통합 (포트 8080)

## 🔧 수동 배포

자동화 스크립트를 사용하지 않는 경우:

### 1. 프론트엔드 빌드
```powershell
cd chatq
npm install
npm run build
```

### 2. 정적 파일 복사
```powershell
# PowerShell
robocopy .\chatq\dist .\chatq-server\src\main\resources\static /E /IS /PURGE

# 또는 수동으로 복사
# chatq/dist/* → chatq-server/src/main/resources/static/
```

### 3. 백엔드 빌드
```powershell
cd chatq-server
.\mvnw.cmd clean package -DskipTests
```

### 4. 서버 실행
```powershell
java -jar target\chatq-server-0.0.1-SNAPSHOT.jar
```

## 📝 배포 체크리스트

배포 전 확인사항:

- [ ] Node.js 설치 확인 (`node --version`)
- [ ] Java 21 설치 확인 (`java --version`)
- [ ] MariaDB 실행 중
- [ ] Ollama 서버 실행 중 (AI 모델 사용 시)
- [ ] `application.properties` 설정 확인
  - 데이터베이스 연결 정보
  - AI 모델 설정
  - 포트 설정

## 🐛 문제 해결

### 빌드 실패 시

**프론트엔드 빌드 오류:**
```powershell
cd chatq
Remove-Item -Recurse -Force node_modules
npm install
npm run build
```

**백엔드 빌드 오류:**
```powershell
cd chatq-server
.\mvnw.cmd clean
.\mvnw.cmd package -DskipTests
```

### 실행 시 404 오류

- Spring Boot의 static 폴더에 파일이 제대로 복사되었는지 확인
- `chatq-server/src/main/resources/static/index.html` 존재 여부 확인

### CORS 오류 (배포 환경)

- 배포 환경에서는 모든 요청이 같은 origin(8080)에서 오므로 CORS 문제 없음
- 만약 다른 도메인에서 접속한다면 `WebConfig.java`의 `allowedOrigins` 수정 필요

## 🌍 프로덕션 배포

### 서버에 배포 시

1. JAR 파일을 서버로 복사
2. 환경 변수 설정 (필요시)
3. systemd 등으로 서비스 등록
4. 리버스 프록시 설정 (nginx 등)

### Docker 배포 (추후 지원)

현재는 JAR 파일 직접 실행 방식을 권장합니다.

## 💡 팁

- **개발 중**: 별도로 실행 (`npm run dev` + Spring Boot)
- **테스트/배포**: 통합 실행 (`.\deploy.ps1 -RunServer`)
- **빠른 재배포**: 변경된 부분만 빌드 (`-SkipFrontend` 또는 `-SkipBackend`)
