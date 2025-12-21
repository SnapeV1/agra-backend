package org.agra.agra_backend.config;

import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.config.SimpleBrokerRegistration;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.StompWebSocketEndpointRegistration;
import org.springframework.web.socket.config.annotation.SockJsServiceRegistration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class WebSocketConfigTest {

    @Test
    void configureMessageBrokerSetsPrefixes() {
        WebSocketAuthChannelInterceptor interceptor = mock(WebSocketAuthChannelInterceptor.class);
        WebSocketConfig config = new WebSocketConfig(interceptor);
        MessageBrokerRegistry registry = mock(MessageBrokerRegistry.class);
        when(registry.enableSimpleBroker(any())).thenReturn(mock(SimpleBrokerRegistration.class));

        config.configureMessageBroker(registry);

        verify(registry).enableSimpleBroker("/topic", "/queue");
        verify(registry).setApplicationDestinationPrefixes("/app");
        verify(registry).setUserDestinationPrefix("/user");
    }

    @Test
    void registerStompEndpointsRegistersWsAndSockJs() {
        WebSocketAuthChannelInterceptor interceptor = mock(WebSocketAuthChannelInterceptor.class);
        WebSocketConfig config = new WebSocketConfig(interceptor);
        StompEndpointRegistry registry = mock(StompEndpointRegistry.class);
        StompWebSocketEndpointRegistration ws = mock(StompWebSocketEndpointRegistration.class);
        StompWebSocketEndpointRegistration sock = mock(StompWebSocketEndpointRegistration.class);
        when(registry.addEndpoint("/ws")).thenReturn(ws);
        when(ws.setAllowedOriginPatterns("*")).thenReturn(ws);
        when(registry.addEndpoint("/ws-sockjs")).thenReturn(sock);
        when(sock.setAllowedOriginPatterns("*")).thenReturn(sock);
        when(sock.withSockJS()).thenReturn(mock(SockJsServiceRegistration.class));

        config.registerStompEndpoints(registry);

        verify(registry).addEndpoint("/ws");
        verify(registry).addEndpoint("/ws-sockjs");
        verify(ws).setAllowedOriginPatterns("*");
        verify(sock).withSockJS();
    }

    @Test
    void configureClientInboundChannelAddsInterceptor() {
        WebSocketAuthChannelInterceptor interceptor = mock(WebSocketAuthChannelInterceptor.class);
        WebSocketConfig config = new WebSocketConfig(interceptor);
        ChannelRegistration registration = mock(ChannelRegistration.class);

        config.configureClientInboundChannel(registration);

        verify(registration).interceptors(interceptor);
    }
}
