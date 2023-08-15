package com.chibbol.wtz.domain;

import com.chibbol.wtz.domain.job.entity.RoomUserJob;
import com.chibbol.wtz.domain.job.repository.RoomUserJobRedisRepository;
import com.chibbol.wtz.domain.record.entiry.UserAbilityRecord;
import com.chibbol.wtz.domain.record.repository.UserAbilityRecordRedisRepository;
import com.chibbol.wtz.domain.vote.dto.VoteDTO;
import com.chibbol.wtz.domain.vote.entity.Vote;
import com.chibbol.wtz.domain.vote.repository.VoteRedisRepository;
import com.chibbol.wtz.domain.vote.service.VoteService;
import com.chibbol.wtz.global.stomp.dto.DataDTO;
import com.chibbol.wtz.global.stomp.service.RedisPublisher;
import com.chibbol.wtz.global.timer.dto.TimerDTO;
import com.chibbol.wtz.global.timer.entity.Timer;
import com.chibbol.wtz.global.timer.repository.TimerRedisRepository;
import com.chibbol.wtz.global.timer.service.NewTimerService;
import com.chibbol.wtz.global.timer.service.StompTimerService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.AllArgsConstructor;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@AllArgsConstructor
@RequestMapping("/api/v1/test")
public class TestController {

    private final VoteRedisRepository voteRedisRepository;
    private final RoomUserJobRedisRepository roomUserJobRedisRepository;
    private final UserAbilityRecordRedisRepository userAbilityRecordRedisRepository;
    private final NewTimerService newTimerService;
    private final TimerRedisRepository timerRedisRepository;
    private final StompTimerService stompTimerService;
    private final VoteService voteService;
    private final ChannelTopic gameTopic;
    private final RedisPublisher publisher;

    @Operation(summary = "더미 데이터 추가(투표, 능력 모두)")
    @PostMapping("/dummyData")
    public ResponseEntity<Void> test(@RequestParam String gameCode) {
        for(Long i = 24L; i <= 31L; i++) {
            roomUserJobRedisRepository.save(RoomUserJob.builder().userSeq(i).gameCode(gameCode).build());
        }

        for (int turn = 1; turn <= 10; turn++) {
            for (Long i = 24L; i <= 31L; i++) {
                int target = ((int)(Math.random() * 8)) + 1;
                Long targetL = (long) target;
                voteRedisRepository.save(Vote.builder().gameCode(gameCode).turn(turn).userSeq(i).targetUserSeq(targetL).build());
            }
        }

        for (int turn = 1; turn <= 10; turn++) {
            for (Long i = 24L; i <= 31L; i++) {
                int target = ((int)(Math.random() * 8)) + 1;
                Long targetL = (long) target;
                userAbilityRecordRedisRepository.save(UserAbilityRecord.builder().gameCode(gameCode).turn(turn).userSeq(i).targetUserSeq(targetL).build());
            }
        }

        return ResponseEntity.ok().build();
    }

    @Operation(summary = "더미데이터 추가(타이머 기본 정보만)")
    @PostMapping("/dummyData2")
    public ResponseEntity<Void> test1(@RequestParam String gameCode) {
        voteRedisRepository.deleteAllByGameCode(gameCode);
        userAbilityRecordRedisRepository.deleteAllByGameCode(gameCode);

        for(Long i = 24L; i <= 31L; i++) {
            roomUserJobRedisRepository.save(RoomUserJob.builder().userSeq(i).gameCode(gameCode).build());
        }

        return ResponseEntity.ok().build();
    }

    @Operation(summary = "타이머 초기화")
    @PostMapping("/resetTimer")
    public ResponseEntity<Void> test2(@RequestParam String gameCode) {
        timerRedisRepository.deleteGameTimer(gameCode);
        Timer timer = newTimerService.createRoomTimer(gameCode);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "현재 타이머 전송")
    @PostMapping("/nowTimer")
    public ResponseEntity<Void> test3(@RequestParam String gameCode) {
        Timer timer = timerRedisRepository.getGameTimerInfo(gameCode);
        if(timer != null) {
            stompTimerService.sendToClient("GAME_TIMER", gameCode, TimerDTO.builder().type(timer.getTimerType()).time(timer.getRemainingTime()).build());
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "다음 타이머로 변경")
    @PostMapping("/nextTimer")
    public ResponseEntity<Void> test4(@RequestParam String gameCode) {
        Timer timer = timerRedisRepository.getGameTimerInfo(gameCode);
        if(timer != null) {
            newTimerService.timerTypeChange(gameCode, timer);
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "타이머 종료 알림")
    @PostMapping("/timerEnd")
    public ResponseEntity<Void> timerEnd(@RequestParam String gameCode, @RequestParam Long userSeq) {
        Timer timer = timerRedisRepository.getGameTimerInfo(gameCode);
        if(timer != null) {
            newTimerService.timerEndUser(gameCode, userSeq);
        }
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "투표중(gamecode, userSeq, targetUserSeq 만 작성하면됨)")
    @PostMapping("/voting")
    public ResponseEntity<Void> voting(@RequestBody VoteDTO voteDTO) {
        Timer timer = timerRedisRepository.getGameTimerInfo(voteDTO.getGameCode());
        if(timer == null) {
            return ResponseEntity.notFound().build();
        }
        voteDTO.setTurn(timer.getTurn());
        voteService.vote(voteDTO);

        // 투표 현황 리스트로 만들어서 전달
        publisher.publish(gameTopic,
                DataDTO.builder()
                        .type("VOTE")
                        .code(voteDTO.getGameCode())
                        .data(voteService.getRealTimeVoteResultWithJob(voteDTO.getGameCode(), timer.getTurn()))
                        .build());

        return ResponseEntity.ok().build();
    }

    @Operation(summary = "타이머 감소")
    @PostMapping("/decreaseTimer")
    public ResponseEntity<Void> decreaseTimer(@RequestParam String gameCode, @RequestParam Long userSeq) {
        Timer timer = timerRedisRepository.getGameTimerInfo(gameCode);
        if(timer != null) {
            newTimerService.timerDecreaseUser(gameCode, userSeq);
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/ability")
    public ResponseEntity<Void> useAbility(@RequestParam String gameCode, @RequestParam int turn, @RequestParam Long userSeq, @RequestParam Long targetUserSeq) {
        userAbilityRecordRedisRepository.save(UserAbilityRecord.builder().gameCode(gameCode).turn(turn).userSeq(userSeq).targetUserSeq(targetUserSeq).build());
        return ResponseEntity.ok().build();
    }
}
