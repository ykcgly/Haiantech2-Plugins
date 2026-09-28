package com.haiman233.haimantech.customs;

import com.xzavier0722.mc.plugin.slimefun4.storage.controller.SlimefunBlockData;
import com.xzavier0722.mc.plugin.slimefun4.storage.util.StorageCacheUtils;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.attributes.EnergyNetProvider;
import io.github.thebusybiscuit.slimefun4.core.attributes.MachineProcessHolder;
import io.github.thebusybiscuit.slimefun4.core.machines.MachineProcessor;
import io.github.thebusybiscuit.slimefun4.implementation.SlimefunItems;
import io.github.thebusybiscuit.slimefun4.implementation.handlers.SimpleBlockBreakHandler;
import io.github.thebusybiscuit.slimefun4.implementation.operations.FuelOperation;
import io.github.thebusybiscuit.slimefun4.utils.ChestMenuUtils;
import io.github.thebusybiscuit.slimefun4.utils.SlimefunUtils;
import java.util.List;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.abstractItems.MachineFuel;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.interfaces.InventoryBlock;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenuPreset;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;

/**
 * 燃料发电机（generators.yml）：消耗燃料发电，可带副产物输出。
 *
 * <p>行为与 RSC 的 CustomGenerator 对齐：</p>
 * <ul>
 *   <li>发电过程在能源网络请求 {@code getGeneratedOutput} 时推进（每粘液刻一次）</li>
 *   <li>发电前提：电容还有 {@code production} 的剩余空间，否则本刻暂停</li>
 *   <li>燃料匹配逐槽进行，命中即扣除 {@code fuel.input.amount} 个并开始燃烧</li>
 *   <li>燃烧结束产出副产物（如有），桶类燃料返还空桶</li>
 *   <li>默认界面与 RSC 默认菜单同布局</li>
 * </ul>
 */
public class HTGenerator extends SlimefunItem implements InventoryBlock, EnergyNetProvider,
        MachineProcessHolder<FuelOperation> {

    private final int capacity;
    private final int production;
    private final int[] inputSlots;
    private final int[] outputSlots;
    private final List<MachineFuel> fuels;
    private final HTMenu menu;
    private final int progressSlot;
    private final ItemStack progressBar = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
    private final MachineProcessor<FuelOperation> processor = new MachineProcessor<>(this);

    public HTGenerator(ItemGroup group, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe,
                       int[] inputSlots, int[] outputSlots, int capacity, int production,
                       List<MachineFuel> fuels, HTMenu menu) {
        super(group, item, recipeType, recipe);
        this.capacity = Math.max(1, capacity);
        this.production = Math.max(1, production);
        this.inputSlots = inputSlots;
        this.outputSlots = outputSlots;
        this.fuels = List.copyOf(fuels);
        this.menu = menu;
        this.progressSlot = menu != null && menu.progressSlot() >= 0 ? menu.progressSlot() : HTMachine.DEFAULT_PROGRESS_SLOT;
        if (menu != null && menu.progressBar() != null) {
            processor.setProgressBar(menu.progressBar());
        } else {
            processor.setProgressBar(progressBar);
        }

        addItemHandler(new SimpleBlockBreakHandler() {
            @Override
            public void onBlockBreak(Block b) {
                BlockMenu inv = StorageCacheUtils.getMenu(b.getLocation());
                if (inv != null) {
                    inv.dropItems(b.getLocation(), getInputSlots());
                    inv.dropItems(b.getLocation(), getOutputSlots());
                }
                processor.endOperation(b.getLocation());
            }
        });
        // 标题与布局由菜单决定；无菜单时回退机器名 + 默认布局（对应 RSC CustomGenerator）
        String title = menu != null && menu.title() != null ? menu.title() : getItemName();
        createPreset(this, title, preset -> {
            if (menu != null) {
                menu.apply(preset);
            } else {
                constructMenu(preset);
            }
        });
    }

    private void constructMenu(BlockMenuPreset preset) {
        HTMachine.buildDefaultMenu(preset, progressBar);
    }

    @Override
    public int getCapacity() {
        return capacity;
    }

    @Override
    public MachineProcessor<FuelOperation> getMachineProcessor() {
        return processor;
    }

    public int getEnergyProduction() {
        return production;
    }

    @Override
    public int[] getInputSlots() {
        return inputSlots;
    }

    @Override
    public int[] getOutputSlots() {
        return outputSlots;
    }

    /** 由能源网络每刻调用：推进燃烧进度并返回本刻发电量。 */
    @Override
    public int getGeneratedOutput(Location l, SlimefunBlockData data) {
        BlockMenu inv = StorageCacheUtils.getMenu(l);
        if (inv == null) return 0;

        FuelOperation op = processor.getOperation(l);
        if (op != null) {
            if (!op.isFinished()) {
                if (inv.hasViewer()) {
                    processor.updateProgressBar(inv, progressSlot, op);
                }
                // 电容快满时暂停燃烧（与 RSC 一致，避免溢出浪费）
                if (getCapacity() - getCharge(l) < getEnergyProduction()) {
                    return 0;
                }
                op.addProgress(1);
                return getEnergyProduction();
            }

            // 燃烧结束：产出副产物 + 桶返还
            ItemStack ingredient = op.getIngredient();
            if (isBucket(ingredient)) {
                inv.pushItem(new ItemStack(Material.BUCKET), outputSlots);
            }
            ItemStack result = op.getResult();
            if (result != null && !result.getType().isAir()) {
                inv.pushItem(result.clone(), outputSlots);
            }
            // 燃烧结束恢复进度槽显示（与 RSC 一致：菜单装饰优先，缺省背景板）
            if (inv.hasViewer()) {
                inv.replaceExistingItem(progressSlot, menu != null
                        ? menu.progressItemAt(progressSlot, ChestMenuUtils.getBackground())
                        : progressBar.clone());
            }
            processor.endOperation(l);
            return 0;
        }

        // 无进行中操作：找燃料并点燃
        for (MachineFuel fuel : fuels) {
            for (int slot : inputSlots) {
                ItemStack cur = inv.getItemInSlot(slot);
                if (cur == null || cur.getType().isAir()) continue;
                if (!fuel.test(cur)) continue;
                int amount = Math.max(1, fuel.getInput().getAmount());
                inv.consumeItem(slot, amount);
                processor.startOperation(l, new FuelOperation(fuel));
                return 0;
            }
        }
        return 0;
    }

    private static boolean isBucket(ItemStack item) {
        if (item == null) return false;
        Material type = item.getType();
        if (type == Material.LAVA_BUCKET || type == Material.WATER_BUCKET) return true;
        // Slimefun 的燃料桶/油桶（与 RSC 一致，燃烧后返还空桶）
        return SlimefunUtils.isItemSimilar(item, SlimefunItems.FUEL_BUCKET, true, false)
                || SlimefunUtils.isItemSimilar(item, SlimefunItems.OIL_BUCKET, true, false);
    }
}
