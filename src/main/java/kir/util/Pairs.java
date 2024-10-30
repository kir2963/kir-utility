package kir.util;

import lombok.Getter;

import java.util.LinkedHashMap;

@Getter
public class Pairs {

    private final LinkedHashMap<String, Object> pairs = new LinkedHashMap<>();

    public void addPair(String key, Object value) {
        pairs.put(key, value);
    }

    public void removePair(String key) {
        pairs.remove(key);
    }

    public static Pairs of(String key, Object value) {
        var pairs = new Pairs();
        pairs.addPair(key, value);
        return pairs;
    }
    public static Pairs of(String key1, Object value1, String key2, Object value2) {
        var pairs = new Pairs();
        pairs.addPair(key1, value1);
        pairs.addPair(key2, value2);
        return pairs;
    }
    public static Pairs of(String key1, Object value1, String key2, Object value2, String key3, Object value3) {
        var pairs = of(key1, value1, key2, value2);
        pairs.addPair(key3, value3);
        return pairs;
    }
    public static Pairs of(String key1, Object value1, String key2, Object value2, String key3, Object value3, String key4, Object value4) {
        var pairs = of(key1, value1, key2, value2, key3, value3);
        pairs.addPair(key4, value4);
        return pairs;
    }
    public static Pairs of(String key1, Object value1, String key2, Object value2, String key3, Object value3, String key4, Object value4, String key5, Object value5) {
        var pairs = of(key1, value1, key2, value2, key3, value3, key4, value4);
        pairs.addPair(key5, value5);
        return pairs;
    }

}
