import java.awt.Color;
import java.awt.Rectangle;
import java.io.IOException;
import java.io.Writer;

import ij.IJ;
import ij.WindowManager;
import ij.gui.OvalRoi;
import ij.gui.Roi;
import ij.measure.ResultsTable;
import ij.process.ImageProcessor;

public class Particle {
	private static AnalysisSettings settings = null;
	
	
	Roi roi; // Actual Roi
	double x, y; // Maxima point
	/**
	 * ID of this particle - note that IDs start at 1
	 */
	int id; // id
	protected double rawred, rawgreen; // Red and green raw intensity

	double redRawPct, greenRawPct, redMeanPct, greenMeanPct;
	boolean bOverlaps = false;
	int[] pixels = null;
	public int redRank, redBelow, greenRank, greenBelow;
	PixelStats stats=null;
	String status = "Undefined";

	public Particle(int id, double x, double y) {
		if ( settings == null ) {
			throw new RuntimeException("settings unavailable for particle creation."); 
		}
		this.id = id;
		this.x = x;
		this.y = y;
		this.roi = new OvalRoi(x - settings.pointDiameter / 2.0, y - settings.pointDiameter / 2.0,
				settings.pointDiameter * 1.0, settings.pointDiameter * 1.0);
		setIntensity();

	}
	
	static public void setSettings(AnalysisSettings s) {
		settings = s;
	}

	/**
	 * 
	 * @return Raw red intensity of this particle
	 */
	public double red() {
		return red(true);
	}
	
	public double red(boolean getRaw) {
		return getRaw?this.rawred:this.stats.redMean;
	}
	
	/**
	 * 
	 * @return Raw green intensity of this particle
	 */
	public double green() {
		return green(true);
	}
	
	public double green(boolean getRaw) {
		return getRaw?this.rawgreen:this.stats.greenMean;
	}
	/**
	 * Set the stroke colour for the ROI of this particle depending on it's type
	 */
	public void classify() {
		if (IsBoth()) {
			status = "Both";
			roi.setStrokeColor(Color.orange);
		} else if (IsRedPositive()) {
			status = "Red Only";
			roi.setStrokeColor(Color.red);
		} else if (IsGreenPositive()) {
			status = "Green only";
			roi.setStrokeColor(Color.green);
		} else {
			status = "Empty";
			roi.setStrokeColor(Color.magenta);
		}
	}

	/**
	 * Calculate the distance of this particle from the given ROI
	 * 
	 * @param r2 ROI for which we want distance from this particle
	 * @return The distance between this particles ROI and the given ROI
	 */
	public double dist(Roi r2) {
		java.awt.Rectangle p1 = this.roi.getBounds();
		java.awt.Rectangle p2 = r2.getBounds();

		return Math.sqrt(Math.pow(p1.x - p2.x, 2.0) + Math.pow(p1.y - p2.y, 2.0));
	}

	/**
	 * Calculate intensity of each channel in given Roi
	 * 
	 * @param roi
	 * @return double[] giving individual r,g and b channels' intensity
	 */
	double[] GetRoiIntensity(Roi roi) {
		double[] rgb = new double[3];
		rgb[0] = rgb[1] = rgb[2] = 0;

		ImageProcessor mask = roi != null ? roi.getMask() : null;
		if (mask == null) {
			IJ.error("Non-rectangular ROI required");
			return rgb;
		}
		Rectangle r = roi.getBounds();

		// Get number of active pixels in mask
		int nCount = 0;
		for (int y = 0; y < r.height; y++) {
			for (int x = 0; x < r.width; x++) {
				if (mask.getPixel(x, y) != 0) {
					nCount++;
				}
			}
		}
		// IJ.log("Pixel count = " + mask.getPixelCount());
		this.pixels = new int[nCount];
		int i = 0;
		ImageProcessor ip = settings.image.getProcessor();
		ip.setRoi(roi);
		int nPixelCount = 0;
		for (int y = 0; y < r.height; y++) {
			for (int x = 0; x < r.width; x++) {
				if (mask.getPixel(x, y) != 0) {
					int px = ip.getPixel(r.x + x, r.y + y) & 0xffffff;
					this.pixels[i++] = px;
					int blue = px & 0xff;
					int green = (px & 0xff00) >> 8;
					int red = (px & 0xff0000) >> 16;
					if (settings.bGreyscale) {
						red = 0;
						blue = 0;
						green = px;
					}
					// if (i == 0) { IJ.log((r.x + x) + " \t" + (r.y + y) + " \t" + px.toString(16)
					// + " \t" + red + " \t" + green + " \t" + blue); }
					rgb[0] += red;
					rgb[1] += green;
					rgb[2] += blue;
					nPixelCount += 1;
				}
			}
		}

		this.stats = new PixelStats(this.pixels); 
	

		return rgb;
	}

	public String GetStatus() {
	
		return this.status;
	}

	public double greenval() {

		return greenval(settings.thresholdMethod != ThresholdMode.THRESHOLD_MEAN);
	}


	public double greenval(boolean bRaw) {
		return bRaw?rawgreen:this.stats.greenMean;

	}
	/**
	 * @return TRUE if particle if BOTH red and green positive, else FALSE
	 */
	protected boolean IsBoth() {
		return IsRedPositive() && IsGreenPositive();
	}

	/**
	 * @return TRUE if particle is neither green nor red positive else FALSE
	 *         (particle is red or green or both)
	 */
	protected boolean IsEmpty() {
		return this.status=="Empty";
	}

	protected boolean IsGreenPositive() {
		if (settings.thresholdMethod == ThresholdMode.THRESHOLD_MEAN) {
			return this.stats.greenMean >= settings.greenThreshold;
		}
		return this.rawgreen >= settings.greenThreshold;
	}

	protected boolean IsRedPositive() {
		if (settings.thresholdMethod == ThresholdMode.THRESHOLD_MEAN) {
			return this.stats.redMean >= settings.redThreshold;
		}
		return this.rawred >= settings.redThreshold;
	}

	/**
	 * sets this particle's ROI outline color based on its classification
	 */
	public boolean Overlaps() {
		return this.bOverlaps;
	}

	public void Overlaps(boolean b) {
		this.bOverlaps = b;
	}

	/**
	 * @return Returns either the raw red intensity of this particle or it's mean,
	 *         depending on the current thresholding method
	 */
	public double redval() {
		return redval(settings.thresholdMethod != ThresholdMode.THRESHOLD_MEAN) ;
	}

	public double redval(boolean bRaw) {
		return bRaw?rawred:this.stats.redMean;
	}
	/**
	 * Sets all the key measures for this ROI: raw red & green, mean & stdDev red &
	 * green Also sets pixels
	 */
	private void setIntensity() {

		double[] rgb = GetRoiIntensity(this.roi);
		this.rawred = rgb[0];
		this.rawgreen = rgb[1];

	}

	public void setPct() {
		this.redRawPct = this.rawred*100.0 / settings.maxRawRed;
		this.greenRawPct = this.rawgreen* 100.0 / settings.maxRawGreen;
		this.redMeanPct = this.stats.redMean*100.0 / settings.maxMeanRed;
		this.greenMeanPct = this.stats.greenMean* 100.0 / settings.maxMeanGreen;

	}

	/**
	 * @param rank Red rank to be set for this particle
	 * @param below Number of red particles below this one
	 */
	public void setRedRank(int rank, int below) {
		this.redRank = rank;
		this.redBelow = below;

	}

	/**
	 * @param rank Green rank to be set for this particle
	 * @param below Number of green particles below this one
	 */
	public void setGreenRank(int rank, int below) {
		this.greenRank = rank;
		this.greenBelow = below;

	}
	public void ShowInfo() {
		ResultsTable rt = new ResultsTable();
		/* if (WindowManager.getWindow("ROI Info") !=null ) {
			 WindowManager.getWindow("ROI Info").dispose();
		 } */


		String[] Labels = { "ID", "Status", "x", "y", 
				"Red Intensity", "Red Mean", "Red StdDev", 
				"% of max red",
				"% of red threshold",
				"Red Ranking", "#Reds below this", 
				"Green Intensity", "Green Mean", "Green StdDev", "% of max green",
				"% of green threshold",
				"Green Ranking", "#Greens below this" };

		String[] Values = { "" + id, GetStatus(), String.valueOf(x), String.valueOf(y), String.format("%.0f", rawred),
				String.format("%.1f", this.stats.redMean), String.format("%.1f", this.stats.redStdDev), String.format("%.1f", redPct),
				String.format("%.1f", redval()/settings.redThreshold*100.0),
				"" + redRank, "" + redBelow, String.format("%.0f", rawgreen), String.format("%.1f", this.stats.greenMean),
				String.format("%.1f", this.stats.greenStdDev), String.format("%.1f", greenPct), 
				String.format("%.1f", greenval()/settings.greenThreshold*100.0),
				"" + greenRank, "" + greenBelow, };
		String[] background = { "", "", "", "", 
				String.format("%.0f", settings.redBackground),String.format("%.1f", settings.redBackgroundMean), String.format("%.1f", settings.redBackgroundStdDev),
				"", "", "", "",
				String.format("%.0f", settings.greenBackground),
				String.format("%.1f", settings.greenBackgroundMean),
				String.format("%.1f", settings.greenBackgroundStdDev), "", "", "","" };

		for (int i = 0; i < Labels.length; i++) {
			rt.incrementCounter();
			rt.addValue("ROI", Values[i]);
			rt.addValue("Background", background[i]);
			rt.addLabel(Labels[i]);
		}

		rt.showRowNumbers(false);
		rt.show("ROI Info");
		WindowManager.getFrame("ROI Info").setSize(400, 600);
		WindowManager.getFrame("ROI Info").toFront();
	}

	public void UpdateROI(boolean bFill) {
		// If this is positive red (ie above threshold )
		if (IsRedPositive()) {
			// Orange ROI if filling and green above background
			if (IsGreenPositive()) {
				
				roi.setFillColor(bFill ? new Color(255, 140, 0) : null);
				if (!bFill) {
					roi.setStrokeColor(new Color(255, 140, 0));
				
				}
				
			} else { // Red with raw green not above greenbackground so this is red only
				roi.setFillColor(bFill ? new Color(255, 0, 0) : null);
				if (!bFill) {
					roi.setStrokeColor(new Color(255, 0, 0));
				}
			} // Otherwise check if raw is above bacground (but is not full red)
		} else { // Not red positive - test if also not green
					// EMpty - we never fill
			if (!IsGreenPositive()) {
				roi.setStrokeColor(Color.magenta);
			}
		}

		// Now do green
		if (IsGreenPositive()) {
			if (!IsRedPositive()) {
				roi.setFillColor(bFill ? new Color(0, 255, 0) : null);
				if (!bFill) {
					roi.setStrokeColor(new Color(0, 255, 0));
				}
			}
		}

	}

	public void writePixels(Writer writer, String channel) throws IOException {
		writer.write(this.id + ",");
		for (int i = 0; i < pixels.length; i++) {
			int px = pixels[i];
			if (channel == "red") {
				px = (px & 0xff0000) >> 16;
			}
			if (channel == "green") {
				px = (px & 0xff00) >> 8;
			}
			writer.write(px + (i < pixels.length - 1 ? "," : "\n"));
		}
	}

}
