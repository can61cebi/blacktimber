# BlackTimber 1.4.0 — Modrinth yayın paketi ve rehberi

Bu klasör, **1.4.0** sürümünü Modrinth'te yayınlaman için gereken her şeyi içerir. Sürüm
zaten yayında olan projeye bir **güncelleme** olarak çıkıyor; açıklama, ikon, lisans,
etiketler bir kez ayarlandı, onlara dokunmana gerek yok. Bu sefer **galeriyi de** güncelliyoruz
çünkü oyuncu menüsü görseli değişti (artık dört anahtar var).

Modrinth arayüz etiketleri İngilizce; açıklamalar Türkçe.

---

## Bu klasörde ne var

| Dosya | Ne için |
| --- | --- |
| `REHBER.md` | Bu rehber |
| `changelog.md` | Modrinth sürüm **Changelog** alanına yapıştırılacak metin |
| `gallery/3-menu-player.png` | **Yeniden yüklenecek** oyuncu menüsü görseli (dört anahtar) |
| `gallery/4-menu-admin.png` | **Yeniden yüklenecek** admin panel görseli (replant açıklaması güncel) |

Jar bu klasörde yok (boyutu büyük, repo'ya girmez). Onu `./gradlew build` ile üretip
`build/libs/BlackTimber-1.4.0.jar`'dan ya da GitHub release'inden alırsın.

---

## 1.4.0 özeti (yeni sürümde ne değişti)

İki düzeltme ve bir yeni özellik:

1. **Yaprak temizleme artık vanilla çürümeyle birebir.** Sabit küp tarama kaldırıldı; sadece
   kesilen ağacın beslediği ve ayakta hiçbir log'un 6 blok içinde tutmadığı yapraklar
   temizlenir. Bütün kanopi (en dış katmanlar dahil) gider, komşu ağacın yaprakları korunur.
2. **Kesim balta dayanıklılığıyla sınırlı.** Balta artık 1 canda takılıp sonsuz kesmiyor;
   gücünün yettiği kadar log keser. `break-tool` varsayılanı `true` (balta vanilla gibi kırılır).
3. **Yeni: fidan yeniden dikme.** Oyuncu başına, varsayılan **kapalı** dördüncü anahtar.
   Kesilen ağacın türüne göre uygun fidanı/propagülü, yalnızca uygun zeminde geri diker.

Tam changelog `changelog.md` içinde.

---

## 1. Jar'ı derle

```bash
./gradlew build
unzip -p build/libs/BlackTimber-1.4.0.jar plugin.yml | grep version   # 1.4.0 görmelisin
```

## 2. GitHub release (önerilen — jar'ı otomatik üretir)

`v1.4.0` etiketi gönderince CI jar'ı derler ve release notlarını
`.github/releases/v1.4.0.md`'den alıp release oluşturur:

```bash
git tag v1.4.0
git push origin v1.4.0
```

`https://github.com/can61cebi/blacktimber/releases` altında `BlackTimber 1.4.0` ve jar oluşur.

## 3. Modrinth'te yeni sürüm oluştur

`modrinth.com/plugin/blacktimber` → **Settings → Versions → Create version**.

| Alan | Değer |
| --- | --- |
| Name | `BlackTimber 1.4.0` |
| Version number | `1.4.0` |
| Release channel | `Release` |
| Loaders | `Paper` ve `Folia` |
| Game versions | `26.1.2` |
| Dependencies | yok |
| Files | `BlackTimber-1.4.0.jar` |
| Changelog | `changelog.md` içeriğini yapıştır |

**Publish version**.

## 4. Galeriyi güncelle (bu sürümde gerekli)

Oyuncu menüsü artık dört anahtar gösteriyor, admin panel açıklaması güncellendi. **Settings →
Gallery** altında şu iki görseli **yeni dosyalarıyla değiştir** (eskisini sil, bu klasördeki
yenisini yükle, başlığı aynı bırak):

| Sıra | Dosya | Başlık |
| --- | --- | --- |
| 3 | `gallery/3-menu-player.png` | The per player menu |
| 4 | `gallery/4-menu-admin.png` | The live admin panel |

1-banner, 2-detection, 5-menu-loot ve 6-stack **değişmedi**, onlara dokunma.

## 5. Açıklama (isteğe bağlı)

Açıklamada özellik listesine bir satır eklemek istersen:
"Per player sapling replanting that reads the felled species and only plants on valid ground."
Eklersen `modrinth/description.md`'yi de güncel tut. Zorunlu değil.

## 6. Yayın sonrası kontrol

- Sürüm numarası, jar boyutu ve loader'lar doğru mu?
- Changelog düzgün göründü mü?
- Galeride 3. ve 4. görsel yeni hâliyle mi?
- (Varsa) `docs.cebi.tr/blacktimber` ve `cebi.tr` sayfalarındaki menü görsellerini de
  repodaki `assets/png/menu-player*.png` ve `menu-admin*.png` ile güncelle/yeniden deploy et.
