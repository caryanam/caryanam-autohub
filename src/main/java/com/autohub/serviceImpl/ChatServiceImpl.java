package com.autohub.serviceImpl;

import com.autohub.configuration.OnlineUserStore;
import com.autohub.dto.ChatMessageRequest;
import com.autohub.dto.ChatMessageResponse;
import com.autohub.dto.ChatUserResponse;
import com.autohub.entity.ChatMessage;
import com.autohub.entity.ChatRoom;
import com.autohub.repository.*;
import com.autohub.service.ChatService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class ChatServiceImpl
        implements ChatService {

    private final ChatRoomRepository roomRepository;

    private final ChatMessageRepository messageRepository;

    private final AdminRepository adminRepository;

    private final DealerRepository dealerRepository;

    private final CustomerRepository customerRepository;

    private final SimpMessagingTemplate messagingTemplate;

    private final OnlineUserStore onlineUserStore;


    @Override
    public void sendMessage(
            Long senderId,
            String senderRole,
            ChatMessageRequest request) {

        System.out.println("========== CHAT REQUEST ==========");
        System.out.println("SenderId     = " + senderId);
        System.out.println("SenderRole   = " + senderRole);
        System.out.println("ReceiverId   = " + request.getReceiverId());
        System.out.println("ReceiverRole = " + request.getReceiverRole());
        System.out.println("Content      = " + request.getContent());
        System.out.println("==================================");

        validateChat(
                senderRole,
                request.getReceiverRole()
        );

        String roomId = generateRoomId(
                senderId,
                senderRole,
                request.getReceiverId(),
                request.getReceiverRole()
        );

        createRoomIfNotExists(
                roomId,
                senderId,
                senderRole,
                request.getReceiverId(),
                request.getReceiverRole()
        );

        ChatMessage message = new ChatMessage();

        message.setRoomId(roomId);
        message.setSenderId(senderId);
        message.setSenderRole(senderRole);


        message.setReceiverId(request.getReceiverId());

        message.setSenderName(
                getUserName(
                        senderId,
                        senderRole
                )
        );

        message.setReceiverRole(
                request.getReceiverRole()
        );

        message.setContent(
                request.getContent()
        );

        String senderKey =
                senderRole + "_" + senderId;

        String receiverKey =
                request.getReceiverRole()
                        + "_"
                        + request.getReceiverId();

        message.setSenderKey(senderKey);
        message.setReceiverKey(receiverKey);

        ChatMessage saved =
                messageRepository.save(message);

        ChatMessageResponse response =
                ChatMessageResponse.builder()
                        .roomId(saved.getRoomId())
                        .senderId(saved.getSenderId())
                        .senderRole(saved.getSenderRole())
                        .receiverId(saved.getReceiverId())
                        .receiverRole(saved.getReceiverRole())
                        .content(saved.getContent())
                        .senderName(saved.getSenderName())
                        .isRead(saved.getIsRead())
                        .sentAt(saved.getSentAt())
                        .readAt(saved.getReadAt())
                        .build();

        // Receiver la message
        messagingTemplate.convertAndSend(
                "/queue/"
                        + saved.getReceiverRole()
                        + "_"
                        + saved.getReceiverId(),
                response
        );

        // Sender la pan live message
        messagingTemplate.convertAndSend(
                "/queue/" + senderRole + "_" + senderId,
                response
        );
    }


    @Override
    public List<ChatMessage> getHistory(
            String roomId) {

        return messageRepository
                .findByRoomIdOrderBySentAtAsc(
                        roomId
                );
    }

    @Override
    public String generateRoomId(
            Long senderId,
            String senderRole,
            Long receiverId,
            String receiverRole) {

        List<String> users =
                new ArrayList<>();

        users.add(
                senderRole + "_" + senderId
        );

        users.add(
                receiverRole + "_" + receiverId
        );

        Collections.sort(users);

        return users.get(0)
                + "_"
                + users.get(1);
    }


    private void createRoomIfNotExists(
            String roomId,
            Long senderId,
            String senderRole,
            Long receiverId,
            String receiverRole) {

        roomRepository.findByRoomId(roomId)
                .orElseGet(() -> {

                    ChatRoom room = new ChatRoom();
                    room.setUser1Key(
                            senderRole + "_" + senderId
                    );

                    room.setUser2Key(
                            receiverRole + "_" + receiverId
                    );

                    room.setRoomId(roomId);
                    room.setUser1Id(senderId);
                    room.setUser1Role(senderRole);
                    room.setUser2Id(receiverId);
                    room.setUser2Role(receiverRole);

                    return roomRepository.save(room);
                });
    }


    private void validateChat(
            String senderRole,
            String receiverRole){

        // Customer to Customer Block
        if ("CUSTOMER".equals(senderRole)
                && "CUSTOMER".equals(receiverRole)) {

            throw new RuntimeException(
                    "Customer to Customer chat not allowed"
            );
        }

        // Dealer to Dealer Block
        if ("DEALER".equals(senderRole)
                && "DEALER".equals(receiverRole)) {

            throw new RuntimeException(
                    "Dealer to Dealer chat not allowed"
            );
        }

        // Admin to Dealer Block
        if (
                ("ADMIN".equals(senderRole)
                        && "DEALER".equals(receiverRole))
                        ||
                        ("DEALER".equals(senderRole)
                                && "ADMIN".equals(receiverRole))
        ) {

            throw new RuntimeException(
                    "Admin to Dealer individual chat not allowed. Use CARYANAM DEALERS🚘 Group."
            );
        }

        // Admin to Customer Block
        if (
                ("ADMIN".equals(senderRole)
                        && "CUSTOMER".equals(receiverRole))
                        ||
                        ("CUSTOMER".equals(senderRole)
                                && "ADMIN".equals(receiverRole))
        ) {

            throw new RuntimeException(
                    "Admin to Customer chat not allowed"
            );
        }
    }


    @Override
    public List<ChatUserResponse> getAvailableUsers(
            Long userId,
            String role) {

        List<ChatUserResponse> users =
                new ArrayList<>();

        // ADMIN & DEALER => Group visible
        if ("ADMIN".equals(role)
                || "DEALER".equals(role)) {

            users.add(
                    ChatUserResponse.builder()
                            .id(0L)
                            .name("CARYANAM DEALERS🚘")
                            .role("GROUP")
                            .chatKey("CARYANAM DEALERS🚘")
                            .online(true)
                            .unreadCount(0L)
                            .build()
            );
        }

        // ADMIN => ONLY GROUP
        if ("ADMIN".equals(role)) {

            return users;
        }

        // DEALER => GROUP + CUSTOMERS ONLY
        else if ("DEALER".equals(role)) {

            customerRepository.findAll()
                    .forEach(customer ->
                            users.add(
                                    buildUserResponse(
                                            userId,
                                            role,
                                            customer.getId(),
                                            "CUSTOMER",
                                            customer.getCustomerName()
                                    )
                            ));
        }


        // CUSTOMER => DEALERS ONLY
        else if ("CUSTOMER".equals(role)) {

            dealerRepository.findAll()
                    .forEach(dealer ->
                            users.add(
                                    buildUserResponse(
                                            userId,
                                            role,
                                            dealer.getId(),
                                            "DEALER",
                                            dealer.getOwnerName()
                                    )
                            ));
        }

        users.sort(
                Comparator.comparing(
                        ChatUserResponse::getLastMessageAt,
                        Comparator.nullsLast(
                                Comparator.reverseOrder()
                        )
                )
        );

        return users;
    }

//    @Override
//    public List<ChatUserResponse> getAvailableUsers(
//            Long userId,
//            String role) {
//
//        List<ChatUserResponse> users =
//                new ArrayList<>();
//
//        if ("ADMIN".equals(role)) {
//
//            dealerRepository.findAll()
//                    .forEach(dealer ->
//                            users.add(
//                                    buildUserResponse(
//                                            userId,
//                                            role,
//                                            dealer.getId(),
//                                            "DEALER",
//                                            dealer.getOwnerName()
//                                    )
//                            ));
//
//            customerRepository.findAll()
//                    .forEach(customer ->
//                            users.add(
//                                    buildUserResponse(
//                                            userId,
//                                            role,
//                                            customer.getId(),
//                                            "CUSTOMER",
//                                            customer.getCustomerName()
//                                    )
//                            ));
//        }
//
//        else if ("DEALER".equals(role)) {
//
//            adminRepository.findAll()
//                    .forEach(admin ->
//                            users.add(
//                                    buildUserResponse(
//                                            userId,
//                                            role,
//                                            admin.getAdminId(),
//                                            "ADMIN",
//                                            admin.getFullName()
//                                    )
//                            ));
//
//            dealerRepository.findAll()
//                    .stream()
//                    .filter(d -> !d.getId().equals(userId))
//                    .forEach(dealer ->
//                            users.add(
//                                    buildUserResponse(
//                                            userId,
//                                            role,
//                                            dealer.getId(),
//                                            "DEALER",
//                                            dealer.getOwnerName()
//                                    )
//                            ));
//
//            customerRepository.findAll()
//                    .forEach(customer ->
//                            users.add(
//                                    buildUserResponse(
//                                            userId,
//                                            role,
//                                            customer.getId(),
//                                            "CUSTOMER",
//                                            customer.getCustomerName()
//                                    )
//                            ));
//        }
//
//        else if ("CUSTOMER".equals(role)) {
//
//            adminRepository.findAll()
//                    .forEach(admin ->
//                            users.add(
//                                    buildUserResponse(
//                                            userId,
//                                            role,
//                                            admin.getAdminId(),
//                                            "ADMIN",
//                                            admin.getFullName()
//                                    )
//                            ));
//
//            dealerRepository.findAll()
//                    .forEach(dealer ->
//                            users.add(
//                                    buildUserResponse(
//                                            userId,
//                                            role,
//                                            dealer.getId(),
//                                            "DEALER",
//                                            dealer.getOwnerName()
//                                    )
//                            ));
//        }
//
//        users.sort(
//                Comparator.comparing(
//                        ChatUserResponse::getLastMessageAt,
//                        Comparator.nullsLast(
//                                Comparator.reverseOrder()
//                        )
//                )
//        );
//
//        return users;
//    }

    @Override
    public Long getUnreadCount(
            Long userId,
            String role){

        return messageRepository
                .countByReceiverIdAndReceiverRoleAndIsReadFalse(
                        userId,
                        role
                );
    }

    @Transactional
    @Override
    public void markAsRead(
            String roomId,
            Long receiverId,
            String receiverRole){
        System.out.println("MARK AS READ");
        System.out.println(roomId);
        System.out.println(receiverId);
        System.out.println(receiverRole);


        int updated = messageRepository.markAsRead(
                roomId,
                receiverId,
                receiverRole
        );

        System.out.println("UPDATED = " + updated);

        if(updated > 0){

            ChatMessage lastMessage =
                    messageRepository
                            .findByRoomIdOrderBySentAtDesc(roomId)
                            .get(0);

            messagingTemplate.convertAndSend(
                    "/queue/"
                            + lastMessage.getSenderRole()
                            + "_"
                            + lastMessage.getSenderId()
                            + "_read",
                    roomId
            );
        }


    }

    private ChatUserResponse buildUserResponse(
            Long loggedInUserId,
            String loggedInRole,
            Long targetId,
            String targetRole,
            String targetName) {

        String roomId = generateRoomId(
                loggedInUserId,
                loggedInRole,
                targetId,
                targetRole
        );

        ChatMessage lastMessage =
                messageRepository
                        .findTopByRoomIdOrderBySentAtDesc(roomId)
                        .orElse(null);

        Long unreadCount =
                messageRepository.getUnreadCountForRoom(
                        roomId,
                        loggedInUserId,
                        loggedInRole
                );

        Boolean online =
                onlineUserStore.isOnline(
                        targetRole + "_" + targetId
                );

        return ChatUserResponse.builder()
                .id(targetId)
                .name(targetName)
                .role(targetRole)
                .chatKey(targetRole + "_" + targetId)
                .lastMessage(
                        lastMessage != null
                                ? lastMessage.getContent()
                                : null
                )
                .lastMessageAt(
                        lastMessage != null
                                ? lastMessage.getSentAt()
                                : null
                )
                .unreadCount(unreadCount)
                .online(online)
                .build();
    }



    @Override
    public void sendGroupMessage(
            Long senderId,
            String senderRole,
            String content) {

        if (!senderRole.equals("ADMIN")
                &&
                !senderRole.equals("DEALER")) {

            throw new RuntimeException(
                    "Only Admin and Dealer allowed"
            );
        }

        ChatMessage message =
                new ChatMessage();

        message.setGroupMessage(true);

        message.setGroupId("CARYANAM DEALERS🚘");

        message.setSenderId(senderId);

        message.setSenderRole(senderRole);

        message.setContent(content);

        ChatMessage saved =
                messageRepository.save(message);

        ChatMessageResponse response =
                ChatMessageResponse.builder()
                        .senderId(saved.getSenderId())
                        .senderRole(saved.getSenderRole())
                        .content(saved.getContent())
                        .sentAt(saved.getSentAt())
                        .build();

//        messagingTemplate.convertAndSend(
//                "/queue/group/all-dealers",
//                response
//        );
        messagingTemplate.convertAndSend(
                "/topic/all-dealers-group",
                response
        );
    }

    @Override
    public List<ChatMessage> getGroupHistory() {

        return messageRepository
                .findByGroupIdOrderBySentAtAsc("CARYANAM DEALERS🚘");
    }

    @Override
    public List<ChatMessage> getGroupHistory(
            String groupId) {

        return messageRepository
                .findByGroupIdOrderBySentAtAsc(
                        groupId
                );
    }

    private String getUserName(
            Long userId,
            String role) {

        if ("ADMIN".equals(role)) {

            return adminRepository
                    .findById(userId)
                    .map(a -> a.getFullName())
                    .orElse("ADMIN");
        }

        if ("DEALER".equals(role)) {

            return dealerRepository
                    .findById(userId)
                    .map(d -> d.getOwnerName())
                    .orElse("DEALER");
        }

        return customerRepository
                .findById(userId)
                .map(c -> c.getCustomerName())
                .orElse("CUSTOMER");
    }

    @Override
    public void sendGroupMessage(
            Long senderId,
            String senderRole,
            ChatMessageRequest request) {

        ChatMessage message =
                new ChatMessage();

        message.setGroupMessage(true);

        message.setGroupId("CARYANAM DEALERS🚘");

        message.setRoomId("CARYANAM DEALERS🚘");

        message.setSenderId(senderId);

        message.setSenderRole(senderRole);

        message.setSenderName(
                getUserName(
                        senderId,
                        senderRole
                )
        );

        message.setContent(
                request.getContent()
        );

        ChatMessage saved =
                messageRepository.save(message);

        ChatMessageResponse response =
                ChatMessageResponse.builder()
                        .roomId(saved.getRoomId())
                        .senderId(saved.getSenderId())
                        .senderRole(saved.getSenderRole())
                        .senderName(saved.getSenderName())
                        .content(saved.getContent())
                        .sentAt(saved.getSentAt())
                        .build();

        messagingTemplate.convertAndSend(
                "/queue/all-dealers-group",
                response
        );
    }
}
