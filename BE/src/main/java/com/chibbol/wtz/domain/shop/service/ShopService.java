package com.chibbol.wtz.domain.shop.service;

import com.chibbol.wtz.domain.point.entity.Point;
import com.chibbol.wtz.domain.point.repository.PointRepository;
import com.chibbol.wtz.domain.shop.dto.BuyItemListDTO;
import com.chibbol.wtz.domain.shop.dto.ItemDTO;
import com.chibbol.wtz.domain.shop.dto.ItemListDTO;
import com.chibbol.wtz.domain.shop.entity.Item;
import com.chibbol.wtz.domain.shop.entity.UserItem;
import com.chibbol.wtz.domain.shop.exception.AlreadyPurchasedException;
import com.chibbol.wtz.domain.shop.exception.InsufficientPointsException;
import com.chibbol.wtz.domain.shop.repository.ItemRepository;
import com.chibbol.wtz.domain.shop.repository.UserItemRepository;
import com.chibbol.wtz.domain.user.entity.User;
import com.chibbol.wtz.domain.user.exception.UserNotFoundException;
import com.chibbol.wtz.domain.user.repository.UserRepository;
import com.chibbol.wtz.domain.user.service.UserService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@AllArgsConstructor
public class ShopService {

    private final String IMAGE_PATH = "static/item_images/";

    private final UserService userService;

    private final UserRepository userRepository;
    private final ItemRepository itemRepository;
    private final PointRepository pointRepository;
    private final UserItemRepository userItemRepository;


    public List<ItemDTO> getItems(String itemType) {
        User user = userService.getLoginUser();
        if(user == null) {
            throw new UserNotFoundException("유저를 찾을 수 없습니다.");
        }

        logGetItem(user.getEmail(), itemType);

        return itemsToItemListDTOs(itemRepository.findAllByType(itemType), itemType, user);
    }

    public List<ItemDTO> getEquippedItems() {
        User user = userService.getLoginUser();
        if(user == null) {
            throw new UserNotFoundException("유저를 찾을 수 없습니다.");
        }

        List<UserItem> userEquippedItems = userItemRepository.findAllByUserAndEquipped(user, true).orElse(new ArrayList<>());

        return userItemsToItemListDTOs(userEquippedItems);
    }

    public List<ItemDTO> getEquippedItemsByUserSeq(Long userSeq) {
        User user = userRepository.findByUserSeq(userSeq);
        if(user == null) {
            throw new UserNotFoundException("유저를 찾을 수 없습니다.");
        }

        List<UserItem> userEquippedItems = userItemRepository.findAllByUserAndEquipped(user, true).orElse(new ArrayList<>());

        return userItemsToItemListDTOs(userEquippedItems);
    }

    public void equipItem(ItemListDTO equippedItemListDto) {
        User user = userService.getLoginUser();
        if (user == null) {
            throw new UserNotFoundException("유저를 찾을 수 없습니다.");
        }

        List<UserItem> userItems = userItemRepository.findAllByUser(user).orElse(new ArrayList<>());
        List<Long> equippedItemList = equippedItemListDto.getItems().stream()
                .map(ItemDTO::getItemSeq)
                .collect(Collectors.toList());

        List<UserItem> changeUserItems = new ArrayList<>();

        for (UserItem userItem : userItems) {
            boolean isEquipped = userItem.isEquipped();
            boolean shouldEquip = equippedItemList.contains(userItem.getItem().getItemSeq());

            if (isEquipped != shouldEquip) {
                if (shouldEquip) {
                    userItem.equip();
                } else {
                    userItem.unequip();
                }
                changeUserItems.add(userItem);
            }
        }

        if (!changeUserItems.isEmpty()) {
            userItemRepository.saveAll(changeUserItems);
        }

        logItemEquipped(user.getEmail(), equippedItemList);
    }



    public ItemDTO getGif(String itemName) {
        try {
            // 이미지를 byte[]로 변환
            Path imageFilePath = Paths.get("static/item_gifs/rabbit/").resolve(itemName);
            Resource resource = new ClassPathResource(imageFilePath.toString());

            byte[] imageData = getImageData(resource);

            return ItemDTO.builder()
                    .itemSeq(0L)
                    .price(0)
                    .image(imageData)
                    .build();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }


    public void buyItem(BuyItemListDTO buyItemListDTO) {
        User user = userService.getLoginUser();
        if (user == null) {
            throw new UserNotFoundException("유저를 찾을 수 없습니다.");
        }

        int totalPrice = 0;
        List<Item> itemsToPurchase = new ArrayList<>();

        for (Long itemSeq : buyItemListDTO.getItems()) {
            Item item = getItemById(itemSeq);
            checkIfItemAlreadyPurchased(user, item);

            itemsToPurchase.add(item);
            totalPrice += item.getPrice();
        }

        checkAndDeductPoints(user, totalPrice);

        savePurchasedItems(user, itemsToPurchase);

        logItemPurchased(user.getEmail(), itemsToPurchase);
    }

    private Item getItemById(Long itemSeq) {
        return itemRepository.findById(itemSeq)
                .orElseThrow(() -> new RuntimeException("아이템을 찾을 수 없습니다."));
    }

    private void checkIfItemAlreadyPurchased(User user, Item item) {
        if (userItemRepository.countByUserAndItem(user, item) != 0) {
            throw new AlreadyPurchasedException("이미 구매한 아이템입니다.");
        }
    }

    private void checkAndDeductPoints(User user, int totalPrice) {
        Point point = pointRepository.findByUserUserSeq(user.getUserSeq())
                .orElse(Point.builder().user(user).point(0).build());

        if (totalPrice > point.getPoint()) {
            throw new InsufficientPointsException("포인트가 부족합니다.");
        }

        point.usePoint(totalPrice);
    }

    private void savePurchasedItems(User user, List<Item> items) {
        List<UserItem> userItems = items.stream()
                .map(item -> UserItem.builder().user(user).item(item).build())
                .collect(Collectors.toList());

        userItemRepository.saveAll(userItems);
    }

    public void addPoint(Long userSeq, int point) {
        User user = userRepository.findById(userSeq).orElseThrow(() -> new UserNotFoundException("유저를 찾을 수 없습니다."));
        Point userPoint = pointRepository.findByUserUserSeq(userSeq).orElse(Point.builder().user(user).point(0).build());
        userPoint.addPoint(point);
        pointRepository.save(userPoint);
    }

    public List<ItemDTO> itemsToItemListDTOs(List<Item> items, String itemType, User user) {
        List<ItemDTO> itemDTOS = new ArrayList<>();
        List<UserItem> userItems = userItemRepository.findAllByUser(user).orElse(new ArrayList<>());
        for (Item item : items) {
            ItemDTO itemDTO = createItemDTO(item, itemType, userItems);
            itemDTOS.add(itemDTO);
        }
        return itemDTOS;
    }

    private ItemDTO createItemDTO(Item item, String itemType, List<UserItem> userItems) {
        try {
            Path imageFilePath = Paths.get(IMAGE_PATH + itemType).resolve(item.getImage());
            Resource resource = new ClassPathResource(imageFilePath.toString());
            byte[] imageData = getImageData(resource);

            boolean isSold = userItems.stream().anyMatch(userItem -> userItem.getItem().getItemSeq().equals(item.getItemSeq()));

            return ItemDTO.builder()
                    .itemSeq(item.getItemSeq())
                    .price(item.getPrice())
                    .image(imageData)
                    .isSold(isSold)
                    .build();
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("이미지 변환 중 오류가 발생했습니다.");
        }
    }

    private List<ItemDTO> userItemsToItemListDTOs(List<UserItem> items) {
        List<ItemDTO> itemDTOS = new ArrayList<>();
        for (UserItem userItem : items) {
            ItemDTO itemDTO = createUserItemDTO(userItem);
            itemDTOS.add(itemDTO);
        }
        return itemDTOS;
    }

    private ItemDTO createUserItemDTO(UserItem userItem) {
        try {
            Path imageFilePath = Paths.get(IMAGE_PATH + userItem.getItem().getType()).resolve(userItem.getItem().getImage());
            Resource resource = new ClassPathResource(imageFilePath.toString());
            byte[] imageData = getImageData(resource);

            return ItemDTO.builder()
                    .itemSeq(userItem.getItem().getItemSeq())
                    .price(userItem.getItem().getPrice())
                    .image(imageData)
                    .isSold(true)
                    .build();
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("이미지 변환 중 오류가 발생했습니다.");
        }
    }

    private byte[] getImageData(Resource resource) throws IOException {
        if (resource != null) {
            return resource.getInputStream().readAllBytes();
        }
        return null;
    }

    private void logGetItem(String email, String itemType) {
        log.info("==================================");
        log.info("ITEM GET");
        log.info("USER : " + email);
        log.info("ITEM TYPE : " + itemType);
        log.info("==================================");
    }

    private void logItemEquipped(String email, List<Long> equippedItemList) {
        log.info("==================================");
        log.info("ITEM EQUIPPED");
        log.info("USER : " + email);
        log.info("ITEMS : " + equippedItemList);
        log.info("==================================");
    }

    private void logItemPurchased(String email, List<Item> items) {
        log.info("==================================");
        log.info("ITEM PURCHASED");
        log.info("USER : " + email);
        log.info("ITEMS : " + items);
        log.info("==================================");
    }
}
