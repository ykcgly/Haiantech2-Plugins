package com.haiman233.haimantech.behavior;

import io.github.thebusybiscuit.slimefun4.api.events.PlayerRightClickEvent;
import io.github.thebusybiscuit.slimefun4.core.handlers.ItemUseHandler;
import java.util.Locale;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryView;

/**
 * 把 RSC 版 scripts/ 下的 11 个 JS 脚本改写为原生 Java 行为。
 *
 * <p>这样顶层不再依赖 RSC 的脚本引擎（GraalVM/Nashorn），物品右键直接由插件自身处理，
 * 同时省去脚本热加载开销。各脚本与函数的对应关系：</p>
 * <ul>
 *   <li>{@code tiezhen} 铁砧 · {@code duanzaotai} 锻造台 · {@code fumotai} 附魔台</li>
 *   <li>{@code moshi} 磨石 · {@code qieshiji} 切石机 · {@code zhitutai} 制图台 · {@code zhibuji} 织布机</li>
 *   <li>{@code seed} 世界种子 · {@code biao} 区块信息 · {@code zhibiao}/{@code zhibiao2} 玩家状态监测</li>
 * </ul>
 */
public final class ScriptBehaviors {

    private ScriptBehaviors() {}

    /** 按 yml 的 {@code script} 名称返回对应行为，未实现返回 null。 */
    public static ItemUseHandler forName(String script) {
        if (script == null) return null;
        switch (script.toLowerCase(Locale.ROOT)) {
            case "tiezhen":    return openGui((p, l) -> p.openAnvil(l, true), "铁砧");
            case "duanzaotai": return openGui((p, l) -> p.openSmithingTable(l, true), "锻造台");
            case "fumotai":    return openGui((p, l) -> p.openEnchanting(l, true), "附魔台");
            case "moshi":      return openGui((p, l) -> p.openGrindstone(l, true), "磨石");
            case "qieshiji":   return openGui((p, l) -> p.openStonecutter(l, true), "切石机");
            case "zhitutai":   return openGui((p, l) -> p.openCartographyTable(l, true), "制图台");
            case "zhibuji":    return openGui((p, l) -> p.openLoom(l, true), "织布机");
            case "seed":        return ScriptBehaviors::seed;
            case "biao":        return ScriptBehaviors::chunkInfo;
            case "zhibiao":     return ScriptBehaviors::vitals;
            case "zhibiao2":    return ScriptBehaviors::vitals2;
            default:            return null;
        }
    }

    /** 打开原版 GUI 的通用行为。返回值为 null 表示未能打开（如被阻挡）。 */
    private static ItemUseHandler openGui(GuiOpener opener, String label) {
        return e -> {
            e.cancel();
            Player p = e.getPlayer();
            try {
                InventoryView view = opener.open(p, p.getLocation());
                if (view != null) {
                    p.sendMessage("§a已打开" + label + "界面。");
                } else {
                    p.sendMessage("§c无法打开" + label + "界面。");
                }
            } catch (Throwable t) {
                p.sendMessage("§c打开" + label + "界面失败。");
            }
        };
    }

    /** 原版 GUI 打开动作。均定义在 {@link org.bukkit.entity.HumanEntity} 上，返回 null 代表失败。 */
    @FunctionalInterface
    private interface GuiOpener {
        InventoryView open(Player p, Location loc);
    }

    /**
     * 世界种子。原 JS 以 OP 身份执行 {@code /minecraft:seed}；
     * 这里直接读取世界种子，效果等价且不提升权限（更安全）。
     */
    private static void seed(PlayerRightClickEvent e) {
        e.cancel();
        Player p = e.getPlayer();
        long s = p.getWorld().getSeed();
        p.sendMessage("§a当前世界种子: §e" + s);
    }

    /** 区块信息。 */
    private static void chunkInfo(PlayerRightClickEvent e) {
        e.cancel();
        Player p = e.getPlayer();
        var chunk = p.getLocation().getChunk();
        p.sendMessage("§a您所处的区块为(§e" + chunk.getX() + "§a, §e" + chunk.getZ() + "§a)");
        p.sendMessage("§a是否为史莱姆区块：§e" + (chunk.isSlimeChunk() ? "是" : "否"));
        p.sendMessage("§a是否为强加载区块：§e" + (chunk.isForceLoaded() ? "是" : "否"));
        p.sendMessage("§a是否能生成生物：§e" + (chunk.isEntitiesLoaded() ? "是" : "否"));
        p.sendMessage("§a是否已加载：§e" + (chunk.isLoaded() ? "是" : "否"));
        p.sendMessage("§a区块加载等级：§e" + chunk.getLoadLevel());
        p.sendMessage("§a此区块被玩家居住的时间：§e" + chunk.getInhabitedTime());
    }

    /** 玩家状态监测（生命/饥饿/氧气等）。 */
    private static void vitals(PlayerRightClickEvent e) {
        e.cancel();
        Player p = e.getPlayer();
        p.sendMessage("§a监测玩家：§e" + p.getName());
        p.sendMessage("§a生命值：§e" + p.getHealth());
        p.sendMessage("§a饥饿值：§e" + p.getFoodLevel());
        p.sendMessage("§a饱和度：§e" + p.getSaturation());
        p.sendMessage("§a消耗值：§e" + p.getExhaustion());
        p.sendMessage("§a最大氧气值：§e" + p.getMaximumAir());
        p.sendMessage("§a剩余氧气值：§e" + p.getRemainingAir());
        p.sendMessage("§a伤害吸收量：§e" + p.getAbsorptionAmount());
        p.sendMessage("§a冰冻剩余时间(刻)：§e" + p.getFreezeTicks());
    }

    /** 玩家状态监测（经验/床/死亡点等）。 */
    private static void vitals2(PlayerRightClickEvent e) {
        e.cancel();
        Player p = e.getPlayer();
        p.sendMessage("§a监测玩家：§e" + p.getName());
        p.sendMessage("§a升级所需经验：§e" + p.getExpToLevel());
        p.sendMessage("§a附魔种子：§e" + p.getEnchantmentSeed());
        p.sendMessage("§a床的位置：§e" + loc(p.getPotentialBedLocation()));
        p.sendMessage("§a死亡位置：§e" + loc(p.getLastDeathLocation()));
        p.sendMessage("§a满饥饿值生命恢复速率：§e" + p.getSaturatedRegenRate());
        p.sendMessage("§a半饥饿值生命恢复速率：§e" + p.getUnsaturatedRegenRate());
        p.sendMessage("§a饥饿掉血速率：§e" + p.getStarvationRate());
    }

    private static String loc(org.bukkit.Location location) {
        if (location == null) return "无";
        return "(" + location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ() + ")";
    }
}
