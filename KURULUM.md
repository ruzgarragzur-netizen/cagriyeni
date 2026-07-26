# Çağrı Kaydedici - Kurulum Rehberi (Kod yazmadan)

## Bu uygulama ne yapar?
Telefon araması başladığı an (sen açtığında veya arama başladığında) otomatik
kayda başlar, görüşme bitince otomatik durur. Kayıtlar telefonda
`CagriKayitlari` klasöründe birikir, uygulamayı açıp dokunarak dinleyebilirsin.

**Önemli:** Bu sadece normal GSM/operatör aramaları içindir. WhatsApp
aramalarını Android'in güvenlik kısıtlaması yüzünden bu şekilde kaydetmek
mümkün değil (bunu ayrı konuşmuştuk).

## Adım 1: GitHub hesabı aç (ücretsiz, 2 dakika)
https://github.com adresine git, "Sign up" ile ücretsiz hesap oluştur.

## Adım 2: Yeni bir repository (depo) oluştur
- Sağ üstteki "+" işaretine tıkla -> "New repository"
- İsim ver (örn: `cagri-kaydedici`), "Public" seçili kalsın, "Create repository" de.

## Adım 3: Dosyaları yükle
- Az önce indirdiğin klasörün/zip'in içindeki TÜM dosya ve klasörleri
  (build.gradle, settings.gradle, app/, .github/ ...) bilgisayarında aç.
- GitHub'daki depo sayfasında "Add file" -> "Upload files" butonuna tıkla.
- Tüm dosya ve klasörleri sürükle-bırak ile oraya at.
- Alt kısımdaki "Commit changes" butonuna bas.

## Adım 4: Otomatik derlemenin bitmesini bekle
- Depo sayfasının üstündeki "Actions" sekmesine tıkla.
- "APK Derle" adında bir işlem otomatik başlamış olacak (sarı nokta = çalışıyor,
  yeşil tik = bitti). 3-5 dakika sürer.

## Adım 5: APK'yı indir
- İşlem yeşil tik olunca üstüne tıkla, en altta "Artifacts" bölümünde
  "cagri-kaydedici-apk" yazan bir dosya göreceksin, ona tıkla, indir (zip
  içinden çıkacak).

## Adım 6: Telefona kur
- İndirdiğin `app-debug.apk` dosyasını telefonuna aktar (kendine WhatsApp,
  Google Drive veya e-posta ile gönderip telefonda indir).
- Dosyaya dokun, "Bilinmeyen kaynaklardan yükleme"ye izin ver dediğinde onayla.
- Kurulum bitince uygulamayı aç, istenen izinleri (mikrofon, telefon durumu,
  bildirim) ver.

## Test et
Birine kısa bir arama yap. Kapattıktan sonra uygulamayı aç, listede yeni bir
kayıt görmen lazım. Dokunca çalması gerekir.

## OnePlus 8T ile ilgili not
OxygenOS'un bazı sürümleri, üçüncü parti uygulamaların karşı tarafın sesini de
yakalamasına izin veren "VOICE_CALL" ses kaynağını kapatmış olabilir. Uygulama
bunu otomatik deniyor, çalışmazsa sırasıyla diğer kaynaklara düşüyor. Eğer
kayıtta sadece kendi sesin varsa, telefonu hoparlöre alarak konuşman kayıtta
karşı tarafı da (mikrofon üzerinden) yakalamana yardımcı olur.

## Hukuki not
Kendi taraf olduğun görüşmeleri kişisel not amacıyla kaydetmek genelde
sorun değildir, ancak kaydı üçüncü kişilerle paylaşmak veya karşı tarafı
büsbütün habersiz bırakıp farklı amaçlarla kullanmak ayrı bir konudur. Emin
değilsen bir hukukçuya danışmak en sağlıklısı.
