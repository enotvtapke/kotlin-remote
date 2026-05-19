package social.repository

import java.util.concurrent.atomic.AtomicLong
import social.model.Notification
import social.model.SendNotificationDto

class NotificationRepository {
    private val nextId = AtomicLong(1)
    private val notifications = mutableMapOf<Long, Notification>()

    fun sendNotification(dto: SendNotificationDto): Notification {
        val n = Notification(nextId.getAndIncrement(), dto.userId, dto.kind, dto.payload)
        notifications[n.id] = n
        return n
    }

    fun getNotifications(userId: Long): List<Notification> =
        notifications.values.filter { it.userId == userId }.sortedByDescending { it.createdAt }

    fun markAsRead(id: Long): Notification {
        val n = notifications[id] ?: error("Notification $id not found")
        val updated = n.copy(read = true)
        notifications[id] = updated
        return updated
    }

    fun markAllAsRead(userId: Long): Int {
        var count = 0
        notifications.entries.filter { it.value.userId == userId && !it.value.read }.forEach {
            notifications[it.key] = it.value.copy(read = true)
            count++
        }
        return count
    }
}
