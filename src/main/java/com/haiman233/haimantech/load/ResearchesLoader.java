package com.haiman233.haimantech.load;

import com.haiman233.haimantech.HT;
import com.haiman233.haimantech.util.Colors;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.researches.Research;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * 研究加载（researches.yml）。对应 RSC 的 {@code ResearchReader}。
 *
 * <p>条目字段：id（数字 ID，>0 且不与其它研究重复）/ name / levelCost /
 * currencyCost（可选）/ items[]（粘液物品 ID 列表）。</p>
 *
 * <p>必须在所有物品注册完成之后调用（研究需要把物品挂到研究上）。</p>
 */
public final class ResearchesLoader {

    private ResearchesLoader() {}

    private static final Pattern VALID_KEY = Pattern.compile("[a-z0-9/._-]+");

    public static void load() {
        YamlConfiguration y = Yaml.loadResource("researches.yml");
        int ok = 0, skip = 0;

        for (String key : y.getKeys(false)) {
            ConfigurationSection s = y.getConfigurationSection(key);
            if (s == null) continue;
            try {
                if (registerOne(key, s)) ok++;
                else skip++;
            } catch (Exception e) {
                HT.warn("研究 " + key + " 注册失败: " + e);
                skip++;
            }
        }
        HT.log("researches.yml: 注册 " + ok + ", 跳过 " + skip);
    }

    private static boolean registerOne(String key, ConfigurationSection s) {
        String nsKey = key.toLowerCase(Locale.ROOT);
        if (!VALID_KEY.matcher(nsKey).matches()) {
            HT.warn("研究注册 ID 无效（仅允许 a-z0-9/._-）: " + key + "，已跳过");
            return false;
        }

        int researchId = s.getInt("id", 0);
        if (researchId <= 0) {
            HT.warn("研究 " + key + " 缺少或配置错误 '研究数字 ID' (id)，已跳过");
            return false;
        }
        if (Research.getResearchByID(researchId).isPresent()) {
            HT.warn("研究 " + key + " 的数字 ID " + researchId + " 已被占用，已跳过");
            return false;
        }

        String name = s.getString("name", "");
        if (name.isBlank()) {
            HT.warn("研究 " + key + " 缺少 '名称' (name)，已跳过");
            return false;
        }
        int cost = s.getInt("levelCost", 0);
        if (cost <= 0) {
            HT.warn("研究 " + key + " 缺少或配置错误 '研究等级花费' (levelCost)，已跳过");
            return false;
        }

        Research research;
        if (s.contains("currencyCost")) {
            research = new Research(new NamespacedKey(HT.plugin, nsKey), researchId,
                    Colors.c(name), cost, Math.max(0, s.getDouble("currencyCost", 0)));
        } else {
            research = new Research(new NamespacedKey(HT.plugin, nsKey), researchId, Colors.c(name), cost);
        }

        List<String> items = s.getStringList("items");
        int missing = 0;
        for (String itemId : items) {
            SlimefunItem sf = SlimefunItem.getById(itemId.toUpperCase(Locale.ROOT));
            if (sf == null) {
                missing++;
                HT.missing("研究目标物品未找到:" + itemId);
                continue;
            }
            research.addItems(sf);
        }

        research.register();
        return true;
    }
}
