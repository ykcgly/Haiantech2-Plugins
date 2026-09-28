package com.haiman233.haimantech.customs;

import io.github.thebusybiscuit.slimefun4.api.MinecraftVersion;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.SlimefunItems;
import io.github.thebusybiscuit.slimefun4.implementation.items.electric.machines.accelerators.AbstractGrowthAccelerator;
import io.github.thebusybiscuit.slimefun4.implementation.items.electric.machines.accelerators.CropGrowthAccelerator;
import io.github.thebusybiscuit.slimefun4.utils.SlimefunUtils;
import io.github.thebusybiscuit.slimefun4.utils.compatibility.VersionedParticle;
import io.github.thebusybiscuit.slimefun4.utils.itemstack.ItemStackWrapper;
import me.mrCookieSlime.Slimefun.api.BlockStorage;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Sapling;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;

/**
 * 可配置参数的生长加速器（simple_machines.yml 的三种 accelerator 类型）。
 *
 * <p>原版 Slimefun 的动物/树木加速器参数为硬编码常量，RSC 为此封装了可配置版本，
 * 这里以同样的 tick 逻辑实现（镜像官方源码），参数来自 yml：</p>
 * <ul>
 *   <li>{@code capacity} 电容容量，{@code consumption} 每次加速耗电，{@code radius} 作用半径</li>
 *   <li>作物加速器另有 {@code speed}（每刻尝试加速的作物数语义由官方逻辑决定）</li>
 * </ul>
 */
public final class HTGrowthAccelerators {

    private HTGrowthAccelerators() {}

    // ---------------------------------------------------------------- 动物

    /** 动物生长加速器：消耗有机食物（ORGANIC_FOOD）催熟周围的幼年生物。 */
    public static class Animal extends AbstractGrowthAccelerator {

        private static final ItemStack ORGANIC_FOOD = ItemStackWrapper.wrap(SlimefunItems.ORGANIC_FOOD);

        private final int capacity;
        private final int consumption;
        private final double radius;

        public Animal(ItemGroup group, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe,
                      int capacity, int radius, int consumption) {
            super(group, item, recipeType, recipe);
            this.capacity = Math.max(1, capacity);
            this.consumption = Math.max(1, consumption);
            this.radius = Math.max(1, radius);
        }

        @Override
        public int getCapacity() {
            return capacity;
        }

        @Override
        protected void tick(Block b) {
            BlockMenu inv = BlockStorage.getInventory(b);
            if (inv == null) return;

            for (Entity n : b.getWorld().getNearbyEntities(b.getLocation(), radius, radius, radius, this::isReadyToGrow)) {
                for (int slot : getInputSlots()) {
                    if (!SlimefunUtils.isItemSimilar(inv.getItemInSlot(slot), ORGANIC_FOOD, false, false)) continue;
                    if (getCharge(b.getLocation()) < consumption) return;

                    Ageable ageable = (Ageable) n;
                    removeCharge(b.getLocation(), consumption);
                    inv.consumeItem(slot);
                    ageable.setAge(ageable.getAge() + 2000);
                    if (ageable.getAge() > 0) ageable.setAge(0);

                    n.getWorld().spawnParticle(VersionedParticle.HAPPY_VILLAGER,
                            ((LivingEntity) n).getEyeLocation(), 8, 0.2F, 0.2F, 0.2F);
                    return;
                }
            }
        }

        private boolean isReadyToGrow(Entity n) {
            return n instanceof Ageable ageable && n.isValid() && !ageable.isAdult();
        }
    }

    // ---------------------------------------------------------------- 树木

    /** 树木生长加速器：消耗肥料（FERTILIZER）催熟周围的树苗（1.17+ 模拟骨粉）。 */
    public static class Tree extends AbstractGrowthAccelerator {

        private static final ItemStack FERTILIZER = ItemStackWrapper.wrap(SlimefunItems.FERTILIZER);

        private final int capacity;
        private final int consumption;
        private final int radius;

        public Tree(ItemGroup group, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe,
                    int capacity, int radius, int consumption) {
            super(group, item, recipeType, recipe);
            this.capacity = Math.max(1, capacity);
            this.consumption = Math.max(1, consumption);
            this.radius = Math.max(1, radius);
        }

        @Override
        public int getCapacity() {
            return capacity;
        }

        @Override
        protected void tick(Block b) {
            BlockMenu inv = BlockStorage.getInventory(b);
            if (inv == null) return;
            if (getCharge(b.getLocation()) < consumption) return;

            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    Block block = b.getRelative(x, 0, z);
                    if (!Tag.SAPLINGS.isTagged(block.getType())) continue;
                    if (boost(b, inv, block)) return;
                }
            }
        }

        private boolean boost(Block machine, BlockMenu inv, Block sapling) {
            if (Slimefun.getMinecraftVersion().isAtLeast(MinecraftVersion.MINECRAFT_1_17)) {
                return applyBoneMeal(machine, inv, sapling);
            }
            Sapling data = (Sapling) sapling.getBlockData();
            if (data.getStage() >= data.getMaximumStage()) return false;
            for (int slot : getInputSlots()) {
                if (!SlimefunUtils.isItemSimilar(inv.getItemInSlot(slot), FERTILIZER, false, false)) continue;
                removeCharge(machine.getLocation(), consumption);
                data.setStage(data.getStage() + 1);
                sapling.setBlockData(data, false);
                inv.consumeItem(slot);
                sapling.getWorld().spawnParticle(VersionedParticle.HAPPY_VILLAGER,
                        sapling.getLocation().add(0.5D, 0.5D, 0.5D), 4, 0.1F, 0.1F, 0.1F);
                return true;
            }
            return false;
        }

        private boolean applyBoneMeal(Block machine, BlockMenu inv, Block sapling) {
            for (int slot : getInputSlots()) {
                if (!SlimefunUtils.isItemSimilar(inv.getItemInSlot(slot), FERTILIZER, false, false)) continue;
                removeCharge(machine.getLocation(), consumption);
                sapling.applyBoneMeal(BlockFace.UP);
                inv.consumeItem(slot);
                sapling.getWorld().spawnParticle(VersionedParticle.HAPPY_VILLAGER,
                        sapling.getLocation().add(0.5D, 0.5D, 0.5D), 4, 0.1F, 0.1F, 0.1F);
                return true;
            }
            return false;
        }
    }

    // ---------------------------------------------------------------- 作物

    /** 作物生长加速器：复用官方逻辑（官方类为抽象），仅注入可配置参数。 */
    public static class Crop extends CropGrowthAccelerator {

        private final int capacity;
        private final int consumption;
        private final int radius;
        private final int speed;

        public Crop(ItemGroup group, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe,
                    int capacity, int radius, int consumption, int speed) {
            super(group, item, recipeType, recipe);
            this.capacity = Math.max(1, capacity);
            this.consumption = Math.max(1, consumption);
            this.radius = Math.max(1, radius);
            this.speed = Math.max(1, speed);
        }

        @Override
        public int getCapacity() {
            return capacity;
        }

        @Override
        public int getEnergyConsumption() {
            return consumption;
        }

        @Override
        public int getRadius() {
            return radius;
        }

        @Override
        public int getSpeed() {
            return speed;
        }
    }
}
