package com.haiman233.haimantech.customs;

import io.github.thebusybiscuit.slimefun4.api.geo.GEOResource;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.items.blocks.UnplaceableBlock;
import java.util.Locale;
import java.util.function.BiFunction;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.inventory.ItemStack;

/**
 * 自然资源（GEO 矿机产出）。对应 RSC 的 {@code CustomGeoResource}：
 * 既是 Slimefun 物品（{@link UnplaceableBlock}），又实现 {@link GEOResource} 供 GEO 扫描器/矿机读取储量。
 */
public class HTGeoResource extends UnplaceableBlock implements GEOResource {

    private final BiFunction<World.Environment, Biome, Integer> supply;
    private final int maxDeviation;
    private final boolean obtainableFromGEOMiner;
    private final String geoName;
    private final NamespacedKey key;

    public HTGeoResource(
            ItemGroup group,
            SlimefunItemStack item,
            RecipeType recipeType,
            ItemStack[] recipe,
            BiFunction<World.Environment, Biome, Integer> supply,
            int maxDeviation,
            boolean obtainableFromGEOMiner,
            String geoName,
            NamespacedKey key) {
        super(group, item, recipeType, recipe);
        this.supply = supply;
        this.maxDeviation = maxDeviation;
        this.obtainableFromGEOMiner = obtainableFromGEOMiner;
        this.geoName = geoName;
        this.key = key;
    }

    @Override
    public int getDefaultSupply(World.Environment environment, Biome biome) {
        return supply.apply(environment, biome);
    }

    @Override
    public int getMaxDeviation() {
        return maxDeviation;
    }

    @Override
    public String getName() {
        return geoName;
    }

    @Override
    public boolean isObtainableFromGEOMiner() {
        return obtainableFromGEOMiner;
    }

    @Override
    public NamespacedKey getKey() {
        return key;
    }
}
