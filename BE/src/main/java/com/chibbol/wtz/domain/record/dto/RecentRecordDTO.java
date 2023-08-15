package com.chibbol.wtz.domain.record.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class RecentRecordDTO {
    private Long jobSeq;
    private String gameCode;
    private boolean win;
    private LocalDateTime startAt;
    private LocalDateTime endAt;

    @Builder
    public RecentRecordDTO(Long jobSeq, String gameCode, boolean win, LocalDateTime startAt, LocalDateTime endAt) {
        this.jobSeq = jobSeq;
        this.gameCode = gameCode;
        this.win = win;
        this.startAt = startAt;
        this.endAt = endAt;
    }
}
