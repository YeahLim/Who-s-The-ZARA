package com.chibbol.wtz.domain.point.service;

import com.chibbol.wtz.domain.point.dto.PointResultDTO;
import com.chibbol.wtz.domain.point.entity.Point;
import com.chibbol.wtz.domain.point.entity.UserPointValue;
import com.chibbol.wtz.domain.point.repository.PointRepository;
import com.chibbol.wtz.domain.point.repository.UserPointValueRedisRepository;
import com.chibbol.wtz.domain.record.entiry.UserAbilityLog;
import com.chibbol.wtz.domain.room.exception.GameNotFoundException;
import com.chibbol.wtz.domain.user.entity.User;
import com.chibbol.wtz.domain.user.exception.UserNotFoundException;
import com.chibbol.wtz.domain.user.service.UserService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@AllArgsConstructor
public class PointService {
    private List<PointResultDTO> pointResults;

    private final UserService userService;

    private final PointRepository pointRepository;
    private final UserPointValueRedisRepository userPointValueRedisRepository;


    public int getPoint() {
        User user = userService.getLoginUser();
        if(user == null) {
            throw new UserNotFoundException("유저를 찾을 수 없습니다.");
        }

        Point point = pointRepository.findByUserUserSeq(user.getUserSeq()).orElse(Point.builder().user(user).point(0).build());
        return point.getPoint();
    }

    public UserPointValue getPointValueInGameCode(String gameCode) {
        User user = userService.getLoginUser();
        if(user == null) {
            throw new UserNotFoundException("유저를 찾을 수 없습니다.");
        }

        UserPointValue userPointValue = userPointValueRedisRepository.findByUserSeq(gameCode, user.getUserSeq());

        if(userPointValue == null) {
            throw new GameNotFoundException("게임을 찾을 수 없습니다.");
        }

        return userPointValue;
    }

    public List<PointResultDTO> updatePoint(List<UserAbilityLog> userAbilityLogs) {
        pointResults = new ArrayList<>();

        for(UserAbilityLog userInfo: userAbilityLogs){
            Point point = pointRepository.findByUserUserSeq(userInfo.getUser().getUserSeq()).orElse(Point.builder().user(userInfo.getUser()).point(0).build());
            PointResultDTO pointResult = new PointResultDTO();

            givePoint(userInfo, point, pointResult);
            pointResult.setCurrentPoint(point.getPoint());
            pointResult.setUserSeq(userInfo.getUser().getUserSeq());

            pointRepository.save(point);
            pointResults.add(pointResult);
        }

        return pointResults;
    }

    // 승패에 따라 포인트 얻기
    public void givePoint(UserAbilityLog userInfo, Point point, PointResultDTO pointResult){
        String gameCode = userInfo.getGameCode();
        Long userSeq = userInfo.getUser().getUserSeq();
        UserPointValue userPointValue = UserPointValue.builder().lastPoint(point.getPoint()).build();

        if(userInfo.isResult()){ // 이기면 15
            pointResult.setPointValue(15);
            point.addPoint(15);
        }

        if(!userInfo.isResult()){ // 지면 10
            pointResult.setPointValue(10);
            point.addPoint(10);
        }

        userPointValue.setCurrentPoint(point.getPoint());
        userPointValueRedisRepository.save(gameCode, userSeq, userPointValue);
    }
}
