package com.haiman233.haimantech.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 颜色解析：支持 {@code &6} 传统色、以及 RSC/CMI 风格的 {@code {#ffff00}} 十六进制色。
 * 对应 RSC 的 {@code CMIChatColor.translate}。
 */
public final class Colors {

    private Colors() {}

    /** {@code {#RRGGBB}} 形式。 */
    private static final Pattern HEX = Pattern.compile("\\{#[0-9A-Fa-f]{6}}");

    /**
     * 先展开 {@code {#hex}}（转为 {@code §x§r§r§g§g§b§b}），再替换 {@code &}。
     * 顺序不可颠倒：否则 {@code &} 替换后会把{#}里的字符误变色。
     */
    public static String c(String s) {
        if (s == null || s.isEmpty()) return s;
        StringBuilder sb = new StringBuilder();
        int last = 0;
        Matcher m = HEX.matcher(s);
        while (m.find()) {
            sb.append(s, last, m.start());
            sb.append('§').append('x');
            for (char ch : m.group().substring(2, m.group().length() - 1).toCharArray()) {
                sb.append('§').append(ch);
            }
            last = m.end();
        }
        sb.append(s.substring(last));
        return sb.toString().replace('&', '§');
    }

    public static List<String> c(List<String> list) {
        List<String> out = new ArrayList<>(list.size());
        for (String s : list) out.add(c(s));
        return out;
    }
}
