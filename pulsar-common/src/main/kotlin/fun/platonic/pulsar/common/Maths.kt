package `fun`.platonic.pulsar.common

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics

/**
 * Find out outlier fences
 *
 * The inter-quartile range (IQR), also called the mid-spread or middle 50%
 *
 * The values for Q1 – 1.5×IQR and Q3 + 1.5×IQR are the "fences" that mark off the "reasonable" values
 * from the outlier values. Outliers lie outside the fences.
 *
 * If we also consider "extreme values",
 * then the values for Q1 – 1.5×IQR and Q3 + 1.5×IQR are the "inner" fences and the values
 * for Q1 – 3×IQR and Q3 + 3×IQR are the "outer" fences.
 *
 * @see https://en.wikipedia.org/wiki/Interquartile_range
 * @see https://en.wikipedia.org/wiki/Box_plot
 * @see https://www.purplemath.com/modules/boxwhisk3.htm
 *
 * @param values The values
 * @param p1 The q1 percentile
 * @param p2 The q3 percentile
 * @param smooth The smooth
 * @return The outlier fence
 * */
fun getOutlierFence(values: DoubleArray, p1: Double = 25.0, p2: Double = 75.0, smooth: Double = 1.0): Pair<Double, Double> {
    val ds = DescriptiveStatistics(values)
    return getOutlierFence(ds, p1, p2, smooth)
}

fun getOutlierFence(ds: DescriptiveStatistics, p1: Double = 25.0, p2: Double = 75.0, smooth: Double = 1.0): Pair<Double, Double> {
    val q1 = ds.getPercentile(p1)
    val q3 = ds.getPercentile(p2)
    val iqr = q3 - q1
    // upper 1.5*IQR whisker
    val f1 = q3 + 1.5 * iqr + smooth
    val f2 = q3 + 3 * iqr + smooth
    return f1 to f2
}
