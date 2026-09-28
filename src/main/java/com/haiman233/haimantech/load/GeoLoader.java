package com.haiman233.haimantech.load;

import com.haiman233.haimantech.HT;
import com.haiman233.haimantech.customs.HTGeoResource;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import java.util.Locale;
import java.util.function.BiFunction;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

/**
 * 自然资源（geo_resources.yml）。对应 RSC 的 {@code GeoResourceReader} + {@code CustomGeoResource}。
 *
 * <p>{@code supply} 段支持两种写法，与 RSC 完全一致：</p>
 * <ul>
 *   <li>扁平：{@code supply: {normal: 3, nether: 1}} —— 键是世界环境名小写</li>
 *   <li>分群系：{@code supply: {normal: {plains: 5, others: 2}}} —— 二级键是群系名小写，缺失时回退 {@code others}</li>
 * </ul>
 */
public final class GeoLoader {

    private GeoLoader() {}

    public static void load() {
        YamlConfiguration y = Yaml.loadResource("geo_resources.yml");
        if (y.getKeys(false).isEmpty()) return;

        int ok = 0, skip = 0;
        for (String id : y.getKeys(false)) {
            ConfigurationSection s = y.getConfigurationSection(id);
            if (s == null) continue;
            try {
                if (register(id, s)) ok++;
                else skip++;
            } catch (Exception e) {
                HT.warn("geo_resources.yml " + id + " 注册失败: " + e);
                skip++;
            }
        }
        HT.plugin.getLogger().info("geo_resources.yml: 注册 " + ok + ", 跳过 " + skip);
    }

    private static boolean register(String id, ConfigurationSection s) {
        String effId = s.getString("id_alias", id).toUpperCase(Locale.ROOT);

        // RSC 语义：ID 已被占用时跳过
        if (io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem.getById(effId) != null) {
            HT.warn("geo_resources.yml " + id + " ID 冲突: " + effId + " 已被占用，按 RSC 语义跳过");
            return false;
        }

        ItemGroup g = HT.group(s.getString("item_group"));
        if (g == null) {
            HT.missing("物品组缺失:" + s.getString("item_group"));
            return false;
        }

        ItemStack display = HT.preload(effId);
        if (display == null) display = HT.preload(id);
        if (display == null) {
            HT.missing("无展示物品:" + effId);
            return false;
        }

        SlimefunItemStack sfis = new SlimefunItemStack(effId, display);
        RecipeType rt = RecipeTypes.resolve(s.getString("recipe_type", "GEO_MINER"));
        ItemStack[] recipe = Read.recipe(s.getConfigurationSection("recipe"), 9);

        int maxDeviation = s.getInt("max_deviation", 1);
        boolean obtainable = s.getBoolean("obtain_from_geo_miner", true);
        String geoName = s.getString("geo_name", "");
        BiFunction<World.Environment, Biome, Integer> supply = buildSupply(s.getConfigurationSection("supply"));

        NamespacedKey key = new NamespacedKey(HT.plugin, effId.toLowerCase(Locale.ROOT));
        HTGeoResource res = new HTGeoResource(g, sfis, rt, recipe, supply, maxDeviation, obtainable, geoName, key);

        res.register(HT.plugin); // Slimefun 物品
        ((io.github.thebusybiscuit.slimefun4.api.geo.GEOResource) res).register(); // GEO 储量注册表
        return true;
    }

    private static BiFunction<World.Environment, Biome, Integer> buildSupply(ConfigurationSection sup) {
        return (env, biome) -> {
            if (sup == null || env == World.Environment.CUSTOM) return 0;
            String envKey = env.name().toLowerCase(Locale.ROOT);
            if (!sup.isConfigurationSection(envKey)) return sup.getInt(envKey, 0);

            ConfigurationSection biomes = sup.getConfigurationSection(envKey);
            if (biomes == null) return 0;
            String biomeKey = biome.toString().toLowerCase(Locale.ROOT);
            return biomes.contains(biomeKey) ? biomes.getInt(biomeKey, 0) : biomes.getInt("others", 0);
        };
    }
}
