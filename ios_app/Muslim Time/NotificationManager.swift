import Foundation
import UserNotifications

final class ReminderManager {
    static let shared = ReminderManager()
    private init() {}

    func requestPermission() async {
        let center = UNUserNotificationCenter.current()
        do {
            _ = try await center.requestAuthorization(options: [.alert, .sound, .badge])
        } catch {
            print("Permission error: \(error)")
        }
    }

    func scheduleDemoReminders() {
        let center = UNUserNotificationCenter.current()
        center.getPendingNotificationRequests { existing in
            guard existing.isEmpty else { return }

            let items: [(String, String, Int)] = [
                ("İmsak", "İmsak vaxtı", 7),
                ("Məğrib", "Məğrib (İftar) vaxtı", 8),
            ]

            for (identifier, title, hour) in items {
                let content = UNMutableNotificationContent()
                content.title = title
                content.body = "Namaz vaxtını qaçırma, Allahla yaxın ol!"
                content.sound = .default

                var dateComponents = DateComponents()
                dateComponents.hour = hour
                dateComponents.minute = 30
                let trigger = UNCalendarNotificationTrigger(dateMatching: dateComponents, repeats: true)

                let request = UNNotificationRequest(
                    identifier: identifier,
                    content: content,
                    trigger: trigger
                )
                center.add(request)
            }

            print("Reminders scheduled: \(items.count)")
        }
    }
}
