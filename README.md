# air-alert

에어코리아 OpenAPI에서 미세먼지 정보를 조회하고 Mattermost로 알림을 전송하는 Java 애플리케이션.

## 요구사항

- Java 8
- Maven 3.6+

## 설정

설정값은 **`config.properties` 파일이 있으면 파일을, 없으면 환경변수**를 사용합니다.

### 로컬 실행 (파일 방식)

```bash
cp config.properties.example config.properties
# config.properties를 열어 실제 값을 입력
mvn compile exec:java
```

### 환경변수 방식

| 환경변수 | 설명 |
| --- | --- |
| `AIRKOREA_SERVICE_KEY` | 에어코리아 OpenAPI 서비스 키 |
| `AIRKOREA_STATION_NAME` | 측정소 이름 (예: `강남대로`) |
| `MATTERMOST_WEBHOOK_URL` | Mattermost Incoming Webhook URL |

## GitHub Actions 자동 실행

`.github/workflows/air-alert.yml`에 정의된 cron 스케줄로 평일 자동 실행됩니다.

### 스케줄 (KST 기준)

- 월~금 06:10
- 월~금 11:40
- 월~금 17:10

> GitHub Actions 무료 cron은 최대 15-20분 지연될 수 있어, 요청한 시각(06:30 / 12:00 / 17:30)보다 20분 앞당겨 설정됨.

### Secrets 등록 (최초 1회)

GitHub repo → **Settings → Secrets and variables → Actions → New repository secret** 에서 다음 3개 등록:

- `AIRKOREA_SERVICE_KEY`
- `AIRKOREA_STATION_NAME`
- `MATTERMOST_WEBHOOK_URL`

### 수동 실행

**Actions 탭 → air-alert 워크플로 → Run workflow** 버튼으로 즉시 실행 가능.
