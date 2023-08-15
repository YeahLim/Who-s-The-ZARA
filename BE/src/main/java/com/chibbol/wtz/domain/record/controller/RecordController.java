package com.chibbol.wtz.domain.record.controller;

import com.chibbol.wtz.domain.record.dto.RecentRecordDTO;
import com.chibbol.wtz.domain.record.service.RecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@AllArgsConstructor
@RequestMapping("/api/v1/record")
public class RecordController {

    private final RecordService recordService;

    @Operation(summary = "최근 플레이 기록 10개")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "성공"),
            @ApiResponse(responseCode = "404", description = "유저정보를 찾을수 없음")
    })
    @GetMapping("/recent")
    public ResponseEntity<List> getRecentRecord() {
        List<RecentRecordDTO> recentRecords = recordService.getRecentRecord();

        return ResponseEntity.ok(recentRecords);
    }

    @Operation(summary = "전체 승률")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "성공"),
            @ApiResponse(responseCode = "404", description = "유저정보를 찾을수 없음")
    })
    @GetMapping("/winRate")
    public ResponseEntity<Integer> getWinRate() {
        return ResponseEntity.ok(recordService.getWinRate());
    }

    @Operation(summary = "직업별 승률")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "성공"),
            @ApiResponse(responseCode = "404", description = "유저정보를 찾을수 없음")
    })
    @GetMapping("/jobWinRate")
    public ResponseEntity<Map> getJobWinRate() {
        return ResponseEntity.ok(recordService.getJobWinRate());
    }

}
