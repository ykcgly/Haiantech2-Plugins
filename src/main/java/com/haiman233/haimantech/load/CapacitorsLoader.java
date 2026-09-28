package com.haiman233.haimantech.load;

import com.haiman233.haimantech.HT;
import io.github.thebusybiscuit.slimefun4.implementation.items.electric.Capacitor;

/**
 * 电容加载（capacitors.yml）。对应 RSC 的 {@code CapacitorsReader}，直接使用
 * Slimefun 自带的 {@link Capacitor}（CAPACITOR 能量组件，可存可放）。
 *
 * <p>条目字段：capacity（必须 > 0）。</p>
 */
public final class CapacitorsLoader {

    private CapacitorsLoader() {}

    public static void load() {
        LoaderSupport.loadFile("capacitors.yml", (file, id, s) -> {
            if (!RegisterConditions.pass(s)) return false;

            int capacity = s.getInt("capacity", -1);
            if (capacity <= 0) {
                HT.warn(file + " " + id + " 缺少或配置错误 '电容容量' (capacity)，已跳过");
                return false;
            }

            String effId = LoaderSupport.effId(id, s);
            LoaderSupport.Prepared p = LoaderSupport.prepare(s, effId);
            if (p == null) return false;

            new Capacitor(p.group(), capacity, p.item(), p.recipeType(), p.recipe()).register(HT.plugin);
            return true;
        });
    }
}
