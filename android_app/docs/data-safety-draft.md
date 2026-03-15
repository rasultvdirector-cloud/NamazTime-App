# Data Safety Draft

Bu sənəd Play Console Data Safety formunu doldurmaq üçün ilkin draftdır.

## Toplanan məlumatlar

### Məkan

İstifadə məqsədi:

- namaz vaxtlarını hesablamaq və uyğun mənbədən gətirmək
- qibləgah istiqamətini göstərmək

Qeyd:

- məkan istifadəçi tərəfindən auto location aktiv ediləndə istifadə olunur
- manual məkan seçimi dəstəklənir
- məkan reklam məqsədilə istifadə olunmur

### Tətbiq ayarları və lokal məlumatlar

İstifadə məqsədi:

- seçilmiş dil
- seçilmiş tema
- bildiriş ayarları
- istifadəçi adı, cins və doğum tarixi
- seçilmiş məkan və namaz mənbəyi
- son Quran/audio vəziyyəti

Qeyd:

- bu məlumatlar əsasən cihaz daxilində saxlanılır
- hesab sistemi yoxdur

## Paylaşım

- məlumat üçüncü tərəfə satış məqsədilə paylaşılmır
- tətbiq reklam SDK-ları üzərində qurulmayıb

## Təhlükəsizlik

- ayarlar və lokal state cihaz daxilində saxlanılır
- bildiriş və dini funksiya məqsədi xaricində şəxsi məlumat istifadəsi nəzərdə tutulmur

## Play Console üçün ehtimal cavab istiqaməti

- Does your app collect location: `Yes`
- Is location optional: `Yes`
- Is it used for app functionality: `Yes`
- Is it shared: `No`
- Are personal infos collected: `Yes`, limited profile info entered by user
- Is data encrypted in transit: `Yes`, for network-based prayer time sources
- Can users request deletion: `Yes`, app data silinərək və ya ayarlar sıfırlanaraq lokal məlumatlar təmizlənə bilər

