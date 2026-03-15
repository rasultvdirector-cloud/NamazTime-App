import SwiftUI

struct ContentView: View {
    @State private var selection = 0

    var body: some View {
        TabView(selection: $selection) {
            PrayerTimesView()
                .tabItem {
                    Label("Namaz", systemImage: "clock")
                }
                .tag(0)

            QuranView()
                .tabItem {
                    Label("Quran", systemImage: "book")
                }
                .tag(1)

            DuaView()
                .tabItem {
                    Label("Dua", systemImage: "hands.sparkles")
                }
                .tag(2)

            ReminderView()
                .tabItem {
                    Label("Xatırlatma", systemImage: "bell.badge")
                }
                .tag(3)

            TimerView()
                .tabItem {
                    Label("Digər", systemImage: "timer")
                }
                .tag(4)
        }
        .accentColor(.green)
    }
}
