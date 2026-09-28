package com.haiman233.haimantech.load;

import com.haiman233.haimantech.HT;
import com.haiman233.haimantech.util.Colors;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.core.guide.SlimefunGuide;
import io.github.thebusybiscuit.slimefun4.core.guide.SlimefunGuideMode;
import io.github.thebusybiscuit.slimefun4.libraries.dough.skins.PlayerHead;
import io.github.thebusybiscuit.slimefun4.libraries.dough.skins.PlayerSkin;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.PotionMeta;

/**
 * 共享读取器：把物品段（{@code material_type/material/name/lore/glow/amount}）解析为 {@link ItemStack}，
 * 以及把配方段（槽位 "1".."N"）解析为 {@code ItemStack[]}。对应 RSC 的 {@code CommonUtils.readItem/readRecipe}。
 *
 * <p>相较 WorldTaste 版本，本类额外支持海曼科技院大量使用的两种 material_type：</p>
 * <ul>
 *   <li>{@code saveditem} —— 指向 {@code saveditems/<name>.yml}，读其 {@code item} 节点</li>
 *   <li>{@code built_in} —— RSC 内置物品，本项目实际用到 {@code slimefun_guide_survival}</li>
 * </ul>
 */
public final class Read {

    private Read() {}

    private static final Pattern HEX64 = Pattern.compile("^[0-9A-Fa-f]{64}$");

    /** 头颅贴图去重缓存（避免同一 hash/base64 反复解码）。 */
    private static final Map<String, PlayerSkin> HASH_SKINS = new HashMap<>();
    private static final Map<String, PlayerSkin> BASE64_SKINS = new HashMap<>();
    private static final Map<String, PlayerSkin> URL_SKINS = new HashMap<>();
    private static final Map<String, ItemStack> SAVED_ITEMS = new HashMap<>();

    /** 读取物品段。{@code countable=true} 时应用 amount。 */
    public static ItemStack item(ConfigurationSection s, boolean countable) {
        if (s == null) return null;
        String material = s.getString("material", "");
        if (material.isEmpty()) return null;

        String type = s.getString("material_type", "mc");
        String lower = material.toLowerCase(Locale.ROOT);
        // 自动识别（与 RSC 一致：base64 贴图以小写 ey/ew 开头，故前缀判断区分大小写）
        if (material.startsWith("ey") || material.startsWith("ew")) type = "skull";
        else if (lower.startsWith("http")) type = "skull_url";
        else if (HEX64.matcher(material).matches()) type = "skull_hash";

        ItemStack stack = resolve(type, material);
        if (stack == null) return null;
        stack = stack.clone();

        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            String name = s.getString("name");
            if (name != null && !name.isEmpty()) meta.setDisplayName(Colors.c(name));
            List<String> lore = s.getStringList("lore");
            if (lore != null && !lore.isEmpty()) meta.setLore(Colors.c(lore));
            String color = s.getString("color");
            if (color != null && !color.isEmpty()) applyColor(meta, color);
            int modelId = s.getInt("modelId", 0);
            if (modelId > 0) meta.setCustomModelData(modelId);
            stack.setItemMeta(meta);
        }

        if (s.getBoolean("glow", false)) {
            try {
                stack.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.LUCK_OF_THE_SEA, 1);
            } catch (Throwable ignored) {
                // 版本差异下忽略
            }
        }
        if (countable) {
            int amt = s.getInt("amount", 1);
            if (amt > 0) stack.setAmount(Math.min(amt, stack.getMaxStackSize()));
        }
        return stack;
    }

    private static ItemStack resolve(String type, String material) {
        switch (type.toLowerCase(Locale.ROOT)) {
            case "none":
                return new ItemStack(Material.AIR);
            case "skull_hash": {
                PlayerSkin skin = HASH_SKINS.computeIfAbsent(material, PlayerSkin::fromHashCode);
                return PlayerHead.getItemStack(skin);
            }
            case "skull":
            case "skull_base64": {
                PlayerSkin skin = BASE64_SKINS.computeIfAbsent(material, PlayerSkin::fromBase64);
                return PlayerHead.getItemStack(skin);
            }
            case "skull_url": {
                PlayerSkin skin = URL_SKINS.computeIfAbsent(material, PlayerSkin::fromURL);
                return PlayerHead.getItemStack(skin);
            }
            case "saveditem": {
                ItemStack cached = SAVED_ITEMS.get(material);
                if (cached != null) return cached.clone();
                YamlConfiguration cfg = Yaml.loadSavedItem(material);
                ItemStack stack = cfg.getItemStack("item");
                if (stack == null) {
                    HT.missing("saveditem缺失:" + material);
                    return new ItemStack(Material.STONE);
                }
                SAVED_ITEMS.put(material, stack);
                return stack.clone();
            }
            case "built_in":
                return builtIn(material);
            case "slimefun":
            case "sf": {
                String id = material.toUpperCase(Locale.ROOT);
                SlimefunItem sf = SlimefunItem.getById(id);
                if (sf != null) return sf.getItem().clone();
                ItemStack pre = HT.preload(id);
                if (pre != null) {
                    // 前向引用：目标尚未注册时补上 id PDC，保证 id 比较与指南导航正确
                    return new SlimefunItemStack(id, pre);
                }
                HT.missing("sf物品未找到:" + id);
                return new ItemStack(Material.STONE);
            }
            default: {
                Material m = matchMaterial(material);
                if (m == null) {
                    HT.missing("材质无效:" + material);
                    return new ItemStack(Material.STONE);
                }
                return new ItemStack(m);
            }
        }
    }

    /** RSC 内置物品。本项目实际使用到生存模式指南书。 */
    private static ItemStack builtIn(String material) {
        if ("slimefun_guide_survival".equalsIgnoreCase(material)) {
            try {
                return SlimefunGuide.getItem(SlimefunGuideMode.SURVIVAL_MODE).clone();
            } catch (Throwable ignored) {
                return new ItemStack(Material.ENCHANTED_BOOK);
            }
        }
        HT.missing("内置物品未识别:" + material);
        return new ItemStack(Material.STONE);
    }

    private static Material matchMaterial(String name) {
        if (name == null) return null;
        if (name.startsWith("minecraft:")) name = name.substring(10);
        Material m = Material.matchMaterial(name);
        if (m != null) return m;
        // 版本感知别名（同 WorldTaste 策略）：材质改名/新增时双向兜底
        switch (name.toUpperCase(Locale.ROOT)) {
            case "GRASS": return Material.matchMaterial("SHORT_GRASS");
            case "SHORT_GRASS": return Material.matchMaterial("GRASS");
            case "SCUTE": return Material.matchMaterial("TURTLE_SCUTE");
            case "TURTLE_SCUTE": return Material.matchMaterial("SCUTE");
            case "ARMADILLO_SCUTE": return Material.matchMaterial("RABBIT_HIDE");
            case "BREEZE_ROD": return Material.matchMaterial("BLAZE_ROD");
            case "WIND_CHARGE": return Material.matchMaterial("SNOWBALL");
            case "OMINOUS_BOTTLE": return Material.matchMaterial("GLASS_BOTTLE");
            case "CHAIN":
            case "IRON_CHAIN": {
                Material chain = Material.matchMaterial("IRON_CHAIN");
                return chain != null ? chain : Material.matchMaterial("CHAIN");
            }
            default: return Material.matchMaterial(name.replace('-', '_'));
        }
    }

    /** 读取配方段，产出长度 size 的数组（空槽为 null）。 */
    public static ItemStack[] recipe(ConfigurationSection recipeSec, int size) {
        ItemStack[] out = new ItemStack[size];
        if (recipeSec == null) return out;
        for (int i = 0; i < size; i++) {
            ConfigurationSection slot = recipeSec.getConfigurationSection(String.valueOf(i + 1));
            if (slot != null) out[i] = item(slot, true);
        }
        return out;
    }

    private static void applyColor(ItemMeta meta, String color) {
        String[] rgb = color.split(",");
        if (rgb.length != 3) return;
        try {
            Color c = Color.fromRGB(Integer.parseInt(rgb[0].trim()),
                    Integer.parseInt(rgb[1].trim()), Integer.parseInt(rgb[2].trim()));
            if (meta instanceof LeatherArmorMeta) ((LeatherArmorMeta) meta).setColor(c);
            else if (meta instanceof PotionMeta) ((PotionMeta) meta).setColor(c);
        } catch (RuntimeException ignored) {
            HT.warn("颜色格式错误: " + color);
        }
    }

    public static void clearCache() {
        HASH_SKINS.clear();
        BASE64_SKINS.clear();
        URL_SKINS.clear();
        SAVED_ITEMS.clear();
    }
}
