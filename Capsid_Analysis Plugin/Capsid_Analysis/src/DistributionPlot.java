import java.awt.BasicStroke;
import java.awt.Color;
import java.util.Arrays;

import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.XYToolTipGenerator;
import org.jfree.chart.plot.DatasetRenderingOrder;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYBarPainter;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.DefaultXYDataset;
import org.jfree.data.xy.XYBarDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.Layer;

import ij.IJ;
import ij.measure.CurveFitter;

public class DistributionPlot {
	private AnalysisSettings settings;

	private Color _color = null;
	private double[] _sortedValues = null;
	private double _threshold;
	private XYPlot _plot = null;
	private String _key = "";
	private double _binWidth;
	private boolean _bAddFit = false;

	public DistributionPlot(AnalysisSettings s, String key, Color color, double[] values, double threshold,
			boolean bAddFit) {
		this.settings = s;
		this._color = color;
		this._sortedValues = values.clone();
		Arrays.sort(this._sortedValues);
		this._key = key;
		this._bAddFit = bAddFit;
		this.setThreshold(threshold);
	}

	public void drawPlot(double[] BinBottoms, double[] Counts, double[] CountsBelowThreshold, double[] values) {
		double[] Bins = new double[BinBottoms.length];
		for (int i = 0; i < Bins.length - 1; i++) {
			Bins[i] = (BinBottoms[i] + BinBottoms[i + 1]) / 2;
		}
		Bins[Bins.length - 1] = BinBottoms[Bins.length - 1]
				+ (BinBottoms[Bins.length - 1] - BinBottoms[Bins.length - 2]) / 2;

		if (settings.debug > 0) {
			IJ.log("\n" + this.getClass().getSimpleName() + " drawPlot\nBins length: " + Bins.length
					+ "\nCOunts length:" + Counts.length);
		}
		XYToolTipGenerator xyToolTipGenerator = null;
		xyToolTipGenerator = new XYToolTipGenerator() {
			public String generateToolTip(XYDataset dataset, int series, int item) {
				Number x1 = dataset.getX(series, item);
				Number y1 = dataset.getY(series, item);
				StringBuilder stringBuilder = new StringBuilder();
				stringBuilder
						.append(String.format("<html><p style='color:#0000ff;'>%s</p>", dataset.getSeriesKey(series)));
				stringBuilder.append(String.format("Mean Range:%.1f - %.1f<br/>", x1.intValue() - _binWidth / 2,
						x1.intValue() + _binWidth / 2));
				stringBuilder.append(String.format("ROI COunt: '%d'", y1.intValue()));
				stringBuilder.append("</html>");
				return stringBuilder.toString();
			}
		};

		// If we have not yet built the plot, then build it
		if (this._plot == null) {
			this._plot = new XYPlot();
			// Tooltip ?
			xyToolTipGenerator = new XYToolTipGenerator() {
				public String generateToolTip(XYDataset dataset, int series, int item) {
					Number x1 = dataset.getX(series, item);
					Number y1 = dataset.getY(series, item);
					StringBuilder stringBuilder = new StringBuilder();
					stringBuilder.append(
							String.format("<html><p style='color:#0000ff;'>%s</p>", dataset.getSeriesKey(series)));
					stringBuilder.append(String.format("Range:%.1f - %.1f<br/>", x1.intValue() - _binWidth / 2,
							x1.intValue() + _binWidth / 2));
					stringBuilder.append(String.format("Count: %d", y1.intValue()));
					stringBuilder.append("</html>");
					return stringBuilder.toString();
				}
			};

		}

		// Prepare the dataset from the given Bins and Counts array
		// final DefaultXYDataset xyData = new DefaultXYDataset();
		XYSeries primary = utils.XYSeriesFromArrays(this._key, Bins, Counts);
		XYSeries secondary = utils.XYSeriesFromArrays(this._key, Bins, CountsBelowThreshold);
		double[][] xy = new double[2][];
		xy[0] = Bins;
		xy[1] = Counts;

		XYSeriesCollection xyData = new XYSeriesCollection();
		XYSeriesCollection xy2Data = new XYSeriesCollection();

		xyData.addSeries(primary);

		// final DefaultXYDataset xy2Data = new DefaultXYDataset();
		double[][] xy2 = new double[2][];
		xy2[0] = Bins;
		xy2[1] = CountsBelowThreshold;
		xy2Data.addSeries(secondary);

		XYBarDataset dataset = new XYBarDataset(xyData, this._binWidth * 0.9);
		XYBarDataset dataset2 = new XYBarDataset(xy2Data, this._binWidth * 0.9);

		XYBarRenderer renderer = new XYBarRenderer();
		renderer.setSeriesPaint(0, this._color);
		// renderer.setSeriesPaint(1, Color.LIGHT_GRAY);
		renderer.setDrawBarOutline(false);
		renderer.setShadowVisible(false);
		renderer.setBarPainter(new StandardXYBarPainter());
		renderer.setBaseToolTipGenerator(xyToolTipGenerator);
		renderer.setMargin(0);
		// Create the plot
		// XYPlot plot = new XYPlot(dataset, new NumberAxis("Mean Intensity"), new
		// NumberAxis("ROI Count"),renderer);

		// construct the plot

		this._plot.setDataset(0, dataset);
		// xy2Data.
		this._plot.setDataset(1, dataset2);
		this._plot.setRenderer(0, renderer);

		if (settings.debug > 0) {
			renderer = (XYBarRenderer) this._plot.getRenderer(0);
			IJ.log("\nRenderer 1\nMargin: " + renderer.getMargin());
		}

		XYBarRenderer renderer2 = new XYBarRenderer();
		// renderer2.setSeriesPaint(0, color);
		renderer2.setSeriesPaint(0, Color.LIGHT_GRAY);
		renderer2.setDrawBarOutline(false);
		renderer2.setShadowVisible(false);
		renderer2.setBarPainter(new StandardXYBarPainter());
		renderer2.setBaseToolTipGenerator(xyToolTipGenerator);

		this._plot.setRenderer(1, renderer2);

		this._plot.setRangeAxis(0, new NumberAxis("Cout of ROI Above Threshold"));
		this._plot.setRangeAxis(1, new NumberAxis("Count of ROI Below Threshold"));
		this._plot.setDomainAxis(new NumberAxis("Mean Intensity"));

		// Map the data to the appropriate axis
		this._plot.mapDatasetToRangeAxis(0, 0);
		this._plot.mapDatasetToRangeAxis(1, 1);

		ValueMarker Marker = new ValueMarker(this._threshold); // position is the value on the axis
		float[] dash = { 3 };
		Marker.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1.0f, dash, 0.0f));
		Marker.setPaint(this._color);
		this._plot.addDomainMarker(Marker, Layer.FOREGROUND);

		// Make sure range axis goes from zero
		this._plot.getRangeAxis(0).setLowerBound(0);
		// Now adjust count of ROI below axis - if it is smaller than count of ROI
		// above, we set it the same
		double upper = this._plot.getRangeAxis(0).getUpperBound();
		if (upper > this._plot.getRangeAxis(1).getUpperBound()) {
			this._plot.getRangeAxis(1).setUpperBound(upper);
		}

		// Make sure x axis starts at zero
		this._plot.getDomainAxis().setLowerBound(0);

		if (this._bAddFit) {
			DefaultXYDataset xyFitDataSet = new DefaultXYDataset();
			;
			CurveFitter cf = utils.GetFit(Bins, Counts, this._key + " (fit)", xyFitDataSet, settings.debug);
			this._plot.setDataset(2, xyFitDataSet);
			// and get rendered to draw it as a line with no shapes
			final XYLineAndShapeRenderer fitRenderer = new XYLineAndShapeRenderer(true, false);
			fitRenderer.setSeriesPaint(0,
					new Color(this._color.getRed(), this._color.getGreen(), this._color.getBlue(), 255));
			this._plot.setRenderer(2, fitRenderer);

			double[] params = cf.getParams();
			this._plot.clearAnnotations();
			utils.AddMarkers(this._plot, utils.Mean(values), utils.StdDev(values, utils.Mean(values)), params[2],
					params[3], cf.getFitGoodness());

			this._plot.setDatasetRenderingOrder(DatasetRenderingOrder.FORWARD);
		}

	}

	// return set of bins from 0 to 255 based on current threshold
	public double[] GetBins() {

		this._binWidth = getBinWidth();
		// Number of bins
		int nBinCount = (int) (255.0 / this._binWidth + 1);
		// We have to make sure the threshold is the start of a bin, so work out offset
		// Ofset is reaminder of threshold over bin width
		double offset = this._binWidth - (this._threshold % this._binWidth);

		double[] Bins = new double[nBinCount];
		if (settings.debug > 0) {
			IJ.log(this.getClass().getSimpleName() + " GetBins bins length :" + Bins.length + "\nBin width = "
					+ this._binWidth + ", Offset = " + offset);
		}
		Bins[0] = 0;
		for (int i = 1; i < Bins.length; i++) {
			Bins[i] = this._binWidth * i - offset;
		}
		return Bins;

	}

	public double getBinWidth() {
		// Get the values that are above current threshold
		int start = utils.getIndexOf(this._sortedValues, this._threshold);
		double[] positives = Arrays.copyOfRange(this._sortedValues, start, this._sortedValues.length - 1);
		double iqr = utils.quantile(positives, 0.75) - utils.quantile(positives, 0.25);

		if (settings.debug > 0) {
			IJ.log("getBinWidth: threshold = " + this._threshold + ", start index =" + start
					+ ", above threshold count =" + positives.length);
			IJ.log("Q3 = " + utils.quantile(positives, 0.75) + ", Q1=" + utils.quantile(positives, 0.25));
			IJ.log("Bin width = " + (2 * iqr / Math.pow(positives.length, 1.0 / 3)));
		}

		return 2 * iqr / Math.pow(positives.length, 1.0 / 3);
	}

	public double[] GetCounts(double[] Bins, boolean overThreshold) {
		if (settings.debug > 0) {
			IJ.log("\n" + this.getClass().getSimpleName() + " GetCounts bins length :" + Bins.length);
		}
		double[] counts = new double[Bins.length];
		int i;
		for (i = 0; i < counts.length; i++) {
			counts[i] = 0;
		}
		int nBin = 0;
		double binTop = Bins[nBin + 1];

		for (i = 0; i < this._sortedValues.length; i++) {
			if (!overThreshold && _sortedValues[i] >= this._threshold) {
				break;
			}

			if (_sortedValues[i] >= binTop) {
				i--;
				nBin++;
				if (nBin < Bins.length - 1) {
					binTop = Bins[nBin + 1];
				} else {
					binTop = Double.MAX_VALUE;
				}
			} else if (overThreshold) {
				if (_sortedValues[i] >= this._threshold) {
					counts[nBin]++;
				}
			} else {
				counts[nBin]++;
			}
		}

		if (settings.debug >= 3) {
			for (int j = 0; j < Bins.length; j++) {
				IJ.log(j + "," + Bins[j] + "," + counts[j]);
			}
		}

		return counts;
	}

	public XYPlot getPlot() {
		return this._plot;
	}

	public void setThreshold(double threshold) {
		this._threshold = threshold;
		double[] Bins = GetBins();
		double[] Counts = GetCounts(Bins, true);
		double[] CountsBelowThreshold = GetCounts(Bins, false);
		if (this._plot != null) {
			this._plot.clearDomainMarkers();
		}
		int start = utils.getIndexOf(this._sortedValues, this._threshold);
		double[] positives = Arrays.copyOfRange(this._sortedValues, start, this._sortedValues.length - 1);
		drawPlot(Bins, Counts, CountsBelowThreshold, positives);
	}
}
