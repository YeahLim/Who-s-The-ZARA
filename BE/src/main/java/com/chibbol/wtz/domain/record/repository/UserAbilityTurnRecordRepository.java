package com.chibbol.wtz.domain.record.repository;

import com.chibbol.wtz.domain.record.entiry.UserAbilityTurnRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserAbilityTurnRecordRepository extends JpaRepository<UserAbilityTurnRecord, Long> {
}
