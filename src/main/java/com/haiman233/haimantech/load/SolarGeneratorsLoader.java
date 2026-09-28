package com.haiman233.haimantech.load;

import com.haiman233.haimantech.HT;
import com.haiman233.haimantech.customs.HTSolarGenerator;

/**
 * 太阳能发电机加载（solar_generators.yml）。对应 RSC 的 {@code SolarGeneratorReader}。
 *
 * <p>条目字段：dayEnergy / nightEnergy / capacity / lightLevel（默认 15）。</p>
 */
public final class SolarGeneratorsLoader {

    private SolarGeneratorsLoader() {}

    public static void load() {
        LoaderSupport.loadFile("solar_generators.yml", (file, id, s) -> {
            if (!RegisterConditions.pass(s)) return false;

            int dayEnergy = s.getInt("dayEnergy", 0);
            int nightEnergy = s.getInt("nightEnergy", 0);
            if (dayEnergy < 1) {
                HT.warn(file + " " + id + " 缺少或配置错误 '白天产电量' (dayEnergy)，已跳过");
                return false;
            }
            if (nightEnergy < 1) {
                HT.warn(file + " " + id + " 缺少或配置错误 '夜晚产电量' (nightEnergy)，已跳过");
                return false;
            }
            int capacity = s.getInt("capacity", 1);
            int lightLevel = s.getInt("lightLevel", 15);
            if (lightLevel < 0 || lightLevel > 15) {
                HT.warn(file + " " + id + " 缺少或配置错误 '所需光照等级' (lightLevel)，已跳过");
                return false;
            }

            String effId = LoaderSupport.effId(id, s);
            LoaderSupport.Prepared p = LoaderSupport.prepare(s, effId);
            if (p == null) return false;

            new HTSolarGenerator(p.group(), p.item(), p.recipeType(), p.recipe(),
                    dayEnergy, nightEnergy, capacity, lightLevel).register(HT.plugin);
            return true;
        });
    }
}
