package com.haiman233.haimantech.load;

import com.haiman233.haimantech.HT;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * 从 jar 自身资源加载 YAML（content/ 已打包至 jar 根目录，saveditems/ 保留子目录）。
 *
 * <p>加载期按文件名缓存，避免同一份大文件（如 items.yml 1.8w 行）被多个 Loader 重复解析。
 * 全部 Loader 只读不写，共享同一实例安全；加载结束由 {@link Setup#loadAll()} 释放。
 */
public final class Yaml {

    private Yaml() {}

    private static final Map<String, YamlConfiguration> CACHE = new HashMap<>();

    /** 加载 jar 根资源，如 "items.yml"。 */
    public static YamlConfiguration loadResource(String name) {
        return load(name, name);
    }

    /** 净化版配置中的旧格式标记键（供 Read 判定走重建路径）。 */
    public static final String LEGACY_MARK = "legacy_saveditem";

    /**
     * 加载 saveditems/ 下的资源，如 "HM_DEBUG_FISH"。
     * 1.20.x 实体标签格式（meta-type: ENTITY_TAG）在 1.21 无法反序列化，且炸点位于
     * YamlConfiguration 加载期（Bukkit 解析 YAML 时即实例化 ItemStack，CraftMetaEntityTag
     * 解码旧实体 NBT 抛 NoSuchElementException），故先预读文本识别，命中后用原生
     * SnakeYAML 解析为纯 Map，绕开 Bukkit 的对象实例化。
     */
    public static YamlConfiguration loadSavedItem(String name) {
        YamlConfiguration cached = CACHE.get("saveditem:" + name);
        if (cached != null) return cached;
        YamlConfiguration cfg = loadSavedItem0(name);
        CACHE.put("saveditem:" + name, cfg);
        return cfg;
    }

    private static YamlConfiguration loadSavedItem0(String name) {
        String path = "saveditems/" + name + ".yml";
        String raw = readResourceText(path);
        if (raw != null && raw.contains("meta-type: ENTITY_TAG")) {
            return sanitizeLegacyEntitySavedItem(name, raw);
        }
        return doLoad(path, name);
    }

    /** 旧实体标签 saveditem 净化：仅提取 item.type 与 item.meta.display-name，供 Read 程序化重建。 */
    private static YamlConfiguration sanitizeLegacyEntitySavedItem(String name, String raw) {
        YamlConfiguration cfg = new YamlConfiguration();
        try {
            // 全限定名调用：与本项目 com.haiman233.haimantech.load.Yaml 同名，不能 import
            Object root = new org.yaml.snakeyaml.Yaml().load(raw);
            if (root instanceof Map<?, ?> map && map.get("item") instanceof Map<?, ?> item) {
                if (item.get("type") != null) cfg.set("item.type", String.valueOf(item.get("type")));
                if (item.get("meta") instanceof Map<?, ?> meta
                        && meta.get("display-name") instanceof String dn) {
                    cfg.set("item.meta.display-name", dn);
                }
            }
            cfg.set(LEGACY_MARK, true);
        } catch (Exception e) {
            HT.warn("saveditem " + name + " 旧格式净化解析失败: " + e);
        }
        return cfg;
    }

    /** 读取 jar 内资源为 UTF-8 文本；不存在或读取失败返回 null。 */
    private static String readResourceText(String path) {
        InputStream in = HT.plugin.getResource(path);
        if (in == null) return null;
        try (InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            StringBuilder sb = new StringBuilder();
            char[] buf = new char[4096];
            int n;
            while ((n = reader.read(buf)) > 0) sb.append(buf, 0, n);
            return sb.toString();
        } catch (IOException e) {
            HT.warn("读取 " + path + " 失败: " + e);
            return null;
        }
    }

    private static YamlConfiguration load(String path, String cacheKey) {
        YamlConfiguration cached = CACHE.get(cacheKey);
        if (cached != null) return cached;
        YamlConfiguration cfg = doLoad(path, cacheKey);
        CACHE.put(cacheKey, cfg);
        return cfg;
    }

    private static YamlConfiguration doLoad(String path, String cacheKey) {
        try (InputStream in = HT.plugin.getResource(path)) {
            if (in == null) {
                HT.missing("资源缺失:" + path);
                return new YamlConfiguration();
            }
            try (InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                return YamlConfiguration.loadConfiguration(reader);
            }
        } catch (IOException e) {
            HT.warn("读取 " + path + " 失败: " + e);
            return new YamlConfiguration();
        }
    }

    public static void clearCache() {
        CACHE.clear();
    }
}
