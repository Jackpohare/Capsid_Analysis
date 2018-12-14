import java.awt.Color;
import java.awt.Rectangle;
import java.io.IOException;
import java.io.Writer;
import java.util.List;

import ij.IJ;
import ij.gui.OvalRoi;
import ij.gui.Roi;
import ij.process.ImageProcessor;

public class Particle {
	static AnalysisSettings settings;
	Roi roi; // Actual Roi
	double x, y; // Maxima point
	/**
	 * ID of this particle - note that IDs start at 1
	 */
	int id; // id
	double rawred, rawgreen; // Red and green raw intensity
	double red, green; // Red and green score
	double redPct, greenPct;
	double redmean = 0, redstdDev = 0;
	double greenmean = 0, greenstdDev = 0;
	boolean bOverlaps = false;
	int[] pixels = null;
	int redMax, greenMax;
	public int redRank, redBelow, greenRank, greenBelow;

	public Particle(int id, double x, double y, AnalysisSettings s) {
		if (id <= 1) {
			settings = s;
		}
		this.id = id;
		this.x = x;
		this.y = y;
		this.roi = new OvalRoi(x - settings.pointDiameter / 2.0, y - settings.pointDiameter / 2.0,
				settings.pointDiameter * 1.0, settings.pointDiameter * 1.0);

	}

	/**
	 * Set the stroke colour for the ROI of this particle depending on it's type
	 */
	public void classify() {
		if (IsBoth()) {
			roi.setStrokeColor(Color.orange);
		} else if (IsRedPositive()) {
			roi.setStrokeColor(Color.red);
		} else if (IsGreenPositive()) {
			roi.setStrokeColor(Color.green);
		} else {
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

		this.greenmean = utils.Mean(this.pixels, "green");
		this.greenstdDev = utils.StdDev(this.pixels, "green");
		this.redmean = utils.Mean(this.pixels, "red");
		this.redstdDev = utils.StdDev(this.pixels, "red");
		this.redMax = utils.Max(this.pixels, "red");
		this.greenMax = utils.Max(this.pixels, "green");

		return rgb;
	}

	public String GetStatus() {
		if (IsBoth()) {
			return "Both";
		} else if (IsEmpty()) {
			return "Empty";
		} else if (IsRedPositive()) {
			return "Red Only";
		} else if (IsGreenPositive()) {
			return "Green Only";
		}
		return "";
	}

	public double greenval() {
		if (settings.thresholdMethod == ThresholdMode.THRESHOLD_MEAN) {
			return greenmean;
		}
		return rawgreen;
	}

	/**
	 * @return TRUE if particle if BOTH red and green positive, else FALSE
	 */
	public boolean IsBoth() {
		return IsRedPositive() && IsGreenPositive();
	}

	/**
	 * @return TRUE if particle is neither green nor red positive else FALSE
	 *         (particle is red or green or both)
	 */
	public boolean IsEmpty() {
		return !IsRedPositive() && !IsGreenPositive();
	}

	public boolean IsGreenPositive() {
		if (settings.thresholdMethod == ThresholdMode.THRESHOLD_MEAN) {
			return this.greenmean >= settings.greenThreshold;
		}
		return this.rawgreen >= settings.greenThreshold;
	}

	public boolean IsRedPositive() {
		if (settings.thresholdMethod == ThresholdMode.THRESHOLD_MEAN) {
			return this.redmean >= settings.redThreshold;
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
		if (settings.thresholdMethod == ThresholdMode.THRESHOLD_MEAN) {
			return redmean;
		}
		return rawred;
	}

	/**
	 * Sets all the key measures for this ROI: raw red & green, mean & stdDev red &
	 * green Also sets pixels
	 */
	public void setIntensity() {

		double[] rgb = GetRoiIntensity(this.roi);
		this.rawred = rgb[0];
		this.rawgreen = rgb[1];
		this.red = this.rawred - settings.redBackground;
		if (this.red < 0) {
			this.red = 0;
		}
		this.green = this.rawgreen - settings.greenBackground;
		if (this.green < 0) {
			this.green = 0;
		}
	}

	public void setPct() {
		this.redPct = this.redval() * 100.0 / settings.maxRed;
		this.greenPct = this.greenval() * 100.0 / settings.maxGreen;

	}

	/**
	 * @param rank Red rank to be set for this particle
	 */
	public void setRedRank(int rank) {
		this.redRank = rank;

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
				if (bFill) {
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
				if (bFill) {
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
