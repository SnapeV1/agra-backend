package org.agra.agra_backend.config;

import org.agra.agra_backend.misc.JwtUtil;
import org.agra.agra_backend.service.PresenceService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebSocketAuthChannelInterceptorTest {

    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private PresenceService presenceService;

    @Test
    void preSendNoopsWhenAccessorMissing() {
        WebSocketAuthChannelInterceptor interceptor = new WebSocketAuthChannelInterceptor(jwtUtil, presenceService);
        Message<?> message = MessageBuilder.withPayload("payload").build();
        MessageChannel channel = mock(MessageChannel.class);

        Message<?> result = interceptor.preSend(message, channel);

        assertThat(result).isSameAs(message);
        verifyNoInteractions(presenceService);
    }

    @Test
    void preSendConnectSetsUserAndMarksOnline() {
        WebSocketAuthChannelInterceptor interceptor = new WebSocketAuthChannelInterceptor(jwtUtil, presenceService);
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setLeaveMutable(true);
        accessor.setSessionId("session-1");
        accessor.addNativeHeader("Authorization", "Bearer token");
        when(jwtUtil.extractUserId("token")).thenReturn("user-1");
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
        MessageChannel channel = mock(MessageChannel.class);

        Message<?> result = interceptor.preSend(message, channel);

        StompHeaderAccessor wrapped = StompHeaderAccessor.wrap(result);
        assertThat(wrapped.getUser()).isNotNull();
        assertThat(wrapped.getUser().getName()).isEqualTo("user-1");
        verify(presenceService).markOnline("user-1", "session-1");
    }

    @Test
    void preSendDisconnectMarksOffline() {
        WebSocketAuthChannelInterceptor interceptor = new WebSocketAuthChannelInterceptor(jwtUtil, presenceService);
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.DISCONNECT);
        accessor.setSessionId("session-1");
        accessor.setUser(new UsernamePasswordAuthenticationToken("user-1", null));
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
        MessageChannel channel = mock(MessageChannel.class);

        interceptor.preSend(message, channel);

        verify(presenceService).markOfflineIfNoSessions("user-1", "session-1");
    }

    @Test
    void preSendRefreshesPresenceOnActivity() {
        WebSocketAuthChannelInterceptor interceptor = new WebSocketAuthChannelInterceptor(jwtUtil, presenceService);
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
        accessor.setSessionId("session-1");
        accessor.setUser(new UsernamePasswordAuthenticationToken("user-1", null));
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
        MessageChannel channel = mock(MessageChannel.class);

        interceptor.preSend(message, channel);

        verify(presenceService).refresh("user-1", "session-1");
    }
}
