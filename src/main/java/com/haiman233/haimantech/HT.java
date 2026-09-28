package com.haiman233.haimantech;

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import org.bukkit.inventory.ItemStack;

/**
 * 全局运行时状态。对应 WorldTaste 的 {@code WT}。
 *
 * <p>ID 规则与 RSC 保持一致：{@code ProjectAddon.getId()} 仅做 {@code toUpperCase(Locale.ROOT)}
 * （本项目 info.yml 未配 idPattern），故物品 Slimefun ID = 配置中的 key 大写。
 * 这保证由 RSC 版迁移过来的旧存档物品不会失效。</p>
 */
public final class HT {

    private HT() {}

    /** 插件实例（onEnable 时赋值）。 */
    public static HaimanTechPlugin plugin;

    /** 物品组表：group key(大写) -> ItemGroup。 */
    private static final Map<String, ItemGroup> GROUPS = new HashMap<>();

    /** 预加载的展示物品堆：有效 item id(大写) -> ItemStack。 */
    public static final Map<String, ItemStack> PRELOAD = new HashMap<>();

    /** 自定义配方类型表：名称(大写) -> RecipeType（recipe_types.yml 注册）。 */
    public static final Map<String, io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType> CUSTOM_RECIPE_TYPES =
            new HashMap<>();

    /** 物品组 Tier（用于 nested 组的排序前提），非必要。保留以便后续扩展。 */
    public static final Map<String, Integer> GROUP_TIERS = new HashMap<>();

    /** 缺失项折叠计数（组/材质/物品 id），加载结束后汇总输出一行，避免逐条刷屏。 */
    public static final Map<String, Integer> MISSING = new TreeMap<>();

    public static ItemStack preload(String id) {
        return PRELOAD.get(id.toUpperCase(Locale.ROOT));
    }

    public static void putPreload(String id, ItemStack stack) {
        PRELOAD.put(id.toUpperCase(Locale.ROOT), stack);
    }

    /** 先在本附属组内找，再回退到 Slimefun 全局已注册组（可引用其它附属/原版的组）。 */
    public static ItemGroup group(String id) {
        if (id == null) return null;
        String key = id.toUpperCase(Locale.ROOT);
        ItemGroup g = GROUPS.get(key);
        if (g != null) return g;
        for (ItemGroup ig : io.github.thebusybiscuit.slimefun4.implementation.Slimefun.getRegistry().getAllItemGroups()) {
            if (ig.getKey().getKey().equalsIgnoreCase(id)) return ig;
        }
        return null;
    }

    public static void putGroup(String id, ItemGroup g) {
        GROUPS.put(id.toUpperCase(Locale.ROOT), g);
    }

    /** 记录一个缺失项（折叠计数）。 */
    public static void missing(String what) {
        MISSING.merge(what, 1, Integer::sum);
    }

    public static void log(String msg) {
        if (plugin != null) plugin.getLogger().info(msg);
    }

    public static void warn(String msg) {
        if (plugin != null) plugin.getLogger().warning(msg);
    }
}
