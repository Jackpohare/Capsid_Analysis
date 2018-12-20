import java.awt.BasicStroke;
import java.awt.Font;
import java.util.Arrays;

import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.DefaultXYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.ui.Layer;
import org.jfree.ui.TextAnchor;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.ImageCanvas;
import ij.gui.ImageWindow;
import ij.measure.CurveFitter;

final public class utils {


	static CurveFitter GetFit(double[] x, double[] y, String key, DefaultXYDataset xyData, int debug) {

		CurveFitter cf = new CurveFitter(x, y);

		cf.doFit(CurveFitter.GAUSSIAN);

		double[] params = cf.getParams();
		for (int i = 0; i < params.length; i++) {
			if (debug > 0) {
				IJ.log("param " + i + ": " + params[i]);
			}
		}

		// Max X
		double xMax = x[x.length - 1];
		double[] fitX = new double[x.length * 2];
		double[] fitY = new double[x.length * 2];
		int j = 0;
		fitX[j] = 0;
		fitY[j] = CurveFitter.f(CurveFitter.GAUSSIAN, params, fitX[j]);
		j++;
		for (int i = 0; i < x.length; i++) {
			fitX[j] = x[i];
			fitY[j] = CurveFitter.f(CurveFitter.GAUSSIAN, params, fitX[j]);
			j++;
			if (i < x.length - 1) {
				fitX[j] = (x[i] + x[i + 1]) / 2.0;
				fitY[j] = CurveFitter.f(CurveFitter.GAUSSIAN, params, fitX[j]);
				j++;
			}
		}

		double[][] xy = new double[2][];
		xy[0] = fitX;
		xy[1] = fitY;

		xyData.addSeries(key, xy);

		return cf;
	}
	
	/**
	 * Return true is given string is parseable to double
	 * @param str
	 * @return true iff given string is parseable to double
	 */
	  static boolean isDouble(String str) {
	        try {
	            Double.parseDouble(str);
	            return true;
	        } catch (NumberFormatException e) {
	            return false;
	        }
	    }

	/**
	 * @param bucketWidth Width of each bucket in the distribution
	 * @param bucket      THe centre values of each bucket
	 * @param values      THe values to be counted into distribution
	 * @param counts      The counts of each value centred on the matching bucket
	 */
	static public void GetFrequencyDistirubtion(double[] bucket, double[] values, double[] counts, double bucketWidth) {

		GetFrequencyDistirubtion(bucket, values, counts, bucketWidth, 0);

	}

	/**
	 * @param bucketWidth Width of each bucket in the distribution
	 * @param bucket      THe centre values of each bucket
	 * @param values      THe values to be counted into distribution
	 * @param counts      The counts of each value centred on the matching bucket
	 */
	static public void GetFrequencyDistirubtion(double[] bucket, double[] values, double[] counts, double bucketWidth,
			int debug) {
		double[] x = Arrays.copyOf(values, values.length);
		Arrays.sort(x);
		;
		int nBinIndex = 0;
		int nBucketCount = bucket.length;

		double binTop = bucket[1] - bucketWidth / 2;

		for (int i = 0; i < counts.length; i++) {
			counts[i] = 0;
		}

		for (int i = 0; i < x.length; i++) {
			if (x[i] < binTop) {
				counts[nBinIndex]++;
			} else {
				nBinIndex++;

				--i;
				if (nBinIndex >= nBucketCount) {
					IJ.log("Value " + (i + 1) + "  outside buckets =" + x[i + 1]);
					break;
				} else {
					binTop = bucket[nBinIndex] + bucketWidth / 2;
				}
			}
		}
		if (debug >= 3) {
			IJ.log("\nGetFrequencyDistirubtion\nBucket width:" + bucketWidth + "\nBucket count: " + bucket.length);
			for (int i = 0; i < bucket.length; i++) {
				IJ.log(i + "," + bucket[i] + "," + counts[i]);
			}

		}
	}

	static public Integer[] rankPositions(double[] values) {

	    int n = values.length;
	    Integer [] indexes = new Integer[n];
	    double[] data = new double[n];
	    for (int i = 0; i < n; i++) {
	        indexes[i] = i;
	        data[i] = values[i];
	    }
	    ArrayIndexComparator comparator = new ArrayIndexComparator(data);
	    indexes = comparator.createIndexArray();
	    Arrays.sort(indexes, comparator);

	    return indexes;
	}

	static public int getIndexOf(double[] sortedArray, double value) {

		for (int i = 0; i < sortedArray.length; i++) {
			if (sortedArray[i] > value) {
				return i;
			}
		}
		return sortedArray.length;
	}

	static double iqr(double[] values) {
		return iqr(values, 0);
	}

	static double iqr(double[] values, int debug) {
		if (debug > 0) {
			IJ.log("IQR on array length " + values.length);
		}
		double[] x = Arrays.copyOf(values, values.length);

		Arrays.sort(x);
		;

		double q3 = quantile(x, 0.75);
		double q1 = quantile(x, 0.25);
		if (debug > 0) {
			IJ.log("IQR Q3 = " + q3);
			IJ.log("IQR Q1 = " + q1);
		}
		return q3 - q1;
	}

	static public void LogWindowDetails(AnalysisSettings settings) { 
		ImagePlus ip =settings.image;
		ImageCanvas ic = settings.canvas;
		ImageWindow iw = settings.win;
		
		String s = String.format("Window: %s (%d stack)\n"
				+"Image width,height:  %d, %d\n"
				+"Window width,height: %d, %d\n"
				+"Canvas width,height: %d, %d;  Mag: %f\n"
				+"Canvas size:         %d, %d\n",
				iw.getTitle(), ip.getStackSize(),
				ip.getWidth(),ip.getHeight(),
				iw.getWidth(),iw.getHeight(),
				ic.getWidth(),ic.getHeight(),ic.getMagnification(),
				ic.getSize().width,ic.getSize().height
				);
		IJ.log(s);
	}
	
	
	static public double Max(double[] array) {
		double max = 0;
		for (int i = 0; i < array.length; i++) {
			if (array[i] > max) {
				max = array[i];
			}
		}
		return max;
	}

	static int Max(int[] pixels, String channel) {
		int max = 0;
		int n = pixels.length;
		for (int i = 0; i < n; i++) {
			int p = (pixels[i] & 0xff00) >> 8; // presume green
			if (channel == "red") {
				p = (pixels[i] & 0xff0000) >> 16;
			}
			if (p > max) {
				max = p;
			}

		}
		return max;

	}

	/**
	 * Get mean of given array
	 * 
	 * @param values = array of values to return mean of
	 * @return mean of given array
	 */
	static double Mean(double[] values) {
		double mean = 0;
		int n = values.length;
		if (n == 0) {
			return 0.0;
		}

		for (int i = 0; i < n; i++) {
			mean += values[i];
		}
		return mean / (double) n;
	}

	static double Mean(int[] pixels, String channel) {
		double mean = 0;
		int n = pixels.length;
		for (int i = 0; i < n; i++) {
			int p = (pixels[i] & 0xff00) >> 8; // presume green
			if (channel == "red") {
				p = (pixels[i] & 0xff0000) >> 16;
			}
			mean += p;
		}
		return mean / n;
	}

	static double quantile(double[] arr, double p) {
		return quantile(arr, p, 0);
	}

	static double quantile(double[] arr, double p, int debug) {
		// Courtesy of https://github.com/compute-io/quantile
		int len = arr.length, id;

		// Cases...

		// [0] 0th percentile is the minimum value...
		if (p == 0.0) {
			return arr[0];
		}
		// [1] 100th percentile is the maximum value...
		if (p == 1.0) {
			return arr[len - 1];
		}
		// Calculate the vector index marking the quantile:
		id = (int) ((len * p) - 1);

		// [2] Is the index an integer?
		if (id == Math.floor(id)) {
			// Value is the average between the value at id and id+1:
			return (arr[id] + arr[id + 1]) / 2.0;
		}
		// [3] Round up to the next index:
		id = (int) Math.ceil(id);
		if (debug > 0) {
			IJ.log("Quantile " + p + " = " + arr[id] + " at index " + id);
		}
		return arr[id];
	} // end FUNCTION quantile()

	/**
	 * Return standard deviation of given value array (given mean)
	 * 
	 * @param values = array of values to find stdDev from given mean
	 * @param mean   = mean of given array
	 * @return stdDev of given values and given mean
	 */
	static double StdDev(double[] values, double mean) {
		double SUM = 0;
		int n = values.length;
		for (int i = 0; i < n; i++) {
			SUM = SUM + (values[i] - mean) * (values[i] - mean);
		}
		return Math.sqrt(SUM / (n - 1));

	}

	static double StdDev(int[] pixels, String channel) {
		double SUM = 0;
		int n = pixels.length;
		double mean = Mean(pixels, channel);
		for (int i = 0; i < n; i++) {
			int p = (pixels[i] & 0xff00) >> 8; // presume green
			if (channel == "red") {
				p = (pixels[i] & 0xff0000) >> 16;
			}

			SUM = SUM + (p - mean) * (p - mean);
		}
		return Math.sqrt(SUM / (n - 1));
	}

	static public XYSeries XYSeriesFromArrays(String key, double[] x, double y[]) {
		XYSeries series = new XYSeries(key);
		for (int i = 0; i < x.length; i++) {
			series.add(x[i], y[i]);
		}
		return series;
	}

}
