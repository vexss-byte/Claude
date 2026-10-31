package dev.lapershop.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Struktur config/lapershop.json. Semua field bisa diedit tanpa mengubah source code. */
public final class LaperConfig {
    public Cooldowns cooldowns = new Cooldowns();
    public Totem totem = new Totem();
    public Mending mending = new Mending();
    public DeathCounter deathCounter = new DeathCounter();
    public Restrictions restrictions = new Restrictions();
    public Shop shop = new Shop();

    public static final class Cooldowns {
        public boolean tridentEnabled = true;
        public int tridentSeconds = 15;
        /** true = hanya Trident dengan Riptide yang kena cooldown. */
        public boolean tridentRiptideOnly = true;
        public boolean enderPearlEnabled = true;
        public int enderPearlSeconds = 5;
        /** Tampilkan countdown di atas hotbar (action bar). */
        public boolean showHud = true;
    }

    public static final class Totem {
        public boolean enabled = true;
        /** Peluang (0.0 - 1.0) Totem tetap drop saat Evoker mati. 0.05 = 5%. */
        public double evokerDropChance = 0.05;
    }

    public static final class Mending {
        public boolean fishingEnabled = true;
        /** Peluang (0-1) buku Mending hasil memancing tetap dipertahankan. */
        public double fishingKeepChance = 0.15;
        /** Item pengganti jika buku Mending "digagalkan". */
        public String fishingReplacementItem = "minecraft:name_tag";

        public boolean villagerEnabled = true;
        /** Peluang (0-1) sebuah trade Mending Librarian dipertahankan. */
        public double villagerKeepChance = 0.15;
        public String villagerReplacementEnchantment = "minecraft:unbreaking";
        public int villagerReplacementLevel = 3;
    }

    public static final class DeathCounter {
        public boolean enabled = true;
        public String objectiveName = "lapershop_deaths";
    }

    public static final class Restrictions {
        public boolean disableCoordinates = true;
        public boolean disableLocatorBar = true;
    }

    public static final class Shop {
        public boolean enabled = true;
        /** Zona waktu IANA, mis. Asia/Jakarta. Jadwal memakai waktu nyata, bukan waktu Minecraft. */
        public String timezone = "Asia/Jakarta";
        /** Jam mulai event (HH:mm). Dua entri = dua kali sehari. */
        public List<String> startTimes = new ArrayList<>(List.of("12:00", "20:00"));
        /** Durasi event dalam menit. Dibatasi maksimal 120 (2 jam). */
        public int durationMinutes = 120;

        public String bossBarText = "LAPERSHOP";
        /** PINK, BLUE, RED, GREEN, YELLOW, PURPLE, WHITE */
        public String bossBarColor = "YELLOW";
        public String gradientStart = "#FFFF55";
        public String gradientEnd = "#FFAA00";

        public String openMessage = "LAPER SHOP SEDANG BUKA!";
        /** Placeholder: {end} = jam tutup. Kode warna '&' didukung. */
        public String openSubMessage = "&7Ketik &e/lapershop &7untuk berbelanja. Tutup pukul &6{end}&7.";
        public String closeMessage = "LAPER SHOP SUDAH TUTUP!";
        /** Placeholder: {next} = jadwal buka berikutnya. */
        public String closedMessage = "&cLAPER SHOP sedang tutup. Buka lagi pukul &e{next}&c.";
        public String guiTitle = "LAPER SHOP";

        /** "event" = batas beli di-reset tiap event, "forever" = batas seumur hidup. */
        public String limitScope = "event";

        public List<Offer> offers = defaultOffers();
    }

    /** Spesifikasi satu item (untuk hadiah). Pembayaran hanya memakai item + count. */
    public static final class ItemSpec {
        public String item = "minecraft:stone";
        public int count = 1;
        /** Nama kustom, mendukung kode '&'. null = default. */
        public String name = null;
        public List<String> lore = null;
        /** id enchantment -> level, mis. {"minecraft:sharpness": 5}. */
        public Map<String, Integer> enchantments = null;

        public ItemSpec() {}
        public ItemSpec(String item, int count) { this.item = item; this.count = count; }
    }

    public static final class Offer {
        /** ID unik (dipakai untuk menyimpan batas pembelian). Jangan diubah setelah dipakai. */
        public String id = "offer";
        public boolean enabled = true;
        public ItemSpec payment = new ItemSpec();
        /** Pembayaran kedua (opsional). */
        public ItemSpec payment2 = null;
        public ItemSpec reward = new ItemSpec();
        /** Hadiah tambahan yang langsung masuk inventory (mis. sisa potongan armor). */
        public List<ItemSpec> bonusRewards = new ArrayList<>();
        /** Batas beli per pemain. 0 = tanpa batas. */
        public int perPlayerLimit = 0;
    }

    private static List<Offer> defaultOffers() {
        List<Offer> list = new ArrayList<>();

        Offer armor = new Offer();
        armor.id = "netherite_armor_set";
        armor.payment = new ItemSpec("minecraft:netherite_ingot", 16);
        armor.reward = enchanted("minecraft:netherite_chestplate", "&6Laper Chestplate", "minecraft:protection", 4, "minecraft:unbreaking", 3);
        armor.bonusRewards.add(enchanted("minecraft:netherite_helmet", "&6Laper Helmet", "minecraft:protection", 4, "minecraft:unbreaking", 3));
        armor.bonusRewards.add(enchanted("minecraft:netherite_leggings", "&6Laper Leggings", "minecraft:protection", 4, "minecraft:unbreaking", 3));
        armor.bonusRewards.add(enchanted("minecraft:netherite_boots", "&6Laper Boots", "minecraft:protection", 4, "minecraft:unbreaking", 3));
        armor.perPlayerLimit = 1;
        list.add(armor);

        Offer apples = new Offer();
        apples.id = "enchanted_golden_apple";
        apples.payment = new ItemSpec("minecraft:diamond", 32);
        apples.reward = new ItemSpec("minecraft:enchanted_golden_apple", 1);
        apples.perPlayerLimit = 2;
        list.add(apples);

        Offer disabled = new Offer();
        disabled.id = "example_disabled";
        disabled.enabled = false;
        disabled.payment = new ItemSpec("minecraft:emerald", 64);
        disabled.payment2 = new ItemSpec("minecraft:diamond", 8);
        disabled.reward = new ItemSpec("minecraft:elytra", 1);
        disabled.perPlayerLimit = 1;
        list.add(disabled);
        return list;
    }

    private static ItemSpec enchanted(String item, String name, String e1, int l1, String e2, int l2) {
        ItemSpec s = new ItemSpec(item, 1);
        s.name = name;
        s.lore = new ArrayList<>(List.of("&7Dari LAPER SHOP"));
        s.enchantments = new LinkedHashMap<>();
        s.enchantments.put(e1, l1);
        s.enchantments.put(e2, l2);
        return s;
    }

    /** Merapikan nilai & mengganti section null dengan default. */
    public void sanitize() {
        if (cooldowns == null) cooldowns = new Cooldowns();
        if (totem == null) totem = new Totem();
        if (mending == null) mending = new Mending();
        if (deathCounter == null) deathCounter = new DeathCounter();
        if (restrictions == null) restrictions = new Restrictions();
        if (shop == null) shop = new Shop();

        cooldowns.tridentSeconds = Math.max(0, cooldowns.tridentSeconds);
        cooldowns.enderPearlSeconds = Math.max(0, cooldowns.enderPearlSeconds);
        totem.evokerDropChance = clamp01(totem.evokerDropChance);
        mending.fishingKeepChance = clamp01(mending.fishingKeepChance);
        mending.villagerKeepChance = clamp01(mending.villagerKeepChance);
        mending.villagerReplacementLevel = Math.max(1, mending.villagerReplacementLevel);
        if (deathCounter.objectiveName == null || deathCounter.objectiveName.isBlank()) deathCounter.objectiveName = "lapershop_deaths";
        if (shop.startTimes == null) shop.startTimes = new ArrayList<>();
        if (shop.offers == null) shop.offers = new ArrayList<>();
        shop.durationMinutes = Math.max(1, Math.min(120, shop.durationMinutes));
    }

    private static double clamp01(double v) { return Math.max(0.0, Math.min(1.0, v)); }
}
