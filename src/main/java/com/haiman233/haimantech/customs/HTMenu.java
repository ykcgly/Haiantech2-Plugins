package com.haiman233.haimantech.customs;

import io.github.thebusybiscuit.slimefun4.utils.ChestMenuUtils;
import java.util.Map;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenuPreset;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * 机器同名自定义菜单（menus.yml）。
 *
 * <p>RSC 中菜单与机器通过"菜单 ID == 机器 ID"隐式关联（无 menu 字段），
 * 机器构造 preset 时若存在同名菜单则<b>只应用菜单的槽位装饰</b>（不再绘制默认边框），
 * 标题与进度条物品也由菜单提供。</p>
 *
 * <ul>
 *   <li>{@code title} —— 界面标题；null/空白时回退机器物品名</li>
 *   <li>{@code items} —— 槽位 → 装饰物品；装饰物点击不可拿取（与 RSC 的
 *       RSCClickHandler 返回 false 一致）</li>
 *   <li>{@code progressSlot} —— 菜单里 {@code progressbar: true} 的槽位；-1 表示未标记
 *       （使用处回退默认 22 号槽）</li>
 *   <li>{@code progressBar} —— 运行期进度条物品（{@code progressBarItem} 或标记槽位自身）；
 *       可空，为空时机器保持默认黑色玻璃板</li>
 * </ul>
 */
public class HTMenu {

    private final String title;
    private final Map<Integer, ItemStack> items;
    private final int progressSlot;
    private final ItemStack progressBar;

    public HTMenu(String title, Map<Integer, ItemStack> items, int progressSlot, ItemStack progressBar) {
        this.title = (title == null || title.isBlank()) ? null : title;
        this.items = Map.copyOf(items);
        this.progressSlot = progressSlot;
        this.progressBar = progressBar;
    }

    /** 界面标题；null 表示使用机器物品名。 */
    public String title() {
        return title;
    }

    /** 进度槽位；-1 表示菜单未标记（回退默认 22）。 */
    public int progressSlot() {
        return progressSlot;
    }

    /** 进度条物品；null 表示保持机器默认。 */
    public ItemStack progressBar() {
        return progressBar;
    }

    /** 闲置/完成时进度或状态槽应恢复显示的物品：优先菜单装饰，缺省时回退 fallback。 */
    public ItemStack progressItemAt(int slot, ItemStack fallback) {
        ItemStack it = items.get(slot);
        return (it != null ? it : fallback).clone();
    }

    /** 应用到机器 preset（对应 RSC 的 {@code CustomMenu.apply}）：仅放置装饰物品。 */
    public void apply(BlockMenuPreset preset) {
        for (Map.Entry<Integer, ItemStack> e : items.entrySet()) {
            ItemStack item = e.getValue().clone();
            ItemMeta meta = item.getItemMeta();
            if (meta != null && !meta.hasDisplayName()) {
                // 与 RSC 一致：无名称的装饰物品统一设为空格名
                meta.setDisplayName(" ");
                item.setItemMeta(meta);
            }
            preset.addItem(e.getKey(), item);
            preset.addMenuClickHandler(e.getKey(), ChestMenuUtils.getEmptyClickHandler());
        }
    }
}
