import SwiftUI

@main
struct MuslimTimeApp: App {
    @State private var initialized = false

    var body: some Scene {
        WindowGroup {
            ContentView()
                .onAppear {
                    guard !initialized else { return }
                    initialized = true
                    Task {
                        await ReminderManager.shared.requestPermission()
                        ReminderManager.shared.scheduleDemoReminders()
                    }
                }
        }
    }
}
