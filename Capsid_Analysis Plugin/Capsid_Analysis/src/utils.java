
final public class utils {
	 /** Return standard deviation of given value array (given mean)
	 * @param values = array of values to find stdDev from given mean
	 * @param mean = mean of given array
	 * @return stdDev of given values and given mean
	 */
	static double StdDev(double[] values, double mean){
		  double SUM=0; 
		    int n = values.length; 
		    for (int i=0; i<n; i++) {
		    	 SUM = SUM + (values[i]-mean) * (values[i]-mean); 
		    } 
		    return Math.sqrt(SUM/(n-1)); 
		
	}
	 
	static double StdDev(int[] pixels, String channel) { 
	    double SUM=0; 
	    int n = pixels.length; 
	    double mean = Mean(pixels,channel); 
	    for (int i=0; i<n; i++) {
	    	 int p = (pixels[i]&0xff00)>>8; // presume green
	    	 if (channel=="red"){ p= (pixels[i]&0xff0000)>>16; }

	    	 SUM = SUM + (p-mean) * (p-mean); 
	    } 
	    return Math.sqrt(SUM/(n-1)); 
	}

	/** Get mean of given array
	 * @param values = array of values to return mean of
	 * @return mean of given array
	 */
	static double  Mean(double[] values) { 
	    double mean=0; 
	    int n = values.length; 
	    if (n==0) {return 0.0;}
	    
	    for (int i =0; i<n; i++) { 
	        mean += values[i]; 
	    } 
	    return mean/(double)n; 
	}
	
	static double  Mean(int[] pixels,String channel) { 
	    double mean=0; 
	    int n = pixels.length; 
	    for (int i =0; i<n; i++) { 
	    	 int p = (pixels[i]&0xff00)>>8; // presume green
   	 		if (channel=="red"){ p= (pixels[i]&0xff0000)>>16; }
	        mean += p; 
	    } 
	    return mean/n; 
	}
	

	static public int getIndexOf(double []sortedArray,double value){
		
		for(int i =0;i<sortedArray.length;i++){
			if (sortedArray[i]>value){return i;}
		}
		return sortedArray.length;
	}
	
	static int Max(int[] pixels,String channel){
	    int max=0; 
	    int n = pixels.length; 
	    for (int i =0; i<n; i++) { 
	    	 int p = (pixels[i]&0xff00)>>8; // presume green
   	 		if (channel=="red"){ p= (pixels[i]&0xff0000)>>16; }
   	 		if ( p > max ) {max = p; }

	    } 
	    return max; 
		
	    
	}
	
	static public double Max(double []array){
		double max = 0;
		for (int i=0;i<array.length;i++){
			if (array[i]>max){
				max = array[i];
			}
		}
		return max;
	}

}
