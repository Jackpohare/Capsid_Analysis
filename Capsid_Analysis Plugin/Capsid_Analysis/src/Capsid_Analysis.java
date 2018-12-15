import java.awt.BasicStroke;
import java.awt.Button;
import java.awt.Checkbox;
import java.awt.Choice;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Panel;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
// import java.awt.event.*;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Vector;

import javax.swing.JPopupMenu;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.ClusteredXYBarRenderer;
import org.jfree.chart.renderer.xy.StandardXYBarPainter;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.xy.DefaultXYDataset;
import org.jfree.data.xy.IntervalXYDataset;
import org.jfree.data.xy.XYBarDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.ui.Layer;
import org.jfree.ui.RectangleEdge;
import org.jfree.ui.VerticalAlignment;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.gui.ImageCanvas;
import ij.gui.NonBlockingGenericDialog;
import ij.gui.Overlay;
import ij.gui.Plot;
import ij.gui.PlotWindow;
import ij.gui.Roi;
// import ij.gui.*;
import ij.measure.CurveFitter;
import ij.measure.ResultsTable;
import ij.plugin.PlugIn;
import ij.plugin.filter.MaximumFinder;
import ij.plugin.frame.RoiManager;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import ij.text.TextPanel;
import ij.text.TextWindow;

public class Capsid_Analysis implements PlugIn, ActionListener {

	class PopupActionListener implements ActionListener {
		Particle p;

		public PopupActionListener(Particle p2) {
			this.p = p2;
		}

		public void actionPerformed(ActionEvent actionEvent) {
			switch (actionEvent.getActionCommand()) {
			case "Remove":
				settings.overlay.remove(p.roi);
				RoiManager rm = RoiManager.getRoiManager();
				rm.select(p.id - 1);
				if (rm.getSelectedIndex() > -1) {
					rm.runCommand("Delete");
				}
				RemoveResult(p);
				particles.remove(p);
				UpdateControlPanel();
				break;
			case "Set as Green Threshold":
				SetThresholdFromParticle(p, "green", false);
				break;
			case "Set as Red Threshold":
				SetThresholdFromParticle(p, "red", false);
				break;
			case "Set BELOW Green Threshold":
				SetThresholdFromParticle(p, "green", true);
				break;
			case "Set BELOW Red Threshold":
				SetThresholdFromParticle(p, "red", true);
				break;
			case "Show Info":
				p.ShowInfo();
				break;
			case "Add ROI here":
				DoAddParticle(this.p);
				break;
			}
		}
	}

	class tableComparator implements Comparator<String[]> {
		private int columnToSortOn;
		private boolean ascending;
		private int[] stringCols;

		// contructor to set the column to sort on.
		tableComparator(int columnToSortOn, boolean ascending, int[] stringCols) {
			this.columnToSortOn = columnToSortOn;
			this.ascending = ascending;
			this.stringCols = stringCols;
		}

		// Implement the abstract method which tells
		// how to order the two elements in the array.
		public int compare(String[] o1, String[] o2) {
			String[] row1 = (String[]) o1;
			String[] row2 = (String[]) o2;
			int res;
			if (Double.parseDouble(row1[columnToSortOn]) == Double.parseDouble(row2[columnToSortOn]))
				return 0;
			if (Double.parseDouble(row1[columnToSortOn]) > Double.parseDouble(row2[columnToSortOn]))
				res = 1;
			else
				res = -1;
			if (ascending)
				return res;
			else
				return (-1) * res;

		}
	}

	// constants
	protected static final int CB_SHOW_ROI = 1, CB_FILL = 2, CB_GREYSCALE = 0, CB_REDONLY = 3;
	protected static final int CB_GREENONLY = CB_REDONLY + 1, CB_BOTH = CB_REDONLY + 2, CB_EMPTY = CB_REDONLY + 3;
	protected static final int CB_SCATTER_POS = CB_EMPTY + 1, CB_INTENSITY_POS = CB_SCATTER_POS + 1,
			CB_FREQUENCY_POS = CB_SCATTER_POS + 2, CB_SCATTER_ALL = CB_SCATTER_POS + 3,
			CB_INTENSITY_ALL = CB_SCATTER_POS + 4, CB_FREQUENCY_ALL = CB_SCATTER_POS + 5;
	protected static final int SF_RED = 0, SF_GREEN = SF_RED + 1;
	protected static final int TF_NOISETOLERANCE = 0;
	protected static final int TF_POINTDIAMETER = TF_NOISETOLERANCE + 1, TF_BACKGROUNDDEV = TF_NOISETOLERANCE + 2;

	public static NonBlockingGenericDialog dlg;

	String sVersion = " (v1.2.10, 14-Dec-2018)";;

	public ResultsTable2 rt;

	public ResultsTable2 resultsSummary;

	public ParticleList particles;

	public AnalysisSettings settings = new AnalysisSettings();

	String[] plotList = new String[] { "All Particles Scatter Plot", "All Particles Sorted Intensity Plot",
			"All Particles Green Distribution Plot", "All Particles Red Distribution Plot",
			"Positive Particles Comparitive Distribution Plot", "Positive Particles Sorted Intensity Plot" };
	

	ItemListener cbxHandler = new ItemListener() {

		@Override
		public void itemStateChanged(ItemEvent event) {
			IJ.log("State changed: " + (String) event.getItem() + ", " + event.getID());
			IJ.log(event.toString());
			Checkbox cbx = (Checkbox) event.getSource();
			IJ.log(cbx.getName() + ", " + cbx.getLabel() + ", ID=" + cbx.getName().replace("checkbox", ""));

			int cbID = Integer.parseInt(cbx.getName().replace("checkbox", ""));
			boolean cbEnabled = cbx.getState();
			IJ.log("cbID = " + cbID + ", state= " + cbEnabled);

			switch (cbID) {
			case CB_SHOW_ROI:
				settings.bShowROI = cbEnabled;
				break;
			case CB_FILL:
				settings.bFillROI = cbEnabled;
				break;
			}
			DoShowROI(settings);
	
	}
	};
	
	
	@Override
	/**
	 * Handler for any button press
	 */
	public void actionPerformed(ActionEvent e) {
		IJ.log("Action performed: " + e.getActionCommand());
		switch (e.getActionCommand()) {
		case "Find Maxima and generate ROI":
			if (IJ.getImage().getType() != ImagePlus.COLOR_RGB) {
				IJ.run("RGB Color");
			}
			dlg.setTitle("Capsid Analysis " + sVersion + " - finding maxima, please wait....");

			// Ensure we have current image, image window and canvas
			settings.image = IJ.getImage();
			settings.win = settings.image.getWindow();
			settings.canvas = settings.win.getCanvas();

			// get noise and point size then do all the work
			Vector<TextField> values = dlg.getNumericFields(); // values 0 is noise, values 1 is point diameter
			Vector<Checkbox> cbx = dlg.getCheckboxes();
			this.settings.noiseTolerance = Double.parseDouble(values.get(TF_NOISETOLERANCE).getText());

			this.settings.pointDiameter = Double.parseDouble(values.get(TF_POINTDIAMETER).getText());

			this.settings.backgroundDevFactor = Double.parseDouble(values.get(TF_BACKGROUNDDEV).getText());

			this.settings.bFillROI = cbx.get(CB_FILL).getState();
			this.settings.bGreyscale = cbx.get(CB_GREYSCALE).getState();

			IJ.log("Finding particles with noise " + settings.noiseTolerance + "and diameter" + settings.pointDiameter);
			this.particles = FindParticles();

			IJ.log("Particle count: " + this.particles.size());

			this.particles.RecalcParticles(this.settings);
			int[] pixels = GetBackgroundPixels();
			IJ.log("Background Red mean and SD: " + utils.Mean(pixels, "red") + ", " + utils.StdDev(pixels, "red"));
			IJ.log("Background Green mean and SD: " + utils.Mean(pixels, "green") + ", "
					+ utils.StdDev(pixels, "green"));

			settings.redBackgroundMean = utils.Mean(pixels, "red");
			settings.redBackgroundStdDev = utils.StdDev(pixels, "red");
			settings.greenBackgroundMean = utils.Mean(pixels, "green");
			settings.greenBackgroundStdDev = utils.StdDev(pixels, "green");

			// Get threshold method and set thresholds
			Vector<Choice> choices = dlg.getChoices();
			settings.thresholdMethod = ThresholdMode.values()[choices.get(0).getSelectedIndex()];
			if (ThresholdMode.THRESHOLD_MEAN == settings.thresholdMethod) {
				this.settings.redThreshold = (settings.redBackgroundMean
						+ settings.redBackgroundStdDev * settings.backgroundDevFactor);
				this.settings.greenThreshold = (settings.greenBackgroundMean
						+ settings.greenBackgroundStdDev * settings.backgroundDevFactor);
			} else {
				this.settings.redThreshold = (settings.redBackgroundMean * 3.1415926
						* ((settings.pointDiameter / 2.0) * (settings.pointDiameter / 2.0)))
						* settings.backgroundDevFactor;
				this.settings.greenThreshold = (settings.greenBackgroundMean * 3.1415926
						* ((settings.pointDiameter / 2.0) * (settings.pointDiameter / 2.0)))
						* settings.backgroundDevFactor;
			}

			this.particles.ClassifyParticles(settings);
			this.particles.SetRoi(settings.debug);
			this.particles.RankParticles();
			SetResults(settings);
			UpdateControlPanel();

			DumpParticles(particles, "c:\\temp\\AllPx.csv", "all");
			DumpParticles(particles, "c:\\temp\\RedPx.csv", "red");
			DumpParticles(particles, "c:\\temp\\GreenPx.csv", "green");

			DoSummaryResults();

			if (settings.image.getOverlay() != null && !settings.image.getHideOverlay()) {
				DoShowROI(this.settings);
			}
			dlg.setTitle("Capsid Analysis" + sVersion);
			settings.win.toFront();
			break;

		case "Delete Empty":
			DoDeleteEmpty();
			break;

		case "Update Results":
			DoUpdateResults();
			break;
		case "Show Overlapping ROI":
			this.settings.bShowOverlaps = true;
			DoShowROI(this.settings);
			break;
		case "Delete Overlapping ROI":
			DoRemove(particles, "overlap");
			break;
		case "Show ROI":
			DoShowROI(this.settings);
			break;

		case "Show Selected Plot":
			DoPlots(settings);

			break;

		case "Red Channel":
			ShowRedChannel();
			break;

		case "Green Channel":
			ShowGreenChannel();
			break;

		case "Show Channels":
			ShowAllChannels();
			break;

		case "Merge Channels":
			ShowRGB();
			break;
		}

		// dlg.setVisible(true);
		e = null;

	}

	public void AddCanvasListener() {
		if (this.settings.targetCanvas == settings.canvas) {
			return;
		}
		// Image Mouse Click
		settings.canvas.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				int x, y, z, flags, offscreenX, offscreenY;
				x = e.getX();
				y = e.getY();
				offscreenX = settings.canvas.offScreenX(x);
				offscreenY = settings.canvas.offScreenY(y);
				GenericDialog gd = new GenericDialog("Options");
				Particle p = particles.FindRoi(offscreenX, offscreenY);
				String text = "";
				if (p != null) {
					roiPopupMenu(p, x, y);
				} else {
					DoPopupMenu(particles, x, y, offscreenX, offscreenY);
				}
			}
		});
		settings.targetCanvas = settings.canvas;
	}

	public void AreaDemo(double[][] green, double[][] red) {
		// create a dataset...
		final double[][] data = new double[2][];

		final DefaultXYDataset dataset = new DefaultXYDataset();

		dataset.addSeries("Green", green);
		dataset.addSeries("Red", red);

		// dataset.addSeries("Red", red);
		// create the chart...
		final JFreeChart chart = createMyAreaChart(dataset);
		final ChartPanel chartPanel = new ChartPanel(chart);
		chartPanel.setPreferredSize(new Dimension(800, 600));
		chartPanel.setEnforceFileExtensions(false);

		ImagePlus imp = IJ.createImage("Distributions", "RGB", 800, 600, 1);
		BufferedImage image = imp.getBufferedImage();
		chart.draw(image.createGraphics(), new Rectangle2D.Float(0, 0, imp.getWidth(), imp.getHeight()));
		imp.setImage(image);
		imp.show();

	}

	public void ColumnChart(double[][] xyGreen, double[][] xyRed) {
		// create a dataset...
		final DefaultXYDataset dataset = new DefaultXYDataset();

		dataset.addSeries("Green", xyGreen);
		dataset.addSeries("Red", xyRed);

		final XYBarDataset barDataset = new XYBarDataset(dataset, 0.3);

		// dataset.addSeries("Red", red);
		// create the chart...
		final JFreeChart chart = createClusteredChart("Mean Based Distribution", "SD", "Frequency", barDataset);
		final ChartPanel chartPanel = new ChartPanel(chart);
		chartPanel.setPreferredSize(new Dimension(1000, 800));
		chartPanel.setEnforceFileExtensions(false);

		ImagePlus imp = IJ.createImage("Mean Based Distribution", "RGB", 1000, 800, 1);
		BufferedImage image = imp.getBufferedImage();
		chart.draw(image.createGraphics(), new Rectangle2D.Float(0, 0, imp.getWidth(), imp.getHeight()));
		imp.setImage(image);
		imp.show();

		AreaDemo(xyGreen, xyRed);
	}

	public JFreeChart createClusteredChart(String title, String categoryAxisLabel, String valueAxisLabel,
			IntervalXYDataset dataset) {

		NumberAxis domainAxis = new NumberAxis(categoryAxisLabel);
		domainAxis.setAutoRangeIncludesZero(true);

		ValueAxis valueAxis = new NumberAxis(valueAxisLabel);

		XYBarRenderer renderer = new ClusteredXYBarRenderer();
		// renderer.setDefaultShadowsVisible(false);
		renderer.setDrawBarOutline(false);
		renderer.setShadowVisible(false);
		renderer.setMargin(0.2);
		renderer.setDefaultShadowsVisible(false);
		renderer.setSeriesPaint(0, new Color(0, 255, 0, 128));
		renderer.setSeriesPaint(1, new Color(255, 0, 0, 128));
		/*
		 * renderer.setBaseItemLabelGenerator( new StandardXYItemLabelGenerator());
		 * renderer.setBaseItemLabelsVisible(true);
		 */
		renderer.setBarAlignmentFactor(0.9);
		renderer.setGradientPaintTransformer(null);
		renderer.setBarPainter(new StandardXYBarPainter());
		XYPlot plot = new XYPlot(dataset, domainAxis, valueAxis, renderer);
		plot.setOrientation(PlotOrientation.VERTICAL);
		plot.getDomainAxis().setAutoRange(true);

		ValueMarker marker = new ValueMarker(0); // position is the value on the axis
		marker.setPaint(Color.black);
		// marker.setLabel("here"); // see JavaDoc for labels, colors, strokes
		marker.setStroke(new BasicStroke(1.0f));
		plot.addDomainMarker(marker, Layer.FOREGROUND);

		/*
		 * final NumberAxis xAxis2 = new NumberAxis("Secondary X Axis");
		 * plot.setDomainAxis(1, xAxis2 ); plot.mapDatasetToDomainAxis(0, 1);
		 */
		JFreeChart chart = new JFreeChart(title, JFreeChart.DEFAULT_TITLE_FONT, plot, true);

		return chart;
	}

	public JFreeChart createMyAreaChart(final DefaultXYDataset dataset) {

		final JFreeChart chart = ChartFactory.createXYAreaChart("Particle Distribution Comparison", // chart title
				"Standard Deviation from Mean", // domain axis label
				"Particle Count", // range axis label
				dataset, // data
				PlotOrientation.VERTICAL, // orientation
				true, // include legend
				true, // tooltips
				false // urls
		);

		// NOW DO SOME OPTIONAL CUSTOMISATION OF THE CHART...

		// set the background color for the chart...
//		        final StandardLegend legend = (StandardLegend) chart.getLegend();
		// legend.setAnchor(StandardLegend.SOUTH);

		chart.setBackgroundPaint(Color.white);
		final TextTitle subtitle = new TextTitle("An area chart demonstration.  We use this "
				+ "subtitle as an example of what happens when you get a really long title or " + "subtitle.");
		subtitle.setFont(new Font("SansSerif", Font.PLAIN, 12));
		subtitle.setPosition(RectangleEdge.TOP);
//		        subtitle.setSpacer(new Spacer(Spacer.RELATIVE, 0.05, 0.05, 0.05, 0.05));
		subtitle.setVerticalAlignment(VerticalAlignment.BOTTOM);
		chart.addSubtitle(subtitle);

		final XYPlot plot = chart.getXYPlot();
		plot.setForegroundAlpha(0.5f);

		// plot.setAxisOffset(new Spacer(Spacer.ABSOLUTE, 5.0, 5.0, 5.0, 5.0));
		plot.setBackgroundPaint(Color.WHITE);
		plot.setDomainGridlinesVisible(true);
		plot.setDomainGridlinePaint(Color.GRAY);
		plot.setRangeGridlinesVisible(true);
		plot.setRangeGridlinePaint(Color.GRAY);

		ValueMarker marker = new ValueMarker(0); // position is the value on the axis
		marker.setPaint(Color.black);
		float[] dash = { 10 };
		marker.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1.0f, dash, 0.0f));
		// marker.setLabel("here"); // see JavaDoc for labels, colors, strokes

		plot.addDomainMarker(marker, Layer.FOREGROUND);

		marker = new ValueMarker(-1); // position is the value on the axis
		marker.setPaint(Color.darkGray);
		dash[0] = 6;
		marker.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1.0f, dash, 0.0f));
		// marker.setLabel("here"); // see JavaDoc for labels, colors, strokes

		plot.addDomainMarker(marker, Layer.FOREGROUND);

		marker = new ValueMarker(1); // position is the value on the axis
		marker.setPaint(Color.darkGray);
		dash[0] = 6;
		marker.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1.0f, dash, 0.0f));
		plot.addDomainMarker(marker, Layer.FOREGROUND);

		marker = new ValueMarker(-2); // position is the value on the axis
		marker.setPaint(Color.darkGray);
		dash[0] = 3;
		marker.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1.0f, dash, 0.0f));
		// marker.setLabel("here"); // see JavaDoc for labels, colors, strokes

		plot.addDomainMarker(marker, Layer.FOREGROUND);

		marker = new ValueMarker(2); // position is the value on the axis
		marker.setPaint(Color.darkGray);
		dash[0] = 3;
		marker.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1.0f, dash, 0.0f));
		// marker.setLabel("here"); // see JavaDoc for labels, colors, strokes

		plot.addDomainMarker(marker, Layer.FOREGROUND);

		XYItemRenderer renderer = plot.getRenderer();
		renderer.setSeriesPaint(0, new Color(0, 255, 0, 255));
		renderer.setSeriesPaint(1, new Color(255, 0, 0, 255));

		/*
		 * final CategoryAxis domainAxis = plot.getDomainAxis();
		 * domainAxis.setCategoryLabelPositions(CategoryLabelPositions.UP_45);
		 * domainAxis.setLowerMargin(0.0); domainAxis.setUpperMargin(0.0);
		 * domainAxis.addCategoryLabelToolTip("Type 1", "The first type.");
		 * domainAxis.addCategoryLabelToolTip("Type 2", "The second type.");
		 * domainAxis.addCategoryLabelToolTip("Type 3", "The third type.");
		 * 
		 * final NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
		 * rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
		 * rangeAxis.setLabelAngle(0 * Math.PI / 2.0); // OPTIONAL CUSTOMISATION
		 * COMPLETED.
		 */
		return chart;

	}

	/**
	 * Add given particles to list of particles and update results etc....
	 * 
	 * @param p Particle to be added
	 */
	public void DoAddParticle(Particle p) {
		RoiManager rm = RoiManager.getRoiManager();
		p.roi.setName(String.valueOf(p.id));
		rm.addRoi(p.roi);
		particles.add(p);
		UpdateControlPanel();
		SetResults(settings);
		DoShowROI(settings);
		DoSummaryResults();

	}

	public void DoDeleteEmpty() {
		for (int i = 0; i < particles.size(); i++) {
			Particle p = particles.get(i);
			if (p.IsEmpty()) {
				particles.remove(i);
				i--;
			}
		}

		DoSummaryResults();
		UpdateControlPanel();
		DoUpdateResults();
		if (settings.image.getOverlay() != null && !settings.image.getHideOverlay()) {
			DoShowROI(settings);
		}

	}

	/**
	 * Set everything up (settings, get image, etc), set up event handlers,
	 * calculate various important image values then show the controlling modal dialog
	 */
	private void DoDialog() {
		IJ.log("Begin DoDialog");
		this.settings = new AnalysisSettings();
		settings.image = IJ.getImage();
		settings.win = settings.image.getWindow();
		settings.canvas = settings.win.getCanvas();

		// set handlers
		IJ.log("DoDialog: Create results table ");
		rt = new ResultsTable2();
		rt.show("Results");
		rt.reset();
		setHandlers();
		// Setup sets background etc
		Setup();
		ShowDialog(settings);
	}

/*	private void DoFrequencyPlot(AnalysisSettings s, double bucketWidth, boolean bPositivesOnly, boolean bRedOnly,
			boolean bGreenOnly, String sTitle) {
		// Get only positivies
		int i;
		double[] red;
		double[] green;

		if (s.debug > 0) {
			IJ.log("\nDoPositiveFrequencyPlot - " + sTitle);
		}

		settings.GetMax(particles);

		int nCount;
		if (bPositivesOnly) {
			red = particles.GetPosRed();
			green = particles.GetPosGreen();
			nCount = red.length;
		} else {
			red = particles.GetArrayFromList("redval");
			green = particles.GetArrayFromList("greenval");
			nCount = red.length;
		}
		double maxGreen = 0, maxRed = 0;
		for (i = 0; i < green.length; i++) {
			if (green[i] > maxGreen) {
				maxGreen = green[i];
			}
		}
		for (i = 0; i < red.length; i++) {
			if (red[i] > maxRed) {
				maxRed = red[i];
			}
		}

		if (s.debug > 0) {
			IJ.log("DoPositiveFrequencyPlot - max positive green: " + maxGreen);
		}

		// check if using Freedman–Diaconis' choice
		if (bucketWidth == 0) {
			double[] arr;
			if (bGreenOnly) {
				arr = green;
			} else if (bRedOnly || bPositivesOnly) {
				arr = red;
			} else {
				arr = maxGreen > maxRed ? green : red;
			}
			if (arr.length <= 0) {
				IJ.log("DoFrequencyPlot auto bin array is empty");
				return;
			}
			if (s.debug > 0) {
				IJ.log("DoFrequencyPlot arr length = " + arr.length);
				if (s.debug > 3) {
					IJ.log("DoFrequencyPlot arr:");
					for (int idx = 0; idx < arr.length; idx++) {
						IJ.log(idx + "," + arr[idx]);
					}
				}
			}
			bucketWidth = 2 * utils.iqr(arr) / Math.pow(arr.length, 1.0 / 3);
			if (s.debug > 0) {
				IJ.log("IQR is " + utils.iqr(arr));
				IJ.log("Divisor is " + Math.pow(arr.length, 1 / 3));
				IJ.log("DoFrequencyPlot auto bin interval is " + bucketWidth);
			}
		}

		// Work out bins based on greenMax - bins go from 0 in binInterval steps to
		// first binInterval above greenMax (.e.g 15 is max & interval is ten then it is
		// 0,10,20)
		// get top limit

		double nTopBin = maxGreen - (maxGreen % bucketWidth) + bucketWidth;
		if (maxRed > maxGreen) {
			nTopBin = maxRed - (maxRed % bucketWidth) + bucketWidth;
		}
		if (bRedOnly) {
			nTopBin = s.maxRed - (s.maxRed % bucketWidth) + bucketWidth;
		}
		if (s.debug > 0) {
			IJ.log("DoPositiveFrequencyPlot - top bin: " + nTopBin);
		}

		// NUmber of bins
		int nBinCount = (int) Math.floor((nTopBin / bucketWidth) + 1);
		if (s.debug > 0) {
			IJ.log("DoPositiveFrequencyPlot -  bin count: " + nBinCount);
		}

		double[] greenBinCounts = new double[nBinCount];
		double[] redBinCounts = new double[nBinCount];
		double[] Bins = new double[nBinCount];
		double maxGreenFrequency = 0, maxRedFrequency = 0;
		// Zero counts
		for (i = 0; i < nBinCount; i++) {
			greenBinCounts[i] = 0;
			redBinCounts[i] = 0;
			Bins[i] = bucketWidth * i + bucketWidth / 2;
		}

		// Now do counts
		for (i = 0; i < green.length; i++) {
			int nBinIndex = (int) Math.floor(green[i] / bucketWidth);
			if (nBinIndex < nBinCount) {
				greenBinCounts[nBinIndex]++;
				if (greenBinCounts[nBinIndex] > maxGreenFrequency) {
					maxGreenFrequency = greenBinCounts[nBinIndex];
				}
			}
			nBinIndex = (int) Math.floor(red[i] / bucketWidth);
			if (nBinIndex < nBinCount) {
				redBinCounts[nBinIndex]++;
				if (redBinCounts[nBinIndex] > maxRedFrequency) {
					maxRedFrequency = redBinCounts[nBinIndex];
				}
			}
		}

		Plot p = new Plot(sTitle,
				(settings.thresholdMethod == ThresholdMode.THRESHOLD_MEAN ? "Mean " : "") + "Intensity",
				"Count of particles");
		// Plot.setLimits(0,x.length, greenMax*1.1);
		// Plot.setFontSize(14);
		// Plot.addText(sTitle,0.1, 0.1);

		// Do green counts as individual lines with a think line to simulate a column
		// chart
		if (!bRedOnly) {
			p.setColor(new Color(0, 255, 0, 128));
			p.setLineWidth(10);
			for (i = 0; i < Bins.length; i++) {
				double[] cx = { Bins[i] + (bGreenOnly ? 0 : -1), Bins[i] + (bGreenOnly ? 0 : -1) },
						cy = { 0, greenBinCounts[i] };
				p.setLineWidth(10);
				p.setColor(new Color(0, 255, 0, 128));
				if (cy[1] > 0) {
					p.setLineWidth(10);
					p.setColor(new Color(0, 255, 0, 128));
					p.add("line", cx, cy);
					// p.drawLine(cx[0], cy[0], cx[1], cy[1]);
				}

				if (s.debug > 0) {
					IJ.log("Green " + (Bins[i] - bucketWidth / 2) + " - " + (Bins[i] + bucketWidth / 2) + ": " + cy[1]);
				}
			}
		}
		if (!bGreenOnly) {
			p.setColor(new Color(255, 0, 0, 128));
			p.setLineWidth(10);
			for (i = 0; i < Bins.length; i++) {
				double[] cx = { Bins[i] + (bRedOnly ? 0 : 1), Bins[i] + (bRedOnly ? 0 : 1) },
						cy = { 0, redBinCounts[i] };
				if (cy[1] > 0) {
					p.setLineWidth(10);
					p.setColor(new Color(255, 0, 0, 128));
					p.add("line", cx, cy);
					// p.drawLine(cx[0], cy[0], cx[1], cy[1]);
				}

				if (s.debug > 0) {
					IJ.log("Red " + (Bins[i] - bucketWidth / 2) + " - " + (Bins[i] + bucketWidth / 2) + ": " + cy[1]);
				}

			}
		}
		
		 * p.setColor(Color.green, Color.green); p.setLineWidth(1); p.add("circle",
		 * Bins, greenBinCounts);
		 * 
		 * p.setColor(Color.red, Color.red); p.setLineWidth(1); p.add("circle", Bins,
		 * redBinCounts);
		 
		for (i = 0; i < nBinCount && s.bLabelValues; i++) {
			p.setFont(new Font("Helvetica", Font.BOLD, PlotWindow.fontSize));
			p.setColor(new Color(153, 0, 0), new Color(153, 0, 0));
			p.addText(String.valueOf(redBinCounts[i]), Bins[i], redBinCounts[i]);
			p.setColor(new Color(0, 102, 51), new Color(0, 102, 51));
			p.addText(String.valueOf(greenBinCounts[i]), Bins[i], greenBinCounts[i]);
		}

		if (!bRedOnly) {
			p.setColor(Color.green, Color.green);
			p.setLineWidth(1);
			if (settings.debug > 0) {
				IJ.log("Fitting green...");
			}
			DoPlotFit(Bins, greenBinCounts, p, s, bPositivesOnly ? Color.green : Color.black, -1);
		}

		if (!bGreenOnly) {
			p.setColor(Color.red, Color.red);
			p.setLineWidth(1);
			if (settings.debug > 0) {
				IJ.log("Fitting red...");
			}

			DoPlotFit(Bins, redBinCounts, p, s, bPositivesOnly ? Color.red : Color.black,
					(bPositivesOnly ? 0.9 : 0.05));
		}

		if (bPositivesOnly) {
			DrawDistributions(Bins, greenBinCounts, Bins, redBinCounts, settings);
			OverlappedFrequencyChart(green, red, s);
			IJ.log("\nDistributionChart - ");
			DistributionChart newChart = new DistributionChart("Overlapped", particles, s, ChartType.OVERLAPPED);
//	    	newChart = new  DistributionChart("MULTI", particles, s, ChartType.MULTI);
			newChart = new DistributionChart("MULTI", particles, s, ChartType.DOUBLE_PLOT);
			newChart = new DistributionChart("Scatter", particles, s, ChartType.ALL_SCATTER);
		}

		if (bRedOnly) {
			// dO BACKGROUND DOTTED LINE
			p.setColor(Color.red, Color.red);
			p.setLineWidth(1);
			p.drawDottedLine(s.redThreshold, 0, s.redThreshold, s.maxGreen * 1.1, 2);

		}
		if (bGreenOnly) {
			// dO BACKGROUND DOTTED LINE
			p.setColor(Color.green, Color.green);
			p.setLineWidth(1);
			p.drawDottedLine(s.greenThreshold, 0, s.greenThreshold, maxGreen * 1.1, 2);

		}

		// p.setLimitsToFit(true);
		// Set xMax and yMax depending on type of frequency plot
		double xMax, yMax;
		if (bGreenOnly) {
			xMax = s.maxGreen * 1.1;
			yMax = maxGreenFrequency * 1.1;
			p.setLimits(0, xMax, 0, yMax);
		} else if (bRedOnly) {
			xMax = s.maxRed * 1.1;
			yMax = maxRedFrequency * 1.1;
			p.setLimits(0, xMax, 0, yMax);
		} else {
			p.setLimitsToFit(false);
		}

		p.setColor(Color.black, Color.black);
		p.setLineWidth(1);
		p.show();

	}

*/	
	 
	/**
	 * @param x
	 * @param y
	 * @param p
	 * @param s
	 * @param paramColor
	 * @param xParam
	 */
	public void DoPlotFit(double[] x, double[] y, Plot p, AnalysisSettings s, Color paramColor, double xParam) {
		if (s.debug > 0) {
			IJ.log("DoPlotFit");
		}
		if (s.debug > 1) {
			for (int i = 0; i < x.length; i++) {
				IJ.log(x[i] + "," + y[i]);
			}
		}
		CurveFitter cf = new CurveFitter(x, y);

		cf.doFit(CurveFitter.GAUSSIAN);

		double[] params = cf.getParams();
		for (int i = 0; i < params.length; i++) {
			if (s.debug > 0) {
				IJ.log("param " + i + ": " + params[i]);
			}
		}

		drawParams(p, params, paramColor, xParam);

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
			// IJ.log("Fit " + fitX[i] + ": " + fitY[i]);
		}

		p.add("line", fitX, fitY);
	}

	public void DoPlots(AnalysisSettings settings) {
		DistributionChart newChart;
		settings.GetMax(particles);
		// Get chosen plot
		Vector<Choice> choices = dlg.getChoices();
		int plotType = choices.get(1).getSelectedIndex();
		/*
		 * if (cbx.get(CB_SCATTER_POS).getState()) { DoScatterPlot(settings, true); } if
		 * (cbx.get(CB_SCATTER_ALL).getState()) { DoScatterPlot(settings, false); }
		 * 
		 * if (cbx.get(CB_INTENSITY_POS).getState()) {
		 * DoSortedIntensityPlot(settings,true); }
		 * 
		 * if (cbx.get(CB_INTENSITY_ALL).getState()) {
		 * DoSortedIntensityPlot(settings,false); } // settings.bucketWidth = 0; if
		 * (cbx.get(CB_FREQUENCY_POS).getState()) { DoFrequencyPlot(settings,
		 * settings.bucketWidth,true,false,false,"Frequency Plot: Positives" ); } if
		 * (cbx.get(CB_FREQUENCY_ALL).getState()) { if (!settings.bGreyscale) {
		 * DoFrequencyPlot(settings, settings.bucketWidth, false,true,false
		 * ,"Frequency Plot: All Red Particles"); } DoFrequencyPlot(settings,
		 * settings.bucketWidth, false, false, true,
		 * "Frequency Plot: All Green Particles"); }
		 */

		switch (plotType) {
		case 0: // All particles scatter plot
			newChart = new DistributionChart("MULTI", particles, settings, ChartType.ALL_SCATTER);
			break;
		case 1: // All Particles sorted intensity plot
			newChart = new DistributionChart("MULTI", particles, settings, ChartType.ALL_INTENSITY);
			break;
		case 2:
			newChart = new DistributionChart("MULTI", particles, settings, ChartType.GREEN_FREQUENCY);
			break;
		case 3:
			newChart = new DistributionChart("MULTI", particles, settings, ChartType.RED_FREQUENCY);
			break;
		case 4: // Comparative Distribution PLot
			newChart = new DistributionChart("MULTI", particles, settings, ChartType.DOUBLE_PLOT);
			break;
		case 5:
			newChart = new DistributionChart("MULTI", particles, settings, ChartType.POSITIVE_INTENSITY);
			break;
		}
	}

	/**
	 * Handles when user clicks canvas on point that is not in ROI Pops up menu that
	 * offers adding ROI manually
	 * 
	 * @param pList List of particles that we will add to if that options is chosen
	 * @param x     Coordinates of canvas click point
	 * @param y
	 */
	public void DoPopupMenu(ParticleList pList, int x, int y, int offScreenX, int offScreenY) {
		JPopupMenu menu = new JPopupMenu("Add Menu");
		Particle p = new Particle(particles.GetMaxID(), offScreenX, offScreenY, settings);
		p.setIntensity();
		p.setPct();
		p.classify();
		PopupActionListener listener = new PopupActionListener(p);

		Font f = menu.getFont().deriveFont((float) 9.0);
		menu.setFont(f);
		menu.add("Add ROI here").addActionListener(listener);

		menu.add("Show Info").addActionListener(listener);

		menu.show(settings.canvas, x, y);
	}

	public void DoRemove(ParticleList pList, String type) {
		switch (type) {
		case "overlap":
			for (int i = pList.size() - 1; i >= 0; i--) {
				if (pList.get(i).Overlaps()) {
					RoiManager rm = RoiManager.getRoiManager();
					rm.select(pList.get(i).id - 1);
					if (rm.getSelectedIndex() > -1) {
						rm.runCommand("Delete");
					}
					RemoveResult(pList.get(i));
					pList.remove(i);
				}
			}
			break;
		}

		SetResults(settings);
		UpdateControlPanel();
		DoSummaryResults();
	}

	public void DoResultsTableSort() {
		GenericDialog gd = new GenericDialog("Sort Results");
		String[] sortOptions = { "Green Mean", "Red Mean", "ID", "Raw Green Intensity", "Raw Red Intensity",
				"Total Raw", "x", "y" };
		gd.addChoice("Sort By:", sortOptions, sortOptions[3]);
		gd.addCheckbox("Sort Ascending:", true);

		gd.showDialog();
		if (gd.wasCanceled())
			return;

		// Columns - if they change, update this
		int nSortCol = 6;
		String SortOrder = gd.getNextChoice();
		if (SortOrder == "Raw Red Intensity") {
			nSortCol = 4;
		}
		if (SortOrder == "ID") {
			nSortCol = 0;
		}
		if (SortOrder == "x") {
			nSortCol = 2;
		}
		if (SortOrder == "y") {
			nSortCol = 3;
		}
		if (SortOrder == "Green Mean") {
			nSortCol = 11;
		}
		if (SortOrder == "Red Mean") {
			nSortCol = 9;
		}
		if (SortOrder == "Total Raw") {
			nSortCol = 8;
		}

		boolean bAscending = gd.getNextBoolean();
		sorting(nSortCol, bAscending, (ResultsTable2)ResultsTable.getResultsTable());
	}

	private void DoScatterPlot(AnalysisSettings s, boolean bPositivesOnly) {
		if (s.debug > 0) {
			IJ.log("Doing scatter plot");
		}

		int i, n, j, k, nPos = 0, nNeg = 0; // Number of points to be plotted
		// Calculate number of points to plot - i.e. excluding those with one or other
		// color with score of 0
		for (i = 0; i < particles.size(); i++) {
			if (particles.get(i).IsRedPositive()) {
				nPos++;
			} else {
				nNeg++;
			}
		}

		double[] xPos = new double[nPos];
		double[] yPos = new double[nPos];
		double[] yNeg = new double[nNeg];
		double[] xNeg = new double[nNeg];
		List<Double> xRed = new ArrayList<Double>();
		;
		List<Double> yRed = new ArrayList<Double>();
		;
		n = 0;
		j = 0;
		k = 0;
		for (i = 0; i < particles.size(); i++) {
			Particle p = particles.get(i);
			if (p.IsRedPositive()) {
				// If also green, then plot in main pos else plot as red
				if (p.IsGreenPositive()) { // was s.rawgreen[i] > s.greenThreshold
					yPos[n] = p.redval();
					xPos[n++] = p.greenval();
				} else {
					yRed.add(p.redval());
					xRed.add(p.greenval());
				}
			} else {
				yNeg[j] = p.redval();
				xNeg[j++] = p.greenval();
			}
		}

		Plot p = new Plot((bPositivesOnly ? "Postiive" : "All") + " Scatter Plot [" + s.image.getTitle() + "]",
				"Green " + (settings.thresholdMethod == ThresholdMode.THRESHOLD_MEAN ? "Mean " : "")
						+ "Intensity   \u2192",
				"Red " + (settings.thresholdMethod == ThresholdMode.THRESHOLD_MEAN ? "Mean " : "")
						+ "Intensity   \u2192");
		double yMax = s.maxRed * 1.05;
		double xMax = (bPositivesOnly ? s.maxPositiveGreen : s.maxGreen) * 1.1;
		/*
		 * if (bPositivesOnly && s.maxRed > s.maxPositiveGreen) { xMax = s.maxRed * 1.1;
		 * } else if (s.maxRed > s.maxGreen) { xMax = s.maxRed * 1.1; }
		 */
		p.setLimits(0, xMax, 0, yMax);
		p.setColor(Color.orange, Color.orange);
		p.setLineWidth(1);
		p.add("circle", xPos, yPos); // NB we swapped red and green axis

		if (!bPositivesOnly) {
			p.setColor(Color.green, Color.green);
			p.setLineWidth(1);
			p.add("circle", xNeg, yNeg);

			if (xRed.size() > 0) {
				p.setColor(Color.red, Color.red);
				p.setLineWidth(1);
				double[] xReds = new double[xRed.size()];
				double[] yReds = new double[xRed.size()];
				for (i = 0; i < xReds.length; i++) {
					xReds[i] = xRed.get(i).doubleValue(); // java 1.4 style
					yReds[i] = yRed.get(i).doubleValue(); // java 1.4 style
				}
				p.add("circle", xReds, yReds);

			}
		}

		// And we draw dotted lines representing red limit
		p.setColor(Color.red, Color.red);
		p.setLineWidth(2);
		p.drawDottedLine(0, s.redThreshold, xMax, s.redThreshold, 2);
		p.setColor(Color.green, Color.green);
		p.setLineWidth(2);
		p.drawDottedLine(s.greenThreshold, 0, s.greenThreshold, s.maxRed * 1.1, 2);
		// Set limits on axes then show plot
		p.setLimits(0, xMax, 0, yMax);
		p.show();

	}

	/**
	 * Shows the current ROI based on checkboxes for which we want to see
	 * 
	 * @param s Settings
	 */
	public void DoShowROI(AnalysisSettings s) {
		if (s.debug > 0) {
			IJ.log("DoShowROI");
		}
		if (dlg == null) {
			IJ.log("No dialog");
		}
		if (!s.bShowROI) {
			if (s.debug > 0) {
				IJ.log("Hide ROI");
			}
		if (s.overlay != null) {
			if (s.debug > 0) {
				IJ.log("DoSHowROI - overlay is not null");
			}
			s.overlay.clear();
			s.image.setOverlay(s.overlay);
			s.image.repaintWindow();
			s.image.getWindow().toFront();
			return;
		}
		}
		/* Get settings for what to show */
		Vector<Checkbox> cbx = (Vector<Checkbox>) dlg.getCheckboxes();

		boolean bRed = cbx.get(CB_REDONLY).getState();
		boolean bGreen = cbx.get(CB_GREENONLY).getState();
		boolean bBoth = cbx.get(CB_BOTH).getState();
		boolean bEmpty = cbx.get(CB_EMPTY).getState();
		boolean bOverlaps = false;
		// boolean bNoisey = cbx.get(CB_NOISEY).getState();

		if (s.bShowOverlaps) {
			particles.FindOverlaps(settings.pointDiameter);
			bOverlaps = true;
			bRed = bGreen = bBoth = bEmpty = false;
		}

		if (s.debug > 0) {
			IJ.log("Showing " + (bRed ? "red, " : "") + (bGreen ? "green, " : "") + (bBoth ? "Both, " : "")
					+ (bOverlaps ? "overlapped, " : ""));
		}

		// If we have an Overlay, use t hat, else create one
		if (s.image.getOverlay() == null) {
			s.overlay = new Overlay();
		} else {
			s.overlay = s.image.getOverlay();
		}
		// Get rid of all
		s.overlay.clear();
		IJ.log("DoSHowROI - looping through " + particles.size() + " particles");
		particles.FillROI(s.bFillROI);
		// Now loop through all ROI figuring out which ones to add
		for (int i = 0; i < particles.size(); i++) {
			Particle p = particles.get(i);
			if (bOverlaps) {
				if (p.Overlaps()) {
					// IJ.log("Adding " + s.id[i]);
					s.overlay.add(p.roi, String.valueOf(p.id));
				}

			}
			if (bBoth) {
				if (p.IsBoth()) {
					// IJ.log("Adding " + s.id[i]);
					s.overlay.add(p.roi, String.valueOf(p.id));
				}
			}
			if (bEmpty) {
				if (!(p.IsRedPositive() || p.IsGreenPositive())) {
					// IJ.log("Adding " + s.id[i]);
					s.overlay.add(p.roi, String.valueOf(p.id));
				}

			}
			if (bRed) {
				if (p.IsRedPositive() && !p.IsGreenPositive()) {
					// IJ.log("Adding " + s.id[i]);
					s.overlay.add(p.roi, String.valueOf(p.id));
				}

			}
			if (bGreen) {
				if (!p.IsRedPositive() && p.IsGreenPositive()) {
					// IJ.log("Adding " + s.id[i]);
					s.overlay.add(p.roi, String.valueOf(p.id));
				}

			}
			/*
			 * if (bNoisey) { if ((p.rawred >s.redBackground && p.rawred <= s.redThreshold
			 * && p.rawgreen <= s.greenThreshold) || (p.rawgreen > s.greenBackground &&
			 * p.rawgreen <= s.greenThreshold && p.rawred <= s.redThreshold)) { //
			 * IJ.log("Adding " + s.id[i]); s.overlay.add(p.roi, String.valueOf(p.id)); }
			 * 
			 * }
			 */
		}
		/*
		 * s.overlay.drawLabels(true);
		 * 
		 * s.overlay.drawNames(true);
		 */
		s.image.setOverlay(s.overlay);
		s.image.repaintWindow();
		s.image.getWindow().toFront();

		IJ.log("Oveylar ROI count: " + s.overlay.size());
		s.bShowOverlaps = false;
	}

	private void DoSortedIntensityPlot(AnalysisSettings s, boolean bPositivesOnly) {

		// Get indexes for red values sorted in increasing order
		if (s.debug > 0) {
			IJ.log("DoSortedIntensityPlot");
			IJ.log("maxPositive green: " + s.maxPositiveGreen);
		}
		// Get
		Integer[] positions;

		int i, j;

		int n = 0; // Number of points to be plotted
		ParticleList pList = particles;

		if (bPositivesOnly) {
			pList = particles.GetPosRedList();
		} else {
			pList = particles;

		}
		positions = utils.rankPositions(pList.GetArrayFromList("redval"));

		n = pList.size();

		double[] x = new double[n];
		double[] y = new double[n];
		double[] index = new double[n];
		n = 0;

		for (i = 0; i < positions.length; i++) {

			j = positions[i];
			index[n] = n;
			x[n] = pList.get(j).redval();
			y[n++] = pList.get(j).greenval();
		}

		double yMax = pList.Max("green", s.thresholdMethod) * 1.1;
		if (bPositivesOnly && pList.Max("red", s.thresholdMethod) > yMax) {
			yMax = pList.Max("red", s.thresholdMethod) * 1.1;
		}

		else if (pList.Max("red", s.thresholdMethod) > yMax) {
			yMax = pList.Max("red", s.thresholdMethod) * 1.1;
		}

		Plot p = new Plot("Sorted Intensity Plot: " + (bPositivesOnly ? "Positives" : "All"), "n",
				settings.thresholdMethod == ThresholdMode.THRESHOLD_MEAN ? "Mean " : "" + "Intensity");
		p.setLimits(0, n, 0, yMax);
		p.setFont(-1, 14);
		if (bPositivesOnly) {
			p.addText("Capsid Positive and AHA Detection", n / 3, yMax * 0.95);
		} else {
			p.addText("All Capsid and AHA Detection", n / 3, yMax * 0.95);

		}
		p.addText("[" + s.image.getTitle() + "]", n / 3, yMax * 0.88);
		p.setFont(-1, 12);
		p.addText("(sorted by Capsid Intensity)", n / 3, yMax * 0.83);

		p.setLineWidth(2);
		p.setColor(Color.green);
		p.add("cross", index, y);
		p.setLineWidth(1);

		p.add("line", index, y);

		// Now if doing all, show the threshold
		if (!bPositivesOnly) {
			p.setLineWidth(2);
			p.setColor(Color.green);
			p.drawDottedLine(0, s.greenThreshold, pList.size(), s.greenThreshold, 3);
			p.setColor(Color.red);
			p.drawDottedLine(0, s.redThreshold, pList.size(), s.redThreshold, 2);
		}

		p.setColor(Color.red);
		p.setLineWidth(1);

		p.add("line", index, x);

		p.setLineWidth(2);
		p.add("dot", index, x);

		p.show();

	}

	public void DoSummaryResults() {

		this.resultsSummary = new ResultsTable2();

		if (WindowManager.getFrame("Results Summary") != null) {
			IJ.selectWindow("Results Summary");
			IJ.run("Close");
		}

		SetSummary();
		// s.resultsSummary.setHeading(0, "");
		resultsSummary.showRowNumbers(false);
		resultsSummary.show("Results Summary");
		Frame f = WindowManager.getFrame("Results Summary");
		f.setSize(800, 500);

	}

	public void DoUpdateResults() {

		Vector<TextField> values = dlg.getNumericFields(); // values 0 is noise, values 1 is point diameter
		Vector<Checkbox> cbx = dlg.getCheckboxes();
		this.settings.noiseTolerance = Double.parseDouble(values.get(TF_NOISETOLERANCE).getText());

		this.settings.pointDiameter = Double.parseDouble(values.get(TF_POINTDIAMETER).getText());

		Vector<Choice> choices = dlg.getChoices();
		settings.thresholdMethod = ThresholdMode.values()[choices.get(0).getSelectedIndex()];

		this.settings.backgroundDevFactor = Double.parseDouble(values.get(TF_BACKGROUNDDEV).getText());

		if (ThresholdMode.THRESHOLD_MEAN == settings.thresholdMethod) {
			this.settings.redThreshold = (settings.redBackgroundMean
					+ settings.redBackgroundStdDev * settings.backgroundDevFactor);
			this.settings.greenThreshold = (settings.greenBackgroundMean
					+ settings.greenBackgroundStdDev * settings.backgroundDevFactor);
		} else {
			this.settings.redThreshold = (settings.redBackgroundMean * 3.1415926
					* ((settings.pointDiameter / 2.0) * (settings.pointDiameter / 2.0)));
			this.settings.greenThreshold = (settings.greenBackgroundMean * 3.1415926
					* ((settings.pointDiameter / 2.0) * (settings.pointDiameter / 2.0)));
		}

		// And update all results
		SetResults(this.settings);

		DoSummaryResults();
		UpdateControlPanel();

		if (settings.overlay != null) {
			settings.overlay.clear();
		}
	}

	public double[][] DrawDistribution(double[] x, double[] y, Plot p, Color color, AnalysisSettings s) {
		if (s.debug > 0) {
			IJ.log("\nDrawDistribution");
		}
		CurveFitter cf = new CurveFitter(x, y);

		cf.doFit(CurveFitter.GAUSSIAN);

		double[] params = cf.getParams();
		// params[2] = mean, params[3] = Standard deviation
		double mean = params[2], stdDev = params[3];
		if (s.debug > 0) {
			IJ.log("Mean = " + mean + "\nStdDev = " + stdDev);
		}
		// Now loop from -3.5SD to + 3.5 SD in .1 increments = 71 points
		double[] fitX = new double[71];

		double[] fitY = new double[71];
		for (int i = 0; i < fitX.length; i++) {
			fitX[i] = -3.5 + (i * 0.1);
			fitY[i] = CurveFitter.f(CurveFitter.GAUSSIAN, params, mean + (fitX[i] * stdDev));
			if (s.debug > 2) {
				IJ.log(i + ", " + fitX[i] + ", " + fitY[i]);
			}

		}

		p.setColor(color);
		p.add("line", fitX, fitY);

		double[][] data = new double[2][];

		data[0] = fitX;
		data[1] = fitY;

		return data;
	}

	public void DrawDistributions(double[] xGreen, double[] yGreen, double[] xRed, double[] yRed, AnalysisSettings s) {
		if (s.debug > 0) {
			IJ.log("\nDrawDistributions");
		}
		Plot p;

		p = new Plot("Distribution", "Standard Deviation from Mean", "Count of particles");

		double[][] green = DrawDistribution(xGreen, yGreen, p, Color.GREEN, s);

		double[][] red = DrawDistribution(xRed, yRed, p, Color.RED, s);

		p.draw();
		p.show();

		AreaDemo(green, red);

	}

	public void drawParams(Plot p, double[] params, Color paramColor, double xParam) {
		double x = 0.05, y = 0.9, xspace = 0.025, yspace = 0.025;
		if (paramColor != null) {
			p.setColor(paramColor);
		}
		if (xParam >= 0) {
			x = xParam;
		}
		// p.drawNormalizedLine(x - xspace, 1-y - yspace, x + 6 * xspace, 1- y -
		// yspace);
		// p.drawNormalizedLine(x - xspace,1-y - yspace, x - xspace, 1-( y - yspace*5));
		p.setFont(Font.BOLD, -1);
		p.addLabel(x, 1 - y, "Fit Parameters");
		p.addLabel(x + xspace, 1 - (y - yspace), "Offset = " + String.format("%.2f", params[0]));
		p.addLabel(x + xspace, 1 - (y - yspace * 2), "Peak = " + String.format("%.2f", params[1]));
		p.addLabel(x + xspace, 1 - (y - yspace * 3), "\u03bc = " + String.format("%.2f", params[2]));
		p.addLabel(x + xspace, 1 - (y - yspace * 4), "\u03c3 = " + String.format("%.2f", params[3]));

		// p.setColor(color);
	}

	public void DumpParticles(ParticleList particles, String filename, String channel) {
		Writer writer = null;

		try {
			writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filename), "utf-8"));
			for (int i = 0; i < particles.size(); i++) {
				Particle p = particles.get(i);
				p.writePixels(writer, channel);
			}

		} catch (IOException ex) {
			// report
		} finally {
			try {
				writer.close();
			} catch (Exception ex) {
				/* ignore */}
		}
	}

	public ParticleList FindParticles() {

		return GetMaxima();
	}


	int[] GetBackgroundPixels() {
		int pixel;
		if (settings.image.getType() != ImagePlus.COLOR_RGB) {
			IJ.run("RGB Color");
		}
		ColorProcessor proc = (ColorProcessor) settings.image.getProcessor();
		if (proc == null) {

			return new int[0];
		}
		int[] pixels = (int[]) proc.getPixels();

		List<Integer> pxList = new ArrayList<Integer>();
		int width = settings.image.getWidth();

		for (int y = 0; y < settings.image.getHeight(); y++) {
			for (int x = 0; x < width; x++) {
				if (particles.FindRoi(x, y) == null) {
					pixel = pixels[y * width + x]; // proc.getPixel(x, y);
					pxList.add(pixel);
				}
			}
		}
		int[] backgroundPixels = new int[pxList.size()];
		for (int i = 0; i < backgroundPixels.length; i++) {
			backgroundPixels[i] = pxList.get(i);
		}
		IJ.log("GetBackgroundPixels count = " + backgroundPixels.length);
		boolean[] hits = null;
		GetROIHits(hits);
		return backgroundPixels;
	}

	/**
	 * Returns a set of Roi centred on maxima found for the current image
	 * 
	 * @return Array of Roi of current pointDiameter centred at maxima as found
	 *         using current noiseTolerance
	 */
	public ParticleList GetMaxima() {
		ParticleList pList = new ParticleList();
		MaximumFinder mf = new MaximumFinder();
		ImageProcessor ip = settings.image.getProcessor();
		// Get us maxima as Polygon

		RoiManager rm = RoiManager.getRoiManager();
		// Make sure to get rid of any existing ROI

		rm.reset();

		IJ.run("Select None");

		java.awt.Polygon maxima = mf.getMaxima(ip, settings.noiseTolerance, true);

		if (settings.debug > 0) {
			IJ.log("Maxima count=" + maxima.xpoints.length);
		}

		Roi[] rois = new Roi[maxima.xpoints.length];

		// Get rid of anything there already
		// if (settings.debug > 0 ) { IJ.log("Reset ROI"); }

		// Add a circle for each
		double x, y;
		int id = 0;
		for (int i = 0; i < maxima.xpoints.length; i++) {

			x = maxima.xpoints[i]; // * 1.0 - settings.pointDiameter / 2.0;
			y = maxima.ypoints[i]; // * 1.0 - settings.pointDiameter / 2.0;
			// Check not too close too edge
			if ((x - settings.pointDiameter / 2.0 >= 0) && (y - settings.pointDiameter / 2.0 >= 0)) {
				pList.add(new Particle(id + 1, x, y, settings));
				id++;
			}
		}
		IJ.log("Getmaxima - " + rois.length + " rois");
		return pList;
	}

	public int GetROIHits(boolean[] hits) {
		Point[] points;
		int backgroundCount = 0;

		int width = IJ.getImage().getWidth();
		int height = IJ.getImage().getHeight();
		backgroundCount = width * height;
		IJ.log("GETROIHIts all pixel count = " + backgroundCount);
		IJ.log("Width = " + width + ", height = " + height);

		hits = new boolean[width * height];

		for (int i = 0; i < hits.length; i++) {
			hits[i] = false;
		}

		for (int i = 0; i < particles.size(); i++) {
			points = particles.get(i).roi.getContainedPoints();
			for (int k = 0; k < points.length; k++) {
				if (points[k].y * width + points[k].x >= hits.length) {
					IJ.log("Error point " + i + " has x,y = " + points[k].x + " , " + points[k].y);
				} else {
					try {
						if (!hits[points[k].y * width + points[k].x]) {
							backgroundCount--;
						}
						hits[points[k].y * width + points[k].x] = true;
					} catch (Exception e) {
						IJ.log("GetROIHits error = " + e.getMessage());
						IJ.log("Particle = " + i + ", roi x,y =" + points[k].x + ", " + points[k].y);

					}
				}
			}
		}
		IJ.log("GetROIHits background count  = " + backgroundCount);
		return backgroundCount;
	}

	/**
	 * Generates frequency histogram column plot for given red and green counts
	 * using SD from mean as X axis
	 * 
	 * @param xGreen
	 * @param yGreen
	 * @param xRed
	 * @param yRed
	 * @param s
	 */
	public void OverlappedFrequencyChart(double[] green, double[] red, AnalysisSettings s) {

		if (s.debug > 0) {
			IJ.log("\nOverlappedFrequencyChart");
		}

		if (s.debug > 2) {
			IJ.log("values=");
			for (int i = 0; i < green.length; i++) {
				IJ.log(i + ", " + green[i] + "," + red[i]);
			}
		}

		// Get the bucket width then we are going to sum based on centering on the mean
		// - i.e. the bucket around the mean
		// goes from mean - (bucketwidth/2) to mean + (bucketwidth/2)
		double bucketWidth = 2 * utils.iqr(green) / Math.pow(green.length, 1.0 / 3);
		if (s.debug > 0) {
			IJ.log("bucketWidth = " + bucketWidth);
		}

		double mean = utils.Mean(green);
		double StdDev = utils.StdDev(green, mean);
		double redMean = utils.Mean(red);
		double redStdDev = utils.StdDev(red, redMean);

		if (s.debug > 0) {
			IJ.log("green mean = " + mean + ", stdDev = " + StdDev);
		}
		if (s.debug > 0) {
			IJ.log("red mean = " + redMean + ", stdDev = " + redStdDev);
		}

		// work out number of buckets
		double offset = mean % (bucketWidth);
		if (s.debug > 0) {
			IJ.log("Offset = " + offset);
		}

		int nBucketCount = (int) ((mean - bucketWidth / 2) / bucketWidth) * 2 + 1;
		double[] bucket = new double[nBucketCount];
		double[] redBucket = new double[nBucketCount];
		double[] xStdDev = new double[nBucketCount];
		double[] greenBinCounts = new double[nBucketCount];
		double[] redBinCounts = new double[nBucketCount];

		for (int i = 0; i < nBucketCount; i++) {
			greenBinCounts[i] = 0;
			redBinCounts[i] = 0;
			bucket[i] = i * bucketWidth + offset;
			xStdDev[i] = (bucket[i] - mean) / StdDev;
			redBucket[i] = redMean + (xStdDev[i] * redStdDev);
//        	if (s.debug > 2){ IJ.log("" + i + ", " + bucket[i] +", " + xStdDev[i]); }
		}

		// Now do counts using plus or minus the bucket value and half the bucketwidth
		utils.GetFrequencyDistirubtion(bucket, green, greenBinCounts, bucketWidth);
		utils.GetFrequencyDistirubtion(redBucket, red, redBinCounts, bucketWidth);

		if (s.debug > 2) {
			IJ.log("\nBuckets");
		}
		for (int i = 0; i < nBucketCount; i++) {
			if (s.debug > 2) {
				IJ.log("" + i + "," + xStdDev[i] + "," + bucket[i] + "," + greenBinCounts[i] + "," + redBucket[i] + ","
						+ redBinCounts[i]);
			}
		}

		double[][] xyGreen = new double[2][];
		double[][] xyRed = new double[2][];

		xyGreen[0] = xStdDev;
		xyGreen[1] = greenBinCounts;
		xyRed[0] = xStdDev;
		xyRed[1] = redBinCounts;

		ColumnChart(xyGreen, xyRed);

	}

	public void RemoveResult(Particle p) {
		// Find the row with this particle's ID and then remove it from results table
		ResultsTable2 rt = (ResultsTable2)ResultsTable.getResultsTable();
		for (int i = 0; i < rt.getCounter(); i++) {
			if ((int) rt.getValueAsDouble(0, i) == p.id) {
				IJ.log("Deleting row " + i + " from results table");
				rt.deleteRow(i);
				rt.updateResults();
				rt.show("Results");
				return;
			}
		}
		IJ.log("Particle " + p.id + " not found");
	}

	public void roiPopupMenu(Particle p, int x, int y) {
		JPopupMenu menu = new JPopupMenu("ROI Menu");
		PopupActionListener listener = new PopupActionListener(p);

		Font f = menu.getFont().deriveFont((float) 9.0);
		menu.setFont(f);
		menu.add("Remove").addActionListener(listener);
		menu.add("Set as Green Threshold").addActionListener(listener);
		menu.add("Set as Red Threshold").addActionListener(listener);
		menu.add("Set BELOW Green Threshold").addActionListener(listener);
		menu.add("Set BELOW Red Threshold").addActionListener(listener);
		menu.add("Show Info").addActionListener(listener);
		menu.show(settings.canvas, x, y);
	}

	/**
	 * THis is the plugin entry point. imageJ automatically calls this when you run the plugin
	 * We set up a dialog and everything is then controlled from it
	 */
	public void run(String arg) {
		// IJ.setDebugMode(true);
		DoDialog();
	}

	public void setHandlers() {
		final TextWindow window = (TextWindow) WindowManager.getWindow("Results");
		final TextPanel textPanel = window.getTextPanel();
		textPanel.addMouseListener(new MouseListener() {
			@Override
			public void mouseClicked(final MouseEvent e) {
				final int line = textPanel.getSelectionStart();
				if (line < 0) {
					DoResultsTableSort();
					e.consume();
					return;
				}
				IJ.log("Mouse clicked on line " + line);
			}

			@Override
			public void mouseEntered(MouseEvent e) {
				// TODO Auto-generated method stub

			}

			@Override
			public void mouseExited(MouseEvent e) {
				// TODO Auto-generated method stub

			}

			@Override
			public void mousePressed(MouseEvent e) {
				// TODO Auto-generated method stub

			}

			@Override
			public void mouseReleased(MouseEvent e) {
				// TODO Auto-generated method stub

			};

		});

		AddCanvasListener();

	}

	/**
	 * SetResults does all the hard work for showing results, counting particles,
	 * get particle intensities etc.. Clears and re-populates the results table
	 * 
	 * @param s Current settings
	 */
	public void SetResults(AnalysisSettings s) {

		Color roiColor;
		if (s.debug > 0) {
			IJ.log("Set results - " + particles.size() + " particles");
		}

		if (s.debug > 0) {
			IJ.log("Background red: " + s.redBackground + ", green: " + s.greenBackground);
		}
		rt = (ResultsTable2) ResultsTable.getResultsTable();
		s.nRed = 0;
		s.nGreen = 0;
		s.nRedNoise = 0;
		s.nRedOnly = 0;
		s.nGreenNoise = 0;
		s.nGreenOnly = 0;
		s.nBoth = 0;
		s.nNoisey = 0;

		s.nBackground = 0; // Count of empty (below background in both channels) ROI

		// Clear any existing results
		rt.reset();
		for (int i = 0; i < particles.size(); i++) {
			Particle p = particles.get(i);

			rt.incrementCounter();
			rt.addValue("ID", p.id);

			rt.addValue("Status", p.GetStatus());

			rt.addValue("x", p.x);
			rt.addValue("y", p.y);

			rt.addValue("RedRawIntDen", p.rawred);
			rt.addValue("MaxOfPctRed", Math.round(p.redPct * 10) / 10.0);

			rt.addValue("GreenRawIntDen", p.rawgreen);
			rt.addValue("MaxOfPctGreen", Math.round(p.greenPct * 10) / 10.0);

			rt.addValue("TotalRawIntDen", p.rawred + p.rawgreen);

			// If this is positive red (ie above threshold )
			
			  if (p.IsRedPositive()) { 
				  rt.addValue("HasRed", "Yes");
				  rt.addValue("redScore", p.red); 
		      }
			  else { // Not red positive - test if also not green 
				  rt.addValue("HasRed", "No");
				 
				  }
			  
			  // Now do green
			  if (p.IsGreenPositive()) { 
				  rt.addValue("HasGreen", "Yes");
				  rt.addValue("greenScore", p.green); 

			  } else { rt.addValue("HasGreen", "No"); }
			 
			rt.addValue("RedMean", p.redmean);
			rt.addValue("RedStdDev", p.redstdDev);
			// rt.addValue("RedMeanDiff", p.redmean-settings.redBackgroundMean);
			rt.addValue("GreenMean", p.greenmean);
			rt.addValue("GreenStdDev", p.greenstdDev);
			// rt.addValue("GreenMeanDiff", p.greenmean-settings.greenBackgroundMean);
			// s.roi[i] = roi;
			// s.rt.addValue("RedMean", s.redMean[i]);
			// s.rt.addValue("GreenMean", s.greenMean[i]);
		}
		rt.showRowNumbers(false);
		rt.show("Results");

	}



	public void SetSummary() {
		AnalysisSettings s = this.settings;

		int count = particles.size();

		String[] Labels = { "Image Name", "Total Number of maxima found", "Noise Tolerance", "ROI Diameter",
				"ROI Red Average Background", "ROI Green Average Background", "# Red ROI", "# Green ROI",
				"# ROI both Red and Green",
				// "# Noisey Red (between background red and red threshold) ","# Noisey Green",
				"# Empty ROI", "Max Red ROI Intensity", "Max Green ROI Intensity", "Background Red Mean",
				"Background Red StdDev", "Background Green Mean", "Background Green StdDev", "Threshold Method",
				"Red Threshold", "Green Threshold" };

		String[] Values = { s.image.getTitle(), String.valueOf(particles.size()), String.valueOf(s.noiseTolerance),
				String.valueOf(s.pointDiameter), String.valueOf(s.redBackground), String.valueOf(s.greenBackground),
				particles.Count("red") + " (" + String.format("%.1f", particles.Count("red") * 100.0 / count)
						+ "%) of which " + particles.Count("redonly") + " ("
						+ String.format("%.1f", particles.Count("redonly") * 100.0 / count) + "%) are red only",
				particles.Count("green") + " (" + String.format("%.1f", particles.Count("green") * 100.0 / count)
						+ "%) of which " + particles.Count("greenonly") + " ("
						+ String.format("%.1f", particles.Count("greenonly") * 100.0 / count) + "%) are green only",
				particles.Count("both") + " (" + String.format("%.1f", particles.Count("both") * 100.0 / count) + "%)",
				// s.nRedNoise + " (" + (s.nRedNoise * 100.0 / s.count)+ "%)",
				// s.nGreenNoise + " (" + (s.nGreenNoise * 100.0 / s.count) + "%)",
				particles.Count("empty") + " (" + String.format("%.1f", particles.Count("empty") * 100.0 / count)
						+ "%)",
				String.format("%.1f", s.maxRed), String.format("%.1f", s.maxGreen),
				String.format("%.2f", settings.redBackgroundMean), String.format("%.2f", settings.redBackgroundStdDev),
				String.format("%.2f", settings.greenBackgroundMean),
				String.format("%.2f", settings.greenBackgroundStdDev), "" + settings.thresholdMethod,
				String.format("%.2f", settings.redThreshold), String.format("%.2f", settings.greenThreshold) };

		for (int i = 0; i < Labels.length; i++) {
			resultsSummary.incrementCounter();
			resultsSummary.setValue(0, i, Values[i]);
			resultsSummary.addLabel(Labels[i]);
		}
	}

	public void SetThresholdFromParticle(Particle p, String channel, boolean setBelow) {
		if (settings.thresholdMethod == ThresholdMode.THRESHOLD_MEAN) {
			if (channel == "green") {
				settings.greenThreshold = p.greenmean;
				if (setBelow) {
					settings.greenThreshold = p.greenmean * 1.005;
				}
			} else {
				settings.redThreshold = p.redmean;
				if (setBelow) {
					settings.redThreshold = p.redmean * 1.005;
				}
			}
		} else {
			if (channel == "green") {
				settings.greenThreshold = p.rawgreen;
				if (setBelow) {
					settings.greenThreshold = p.rawgreen * 1.005;
				}
			} else {
				settings.redThreshold = p.rawred;
				if (setBelow) {
					settings.redThreshold = p.rawred * 1.005;
				}
			}

		}
		particles.ClassifyParticles(settings);
		SetResults(settings);
		UpdateControlPanel();

		DoShowROI(settings);
		DoSummaryResults();

	}

	private void Setup() {
		// get current weights
		double[] currentWeights = ColorProcessor.getWeightingFactors();

		// Filter on red channel so we can calculate mean background
		ColorProcessor.setWeightingFactors(1, 0, 0);
		// Need to select whole image (or clear selection)
		IJ.run("Select None");
		// Get stats (presume whole image is selected
		ImageStatistics stats = settings.image.getProcessor().getStatistics();
		// Use mean to workout background intensity of circle

		settings.redBackground = stats.mean * (settings.pointDiameter / 2.0) * (settings.pointDiameter / 2.0)
				* 3.1415926;
		settings.redStats = stats;
		// s.redThreshold = s.redBackground * 1.1;
		settings.redThreshold = settings.redBackground * (1.0 + (255.0 / stats.max) * 0.1); // Default threshold

		if (settings.debug > 0) {
			IJ.log("red Mean, mode, median=" + stats.mean + ", " + stats.mode + ", " + stats.median);
			IJ.log("red SD=" + stats.stdDev);
			IJ.log("red max,min=" + stats.max + " , " + stats.min);
			IJ.log("red Background=" + settings.redBackground);
			IJ.log("red Threshold=" + settings.redThreshold);
		}

		// repeat for green
		if (settings.bGreyscale) {
			if (settings.debug > 0) {
				IJ.log("Setting greyscale weightings");
			}
			ColorProcessor.setWeightingFactors(1 / 3, 1 / 3, 1 / 3);
		} else {
			ColorProcessor.setWeightingFactors(0, 1, 0);
		}
		stats = settings.image.getProcessor().getStatistics();
		settings.greenStats = stats;
		settings.greenBackground = stats.mean * (settings.pointDiameter / 2.0) * (settings.pointDiameter / 2.0)
				* 3.1415926;
		// s.greenThreshold = s.greenBackground * 1.1;
		settings.greenThreshold = settings.greenBackground * (1.0 + (255.0 / stats.max) * 0.1);

		if (settings.debug > 0) {
			IJ.log("green Mean, mode, median=" + stats.mean + ", " + stats.mode + ", " + stats.median);
			IJ.log("green SD=" + stats.stdDev);
			IJ.log("green max,min=" + stats.max + " , " + stats.min);
			IJ.log("green Background=" + settings.greenBackground);
			IJ.log("green Threshold=" + settings.greenThreshold);
		}
		// Put settings back
		ColorProcessor.setWeightingFactors(currentWeights[0], currentWeights[1], currentWeights[2]);

		// Test my mean and SD
		int[] pixels = (int[]) settings.image.getProcessor().getPixels();
		IJ.log("My Red mean and SD: " + utils.Mean(pixels, "red") + ", " + utils.StdDev(pixels, "red"));
		IJ.log("My Green mean and SD: " + utils.Mean(pixels, "green") + ", " + utils.StdDev(pixels, "green"));

	}

	
	public void ShowAllChannels() {
		ImagePlus imp = IJ.getImage();
        ImageCanvas ic = imp.getCanvas();
        int curHeight = ic.getHeight(), curWidth = ic.getWidth();
		double curMag = ic.getMagnification();
		int winWidth = settings.win.getWidth(), winHeight=settings.win.getHeight();
	
 		if (settings.debug > 0) {
			IJ.log("ShowAllChannels - image details before");
			utils.LogWindowDetails(settings);
        }

		IJ.run("Make Composite");

		settings.image = IJ.getImage();
		settings.image.setDisplayMode(IJ.COLOR);
		ImageStack stack = settings.image.getImageStack();
		IJ.log(stack == null ? "Stack null" : "Stack not null");
		if (stack != null) {
			IJ.log("Stack size: " + settings.image.getImageStackSize());
		}

		// settings.image.setActiveChannels("111");
		settings.win = settings.image.getWindow();
		settings.canvas = settings.image.getCanvas();
		if (settings.debug > 0) {
			IJ.log("Setting canvas mag to "+curMag+"; size= "+curWidth+","+curHeight);
		}
		settings.win.setSize(winWidth, winHeight);
		settings.canvas.setSize(curWidth, curHeight);
		settings.canvas.setMagnification(curMag);
		settings.canvas.zoomIn(0, 0);
		// settings.canvas.setSize(width, height);

		AddCanvasListener();
		
 		if (settings.debug > 0) {
			IJ.log("ShowAllChannels - image details after");
			utils.LogWindowDetails(settings);
        }

	}

	/**
	 * Create a show the main dialog complete with buttons etc and setup handlers
	 * 
	 * @param s Settings for this dialog
	 */
	private void ShowDialog(AnalysisSettings s) {

		NonBlockingGenericDialog cp = new NonBlockingGenericDialog("Capsid Analysis" + sVersion);
		cp.hideCancelButton();
		cp.setOKLabel("Done");
		// Noise tolerance slider
		cp.addSlider("Noise tolerance:", 5.0, 20.0, 10);
		cp.addSlider("ROI Diameter:", 1.0, 30.0, 5);

		cp.addCheckbox("Greyscale image (treated as green)", false);

		Button bt = new Button("Find Maxima and generate ROI");
		bt.addActionListener(this);

		Panel pnl = new Panel();
		pnl.add(bt);
		cp.addPanel(pnl);

		// Panel for background limits

		cp.addMessage("__________________________________________________________________________");

		cp.addChoice("Threshold method:",
				new String[] { "Use area based threshold compared to ROI area", "Compare background mean to ROI mean" },
				"Compare background mean to ROI mean");
		cp.addSlider("Std Devs / Background factor(1=100%):", 0.0, 6.0, 1);

		cp.addStringField("Red threshold:", "");
		cp.addStringField("Green threshold:", "");

		bt = new Button("Update Results");
		bt.addActionListener(this);

		pnl = new Panel();
		pnl.add(bt);

		cp.addPanel(pnl);

		bt = new Button("Show Overlapping ROI");
		bt.addActionListener(this);
		pnl = new Panel();
		pnl.add(bt);

		bt = new Button("Delete Overlapping ROI");
		bt.addActionListener(this);
		pnl.add(bt);

		cp.addMessage("__________________________________________________________________________");
		cp.addPanel(pnl);

		// Panel for showing ROI
		/*
		 * bt = new Button("Show ROI"); bt.addActionListener(this); pnl = new Panel();
		 * pnl.add(bt);
		 * 
		 * bt = new Button("Hide ROI"); bt.addActionListener(this); pnl.add(bt);
		 */
		/*
		 * bt = new Button("Red Channel"); bt.addActionListener(this); pnl.add(bt);
		 * 
		 * bt = new Button("Green Channel"); bt.addActionListener(this); pnl.add(bt);
		 */
		bt = new Button("Show Channels");
		bt.addActionListener(this);
		pnl.add(bt);

		bt = new Button("Merge Channels");
		bt.addActionListener(this);
		pnl.add(bt);

		bt = new Button("Delete Empty");
		bt.addActionListener(this);
		pnl.add(bt);

		/*
		 * bt = new Button("Delete Noisey"); bt.addActionListener(this); pnl.add(bt);
		 */

		cp.addPanel(pnl);

		// cp.addCheckbox("Fill ROI", false);
		// Checkboxes for show/hide ROI and FIll
		cp.addCheckboxGroup(1, 2, new String[] { "Show ROI", "Fill ROI" }, new boolean[] { false, false });

		cp.addCheckboxGroup(1, 4,
				new String[] { "Red Only (____)", "Green Only (____)", "Both (_____)", "Empty (____)" },
				new boolean[] { true, true, true, true });
		cp.addMessage("__________________________________________________________________________");

		Vector<Checkbox> cbx = cp.getCheckboxes();
		if (cbx != null) {
			IJ.log("Got checkboxes");
			IJ.log("number of checkboxes: " + cbx.size());
			for (int i = 0; i < cbx.size(); i++) {
				Checkbox cb = cbx.get(i);
				IJ.log(i + ": " + cb.getName() + ", " + cb.getLabel() + ", ID=" + cb.getName().replace("checkbox", ""));
				cb.addItemListener(cbxHandler);
			}
		}
		cp.addChoice("Select a plot :", plotList, plotList[0]);

		pnl = new Panel();
		bt = new Button("Show Selected Plot");
		bt.addActionListener(this);
		pnl.add(bt);
		cp.addPanel(pnl);

		/*
		 * cp.addCheckboxGroup(2, 3, new String [] {"Scatter Plot (positives)",
		 * "Intensity Plot (positives)", "Frequency Plot (positives)",
		 * "Scatter Plot (all)", "Intensity Plot (all)", "Frequency Plot (all)"}, new
		 * boolean [] {true, true, true,false,false,false});
		 */
		// cp.addSlider("Frequency Plot Bucket width (0=auto):", 0.0, 5000.0,
		// s.bucketWidth);

		cp.doLayout();
		cp.repaint();

		this.dlg = cp;
		cp.setAlwaysOnTop(true);
		cp.setUndecorated(false);
		cp.showDialog();
	}

	public void ShowGreenChannel() {
		// Get current magnification
		double magnification = settings.image.getWindow().getCanvas().getMagnification();
		int height = settings.image.getWindow().getHeight();
		int width = settings.image.getWindow().getWidth();
		IJ.run("Make Composite");
		settings.image = IJ.getImage();
		settings.image.setDisplayMode(IJ.COLOR);
		settings.win = settings.image.getWindow();
		settings.canvas = settings.win.getCanvas();
		settings.win.setSize(width, height);
		settings.win.getCanvas().fitToWindow();
		;
		settings.canvas.setMagnification(magnification);

		AddCanvasListener();
	}


	public void ShowRedChannel() {
		// Get current magnification
		double magnification = settings.image.getWindow().getCanvas().getMagnification();
		int height = settings.image.getWindow().getHeight();
		int width = settings.image.getWindow().getWidth();
		IJ.run("Make Composite");
		settings.image = IJ.getImage();
		// settings.image.setActiveChannels("100");
		settings.image.setDisplayMode(IJ.COLOR);
		settings.win = settings.image.getWindow();
		settings.canvas = settings.win.getCanvas();

		settings.win.setSize(width, height);
		settings.win.getCanvas().fitToWindow();
		;
		settings.canvas.setMagnification(magnification);
		AddCanvasListener();
	}

	public void ShowRGB() {
		IJ.run("RGB Color");
		settings.image = IJ.getImage();

		settings.win = settings.image.getWindow();
		settings.canvas = settings.win.getCanvas();
		AddCanvasListener();
	}

	public void sorting(int nSortingColumn, boolean bAscending, ResultsTable2 rt) {
		IJ.log("Sorting Results Table: Preparation...");

		int[] stringCols = { 1 };
		int colNumber;
		String cellValue;
		int len;
		int i, j;

		colNumber = rt.getLastColumn() + 1;
		// IJ.log("colNumber=" + colNumber);
		float[] s = rt.getColumn(0);
		len = s.length;
		// IJ.log("length=" + len);
		String[][] data = new String[len][colNumber];
		for (i = 0; i < len; i++) {
			for (j = 0; j < colNumber; j++) {
				data[i][j] = "";
			}
		}

		IJ.showStatus("Sorting Results Table: Preparation...");
		if (settings.debug > 0) {
			IJ.log("Sorting Results Table: Preparation...");
		}
		for (i = 0; i < colNumber; i++) {
			// IJ.log("Preparing column " + i);
			for (j = 0; j < len; j++) {
				// IJ.log("Getting row " + j);
				cellValue = (i == 1) ? rt.getStringValue(i, j) : String.format("%.4f", rt.getValueAsDouble(i, j));
				if (j == 0) {
					// IJ.log("typeof row,col " + j + ", " + i + " is" + (typeof cellValue));
					// IJ.log("Setting " + j + ", " + i + " to " + cellValue);
				}
				data[j][i] = cellValue;
			}
		}

		IJ.showStatus("Sorting Results Table: Sorting...");
		if (settings.debug > 0) {
			IJ.log("Sorting Results Table: Sorting...");
		}

		Arrays.sort(data, new tableComparator(nSortingColumn, bAscending, stringCols));

		IJ.showStatus("Sorting Results Table: Updating Table...");
		if (settings.debug > 0) {
			IJ.log("Sorting Results Table: Updating Table...");
		}
		for (i = 0; i < colNumber; i++) {
			// IJ.log("Updating column " + i);
			for (j = 0; j < len; j++) {
				if (i == 1)
					rt.setValue(i, j, data[j][i]);
				else
					rt.setValue(i, j, Double.parseDouble(data[j][i]));
			}
		}
		if (settings.debug > 0) {
			IJ.log("Updating results");
		}
		rt.show("Results");
		IJ.showStatus("Results sort complete");
		// rt.getResultsWindow().getTextPanel().scrollToTop();
		TextPanel rtp = IJ.getTextPanel();
		if (rtp != null) {
			rtp.scrollToTop();
		} else {
			IJ.log("Could not get tesults text panel");
		}
	}

	public void UpdateControlPanel() {
		Vector<Checkbox> cbx = dlg.getCheckboxes();
		int[] idx = { CB_REDONLY, CB_GREENONLY, CB_BOTH, CB_EMPTY };

		String[] labels = { "Red Only (" + particles.Count("redonly") + ")",
				"Green Only (" + particles.Count("greenonly") + ")", "Both (" + particles.Count("both") + ")",
				"Empty (" + particles.Count("empty") + ")" };

		for (int i = 0; i < idx.length; i++) {
			cbx.get(idx[i]).setLabel(labels[i]);
		}

		// Set thresholds in dialog
		Vector<TextField> sFields = dlg.getStringFields();
		sFields.get(SF_GREEN).setEditable(false);
		sFields.get(SF_RED).setEditable(false);

		sFields.get(SF_GREEN).setText(String.format("%.2f", this.settings.greenThreshold));
		sFields.get(SF_RED).setText(String.format("%.2f", this.settings.redThreshold));

		/*
		 * // Work out what percentage the threshold is for max of each of red and green
		 * and set doulbe redPct = s.redThreshold * 100.0 / s.maxRed; var greenPct =
		 * s.greenThreshold * 100.0 / s.maxGreen; var sFields = gd.getStringFields(); if
		 * (s.debug) { IJ.log("UpdateControlPanel: " + sFields.length +
		 * " string fields"); }
		 * 
		 * sFields[SF_RED].setText(redPct.toFixed(1));
		 * sFields[SF_GREEN].setText(greenPct.toFixed(1));
		 */
		dlg.doLayout();

	}

}