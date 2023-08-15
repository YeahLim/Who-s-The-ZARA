package com.chibbol.wtz.domain.record.service;

import com.chibbol.wtz.domain.job.entity.Job;
import com.chibbol.wtz.domain.job.repository.JobRepository;
import com.chibbol.wtz.domain.record.dto.RecentRecordDTO;
import com.chibbol.wtz.domain.record.entiry.UserAbilityLog;
import com.chibbol.wtz.domain.record.repository.UserAbilityLogRepository;
import com.chibbol.wtz.domain.user.entity.User;
import com.chibbol.wtz.domain.user.exception.UserNotFoundException;
import com.chibbol.wtz.domain.user.service.UserService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class RecordService {

    private final UserService userService;
    private final JobRepository jobRepository;
    private final UserAbilityLogRepository userAbilityLogRepository;

    public List<RecentRecordDTO> getRecentRecord() {
        User user = userService.getLoginUser();
        if (user == null) {
            throw new UserNotFoundException("유저 정보를 찾을 수 없습니다.");
        }

        List<UserAbilityLog> userAbilityLogs = userAbilityLogRepository.findTop10ByUserOrderByStartAtDesc(user);

        return userAbilityLogsToRecentRecordDTO(userAbilityLogs);
    }

    public int getWinRate() {
        User user = userService.getLoginUser();
        if (user == null) {
            throw new UserNotFoundException("유저 정보를 찾을 수 없습니다.");
        }

        int playCount = userAbilityLogRepository.countByUser(user);
        int winCount = userAbilityLogRepository.countByUserAndResult(user, true);

        if (playCount == 0) {
            return 0;
        }

        return (int) ((double) winCount / playCount * 100);
    }

    public Map<Long, Integer> getJobWinRate() {
        User user = userService.getLoginUser();
        if (user == null) {
            throw new UserNotFoundException("유저 정보를 찾을 수 없습니다.");
        }

        List<Job> jobs = jobRepository.findAll();

        Map<Long, Integer> jobWinRateMap = new HashMap<>();

        for (Job job : jobs) {
            int playCount = userAbilityLogRepository.countByUserAndJob(user, job);
            int winCount = userAbilityLogRepository.countByUserAndJobAndResult(user, job, true);

            if (playCount == 0) {
                jobWinRateMap.put(job.getJobSeq(), 0);
            } else {
                jobWinRateMap.put(job.getJobSeq(), (int) ((double) winCount / playCount * 100));
            }
        }

        return jobWinRateMap;
    }

    public List<RecentRecordDTO> userAbilityLogsToRecentRecordDTO(List<UserAbilityLog> userAbilityLogs) {
        return userAbilityLogs.stream()
                .map(userAbilityLog -> RecentRecordDTO.builder()
                        .jobSeq(userAbilityLog.getJob().getJobSeq())
                        .gameCode(userAbilityLog.getGameCode())
                        .win(userAbilityLog.isResult())
                        .startAt(userAbilityLog.getStartAt())
                        .endAt(userAbilityLog.getEndAt())
                        .build())
                .collect(Collectors.toList());
    }
}
