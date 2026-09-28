package com.haiman233.haimantech.customs;

import com.xzavier0722.mc.plugin.slimefun4.storage.controller.SlimefunBlockData;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.attributes.EnergyNetProvider;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;

/**
 * 太阳能发电机（solar_generators.yml）。
 *
 * <p>与 RSC 的 CustomSolarGenerator 同语义：</p>
 * <ul>
 *   <li>仅主世界发电</li>
 *   <li>白天判定：非雷暴天气且时间在 12300~23850 之外</li>
 *   <li>上方方块的天空光照需 ≥ lightLevel（默认 15）</li>
 *   <li>白天输出 dayEnergy，夜晚输出 nightEnergy</li>
 * </ul>
 */
public class HTSolarGenerator extends SlimefunItem implements EnergyNetProvider {

    private final int dayEnergy;
    private final int nightEnergy;
    private final int capacity;
    private final int lightLevel;

    public HTSolarGenerator(ItemGroup group, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe,
                            int dayEnergy, int nightEnergy, int capacity, int lightLevel) {
        super(group, item, recipeType, recipe);
        this.dayEnergy = Math.max(0, dayEnergy);
        this.nightEnergy = Math.max(0, nightEnergy);
        this.capacity = Math.max(1, capacity);
        this.lightLevel = Math.min(15, Math.max(0, lightLevel));
    }

    @Override
    public int getCapacity() {
        return capacity;
    }

    public int getDayEnergy() {
        return dayEnergy;
    }

    public int getNightEnergy() {
        return nightEnergy;
    }

    @Override
    public int getGeneratedOutput(Location l, SlimefunBlockData data) {
        World world = l.getWorld();
        if (world == null || world.getEnvironment() != World.Environment.NORMAL) {
            return 0;
        }
        boolean daytime = isDaytime(world);
        if (!daytime && nightEnergy < 1) {
            return 0;
        }
        if (!world.isChunkLoaded(l.getBlockX() >> 4, l.getBlockZ() >> 4)
                || l.getBlock().getRelative(0, 1, 0).getLightFromSky() < lightLevel) {
            return 0;
        }
        return daytime ? dayEnergy : nightEnergy;
    }

    private static boolean isDaytime(World world) {
        long time = world.getTime();
        return !world.hasStorm() && !world.isThundering() && (time < 12300L || time > 23850L);
    }
}
