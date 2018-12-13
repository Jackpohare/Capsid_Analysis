import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class HandleThresholdSlider implements ChangeListener {

		private DistributionPlot _plot = null;

		public HandleThresholdSlider(DistributionPlot plot) {
			this._plot = plot;
		}

		@Override
		public void stateChanged(ChangeEvent e) {
			JSlider slider = (JSlider) e.getSource();
			if (!slider.getValueIsAdjusting()) {
				_plot.setThreshold(slider.getValue());
			}
		}

	}

