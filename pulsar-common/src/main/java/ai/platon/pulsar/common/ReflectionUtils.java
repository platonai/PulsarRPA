package ai.platon.pulsar.common;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by vincent on 17-3-2.
 *
 * @author vincent
 * @version $Id: $Id
 */
public class ReflectionUtils {

    /** Constant <code>LOG</code> */
    public static final Logger LOG = LoggerFactory.getLogger(ReflectionUtils.class);

    private static final Map<Class<?>, Constructor<?>> CONSTRUCTOR_CACHE = new ConcurrentHashMap<>();

    private static final Class<?>[] EMPTY_ARRAY = new Class[]{};

    /**
     * <p>newInstance.</p>
     *
     * @param theClass a {@link java.lang.Class} object.
     * @param <T> a T object.
     * @return a T object.
     */
    public static <T> T newInstance(Class<T> theClass) {
        T result;
        try {
            Constructor<T> constructor = (Constructor<T>) CONSTRUCTOR_CACHE.get(theClass);
            if (constructor == null) {
                constructor = theClass.getDeclaredConstructor(EMPTY_ARRAY);
                constructor.setAccessible(true);
                CONSTRUCTOR_CACHE.put(theClass, constructor);
            }
            result = constructor.newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return result;
    }

    /**
     * <p>forName.</p>
     *
     * @param className a {@link java.lang.String} object.
     * @param <T> a T object.
     * @return a T object.
     * @throws java.lang.ClassNotFoundException if any.
     * @throws java.lang.IllegalAccessException if any.
     * @throws java.lang.InstantiationException if any.
     */
    public static <T> T forName(@NotNull String className) throws ClassNotFoundException, IllegalAccessException, InstantiationException {
         return (T)Class.forName(className).newInstance();
    }

    /**
     * <p>forNameOrNull.</p>
     *
     * @param className a {@link java.lang.String} object.
     * @param <T> a T object.
     * @return a T object.
     */
    public static <T> T forNameOrNull(@NotNull String className) {
        try {
            return (T)Class.forName(className).newInstance();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        return null;
    }
}
