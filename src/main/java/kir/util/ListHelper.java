package kir.util;

import java.util.ArrayList;
import java.util.List;

public final class ListHelper {

    public static <T> List<List<T>> divide(List<T> src, int count) {
        if (src == null || count <= 0) {
            throw new IllegalArgumentException("Source list cannot be null and count must be greater than 0.");
        }

        var result = new ArrayList<List<T>>();
        int size = src.size();
        int numOfSublists = (int) Math.ceil((double) size / count);

        for (int i = 0; i < numOfSublists; i++) {
            int head = i * count;
            int tail = Math.min(head + count, size);
            List<T> sublist = new ArrayList<>(src.subList(head, tail));
            result.add(sublist);
        }

        return result;
    }

}
