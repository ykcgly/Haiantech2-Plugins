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

    /** 加载 saveditems/ 下的资源，如 "HM_DEBUG_FISH"。 */
    public static YamlConfiguration loadSavedItem(String name) {
        return load("saveditems/" + name + ".yml", "saveditem:" + name);
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
            HT.warn("读取 " + path + " 失败: " + e);
            return new YamlConfiguration();
        }
    }

    public static void clearCache() {
        CACHE.clear();
    }
}
