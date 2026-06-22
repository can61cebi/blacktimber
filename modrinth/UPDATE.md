# Yeni sürüm yayınlama rehberi (güncelleme akışı)

Bu dosya, **zaten yayında olan** BlackTimber projesine yeni bir sürüm (örnek: `1.4.0`)
yüklemek içindir. İlk yayın için `MODRINTH.md` veya `modrinth/README.md` kullanılır; burada
sadece sürüm güncellemesinde gereken adımlar var. Açıklama, ikon, galeri, lisans, etiketler
bir kez ayarlandı; onlara dokunmana gerek yok (özellik değişmediyse).

Modrinth arayüz etiketlerini İngilizce bıraktım, açıklamalar Türkçe.

---

## 1. Jar'ı derle

```bash
./gradlew build
```

Çıktı: `build/libs/BlackTimber-1.4.0.jar`. Sürüm numarası `build.gradle.kts` içindeki
`version` alanından gelir; jar'ın içindeki `plugin.yml` otomatik olarak aynı numarayı alır.
Kontrol:

```bash
unzip -p build/libs/BlackTimber-1.4.0.jar plugin.yml | grep version
```

`1.4.0` görmelisin. Yerel yükleme paketi için bu jar `modrinth/` klasörüne de kopyalanır
(git'e girmez, `.gitignore` ile dışlanır; release ile dağıtılır).

---

## 2. GitHub release (önerilen — jar'ı otomatik üretir)

Repo'da CI kurulu: `v` ile başlayan bir etiket gönderince GitHub Actions jar'ı derler ve
release oluşturur. Release notları `.github/releases/v1.4.0.md` dosyasından alınır (zaten
hazır).

```bash
git tag v1.4.0
git push origin v1.4.0
```

Birkaç dakika sonra `https://github.com/can61cebi/blacktimber/releases` altında
`BlackTimber 1.4.0` release'i ve jar oluşur. Modrinth'e bu jar'ı da yükleyebilirsin (aynı
dosya).

> Not: Etiketi göndermek herkese açık bir release tetikler; hazır olduğundan emin olduğunda
> gönder. Önce kodu `main`'e merge etmiş olmalısın.

---

## 3. Modrinth'te yeni sürüm oluştur

`modrinth.com/plugin/blacktimber` → **Settings** → **Versions** → **Create version**.

| Alan | Değer |
| --- | --- |
| Name | `BlackTimber 1.4.0` |
| Version number | `1.4.0` |
| Release channel | `Release` |
| Loaders | `Paper` ve `Folia` |
| Game versions | `26.1.2` |
| Dependencies | yok |
| Files | `build/libs/BlackTimber-1.4.0.jar` (veya GitHub release'teki jar) |
| Changelog | `modrinth/version-changelog.md` içeriğini yapıştır |

**Publish version** ile yayınla. Eski sürümler listede kalır; en yeni sürüm otomatik öne
çıkar.

---

## 4. Açıklamayı güncellemek gerekiyor mu? (genelde hayır)

1.4.0 bir hata düzeltme sürümü; özellik listesi değişmedi, bu yüzden **Description**,
**Gallery**, **Tags**, **Links** olduğu gibi kalabilir.

Yine de iki davranış değişikliğini açıklamada belirtmek istersen (isteğe bağlı):

- Yaprak temizleme artık vanilla çürüme kuralını birebir uyguluyor (kesilen ağacın tüm
  kanopisi, en dış katmanlar dahil; komşu ağacın yaprakları korunur).
- Kesim balta dayanıklılığıyla sınırlı; `break-tool` artık varsayılan `true` (balta vanilla
  gibi kırılır).

Description'ı değiştirirsen `modrinth/description.md` dosyasını da güncel tutmak iyi olur ki
tek kaynak kalsın.

---

## 5. Yayın sonrası kontrol

- Modrinth sürüm sayfasında jar boyutu ve sürüm numarası doğru mu?
- Changelog metni düzgün göründü mü?
- (GitHub kullandıysan) release'teki jar ile Modrinth'teki jar aynı dosya mı?

---

## Sürüm yükseltirken nereyi değiştiriyorsun? (sonraki sürümler için kısa liste)

1. `build.gradle.kts` → `version`.
2. `.github/releases/v<sürüm>.md` → yeni release notları.
3. `modrinth/version-changelog.md` → Modrinth changelog metni.
4. `modrinth/README.md` ve `MODRINTH.md` içindeki sürüm/jar adı referansları.
5. Gerekirse `README.md` (özellik veya config değiştiyse).
6. `./gradlew build`, sonra commit + (hazırsa) `git tag v<sürüm> && git push origin v<sürüm>`.
