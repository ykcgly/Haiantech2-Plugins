package com.haiman233.haimantech.load;

import com.haiman233.haimantech.HT;
import com.haiman233.haimantech.customs.HTMenu;
import com.haiman233.haimantech.util.Colors;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

/**
 * menus.yml 加载器：把同名菜单与机器（菜单 ID == 机器 ID）关联起来。
 *
 * <p>对应 RSC 的 MenuReader。本项目内容实际使用的字段只有：</p>
 * <ul>
 *   <li>{@code title} —— 界面标题（支持 & 颜色与 {#hex}），283/283 使用</li>
 *   <li>{@code slots} —— 槽位装饰物品，键支持单槽位（"22"）与区间（"0-8"）；
 *       槽内物品段与物品通用格式一致（{@link Read#item}），2609 个槽位中仅
 *       HM_ZJSPDFQ 使用区间写法</li>
 *   <li>槽位段内 {@code progressbar: true} 标记进度槽（217 处），可选
 *       {@code progressBarItem} 覆盖运行期进度条物品（213 处），缺省用槽位物品本身</li>
 * </ul>
 *
 * <p>{@code size/import/matrix/script/playerInvClickable} 未被内容使用，暂不支持；
 * 若未来出现将按 RSC 语义补齐。</p>
 */
public final class MenusLoader {

    private static final Map<String, HTMenu> MENUS = new HashMap<>();

    private MenusLoader() {}

    /** 必须在所有机器类加载器之前调用。 */
    static void load() {
        MENUS.clear();
        LoaderSupport.loadFile("menus.yml", (file, id, s) -> {
            String key = id.toUpperCase(Locale.ROOT);
            String title = Colors.c(s.getString("title"));
            int progressSlot = -1;
            ItemStack progressBar = null;
            Map<Integer, ItemStack> items = new HashMap<>();

            ConfigurationSection slots = s.getConfigurationSection("slots");
            if (slots == null) {
                HT.warn(file + " " + id + " 缺少槽位装饰 (slots)");
                return false;
            }

            for (String k : slots.getKeys(false)) {
                ConfigurationSection itemSec = slots.getConfigurationSection(k);
                ItemStack stack = Read.item(itemSec, false);
                if (stack == null) {
                    HT.warn(file + " " + id + " 槽位物品无效 (slots." + k + ")，已跳过");
                    continue;
                }
                boolean progressMark = itemSec != null && itemSec.getBoolean("progressbar", false);

                if (k.chars().allMatch(Character::isDigit)) {
                    int slot = Integer.parseInt(k);
                    if (slot < 0 || slot > 53) {
                        HT.warn(file + " " + id + " 槽位超出范围: " + k + " (0-53)，已跳过");
                        continue;
                    }
                    items.put(slot, stack);
                    if (progressMark) {
                        progressSlot = slot;
                        ConfigurationSection barSec = itemSec.getConfigurationSection("progressBarItem");
                        ItemStack bar = barSec != null ? Read.item(barSec, false) : null;
                        progressBar = bar != null ? bar : stack;
                    }
                } else {
                    // 区间写法 "a-b"
                    String[] range = k.split("-");
                    if (range.length != 2 || range[0].isBlank() || range[1].isBlank()) {
                        HT.warn(file + " " + id + " 槽位区间表达式非法 (slots): " + k);
                        continue;
                    }
                    try {
                        int a = Integer.parseInt(range[0].trim());
                        int b = Integer.parseInt(range[1].trim());
                        for (int i = Math.min(a, b); i <= Math.max(a, b); i++) {
                            if (i < 0 || i > 53) {
                                HT.warn(file + " " + id + " 槽位超出范围: " + k + " -> " + i + " (0-53)，已跳过");
                                continue;
                            }
                            items.put(i, stack);
                        }
                    } catch (NumberFormatException e) {
                        HT.warn(file + " " + id + " 槽位区间表达式非法 (slots): " + k);
                        continue;
                    }
                    if (progressMark) {
                        HT.warn(file + " " + id + " 区间槽位不支持 progressbar 标记，已忽略: " + k);
                    }
                }
            }

            MENUS.put(key, new HTMenu(title, items, progressSlot, progressBar));
            return true;
        });
    }

    /** 按机器 ID（大写）查同名菜单；不存在返回 null（机器使用默认布局）。 */
    public static HTMenu get(String machineId) {
        return machineId == null ? null : MENUS.get(machineId);
    }
}
