import javax.swing.JFormattedTextField;

import ij.ImagePlus;
import ij.gui.ImageCanvas;
import ij.gui.ImageWindow;
import ij.gui.Overlay;
import ij.process.ImageStatistics;

public class AnalysisSettings {
	public boolean autoBin = true;
	public double backgroundDevFactor = 1.0;
	public boolean bFillROI = false;
	public boolean bGreyscale = false, bAdjust = false, bSubtract = false, bRemoveOutliers = false;
	public JFormattedTextField binField;
	public boolean bLabelValues = false;
	public boolean bResultsListenerOn = false, bDoPlots = false;
	public boolean bShowOverlaps = false;
	boolean bShowROI = true;
	public double bucketWidth = 0;
	public ImageCanvas canvas, targetCanvas = null;
	public int debug = 0, count = 0;
	public double greenBackgroundMean;
	public double greenBackgroundStdDev;
	public ImageStatistics greenStats;
	public int id[];
	public ImagePlus image;
	double maxPositiveGreen = 0;
	double maxRed, maxGreen;
	public double[] newx, newy, maxDensity;
	public double noiseTolerance = 10.0;
	int nRed, nGreen, nRedNoise, nRedOnly, nGreenNoise, nGreenOnly, nBoth, nNoisey, nBackground;
	Overlay overlay = null;
	public double pointDiameter = 5.0;
	public double redBackground = 0, greenBackground = 0, redThreshold = 0, greenThreshold = 0;
	public double redBackgroundMean;
	public double redBackgroundStdDev;
	public double redPct[], greenPct[];
	public ImageStatistics redStats;
	public double rm, rt;
	ThresholdMode thresholdMethod = ThresholdMode.THRESHOLD_AREA;
	public ImageWindow win;

	public void GetMax(ParticleList pList) {
		this.maxRed = pList.Max("red", this.thresholdMethod);
		this.maxGreen = pList.Max("green", this.thresholdMethod);
		this.maxPositiveGreen = this.maxGreen;

	};
}
