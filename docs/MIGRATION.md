# 기존 MySQL 데이터가 있는 경우

이 변경은 기존 운영 DB에 자동으로 적용하거나 기존 배포 서비스를 재배포하지 않습니다.
로컬 `demo`/테스트는 새 H2 스키마에서 검증합니다. 기존 MySQL에는 다음 사항을 확인한 뒤 적용해야 합니다.

## 적용 순서

1. 데이터베이스를 백업하고 복제본에서 먼저 검증합니다.
2. 사용자·관리자 계정의 역할(`users.user_type`)을 확인합니다. 회원가입으로 ADMIN을 지정할 수 없습니다.
3. 새 코드가 추가하는 컬럼을 확인합니다: `seat.held_by_user_id`, `booking.expires_at`, `queue_entry.user_id`, `queue_entry.performance_id`.
4. `booking_seats`의 `seat_id` 단독 UNIQUE 인덱스를 제거해야 합니다. 취소한 예매의 이력을 보존하면서 같은 좌석을 다른 예매에 연결하려면 단독 UNIQUE가 없어야 합니다.
5. `payment.booking_id`는 예매당 결제 하나만 허용합니다. 기존 중복 결제 레코드를 확인·정리한 뒤 UNIQUE를 적용합니다.
6. 기존 HELD 좌석과 PENDING 예매에는 새 소유자/만료 정보가 없습니다. 이 레코드를 무조건 유효한 예매로 승격하지 말고, 서비스 중단 시간에 별도 정책으로 만료 처리합니다.
7. 적용 후 일반 회원·관리자 로그인, 입장 대기, 좌석 선점·취소·모의 결제를 확인합니다.

## 좌석 이력 인덱스 확인 예시

```sql
SHOW INDEX FROM booking_seats;
```

`seat_id`만 포함하고 `Non_unique=0`인 인덱스가 있다면, 외래 키 유지에 필요한 일반 인덱스를 확보한 뒤 해당 UNIQUE 인덱스를 제거합니다.

```sql
-- 아래 이름은 실제 인덱스 목록을 확인한 뒤 지정합니다.
ALTER TABLE booking_seats ADD INDEX idx_booking_seats_seat_id (seat_id);
ALTER TABLE booking_seats DROP INDEX `실제_seat_id_단독_unique_인덱스명`;
```

인덱스가 이미 있으면 중복 생성하지 않습니다. 이 예시는 실제 MySQL에서 실행 검증한 마이그레이션 스크립트가 아닙니다.
Hibernate의 `ddl-auto=update`만으로 기존 UNIQUE 인덱스가 자동 삭제된다고 가정하면 안 됩니다.

## 배포 설정

기존 `application.properties`가 참조하는 MySQL 환경변수를 사용합니다. 비밀번호를 소스에 넣지 않습니다.
TLS 종료 프록시 환경에서는 실제 HTTPS·세션 쿠키 전달을 확인하고 `SERVER_SERVLET_SESSION_COOKIE_SECURE=true`를 설정합니다.
`demo` 프로필은 알려진 데모 계정을 생성하고 메모리 DB를 초기화하므로 운영 데이터가 있는 서비스에 사용하지 않습니다.
