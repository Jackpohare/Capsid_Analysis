import java.awt.Color;

import ij.IJ;
import ij.gui.Plot;

public class SortedIntensityPlot {
	/**
	 * 
	 * @param particles
	 * @param bPositivesOnly
	 * @param bUseMean
	 * @param title
	 * @param greenThreshold
	 * @param redThreshold
	 */
	public SortedIntensityPlot(ParticleList particles, boolean bPositivesOnly, boolean bUseMean, String title, double greenThreshold,double redThreshold) {

		// Get indexes for red values sorted in increasing order
		if (IJ.debugMode) {
			IJ.log("DoSortedIntensityPlot - " + (bUseMean?"Mean":"Raw"));
		}
		// Get
		Integer[] positions;

		int i, j;

		int n = 0; // Number of points to be plotted
	
		ParticleList pList;
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

		double yMax = pList.Max("green", bUseMean) * 1.1;
		if (bPositivesOnly && pList.Max("red", bUseMean) > yMax) {
			yMax = pList.Max("red", bUseMean) * 1.1;
		}

		else if (pList.Max("red", bUseMean) > yMax) {
			yMax = pList.Max("red", bUseMean) * 1.1;
		}

		Plot p = new Plot("Sorted Intensity Plot: " + (bPositivesOnly ? "Positives" : "All"), "n",
				(bUseMean ? "Mean " : "Raw") + "Intensity");
		p.setLimits(0, n, 0, yMax);
		p.setFont(-1, 14);
		if (bPositivesOnly) {
			p.addText("Capsid Positive and AHA Detection", n / 3, yMax * 0.95);
		} else {
			p.addText("All Capsid and AHA Detection", n / 3, yMax * 0.95);

		}
		p.addText("[" + title + "]", n / 3, yMax * 0.88);
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
			p.drawDottedLine(0, greenThreshold, pList.size(), greenThreshold, 3);
			p.setColor(Color.red);
			p.drawDottedLine(0, redThreshold, pList.size(), redThreshold, 2);
		}

		p.setColor(Color.red);
		p.setLineWidth(1);

		p.add("line", index, x);

		p.setLineWidth(2);
		p.add("dot", index, x);

		p.show();

	}
}
