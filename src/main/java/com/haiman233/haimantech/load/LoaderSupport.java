package com.haiman233.haimantech.load;

import com.haiman233.haimantech.HT;
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
 * 机器类加载器共用骨架：lateInit 两遍遍历 + 统计日志 + 机器条目公共字段读取。
 * 与 {@link ItemsLoader} 的注册范式保持一致。
 */
final class LoaderSupport {

    private LoaderSupport() {}

    /** 单条目注册回调；返回 false 记为跳过。 */
    interface Entry {
        boolean register(String file, String id, ConfigurationSection s);
    }

    /** 通用两遍加载（第一遍非 lateInit，第二遍 lateInit），逐条 try/catch 故障隔离。 */
    static void loadFile(String file, Entry handler) {
        YamlConfiguration y = Yaml.loadResource(file);
        List<ConfigurationSection> late = new ArrayList<>();
        List<String> lateIds = new ArrayList<>();
        int ok = 0, skip = 0;

        for (String id : y.getKeys(false)) {
            ConfigurationSection s = y.getConfigurationSection(id);
            if (s == null) continue;
            if (s.getBoolean("lateInit", false)) {
                late.add(s);
                lateIds.add(id);
                continue;
            }
            try {
                if (handler.register(file, id, s)) ok++;
                else skip++;
            } catch (Exception e) {
                HT.warn(file + " " + id + " 注册失败: " + e);
                skip++;
            }
        }
        for (int i = 0; i < late.size(); i++) {
            try {
                if (handler.register(file, lateIds.get(i), late.get(i))) ok++;
                else skip++;
            } catch (Exception e) {
                HT.warn(file + " " + lateIds.get(i) + "(lateInit) 注册失败: " + e);
                skip++;
            }
        }
        HT.log(file + ": 注册 " + ok + ", 跳过 " + skip);
    }

    /** 机器条目公共字段：组 + 展示堆 + 配方类型 + 合成配方；缺失时折叠上报并返回 null。 */
    record Prepared(ItemGroup group, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe) {}

    static Prepared prepare(ConfigurationSection s, String effId) {
        // RSC 语义：ID 已被占用时跳过（VERSIONED_ 变体与基础条目共用 id_alias 的预期重复）
        String upper = effId.toUpperCase(Locale.ROOT);
        if (SlimefunItem.getById(upper) != null || SlimefunItem.getById(effId) != null) {
            HT.warn("ID 冲突: " + upper + " 已被占用，按 RSC 语义跳过");
            return null;
        }

        ItemGroup g = HT.group(s.getString("item_group"));
        if (g == null) {
            HT.missing("物品组缺失:" + s.getString("item_group"));
            return null;
        }
        ItemStack display = HT.preload(effId);
        if (display == null) {
            HT.missing("无展示物品:" + effId);
            return null;
        }
        SlimefunItemStack sfis = new SlimefunItemStack(effId, display);
        RecipeType rt = RecipeTypes.resolve(s.getString("recipe_type", "NULL"));
        ItemStack[] recipe = Read.recipe(s.getConfigurationSection("recipe"), 9);
        return new Prepared(g, sfis, rt, recipe);
    }

    static String effId(String id, ConfigurationSection s) {
        return s.getString("id_alias", id).toUpperCase(Locale.ROOT);
    }

    /** 读取槽位列表（如 input/output），非法值过滤为 0-53 范围内整数。 */
    static int[] slots(ConfigurationSection s, String key) {
        List<Integer> list = s.getIntegerList(key);
        List<Integer> valid = new ArrayList<>();
        for (Integer v : list) {
            if (v != null && v >= 0 && v <= 53) valid.add(v);
        }
        int[] out = new int[valid.size()];
        for (int i = 0; i < out.length; i++) out[i] = valid.get(i);
        return out;
    }
}
