package fun.platonic.pulsar.common;

import org.apache.hadoop.conf.Configurable;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.serializer.SerializationFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by vincent on 17-3-2.
 */
public class ReflectionUtils {

    public static final Logger LOG = LoggerFactory.getLogger(ReflectionUtils.class);

    private static final Map<Class<?>, Constructor<?>> CONSTRUCTOR_CACHE = new ConcurrentHashMap<>();

    private static final Class<?>[] EMPTY_ARRAY = new Class[]{};

    volatile private static SerializationFactory serialFactory = null;

    public static void setConf(Object theObject, Configuration conf) {
        if (conf != null) {
            if (theObject instanceof Configurable) {
                ((Configurable) theObject).setConf(conf);
            }
            setJobConf(theObject, conf);
        }
    }

    /**
     * This code is to support backward compatibility and break the compile
     * time dependency of core on mapred.
     * This should be made deprecated along with the mapred package HADOOP-1230.
     * Should be removed when mapred package is removed.
     */
    private static void setJobConf(Object theObject, Configuration conf) {
        //If JobConf and JobConfigurable are in classpath, AND
        //theObject is of type JobConfigurable AND
        //conf is of type JobConf then
        //invoke configure on theObject
        try {
            Class<?> jobConfClass = conf.getClassByNameOrNull("org.apache.hadoop.mapred.JobConf");
            if (jobConfClass == null) {
                return;
            }

            Class<?> jobConfigurableClass = conf.getClassByNameOrNull("org.apache.hadoop.mapred.JobConfigurable");
            if (jobConfigurableClass == null) {
                return;
            }

            if (jobConfClass.isAssignableFrom(conf.getClass()) &&
                    jobConfigurableClass.isAssignableFrom(theObject.getClass())) {
                Method configureMethod = jobConfigurableClass.getMethod("configure", jobConfClass);
                configureMethod.invoke(theObject, conf);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error in configuring object", e);
        }
    }

    public static <T> T newInstance(Class<T> theClass, Configuration conf) {
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
        setConf(result, conf);
        return result;
    }
}
