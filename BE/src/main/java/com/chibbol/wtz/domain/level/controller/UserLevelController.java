package com.chibbol.wtz.domain.level.controller;

import com.chibbol.wtz.domain.level.dto.LevelResultDTO;
import com.chibbol.wtz.domain.level.dto.UserLevelDataDTO;
import com.chibbol.wtz.domain.level.entity.UserExpValue;
import com.chibbol.wtz.domain.level.entity.UserLevel;
import com.chibbol.wtz.domain.level.repository.UserLevelRepository;
import com.chibbol.wtz.domain.level.service.UserLevelService;
import com.chibbol.wtz.domain.level.service.WeightMappingService;
import com.chibbol.wtz.domain.record.entiry.UserAbilityLog;
import com.chibbol.wtz.domain.record.repository.UserAbilityLogRepository;
import com.chibbol.wtz.domain.user.entity.User;
import com.chibbol.wtz.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/level")
@RequiredArgsConstructor
public class UserLevelController {
    private final UserService userService;
    private final UserLevelService userLevelService;
    private final UserAbilityLogRepository userAbilityLogRepository;
    private final WeightMappingService weightMappingService;
    private final UserLevelRepository userLevelRepository;

    @GetMapping
    public ResponseEntity<UserLevelDataDTO> getUserLevelData(){
        User user = userService.getLoginUser();
        UserLevel userLevel = userLevelRepository.findByUserUserSeq(user.getUserSeq())
                .orElse(UserLevel.builder().user(user).level(1).exp(0L).build());;
        UserLevelDataDTO data = UserLevelDataDTO.builder()
                .level(userLevel.getLevel())
                .exp(userLevel.getExp())
                .maxExp(userLevelService.getLevelUpExp(userLevel.getLevel()))
                .build();

        return ResponseEntity.ok(data);
    }

    @GetMapping("/{gameCode}")
    public ResponseEntity<UserExpValue> getExpValueInGameCode(@PathVariable String gameCode) {
        UserExpValue userExpValue = userLevelService.getExpValueInGameCode(gameCode);

        return ResponseEntity.ok(userExpValue);
    }

    @PatchMapping("/{gameCode}")
    public ResponseEntity<Void> updateUserLevel(@PathVariable String gameCode){
        List<UserAbilityLog> userAbilityLogs = userAbilityLogRepository.findAllByGameCode(gameCode);
        List<LevelResultDTO> list = userLevelService.updateExp(userAbilityLogs);

        for(LevelResultDTO temp : list){
            log.info(temp.getUserSeq()+" "+temp.getLevel()+" "+temp.getCurrentExp()+" "+temp.getMaxExp()+" "+temp.getExpValue());
        }

        return ResponseEntity.ok().build();
    }

    @GetMapping("/weight")
    public ResponseEntity<Void> getWeightList(){
        Map<Long, Double> map = weightMappingService.getJobWeightMap();

        for(Map.Entry<Long, Double> entry: map.entrySet()){
            log.info(entry.getKey()+" "+entry.getValue());
        }

        return ResponseEntity.ok().build();
    }
}
