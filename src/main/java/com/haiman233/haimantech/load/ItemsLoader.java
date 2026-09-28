package com.haiman233.haimantech.load;

import com.haiman233.haimantech.HT;
import com.haiman233.haimantech.behavior.ScriptBehaviors;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

/**
 * 物品加载（items.yml / simple_machines.yml 等纯物品类文件）。对应 RSC 的 {@code ItemReader}。
 *
 * <p>支持：register.conditions、lateInit（两遍）、id_alias、placeable、
 * 以及 {@code script} 字段（RSC 的 JS 脚本物品 → 本项目的原生 Java 行为）。</p>
 */
public final class ItemsLoader {

    private ItemsLoader() {}

    public static void load() {
        loadFile("items.yml");
    }

    public static void loadFile(String file) {
        YamlConfiguration y = Yaml.loadResource(file);
        List<ConfigurationSection> late = new ArrayList<>();
        List<String> lateIds = new ArrayList<>();
        int ok = 0, skip = 0;

        // 第一遍：非 lateInit
        for (String id : y.getKeys(false)) {
            ConfigurationSection s = y.getConfigurationSection(id);
            if (s == null) continue;
            if (s.getBoolean("lateInit", false)) {
                late.add(s);
                lateIds.add(id);
                continue;
            }
            try {
                if (register(file, id, s)) ok++; else skip++;
            } catch (Exception e) {
                HT.warn(file + " " + id + " 注册失败: " + e);
                skip++;
            }
        }
        // 第二遍：lateInit
        for (int i = 0; i < late.size(); i++) {
            try {
                if (register(file, lateIds.get(i), late.get(i))) ok++; else skip++;
            } catch (Exception e) {
                HT.warn(file + " " + lateIds.get(i) + "(lateInit) 注册失败: " + e);
                skip++;
            }
        }
        HT.plugin.getLogger().info(file + ": 注册 " + ok + ", 跳过 " + skip);
    }

    /** 通用物品注册，成功返回 true。 */
    public static boolean register(String file, String id, ConfigurationSection s) {
        if (!RegisterConditions.pass(s)) return false;

        String effId = s.getString("id_alias", id);
        String effIdUpper = effId.toUpperCase(Locale.ROOT);

        ItemGroup g = HT.group(s.getString("item_group"));
        if (g == null) {
            HT.missing("物品组缺失:" + s.getString("item_group"));
            return false;
        }

        ItemStack display = HT.preload(effIdUpper);
        if (display == null) display = HT.preload(id);
        if (display == null) {
            HT.missing("无展示物品:" + effId);
            return false;
        }

        SlimefunItemStack sfis = new SlimefunItemStack(effId, display);
        RecipeType rt = RecipeTypes.resolve(s.getString("recipe_type", "NULL"));
        ItemStack[] recipe = Read.recipe(s.getConfigurationSection("recipe"), 9);

        SlimefunItem item = new SlimefunItem(g, sfis, rt, recipe);

        boolean placeable = s.getBoolean("placeable", true);
        try {
            item.setUseableInWorkbench(!placeable);
        } catch (Throwable ignored) {
            // 老旧 API 不支持时忽略
        }

        // script 字段：RSC 的 JS 脚本 → 原生 Java 交互行为
        String script = s.getString("script");
        if (script != null && !script.isEmpty()) {
            var handler = ScriptBehaviors.forName(script);
            if (handler != null) item.addItemHandler(handler);
            else HT.missing("脚本未实现:" + script);
        }

        item.register(HT.plugin);
        return true;
    }
}
