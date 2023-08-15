package com.chibbol.wtz.domain.vote.service;

import com.chibbol.wtz.domain.job.entity.RoomUserJob;
import com.chibbol.wtz.domain.job.repository.JobRepository;
import com.chibbol.wtz.domain.job.repository.RoomUserJobRedisRepository;
import com.chibbol.wtz.domain.record.repository.UserAbilityRecordRedisRepository;
import com.chibbol.wtz.domain.vote.dto.VoteDTO;
import com.chibbol.wtz.domain.vote.dto.VoteResultDTO;
import com.chibbol.wtz.domain.vote.entity.Vote;
import com.chibbol.wtz.domain.vote.repository.VoteRedisRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class VoteService {
    private final VoteRedisRepository voteRedisRepository;
    private final RoomUserJobRedisRepository roomUserJobRedisRepository;
    private final JobRepository jobRepository;
    private final UserAbilityRecordRedisRepository userAbilityRecordRedisRepository;

    public void vote(VoteDTO voteDTO) {
        // 투표 가능한 상태인지 확인 ( 살아있는지, 투표권한이 있는지 )
        boolean canVote = roomUserJobRedisRepository.canVote(voteDTO.getGameCode(), voteDTO.getUserSeq());
        if (!canVote) {
            log.info("====================================");
            log.info("VOTE FAIL (CAN'T VOTE)");
            log.info("ROOM: " + voteDTO.getGameCode());
            log.info("TURN: " + voteDTO.getTurn());
            log.info("VOTE USER: " + voteDTO.getUserSeq());
            log.info("====================================");
            return;
        }

        Vote vote = Vote.builder().gameCode(voteDTO.getGameCode()).turn(voteDTO.getTurn()).userSeq(voteDTO.getUserSeq()).targetUserSeq(voteDTO.getTargetUserSeq()).build();
        voteRedisRepository.save(vote);

        log.info("====================================");
        log.info("VOTE SUCCESS");
        log.info("ROOM: " + voteDTO.getGameCode());
        log.info("TURN: " + voteDTO.getTurn());
        log.info("VOTE USER: " + voteDTO.getUserSeq());
        log.info("TARGET USER: " + voteDTO.getTargetUserSeq());
        log.info("====================================");
    }

    @Transactional
    public Long voteResult(String gameCode, int turn) {
        List<Vote> votes = voteRedisRepository.findAllByGameCodeAndTurn(gameCode, turn);

        Long politicianSeq = jobRepository.findByName("Politician").getJobSeq();
        Long politician = null;

        List<RoomUserJob> roomUserJobs = roomUserJobRedisRepository.findAllByGameCode(gameCode);
        Map<Long, Boolean> canVoteMap = new HashMap<>();
        for(RoomUserJob roomUserJob : roomUserJobs) {
            canVoteMap.put(roomUserJob.getUserSeq(), roomUserJob.isAlive() && roomUserJob.isCanVote());
            if(roomUserJob.getJobSeq().equals(politicianSeq)) {
                politician = roomUserJob.getUserSeq();
            }
        }

        // 투표 결과를 저장할 맵
        Map<Long, Integer> voteCountMap = new HashMap<>();
        for(Vote vote : votes) {
            if(!canVoteMap.get(vote.getUserSeq())) {
                continue;
            }

            Long targetUserSeq = vote.getTargetUserSeq();
            // 무투표(0) 선택하였을 경우
            if(targetUserSeq.equals(0)) {
                continue;
            }

            // 정치인은 2표 나머지는 1표씩 적용
            if(vote.getUserSeq().equals(politician)) {
                voteCountMap.put(targetUserSeq, voteCountMap.getOrDefault(targetUserSeq, 0) + 2);
            } else {
                voteCountMap.put(targetUserSeq, voteCountMap.getOrDefault(targetUserSeq, 0) + 1);
            }
        }

        // 가장 많이 투표된 targetUserSeq를 찾기 위해 맵을 순회
        Long mostVotedTargetUserSeq = null;
        int maxVotes = 0;
        for (Map.Entry<Long, Integer> entry : voteCountMap.entrySet()) {
            Long targetUserSeq = entry.getKey();
            int voteCount = entry.getValue();
            if (voteCount > maxVotes) {
                maxVotes = voteCount;
                mostVotedTargetUserSeq = targetUserSeq;
            } else if (voteCount == maxVotes) {
                // 최다 득표자가 2명 이상일 때 null을 반환
                mostVotedTargetUserSeq = null;
            }
        }

        //
        roomUserJobRedisRepository.updateCanVoteByRoomSeq(gameCode, true);

        // 최다 득표자가 존재하면 사망 처리
        if(mostVotedTargetUserSeq != null) {
            // 최다 득표가 스킵일때
            if(mostVotedTargetUserSeq.equals(0L)) {
                mostVotedTargetUserSeq = null;

                log.info("====================================");
                log.info("SKIP VOTE");
                log.info("GAME_CODE: " + gameCode);
                log.info(("TURN: " + turn));
                log.info("====================================");

            // 최다 득표가 정치인일 떄
            } else if(mostVotedTargetUserSeq.equals(politician)) {
                // TODO : 정치인 능력 성공으로 저장 해야함
//                UserAbilityRecord userAbilityRecord = userAbilityRecordRedisRepository.findByGameCodeAndTurnAndUserSeq(gameCode, turn, mostVotedTargetUserSeq);
//                userAbilityRecord.success();
//                userAbilityRecordRedisRepository.save(userAbilityRecord);

                mostVotedTargetUserSeq = null;
                log.info("====================================");
                log.info("MOST VOTED USER IS POLITICIAN");
                log.info("GAME_CODE: " + gameCode);
                log.info(("TURN: " + turn));
                log.info("====================================");
            } else  {
                RoomUserJob mostVotedTargetUser = roomUserJobRedisRepository.findByGameCodeAndUserSeq(gameCode, mostVotedTargetUserSeq);
                mostVotedTargetUser.setAlive(false);
                mostVotedTargetUser.setCanVote(false);
                roomUserJobRedisRepository.save(mostVotedTargetUser);

                log.info("====================================");
                log.info("VOTE RESULT");
                log.info("ROOM: " + gameCode);
                log.info("TURN: " + turn);
                log.info("VOTE COUNT MAP: " + voteCountMap);
                log.info("MOST VOTED TARGET USER: " + mostVotedTargetUserSeq);
                log.info("====================================");
            }
        }

        return mostVotedTargetUserSeq;
    }

    public List<VoteResultDTO> getRealTimeVoteResult(String gameCode, int turn) {
        List<RoomUserJob> roomUserJobs = roomUserJobRedisRepository.findAllByGameCode(gameCode);
        List<Vote> votes = voteRedisRepository.findAllByGameCodeAndTurn(gameCode, turn);

        Map<Long, Integer> voteCountMap = new HashMap<>();
        voteCountMap.put(0L, 0);    // 무투표
        for(RoomUserJob roomUserJob : roomUserJobs) {
            voteCountMap.put(roomUserJob.getUserSeq(), 0);
        }

        for (Vote vote : votes) {
            Long targetUserSeq = vote.getTargetUserSeq();
            voteCountMap.put(targetUserSeq, voteCountMap.getOrDefault(targetUserSeq, 0) + 1);
        }

        List<VoteResultDTO> voteResultDTOList = new ArrayList<>();
        for(Long userSeq : voteCountMap.keySet()) {
            voteResultDTOList.add(VoteResultDTO.builder().userSeq(userSeq).cnt(voteCountMap.get(userSeq)).build());
        }

        return voteResultDTOList;
    }

    public List<VoteResultDTO> getRealTimeVoteResultWithJob(String gameCode, int turn) {
        List<RoomUserJob> roomUserJobs = roomUserJobRedisRepository.findAllByGameCode(gameCode);
        List<Vote> votes = voteRedisRepository.findAllByGameCodeAndTurn(gameCode, turn);

        Map<Long, Integer> voteCountMap = new HashMap<>();
        voteCountMap.put(0L, 0);    // 무투표
        for(RoomUserJob roomUserJob : roomUserJobs) {
            voteCountMap.put(roomUserJob.getUserSeq(), 0);
        }

        Long politicianSeq = jobRepository.findByName("Politician").getJobSeq();
        log.info("Politician Seq: " + politicianSeq);
        Long politician = null;

        Map<Long, Boolean> canVoteMap = new HashMap<>();
        for(RoomUserJob roomUserJob : roomUserJobs) {
            canVoteMap.put(roomUserJob.getUserSeq(), roomUserJob.isAlive() && roomUserJob.isCanVote());
            if(roomUserJob.getJobSeq().equals(politicianSeq)) {
                politician = roomUserJob.getUserSeq();
                log.info(politician+"");
            }
        }

        // 투표 결과를 저장할 맵
        for(Vote vote : votes) {
            if(!canVoteMap.get(vote.getUserSeq())) {
                continue;
            }

            Long targetUserSeq = vote.getTargetUserSeq();
            // 무투표(0) 선택하였을 경우
            if(targetUserSeq.equals(0)) {
                continue;
            }

            // 정치인은 2표 나머지는 1표씩 적용
            if(vote.getUserSeq().equals(politician)) {
                log.info("========== 정치인 ==============");
                voteCountMap.put(targetUserSeq, voteCountMap.getOrDefault(targetUserSeq, 0) + 2);
            } else {
                voteCountMap.put(targetUserSeq, voteCountMap.getOrDefault(targetUserSeq, 0) + 1);
            }
        }

        List<VoteResultDTO> voteResultDTOList = new ArrayList<>();
        for(Long userSeq : voteCountMap.keySet()) {
            voteResultDTOList.add(VoteResultDTO.builder().userSeq(userSeq).cnt(voteCountMap.get(userSeq)).build());
        }

        return voteResultDTOList;
    }


    public boolean checkGameOver(String gameCode) {
        Long mafiaSeq = jobRepository.findByName("Mafia").getJobSeq();

        long mafiaCount = roomUserJobRedisRepository.countByAliveUser(gameCode, mafiaSeq, true);
        long citizenCount = roomUserJobRedisRepository.countByAliveUser(gameCode, mafiaSeq, false);

        return mafiaCount >= citizenCount ? true : false;
    }
}
