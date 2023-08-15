package com.chibbol.wtz.domain.record.entiry;

import com.chibbol.wtz.domain.job.entity.Job;
import com.chibbol.wtz.domain.user.entity.User;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
public class UserAbilityLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userAbilitySeq;

    @JoinColumn(name = "user_seq", nullable = false)
    @ManyToOne
    private User user;

    @JoinColumn(name = "job_seq", nullable = false)
    @ManyToOne
    private Job job;

    @Column
    private String gameCode;

    @Column
    private boolean result;

    @Column
    @ColumnDefault("0")
    private int abilitySuccessCount;

    @Column
    private LocalDateTime startAt;

    @Column
    private LocalDateTime endAt;

    @Builder
    public UserAbilityLog(String gameCode, User user, Job job, boolean result, int abilitySuccessCount, LocalDateTime startAt, LocalDateTime endAt) {
        this.gameCode = gameCode;
        this.user = user;
        this.job = job;
        this.result = result;
        this.abilitySuccessCount = abilitySuccessCount;
        this.startAt = startAt;
        this.endAt = endAt;
    }

    public void addAbilitySuccessCount() {
        this.abilitySuccessCount++;
    }
}
