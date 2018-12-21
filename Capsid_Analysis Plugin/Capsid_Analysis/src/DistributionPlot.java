import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.util.Arrays;

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
import org.jfree.ui.TextAnchor;

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
	private double[] fitParams;

	private int countOverThreshold;

	private int countUnderThreshold;

	/**
	 * Distribution plot constructor
	 * 
	 * @param s         Settings to be used for threshold etc
	 * @param key       Title string
	 * @param color     Colour to use for drawing histogram
	 * @param values    Data (mean intensity values)
	 * @param threshold Threshold value to draw a vertical range marker at
	 * @param bAddFit   Add a fit to the data and overaly on chart
	 */
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
	
	public  void AddMarkers( double mean, double stdDev, double fittedMean, double fittedStdDev, double fittedGoodness, String txtBeforeObserved) {
		AddMarkers(this._plot , mean,  stdDev,  fittedMean,  fittedStdDev,  fittedGoodness, txtBeforeObserved, 0);
	}
	/**
	 * Add Markers lines for mean and S.D. and add observed data and fit mean and
	 * S.D. details to plot
	 * 
	 * @param mean
	 * @param stdDev
	 * @param fittedMean
	 * @param fittedStdDev
	 * @param fittedGoodness
	 * @param binWidth  Bin width being used (non zero when plotting histograms)
	 */
	public static  void AddMarkers(XYPlot plot, double mean, double stdDev, double fittedMean, double fittedStdDev, double fittedGoodness
			, String txtBeforeObserved,  double binWidth) {

		double maxValue = plot.getRangeAxis().getUpperBound();
		double lowerBound = plot.getDomainAxis().getLowerBound();
		double[] markerValues = { mean, mean - stdDev, mean + stdDev, mean - (stdDev * 2), mean + (stdDev * 2) };
		float[] dashes = { 10, 5, 5, 3, 3, 1, 1 };
		for (int i = 0; i < markerValues.length; i++) {
			ValueMarker marker = new ValueMarker(markerValues[i]); // position is the value on the axis
			float[] dash = { dashes[i] };
			marker.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1.0f, dash, 0.0f));
			plot.addDomainMarker(marker, Layer.FOREGROUND);
		}

		String str = "Observed Mean (\u03bc): " + String.format("%.2f", mean) + "\nObserved StdDev (\u03b4): " + String.format("%.2f", stdDev)
		+ "\nCV: " + String.format("%.2f", stdDev / mean * 100.0)
		+ (binWidth >0?String.format("\nBinWidth: %.1f", binWidth):"");
		if (txtBeforeObserved != null ) {
			str = txtBeforeObserved + str;
		}
		AddTextLabel(plot, str,
				lowerBound + 170, maxValue * 0.8);
		
		AddTextLabel(plot, "Fitted Mean: " + String.format("%.2f", fittedMean)
				+ "\nFitted StdDev: " + String.format("%.2f", fittedStdDev) + "\nFit Goodness: "
				+ String.format("%.2f", fittedGoodness), 170 + lowerBound, maxValue * 0.5);
		
		AddTextLabel(plot, "\u03bc", mean,maxValue*0.98);
		AddTextLabel(plot, "+\u03b4", mean+stdDev,maxValue*0.98);
		AddTextLabel(plot, "-\u03b4", mean-stdDev,maxValue*0.98);
	}
	
	public static void AddTextLabel(XYPlot plot, String label, double x,double y) {

		BasicMultiLineXYTextAnnotation newLabel = new BasicMultiLineXYTextAnnotation(label, x,y);
		newLabel.setFont(new Font("Arial", Font.BOLD, 12));
		newLabel.setTextAnchor(TextAnchor.TOP_LEFT);
		plot.addAnnotation(newLabel);

	}

	public void DoFit(double[] Bins, double[] Counts, double[] values, String txtBefore) {
		DefaultXYDataset xyFitDataSet = new DefaultXYDataset();

		CurveFitter cf = utils.GetFit(Bins, Counts, this._key + " (fit)", xyFitDataSet, settings.debug);
		this._plot.setDataset(2, xyFitDataSet);
		// and get rendered to draw it as a line with no shapes
		final XYLineAndShapeRenderer fitRenderer = new XYLineAndShapeRenderer(true, false);
		fitRenderer.setSeriesPaint(0,
				new Color(this._color.getRed(), this._color.getGreen(), this._color.getBlue(), 255));
		this._plot.setRenderer(2, fitRenderer);

		fitParams = cf.getParams();

		this._plot.clearAnnotations();
		AddMarkers(utils.Mean(values), utils.StdDev(values, utils.Mean(values)), fitParams[2], fitParams[3],
				cf.getFitGoodness(), txtBefore);

		this._plot.setDatasetRenderingOrder(DatasetRenderingOrder.FORWARD);
	}

	/**
	 * Draws this plot using the given data for bins and counts
	 * 
	 * @param BinBottoms
	 * @param Counts
	 * @param CountsBelowThreshold
	 * @param values
	 */
	public void drawPlot(double[] BinBottoms, double[] Counts, double[] CountsBelowThreshold, double[] values) {
		double[] Bins = new double[BinBottoms.length];
		for (int i = 0; i < Bins.length - 1; i++) {
			Bins[i] = (BinBottoms[i] + BinBottoms[i + 1]) / 2;
		}
		Bins[Bins.length - 1] = BinBottoms[Bins.length - 1]
				+ (BinBottoms[Bins.length - 1] - BinBottoms[Bins.length - 2]) / 2;

		if (IJ.debugMode ) {
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
				stringBuilder.append(String.format("Mean Range:%.1f - %.1f<br/>", x1.doubleValue() - (_binWidth / 2),
						x1.doubleValue() + (_binWidth / 2)));
				stringBuilder.append(String.format("ROI COunt: %d", y1.intValue()));
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
					stringBuilder.append(String.format("Range:%.1f - %.1f<br/>", x1.doubleValue() - (_binWidth / 2),
							x1.doubleValue() + (_binWidth / 2)));
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
		renderer.setDrawBarOutline(true);
		renderer.setShadowVisible(false);
		renderer.setBarPainter(new StandardXYBarPainter());
		renderer.setBaseToolTipGenerator(xyToolTipGenerator);
		renderer.setSeriesOutlinePaint(0, Color.BLACK);
		renderer.setMargin(0);
		// Create the plot
		// XYPlot plot = new XYPlot(dataset, new NumberAxis("Mean Intensity"), new
		// NumberAxis("ROI Count"),renderer);

		// construct the plot

		this._plot.setDataset(0, dataset);
		// xy2Data.
		this._plot.setDataset(1, dataset2);
		this._plot.setRenderer(0, renderer);

		if (IJ.debugMode) {
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

		

		// Make sure range axis goes from zero to highest value for the two axes
		this._plot.getRangeAxis(0).setLowerBound(0);
		// Now adjust count of ROI below threshold - if it is smaller than count of ROI
		// above, we set it the same
		double upper = this._plot.getRangeAxis(0).getUpperBound();
	
		if (upper > this._plot.getRangeAxis(1).getUpperBound() ||
				upper > this._plot.getRangeAxis(0).getUpperBound()) {
			this._plot.getRangeAxis(1).setUpperBound(upper);
		
		}

		// Make sure x axis starts at zero
		this._plot.getDomainAxis().setLowerBound(0);
		// Add a fitted curve (gaussian) if required on this histogram plot
		if (this._bAddFit) {
			DoFit(Bins, Counts, values, "Total # of particles: "+this._sortedValues.length+"\nTotal over threshold: "+this.countOverThreshold
					+"\nTotal under threshold: "+this.countUnderThreshold+"\n \n");
		}
		

	}

	// return set of bins from 0 to 255 based on current threshold
	public double[] GetBins() {

		this._binWidth = getBinWidth();
		if (settings.autoBin) { 
			settings.binField.setValue(this._binWidth);
		}
		else {
			double manualBin =  ((Number)settings.binField.getValue()).doubleValue();
			if (IJ.debugMode) {IJ.log("Using manual bin: " + manualBin); }
			if (manualBin > 0) {
				this._binWidth = manualBin;
			}
		}
		// Number of bins
		int nBinCount = (int) (255.0 / this._binWidth + 1);
		// We have to make sure the threshold is the start of a bin, so work out offset
		// Ofset is reaminder of threshold over bin width
		double offset = this._binWidth - (this._threshold % this._binWidth);

		double[] Bins = new double[nBinCount];
		if (IJ.debugMode ) {
			IJ.log(this.getClass().getSimpleName() + " GetBins bins length :" + Bins.length + "\nBin width = "
					+ this._binWidth + ", Offset = " + offset);
		}
		Bins[0] = 0;
		for (int i = 1; i < Bins.length; i++) {
			Bins[i] = this._binWidth * i - offset;
		}
		return Bins;

	}

	/**
	 * Used Freedman-Draconis
	 * (https://en.wikipedia.org/wiki/Freedman%E2%80%93Diaconis_rule) to get bin
	 * width
	 * 
	 * @return binWidth to be used for frequency histograms
	 */
	public double getBinWidth() {
		// Get the values that are above current threshold
		int start = utils.getIndexOf(this._sortedValues, this._threshold);
		double[] positives = Arrays.copyOfRange(this._sortedValues, start, this._sortedValues.length - 1);
		double iqr = utils.quantile(positives, 0.75) - utils.quantile(positives, 0.25);

		if (IJ.debugMode) {
			IJ.log("getBinWidth: threshold = " + this._threshold + ", start index =" + start
					+ ", above threshold count =" + positives.length);
			IJ.log("Q3 = " + utils.quantile(positives, 0.75) + ", Q1=" + utils.quantile(positives, 0.25));
			IJ.log("Bin width = " + (2 * iqr / Math.pow(positives.length, 1.0 / 3)));
		}

		return 2 * iqr / Math.pow(positives.length, 1.0 / 3);
	}

	/**
	 * Returns the correct bin number for the given value for the givens bins
	 * @param Bins
	 * @param value
	 * @return
	 */
	public int GetBinForValue(double []Bins, double value) {

		for(int i=0; i<Bins.length; i++) {
			if (value<Bins[i]  ) {
			return i;
			}
		}
		return Bins.length - 1;
	}
	/**
	 * Returns counts of particles over or under given threshold using given bins
	 * 
	 * @param Bins          Bin ranges (Bins[0]<=particles value <= Bins[1] etc...)
	 * @param overThreshold false if counting particles under threshold
	 * @return
	 */
	public double[] GetCounts(double[] Bins, boolean bOverThreshold) {
		if (IJ.debugMode) {
			IJ.log("\n" + this.getClass().getSimpleName() + " GetCounts " +(bOverThreshold?"over":"under")+" threshold bins length :" + Bins.length);
		}
		double[] counts = new double[Bins.length];
		int i;
		for (i = 0; i < counts.length; i++) {
			counts[i] = 0;
		}
		this.countOverThreshold = 0;
		this.countUnderThreshold = 0;

		for (i = 0; i < this._sortedValues.length; i++) {
			double value = this._sortedValues[i];
			if ( value >= this._threshold) {
				this.countOverThreshold++;
				IJ.log("sortedValues[" +i+"] = "+value+" over threshold: "+this.countOverThreshold);
			} else {
				this.countUnderThreshold++;
				IJ.log("sortedValues[" +i+"] = "+value+" under threshold: "+this.countUnderThreshold);
			}
			
			if (!bOverThreshold && _sortedValues[i] >= this._threshold) {
				IJ.log("Break");
				break;
			}
			int nBin = GetBinForValue(Bins, value);
			if (bOverThreshold && value >= this._threshold) {
				counts[nBin]++;
			} else  if ( !bOverThreshold && value < this._threshold ) {
				counts[nBin]++;
			}
		}

		if (settings.debug >= 3) {
			for (int j = 0; j < Bins.length; j++) {
				IJ.log("Bin " + j + ": " + Bins[j] + "," + counts[j]);
			}
		}

		return counts;
	}

	public XYPlot getPlot() {
		return this._plot;
	}

	/**
	 * Add a vertical marker for the given threshold then draw the plot
	 * 
	 * @param threshold THe threshold value at which to draw marker
	 */
	public void setThreshold(double threshold) {
		this._threshold = threshold;
		double[] Bins = GetBins();
		double[] CountsBelowThreshold = GetCounts(Bins, false);
		double[] Counts = GetCounts(Bins, true);

		if (this._plot != null) {
			this._plot.clearDomainMarkers();
		}
		int start = utils.getIndexOf(this._sortedValues, this._threshold);
		double[] positives = Arrays.copyOfRange(this._sortedValues, start, this._sortedValues.length - 1);
		drawPlot(Bins, Counts, CountsBelowThreshold, positives);
	}
}
