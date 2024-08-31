package run.halo.app.extension.index;

import java.util.Set;
import java.util.function.Function;
import lombok.experimental.UtilityClass;
import run.halo.app.extension.Extension;

@UtilityClass
public class IndexAttributeFactory {

    public static <E extends Extension, T extends Comparable<? super T>> IndexAttribute<T>
        simpleAttribute(
        Class<E> type,
        Function<E, T> valueFunc) {
        return new FunctionalIndexAttribute<>(type, valueFunc);
    }

    public static <E extends Extension, T extends Comparable<? super T>> IndexAttribute<T>
        multiValueAttribute(
        Class<E> type,
        Function<E, Set<T>> valueFunc) {
        return new FunctionalMultiValueIndexAttribute<>(type, valueFunc);
    }
}
