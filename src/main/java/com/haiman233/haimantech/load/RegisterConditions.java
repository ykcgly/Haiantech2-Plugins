package com.haiman233.haimantech.load;

import com.haiman233.haimantech.HT;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
 *   <li>{@code version <op> <ver>}          —— 服务端 MC 版本比较，op 支持 {@code < <= > >= == = !=}</li>
 *   <li>{@code unfinished true}             —— 未完成的条目直接跳过</li>
 * </ul>
 *
 * <p>另支持 {@code register: {warn: true}} 在跳过时输出告警。</p>
 *
 * <p><b>version 条件</b>：content 中同一物品常按服务端版本提供多个条目（如 {@code DIAOYUJI}
 * 配 {@code version < 1.21}、{@code VERSION_DIAOYUJI} 配 {@code version >= 1.21}），
 * 二者通过 {@code id_alias} 共用同一个粘液 ID。若不做版本过滤，两个条目会同时通过条件，
 * 先注册者（通常是旧版本定义）胜出、后者按 ID 冲突被跳过 —— 既刷屏，也导致实际注册的是
 * 与当前服务端不匹配的那个版本。本类实现版本比较后，只有与服务端匹配的那一个会被注册。</p>
 */
public final class RegisterConditions {

    private RegisterConditions() {}

    /** version 条件：{@code version <op> 1.21.11}，允许写作 {@code version>=1.21}（无空格）。 */
    private static final Pattern VERSION_CONDITION =
            Pattern.compile("version\\s*(<=|>=|==|!=|=|<|>)\\s*([0-9]+(?:\\.[0-9]+)*)", Pattern.CASE_INSENSITIVE);

    /** 服务端 MC 版本段（如 1.21.11 -> [1,21,11]），首次使用时解析并缓存。 */
    private static int[] serverVersion;

    /** 返回 true 表示通过注册条件。 */
    public static boolean pass(ConfigurationSection s) {
        if (s == null) return true;
        ConfigurationSection reg = s.getConfigurationSection("register");
        if (reg == null) return true;

        boolean warn = reg.getBoolean("warn", false);
        if (reg.getBoolean("unfinished", false)) return false;

        List<String> conditions = reg.getStringList("conditions");
        for (String condition : conditions) {
            if (condition == null || condition.isBlank()) continue;

            // version 条件优先匹配：可能无空格（version>=1.21），split(" ") 切不出来
            Matcher vm = VERSION_CONDITION.matcher(condition.trim());
            if (vm.matches()) {
                if (versionFilterEnabled() && !matches(vm.group(1), vm.group(2))) {
                    if (warn) {
                        HT.warn("当前服务端 " + serverVersionText() + " 不满足 [" + condition + "]，跳过: " + s.getName());
                    }
                    return false;
                }
                continue;
            }

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

    /**
     * 仅判定 version 条件，供预加载阶段使用。
     *
     * <p>预加载在物品注册之前执行，此时 {@code itemexist} 等依赖注册表/预加载表的条件尚不可靠，
     * 故这里只看版本条件，避免展示堆被与服务端不匹配的那个版本覆盖。</p>
     */
    public static boolean passVersion(ConfigurationSection s) {
        if (s == null) return true;
        ConfigurationSection reg = s.getConfigurationSection("register");
        if (reg == null) return true;
        if (!versionFilterEnabled()) return true;

        for (String condition : reg.getStringList("conditions")) {
            if (condition == null || condition.isBlank()) continue;
            Matcher vm = VERSION_CONDITION.matcher(condition.trim());
            if (vm.matches() && !matches(vm.group(1), vm.group(2))) return false;
        }
        return true;
    }

    /** 版本条件过滤开关（config.yml 的 version-filter，默认 true = 严格按版本选择条目）。 */
    private static boolean versionFilterEnabled() {
        try {
            return HT.plugin.getConfig().getBoolean("version-filter", true);
        } catch (Throwable t) {
            return true;
        }
    }

    /** 按操作符比较服务端版本与条件版本。 */
    private static boolean matches(String op, String required) {
        int cmp = compare(serverVersion(), segments(required));
        switch (op) {
            case "<":
                return cmp < 0;
            case "<=":
                return cmp <= 0;
            case ">":
                return cmp > 0;
            case ">=":
                return cmp >= 0;
            case "==":
            case "=":
                return cmp == 0;
            case "!=":
                return cmp != 0;
            default:
                return true;
        }
    }

    private static int[] serverVersion() {
        if (serverVersion == null) {
            String raw = "0";
            try {
                String v = Bukkit.getBukkitVersion(); // 形如 "1.21.11-R0.1-SNAPSHOT"
                if (v != null && !v.isEmpty()) {
                    int dash = v.indexOf('-');
                    raw = (dash > 0) ? v.substring(0, dash) : v;
                }
            } catch (Throwable ignored) {
                // 取不到版本时按 0 处理，条件判定会退化为不匹配
            }
            serverVersion = segments(raw);
        }
        return serverVersion;
    }

    private static String serverVersionText() {
        int[] v = serverVersion();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < v.length; i++) {
            if (i > 0) sb.append('.');
            sb.append(v[i]);
        }
        return sb.toString();
    }

    /** 版本字符串 -> 数字段；非数字段记 0，位数不足按 0 补齐（1.21 与 1.21.0 等价）。 */
    private static int[] segments(String raw) {
        String[] parts = raw.trim().split("\\.");
        int[] out = new int[Math.max(3, parts.length)];
        for (int i = 0; i < parts.length && i < out.length; i++) {
            try {
                out[i] = Integer.parseInt(parts[i].trim());
            } catch (NumberFormatException e) {
                out[i] = 0;
            }
        }
        return out;
    }

    private static int compare(int[] a, int[] b) {
        int n = Math.max(a.length, b.length);
        for (int i = 0; i < n; i++) {
            int x = (i < a.length) ? a[i] : 0;
            int y = (i < b.length) ? b[i] : 0;
            if (x != y) return (x < y) ? -1 : 1;
        }
        return 0;
    }

    /** 物品是否存在：已注册物品优先，其次预加载表（跨文件前向引用）。 */
    private static boolean exists(String id) {
        String u = id.toUpperCase(Locale.ROOT);
        return SlimefunItem.getById(u) != null || HT.preload(u) != null;
    }
}
