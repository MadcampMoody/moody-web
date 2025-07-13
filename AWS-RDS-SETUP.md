# AWS RDS 설정 가이드

## 1. AWS RDS 정보 확인
AWS RDS 콘솔에서 다음 정보를 확인하세요:
- **엔드포인트**: `your-db-instance.xxxxx.region.rds.amazonaws.com`
- **포트**: 3306 (MySQL 기본)
- **데이터베이스 이름**: 생성 시 설정한 DB 이름
- **사용자명**: 마스터 사용자명
- **비밀번호**: 설정한 비밀번호

## 2. 보안 그룹 설정
RDS 보안 그룹에서 다음 인바운드 규칙을 추가하세요:
- **타입**: MySQL/Aurora
- **포트**: 3306
- **소스**: 0.0.0.0/0 (개발용) 또는 특정 IP

## 3. application.properties 수정
`application-aws.properties` 파일의 다음 항목을 실제 값으로 수정하세요:

```properties
spring.datasource.url=jdbc:mysql://YOUR_RDS_ENDPOINT:3306/YOUR_DATABASE_NAME?useSSL=true&serverTimezone=UTC&allowPublicKeyRetrieval=true
spring.datasource.username=YOUR_RDS_USERNAME
spring.datasource.password=YOUR_RDS_PASSWORD
```

## 4. 데이터베이스 마이그레이션
1. MySQL Workbench나 CLI를 사용하여 RDS에 연결
2. `migration-script.sql` 파일을 실행하여 테이블 생성
3. 기존 데이터가 있다면 export/import 진행

## 5. 애플리케이션 실행
```bash
# AWS 프로파일로 실행
./gradlew bootRun --args='--spring.profiles.active=aws'

# 또는 application.properties를 직접 수정 후 실행
./gradlew bootRun
```

## 6. 연결 테스트
애플리케이션 실행 후 로그에서 다음을 확인:
- 데이터베이스 연결 성공 메시지
- 테이블 생성/업데이트 로그
- 에러 메시지가 없는지 확인

## 7. 문제 해결
### 연결 실패 시 확인사항:
- 보안 그룹 설정 확인
- RDS 엔드포인트 정확성 확인
- 사용자명/비밀번호 확인
- 데이터베이스 이름 확인

### 일반적인 에러:
- `Communications link failure`: 보안 그룹 또는 네트워크 문제
- `Access denied`: 사용자명/비밀번호 오류
- `Unknown database`: 데이터베이스 이름 오류 