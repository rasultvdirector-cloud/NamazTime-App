import SwiftUI

struct PrayerTimesView: View {
    @State private var selectedCityIndex = 0

    var body: some View {
        NavigationStack {
            Form {
                Picker("Şəhər", selection: $selectedCityIndex) {
                    ForEach(Array(samplePrayerTimes.enumerated()), id: \.offset) { index, city in
                        Text("\(city.city), \(city.country)").tag(index)
                    }
                }
                .pickerStyle(.navigationLink)

                Section("Namaz vaxtları") {
                    ForEach(samplePrayerTimes[selectedCityIndex].times) { item in
                        HStack {
                            Text(item.name)
                            Spacer()
                            Text(item.time)
                                .foregroundStyle(.secondary)
                        }
                    }
                }
            }
            .navigationTitle("Namaz Vaxtları")
        }
    }
}
