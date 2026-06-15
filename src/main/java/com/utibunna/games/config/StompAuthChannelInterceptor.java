package com.utibunna.games.config;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

/**
 * Reads the gateway-forwarded {@code X-User-Id} from the STOMP CONNECT frame and attaches it as the
 * session Principal, so every {@code @MessageMapping} handler receives the userId via {@code Principal}.
 *
 * <p>Trusted-gateway model: we do NOT re-validate a JWT here. This is safe only because the gateway is
 * the sole ingress and overwrites this header for external callers.</p>
 */
@Component
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    public static final String USER_ID_HEADER = "X-User-Id";

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String userId = accessor.getFirstNativeHeader(USER_ID_HEADER);
            if (userId == null || userId.isBlank()) {
                throw new IllegalArgumentException("Missing " + USER_ID_HEADER + " header on CONNECT");
            }
            accessor.setUser(new UserPrincipal(userId));
        }
        return message;
    }
}
