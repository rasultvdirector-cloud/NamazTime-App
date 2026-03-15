import SwiftUI

struct QuranView: View {
    var body: some View {
        NavigationStack {
            List {
                Section("Ayələr") {
                    ForEach(sampleVerses) { verse in
                        Text(verse.text)
                            .multilineTextAlignment(.leading)
                    }
                }

                Section("Səsli Quran") {
                    ForEach(sampleTracks) { track in
                        HStack {
                            VStack(alignment: .leading) {
                                Text(track.title)
                                    .font(.headline)
                                Text(track.reciter)
                                    .font(.subheadline)
                                    .foregroundStyle(.secondary)
                            }
                            Spacer()
                            Button("Play") {}
                                .buttonStyle(.bordered)
                        }
                    }
                }
            }
            .navigationTitle("Quran")
            .toolbar {
                Text("İlkin versiya: səsli playback növbəti addım")
                    .font(.caption)
            }
        }
    }
}
