package com.chibbol.wtz.domain.record.entiry;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@Getter
@NoArgsConstructor
public class VoteTurnRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long voteTurnRecordSeq;

    @Column
    private String gameCode;

    @Column
    private int turn;

    @Column
    private Long userSeq;

    @Column
    private Long targetUserSeq;

    @Builder
    public VoteTurnRecord(String gameCode, int turn, Long userSeq, Long targetUserSeq) {
        this.gameCode = gameCode;
        this.turn = turn;
        this.userSeq = userSeq;
        this.targetUserSeq = targetUserSeq;
    }
}
