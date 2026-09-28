package com.haiman233.haimantech.load;

import com.haiman233.haimantech.HT;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import java.lang.reflect.Field;
import java.util.Locale;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

/**
 * 配方类型解析与自定义配方类型注册，对应 RSC 的 {@code CommonUtils.getRecipeType} + {@code RecipeTypeReader}。
 *
 * <p>解析顺序与 RSC 一致：</p>
 * <ol>
 *   <li>recipe_types.yml 注册的自定义类型</li>
 *   <li>{@link RecipeType} 自身的静态字段（反射取字段名，如 ENHANCED_CRAFTING_TABLE / NULL）</li>
 *   <li>{@code minecraft:} 命名空间形式（如 {@code minecraft:crafting_table}）</li>
 *   <li>兜底为 {@link RecipeType#NULL} 并折叠上报</li>
 * </ol>
 */
public final class RecipeTypes {

    private RecipeTypes() {}

    /** 自定义配方类型],[RecipeType]（key 为 yml 顶层 key 的大写形式）。 */
    public static void load() {
        YamlConfiguration y = Yaml.loadResource("recipe_types.yml");
        int ok = 0;
        for (String id : y.getKeys(false)) {
            ConfigurationSection s = y.getConfigurationSection(id);
            if (s == null) continue;
            if (s.getBoolean("lateInit", false)) continue; // 延后项
            if (registerOne(id, s)) ok++;
        }
        HT.log("recipe_types.yml: 注册 " + ok);
    }

    private static boolean registerOne(String id, ConfigurationSection s) {
        ItemStack display = Read.item(s, false);
        if (display == null) return false;
        String key = id.toUpperCase(Locale.ROOT);
        try {
            NamespacedKey nk = new NamespacedKey(HT.plugin, key.toLowerCase(Locale.ROOT));
            RecipeType rt = new RecipeType(nk, display);
            HT.CUSTOM_RECIPE_TYPES.put(key, rt);
            return true;
        } catch (Throwable e) {
            HT.warn("注册自定义配方类型 " + id + " 失败: " + e);
            return false;
        }
    }

    /**
     * 解析配置里的 {@code recipe_type} 字符串为 {@link RecipeType}。
     * 与 RSC 保持一致优先走静态字段反射，因此其它附属/原版提供的类型也能被正确引用。
     */
    public static RecipeType resolve(String name) {
        if (name == null || name.isEmpty()) return RecipeType.NULL;
        String key = name.toUpperCase(Locale.ROOT);

        RecipeType custom = HT.CUSTOM_RECIPE_TYPES.get(key);
        if (custom != null) return custom;

        // 1) RecipeType 静态字段名（NULL / ENHANCED_CRAFTING_TABLE / MAGIC_WORKBENCH …）
        try {
            Field f = RecipeType.class.getDeclaredField(key);
            Object v = f.get(null);
            if (v instanceof RecipeType) return (RecipeType) v;
        } catch (ReflectiveOperationException ignored) {
            // 非静态字段而已，继续走后续分支
        }

        // 2) minecraft:xxx 形式
        if (key.startsWith("MINECRAFT:")) {
            String maybe = key.substring("MINECRAFT:".length());
            try {
                Field f = RecipeType.class.getDeclaredField(maybe);
                Object v = f.get(null);
                if (v instanceof RecipeType) return (RecipeType) v;
            } catch (ReflectiveOperationException ignored) {
                // ignore
            }
        }

        HT.missing("配方类型未识别:" + name);
        return RecipeType.NULL;
    }
}
