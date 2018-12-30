import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSlider;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;

public class FrequencyPlot {
ParticleList particles;
double threshold;
String key;
Color color;
AnalysisSettings settings;

	public FrequencyPlot(ParticleList particles, String key, Color color, double threshold, AnalysisSettings
			settings) {
		this.particles =particles;
		this.threshold=threshold;
		this.settings =settings;
		this.key = key;
		this.color = color;
	}
	
	public void NewFrequencyPlot(String key, Color color, double[] values, double threshold) {
		DistributionPlot dPlot = new DistributionPlot(settings, key, color, values, threshold, true);
		JFrame frame = new JFrame(key);
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		// frame.setLayout(new GridLayout(0, 1));
		JPanel panel = new JPanel(new BorderLayout());
		JSlider thresholdSlider = new JSlider(0, 255, (int) threshold) {

			public String getToolTipText(MouseEvent e) {

				return "Threshold: " + this.getValue();
			}

		};
		// Create chart
		JFreeChart chart = new JFreeChart(key, JFreeChart.DEFAULT_TITLE_FONT, dPlot.getPlot(), false);
		ChartPanel chartPanel = new ChartPanel(chart) {

			@Override
			public Dimension getPreferredSize() {
				return new Dimension(800, 600);
			}
		};

		thresholdSlider.setMajorTickSpacing(10);
		thresholdSlider.setMinorTickSpacing(1);
		thresholdSlider.setPaintLabels(true);
		thresholdSlider.setPaintTicks(true);
		thresholdSlider.setToolTipText("Try again");

		thresholdSlider.addChangeListener(new HandleThresholdSlider(dPlot));
		// Create chart and add panel to frame
		panel.setSize(800, 50);
		// panel.add(thresholdSlider);
		// frame.add(panel);
		// chartPanel.add(thresholdSlider);
		// frame.add(chartPanel);
		// frame.getContentPane().setLayout(mgr);;
		JPanel p2 = new JPanel();
		p2.setLayout(new BorderLayout());
		p2.setBorder(BorderFactory.createEmptyBorder(5, 40, 5, 40));
		p2.add(thresholdSlider);
		frame.getContentPane().add(chartPanel, BorderLayout.CENTER);
		frame.getContentPane().add(p2, BorderLayout.PAGE_START);
		frame.pack();
		frame.setLocationRelativeTo(null);
		frame.validate();
		// SHow frame
		frame.setVisible(true);

	}


}
