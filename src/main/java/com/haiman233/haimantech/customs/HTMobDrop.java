package com.haiman233.haimantech.customs;

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.attributes.NotPlaceable;
import io.github.thebusybiscuit.slimefun4.core.attributes.RandomMobDrop;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;

/**
 * 怪物掉落物。对应 RSC 的 {@code CustomMobDrop}。
 *
 * <p>实现 {@link RandomMobDrop} 后 Slimefun 会在实体死亡时按 {@link #getMobDropChance()} 判定掉落；
 * 同时实现 {@link NotPlaceable} 禁止放置。</p>
 */
public class HTMobDrop extends SlimefunItem implements RandomMobDrop, NotPlaceable {

    private final int chance;
    private final EntityType entityType;

    public HTMobDrop(
            ItemGroup group,
            SlimefunItemStack sfis,
            RecipeType recipeType,
            ItemStack[] recipe,
            ItemStack recipeOutput,
            int chance,
            EntityType entityType) {
        super(group, sfis, recipeType, recipe, recipeOutput);
        this.chance = chance;
        this.entityType = entityType;
    }

    @Override
    public int getMobDropChance() {
        return chance >= 100 ? 100 : Math.max(chance, 1);
    }

    public EntityType getEntityType() {
        return entityType;
    }
}
