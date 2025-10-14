package run.halo.app.extension.index;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import org.springframework.lang.Nullable;
import run.halo.app.extension.Extension;

@UtilityClass
public class IndexAttributeFactory {

    public static <E extends Extension> IndexAttribute<E, UnknownKey> simpleAttribute(Class<E> type,
        Function<E, String> valueFunc) {
        return attribute(type, UnknownKey.class, (E e) -> Optional.ofNullable(valueFunc.apply(e))
            .map(UnknownKey::new)
            .orElse(null));
    }

    public static <E extends Extension> IndexAttribute<E, UnknownKey> multiValueAttribute(
        Class<E> type,
        Function<E, Set<String>> valuesFunc) {
        return attributes(type, UnknownKey.class, (E e) -> Optional.ofNullable(valuesFunc.apply(e))
            .map(values -> values.stream()
                .map(UnknownKey::new)
                .collect(Collectors.toSet())
            )
            .orElse(null));
    }

    public static <E extends Extension, K extends Comparable<K>> IndexAttribute<E, K> attributes(
        Class<E> objectType, Class<K> keyType, Function<E, Set<K>> valuesFunc
    ) {
        return new DefaultIndexAttribute<>(valuesFunc, objectType, keyType);
    }

    public static <E extends Extension, K extends Comparable<K>> IndexAttribute<E, K> attribute(
        Class<E> objectType, Class<K> keyType, Function<E, K> valueFunc
    ) {
        return new DefaultIndexAttribute<>(e -> Optional.ofNullable(valueFunc.apply(e))
            .map(Set::of)
            .orElse(null), objectType, keyType);
    }

    /**
     * String key wrapper for nullable string comparison. Only for backward compatibility.
     *
     * @author johnniang
     * @since 2.22.0
     *
     */
    record UnknownKey(@Nullable String value) implements Comparable<UnknownKey> {

        @Override
        public int compareTo(@NotNull UnknownKey o) {
            return KeyComparator.INSTANCE.compare(this.value, o.value);
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            UnknownKey unknownKey = (UnknownKey) o;
            return Objects.equals(value, unknownKey.value);
        }

    }
}
