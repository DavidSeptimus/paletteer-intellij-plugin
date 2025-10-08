package com.github.davidseptimus.paletteer

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project

object PaletteerNotifier {
    fun notifyInfo(project: Project?, content: String) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("Paletteer Notifications")
            .createNotification(content, NotificationType.INFORMATION)
            .notify(project)
    }
    fun notifyError(project: Project?, content: String) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("Paletteer Notifications")
            .createNotification(content, NotificationType.ERROR)
            .notify(project)
    }
}