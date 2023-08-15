package com.chibbol.wtz.domain.point.conroller;

import com.chibbol.wtz.domain.point.dto.PointResultDTO;
import com.chibbol.wtz.domain.point.entity.UserPointValue;
import com.chibbol.wtz.domain.point.repository.UserPointValueRedisRepository;
import com.chibbol.wtz.domain.point.service.PointService;
import com.chibbol.wtz.domain.record.entiry.UserAbilityLog;
import com.chibbol.wtz.domain.record.repository.UserAbilityLogRepository;
import com.chibbol.wtz.domain.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/point")
@RequiredArgsConstructor
public class PointController {
    private final PointService pointService;
    private final UserService userService;

    private final UserAbilityLogRepository userAbilityLogRepository;
    private final UserPointValueRedisRepository userPointValueRedisRepository;

    @PatchMapping("/{gameCode}")
    public ResponseEntity<Void> updatePoint(@PathVariable String gameCode){
        List<UserAbilityLog> userAbilityLogs = userAbilityLogRepository.findAllByGameCode(gameCode);
        List<PointResultDTO> list = pointService.updatePoint(userAbilityLogs);

        for(PointResultDTO temp: list){
            log.info(temp.getUserSeq()+" "+temp.getCurrentPoint() +" "+ temp.getPointValue());
        }
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "포인트 조회")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "포인트 조회 성공"),
            @ApiResponse(responseCode = "404", description = "유저를 찾을 수 없습니다.")
    })
    @GetMapping("")
    public ResponseEntity<Integer> getPoint() {
        int point = pointService.getPoint();
        return ResponseEntity.ok(point);
    }

    @GetMapping("/{gameCode}")
    public ResponseEntity<UserPointValue> getPointValueInGameCode(@PathVariable String gameCode) {
        UserPointValue userPointValue = pointService.getPointValueInGameCode(gameCode);

        return ResponseEntity.ok(userPointValue);
    }
}
