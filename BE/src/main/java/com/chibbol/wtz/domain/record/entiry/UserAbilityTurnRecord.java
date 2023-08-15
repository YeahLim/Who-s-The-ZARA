package com.chibbol.wtz.domain.record.entiry;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
public class UserAbilityTurnRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userAbilityTurnRecordSeq;

    @Column
    int turn;

    @Column
    private String gameCode;

    @Column
    private Long userSeq;

    @Column
    private Long targetUserSeq;

    @Column
    private boolean success;

    @Column
    private LocalDateTime usedAt;

    @Builder
    public UserAbilityTurnRecord(int turn, String gameCode, Long userSeq, Long targetUserSeq, boolean success, LocalDateTime usedAt) {
        this.turn = turn;
        this.gameCode = gameCode;
        this.userSeq = userSeq;
        this.targetUserSeq = targetUserSeq;
        this.success = success;
        this.usedAt = usedAt;
    }
}
