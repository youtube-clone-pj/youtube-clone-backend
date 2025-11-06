package com.youtube.live.interaction.livestreaming.controller;

import com.youtube.api.testfixtures.support.TestAuthSupport;
import com.youtube.core.testfixtures.builder.UserBuilder;
import com.youtube.core.channel.domain.Channel;
import com.youtube.live.interaction.config.WebSocketConfig;
import com.youtube.live.interaction.config.WebSocketStompTest;
import com.youtube.live.interaction.livestreaming.controller.dto.ChatMessageRequest;
import com.youtube.live.interaction.livestreaming.controller.dto.ChatMessageResponse;
import com.youtube.live.interaction.livestreaming.domain.ChatMessageType;
import com.youtube.live.interaction.livestreaming.domain.LiveStreaming;
import com.youtube.live.interaction.support.TestStompSession;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static com.youtube.core.testfixtures.builder.ChannelBuilder.*;
import static com.youtube.live.interaction.builder.LiveStreamingBuilder.*;
import static com.youtube.live.interaction.config.WebSocketConfig.Destinations.getRoomTopic;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@Slf4j
class WebSocketChatControllerTest extends WebSocketStompTest {

    @Test
    @DisplayName("채팅 메시지를 전송하면 구독자가 메시지를 수신한다")
    void sendMessageAndReceive() throws ExecutionException, InterruptedException, TimeoutException {
        // given
        Long savedUserId = TestAuthSupport.signUp(
                        "test@example.com",
                        "테스트 유저",
                        "encodedPassword123!")
                        .as(Long.class);
        Channel savedChannel = testSupport.save(
                Channel().withUser(UserBuilder.User().withId(savedUserId).build()).build()
        );
        LiveStreaming savedLiveStreaming = testSupport.save(
                LiveStreaming().withChannel(savedChannel).build()
        );

        final String jsessionId = TestAuthSupport.login("test@example.com", "encodedPassword123!");
        final TestStompSession<ChatMessageResponse> testSession = TestStompSession.connect(wsUrl, jsessionId);

        // 방 구독
        testSession.subscribe(
                getRoomTopic(savedLiveStreaming.getId()),
                ChatMessageResponse.class
        );

        // when
        final ChatMessageRequest request = new ChatMessageRequest();
        request.setMessage("안녕하세요");
        request.setChatMessageType(ChatMessageType.CHAT);

        testSession.send(
                WebSocketConfig.Destinations.APP_PREFIX + "/chat/rooms/" + savedLiveStreaming.getId() + "/messages",
                request
        );

        // then
        await().atMost(Duration.ofSeconds(2))
                .untilAsserted(() -> {
                    final List<ChatMessageResponse> receivedMessages = testSession.getReceivedMessages(getRoomTopic(savedLiveStreaming.getId()));
                    assertThat(receivedMessages).hasSize(1);
                    assertThat(receivedMessages.get(0).getUsername()).isEqualTo("테스트 유저");
                    assertThat(receivedMessages.get(0).getMessage()).isEqualTo("안녕하세요");
                    assertThat(receivedMessages.get(0).getChatMessageType()).isEqualTo(ChatMessageType.CHAT);
                    assertThat(receivedMessages.get(0).getTimestamp()).isNotNull();
                });

        testSession.disconnect();
    }

    @Test
    @DisplayName("여러 클라이언트가 같은 방에서 메시지를 주고받는다")
    void multipleClientsInSameRoom() throws ExecutionException, InterruptedException, TimeoutException {
        // given
        Long user1Id = TestAuthSupport.signUp(
                        "user1@example.com",
                        "유저1",
                        "password1!")
                .as(Long.class);
        Long user2Id = TestAuthSupport.signUp(
                        "user2@example.com",
                        "유저2",
                        "password2!")
                .as(Long.class);

        Channel savedChannel = testSupport.save(
                Channel().withUser(UserBuilder.User().withId(user1Id).build()).build()
        );
        LiveStreaming savedLiveStreaming = testSupport.save(
                LiveStreaming().withChannel(savedChannel).build()
        );

        final String jsessionId1 = TestAuthSupport.login("user1@example.com", "password1!");
        final String jsessionId2 = TestAuthSupport.login("user2@example.com", "password2!");

        final TestStompSession<ChatMessageResponse> session1 = TestStompSession.connect(wsUrl, jsessionId1);
        final TestStompSession<ChatMessageResponse> session2 = TestStompSession.connect(wsUrl, jsessionId2);

        // 두 클라이언트 모두 같은 방 구독
        final String roomTopic = getRoomTopic(savedLiveStreaming.getId());
        session1.subscribe(roomTopic, ChatMessageResponse.class);
        session2.subscribe(roomTopic, ChatMessageResponse.class);

        // when: 클라이언트 1이 메시지 전송
        final ChatMessageRequest request = new ChatMessageRequest();
        request.setMessage("Hello from user1");
        request.setChatMessageType(ChatMessageType.CHAT);

        session1.send(
                WebSocketConfig.Destinations.APP_PREFIX + "/chat/rooms/" + savedLiveStreaming.getId() + "/messages",
                request
        );

        // then: 두 클라이언트 모두 메시지 수신 확인
        await().atMost(Duration.ofSeconds(2))
                .untilAsserted(() -> {
                    final List<ChatMessageResponse> client1Messages = session1.getReceivedMessages(roomTopic);
                    final List<ChatMessageResponse> client2Messages = session2.getReceivedMessages(roomTopic);

                    assertThat(client1Messages).hasSize(1);
                    assertThat(client2Messages).hasSize(1);

                    // 두 클라이언트가 같은 메시지를 받았는지 확인
                    assertThat(client1Messages.get(0).getMessage()).isEqualTo("Hello from user1");
                    assertThat(client2Messages.get(0).getMessage()).isEqualTo("Hello from user1");
                    assertThat(client1Messages.get(0).getUsername()).isEqualTo("유저1");
                    assertThat(client2Messages.get(0).getUsername()).isEqualTo("유저1");
                });

        session1.disconnect();
        session2.disconnect();
    }

    @Test
    @DisplayName("다른 방에 구독한 클라이언트는 메시지를 수신하지 않는다")
    void clientsInDifferentRooms() throws ExecutionException, InterruptedException, TimeoutException {
        // given: 두 개의 라이브 스트리밍 생성
        Long user1Id = TestAuthSupport.signUp(
                        "user1@example.com",
                        "유저1",
                        "password1!")
                .as(Long.class);
        Long user2Id = TestAuthSupport.signUp(
                        "user2@example.com",
                        "유저2",
                        "password2!")
                .as(Long.class);

        Channel channel1 = testSupport.save(
                Channel().withUser(UserBuilder.User().withId(user1Id).build()).build()
        );
        Channel channel2 = testSupport.save(
                Channel().withUser(UserBuilder.User().withId(user2Id).build()).build()
        );

        LiveStreaming liveStreaming1 = testSupport.save(
                LiveStreaming().withChannel(channel1).build()
        );
        LiveStreaming liveStreaming2 = testSupport.save(
                LiveStreaming().withChannel(channel2).build()
        );

        final String jsessionId1 = TestAuthSupport.login("user1@example.com", "password1!");
        final String jsessionId2 = TestAuthSupport.login("user2@example.com", "password2!");

        final TestStompSession<ChatMessageResponse> session1 = TestStompSession.connect(wsUrl, jsessionId1);
        final TestStompSession<ChatMessageResponse> session2 = TestStompSession.connect(wsUrl, jsessionId2);

        // 클라이언트 1은 방 1 구독, 클라이언트 2는 방 2 구독
        final String room1Topic = getRoomTopic(liveStreaming1.getId());
        final String room2Topic = getRoomTopic(liveStreaming2.getId());
        session1.subscribe(room1Topic, ChatMessageResponse.class);
        session2.subscribe(room2Topic, ChatMessageResponse.class);

        // when: 클라이언트 1이 방 1에 메시지 전송
        final ChatMessageRequest request = new ChatMessageRequest();
        request.setMessage("Hello from room 1");
        request.setChatMessageType(ChatMessageType.CHAT);

        session1.send(
                WebSocketConfig.Destinations.APP_PREFIX + "/chat/rooms/" + liveStreaming1.getId() + "/messages",
                request
        );

        // then: 방 1 구독자만 메시지 수신
        await().atMost(Duration.ofSeconds(2))
                .untilAsserted(() -> {
                    final List<ChatMessageResponse> room1Messages = session1.getReceivedMessages(room1Topic);
                    final List<ChatMessageResponse> room2Messages = session2.getReceivedMessages(room2Topic);

                    assertThat(room1Messages).hasSize(1);
                    assertThat(room2Messages).isEmpty(); // 방 2 구독자는 메시지를 받지 않음
                });

        session1.disconnect();
        session2.disconnect();
    }

    @Test
    @DisplayName("세션 정보가 없으면 메시지 전송이 실패하고 브로드캐스트되지 않는다")
    void sendMessageWithoutSession() throws ExecutionException, InterruptedException, TimeoutException {
        // given
        Long userId = TestAuthSupport.signUp(
                        "test@example.com",
                        "테스트 유저",
                        "password!")
                .as(Long.class);

        Channel savedChannel = testSupport.save(
                Channel().withUser(UserBuilder.User().withId(userId).build()).build()
        );
        LiveStreaming savedLiveStreaming = testSupport.save(
                LiveStreaming().withChannel(savedChannel).build()
        );

        // 세션 정보 없이 WebSocket 연결 (jsessionId를 null로 전달)
        final TestStompSession<ChatMessageResponse> testSession = TestStompSession.connect(wsUrl, null);

        // 방 구독
        final String roomTopic = getRoomTopic(savedLiveStreaming.getId());
        testSession.subscribe(roomTopic, ChatMessageResponse.class);

        // when: 메시지 전송 시도
        final ChatMessageRequest request = new ChatMessageRequest();
        request.setMessage("This should not be broadcasted");
        request.setChatMessageType(ChatMessageType.CHAT);

        testSession.send(
                WebSocketConfig.Destinations.APP_PREFIX + "/chat/rooms/" + savedLiveStreaming.getId() + "/messages",
                request
        );

        // then: 메시지가 브로드캐스트되지 않음
        final List<ChatMessageResponse> receivedMessages = testSession.getReceivedMessages(roomTopic);
        assertThat(receivedMessages).isEmpty();

        testSession.disconnect();
    }

    @Test
    @DisplayName("한 방에서 여러 메시지를 연속으로 전송하고 순서대로 수신한다")
    void sendMultipleMessagesInOrder() throws ExecutionException, InterruptedException, TimeoutException {
        // given
        Long userId = TestAuthSupport.signUp(
                        "test@example.com",
                        "테스트 유저",
                        "password!")
                .as(Long.class);

        Channel savedChannel = testSupport.save(
                Channel().withUser(UserBuilder.User().withId(userId).build()).build()
        );
        LiveStreaming savedLiveStreaming = testSupport.save(
                LiveStreaming().withChannel(savedChannel).build()
        );

        final String jsessionId = TestAuthSupport.login("test@example.com", "password!");
        final TestStompSession<ChatMessageResponse> testSession = TestStompSession.connect(wsUrl, jsessionId);

        // 방 구독
        final String roomTopic = getRoomTopic(savedLiveStreaming.getId());
        testSession.subscribe(roomTopic, ChatMessageResponse.class);

        // when: 여러 메시지 연속 전송
        final List<String> messagesToSend = List.of("첫 번째 메시지", "두 번째 메시지", "세 번째 메시지");
        for (final String message : messagesToSend) {
            final ChatMessageRequest request = new ChatMessageRequest();
            request.setMessage(message);
            request.setChatMessageType(ChatMessageType.CHAT);
            testSession.send(
                    WebSocketConfig.Destinations.APP_PREFIX + "/chat/rooms/" + savedLiveStreaming.getId() + "/messages",
                    request
            );
        }

        // then: 모든 메시지를 순서대로 수신
        await().atMost(Duration.ofSeconds(2))
                .untilAsserted(() -> {
                    final List<ChatMessageResponse> receivedMessages = testSession.getReceivedMessages(roomTopic);
                    assertThat(receivedMessages).hasSize(3);
                    assertThat(receivedMessages.get(0).getMessage()).isEqualTo("첫 번째 메시지");
                    assertThat(receivedMessages.get(1).getMessage()).isEqualTo("두 번째 메시지");
                    assertThat(receivedMessages.get(2).getMessage()).isEqualTo("세 번째 메시지");
                });

        testSession.disconnect();
    }
}
