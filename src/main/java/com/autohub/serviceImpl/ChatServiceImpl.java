package com.autohub.serviceImpl;

import com.autohub.configuration.ChatConstants;
import com.autohub.configuration.OnlineUserStore;
import com.autohub.dto.ChatMessageRequest;
import com.autohub.dto.ChatMessageResponse;
import com.autohub.dto.ChatUserResponse;
import com.autohub.entity.*;
import com.autohub.repository.*;
import com.autohub.service.ChatService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.*;



@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final ChatRoomRepository roomRepository;

    private final ChatMessageRepository messageRepository;

    private final AdminRepository adminRepository;

    private final DealerRepository dealerRepository;

    private final CustomerRepository customerRepository;

    private final CustomerLeadRepository customerLeadRepository;

    private final SimpMessagingTemplate messagingTemplate;

    private final OnlineUserStore onlineUserStore;

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

                    ChatRoom room =
                            new ChatRoom();

                    room.setRoomId(roomId);

                    room.setUser1Id(senderId);
                    room.setUser1Role(senderRole);

                    room.setUser2Id(receiverId);
                    room.setUser2Role(receiverRole);

                    room.setUser1Key(
                            senderRole + "_" + senderId
                    );

                    room.setUser2Key(
                            receiverRole + "_" + receiverId
                    );

                    return roomRepository.save(room);
                });
    }

    private void validateChat(
            String senderRole,
            String receiverRole) {

        if ("CUSTOMER".equals(senderRole)
                &&
                "CUSTOMER".equals(receiverRole)) {

            throw new RuntimeException(
                    "Customer to Customer chat not allowed"
            );
        }

        if ("DEALER".equals(senderRole)
                &&
                "DEALER".equals(receiverRole)) {

            throw new RuntimeException(
                    "Dealer to Dealer chat not allowed"
            );
        }

        if ("ADMIN".equals(senderRole)
                ||
                "ADMIN".equals(receiverRole)) {

            throw new RuntimeException(
                    "Admin private chat not allowed"
            );
        }
    }

    @Override
    public void sendMessage(
            Long senderId,
            String senderRole,
            ChatMessageRequest request) {

        validateChat(
                senderRole,
                request.getReceiverRole()
        );

        String roomId =
                generateRoomId(
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

        ChatMessage message =
                new ChatMessage();

        message.setRoomId(roomId);

        message.setSenderId(senderId);

        message.setSenderRole(senderRole);

        message.setSenderName(
                getUserName(
                        senderId,
                        senderRole
                )
        );

        message.setReceiverId(
                request.getReceiverId()
        );

        message.setReceiverRole(
                request.getReceiverRole()
        );

        message.setContent(
                request.getContent()
        );

        message.setSenderKey(
                senderRole + "_" + senderId
        );

        message.setReceiverKey(
                request.getReceiverRole()
                        + "_"
                        + request.getReceiverId()
        );

        ChatMessage saved =
                messageRepository.save(message);

        ChatMessageResponse response =
                ChatMessageResponse.builder()
                        .roomId(saved.getRoomId())
                        .senderId(saved.getSenderId())
                        .senderRole(saved.getSenderRole())
                        .senderName(saved.getSenderName())
                        .receiverId(saved.getReceiverId())
                        .receiverRole(saved.getReceiverRole())
                        .content(saved.getContent())
                        .isRead(saved.getIsRead())
                        .sentAt(saved.getSentAt())
                        .build();

        messagingTemplate.convertAndSend(
                "/queue/"
                        + saved.getReceiverRole()
                        + "_"
                        + saved.getReceiverId(),
                response
        );

        messagingTemplate.convertAndSend(
                "/queue/"
                        + senderRole
                        + "_"
                        + senderId,
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
    public List<ChatUserResponse> getAvailableUsers(
            Long userId,
            String role) {

        List<ChatUserResponse> users =
                new ArrayList<>();

        // ADMIN

        if ("ADMIN".equals(role)) {

            users.add(
                    buildGroupResponse()
            );

            return users;
        }

        // DEALER

        if ("DEALER".equals(role)) {

            users.add(
                    buildGroupResponse()
            );

            customerLeadRepository
                    .findCustomersByDealer(userId)
                    .forEach(customer ->

                            users.add(
                                    buildUserResponse(
                                            userId,
                                            role,
                                            customer.getId(),
                                            "CUSTOMER",
                                            customer.getCustomerName()
                                    )
                            )
                    );

            return users;
        }

        // CUSTOMER

        customerLeadRepository
                .findDealersByCustomer(userId)
                .forEach(dealer ->

                        users.add(
                                buildUserResponse(
                                        userId,
                                        role,
                                        dealer.getId(),
                                        "DEALER",
                                        dealer.getOwnerName()
                                )
                        )
                );

        return users;
    }


    private ChatUserResponse
    buildGroupResponse() {

        return ChatUserResponse.builder()
                .id(0L)
                .group(true)
                .role("GROUP")
                .name(
                        ChatConstants.DEALER_GROUP_NAME
                )
                .chatKey(
                        ChatConstants.DEALER_GROUP_ID
                )
                .online(true)
                .unreadCount(0L)
                .build();
    }

    private ChatUserResponse buildUserResponse(
            Long loginId,
            String loginRole,
            Long targetId,
            String targetRole,
            String targetName) {

        String roomId =
                generateRoomId(
                        loginId,
                        loginRole,
                        targetId,
                        targetRole
                );

        ChatMessage lastMessage =
                messageRepository
                        .findTopByRoomIdOrderBySentAtDesc(
                                roomId
                        )
                        .orElse(null);

        Long unreadCount =
                messageRepository
                        .getUnreadCountForRoom(
                                roomId,
                                loginId,
                                loginRole
                        );

        return ChatUserResponse.builder()
                .id(targetId)
                .name(targetName)
                .role(targetRole)
                .chatKey(
                        targetRole + "_" + targetId
                )
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
                .online(
                        onlineUserStore.isOnline(
                                targetRole + "_" + targetId
                        )
                )
                .unreadCount(unreadCount)
                .group(false)
                .build();
    }

    @Override
    public Long getUnreadCount(
            Long userId,
            String role) {

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
            String receiverRole) {

        messageRepository.markAsRead(
                roomId,
                receiverId,
                receiverRole
        );
    }

    private String getUserName(
            Long id,
            String role) {

        if ("ADMIN".equals(role)) {

            return adminRepository
                    .findById(id)
                    .map(Admin::getFullName)
                    .orElse("ADMIN");
        }

        if ("DEALER".equals(role)) {

            return dealerRepository
                    .findById(id)
                    .map(Dealer::getOwnerName)
                    .orElse("DEALER");
        }

        return customerRepository
                .findById(id)
                .map(Customer::getCustomerName)
                .orElse("CUSTOMER");
    }

    @Override
    public void sendGroupMessage(
            Long senderId,
            String senderRole,
            ChatMessageRequest request) {

        if (!senderRole.equals("ADMIN")
                &&
                !senderRole.equals("DEALER")) {

            throw new RuntimeException(
                    "Group access denied"
            );
        }

        ChatMessage message =
                new ChatMessage();

        message.setGroupMessage(true);

        message.setGroupId(
                ChatConstants.DEALER_GROUP_ID
        );

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

        messagingTemplate.convertAndSend(
                "/topic/dealers-group",
                saved
        );
    }

    @Override
    public List<ChatMessage> getGroupHistory(
            String groupId) {

        return messageRepository
                .findByGroupIdOrderBySentAtAsc(
                        groupId
                );
    }
}
