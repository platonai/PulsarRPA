package ai.platon.pulsar.common;

// < 0.5, 0.5 ~ 0.6, 0.6 ~ 0.8, 0.8 ~ 0.95, 0.95 ~ 1, 1
/**
 * <p>FuzzyProbability class.</p>
 *
 * @author vincent
 * @version $Id: $Id
 */
public enum FuzzyProbability {
    STRICTLY_NOT(0, 0.5), UNSURE(0.5, 0.6), MAYBE(0.6, 0.8), VERY_LIKELY(0.8,
            0.95), MUST_BE(0.95, 1), CERTAINLY(1, 1.01);

    private final double floor;
    private final double ceiling;

    FuzzyProbability(double floor, double ceiling) {
        this.floor = floor;
        this.ceiling = ceiling;
    }

    /**
     * <p>floor.</p>
     *
     * @return a double.
     */
    public double floor() {
        return floor;
    }

    /**
     * <p>ceiling.</p>
     *
     * @return a double.
     */
    public double ceiling() {
        return ceiling;
    }

    /**
     * <p>of.</p>
     *
     * @param sim a double.
     * @return a {@link ai.platon.pulsar.common.FuzzyProbability} object.
     */
    public static FuzzyProbability of(double sim) {
        if (sim < 0.5) {
            return STRICTLY_NOT;
        } else if (sim < 0.60) {
            return UNSURE;
        } else if (sim < 0.8) {
            return MAYBE;
        } else if (sim < 0.95) {
            return VERY_LIKELY;
        } else if (sim < 1) {
            return MUST_BE;
        }

        return CERTAINLY;
    }

    /**
     * <p>maybe.</p>
     *
     * @param p a {@link ai.platon.pulsar.common.FuzzyProbability} object.
     * @return a boolean.
     */
    public static boolean maybe(FuzzyProbability p) {
        return p.ordinal() >= MAYBE.ordinal();
    }

    /**
     * <p>veryLikely.</p>
     *
     * @param p a {@link ai.platon.pulsar.common.FuzzyProbability} object.
     * @return a boolean.
     */
    public static boolean veryLikely(FuzzyProbability p) {
        return p.ordinal() >= VERY_LIKELY.ordinal();
    }

    /**
     * <p>mustBe.</p>
     *
     * @param p a {@link ai.platon.pulsar.common.FuzzyProbability} object.
     * @return a boolean.
     */
    public static boolean mustBe(FuzzyProbability p) {
        return p.ordinal() >= MUST_BE.ordinal();
    }

    /**
     * <p>certainly.</p>
     *
     * @param p a {@link ai.platon.pulsar.common.FuzzyProbability} object.
     * @return a boolean.
     */
    public static boolean certainly(FuzzyProbability p) {
        return p.ordinal() >= CERTAINLY.ordinal();
    }

    /**
     * <p>strictlyNot.</p>
     *
     * @param sim a double.
     * @return a boolean.
     */
    public static boolean strictlyNot(double sim) {
        return sim < STRICTLY_NOT.ceiling;
    }

    /**
     * <p>maybe.</p>
     *
     * @param sim a double.
     * @return a boolean.
     */
    public static boolean maybe(double sim) {
        return sim >= MAYBE.floor;
    }

    /**
     * <p>veryLikely.</p>
     *
     * @param sim a double.
     * @return a boolean.
     */
    public static boolean veryLikely(double sim) {
        return sim >= VERY_LIKELY.floor;
    }

    /**
     * <p>mustBe.</p>
     *
     * @param sim a double.
     * @return a boolean.
     */
    public static boolean mustBe(double sim) {
        return sim >= MUST_BE.floor;
    }

    /**
     * <p>certainly.</p>
     *
     * @param sim a double.
     * @return a boolean.
     */
    public static boolean certainly(double sim) {
        return sim >= CERTAINLY.floor;
    }
}
