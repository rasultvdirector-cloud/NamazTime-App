import SwiftUI

struct TimerView: View {
    var body: some View {
        NavigationStack {
            VStack(spacing: 12) {
                Text("Tasbih / Zikr")
                    .font(.title2)
                    .bold()
                Text("Bu modul növbəti mərhələdə tam olacaq.")
                    .foregroundStyle(.secondary)
            }
            .padding()
            .navigationTitle("Timer")
        }
    }
}
