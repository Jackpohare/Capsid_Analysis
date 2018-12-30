import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.geom.Ellipse2D;

import javax.swing.JFrame;
import javax.swing.JPanel;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.ui.Layer;

import ij.IJ;

/**
 * Implements creating a scatter plot in new window frame from a set of particles
 * @author jackp
 *
 */
public class ScatterPlot {

	private ParticleList particles;
	double redThreshold;
	double greenThreshold;
	
	/**
	 * Create the plot using given particle list and thresholds (used for drawing markers)
	 * @param particles
	 * @param redThreshold
	 * @param greenThreshold
	 */
	public ScatterPlot(ParticleList particles, double redThreshold, double greenThreshold) {
		this.particles = particles;
		this.redThreshold = redThreshold;
		this.greenThreshold = greenThreshold;
	}
	
	/**
	 * Create and show the actual plot
	 * @param title Title to use from frame and plot itself
	 * @param bUseMean Whether we are plotting use particle mean or particle raw values
	 */
	public void ShowAllScatterPlot(String title, boolean bUseMean) {
		if (IJ.debugMode) {
			IJ.log("\nAllScatterPlot");
		}
		JFrame frame = new JFrame("Scatter plot (All particles): " + title);
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.add(createScatterPanel(title, bUseMean));
		frame.pack();
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
	}
	

	/**
	 * Creates a scatter plotv  in a new jPanel of red vs green intensity for each Particle
	 * 
	 * @return The new panel containing the created scatter plot and axis
	 */
	private JPanel createScatterPanel(String title, boolean bUseMean) {
		JPanel panel = new JPanel(new BorderLayout());

		String intensity = (bUseMean ? "Mean" : "Raw ") + " intensity";
		
		JFreeChart chart = ChartFactory.createScatterPlot(title, "Green " + intensity,
				"Red " + intensity, particles.GetAllXY(!bUseMean));
		
		chart.setTitle(new org.jfree.chart.title.TextTitle(title,
				new java.awt.Font("SansSerif", java.awt.Font.BOLD, 12)));

		if (IJ.debugMode) {
			IJ.log("Chart created, getting XY plot");
		}
		
		XYPlot plot = chart.getXYPlot();
		
		if (IJ.debugMode) {
			IJ.log("XY plot got, getting renderer");
		}
		XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) plot.getRenderer();

		renderer.setSeriesShape(0, new Ellipse2D.Double(-2d, -2d, 4d, 4d));
		renderer.setSeriesShape(1, new Ellipse2D.Double(-2d, -2d, 4d, 4d));
		renderer.setSeriesShape(2, new Ellipse2D.Double(-2d, -2d, 4d, 4d));
		renderer.setSeriesShape(3, new Ellipse2D.Double(-2d, -2d, 4d, 4d));
		renderer.setSeriesPaint(0, Color.GREEN);
		renderer.setSeriesPaint(1, Color.RED);
		renderer.setSeriesPaint(3, Color.MAGENTA);
		renderer.setSeriesPaint(2, Color.ORANGE);
		plot.setRenderer(0, renderer);

		plot.setBackgroundPaint(Color.white);
		plot.setRangeGridlinePaint(Color.DARK_GRAY);
		plot.setDomainGridlinePaint(Color.DARK_GRAY);
		if (bUseMean) {
		plot.getDomainAxis().setRange(0, 255);
		plot.getRangeAxis().setRange(0, 255);
		}
		ValueMarker redmarker = new ValueMarker(greenThreshold); // position green THreshold line marker on the  X axis
		float[] dash = { 10.0f };
		redmarker.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1.0f, dash, 0.0f));
		redmarker.setPaint(Color.GREEN);

		if (IJ.debugMode) {
			IJ.log("Adding redmarker for green threshold");
		}
		plot.addDomainMarker(redmarker, Layer.FOREGROUND);

		if (IJ.debugMode) {
			IJ.log("Creating marker for red threshold");
		}
		ValueMarker marker = new ValueMarker(redThreshold); // position red THreshold line marker on the axis
		marker.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1.0f, dash, 0.0f));
		marker.setPaint(Color.RED);

		plot.addRangeMarker(marker, Layer.FOREGROUND);

		ChartPanel chartPanel = new ChartPanel(chart) {

			@Override
			public Dimension getPreferredSize() {
				return new Dimension(600, 600);
			}
		};
		chartPanel.setBackground(Color.WHITE);
		panel.add(chartPanel, BorderLayout.CENTER);
		return panel;
	}


}
