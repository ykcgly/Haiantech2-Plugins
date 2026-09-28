package com.haiman233.haimantech.load;

import com.haiman233.haimantech.HT;
import com.haiman233.haimantech.customs.HTGenerator;
import java.util.ArrayList;
import java.util.List;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.abstractItems.MachineFuel;
import org.bukkit.configuration.ConfigurationSection;

/**
 * 燃料发电机加载（generators.yml）。对应 RSC 的 {@code GeneratorReader}。
 *
 * <p>条目字段：capacity / production / input[] / output[] / fuels{}（seconds + item + 可选 output）。</p>
 */
public final class GeneratorsLoader {

    private GeneratorsLoader() {}

    public static void load() {
        LoaderSupport.loadFile("generators.yml", (file, id, s) -> {
            if (!RegisterConditions.pass(s)) return false;

            int capacity = s.getInt("capacity", 1);
            int production = s.getInt("production", 0);
            if (production < 1) {
                HT.warn(file + " " + id + " 缺少或配置错误 '产电量' (production)，已跳过");
                return false;
            }

            int[] input = LoaderSupport.slots(s, "input");
            int[] output = LoaderSupport.slots(s, "output");

            List<MachineFuel> fuels = readFuels(file, id, s.getConfigurationSection("fuels"));
            if (fuels.isEmpty()) {
                HT.warn("发电机 " + id + " 不含任何燃料");
            }

            String effId = LoaderSupport.effId(id, s);
            LoaderSupport.Prepared p = LoaderSupport.prepare(s, effId);
            if (p == null) return false;

            new HTGenerator(p.group(), p.item(), p.recipeType(), p.recipe(),
                    input, output, capacity, production, fuels, MenusLoader.get(effId)).register(HT.plugin);
            return true;
        });
    }

    private static List<MachineFuel> readFuels(String file, String id, ConfigurationSection fuels) {
        List<MachineFuel> out = new ArrayList<>();
        if (fuels == null) return out;
        for (String key : fuels.getKeys(false)) {
            ConfigurationSection f = fuels.getConfigurationSection(key);
            if (f == null) continue;

            var item = Read.item(f.getConfigurationSection("item"), true);
            if (item == null) {
                HT.warn(file + " " + id + " 燃料 " + key + " 缺少 '输入物品' (item)，已跳过");
                continue;
            }
            int seconds = f.getInt("seconds", -1);
            if (seconds < 0) {
                HT.warn(file + " " + id + " 燃料 " + key + " 缺少或配置错误 '配方耗时' (seconds)，已跳过");
                continue;
            }

            ConfigurationSection outSec = f.getConfigurationSection("output");
            if (outSec != null) {
                var result = Read.item(outSec, true);
                if (result == null) {
                    HT.warn(file + " " + id + " 燃料 " + key + " 输出物品无效，已跳过");
                    continue;
                }
                out.add(new MachineFuel(seconds, item, result));
            } else {
                out.add(new MachineFuel(seconds, item));
            }
        }
        return out;
    }
}
