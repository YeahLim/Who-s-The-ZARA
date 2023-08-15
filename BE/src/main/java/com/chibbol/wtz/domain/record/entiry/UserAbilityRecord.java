package com.chibbol.wtz.domain.record.entiry;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

import java.time.LocalDateTime;

@Getter
@ToString
@RedisHash("userAbilityRecord")
public class UserAbilityRecord {
    private String gameCode;
    private int turn;
    @Id
    private Long userSeq;
    private Long targetUserSeq;
    private boolean success;
    private LocalDateTime usedAt;

    @Builder
    public UserAbilityRecord(String gameCode, int turn, Long userSeq, Long targetUserSeq) {
        this.gameCode = gameCode;
        this.turn = turn;
        this.userSeq = userSeq;
        this.targetUserSeq = targetUserSeq;
        this.success = false;
        this.usedAt = LocalDateTime.now();
    }

    public UserAbilityRecord success() {
        this.success = true;
        return this;
    }
}
