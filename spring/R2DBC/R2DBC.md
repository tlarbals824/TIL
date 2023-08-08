# R2DBC

## R2DBC란?

* R2DBC는 관계형 데이터베이스에 리액티브 프로그래밍 API를 제공하기 위한 개방형 사양입니다.
* 드라이버 벤더가 구현합니다.
* 클라이언트가 사용하기 위한 SPI 입니다.

> R2DBC 지원 관계형 데이터베이스
> 1. H2
> 2. MySQL
> 3. jasync-sql MySQL
> 4. MariaDB
> 5. Microsoft SQL Server
> 6. PostgreSQL
> 7. Oracle
> 8. Cloud Spanner(Google Cloud Platform)

## Spring Data R2DBC 란?

* Spring Data R2DBC는 R2DBC 기반 Repository를 좀 더 쉽게 구현하게 해주는 Spring Data Family의 프로젝트입니다.
* Spring Data R2DBC는 Spring JPA와 같은 추상화 기법이 적용되었습니다.
* Spring Data R2DBC는 JPA와 같은 ORM 프레임워크에서 제공하는 캐싱(caching), 지연로딩, 기타 ORM 프레임워크에서 가지고 있는 특징들이 제거되어 단순하고 심플한 방법으로 사용할 수 있습니다.
* Spring Data R2DBC는 데이터 엑세스 계층의 보일러 플레이트 코드의 양을 대폭 줄여줍니다.

## Spring Data R2DBC 도메인 엔티티 클래스 매핑

~~~java
@Table("POST") // 테이블 매핑
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Post extends BaseEntity {

    @Id // 기본키 지정
    private Long id;
    private String title;
    private String description;
    private Long viewCount;
    private Long userId;

    @PersistenceCreator // SELECT 쿼리를 통해 조회한 결과를 엔티티 객체로 매핑할 때 사용
    public Post(Long id, String title, String description, Long viewCount, Long userId) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.viewCount = viewCount;
        this.userId = userId;
    }
}
~~~

* @Table : 테이블 이름을 지정합니다.
* @Id : 기본키를 지정합니다.
* @PersistenceCreator : SELECT 쿼리를 통해 조회한 결과를 엔티티 객체로 매핑할 때 사용합니다.

## Spring Data R2DBC Repository 인터페이스

Spring Data JPA에서의 방식과 동일하게 인터페이스를 정의하면 됩니다. 이때 사용하는 인터페이스는 R2dbcRepository<Entity, IdType>입니다.

~~~java
public interface PostR2dbcRepository extends R2dbcRepository<Post, Long> {
    Flux<Post> findAllByUserId(Long userId);
}
~~~

> 참고 :
>
> 스프링으로 시작하는 리액티브 프로그래밍(https://product.kyobobook.co.kr/detail/S000201399476)