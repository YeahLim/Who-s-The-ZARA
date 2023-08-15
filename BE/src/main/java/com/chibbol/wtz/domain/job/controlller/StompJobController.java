package com.chibbol.wtz.domain.job.controlller;

import com.chibbol.wtz.domain.job.dto.TargetUserDTO;
import com.chibbol.wtz.domain.job.entity.RoomUserJob;
import com.chibbol.wtz.domain.job.repository.RoomUserJobRedisRepository;
import com.chibbol.wtz.domain.record.entiry.UserAbilityRecord;
import com.chibbol.wtz.domain.record.repository.UserAbilityRecordRedisRepository;
import com.chibbol.wtz.global.stomp.dto.DataDTO;
import com.chibbol.wtz.global.stomp.service.RedisPublisher;
import com.chibbol.wtz.global.timer.service.NewTimerService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

import java.util.List;


@Slf4j
@Controller
@RequiredArgsConstructor
public class StompJobController {
    private final UserAbilityRecordRedisRepository userAbilityRecordRepository;
    private final RedisPublisher publisher;
    private final NewTimerService newTimerService;
    private final RoomUserJobRedisRepository roomUserJobRedisRepository;
    private final ChannelTopic gameTopic;

    @Operation(summary = "능력 사용 정보")
    @MessageMapping("/game/{gameCode}/ability")
    public void recordAbility(@DestinationVariable String gameCode, TargetUserDTO targetUserDTO) {
//        stompService.addTopic(gameCode);
        log.info("GameCode: " + gameCode + ", DTO: " + targetUserDTO.toString());

        // userSeq, targetUserSeq 가 방에 참여한 유저가 아닐 경우 저장하지 않음
        List<RoomUserJob> roomUserJobs = roomUserJobRedisRepository.findAllByGameCode(gameCode);
        boolean userIn = false;
        boolean targetUserIn = false;
        for(RoomUserJob roomUserJob : roomUserJobs) {
            if(roomUserJob.getUserSeq().equals(targetUserDTO.getUserSeq())) {
                userIn = true;
            }
            if(roomUserJob.getUserSeq().equals(targetUserDTO.getTargetUserSeq())) {
                targetUserIn = true;
            }
        }

        if(!(userIn && targetUserIn)) {
            return;
        }

        // 능력 사용 저장
        userAbilityRecordRepository.save(UserAbilityRecord.builder()
                .gameCode(gameCode)
                .turn(newTimerService.getTimerInfo(gameCode).getTurn())
                .userSeq(targetUserDTO.getUserSeq())
                .targetUserSeq(targetUserDTO.getTargetUserSeq())
                .build());

        // 자라이면 targetUserSeq 전송
        RoomUserJob userJob = roomUserJobRedisRepository.findByGameCodeAndUserSeq(gameCode, targetUserDTO.getUserSeq());
            if(userJob.getJobSeq() == 2){ // 자라 jobSeq == 2
                publisher.publish(gameTopic,
                        DataDTO.builder()
                                .type("ABILITY")
                                .code(gameCode)
                                .data(targetUserDTO.getTargetUserSeq())
                                .build());
            }
    }
}
