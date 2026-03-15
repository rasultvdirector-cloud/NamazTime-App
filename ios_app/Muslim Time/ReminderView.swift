import SwiftUI

struct ReminderView: View {
    @State private var reminderEnabled = true

    var body: some View {
        NavigationStack {
            Form {
                Toggle("Namaz xatırlatmalarını aktiv et", isOn: $reminderEnabled)
                    .onChange(of: reminderEnabled) { newValue in
                        if newValue {
                            Task {
                                await ReminderManager.shared.requestPermission()
                                ReminderManager.shared.scheduleDemoReminders()
                            }
                        }
                    }
                Section("Status") {
                    Text("Qeyd: Bu mərhələdə xatırlatmalar gündəlik təməl saatda qurulur.")
                        .foregroundStyle(.secondary)
                }
            }
            .navigationTitle("Xatırlatmalar")
        }
    }
}
