package com.youtube.live.interaction.livestreaming.controller;

import com.youtube.api.testfixtures.support.TestAuthSupport;
import com.youtube.core.testfixtures.builder.UserBuilder;
import com.youtube.core.channel.domain.Channel;
import com.youtube.core.user.domain.User;
import com.youtube.live.interaction.config.WebSocketConfig;
import com.youtube.live.interaction.config.WebSocketStompTest;
import com.youtube.live.interaction.livestreaming.controller.dto.ChatMessageRequest;
import com.youtube.live.interaction.livestreaming.repository.dto.ChatMessageResponse;
import com.youtube.live.interaction.livestreaming.controller.dto.ErrorResponse;
import com.youtube.live.interaction.livestreaming.controller.dto.InitialChatMessagesResponse;
import com.youtube.live.interaction.livestreaming.domain.ChatMessageType;
import com.youtube.live.interaction.livestreaming.domain.LiveStreaming;
import com.youtube.live.interaction.support.TestStompSession;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static com.youtube.core.testfixtures.builder.ChannelBuilder.*;
import static com.youtube.core.testfixtures.builder.UserBuilder.User;
import static com.youtube.live.interaction.builder.LiveStreamingBuilder.*;
import static com.youtube.live.interaction.builder.LiveStreamingChatBuilder.LiveStreamingChat;
import static com.youtube.live.interaction.config.WebSocketConfig.Destinations.getChatLivestreamMessagesTopic;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

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

        // 라이브 스트리밍 구독
        testSession.subscribe(
                getChatLivestreamMessagesTopic(savedLiveStreaming.getId()),
                ChatMessageResponse.class
        );

        // when
        final ChatMessageRequest request = new ChatMessageRequest();
        request.setMessage("안녕하세요");
        request.setChatMessageType(ChatMessageType.CHAT);

        testSession.send(
                WebSocketConfig.Destinations.APP_PREFIX + "/livestreams/" + savedLiveStreaming.getId() + "/chat/messages",
                request
        );

        // then
        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    final List<ChatMessageResponse> receivedMessages = testSession.getReceivedMessages(getChatLivestreamMessagesTopic(savedLiveStreaming.getId()));
                    assertThat(receivedMessages).hasSize(1);
                    assertThat(receivedMessages.get(0).getUsername()).isEqualTo("테스트 유저");
                    assertThat(receivedMessages.get(0).getMessage()).isEqualTo("안녕하세요");
                    assertThat(receivedMessages.get(0).getChatMessageType()).isEqualTo(ChatMessageType.CHAT);
                    assertThat(receivedMessages.get(0).getTimestamp()).isNotNull();
                });

        testSession.disconnect();
    }

    @Test
    @DisplayName("여러 클라이언트가 같은 라이브 스트리밍에서 메시지를 주고받는다")
    void multipleClientsInSameLivestream() throws ExecutionException, InterruptedException, TimeoutException {
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

        // 두 클라이언트 모두 같은 라이브 스트리밍 구독
        final String livestreamTopic = getChatLivestreamMessagesTopic(savedLiveStreaming.getId());
        session1.subscribe(livestreamTopic, ChatMessageResponse.class);
        session2.subscribe(livestreamTopic, ChatMessageResponse.class);

        // when: 클라이언트 1이 메시지 전송
        final ChatMessageRequest request = new ChatMessageRequest();
        request.setMessage("Hello from user1");
        request.setChatMessageType(ChatMessageType.CHAT);

        session1.send(
                WebSocketConfig.Destinations.APP_PREFIX + "/livestreams/" + savedLiveStreaming.getId() + "/chat/messages",
                request
        );

        // then: 두 클라이언트 모두 메시지 수신 확인
        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    final List<ChatMessageResponse> client1Messages = session1.getReceivedMessages(livestreamTopic);
                    final List<ChatMessageResponse> client2Messages = session2.getReceivedMessages(livestreamTopic);

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
    @DisplayName("다른 라이브 스트리밍에 구독한 클라이언트는 메시지를 수신하지 않는다")
    void clientsInDifferentLivestreams() throws ExecutionException, InterruptedException, TimeoutException {
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

        // 클라이언트 1은 라이브 스트리밍 1 구독, 클라이언트 2는 라이브 스트리밍 2 구독
        final String livestream1Topic = getChatLivestreamMessagesTopic(liveStreaming1.getId());
        final String livestream2Topic = getChatLivestreamMessagesTopic(liveStreaming2.getId());
        session1.subscribe(livestream1Topic, ChatMessageResponse.class);
        session2.subscribe(livestream2Topic, ChatMessageResponse.class);

        // when: 클라이언트 1이 라이브 스트리밍 1에 메시지 전송
        final ChatMessageRequest request = new ChatMessageRequest();
        request.setMessage("Hello from livestream 1");
        request.setChatMessageType(ChatMessageType.CHAT);

        session1.send(
                WebSocketConfig.Destinations.APP_PREFIX + "/livestreams/" + liveStreaming1.getId() + "/chat/messages",
                request
        );

        // then: 라이브 스트리밍 1 구독자만 메시지 수신
        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    final List<ChatMessageResponse> livestream1Messages = session1.getReceivedMessages(livestream1Topic);
                    final List<ChatMessageResponse> livestream2Messages = session2.getReceivedMessages(livestream2Topic);

                    assertThat(livestream1Messages).hasSize(1);
                    assertThat(livestream2Messages).isEmpty(); // 라이브 스트리밍 2 구독자는 메시지를 받지 않음
                });

        session1.disconnect();
        session2.disconnect();
    }

    @Test
    @DisplayName("인증되지 않은 사용자도 WebSocket 연결에 성공한다")
    void connectWithoutSessionSucceeds() throws ExecutionException, InterruptedException, TimeoutException {
        // given & when
        final TestStompSession<ChatMessageResponse> testSession = TestStompSession.connect(wsUrl, null);
        // then
        assertThat(testSession).isNotNull();

        testSession.disconnect();
    }

    @Test
    @DisplayName("한 라이브 스트리밍에서 여러 메시지를 연속으로 전송하고 순서대로 수신한다")
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

        // 라이브 스트리밍 구독
        final String livestreamTopic = getChatLivestreamMessagesTopic(savedLiveStreaming.getId());
        testSession.subscribe(livestreamTopic, ChatMessageResponse.class);

        // when: 여러 메시지 연속 전송
        final List<String> messagesToSend = List.of("첫 번째 메시지", "두 번째 메시지", "세 번째 메시지");
        for (final String message : messagesToSend) {
            final ChatMessageRequest request = new ChatMessageRequest();
            request.setMessage(message);
            request.setChatMessageType(ChatMessageType.CHAT);
            testSession.send(
                    WebSocketConfig.Destinations.APP_PREFIX + "/livestreams/" + savedLiveStreaming.getId() + "/chat/messages",
                    request
            );
        }

        // then: 모든 메시지를 순서대로 수신
        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    final List<ChatMessageResponse> receivedMessages = testSession.getReceivedMessages(livestreamTopic);
                    assertThat(receivedMessages).hasSize(3);
                    assertThat(receivedMessages.get(0).getMessage()).isEqualTo("첫 번째 메시지");
                    assertThat(receivedMessages.get(1).getMessage()).isEqualTo("두 번째 메시지");
                    assertThat(receivedMessages.get(2).getMessage()).isEqualTo("세 번째 메시지");
                });

        testSession.disconnect();
    }

    @Test
    @DisplayName("존재하지 않는 라이브 스트리밍에 메시지 전송 시 에러 응답을 받는다")
    void sendMessageToNonExistentLivestream() throws ExecutionException, InterruptedException, TimeoutException {
        // given
        Long userId = TestAuthSupport.signUp(
                        "test@example.com",
                        "테스트 유저",
                        "password!")
                .as(Long.class);

        final String jsessionId = TestAuthSupport.login("test@example.com", "password!");
        final TestStompSession<ErrorResponse> testSession = TestStompSession.connect(wsUrl, jsessionId);

        // 에러 큐 구독 (@SendToUser는 /user prefix가 자동으로 붙음)
        testSession.subscribe("/user/queue/errors", ErrorResponse.class);

        // when: 존재하지 않는 livestreamId로 메시지 전송
        final Long nonExistentLivestreamId = 999999L;
        final ChatMessageRequest request = new ChatMessageRequest();
        request.setMessage("This should fail");
        request.setChatMessageType(ChatMessageType.CHAT);

        testSession.send(
                WebSocketConfig.Destinations.APP_PREFIX + "/livestreams/" + nonExistentLivestreamId + "/chat/messages",
                request
        );

        // then: 에러 응답을 받음
        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    final List<ErrorResponse> errors = testSession.getReceivedMessages("/user/queue/errors");
                    assertThat(errors).hasSize(1);
                    assertThat(errors.get(0).getMessage()).isNotNull();
                    assertThat(errors.get(0).getTimestamp()).isNotNull();
                });

        testSession.disconnect();
    }

    @Test
    @DisplayName("broadcast=false: 에러가 발생한 세션만 에러 메시지를 받는다")
    void errorMessageSentToOnlyTheSessionThatCausedError() throws ExecutionException, InterruptedException, TimeoutException {
        // given: 같은 사용자로 두 개의 WebSocket 세션 연결
        TestAuthSupport.signUp(
                        "test@example.com",
                        "테스트 유저",
                        "password!")
                .as(Long.class);

        final String jsessionId1 = TestAuthSupport.login("test@example.com", "password!");
        final String jsessionId2 = TestAuthSupport.login("test@example.com", "password!");

        final TestStompSession<ErrorResponse> session1 = TestStompSession.connect(wsUrl, jsessionId1);
        final TestStompSession<ErrorResponse> session2 = TestStompSession.connect(wsUrl, jsessionId2);

        // 두 세션 모두 에러 큐 구독
        final String errorQueue = "/user/queue/errors";
        session1.subscribe(errorQueue, ErrorResponse.class);
        session2.subscribe(errorQueue, ErrorResponse.class);

        // when: 세션1에서만 에러를 발생시킴 (존재하지 않는 라이브 스트리밍에 메시지 전송)
        final Long nonExistentLivestreamId = 999999L;
        final ChatMessageRequest request = new ChatMessageRequest();
        request.setMessage("This should fail");
        request.setChatMessageType(ChatMessageType.CHAT);

        session1.send(
                WebSocketConfig.Destinations.APP_PREFIX + "/livestreams/" + nonExistentLivestreamId + "/chat/messages",
                request
        );

        // then: 세션1만 에러 메시지를 받고, 세션2는 받지 않음 (broadcast=false)
        await().atMost(Duration.ofSeconds(2))
                .untilAsserted(() -> {
                    final List<ErrorResponse> session1Errors = session1.getReceivedMessages(errorQueue);
                    final List<ErrorResponse> session2Errors = session2.getReceivedMessages(errorQueue);

                    // 세션1은 에러를 받음
                    assertThat(session1Errors).hasSize(1);
                    assertThat(session1Errors.get(0).getMessage()).isNotNull();

                    // 세션2는 에러를 받지 않음 (broadcast=false이므로)
                    assertThat(session2Errors).isEmpty();
                });

        session1.disconnect();
        session2.disconnect();
    }

    @Test
    @DisplayName("채팅방 입장 시 최근 대화 내용을 오래된 것부터 순서대로 받는다")
    void subscribeToChat_ReceivesInitialMessages() throws ExecutionException, InterruptedException, TimeoutException {
        // given
        final User user = testSupport.save(User().withEmail("test1@example.com").build());
        final Channel channel = testSupport.save(Channel().withUser(user).build());
        final LiveStreaming liveStreaming = testSupport.save(LiveStreaming().withChannel(channel).build());

        // 채팅 메시지 5개 저장
        for (int i = 1; i <= 5; i++) {
            testSupport.save(
                    LiveStreamingChat()
                            .withLiveStreaming(liveStreaming)
                            .withUser(user)
                            .withMessage("초기 메시지 " + i)
                            .build()
            );
        }

        // when & then: 회원이 채팅 구독
        TestAuthSupport.signUp("test2@example.com", "테스트 유저2", "password!");
        final String jsessionId = TestAuthSupport.login("test2@example.com", "password!");
        final TestStompSession<InitialChatMessagesResponse> memberSession = TestStompSession.connect(wsUrl, jsessionId);

        memberSession.subscribe(
                WebSocketConfig.Destinations.APP_PREFIX + "/livestreams/" + liveStreaming.getId() + "/chat/messages",
                InitialChatMessagesResponse.class
        );

        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    final List<InitialChatMessagesResponse> receivedResponses = memberSession.getReceivedMessages(
                            WebSocketConfig.Destinations.APP_PREFIX + "/livestreams/" + liveStreaming.getId() + "/chat/messages"
                    );

                    final List<ChatMessageResponse> messages = receivedResponses.get(0).messages();
                    assertThat(messages).hasSize(5);
                    assertThat(messages.get(0).getMessage()).isEqualTo("초기 메시지 1");
                    assertThat(messages.get(4).getMessage()).isEqualTo("초기 메시지 5");
                });

        memberSession.disconnect();

        // when & then: 비회원이 채팅 구독
        final TestStompSession<InitialChatMessagesResponse> guestSession = TestStompSession.connect(wsUrl, null);

        guestSession.subscribe(
                WebSocketConfig.Destinations.APP_PREFIX + "/livestreams/" + liveStreaming.getId() + "/chat/messages",
                InitialChatMessagesResponse.class
        );

        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    final List<InitialChatMessagesResponse> receivedResponses = guestSession.getReceivedMessages(
                            WebSocketConfig.Destinations.APP_PREFIX + "/livestreams/" + liveStreaming.getId() + "/chat/messages"
                    );

                    final List<ChatMessageResponse> messages = receivedResponses.get(0).messages();
                    assertThat(messages).hasSize(5);
                    assertThat(messages.get(0).getMessage()).isEqualTo("초기 메시지 1");
                    assertThat(messages.get(4).getMessage()).isEqualTo("초기 메시지 5");
                });

        guestSession.disconnect();
    }
}
