import SwiftUI

struct DuaView: View {
    var body: some View {
        NavigationStack {
            List(sampleDuas) { dua in
                VStack(alignment: .leading, spacing: 6) {
                    Text(dua.title)
                        .font(.headline)
                    Text(dua.text)
                        .font(.body)
                        .foregroundStyle(.secondary)
                }
                .padding(.vertical, 4)
            }
            .navigationTitle("Dua")
        }
    }
}
