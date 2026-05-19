package social.server

import social.api.NotificationService
import social.model.Notification
import social.model.SendNotificationDto
import social.repository.NotificationRepository

class NotificationServiceImpl(
    private val repo: NotificationRepository,
) : NotificationService {
    override suspend fun sendNotification(dto: SendNotificationDto): Notification = repo.sendNotification(dto)
    override suspend fun getNotifications(userId: Long): List<Notification> = repo.getNotifications(userId)
    override suspend fun markAsRead(id: Long): Notification = repo.markAsRead(id)
    override suspend fun markAllAsRead(userId: Long): Int = repo.markAllAsRead(userId)
}
