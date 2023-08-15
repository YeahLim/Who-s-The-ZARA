package com.chibbol.wtz.domain.record.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserAbilityRecordDTO {
    private String roomSeq;
    private int turn;
    private Long userSeq;
    private Long targetUserSeq;
}
