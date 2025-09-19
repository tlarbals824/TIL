# 부록 C, DB로 분산 잠금 구현하기

## 잠금 정보 저장 테이블

```sql
create table dist_lock
(
    name varchar(100) not null comment '락 이름',
    owner varchar(100) comment '락 소유자',
    expiry datetime comment '락 만료 시간',
    primary key (name)
)
```

위 테이블을 통해 락을 관리할 수 있다.

예를들면 select ~~ for update를 통해 배타락을 걸고 해당 행에 대해서 만료 시간이 지났으면 갱신해서 사용하고 지나지 않았으면 에러를 발생하도록 하면 분산 락을 구현할 수 있다.


## DB 잠금 구현

락의 정보는 다음 클래스를 통해 관리한다.

```kotlin
package distlock

import java.sql.*
import java.time.Duration
import java.time.LocalDateTime

// 락 소유자
data class LockOwner(
    val owner: String?,
    val expiry: LocalDateTime?
) {
    fun isOwnedBy(owner: String): Boolean =
        this.owner != null && this.owner == owner

    fun isExpired(): Boolean =
        this.expiry != null && expiry.isBefore(LocalDateTime.now())
}

// 분산 락 클래스
class DistLock(
    private val dataSource: javax.sql.DataSource
) {

    /**
     * 락 획득 시도
     */
    fun tryLock(name: String, owner: String, duration: Duration): Boolean {
        var conn: Connection? = null
        var owned = false

        try {
            conn = dataSource.connection
            conn.autoCommit = false

            val lockOwner = getLockOwner(conn, name)

            owned = when {
                lockOwner == null || lockOwner.owner == null -> {
                    insertLockOwner(conn, name, owner, duration)
                    true
                }
                lockOwner.isOwnedBy(owner) -> {
                    updateLockOwner(conn, name, owner, duration)
                    true
                }
                lockOwner.isExpired() -> {
                    updateLockOwner(conn, name, owner, duration)
                    true
                }
                else -> false // 다른 소유자 && 만료 안됨
            }

            conn.commit()
        } catch (e: Exception) {
            rollback(conn)
            owned = false
        } finally {
            close(conn)
        }
        return owned
    }

    /**
     * 락 해제
     */
    fun unlock(name: String, owner: String) {
        var conn: Connection? = null
        try {
            conn = dataSource.connection
            conn.autoCommit = false

            val lockOwner = getLockOwner(conn, name)
            if (lockOwner == null || !lockOwner.isOwnedBy(owner)) {
                throw IllegalStateException("no lock owner")
            }
            if (lockOwner.isExpired()) {
                throw IllegalStateException("lock is expired")
            }

            clearOwner(conn, name)
            conn.commit()
        } catch (e: SQLException) {
            rollback(conn)
            throw RuntimeException("fail to unlock: ${e.message}", e)
        } finally {
            close(conn)
        }
    }

    /**
     * 현재 락 소유자 조회
     */
    private fun getLockOwner(conn: Connection, name: String): LockOwner? {
        val sql = "select * from dist_lock where name = ? for update"
        conn.prepareStatement(sql).use { pstmt ->
            pstmt.setString(1, name)
            pstmt.executeQuery().use { rs ->
                if (rs.next()) {
                    return LockOwner(
                        rs.getString("owner"),
                        rs.getTimestamp("expiry")?.toLocalDateTime()
                    )
                }
            }
        }
        return null
    }

    /**
     * 락 신규 생성
     */
    private fun insertLockOwner(conn: Connection, name: String, owner: String, duration: Duration) {
        val sql = "insert into dist_lock values (?, ?, ?)"
        conn.prepareStatement(sql).use { pstmt ->
            pstmt.setString(1, name)
            pstmt.setString(2, owner)
            pstmt.setTimestamp(3, getExpiry(duration))
            pstmt.executeUpdate()
        }
    }

    /**
     * 락 연장 / 소유자 변경
     */
    private fun updateLockOwner(conn: Connection, name: String, owner: String, duration: Duration) {
        val sql = "update dist_lock set owner = ?, expiry = ? where name = ?"
        conn.prepareStatement(sql).use { pstmt ->
            pstmt.setString(1, owner)
            pstmt.setTimestamp(2, getExpiry(duration))
            pstmt.setString(3, name)
            pstmt.executeUpdate()
        }
    }

    /**
     * 락 해제 (owner, expiry null 처리)
     */
    private fun clearOwner(conn: Connection, name: String) {
        val sql = "update dist_lock set owner = null, expiry = null where name = ?"
        conn.prepareStatement(sql).use { pstmt ->
            pstmt.setString(1, name)
            pstmt.executeUpdate()
        }
    }

    /**
     * 만료 시간 계산
     */
    private fun getExpiry(duration: Duration): Timestamp =
        Timestamp.valueOf(LocalDateTime.now().plusSeconds(duration.seconds))

    /**
     * 롤백 처리
     */
    private fun rollback(conn: Connection?) {
        try {
            conn?.rollback()
        } catch (_: SQLException) {
        }
    }

    /**
     * 연결 종료 처리
     */
    private fun close(conn: Connection?) {
        if (conn != null) {
            try {
                conn.autoCommit = false
            } catch (_: SQLException) {
            }
            try {
                conn.close()
            } catch (_: SQLException) {
            }
        }
    }
}
```


### 락 획득

1. DB에서 해당 리소스의 락 정보를 select ... for update로 조회
2. 세 가지 경우에 따라 동작
    * 아직 락이 없음 → insert로 새 락 생성 → 성공
    * 현재 소유자가 나 자신 → update로 만료 시간 연장 → 성공
    * 다른 소유자지만 만료됨 → update로 소유자를 나로 변경 → 성공
    * 다른 소유자 & 아직 만료 안 됨 → 실패
3. 트랜잭션을 commit → 락 소유 확정