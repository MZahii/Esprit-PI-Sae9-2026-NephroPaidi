package tn.esprit.spring.Administrationservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.spring.Administrationservice.entity.Notification;
import tn.esprit.spring.Administrationservice.repository.NotificationRepository;

@Service
@RequiredArgsConstructor
public class NotificationPublisherService {

    private final NotificationRepository notificationRepository;

    public void publish(String type, String title, String message) {
        publish(type, title, message, null);
    }

    public void publish(String type, String title, String message, Long targetUserId) {
        notificationRepository.save(Notification.builder()
                .type(type)
                .title(title)
                .message(message)
                .targetUserId(targetUserId)
                .read(false)
                .build());
    }
}

