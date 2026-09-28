package com.haiman233.haimantech.load;

import com.haiman233.haimantech.HT;
import com.haiman233.haimantech.customs.HTOrePopulator;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

/**
 * generations.yml 加载器（矿物世界生成）。对应 RSC 的 {@code GenerationReader}。
 *
 * <p>条目字段：{@code slimefun_id}（矿物物品 ID，大写）+ {@code areas{1..n}}
 * （maxHeight/minHeight/most/amount/maxSize/minSize/replacement/environment）。</p>
 *
 * <p>与 RSC 的差异：RSC 读取 {@code mixHeight}（拼写笔误导致 minHeight 失效），
 * 本项目按正确语义读取 {@code minHeight}（兼容读 {@code mixHeight}）。
 * slimefun_id 在内容中无定义时跳过该条目（与 RSC 静默跳过一致，另留 WARN 痕迹）——
 * 当前内容 HMXC 条目引用的 HAIMAN_STAR_DUST 即属此类死配置。</p>
 */
public final class GenerationsLoader {

    private GenerationsLoader() {}

    /** 返回已解析的矿物定义；须在物品注册完成后调用。 */
    static List<HTOrePopulator.Info> load() {
        List<HTOrePopulator.Info> infos = new ArrayList<>();
        var y = Yaml.loadResource("generations.yml");
        int ok = 0, skip = 0;
        for (String id : y.getKeys(false)) {
            ConfigurationSection s = y.getConfigurationSection(id);
            if (s == null) continue;
            try {
                if (register(id, s, infos)) ok++;
                else skip++;
            } catch (Exception e) {
                HT.warn("generations.yml " + id + " 解析失败: " + e);
                skip++;
            }
        }
        HT.log("generations.yml: 注册 " + ok + ", 跳过 " + skip);
        return infos;
    }

    private static boolean register(String id, ConfigurationSection s, List<HTOrePopulator.Info> infos) {
        String sfId = s.getString("slimefun_id", "");
        if (sfId.isEmpty()) {
            HT.warn("generations.yml " + id + " 缺少 slimefun_id，已跳过");
            return false;
        }
        String effId = sfId.toUpperCase(Locale.ROOT);
        ItemStack pre = HT.preload(effId);
        SlimefunItemStack stack = pre != null ? new SlimefunItemStack(effId, pre) : null;
        if (stack == null) {
            // 与 RSC 一致：矿物物品不存在则整条跳过（当前 HMXC 条目即死配置）
            HT.warn("generations.yml " + id + " 引用的物品不存在 (slimefun_id): " + effId + "，已跳过");
            return false;
        }

        ConfigurationSection areaSec = s.getConfigurationSection("areas");
        if (areaSec == null) {
            HT.warn("generations.yml " + id + " 缺少生成区域 (areas)，已跳过");
            return false;
        }

        List<HTOrePopulator.Area> areas = new ArrayList<>();
        int c = 1;
        while (areaSec.contains(String.valueOf(c))) {
            ConfigurationSection a = areaSec.getConfigurationSection(String.valueOf(c));
            if (a == null) {
                HT.warn("generations.yml " + id + " 无效的生成区域 (areas): " + c);
                c++;
                continue;
            }
            int maxHeight = a.getInt("maxHeight");
            int minHeight = a.contains("minHeight") ? a.getInt("minHeight") : a.getInt("mixHeight");
            int most = a.getInt("most");
            int amount = Math.min(200, a.getInt("amount", 1));
            int minSize = a.getInt("minSize", 1);
            int maxSize = a.getInt("maxSize", 1);
            Material replacement = Material.matchMaterial(a.getString("replacement", ""));
            if (replacement == null) {
                HT.warn("generations.yml " + id + " 替换材质无效 (replacement): "
                        + a.getString("replacement") + "，按 STONE 处理");
                replacement = Material.STONE;
            }
            World.Environment env;
            try {
                env = World.Environment.valueOf(a.getString("environment", "NORMAL"));
            } catch (IllegalArgumentException e) {
                HT.warn("generations.yml " + id + " 环境无效 (environment): "
                        + a.getString("environment") + "，按 NORMAL 处理");
                env = World.Environment.NORMAL;
            }
            areas.add(new HTOrePopulator.Area(minHeight, maxHeight, most, amount, minSize, maxSize, replacement, env));
            c++;
        }

        if (areas.isEmpty()) {
            HT.warn("generations.yml " + id + " 无有效生成区域，已跳过");
            return false;
        }
        infos.add(new HTOrePopulator.Info(stack, areas));
        return true;
    }
}
