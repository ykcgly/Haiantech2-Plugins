package com.haiman233.haimantech.load;

import com.haiman233.haimantech.HT;
import com.haiman233.haimantech.customs.HTMaterialGenerator;
import com.haiman233.haimantech.customs.HTRecipe;
import java.util.List;
import org.bukkit.configuration.ConfigurationSection;

/**
 * 材料生成器加载（mat_generators.yml）。对应 RSC 的 {@code MaterialGeneratorReader}。
 *
 * <p>条目字段：capacity / per（每刻耗电）/ tickRate（每 N 粘液刻产出一次）/
 * output[]（输出槽）/ status（状态槽，可为 -1）/ outputItem 或 outputs{}（产出与概率）。</p>
 */
public final class MaterialGeneratorsLoader {

    private MaterialGeneratorsLoader() {}

    public static void load() {
        LoaderSupport.loadFile("mat_generators.yml", (file, id, s) -> {
            if (!RegisterConditions.pass(s)) return false;

            int capacity = s.getInt("capacity", 0);
            if (capacity < 0) {
                HT.warn(file + " " + id + " 配置错误 '能源容量' (capacity)，已跳过");
                return false;
            }
            int per = s.getInt("per", 0);
            if (per < 0) {
                HT.warn(file + " " + id + " 配置错误 '能量消耗' (per)，已跳过");
                return false;
            }
            int tickRate = s.getInt("tickRate", 0);
            if (tickRate < 1) {
                HT.warn(file + " " + id + " 配置错误 '配方耗时' (tickRate)，已跳过");
                return false;
            }
            int[] output = LoaderSupport.slots(s, "output");
            if (output.length == 0) {
                HT.warn(file + " " + id + " 缺少 '输出槽' (output)，已跳过");
                return false;
            }
            int status = s.getInt("status", -1);

            String effId = LoaderSupport.effId(id, s);
            LoaderSupport.Prepared p = LoaderSupport.prepare(s, effId);
            if (p == null) return false;

            List<HTRecipe.Output> outputs = RecipeMachinesLoader.readOutputs(file, id, null,
                    s.getConfigurationSection("outputs"));
            ConfigurationSection single = s.getConfigurationSection("outputItem");
            if (single != null) {
                var stack = Read.item(single, true);
                if (stack != null) {
                    int chance = Math.min(100, Math.max(1, single.getInt("chance", 100)));
                    outputs.add(new HTRecipe.Output(stack, chance));
                } else {
                    HT.warn(file + " " + id + " 物品配置错误 (outputItem)");
                }
            }
            if (outputs.isEmpty()) {
                HT.warn(file + " " + id + " 缺少产出物品 (outputItem/outputs)，已跳过");
                return false;
            }

            HTRecipe recipe = HTRecipe.ofTicks(tickRate, List.of(), outputs, s.getBoolean("chooseOne", false));
            new HTMaterialGenerator(p.group(), p.item(), p.recipeType(), p.recipe(),
                    output, per, capacity, List.of(recipe), status, MenusLoader.get(effId)).register(HT.plugin);
            return true;
        });
    }
}
