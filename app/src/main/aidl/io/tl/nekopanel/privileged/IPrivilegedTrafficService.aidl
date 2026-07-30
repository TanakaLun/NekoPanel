package io.tl.nekopanel.privileged;

interface IPrivilegedTrafficService {
    void configure(String baseUrl, String secret, String notificationPriority);
    void startMonitoring();
    void updateNotificationPriority(String notificationPriority);
    void stopMonitoring();
    int getUid();
    void destroy();
}
