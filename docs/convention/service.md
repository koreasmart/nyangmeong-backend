# 냥멍케어 백엔드 컨벤션: Service

`AGENTS.md`에서 이어지는 문서다.

## Service

- `@Transactional`은 클래스가 아니라 메서드에 붙인다. 데이터를 바꾸는 메서드에는 `@Transactional`, 조회만 하는 메서드에는 `@Transactional(readOnly = true)`를 붙인다. Service의 `public` 메서드에는 둘 중 하나가 반드시 있어야 한다. 빠뜨리면 ArchUnit 테스트(`ArchitectureTest`)가 실패한다. 트랜잭션이 필요 없는 `public` 메서드는 이 테스트의 예외 목록에 추가한다.

```java
@Service @RequiredArgsConstructor
public class PostService {

    @Transactional(readOnly = true)
    public PostResponse getPost(Long pstSn) { ... }

    @Transactional
    public PostResponse create(Long memberId, PostCreateRequest req) { ... }
}
```

- 권한 검사는 데이터를 바꾸거나 돌려주기 전에 한다.
  - 반려동물 권한은 `PetAuthorizer`로 메서드 첫 줄에서 검사한다.
  - 작성자 권한은 대상을 조회한 바로 다음에 `OwnerAuthorizer.checkOwner`로 검사한다.

- 오류는 `ErrorCode`에 추가하고 `CustomException`을 던진다(`throw new CustomException(ErrorCode.PET_NOT_FOUND)`). 도메인별 예외 클래스를 만들지 않고 `BaseException`을 직접 던지지도 않는다. 메시지는 화면설계서 문구를 그대로 쓴다.
- 암호화 컬럼은 `FieldCryptoService`의 `encrypt()`·`decrypt()`·`hmac()`을 서비스에서 호출한다. XML에는 암호문이 들어가고 암호문이 나온다.
- 이메일과 전화번호는 암호화하거나 비교하기 전에 정규화한다(이메일은 `normalizeEmail()`: 앞뒤 공백 제거와 소문자 변환, 전화번호는 `PhoneNumberNormalizer.normalize()`).
- 파일은 `FileStore`를 쓴다. 크기와 확장자 검사는 `FileStore`가 저장할 때 한다. 도메인 코드에서 다시 검사하지 않는다.
  - 파일 하나에 10MB 이하, 확장자는 jpg·jpeg·png·webp다. 검진 첨부만 pdf도 된다.
  - 어기면 `FILE_TOO_LARGE`, `UNSUPPORTED_FILE_TYPE`(둘 다 400)이 나간다.
  - 한 번에 올릴 수 있는 파일 개수는 화면마다 다르므로 기능명세서를 따른다.
- 알림은 `NotificationEvent`, 메일은 `MailEvent`를 발행한다. 알림 테이블에 직접 insert하지 않는다.
- 이벤트를 받아 알림을 저장하는 service는 `notification` 도메인에 있다. 이벤트 클래스는 `global/notification`에 있다.
- 이벤트는 `@Transactional` 메서드 안에서 `EventPublisher.publish()`로만 발행한다. `ApplicationEventPublisher`를 직접 쓰지 않는다. 직접 쓰면 트랜잭션 밖에서 발행한 이벤트를 리스너가 받지 않고 버리며 오류도 나지 않는다.
- `EventPublisher.publish()`는 트랜잭션이 없으면 예외를 던진다. 같은 클래스의 `@Transactional` 메서드를 `this.메서드()`로 부른 경우에도 트랜잭션이 걸리지 않으므로 예외가 난다. 이때는 호출하는 메서드에 `@Transactional`을 붙인다.

```java
@Transactional
public void invite(Long memberId, InviteCreateRequest req) {
    // ... 초대 저장 ...
    eventPublisher.publish(new NotificationEvent(...));
}
// NG: notificationMapper.insert(...)  -- 알림 직접 insert 금지
```
