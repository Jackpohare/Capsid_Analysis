import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import ij.IJ;
import ij.gui.Roi;
import ij.plugin.frame.RoiManager;
import ij.process.ImageStatistics;

public class ParticleList extends ArrayList<Particle> {
	/**
	 * Enumerate and classify the ROI for all particles in the given list
	 * 
	 * @param particles
	 */
	public void ClassifyParticles(AnalysisSettings settings) {
		Particle.settings = settings;
		// Loop through all particles, testing for positives, removing non positives and
		// colouring ROI appropriately
		for (Particle p : this) {
			p.classify();
		}
	}

	/**
	 * Give list of particles and a type, return a count of particles of the
	 * requested type
	 * 
	 * @param particles List of particles to be counted
	 * @param type      String indicating which type of particles we want count of
	 * @return count of particles of given type
	 */
	public int Count(String type) {
		int count = 0;
		for (int i = 0; i < this.size(); i++) {
			Particle p = this.get(i);
			switch (type) {
			case "red":
				count += p.IsRedPositive() ? 1 : 0;
				break;
			case "redonly":
				count += p.IsRedPositive() && !p.IsGreenPositive() ? 1 : 0;
				break;
			case "green":
				count += p.IsGreenPositive() ? 1 : 0;
				break;
			case "greenonly":
				count += !p.IsRedPositive() && p.IsGreenPositive() ? 1 : 0;
				break;
			case "both":
				count += p.IsRedPositive() && p.IsGreenPositive() ? 1 : 0;
				break;
			case "empty":
				count += !p.IsRedPositive() && !p.IsGreenPositive() ? 1 : 0;
				break;

			}
		}
		return count;
	}

	public ParticleList NewSet(String type) {
		ParticleList pList = new ParticleList();

		for (int i = 0; i < this.size(); i++) {
			Particle p = this.get(i);
			switch (type) {
			case "red":
				if (p.IsRedPositive() ) { pList.add(p); }
				break;
			case "redonly":
				if (p.IsRedPositive() && !p.IsGreenPositive() ) { pList.add(p); }
				break;
			case "green":
				if ( p.IsGreenPositive() ) { pList.add(p); }
				break;
			case "greenonly":
				if ( !p.IsRedPositive() && p.IsGreenPositive() ) { pList.add(p); }
				break;
			case "both":
				if ( p.IsRedPositive() && p.IsGreenPositive() ) { pList.add(p); }
				break;
			case "empty":
				if ( !p.IsRedPositive() && !p.IsGreenPositive() ) { pList.add(p); }
				break;

			}
		}
		return pList;
	}
	
	
	public void FillROI(boolean bFill) {
		for (Particle p : this) {
			p.UpdateROI(bFill);
		}
	}

	public Particle FindFirstOverlap(Particle p, int startIndex, double pointDiameter) {
		for (int i = startIndex; i < this.size(); i++) {
			double thisDist = this.get(i).dist(p.roi);
			if (thisDist < pointDiameter / 2.0) {
				return this.get(i);
			}
		}
		return null;
	}

	public void FindOverlaps(double pointDiameter) {
		Particle p, pOverlap;

		for (int i = 0; i < this.size() - 1; i++) {
			p = this.get(i);
			if (p.Overlaps()) {
				continue;
			}
			if ((pOverlap = this.FindFirstOverlap(p, i + 1, pointDiameter)) != null) {
				p.Overlaps(true);
				pOverlap.Overlaps(true);
			}
		}
	}

	/**
	 * Given an image x,y co-ordinate, return the Roi if co-ordinates is inside one,
	 * else return null
	 * 
	 * @param x Image x
	 * @param y Image y
	 * @return roi containing given point, or none if not contained
	 */
	Particle FindRoi(int x, int y) {

		// Loop through all ROI and see if given point is inside one - return it if so
		for (Particle p : this) {
			if (p.roi.contains(x, y)) {
				return p;
			}
		}

		return null;
	}

	double[] GetArrayFromList(String field) {
		double[] array = new double[this.size()];
		for (int i = 0; i < array.length; i++) {
			Particle p = this.get(i);
			switch (field) {
			case "rawred":
				array[i] = p.rawred;
				break;

			case "rawgreen":
				array[i] = p.rawgreen;
				break;

			case "greenval":
				array[i] = p.greenval();
				break;
			case "redval":
				array[i] = p.redval();
				break;

			}
		}
		return array;
	}

	/**
	 * Enumerates given particles list and returns array of ALL green intensity
	 * values
	 * 
	 * @param pList Given particle list
	 * @return array of green intensity values for all given particvles
	 */
	double[] GetGreen() {
		return GetGreen(false);
	}

	/**
	 * Return array of green intensity values for either all or just green positive
	 * particles in the given list
	 * 
	 * @param pList List of particles
	 * @param bAll  Flag to indicate whether to get all green values (true) or just
	 *              green positive particles (false)
	 * @return Returns array of green intensity values for selected set of particles
	 */
	double[] GetGreen(boolean bAll) {
		ParticleList posList = (ParticleList) new ArrayList<Particle>();
		for (int i = 0; i < this.size(); i++) {
			Particle p = this.get(i);
			if (p.IsGreenPositive() || bAll) {
				posList.add(p);
			}
		}

		return posList.GetArrayFromList("greenval");
	} // end GetGreen

	/**
	 * Get the current max ID+1 from the given particle list
	 * 
	 * @return returns 1 beyond the current max ID - ie the ID of the NEXT particle
	 *         that would be added
	 */
	public int GetMaxID() {
		int id = 0;
		if (this.size() == 0)
			return 0;
		for (int i = 0; i < this.size(); i++) {
			Particle p = this.get(i);
			if (p.id > id) {
				id = p.id;
			}
		}
		return id + 1;
	}

	/**
	 * @param r
	 * @param rois
	 * @return
	 */
	public int GetNearestOverlap(Roi r, double pointDiameter) {
		// first find index of nearest - so we need to loop through all (non null) Roi
		int index = -1;
		double distance = pointDiameter;
		for (int i = 0; i < this.size(); i++) {
			double thisDist = this.get(i).dist(r);
			if (thisDist < pointDiameter / 2.0 && thisDist < distance) {
				index = i;
				distance = thisDist;
			}
		}
		return index;
	}

	double[] GetNegGreen() {
		ParticleList posList = (ParticleList) new ArrayList<Particle>();
		for (int i = 0; i < this.size(); i++) {
			Particle p = this.get(i);
			if (!p.IsGreenPositive()) {
				posList.add(p);
			}
		}

		return GetArrayFromList("greenval");
	}

	double[] GetNegRed() {
		ParticleList posList = (ParticleList) new ArrayList<Particle>();
		for (int i = 0; i < this.size(); i++) {
			Particle p = this.get(i);
			if (!p.IsRedPositive()) {
				posList.add(p);
			}
		}

		return GetArrayFromList("redval");
	}

	double[] GetPosGreen() {
		ParticleList posList = (ParticleList) new ArrayList<Particle>();
		for (int i = 0; i < this.size(); i++) {
			Particle p = this.get(i);
			if (p.IsRedPositive()) {
				posList.add(p);
			}
		}

		return GetArrayFromList("greenval");
	}

	double[] GetPosRed() {

		return GetPosRedList().GetArrayFromList("redval");
	}

	public ParticleList GetPosRedList() {
		ParticleList posList = (ParticleList) new ArrayList<Particle>();
		for (int i = 0; i < this.size(); i++) {
			Particle p = this.get(i);
			if (p.IsRedPositive()) {
				posList.add(p);
			}
		}

		return posList;
	}

	/**
	 * Return array of red intensity values for either all or just red positive
	 * particles in the given list
	 * 
	 * @param pList List of particles
	 * @param bAll  Flag to indicate whether to get all red values (true) or just
	 *              red positive particles (false)
	 * @return Returns array of red intensity values for selected set of particles
	 */
	double[] GetRed(boolean bAll) {
		ParticleList posList = (ParticleList) new ArrayList<Particle>();
		for (int i = 0; i < this.size(); i++) {
			Particle p = this.get(i);
			if (p.IsRedPositive() || bAll) {
				posList.add(p);
			}
		}

		return posList.GetArrayFromList("redval");
	}

	public double Max(String channel, ThresholdMode thresholdMethod) {
		double max = 0;
		for (int i = 0; i < this.size(); i++) {
			Particle p = this.get(i);
			if (channel == "green") {
				if (thresholdMethod == ThresholdMode.THRESHOLD_MEAN) {
					max = p.greenmean > max ? p.greenmean : max;
				} else {
					max = p.rawgreen > max ? p.rawgreen : max;
				}
			} else {
				if (thresholdMethod == ThresholdMode.THRESHOLD_MEAN) {
					max = p.redmean > max ? p.redmean : max;
				} else {
					max = p.rawred > max ? p.rawred : max;
				}
			}
		}
		return max;
	}

	public ParticleList Merge(ParticleList green, AnalysisSettings settings) {
		IJ.log("Merge " + this.size() + " red and " + green.size() + " green");
		ParticleList roiList = (ParticleList) new ArrayList<Particle>();

		// Loop through all reds, looking for overlapping green and create a new merged
		// Roi which gets added to list
		for (int i = 0; i < this.size(); i++) {
			int index = green.GetNearestOverlap(this.get(i).roi, settings.pointDiameter);
			if (index >= 0) {
				// SHould do something here about adjusting centre
				this.get(i).roi.setStrokeColor(Color.orange);
				if (settings.bFillROI) {
					this.get(i).roi.setFillColor(Color.orange);
				}
				roiList.add(this.get(i));

				green.remove(index);
			} else {
				this.get(i).roi.setStrokeColor(Color.red);
				if (settings.bFillROI) {
					this.get(i).roi.setFillColor(Color.red);
				}
				roiList.add(this.get(i));
			}
		}

		// Loop through all remaining greens and added to list
		for (int i = 0; i < green.size(); i++) {
			green.get(i).roi.setStrokeColor(Color.green);
			if (settings.bFillROI) {
				this.get(i).roi.setFillColor(Color.green);
			}
			roiList.add(green.get(i));
		}

		return roiList;
	}

	/**
	 * Enumerates given particle list working out red and green ranking info and
	 * setting for each particle

	 */
	public void RankParticles() {
		IJ.log("Rank particles");
		// Get all the red particles, sort and set ranking info
		ParticleList pList = this.NewSet("red");
		IJ.log("Red particle count: "+pList.size());
		pList.sort(Collections.reverseOrder(Comparator.comparing(Particle::redval)));
		for (int i = 0; i < pList.size(); i++) {
			pList.get(i).setRedRank(i+1, pList.size()-i-1);
		}
		// Get all the green  particles, sort and set ranking info
		pList = this.NewSet("green");
		IJ.log("Green particle count: "+pList.size());
		pList.sort(Collections.reverseOrder(Comparator.comparing(Particle::greenval)));
		for (int i = 0; i < pList.size(); i++) {
			pList.get(i).setGreenRank(i+1, pList.size()-i-1);
		}

	}

	/**
	 * Enumerates given list of particles calculating & setting intensities and
	 * percentages for each Also uses given list to set maximum intensities in
	 * settings
	 * 
	 * @param pList
	 */
	public void RecalcParticles(AnalysisSettings settings) {
		// First set intensity for each particle, then get max red and green, then set
		// percentages
		this.setIntensities(settings);
		settings.GetMax(this);
		for (int i = 0; i < this.size(); i++) {
			this.get(i).setPct();
		}
	}

	/**
	 * Enumerate given particle list, setting intensity values for each particle
	 * 
	 * @param pList
	 */
	public void setIntensities(AnalysisSettings settings) {
		ImageStatistics stats = this.get(0).roi.getStatistics();
		if (settings.debug > 1) {
			IJ.log("ROI pixel count: " + stats.pixelCount);
		}
		for (int i = 0; i < this.size(); i++) {
			this.get(i).setIntensity();
		}
	}

	/**
	 * Add all Particle's ROIs to the ROI manager, setting each ROI name to match
	 * its ID
	 * 
	 * @param debug Debug level
	 */
	public void SetRoi(int debug) {
		RoiManager rm = RoiManager.getRoiManager();
		for (Particle p : this) {
			if (p == null) {
				IJ.log("!!Particle is null");
			} else {
				p.roi.setName(String.valueOf(p.id));
				if (debug > 3) {
					IJ.log("ROI " + p.id + ": " + p.roi.toString());
				}
				rm.addRoi(p.roi);
			}
		}
	}

	/**
	 * Update the ROI in this list to set the correct colour for each particle's ROI
	 * (and whether it is filled)
	 * 
	 * @param bFill Indicat4s whether or not the ROIs should be filled
	 */
	public void UpdateROI(boolean bFill) {

		for (Particle p : this) {
			p.UpdateROI(bFill);
		}

	}
}
