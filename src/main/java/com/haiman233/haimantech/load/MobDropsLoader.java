package com.haiman233.haimantech.load;

import com.haiman233.haimantech.HT;
import com.haiman233.haimantech.customs.HTMobDrop;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * 怪物掉落（mob_drops.yml）。对应 RSC 的 {@code MobDropsReader} + {@code CustomMobDrop}。
 *
 * <p>每个条目注册为一个带 {@code RandomMobDrop} 属性的 Slimefun 物品，
 * 其配方展示为「生物刷怪蛋 + 概率说明」，配方类型为 {@link RecipeType#MOB_DROP}。</p>
 */
public final class MobDropsLoader {

    private MobDropsLoader() {}

    public static void load() {
        YamlConfiguration y = Yaml.loadResource("mob_drops.yml");
        int ok = 0, skip = 0;

        for (String id : y.getKeys(false)) {
            ConfigurationSection s = y.getConfigurationSection(id);
            if (s == null) continue;
            try {
                if (register(id, s)) ok++;
                else skip++;
            } catch (Exception e) {
                HT.warn("mob_drops.yml " + id + " 注册失败: " + e);
                skip++;
            }
        }
        HT.plugin.getLogger().info("mob_drops.yml: 注册 " + ok + ", 跳过 " + skip);
    }

    private static boolean register(String id, ConfigurationSection s) {
        String effId = s.getString("id_alias", id).toUpperCase(Locale.ROOT);

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

        String raw = s.getString("entity");
        EntityType entity = parseEntity(raw);
        if (entity == null) {
            HT.warn("mob_drops.yml " + id + " 生物类型无效: " + raw + "，跳过");
            return false;
        }

        int chance = s.getInt("chance", 100);
        if (chance < 1) chance = 1;
        else if (chance > 100) chance = 100;

        SlimefunItemStack sfis = new SlimefunItemStack(effId, display);
        ItemStack[] recipe = buildRecipe(entity, chance);

        HTMobDrop item = new HTMobDrop(g, sfis, RecipeType.MOB_DROP, recipe, sfis, chance, entity);
        item.register(HT.plugin);

        // 与 RSC 一致：把物品堆挂进 Slimefun 的掉落表；不依赖 load() 的调用时机，直接写入
        Slimefun.getRegistry()
                .getMobDrops()
                .computeIfAbsent(entity, k -> new HashSet<>())
                .add(item.getItem());
        return true;
    }

    /** 展示用配方：中央放该生物的刷怪蛋，并附一行「击杀 X 时有 N% 概率掉落」的说明。 */
    private static ItemStack[] buildRecipe(EntityType entity, int chance) {
        ItemStack[] recipe = new ItemStack[9];
        Material egg = Material.matchMaterial(entity.toString() + "_SPAWN_EGG");
        ItemStack center = new ItemStack(egg != null ? egg : Material.EGG);
        ItemMeta meta = center.getItemMeta();
        if (meta != null) {
            meta.lore(List.of(dropLore(entity, chance)));
            center.setItemMeta(meta);
        }
        recipe[4] = center;
        return recipe;
    }

    private static Component dropLore(EntityType entity, int chance) {
        LegacyComponentSerializer ser = LegacyComponentSerializer.legacyAmpersand();
        return ser.deserialize("&a击杀 ")
                .append(ser.deserialize("&b"))
                .append(Component.translatable(entity.translationKey()))
                .append(ser.deserialize(" &a时会有"))
                .append(ser.deserialize(" &b " + chance + "%"))
                .append(ser.deserialize(" &a的概率掉落"));
    }

    private static EntityType parseEntity(String raw) {
        if (raw == null || raw.isEmpty()) return null;
        try {
            return EntityType.valueOf(raw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
