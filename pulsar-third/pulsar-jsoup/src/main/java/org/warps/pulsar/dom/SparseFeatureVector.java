package org.warps.pulsar.dom;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.helper.Validate;

import java.util.*;

// TODO: use a machine learning library
public class SparseFeatureVector implements Iterable<Feature>, Cloneable {

  private LinkedHashMap<String, Feature> features = null;

  // linked hash map to preserve insertion order.
  // null be default as so many elements have no features -- saves a good
  // chunk of memory

  /**
   * Get an feature value by key.
   * 
   * @param key
   *          the feature key
   * @return the feature value if set; or empty string if not set.
   * @see #hasKey(String)
   */
  public double get(String key) {
    Validate.notEmpty(key);

    if (features == null) {
      return 0.0;
    }

    Feature feature = features.get(key.toLowerCase());
    return feature != null ? feature.getValue() : 0.0;
  }

  public Feature getFeature(String key) {
    Validate.notEmpty(key);

    if (features == null) {
      return null;
    }

    return features.get(key.toLowerCase());
  }

  /**
   * Set a new feature, or replace an existing one by key.
   * 
   * @param key
   *          feature key
   * @param value
   *          feature value
   */
  public void put(String key, double value) {
    if (value != 0.0) {
      Feature feature = new Feature(key, value);
      put(feature);
    }
  }

  /**
   * Set a new feature, or replace an existing one by key.
   *
   * @param feature feature
   */
  public void put(Feature feature) {
    if (feature.getValue() != 0.0) {
      Validate.notNull(feature);
      if (features == null) {
        features = new LinkedHashMap<>();
      }

      features.put(feature.getKey(), feature);
    }
  }

  /**
   * Remove an feature by key.
   * 
   * @param key
   *          feature key to remove
   */
  public void remove(String key) {
    Validate.notEmpty(key);
    if (features == null)
      return;
    features.remove(key.toLowerCase());
  }

  public void clear() {
    if (features != null) features.clear();
  }

  /**
   * Tests if these features contain an feature with this key.
   * 
   * @param key
   *          key to check for
   * @return true if key exists, false otherwise
   */
  public boolean hasKey(String key) {
    return features != null && features.containsKey(key.toLowerCase());
  }

  /**
   * Get the number of features in this set.
   * 
   * @return size
   */
  public int size() {
    if (features == null)
      return 0;
    return features.size();
  }

  /**
   * Add all the features from the incoming set to this set.
   * 
   * @param incoming
   *          features to put to these features.
   */
  public void addAll(SparseFeatureVector incoming) {
    if (incoming.size() == 0)
      return;
    if (features == null)
      features = new LinkedHashMap<String, Feature>(incoming.size());
    features.putAll(incoming.features);
  }

  public Iterator<Feature> iterator() {
    return asList().iterator();
  }

  public SparseFeatureVector subset(String... names) {
    SparseFeatureVector results = new SparseFeatureVector();

    for (Feature feature : features.values()) {
      for (String name : names) {
        if (feature.getKey().equals(name)) {
          results.put(feature);
        }
      }
    }

    return results;
  }

  /**
   * Get the features as a List, for iteration. Do not modify the keys of the
   * features via this view, as changes to keys will not be recognised in the
   * containing set.
   * 
   * @return an view of the features as a List.
   */
  public List<Feature> asList() {
    if (features == null)
      return Collections.emptyList();

    List<Feature> list = new ArrayList<Feature>(features.size());
    for (Map.Entry<String, Feature> entry : features.entrySet()) {
      list.add(entry.getValue());
    }
    return Collections.unmodifiableList(list);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof SparseFeatureVector))
      return false;

    SparseFeatureVector that = (SparseFeatureVector) o;

    return features != null ? features.equals(that.features) : that.features == null;

  }

  @Override
  public int hashCode() {
    return features != null ? features.hashCode() : 0;
  }

  @Override
  public SparseFeatureVector clone() {
    if (features == null)
      return new SparseFeatureVector();

    SparseFeatureVector clone;
    try {
      clone = (SparseFeatureVector) super.clone();
    } catch (CloneNotSupportedException e) {
      throw new RuntimeException(e);
    }
    clone.features = new LinkedHashMap<String, Feature>(features.size());
    for (Feature feature : this)
      clone.features.put(feature.getKey(), feature.clone());
    return clone;
  }

  @Override
  public String toString() {
    if (features == null) return "";

    return StringUtils.join(features.values(), ", ");
  }
}
