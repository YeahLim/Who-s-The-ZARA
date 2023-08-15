package com.chibbol.wtz.domain.vote.service;

import com.chibbol.wtz.domain.job.entity.RoomUserJob;
import com.chibbol.wtz.domain.job.repository.JobRepository;
import com.chibbol.wtz.domain.job.repository.RoomUserJobRedisRepository;
import com.chibbol.wtz.domain.record.entiry.UserAbilityRecord;
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
import java.util.function.Function;
import java.util.stream.Collectors;

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
            logCantVote(voteDTO);
            return;
        }

        Vote vote = Vote.builder().gameCode(voteDTO.getGameCode()).turn(voteDTO.getTurn()).userSeq(voteDTO.getUserSeq()).targetUserSeq(voteDTO.getTargetUserSeq()).build();
        voteRedisRepository.save(vote);

        logVoteSuccess(voteDTO);
    }

    @Transactional
    public Long voteResult(String gameCode, int turn) {
        List<Vote> votes = voteRedisRepository.findAllByGameCodeAndTurn(gameCode, turn);
        Long politicianSeq = jobRepository.findByName("Politician").getJobSeq();
        RoomUserJob politician = roomUserJobRedisRepository.findByRoomSeqAndJobSeq(gameCode, politicianSeq);

        Map<Long, RoomUserJob> roomUserJobs = getRoomUserJobs(gameCode);
        Map<Long, Boolean> canVoteMap = getCanVoteMap(roomUserJobs);
        Map<Long, Integer> voteCountMap = calculateVoteCounts(votes, canVoteMap, roomUserJobs, politicianSeq);

        Long mostVotedTargetUserSeq = findMostVotedTargetUserSeq(voteCountMap);

        updateCanVoteForAll(roomUserJobs);

        return processVoteResult(gameCode, turn, mostVotedTargetUserSeq, politician.getUserSeq());
    }

    public List<VoteResultDTO> getRealTimeVoteResultWithJob(String gameCode, int turn) {
        List<Vote> votes = voteRedisRepository.findAllByGameCodeAndTurn(gameCode, turn);
        Long politicianSeq = jobRepository.findByName("Politician").getJobSeq();

        Map<Long, RoomUserJob> roomUserJobs = getRoomUserJobs(gameCode);
        Map<Long, Boolean> canVoteMap = getCanVoteMap(roomUserJobs);
        Map<Long, Integer> voteCountMap = calculateVoteCounts(votes, canVoteMap, roomUserJobs, politicianSeq);

        List<VoteResultDTO> voteResultDTOList = createVoteResultDTOList(voteCountMap, roomUserJobs);
        log.info("voteResultDTOList: " + voteResultDTOList.toString());

        return voteResultDTOList;
    }

    private Map<Long, RoomUserJob> getRoomUserJobs(String gameCode) {
        List<RoomUserJob> roomUserJobs = roomUserJobRedisRepository.findAllByGameCode(gameCode);
        return roomUserJobs.stream().collect(Collectors.toMap(RoomUserJob::getUserSeq, Function.identity()));
    }

    private Map<Long, Boolean> getCanVoteMap(Map<Long, RoomUserJob> roomUserJobs) {
        return roomUserJobs.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().isAlive() && entry.getValue().isCanVote()));
    }

    private Map<Long, Integer> calculateVoteCounts(List<Vote> votes, Map<Long, Boolean> canVoteMap, Map<Long, RoomUserJob> roomUserJobs, Long politicianSeq) {
        Map<Long, Integer> voteCountMap = new HashMap<>();

        for (Vote vote : votes) {
            RoomUserJob roomUserJob = roomUserJobs.get(vote.getUserSeq());
            if (roomUserJob != null && canVoteMap.get(vote.getUserSeq())) {
                calculateVoteCount(voteCountMap, vote, roomUserJob.getJobSeq(), politicianSeq);
            }
        }
        return voteCountMap;
    }

    private void calculateVoteCount(Map<Long, Integer> voteCountMap, Vote vote, Long jobSeq, Long politicianSeq) {
        Long targetUserSeq = vote.getTargetUserSeq();
        if (targetUserSeq.equals(0)) {
            return;
        }

        int voteWeight = politicianSeq.equals(jobSeq) ? 2 : 1;
        voteCountMap.put(targetUserSeq, voteCountMap.getOrDefault(targetUserSeq, 0) + voteWeight);
    }

    private Long findMostVotedTargetUserSeq(Map<Long, Integer> voteCountMap) {
        Long mostVotedTargetUserSeq = null;
        int maxVotes = 0;
        for (Map.Entry<Long, Integer> entry : voteCountMap.entrySet()) {
            Long targetUserSeq = entry.getKey();
            int voteCount = entry.getValue();
            if (voteCount > maxVotes) {
                maxVotes = voteCount;
                mostVotedTargetUserSeq = targetUserSeq;
            } else if (voteCount == maxVotes) {
                mostVotedTargetUserSeq = null;
            }
        }
        return mostVotedTargetUserSeq;
    }

    private void updateCanVoteForAll(Map<Long, RoomUserJob> roomUserJobs) {
        for (RoomUserJob roomUserJob : roomUserJobs.values()) {
            roomUserJob.setCanVote(true);
            roomUserJobRedisRepository.save(roomUserJob);
        }
    }

    private Long processVoteResult(String gameCode, int turn, Long mostVotedTargetUserSeq, Long politician) {
        log.warn("mostVotedTargetUserSeq: " + mostVotedTargetUserSeq);
        log.warn("politicianSeq: " + politician);

        if (mostVotedTargetUserSeq != null) {
            if (mostVotedTargetUserSeq.equals(0L) || mostVotedTargetUserSeq.equals(0)) {
                mostVotedTargetUserSeq = null;
                logSkipVote(gameCode, turn);
            } else if (mostVotedTargetUserSeq.equals(politician)) {
                savePoliticianAbility(gameCode, turn, politician);
                mostVotedTargetUserSeq = null;
                logMostVotedPolitician(gameCode, turn);
            } else {
                RoomUserJob mostVotedTargetUser = roomUserJobRedisRepository.findByGameCodeAndUserSeq(gameCode, mostVotedTargetUserSeq);
                mostVotedTargetUser.setAlive(false);
                mostVotedTargetUser.setCanVote(false);
                roomUserJobRedisRepository.save(mostVotedTargetUser);
            }
        }

        return mostVotedTargetUserSeq;
    }

    private List<VoteResultDTO> createVoteResultDTOList(Map<Long, Integer> voteCountMap, Map<Long, RoomUserJob> roomUserJobs) {
        List<VoteResultDTO> voteResultDTOList = new ArrayList<>();
        voteResultDTOList.add(VoteResultDTO.builder()
                .userSeq(0L)
                .cnt(voteCountMap.getOrDefault(0L, 0))
                .build());

        for(Map.Entry<Long, RoomUserJob> entry : roomUserJobs.entrySet()) {
            if(roomUserJobs.get(entry.getKey()).isAlive()) {
                voteResultDTOList.add(VoteResultDTO.builder()
                        .userSeq(entry.getKey())
                        .cnt(voteCountMap.getOrDefault(entry.getKey(), 0))
                        .build());
            }
        }

        return voteResultDTOList;
    }

    private void savePoliticianAbility(String gameCode, int turn, Long politician) {
        UserAbilityRecord userAbilityRecord = UserAbilityRecord.builder()
                .gameCode(gameCode)
                .turn(turn)
                .userSeq(politician)
                .build();
        userAbilityRecord.success();
        userAbilityRecordRedisRepository.save(userAbilityRecord);
    }

    private void logCantVote(VoteDTO voteDTO) {
        log.info("====================================");
        log.info("VOTE FAIL");
        log.info("ROOM: " + voteDTO.getGameCode());
        log.info("TURN: " + voteDTO.getTurn());
        log.info("VOTE USER: " + voteDTO.getUserSeq());
        log.info("TARGET USER: " + voteDTO.getTargetUserSeq());
        log.info("====================================");
    }

    private void logVoteSuccess(VoteDTO voteDTO) {
        log.info("====================================");
        log.info("VOTE SUCCESS");
        log.info("ROOM: " + voteDTO.getGameCode());
        log.info("TURN: " + voteDTO.getTurn());
        log.info("VOTE USER: " + voteDTO.getUserSeq());
        log.info("TARGET USER: " + voteDTO.getTargetUserSeq());
        log.info("====================================");
    }

    private void logSkipVote(String gameCode, int turn) {
        log.info("====================================");
        log.info("SKIP VOTE");
        log.info("ROOM: " + gameCode);
        log.info("TURN: " + turn);
        log.info("====================================");
    }

    private void logMostVotedPolitician(String gameCode, int turn) {
        log.info("====================================");
        log.info("MOST VOTED POLITICIAN");
        log.info("ROOM: " + gameCode);
        log.info("TURN: " + turn);
        log.info("====================================");
    }
}
