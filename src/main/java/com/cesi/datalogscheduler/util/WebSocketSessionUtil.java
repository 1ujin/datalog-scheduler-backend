package com.cesi.datalogscheduler.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@SuppressWarnings("unused")
public class WebSocketSessionUtil {
    /**
     * 会话池
     */
    public static final Map<Object, WebSocketSession> SESSION_MAP = new ConcurrentHashMap<>();

    public static WebSocketSession put(Object key, WebSocketSession session) {
        return SESSION_MAP.put(key, session);
    }

    public static WebSocketSession get(Object key) {
        return SESSION_MAP.get(key);
    }

    public static WebSocketSession remove(Object key) {
        return SESSION_MAP.remove(key);
    }

    public static WebSocketSession removeAndClose(Object key, CloseStatus status) {
        WebSocketSession session = remove(key);
        if (session != null) {
            try {
                session.close(status);
            } catch (IOException e) {
                e.printStackTrace();
                log.error("关闭WebSocket会话失败: [key=" + key + ", status=" + status + "]" , e);
            }
        }
        return session;
    }
}
