package com.haiman233.haimantech.customs;

import com.destroystokyo.paper.profile.PlayerProfile;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.libraries.dough.skins.PlayerHead;
import io.github.thebusybiscuit.slimefun4.libraries.dough.skins.PlayerSkin;
import java.util.List;
import java.util.Random;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.inventory.meta.SkullMeta;

/**
 * 矿物世界生成（generations.yml）。对应 RSC 的 GenerationInfo/GenerationArea/BlockPopulator。
 *
 * <p>行为完全对齐 RSC：</p>
 * <ul>
 *   <li>区块生成期（BlockPopulator）按环境过滤后，每区块尝试 {@code amount} 次</li>
 *   <li>高度按以 {@code most} 为峰的三角分布选取（RSC 原算法）</li>
 *   <li>矿脉从随机起点随机游走（+x/+y/+z），仅替换命中 {@code replacement} 材质的方块</li>
 *   <li>放置后向 Slimefun 数据库注册方块（{@code createBlock}），破坏时掉落对应粘液物品；
 *       玩家头材质额外写入皮肤贴图</li>
 * </ul>
 *
 * <p>注：RSC 读取 {@code mixHeight}（拼写笔误），本项目按正确语义读取 {@code minHeight}
 * （并兼容 {@code mixHeight}）。</p>
 */
public class HTOrePopulator extends org.bukkit.generator.BlockPopulator {

    /** 单个生成区域。 */
    public record Area(int minH, int maxH, int most, int amount, int minSize, int maxSize,
                       Material replacement, World.Environment env) {}

    /** 单个矿物的生成定义。 */
    public record Info(SlimefunItemStack stack, List<Area> areas) {}

    private final List<Info> infos;

    public HTOrePopulator(List<Info> infos) {
        this.infos = List.copyOf(infos);
    }

    @Override
    public void populate(World world, Random random, Chunk source) {
        for (Info info : infos) {
            for (Area area : info.areas()) {
                if (area.env() != world.getEnvironment()) continue;
                for (int i = 0; i < area.amount(); i++) {
                    generateNext(source.getX(), source.getZ(), world, random, info, area);
                }
            }
        }
    }

    private void generateNext(int chunkX, int chunkZ, World world, Random random, Info info, Area area) {
        // 高度三角分布：most 处概率最高，向 min/max 两侧递减（RSC 原算法）
        int h = area.maxH() - area.minH() + 1;
        if (h < 0) h = 1;
        double s2 = random.nextDouble(0, h);
        double sTop = area.maxH() - area.most() + 1;
        int y;
        if (s2 < sTop) {
            y = area.maxH() - (int) (s2 * 2);
        } else {
            s2 -= sTop;
            y = area.minH() + (int) (s2 * 2);
        }

        int x = (chunkX << 4) + random.nextInt(16);
        int z = (chunkZ << 4) + random.nextInt(16);

        int baseX = chunkX << 4;
        int baseZ = chunkZ << 4;
        SlimefunItemStack stack = info.stack();
        int size = random.nextInt(area.minSize(), area.maxSize() + 1);
        for (int i = 0; i < size; i++) {
            // 随机游走出当前区块即停（避免跨区块写入竞态）
            if (x < baseX || x >= baseX + 16 || z < baseZ || z >= baseZ + 16) break;
            Block block = world.getBlockAt(x, y, z);
            if (block.getType() != area.replacement()) break;

            block.setType(stack.getType(), false);
            if (stack.getType() == Material.PLAYER_HEAD) {
                applySkin(block, stack);
            }
            Slimefun.getDatabaseManager().getBlockDataController()
                    .createBlock(new Location(world, x, y, z), stack.getItemId());

            int dir = random.nextInt(3);
            if (dir == 0) x++;
            else if (dir == 1) y++;
            else z++;
        }
    }

    private static void applySkin(Block block, SlimefunItemStack stack) {
        if (stack.getItemMeta() instanceof SkullMeta meta) {
            PlayerProfile profile = meta.getPlayerProfile();
            if (profile != null && profile.getTextures().getSkin() != null) {
                PlayerHead.setSkin(block, PlayerSkin.fromURL(profile.getTextures().getSkin().toString()), false);
            }
        }
    }
}
