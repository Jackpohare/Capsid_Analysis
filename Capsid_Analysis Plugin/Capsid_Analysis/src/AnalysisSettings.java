import java.util.List;

import ij.ImagePlus;
import ij.gui.ImageCanvas;
import ij.gui.ImageWindow;
import ij.gui.Overlay;
import ij.process.ImageStatistics;

public class AnalysisSettings {
		public double pointDiameter = 5.0;
		public double noiseTolerance= 10.0;
		public double backgroundDevFactor = 1.0;
	    public boolean bFillROI = false;
	    public ImagePlus  image;
	    public ImageWindow win;
	    public ImageCanvas canvas,targetCanvas=null;
	    public double redPct [], greenPct [];
		public int	    id [];
		public double redBackground=0, greenBackground=0, redThreshold=0, greenThreshold=0;
		public double  rm, rt;
		public int  debug= 3, count= 0;
		public boolean bResultsListenerOn= false, bDoPlots= false;
		public double[] newx , newy , maxDensity;
		public boolean bGreyscale = false,bAdjust=false, bSubtract=false, bRemoveOutliers=false;
		public boolean bLabelValues = false;
		public double  bucketWidth= 0; 
		int nRed,nGreen,nRedNoise,nRedOnly,nGreenNoise,nGreenOnly,nBoth,nNoisey,nBackground;
		double maxRed,maxGreen;
		double maxPositiveGreen=0;
		Overlay overlay = null;
		public ImageStatistics redStats;
		public ImageStatistics greenStats;
		public double redBackgroundMean;		public double redBackgroundStdDev; 
		public double greenBackgroundMean;
		public double greenBackgroundStdDev;
		public boolean bShowOverlaps=false;
		ThresholdMode thresholdMethod=ThresholdMode.THRESHOLD_AREA;
			
		public void GetMax(ParticleList pList){
			this.maxRed = pList.Max("red", this.thresholdMethod);
			this.maxGreen = pList.Max("green", this.thresholdMethod);
			this.maxPositiveGreen = this.maxGreen;
			
		};
		}

