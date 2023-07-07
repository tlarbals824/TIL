# R2DBC

## R2DBC란?

R2DBC는 관계형 데이터베이스에 리액티브 프로그래밍 API를 제공하기 위한 사양이다.
간단한 예로는 jdbc를 들 수 있다.

하지만 R2DBC는 ORM이 아니기 때문에 jpa에서 자동으로 테이블을 생성해주는 과정이 없다.
사용자가 계획한 테이블 구조에 대해서 spring의 resource 패키지에 schema.sql을 설정하여 관리를 한다. 관리를 flyway를 통해 진행하는것을 추천한다.

## R2DBC 설정

### gradle

gradle 설정은 spring-data-r2dbc 의존성과 사용하는 db의 r2dbc 전용 드라이버를 추가하면 된다.

~~~
// r2dbc
implementation 'org.springframework.boot:spring-boot-starter-data-r2dbc'
// r2dbc mysql
implementation("io.asyncer:r2dbc-mysql:0.9.2")   
~~~

### Domain

일단 USER 테이블에 대해서 데이터를 가져온다고 가정하고 설정하면 다음과 같다.

spring jpa에서 제공하는 @MappedSuperClass는 없어보인다. 또한 r2dbc와 jpa와의 가장 큰 차이점으로는 @Entity 애노테이션에 대한 유무라고 생각한다. r2dbc는 orm이 아니기때문에 primary key에 대한 설정말고는 딱히 해줄것이 없다.

@PersistenceCreator는 r2dbc를 이용하여 db에서 엔티티를 서버로 가져오는 과정에서 해당 엔티티 정보로 인스턴스를 만들떄 사용하는 생성자를 설정하는 애노테이션이다.

~~~java
@Table("USER")  // 엔티티의 목적 테이블 설정
@Getter
@AllArgsConstructor
public class User {
    @Id // primary key 설정
    private Long id;
    private String username;
    private String password;
    
    @PersistenceCreator // db -> 서버로 엔티티를 가져오는 과정에서 인스턴스 변환시 사용하는 생성자
    public User(Long id, String username, String password) {
        this.id = id;
        this.username = username;
        this.password = password;
    }
}
~~~

### Repository

r2dbc의 repository 생성은 jpa의 repository 생성과 똑같다. 즉 jpa의 방식과 같이 메서드의 이름을 가지고 쿼리를 생성해준다.

하지만 r2dbc는 jpa와 다르게 Page에 대한 공식지원이 없다. 이에 대해서는 더 찾아봐야할것같다.

~~~java
public interface UserR2dbcRepository extends R2dbcRepository<User, Long> {
    Mono<Boolean> existsByUsername(String username);
}
~~~