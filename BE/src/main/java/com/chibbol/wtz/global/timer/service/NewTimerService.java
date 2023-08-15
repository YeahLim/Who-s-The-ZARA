package com.chibbol.wtz.global.timer.service;

import com.chibbol.wtz.domain.job.entity.RoomUserJob;
import com.chibbol.wtz.domain.job.repository.RoomUserJobRedisRepository;
import com.chibbol.wtz.domain.job.service.JobService;
import com.chibbol.wtz.domain.level.service.UserLevelService;
import com.chibbol.wtz.domain.point.service.PointService;
import com.chibbol.wtz.domain.record.entiry.UserAbilityLog;
import com.chibbol.wtz.domain.record.entiry.UserAbilityRecord;
import com.chibbol.wtz.domain.record.repository.UserAbilityLogRepository;
import com.chibbol.wtz.domain.record.repository.UserAbilityRecordRedisRepository;
import com.chibbol.wtz.domain.room.dto.CurrentSeatsDTO;
import com.chibbol.wtz.domain.room.entity.Room;
import com.chibbol.wtz.domain.room.repository.GameRepository;
import com.chibbol.wtz.domain.room.service.RoomEnterInfoRedisService;
import com.chibbol.wtz.domain.shop.entity.UserItem;
import com.chibbol.wtz.domain.shop.repository.UserItemRepository;
import com.chibbol.wtz.domain.user.repository.UserRepository;
import com.chibbol.wtz.domain.vote.service.VoteService;
import com.chibbol.wtz.global.timer.dto.*;
import com.chibbol.wtz.global.timer.entity.Timer;
import com.chibbol.wtz.global.timer.exception.TimerNotExistException;
import com.chibbol.wtz.global.timer.repository.TimerRedisRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NewTimerService {
    private int DAY_TIME = 90;
    private int VOTE_TIME = 15;
    private int VOTE_RESULT_TIME = 3;
    private int NIGHT_TIME = 15;
    private int NIGHT_RESULT_TIME = 3;
    private final String IMAGE_PATH = "static/item_images/";
    private final String GIF_PATH = "static/item_gifs/";

    private final JobService jobService;
    private final VoteService voteService;
    private final PointService pointService;
    private final UserLevelService userLevelService;
    private final StompTimerService stompTimerService;
    private final RoomEnterInfoRedisService roomEnterInfoRedisService;

    private final UserRepository userRepository;
    private final GameRepository gameRepository;
    private final UserItemRepository userItemRepository;
    private final TimerRedisRepository timerRedisRepository;
    private final RoomUserJobRedisRepository roomUserJobRedisRepository;
    private final UserAbilityLogRepository userAbilityLogRepository;
    private final UserAbilityRecordRedisRepository userAbilityRecordRedisRepository;

    // 타이머 생성
    public Timer createRoomTimer(String gameCode) {
        timerRedisRepository.createGameTimer(gameCode);

        Room room = gameRepository.findRoomByGameCode(gameCode);

        List<CurrentSeatsDTO> currentSeatsDTOs = roomEnterInfoRedisService.getUserEnterInfo(room.getRoomCode());
        for(CurrentSeatsDTO currentSeatsDTO : currentSeatsDTOs) {
            if(currentSeatsDTO.getUserSeq() <= 0) {
                continue;
            }
            roomUserJobRedisRepository.save(RoomUserJob.builder().userSeq(currentSeatsDTO.getUserSeq()).gameCode(gameCode).build());
        }

        return timerRedisRepository.getGameTimerInfo(gameCode);
    }

    // 타이머 시작
    public Timer startRoomTimer(String gameCode) {
        Timer timer = timerRedisRepository.getGameTimerInfo(gameCode);
        if(timer != null) {
            timer = Timer.builder().timerType("DAY").remainingTime(DAY_TIME).turn(1).build();
            timerRedisRepository.updateTimer(gameCode, timer);
        }
        return timer;
    }

    // 해당 방 타이머 정보 조회
    public Timer getTimerInfo(String gameCode) {
        Timer timer = timerRedisRepository.getGameTimerInfo(gameCode);
        if(timer == null) {
            throw new TimerNotExistException("Timer does not exist");
        }
        return timer;
    }

    // 해당 유저의 타이머 끝남을 저장
    public void timerEndUser(String gameCode, Long userSeq) {
        Timer timer = timerRedisRepository.getGameTimerInfo(gameCode);

        if(timer == null) {
            throw new TimerNotExistException("Timer does not exist");
        }

        if(timer.getTimerEndUserSeqs().contains(userSeq)) {
            return;
        }

        timer.getTimerEndUserSeqs().add(userSeq);
        timerRedisRepository.updateTimer(gameCode, timer);

        checkTimerEnd(gameCode, timer);
    }

    // 방에 있는 모든 유저의 타이머 끝남을 확인
    private void checkTimerEnd(String gameCode, Timer timer) {
        Room room = gameRepository.findRoomByGameCode(gameCode);

        List<CurrentSeatsDTO> currentSeatsDTOs = roomEnterInfoRedisService.getUserEnterInfo(room.getRoomCode());

        List<Long> enterUsers = currentSeatsDTOs.stream().map(CurrentSeatsDTO::getUserSeq).collect(Collectors.toList());

        for(Long enterUser : enterUsers) {
            if(enterUser <= 0) {
                continue;
            }

            if(!timer.getTimerEndUserSeqs().contains(enterUser)) {
                log.info(timer.getTimerEndUserSeqs().toString());
                log.info("gameUser : " + timer.getTimerEndUserSeqs());
                log.info("enterUser : " + enterUsers);
                log.info("timer not end");
                return;
            }
        }

        timerTypeChange(gameCode, timer);

        log.info(timer.getTimerEndUserSeqs().toString());
        log.info("gameUser : " + timer.getTimerEndUserSeqs());
        log.info("enterUser : " + enterUsers);
        log.info("timer type change");
    }

    public void timerDecreaseUser(String gameCode, Long userSeq) {
        Timer timer = timerRedisRepository.getGameTimerInfo(gameCode);

        if(timer == null) {
            throw new TimerNotExistException("Timer does not exist");
        }

        // 낮시간에만 시간을 줄일 수 있음
        if(!timer.getTimerType().equals("DAY")) {
            return;
        }

        // 죽은 사람이 요청을 보냈을 때
        if(!roomUserJobRedisRepository.findByGameCodeAndUserSeq(gameCode, userSeq).isAlive()) {
            return;
        }

        // 이미 요청을 보낸 사람일 때
        if(timer.getTimerDecreaseUserSeqs().contains(userSeq)) {
            return;
        }

        timer.getTimerDecreaseUserSeqs().add(userSeq);
        timerRedisRepository.updateTimer(gameCode, timer);
        stompTimerService.sendToClient("GAME_TIMER_DECREASE", gameCode, userSeq);
    }

    // 타이머 타입 변경
    public void timerTypeChange(String gameCode, Timer timer) {
        String type = timer.getTimerType();

        switch (type) {
            case "NONE" :
                timer = Timer.builder().timerType("DAY").remainingTime(DAY_TIME).turn(1).build();
                timerRedisRepository.updateTimer(gameCode, timer);

                List<RoomUserJob> roomUserRandomJob = jobService.randomJobInGameUser(gameCode);

                // 직업 정보, 게임 시작 알림
                stompTimerService.sendToClient("GAME_START", gameCode, roomUserJobsToData(roomUserRandomJob));
                stompTimerService.sendToClient("GAME_TIMER", gameCode, TimerDTO.builder().type(timer.getTimerType()).time(timer.getRemainingTime()).build());
                stompTimerService.sendToClient("GAME_BLACKOUT", gameCode, generateBlackOutData(gameCode));
                break;

            case "DAY" :
                timer.update(Timer.builder().timerType("VOTE").remainingTime(VOTE_TIME).build());
                timerRedisRepository.updateTimer(gameCode, timer);

                stompTimerService.sendToClient("GAME_TIMER", gameCode, TimerDTO.builder().type(timer.getTimerType()).time(timer.getRemainingTime()).build());
                break;

            case "VOTE" :
                Long mostVotedUser = voteService.voteResult(gameCode, timer.getTurn());
                stompTimerService.sendToClient("GAME_VOTE_RESULT", gameCode, mostVotedUser);

                // 게임 끝났으면 GAME_OVER, 아니면 VOTE_RESULT
                List<UserAbilityLog> userAbilityLogsV = jobService.checkGameOver(gameCode);
                if(userAbilityLogsV != null) {
                    giveExpAndPoint(userAbilityLogsV);

                    stompTimerService.sendToClient("GAME_OVER", gameCode, userAbilityLogsToData(userAbilityLogsV));

                    timerRedisRepository.deleteGameTimer(gameCode);
                } else {
                    timer.update(Timer.builder().timerType("VOTE_RESULT").remainingTime(VOTE_RESULT_TIME).build());
                    timerRedisRepository.updateTimer(gameCode, timer);
                    stompTimerService.sendToClient("GAME_TIMER", gameCode, TimerDTO.builder().type(timer.getTimerType()).time(timer.getRemainingTime()).build());
                }
                break;

            case "VOTE_RESULT" :
                timer.update(Timer.builder().timerType("NIGHT").remainingTime(NIGHT_TIME).build());
                timerRedisRepository.updateTimer(gameCode, timer);
                stompTimerService.sendToClient("GAME_TIMER", gameCode, TimerDTO.builder().type(timer.getTimerType()).time(timer.getRemainingTime()).build());
                break;

            case "NIGHT" :
                Map<String, Long> turnResult = jobService.useAbilityNight(gameCode, timer.getTurn());
                List<UserAbilityRecord> userAbilityRecords = userAbilityRecordRedisRepository.findAllByGameCodeAndTurn(gameCode, timer.getTurn());
                List<RoomUserJob> roomUser = roomUserJobRedisRepository.findAllByGameCode(gameCode);

                stompTimerService.sendToClient("GAME_NIGHT_RESULT", gameCode, userAbilityLogsToData(turnResult, userAbilityRecords, roomUser));

                // 게임 끝났으면 GAME_OVER, 아니면 NIGHT_RESULT
                List<UserAbilityLog> userAbilityLogsN = jobService.checkGameOver(gameCode);
                if(userAbilityLogsN != null) {
                    giveExpAndPoint(userAbilityLogsN);

                    stompTimerService.sendToClient("GAME_OVER", gameCode, userAbilityLogsToData(userAbilityLogsN));

                    timerRedisRepository.deleteGameTimer(gameCode);
                } else {
                    timer.update(Timer.builder().timerType("NIGHT_RESULT").remainingTime(NIGHT_RESULT_TIME).build());
                    timerRedisRepository.updateTimer(gameCode, timer);
                    stompTimerService.sendToClient("GAME_TIMER", gameCode, TimerDTO.builder().type(timer.getTimerType()).time(timer.getRemainingTime()).build());
                }
                break;

            case "NIGHT_RESULT" :
                timer.update(Timer.builder().timerType("DAY").remainingTime(DAY_TIME).turn(timer.getTurn() + 1).build());
                timerRedisRepository.updateTimer(gameCode, timer);
                stompTimerService.sendToClient("GAME_TIMER", gameCode, TimerDTO.builder().type(timer.getTimerType()).time(timer.getRemainingTime()).build());
                stompTimerService.sendToClient("GAME_BLACKOUT", gameCode, generateBlackOutData(gameCode));
                break;
            default :
                break;
        }

    }

    private BlackOutDataDTO generateBlackOutData(String gameCode) {
        List<RoomUserJob> roomUsers = roomUserJobRedisRepository.findAllByGameCode(gameCode);
        List<Long> aliveRoomUserSeq = roomUsers.stream()
                .filter(RoomUserJob::isAlive)
                .map(RoomUserJob::getUserSeq)
                .collect(Collectors.toList());

        if (!aliveRoomUserSeq.isEmpty()) {
            // 블랙아웃 유저 랜덤으로 설정
            Random random = new Random();
            Long randomUserSeq = aliveRoomUserSeq.get(random.nextInt(aliveRoomUserSeq.size()));

            // 블랙아웃 시작 시간 랜덤으로 설정
            // TODO : 현재 테스트용
//            int startSecond = random.nextInt(40) + 10;
            int startSecond = 10;

            log.info("====================================");
            log.info("randomUserSeq : " + randomUserSeq);
            log.info("startSecond : " + startSecond);
            log.info("====================================");


            return BlackOutDataDTO.builder()
                    .userSeq(randomUserSeq)
                    .startSecond(startSecond)
                    .build();
        }

        return BlackOutDataDTO.builder().build();
    }

    private List<GameUserDataDTO> roomUserJobsToData(List<RoomUserJob> roomUserJobs) {
        List<GameUserDataDTO> gameUserDataDTOList = roomUserJobs.stream()
                                                        .map(roomUserJob -> GameUserDataDTO.builder()
                                                                .userSeq(roomUserJob.getUserSeq())
                                                                .jobSeq(roomUserJob.getJobSeq())
                                                                .nickname(userRepository.findNicknameByUserSeq(roomUserJob.getUserSeq()))
                                                                .build())
                                                        .collect(Collectors.toList());

        for(GameUserDataDTO gameUserDataDTO : gameUserDataDTOList) {
            Map<String, byte[]> equippedItems = gameUserDataDTO.getEquippedItems();
            Map<String, byte[]> equippedItemsGif = gameUserDataDTO.getEquippedItemsGif();

            for(String type : equippedItems.keySet()) {
                equippedItems.put(type, getEquippedItem(gameUserDataDTO.getUserSeq(), type));
                equippedItemsGif.put(type, getEquippedItemGif(gameUserDataDTO.getUserSeq(), type));
            }
        }

        return gameUserDataDTOList;
    }

    private GameResultDataDTO userAbilityLogsToData(List<UserAbilityLog> userAbilityLogs) {
        if(userAbilityLogs.size() > 0) {
            return GameResultDataDTO.builder()
                    .rabbitWin(isRabbitWin(userAbilityLogs.get(0).getGameCode()))
                    .userInfo(userAbilityLogs.stream()
                            .map(userAbilityLog -> GameResultDataDTO.GameResult.builder()
                                    .userSeq(userAbilityLog.getUser().getUserSeq())
                                    .jobSeq(userAbilityLog.getJob().getJobSeq())
                                    .nickname(userAbilityLog.getUser().getNickname())
                                    .win(userAbilityLog.isResult())
                                    .build())
                            .collect(Collectors.toList()))
                    .build();
        } else {
            return null;
        }
    }

    private NightResultDataDTO userAbilityLogsToData(Map<String, Long> turnResult, List<UserAbilityRecord> userAbilityLogs, List<RoomUserJob> roomUserJobs) {
        NightResultDataDTO nightResultDataDTO = NightResultDataDTO.builder()
                .deadUserSeq(turnResult.get("kill"))
                .threatUserSeq(turnResult.get("Gangster"))
                .healUserSeq(turnResult.get("Doctor"))
                .build();
        for(RoomUserJob roomUserJob : roomUserJobs) {
            nightResultDataDTO.addAbility(roomUserJob.getUserSeq(), null, false);
        }
        for(UserAbilityRecord userAbilityRecord : userAbilityLogs) {
            nightResultDataDTO.addAbility(userAbilityRecord.getUserSeq(), userAbilityRecord.getTargetUserSeq(), userAbilityRecord.isSuccess());
        }

        return nightResultDataDTO;
    }

    private boolean isRabbitWin(String gameCode) {
        List<UserAbilityLog> userAbilityLogs = userAbilityLogRepository.findAllByGameCode(gameCode);

        Long mafiaSeq = jobService.getMafiaSeq();
        for (UserAbilityLog userAbilityLog : userAbilityLogs) {
            if(userAbilityLog.getJob().getJobSeq().equals(mafiaSeq)) {
                return !userAbilityLog.isResult();
            }
        }

        log.info("isRabbitWin error");
        return false;
    }

    private void giveExpAndPoint(List<UserAbilityLog> userAbilityLogs) {
        userLevelService.updateExp(userAbilityLogs);
        pointService.updatePoint(userAbilityLogs);
    }

    private byte[] getEquippedItem(Long userSeq, String type) {
        UserItem item = userItemRepository.findByUserUserSeqAndItemTypeAndEquipped(userSeq, type, true);

        byte[] imageData = null;
        try {
            Path imageFilePath = null;
            if(item == null) {
                // 이미지를 byte[]로 변환
                imageFilePath = Paths.get(IMAGE_PATH + type).resolve("none.png");
            } else {
                // 이미지를 byte[]로 변환
                imageFilePath = Paths.get(IMAGE_PATH + item.getItem().getType()).resolve(item.getItem().getImage());

            }
            Resource resource = new ClassPathResource(imageFilePath.toString());
                if (resource != null) {
                    imageData = resource.getInputStream().readAllBytes();

                    return imageData;
                }
        } catch (IOException e) {
            e.printStackTrace();
        }


        return null;
    }

    private byte[] getEquippedItemGif(Long userSeq, String type) {
        UserItem item = userItemRepository.findByUserUserSeqAndItemTypeAndEquipped(userSeq, type, true);

        byte[] imageData = null;
        try {
            Path imageFilePath = null;
            if(item == null) {
                // 이미지를 byte[]로 변환
                imageFilePath = Paths.get(GIF_PATH + type).resolve("none.png");
            } else {
                // 이미지를 byte[]로 변환
                imageFilePath = Paths.get(GIF_PATH + item.getItem().getType()).resolve(item.getItem().getGif());

            }
            Resource resource = new ClassPathResource(imageFilePath.toString());
            if (resource != null) {
                imageData = resource.getInputStream().readAllBytes();

                return imageData;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }


        return null;
    }
}
