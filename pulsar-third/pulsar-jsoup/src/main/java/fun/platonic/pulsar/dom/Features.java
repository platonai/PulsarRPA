package fun.platonic.pulsar.dom;

import fun.platonic.pulsar.common.OrderedIntDoubleMapping;
import org.jsoup.helper.Validate;

import java.util.*;

/**
 * The features of an Element.
 * @author Vincent Zhang, galaxyeye@live.cn
 */
public class Features {
    private OrderedIntDoubleMapping mapping = new OrderedIntDoubleMapping();

    public int[] getIndices() {
        return mapping.getIndices();
    }

    public int indexAt(int offset) {
        return mapping.indexAt(offset);
    }

    public double[] getValues() {
        return mapping.getValues();
    }

    public int getNumMappings() {
        return mapping.getNumMappings();
    }

    public double get(int index) {
        return mapping.get(index);
    }

    public void set(int index, double value) {
        mapping.set(index, value);
    }

    public void clear() {
        mapping = new OrderedIntDoubleMapping();
    }

    @Override
    public int hashCode() {
        return mapping.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Features && mapping.equals(((Features)o).mapping);
    }

    @Override
    public String toString() {
        return mapping.toString();
    }
}
