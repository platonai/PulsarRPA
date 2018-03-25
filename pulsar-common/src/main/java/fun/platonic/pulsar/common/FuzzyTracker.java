package fun.platonic.pulsar.common;

import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

public class FuzzyTracker<T extends Comparable<T>> {

	private Map<T, Double> trackees = new TreeMap<T, Double>();

	public FuzzyTracker() {
	}

	public int size() {
		return trackees.size();
	}

	public boolean empty() {
		return trackees.size() == 0;
	}

	public double get(T t) {
		Double sim = trackees.get(t);

		if (sim == null)
			sim = 0.0;

		return sim;
	}

	public void set(T t, FuzzyProbability p) {
		set(t, p.floor());
	}

	public void set(T t, double sim) {
		Double oldValue = trackees.get(t);

		if (sim > 1.0) {
			sim = 1.0;
		}

		if (oldValue == null || oldValue < sim) {
			trackees.put(t, sim);
		}
	}

	public double inc(T t, double sim) {
		Double oldSim = trackees.get(t);

		if (oldSim != null) {
			sim += oldSim;
		}

		if (sim > 1) {
			sim = 1;
		}

		trackees.put(t, sim);

		return sim;
	}

	public double dec(T t, double sim) {
		Double oldSim = trackees.get(t);

		if (oldSim != null) {
			sim = oldSim - sim;
		}

		if (sim < FuzzyProbability.STRICTLY_NOT.ceiling()) {
			sim = 0.0;
			trackees.remove(t);
		} else {
			trackees.put(t, sim);
		}

		return sim;
	}

	public double remove(T t) {
		Double r = trackees.remove(t);
		return r == null ? 0.0 : r;
	}

	// 寻找相似度值最大的项
	public T primaryKey() {
		T lastType = null;
		Double lastSim = 0.0;

		for (Entry<T, Double> entry : trackees.entrySet()) {
			if (lastSim < entry.getValue()) {
				lastSim = entry.getValue();
				lastType = entry.getKey();
			}
		}

		return lastType;
	}

	public Set<T> keySet() {
		return trackees.keySet();
	}

	public Set<T> keySet(FuzzyProbability p) {
		Set<T> keys = new HashSet<T>();

		for (T key : trackees.keySet()) {
			if (is(key, p)) {
				keys.add(key);
			}
		}

		return keys;
	}

	public boolean is(T key, FuzzyProbability p) {
		Double sim = trackees.get(key);
		if (sim == null) {
			return false;
		}

		FuzzyProbability p2 = FuzzyProbability.of(sim);

		return p2.floor() >= p.floor();
	}

	public boolean maybe(T key) {
		Double p = trackees.get(key);
		if (p == null) {
			return false;
		}

		return FuzzyProbability.maybe(p);
	}

	public boolean veryLikely(T key) {
		Double p = trackees.get(key);
		if (p == null) {
			return false;
		}

		return FuzzyProbability.veryLikely(p);
	}

	public boolean mustBe(T key) {
		Double p = trackees.get(key);
		if (p == null) {
			return false;
		}

		return FuzzyProbability.mustBe(p);
	}

	public boolean certainly(T key) {
		Double p = trackees.get(key);
		if (p == null) {
			return false;
		}

		return FuzzyProbability.certainly(p);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();

		int i = 0;
		for (Entry<T, Double> entry : trackees.entrySet()) {
			if (i++ > 0) {
				sb.append(",");
			}

			sb.append(entry.getKey());
			sb.append(":");
			sb.append(String.format("%1.2f", entry.getValue()));
		}

		return sb.toString();
	}
}
