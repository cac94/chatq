# SSL 인증서 설치 가이드

## 현재 설정
- **인증서 파일**: `chatq.dev-new.p12`
- **유효 도메인**: 
  - chatq.dev
  - *.chatq.dev
  - localhost
  - 127.0.0.1
  - 192.168.1.72 (현재 서버 IP)
- **포트**: 443 (HTTPS)
- **만료일**: 2028년 3월 29일

## 서버 PC (현재 PC)
mkcert가 이미 설치되고 로컬 CA가 신뢰되도록 설정되어 있습니다.

## 다른 PC에서 접속하기

### 1. hosts 파일 설정
다른 PC의 hosts 파일에 서버 IP 추가:

**Windows**: `C:\Windows\System32\drivers\etc\hosts`
**Mac/Linux**: `/etc/hosts`

```
192.168.1.72  chatq.dev
192.168.1.72  www.chatq.dev
```

### 2. CA 인증서 설치 (필수)

#### Windows:
1. `mkcert-rootCA.pem` 파일을 다른 PC로 복사
2. 파일 더블클릭 또는 우클릭 > "인증서 설치"
3. "로컬 컴퓨터"에 저장 선택
4. "모든 인증서를 다음 저장소에 저장" 선택
5. "신뢰할 수 있는 루트 인증 기관" 선택
6. 완료

#### Mac:
```bash
sudo security add-trusted-cert -d -r trustRoot -k /Library/Keychains/System.keychain mkcert-rootCA.pem
```

#### Linux:
```bash
sudo cp mkcert-rootCA.pem /usr/local/share/ca-certificates/mkcert-rootCA.crt
sudo update-ca-certificates
```

### 3. 브라우저에서 접속
```
https://chatq.dev
```
또는
```
https://192.168.1.72
```

## 서버 IP가 변경된 경우

서버 IP가 변경되면 새 인증서를 생성해야 합니다:

```powershell
cd D:\chatq\dev\chatq-server\src\main\resources\ssl

# 새 IP로 인증서 생성 (예: 192.168.1.100)
mkcert chatq.dev "*.chatq.dev" localhost 127.0.0.1 ::1 192.168.1.100

# PKCS12로 변환
& "C:\Program Files\FireDaemon OpenSSL 3\bin\openssl.exe" pkcs12 -export -in "chatq.dev+5.pem" -inkey "chatq.dev+5-key.pem" -out "chatq.dev-new.p12" -name chatq -password pass:changeit
```

## 주의사항

1. **CA 인증서 보안**: `mkcert-rootCA.pem`과 `rootCA-key.pem`은 매우 중요합니다. 이 파일이 유출되면 공격자가 신뢰할 수 있는 인증서를 생성할 수 있습니다.

2. **개발 전용**: mkcert는 개발/테스트 환경 전용입니다. 프로덕션에서는 Let's Encrypt 등 공인 인증서를 사용하세요.

3. **네트워크 범위**: 이 인증서는 로컬 네트워크 내에서만 작동합니다.

## 문제 해결

### 브라우저에서 "안전하지 않음" 경고가 표시되는 경우
- CA 인증서가 제대로 설치되지 않았을 수 있습니다
- 브라우저를 재시작해보세요
- Windows의 경우 인증서가 "신뢰할 수 있는 루트 인증 기관"에 있는지 확인하세요

### 연결이 안 되는 경우
- 방화벽에서 443 포트가 열려있는지 확인
- 서버가 실행 중인지 확인
- hosts 파일이 올바르게 설정되었는지 확인
