# Firebase FCM Setup

NamazTime bu quruluşla Firebase Console üzərindən ayrıca server olmadan push mesaj qəbul edə bilər.

## App tərəfdə artıq hazır olanlar

- Firebase Messaging SDK qoşulub
- `all_users` və `android_users` topic subscription hazırlanıb
- foreground mesajları üçün lokal notification handling əlavə olunub

## Səndən lazım olan 1 əsas addım

1. Firebase-də yeni project yarat
2. Android app əlavə et:
   - package name: `com.muslimtime.app`
3. `google-services.json` faylını yüklə
4. Bu faylı bura yerləşdir:

```text
android_app/app/google-services.json
```

## Sonra nə olacaq

- app açılan kimi Firebase token alacaq
- cihaz `all_users` və `android_users` topic-lərinə yazılacaq
- Firebase Console-dan mesaj göndərmək mümkün olacaq

## Firebase Console-dan ilk mesaj

1. Firebase Console -> Messaging
2. `Create your first campaign`
3. `Firebase Notification messages`
4. Başlıq və mətn yaz
5. Android app-i seç
6. Göndər

## Nə üçün ayrıca server lazım deyil

İlk mərhələdə mesajları birbaşa Firebase Console-dan göndərə bilərsən. Öz serverin yalnız daha sonra lazım olar:

- xüsusi seqmentlər
- admin panel
- avtomatik və planlı push-lar
- versiyaya görə xüsusi hədəfləmə
