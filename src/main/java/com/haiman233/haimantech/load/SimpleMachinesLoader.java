package com.haiman233.haimantech.load;

import com.haiman233.haimantech.HT;
import com.haiman233.haimantech.customs.HTGrowthAccelerators;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.implementation.items.electric.machines.AutoAnvil;
import io.github.thebusybiscuit.slimefun4.implementation.items.electric.machines.AutoBrewer;
import io.github.thebusybiscuit.slimefun4.implementation.items.electric.machines.AutoDrier;
import io.github.thebusybiscuit.slimefun4.implementation.items.electric.machines.CarbonPress;
import io.github.thebusybiscuit.slimefun4.implementation.items.electric.machines.ChargingBench;
import io.github.thebusybiscuit.slimefun4.implementation.items.electric.machines.ElectricDustWasher;
import io.github.thebusybiscuit.slimefun4.implementation.items.electric.machines.ElectricFurnace;
import io.github.thebusybiscuit.slimefun4.implementation.items.electric.machines.ElectricGoldPan;
import io.github.thebusybiscuit.slimefun4.implementation.items.electric.machines.ElectricIngotFactory;
import io.github.thebusybiscuit.slimefun4.implementation.items.electric.machines.ElectricIngotPulverizer;
import io.github.thebusybiscuit.slimefun4.implementation.items.electric.machines.ElectricOreGrinder;
import io.github.thebusybiscuit.slimefun4.implementation.items.electric.machines.ElectricPress;
import io.github.thebusybiscuit.slimefun4.implementation.items.electric.machines.ElectricSmeltery;
import io.github.thebusybiscuit.slimefun4.implementation.items.electric.machines.ElectrifiedCrucible;
import io.github.thebusybiscuit.slimefun4.implementation.items.electric.machines.FoodFabricator;
import io.github.thebusybiscuit.slimefun4.implementation.items.electric.machines.Freezer;
import io.github.thebusybiscuit.slimefun4.implementation.items.electric.machines.HeatedPressureChamber;
import io.github.thebusybiscuit.slimefun4.implementation.items.electric.machines.Refinery;
import io.github.thebusybiscuit.slimefun4.implementation.items.electric.machines.enchanting.AutoDisenchanter;
import io.github.thebusybiscuit.slimefun4.implementation.items.electric.machines.enchanting.AutoEnchanter;
import io.github.thebusybiscuit.slimefun4.implementation.items.electric.machines.enchanting.BookBinder;
import io.github.thebusybiscuit.slimefun4.implementation.items.electric.machines.entities.ProduceCollector;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.abstractItems.AContainer;
import org.bukkit.configuration.ConfigurationSection;

/**
 * 简单机器加载（simple_machines.yml）。对应 RSC 的 {@code SimpleMachineReader + SimpleMachineFactory}。
 *
 * <p>条目字段：type（机器类型）+ settings{capacity, consumption, speed, radius, repair_factor}。
 * 绝大多数类型直接实例化 Slimefun 自带机器类并注入速度/耗电/容量参数；
 * 动物/树木加速器官方类参数硬编码，使用本项目的可配置实现（{@link HTGrowthAccelerators}）。</p>
 */
public final class SimpleMachinesLoader {

    private SimpleMachinesLoader() {}

    public static void load() {
        LoaderSupport.loadFile("simple_machines.yml", (file, id, s) -> {
            if (!RegisterConditions.pass(s)) return false;

            String typeStr = s.getString("type", "");
            ConfigurationSection settings = s.getConfigurationSection("settings");
            if (settings == null) {
                HT.warn(file + " " + id + " 缺少简单机器类型配置 (settings)，已跳过");
                return false;
            }
            Type type = Type.parse(typeStr);
            if (type == null) {
                HT.warn(file + " " + id + " 错误的简单机器类型 (type): " + typeStr + "，已跳过");
                return false;
            }

            int capacity = settings.getInt("capacity", 0);
            int consumption = settings.getInt("consumption", 0);
            int speed = Math.max(1, settings.getInt("speed", 1));
            int radius = Math.max(1, settings.getInt("radius", 1));
            int repairFactor = Math.max(1, settings.getInt("repair_factor", 10));
            if (type.energy && (capacity <= 0 || consumption <= 0)) {
                HT.warn(file + " " + id + " 缺少 capacity/consumption 配置，已跳过");
                return false;
            }

            String effId = LoaderSupport.effId(id, s);
            LoaderSupport.Prepared p = LoaderSupport.prepare(s, effId);
            if (p == null) return false;

            SlimefunItem item = create(type, p, capacity, consumption, speed, radius, repairFactor);
            item.register(HT.plugin);
            return true;
        });
    }

    private static SlimefunItem create(Type type, LoaderSupport.Prepared p,
                                       int capacity, int consumption, int speed, int radius, int repairFactor) {
        return switch (type) {
            case ELECTRIC_FURNACE -> cfg(new ElectricFurnace(p.group(), p.item(), p.recipeType(), p.recipe()),
                    capacity, consumption, speed);
            case ELECTRIC_GOLD_PAN -> cfg(new ElectricGoldPan(p.group(), p.item(), p.recipeType(), p.recipe()),
                    capacity, consumption, speed);
            case ELECTRIC_DUST_WASHER -> cfg(new ElectricDustWasher(p.group(), p.item(), p.recipeType(), p.recipe()),
                    capacity, consumption, speed);
            case ELECTRIC_ORE_GRINDER -> cfg(new ElectricOreGrinder(p.group(), p.item(), p.recipeType(), p.recipe()),
                    capacity, consumption, speed);
            case ELECTRIC_INGOT_FACTORY -> cfg(new ElectricIngotFactory(p.group(), p.item(), p.recipeType(), p.recipe()),
                    capacity, consumption, speed);
            case ELECTRIC_INGOT_PULVERIZER ->
                    cfg(new ElectricIngotPulverizer(p.group(), p.item(), p.recipeType(), p.recipe()),
                            capacity, consumption, speed);
            case ELECTRIC_SMELTERY -> cfg(new ElectricSmeltery(p.group(), p.item(), p.recipeType(), p.recipe()),
                    capacity, consumption, speed);
            case ELECTRIC_CRUCIBLE -> cfg(new ElectrifiedCrucible(p.group(), p.item(), p.recipeType(), p.recipe()),
                    capacity, consumption, speed);
            case ELECTRIC_PRESS -> cfg(new ElectricPress(p.group(), p.item(), p.recipeType(), p.recipe()),
                    capacity, consumption, speed);
            case CARBON_PRESS -> cfg(new CarbonPress(p.group(), p.item(), p.recipeType(), p.recipe()),
                    capacity, consumption, speed);
            case CHARGING_BENCH -> cfg(new ChargingBench(p.group(), p.item(), p.recipeType(), p.recipe()),
                    capacity, consumption, speed);
            case FREEZER -> cfg(new Freezer(p.group(), p.item(), p.recipeType(), p.recipe()),
                    capacity, consumption, speed);
            case FOOD_FABRICATOR -> cfg(new FoodFabricator(p.group(), p.item(), p.recipeType(), p.recipe()),
                    capacity, consumption, speed);
            case HEATED_PRESSURE_CHAMBER ->
                    cfg(new HeatedPressureChamber(p.group(), p.item(), p.recipeType(), p.recipe()),
                            capacity, consumption, speed);
            case AUTO_ENCHANTER -> cfg(new AutoEnchanter(p.group(), p.item(), p.recipeType(), p.recipe()),
                    capacity, consumption, speed);
            case AUTO_DISENCHANTER -> cfg(new AutoDisenchanter(p.group(), p.item(), p.recipeType(), p.recipe()),
                    capacity, consumption, speed);
            case AUTO_DRIER -> cfg(new AutoDrier(p.group(), p.item(), p.recipeType(), p.recipe()),
                    capacity, consumption, speed);
            case AUTO_BREWER -> cfg(new AutoBrewer(p.group(), p.item(), p.recipeType(), p.recipe()),
                    capacity, consumption, speed);
            case BOOK_BINDER -> cfg(new BookBinder(p.group(), p.item(), p.recipeType(), p.recipe()),
                    capacity, consumption, speed);
            case REFINERY -> cfg(new Refinery(p.group(), p.item(), p.recipeType(), p.recipe()),
                    capacity, consumption, speed);
            case PRODUCE_COLLECTOR -> cfg(new ProduceCollector(p.group(), p.item(), p.recipeType(), p.recipe()),
                    capacity, consumption, speed);
            case AUTO_ANVIL -> cfg(new AutoAnvil(p.group(), repairFactor, p.item(), p.recipeType(), p.recipe()),
                    capacity, consumption, speed);
            case ANIMAL_GROWTH_ACCELERATOR ->
                    new HTGrowthAccelerators.Animal(p.group(), p.item(), p.recipeType(), p.recipe(),
                            capacity, radius, consumption);
            case TREE_GROWTH_ACCELERATOR ->
                    new HTGrowthAccelerators.Tree(p.group(), p.item(), p.recipeType(), p.recipe(),
                            capacity, radius, consumption);
            case CROP_GROWTH_ACCELERATOR ->
                    new HTGrowthAccelerators.Crop(p.group(), p.item(), p.recipeType(), p.recipe(),
                            capacity, radius, consumption, speed);
        };
    }

    /** 注入容量/耗电/速度（所有 AContainer 系机器通用）。 */
    private static AContainer cfg(AContainer machine, int capacity, int consumption, int speed) {
        machine.setCapacity(capacity);
        machine.setEnergyConsumption(consumption);
        machine.setProcessingSpeed(speed);
        return machine;
    }

    /** simple_machines.yml 支持的机器类型（与 RSC 的 SimpleMachineType 对齐）。 */
    private enum Type {
        ELECTRIC_SMELTERY(true),
        ELECTRIC_FURNACE(true),
        ELECTRIC_GOLD_PAN(true),
        ELECTRIC_DUST_WASHER(true),
        ELECTRIC_ORE_GRINDER(true),
        ELECTRIC_INGOT_FACTORY(true),
        ELECTRIC_INGOT_PULVERIZER(true),
        CHARGING_BENCH(true),
        ANIMAL_GROWTH_ACCELERATOR(true),
        TREE_GROWTH_ACCELERATOR(true),
        CROP_GROWTH_ACCELERATOR(true),
        FREEZER(true),
        CARBON_PRESS(true),
        ELECTRIC_PRESS(true),
        ELECTRIC_CRUCIBLE(true),
        FOOD_FABRICATOR(true),
        HEATED_PRESSURE_CHAMBER(true),
        AUTO_ENCHANTER(true),
        AUTO_DISENCHANTER(true),
        BOOK_BINDER(true),
        AUTO_ANVIL(true),
        AUTO_DRIER(true),
        AUTO_BREWER(true),
        REFINERY(true),
        PRODUCE_COLLECTOR(true);

        private final boolean energy;

        Type(boolean energy) {
            this.energy = energy;
        }

        static Type parse(String s) {
            if (s == null || s.isEmpty()) return null;
            try {
                return Type.valueOf(s.toUpperCase(java.util.Locale.ROOT));
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
    }
}
