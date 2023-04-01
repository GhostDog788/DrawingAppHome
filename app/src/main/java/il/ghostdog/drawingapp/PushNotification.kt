package il.ghostdog.drawingapp

data class PushNotification(
    val to: String,
    val data: Map<String, String>,
    val notification: NotificationPayload
) {
    data class NotificationPayload(
        val title: String,
        val body: String
    )
}