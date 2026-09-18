package com.agentbook.ch11;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class LocalHashEmbedding {
    public static final int DIM = 64;
    private static final Pattern WORD = Pattern.compile("[a-z0-9]+");

    private LocalHashEmbedding() {
    }

    public static double[] embed(String text) {
        double[] vec = new double[DIM];
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            List<String> tokens = tokens(text);
            for (String tok : tokens) {
                byte[] digest = md.digest(tok.getBytes(StandardCharsets.UTF_8));
                int idx = ((digest[0] & 0xff) | ((digest[1] & 0xff) << 8)) % DIM;
                double sign = ((digest[2] & 0xff) % 2 == 0) ? 1.0 : -1.0;
                vec[idx] += sign;
            }
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
        double norm = 0;
        for (double x : vec) {
            norm += x * x;
        }
        norm = Math.sqrt(norm == 0 ? 1 : norm);
        for (int i = 0; i < vec.length; i++) {
            vec[i] /= norm;
        }
        return vec;
    }

    public static double dot(double[] a, double[] b) {
        double s = 0;
        for (int i = 0; i < a.length; i++) {
            s += a[i] * b[i];
        }
        return s;
    }

    static List<String> tokens(String text) {
        List<String> out = new ArrayList<>();
        Matcher m = WORD.matcher(text.toLowerCase(Locale.ROOT));
        while (m.find()) {
            out.add(m.group());
        }
        List<String> chars = new ArrayList<>();
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch >= 0x4e00 && ch <= 0x9fff) {
                chars.add(String.valueOf(ch));
            }
        }
        out.addAll(chars);
        for (int i = 0; i < chars.size() - 1; i++) {
            out.add(chars.get(i) + chars.get(i + 1));
        }
        return out;
    }
}
