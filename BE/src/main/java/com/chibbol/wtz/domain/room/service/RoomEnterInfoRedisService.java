package com.chibbol.wtz.domain.room.service;

import com.chibbol.wtz.domain.room.dto.CurrentSeatsDTO;
import com.chibbol.wtz.domain.room.dto.CurrentSeatsDTOList;
import com.chibbol.wtz.domain.room.exception.SeatNotFoundException;
import com.chibbol.wtz.domain.room.repository.RoomEnterRedisRepository;
import com.chibbol.wtz.domain.shop.service.ShopService;
import com.chibbol.wtz.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class RoomEnterInfoRedisService {
    private final RoomEnterRedisRepository roomEnterRedisRepository;
    private final ShopService shopService;

    public void createCurrentSeat(String roomCode, int maxUserNum) {
        roomEnterRedisRepository.createCurrentSeat(roomCode, maxUserNum);
    }

    public CurrentSeatsDTO enterUser(String roomCode, User user) {
        CurrentSeatsDTO currentSeatsDTO = roomEnterRedisRepository.enterUser(roomCode, user, shopService.getEquippedItemsByUserSeq(user.getUserSeq()));
     
        if (currentSeatsDTO == null) {
            throw new SeatNotFoundException("빈 자리가 없습니다!");
        }

        log.info(currentSeatsDTO.toString()); //
        return currentSeatsDTO;
    }

    public void setUserExitInfo(String roomCode, Long userSeq) {
        roomEnterRedisRepository.setUserExitInfo(roomCode, userSeq);
    }

    public void updateCurrentSeatsDTO(String roomCode, CurrentSeatsDTOList currentSeatsDTOList) {
        roomEnterRedisRepository.updateCurrentSeat(roomCode, currentSeatsDTOList);
    }

    public boolean increaseUserCount(String roomCode) {
        return roomEnterRedisRepository.increaseUserCount(roomCode);
    }

    public boolean decreaseUserCount(String roomCode) {
        return roomEnterRedisRepository.decreaseUserCount(roomCode);
    }

    public int getMaxUserNum(String roomCode) {
        return roomEnterRedisRepository.getMaxUserNum(roomCode);
    }

    public List<CurrentSeatsDTO> getUserEnterInfo(String roomCode) {
        return roomEnterRedisRepository.getUserEnterInfo(roomCode);
    }

    public int getUsingSeats(String roomCode) {
        return roomEnterRedisRepository.getUsingSeats(roomCode);
    }

    public List<CurrentSeatsDTO> toCurrentSeatsDTO(List<Object> jsonList) {
        return roomEnterRedisRepository.toCurrentSeatsDTO(jsonList);
    }
}
