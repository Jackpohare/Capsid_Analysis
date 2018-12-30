
public class PixelStats {

	double redMean, redStdDev;
	double greenMean, greenStdDev;
	int redMax=-1,greenMax=-1;

	
	public PixelStats(int[] pixels) {
	
		GetMeans(pixels);
		GetStdDevs(pixels);

	}
	
	public PixelStats(int[] pixels, AnalysisSettings settings) {
		this(pixels);
		settings.redBackgroundMean = this.redMean;
		settings.redBackgroundStdDev = this.redStdDev;
		settings.greenBackgroundMean = this.greenMean;
		settings.greenBackgroundStdDev = this.greenStdDev;
		
	}
	
	
	private void GetStdDevs(int[] pixels) {
		double redSum = 0, greenSum = 0;
		int n = pixels.length;
		int pxGreen,pxRed;
		for (int p: pixels) {
			pxGreen = (p & 0xff00) >> 8; // presume green
			pxRed =  (p & 0xff0000) >> 16;
			redSum += (pxRed - redMean) * (pxRed - redMean);
			greenSum += (pxGreen - redMean) * (pxGreen - redMean);
		}
		this.greenStdDev =  Math.sqrt(greenSum / (n - 1));
		this.redStdDev =  Math.sqrt(redSum / (n - 1));
	}
	
	private void GetMeans(int[] pixels) {
		double redSum = 0, greenSum=0;
		int n = pixels.length;
		int red,green;
		for (int p: pixels) {
			green = (p & 0xff00) >> 8;
			red = (p & 0xff0000) >> 16;
			greenSum += green; 
			redSum += red;
			if (red > this.redMax) {this.redMax = red;}
			if (green > this.greenMax) {this.greenMax = green; }
		}
		this.redMean = redSum / n;
		this.greenMean = greenSum/n;
	}

}
