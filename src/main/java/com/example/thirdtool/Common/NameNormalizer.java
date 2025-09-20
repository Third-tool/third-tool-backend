package com.example.thirdtool.Common;

import java.text.Normalizer;
import java.text.Normalizer.Form;

public final class NameNormalizer {

    private NameNormalizer() {}

    public static String toDisplay(String s){
        if (s== null) {
            return "";
        }
        return Normalizer.normalize(s, Normalizer.Form.NFKC).strip();
    }

    public static String toKey(String s){
        return toDisplay(s.toLowerCase());
    }
}
