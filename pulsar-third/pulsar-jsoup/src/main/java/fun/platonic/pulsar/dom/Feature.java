package fun.platonic.pulsar.dom;

import org.jsoup.helper.Validate;

import java.util.Map;

public class Feature implements Map.Entry<String, Double>, Cloneable, Comparable<Feature> {

    private String key;
    private double value;

    /**
     * Create a new feature from unencoded (raw) name and value.
     *
     * @param name  feature name
     * @param value feature value
     */
    public Feature(String name, double value) {
        Validate.notEmpty(name);
        this.key = name.trim().toLowerCase();
        this.value = value;
    }

    /**
     * Get the feature name.
     *
     * @return the feature name
     */
    public String getKey() {
        return key;
    }

    /**
     * Set the feature name. Gets normalised as per the constructor method.
     *
     * @param name the new name; must not be null
     */
    public void setKey(String name) {
        Validate.notEmpty(name);
        this.key = name.trim().toLowerCase();
    }

    /**
     * Get the feature value.
     *
     * @return the feature value
     */
    public Double getValue() {
        return value;
    }

    /**
     * Set the feature value.
     *
     * @param value the new feature value
     */
    public Double setValue(Double value) {
        if (value == null) {
            value = 0.0;
        }

        double old = this.value;
        this.value = value;
        return old;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof Feature))
            return false;

        Feature feature = (Feature) o;

        if (key != null ? !key.equals(feature.key) : feature.key != null)
            return false;
        return value == feature.value;

    }

    @Override
    public int hashCode() {
        int result = key != null ? key.hashCode() : 0;
        result = 31 * result + new Double(value).hashCode();
        return result;
    }

    @Override
    public Feature clone() {
        try {
            return (Feature) super.clone(); // only fields are immutable strings name
            // and value, so no more deep copy
            // required
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Get the string representation of this feature
     *
     * @return string
     */
    @Override
    public String toString() {
        return key + ":" + value;
    }

    @Override
    public int compareTo(Feature other) {
        return key.compareTo(other.key);
    }
}
