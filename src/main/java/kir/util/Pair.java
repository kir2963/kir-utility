package kir.util;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public final class Pair<V1, V2> {
    private final V1 value1;
    private final V2 value2;

    public static <V1, V2> Pair<V1, V2> of(V1 value1, V2 value2) {
        return new Pair<>(value1, value2);
    }
}
