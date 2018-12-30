
public class ParticleListStats {

	private class Stats {
		double redMean, greenMean,redStdDev,greenStdDev, redSum=0,greenSum=0;
	}
	
	Stats Mean = new Stats();
	Stats Raw = new Stats();
	
	double redMaxMean=-1,greenMaxMean=-1,redMaxRaw=-1,greenMaxRaw=-1;
	ParticleList particles;
	
	public ParticleListStats(ParticleList particles) {
		this.particles = particles;
		GetStats();
	}
	
	private void GetStats() {
		
		for(Particle p: particles) {
			Raw.redSum += p.red();
			Raw.greenSum += p.green();
			Mean.redSum += p.stats.redMean;
			Mean.greenSum += p.stats.greenMean;
			
			if (p.red > redMaxRaw) { redMaxRaw = p.red; }
			if (p.green > greenMaxRaw) { greenMaxRaw = p.green; }
			if (p.stats.redMean > redMaxMean) { redMaxMean = p.stats.redMean;}
			if (p.stats.greenMean > greenMaxMean) { greenMaxMean = p.stats.greenMean;}
		}
		int n = particles.size();
		Mean.redMean = Mean.redSum/n;
		Mean.greenMean = Mean.greenSum/n;
		Raw.redMean = Raw.redSum/n;
		Raw.greenMean = Raw.greenSum/n;
		GetStdDevs();
	}
	
	private void GetStdDevs() {
		double redRawSum=0,greenRawSum=0, redMeanSum=0,greenMeanSum=0;
		int n = particles.size();
		for(Particle p: particles) {
			redRawSum += (p.rawred - Raw.redMean) * (p.rawred - Raw.redMean);
			greenRawSum += (p.rawgreen - Raw.greenMean) * (p.rawgreen - Raw.greenMean);
			
			redMeanSum += (p.stats.redMean - Mean.redMean) * (p.stats.redMean - Mean.redMean);
			greenMeanSum += (p.stats.greenMean - Mean.greenMean) * (p.stats.greenMean - Mean.greenMean);
		}
		Mean.redStdDev = Math.sqrt(redMeanSum/n);
		Mean.greenStdDev = Math.sqrt(greenMeanSum/n);
		Raw.redStdDev = Math.sqrt(redRawSum/n);
		Raw.greenStdDev = Math.sqrt(greenRawSum/n);
	}
}
