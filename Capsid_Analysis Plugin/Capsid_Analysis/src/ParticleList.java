import java.util.ArrayList;
import java.util.List;

import ij.IJ;
import ij.process.ImageStatistics;

public class ParticleList extends ArrayList<Particle>  {
	

	public  void FindOverlaps(double pointDiameter){
		Particle p,pOverlap;

		for(int i=0;i<this.size()-1;i++){
			p=this.get(i);
			if (p.Overlaps()){continue;}
			if((pOverlap=this.FindFirstOverlap(p,i+1, pointDiameter)) != null){
				p.Overlaps(true);
				pOverlap.Overlaps(true);
			}
		}
	}
	
	public  Particle FindFirstOverlap(Particle p, int startIndex, double pointDiameter){
		for(int i=startIndex;i<this.size();i++){
				double thisDist = this.get(i).dist(p.roi); 
				if (thisDist  < pointDiameter/2.0 ){
					return this.get(i);
				}
		}
		return null;
	}
	
	/**
	 * Give list of particles and a type, return a count of particles of the requested type
	 * @param particles List of particles to be counted
	 * @param type String indicating which type of particles we want count of
	 * @return count of particles of given type
	 */
	public int Count(String type){
		int count = 0;
		for(int i=0;i<this.size();i++){
			Particle p = this.get(i);
			switch(type){
			case "red":
				count += p.IsRedPositive() ?1:0;
				break;
			case "redonly":
				count += p.IsRedPositive() && !p.IsGreenPositive()?1:0;
				break;
			case "green":
				count += p.IsGreenPositive()?1:0;
				break;
			case "greenonly":
				count += !p.IsRedPositive() && p.IsGreenPositive()?1:0;
				break;
			case "both":
				count += p.IsRedPositive() && p.IsGreenPositive()?1:0;
				break;
			case "empty":
				count += !p.IsRedPositive() && !p.IsGreenPositive()?1:0;
				break;

			}
		}
		return count;
	}

	public double Max(String channel, ThresholdMode thresholdMethod){
		double max = 0;
		for(int i=0;i<this.size();i++){
			Particle p = this.get(i);
			if(channel=="green"){
				if ( thresholdMethod==ThresholdMode.THRESHOLD_MEAN){
					max =  p.greenmean >max ?p.greenmean:max;
					} else {
						max =  p.rawgreen >max ?p.rawgreen:max;
					}
			} else {
				if ( thresholdMethod== ThresholdMode.THRESHOLD_MEAN){
					max =  p.redmean >max ?p.redmean:max;
					} else {
						max =  p.rawred >max ?p.rawred:max;
					}
			}
		}
		return max;
	}

	double[] GetPosGreen(){
		List<Particle>posList = new ArrayList<Particle>();
		for(int i=0;i<this.size();i++){
			Particle p = this.get(i);
			if (p.IsRedPositive()){
				posList.add(p);
			}
		}

		return GetArrayFromList("greenval");
	}
	
	double[] GetNegGreen(){
		List<Particle>posList = new ArrayList<Particle>();
		for(int i=0;i<this.size();i++){
			Particle p = this.get(i);
			if (!p.IsGreenPositive()){
				posList.add(p);
			}
		}

		return GetArrayFromList("greenval");
	}

	double[] GetNegRed(){
		List<Particle>posList = new ArrayList<Particle>();
		for(int i=0;i<this.size();i++){
			Particle p = this.get(i);
			if (!p.IsRedPositive()){
				posList.add(p);
			}
		}

		return GetArrayFromList("redval");
	}
	
	double[] GetArrayFromList( String field){
		double[] array = new double[this.size()];
		for(int i=0;i<array.length;i++){
			Particle p = this.get(i);
			switch(field){
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
	 * Enumerate and classify the ROI for all particles in the given list
	 * @param particles
	 */
	public void ClassifyParticles(){
		// Loop through all particles, testing for positives, removing non positives and colouring ROI appropriately
		for(int i=0;i<this.size();i++){
			this.get(i).classify();
		}
	}
	
	/**
	 * Enumerate given particle list, setting intensity values for each particle 
	 * @param pList
	 */
	public void setIntensities(AnalysisSettings settings) {
		ImageStatistics stats = this.get(0).roi.getStatistics();
		if (settings.debug>1){IJ.log("ROI pixel count: " + stats.pixelCount); }
		for(int i=0;i<this.size();i++){
			this.get(i).setIntensity();
		}
	}
	
	/**
	 * Enumerates given list of particles calculating & setting intensities and percentages for each
	 * Also uses given list to set maximum intensities in settings
	 * @param pList
	 */
	public void RecalcParticles(AnalysisSettings settings) {
		// First set intensity for each particle, then get max red and green, then set percentages
		this.setIntensities(settings);
		settings.GetMax(this);
		for(int i=0;i<this.size();i++){
			this.get(i).setPct();
		}
	}
}
