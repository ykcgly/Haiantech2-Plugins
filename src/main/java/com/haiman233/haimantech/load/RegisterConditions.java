package com.haiman233.haimantech.load;

import com.haiman233.haimantech.HT;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import java.util.List;
import java.util.Locale;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;

/**
 * 注册条件判定，对应 RSC 的 {@code YamlReader#checkForRegistration}（yml 的 {@code register:} 段）。
 *
 * <p>支持的条件（与 RSC 同语义）：</p>
 * <ul>
 *   <li>{@code hasplugin <name>}            —— 指定插件已启用</li>
 *   <li>{@code !hasplugin <name>}           —— 指定插件未启用</li>
 *   <li>{@code itemexist <id>}              —— 指定粘液物品存在</li>
 *   <li>{@code !itemexist <id>}             —— 指定粘液物品不存在</li>
 *   <li>{@code unfinished true}             —— 未完成的条目直接跳过</li>
 * </ul>
 *
 * <p>另支持 {@code register: {warn: true}} 在跳过时输出告警。</p>
 */
public final class RegisterConditions {

    private RegisterConditions() {}

    /** 返回 true 表示通过注册条件。 */
    public static boolean pass(ConfigurationSection s) {
        if (s == null) return true;
        ConfigurationSection reg = s.getConfigurationSection("register");
        if (reg == null) return true;

        boolean warn = reg.getBoolean("warn", false);
        if (reg.getBoolean("unfinished", false)) return false;

        List<String> conditions = reg.getStringList("conditions");
        for (String condition : conditions) {
            String[] sp = condition.split(" ");
            String head = sp[0];
            if (head.equalsIgnoreCase("hasplugin")) {
                if (sp.length != 2) continue;
                if (Bukkit.getPluginManager().getPlugin(sp[1]) == null) {
                    if (warn) HT.warn("需要服务端插件 " + sp[1] + " 才能注册: " + s.getName());
                    return false;
                }
            } else if (head.equalsIgnoreCase("!hasplugin")) {
                if (sp.length != 2) continue;
                if (Bukkit.getPluginManager().getPlugin(sp[1]) != null) {
                    if (warn) HT.warn("需要卸载服务端插件 " + sp[1] + " 才能注册: " + s.getName());
                    return false;
                }
            } else if (head.equalsIgnoreCase("itemexist")) {
                if (sp.length != 2) continue;
                if (exists(sp[1])) {
                    // 需要存在才算通过，存在则继续
                } else {
                    if (warn) HT.warn("需要物品 " + sp[1] + " 才能注册: " + s.getName());
                    return false;
                }
            } else if (head.equalsIgnoreCase("!itemexist")) {
                if (sp.length != 2) continue;
                if (exists(sp[1])) {
                    if (warn) HT.warn("需要物品 " + sp[1] + " 不存在才能注册: " + s.getName());
                    return false;
                }
            }
        }
        return true;
    }

    /** 物品是否存在：已注册物品优先，其次预加载表（跨文件前向引用）。 */
    private static boolean exists(String id) {
        String u = id.toUpperCase(Locale.ROOT);
        return SlimefunItem.getById(u) != null || HT.preload(u) != null;
    }
}
