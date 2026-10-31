# LaperShop (Fabric, Minecraft 26.3)

Satu mod **server-side** (pemain **tidak perlu** memasang mod di client) untuk survival multiplayer kompetitif.

| Komponen | Versi (dicek untuk 26.3) |
|---|---|
| Minecraft | 26.3 |
| Fabric Loader | ≥ 0.19.5 |
| Fabric API | 0.165.0+26.3 |
| Java | 25 |
| Gradle | 9.6.0 (diinstal oleh workflow) |
| Loom | `net.fabricmc.fabric-loom` 1.17 (plugin non-obfuscated, versi di `gradle.properties`) |

## Instalasi
1. Unduh `lapershop-x.y.z.jar` dari **GitHub Actions → Artifacts** atau **Releases**.
2. Taruh di folder `mods/` server bersama **Fabric API 26.3**.
3. Jalankan server sekali → `config/lapershop.json` dibuat otomatis. Edit, simpan; perubahan dimuat otomatis (≤10 detik) tanpa restart.

## Build otomatis lewat GitHub
1. Buat repository baru, upload isi folder ini, push ke branch `main`.
2. Tab **Actions → Build** berjalan otomatis (atau klik *Run workflow*). JAR ada di bagian **Artifacts** (`lapershop-jar`).
3. Untuk Release: `git tag v1.0.0 && git push origin v1.0.0` → JAR otomatis terlampir di **Releases**.
4. (Opsional lokal) `gradle wrapper --gradle-version 9.6.0` lalu `./gradlew build`.

> **Jika build gagal:** salin log error dari tab Actions. Versi Loom (`loom_version`) dan beberapa nama class Minecraft 26.3 tidak bisa saya verifikasi dari lingkungan saya (tanpa internet). Titik rawan ada di tabel bawah.

## Fitur & cara kerja
**1–2. Cooldown Trident (Riptide) 15 dtk & Ender Pearl 5 dtk** – dilacak per pemain (UUID + waktu nyata), jadi ganti slot/pindah item/relog tidak bisa menghindarinya. Berlaku untuk *semua* Ender Pearl & semua Trident ber-Riptide; Trident tanpa Riptide bebas. Cooldown Trident aktif saat Riptide benar-benar meluncurkan pemain. Countdown tampil di action bar tepat di atas hotbar, kedua cooldown bisa tampil bersamaan: `Trident 12.3s | Ender Pearl 3.1s`.

**3. Totem** – peluang drop Evoker (termasuk Raid) default 5 % (`totem.evokerDropChance`).

**4. Mending** – memancing: buku Mending dipertahankan dengan peluang `fishingKeepChance` (selain itu diganti item lain); Librarian: tiap trade Mending dipertahankan dengan peluang `villagerKeepChance` (keputusan konsisten per villager, tidak berubah setiap dibuka), sisanya diganti enchantment lain. Tidak pernah 0 selama peluang > 0.

**5. Death counter** – data permanen di `<world>/lapershop-data.json`. Tampil di TAB: `Nama ☠ 12` (☠ merah) memakai scoreboard slot TAB-list.

**6–7. Koordinat & Locator Bar** – memakai gamerule `reducedDebugInfo=true` dan `locatorBar=false`, ditegakkan ulang tiap 10 detik jika ada yang mengubahnya.

**8. LAPER SHOP** – jadwal waktu nyata (`shop.timezone`, `shop.startTimes`, maks 120 menit), pengumuman gradasi kuning→oranye saat buka/tutup, Boss Bar `LAPERSHOP | 01:59:59`, command `/lapershop` (divalidasi di server; menolak jika toko tutup), GUI memakai tampilan Villager Trading. Jadwal dihitung dari jam sistem sehingga tahan restart; pengumuman "buka" tidak diulang setelah restart di tengah event.

### Contoh penawaran (`shop.offers`)
```json
{
  "id": "netherite_armor_set",
  "enabled": true,
  "payment":  { "item": "minecraft:netherite_ingot", "count": 16 },
  "payment2": null,
  "reward":   { "item": "minecraft:netherite_chestplate", "count": 1, "name": "&6Laper Chestplate",
                "lore": ["&7Dari LAPER SHOP"], "enchantments": { "minecraft:protection": 4 } },
  "bonusRewards": [ { "item": "minecraft:netherite_helmet", "enchantments": { "minecraft:protection": 4 } } ],
  "perPlayerLimit": 1
}
```
Item mod lain cukup memakai id-nya (`"modid:nama_item"`). `limitScope`: `"event"` (reset tiap event) atau `"forever"`.

## Keterbatasan & alternatif teknis (jujur)
- **"Tidak bisa memegang/memindahkan Trident saat cooldown"**: tanpa mixin/mod client, yang bisa dijamin server adalah *tidak bisa menggunakan* (start charge & Riptide diblokir untuk semua Trident ber-Riptide, di kedua tangan). Karena cooldown melekat pada pemain, memegang/memindahkan tidak memberi keuntungan apa pun. Memblokir fisik pergerakan item butuh mixin pada klik inventory.
- **Koordinat**: `reducedDebugInfo` menyembunyikan XYZ di F3 (mekanisme resmi). Mod client/minimap tidak bisa dicegah dari server.
- **Satu transaksi = satu item hasil** (batas MerchantOffer vanilla). Set armor lengkap dibuat dengan `reward` + `bonusRewards` (masuk inventory otomatis, jatuh ke tanah jika penuh). Pembayaran hanya item + jumlah (tanpa syarat nama/NBT).
- Death counter memakai slot TAB-list scoreboard; plugin/mod lain yang memakai slot itu akan bertabrakan.
- Cooldown tidak disimpan ke disk (hilang jika server restart); data kematian, batas beli, dan status pengumuman permanen.

## Titik rawan kompatibilitas 26.3 (jika ada error compile)
| Berkas | Hal yang mungkin berubah |
|---|---|
| `util/Ids.java` | `Identifier` (dulu `ResourceLocation`), `Registry.getValue/containsKey` |
| `shop/ShopMerchant.java` | method wajib interface `Merchant`, ctor `MerchantOffer`, `ItemCost` |
| `feature/CooldownManager.java` | callback `UseItemCallback` (tipe return), `isAutoSpinAttack()` |
| `feature/DeathCounter.java` | `Scoreboard.addObjective(...)`, `FixedFormat`, `ScoreAccess` |
| `feature/Restrictions.java` | nama gamerule (`snake_case` vs `camelCase`; keduanya dicoba otomatis) |
| `feature/DropRules.java` | `ServerLivingEntityEvents.AFTER_DEATH`, `MerchantOffer.getResult()` |
| `gradle.properties` | `loom_version` |
