# Muslim Time iOS (SwiftUI)

Bu qovluqda iOS app-in minimal SwiftUI layihəsi var:
- `Muslim Time.xcodeproj` — Xcode layihə faylı
- `Muslim Time/` — Swift kodları

## Addımlar

1) Xcode açın və `Open a Project or File...` ilə:
   `/Users/rasulalekberov/APP/Muslim Time/ios_app/Muslim Time.xcodeproj`
2) Target: `MuslimTime`
3) Scheme: `MuslimTime`
4) iPhone simulator seçib `Run` basın.

## Komandalar (Terminal)

```
cd '/Users/rasulalekberov/APP/Muslim Time/ios_app'
xcodebuild -project 'Muslim Time.xcodeproj' \
  -scheme MuslimTime \
  -configuration Debug \
  -destination 'generic/platform=iOS Simulator' \
  -derivedDataPath '/tmp/MuslimTimeBuild' \
  CODE_SIGNING_ALLOWED=NO CODE_SIGN_STYLE=Automatic build
```

Əgər cihazda işlətmək istəyirsinizsə, bu xəttlə bağlı signing xətası verə bilər. O zaman Xcode-da `Signing & Capabilities`-də
`Automatically manage signing` + Apple ID + Team əlavə edin.

Terminal build zamanı `CoreSimulatorService` erroru görsəniz, simulyator xidmətini yenidən başladın və ya birbaşa Xcode ilə run edin.
