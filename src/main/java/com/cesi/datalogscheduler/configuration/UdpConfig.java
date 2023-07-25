package com.cesi.datalogscheduler.configuration;

import com.cesi.datalogscheduler.util.WebSocketSessionUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.annotation.Transformer;
import org.springframework.integration.ip.udp.UnicastReceivingChannelAdapter;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class UdpConfig {
    private static final AtomicReference<WebSocketSession> ATOMIC_REFERENCE = new AtomicReference<>();

    @Value("${udp.port.log:9001}")
    private int port;

    private final SimpMessagingTemplate smt;

    @Bean
    public UnicastReceivingChannelAdapter getUnicastReceivingChannelAdapter() {
        // 实例化一个 UDP 端口
        UnicastReceivingChannelAdapter adapter = new UnicastReceivingChannelAdapter(port);
        adapter.setOutputChannelName("udp");
        return adapter;
    }

    @Transformer(inputChannel = "udp", outputChannel = "handle")
    public String transformer(Message<?> message) {
        // 把接收的数据转化为字符串
        return new String((byte[]) message.getPayload());
    }

    @ServiceActivator(inputChannel = "handle")
    public void udpMessageHandle(String message) {
        smt.convertAndSend("/topic/log", message);
        for (WebSocketSession session : WebSocketSessionUtil.SESSION_MAP.values()) {
            try {
                // 加锁 如果自旋锁已经是session了，说明有别的线程在占用，循环等待
                while (!ATOMIC_REFERENCE.compareAndSet(null, session));
                session.sendMessage(new TextMessage(message));
            } catch (IOException e) {
                e.printStackTrace();
                log.error("WebSocket发送消息失败: [ID=" + session.getId() + ", message=" + message + "]", e);
            } finally {
                // 解锁 当前线程使用结束，自旋锁恢复null
                ATOMIC_REFERENCE.compareAndSet(session, null);
            }
        }
    }
}
