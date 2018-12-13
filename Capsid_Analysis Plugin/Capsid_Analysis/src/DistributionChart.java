import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Shape;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYTextAnnotation;
import org.jfree.chart.axis.AxisLocation;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.labels.XYToolTipGenerator;
import org.jfree.chart.plot.CombinedDomainXYPlot;
import org.jfree.chart.plot.DatasetRenderingOrder;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.SeriesRenderingOrder;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYBarPainter;
import org.jfree.chart.renderer.xy.StandardXYItemRenderer;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.DefaultXYDataset;
import org.jfree.data.xy.IntervalXYDataset;
import org.jfree.data.xy.XYBarDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.Layer;
import org.jfree.ui.RectangleAnchor;
import org.jfree.ui.TextAnchor;
import org.jfree.util.ShapeUtilities;


import ij.IJ;
import ij.ImagePlus;
import ij.measure.CurveFitter;

public class DistributionChart {
		
		private static final int MAX = 1;
		public JFreeChart chart = null;
		
		private XYPlot plot = null;
		private double[] red, green;
		private ParticleList _particles;
	    private int nBinCount;
	    private double[] greenBinCounts, redBinCounts, Bins;
	    double maxGreenFrequency= 0,maxRedFrequency=0;
	    double bucketWidth=0;
	    AnalysisSettings settings;
	    double maxGreen = 0,maxRed=0;
	    double _threshold;

		private String _title;
		
		private  final Random rand = new Random();
		
		public DistributionChart(String title,  ParticleList particles, AnalysisSettings s, ChartType type){
			IJ.log("\nDistirubtion Chart Object");
			this.settings =settings;
			_particles = particles;
			_title= title;
		    // Get only positivies (i.e. equal number of red and green as positives have to be both positive green and positive red)
			red = _particles.GetPosRed();
	    	green = _particles.GetPosGreen();
		    settings.GetMax(_particles);
		    
		    // Calculate the bins etc in case doing distributions
		    double GreenBucketWidth=2*utils.iqr(green)/Math.pow(green.length, 1.0/3);
			if (settings.debug > 0){ IJ.log(" Green bucketWidth = "+GreenBucketWidth); }
		    double RedBucketWidth=2*utils.iqr(red)/Math.pow(red.length, 1.0/3);
			if (settings.debug > 0){ IJ.log(" Red bucketWidth = "+RedBucketWidth); }

			this.bucketWidth = GreenBucketWidth;
			
			if ( RedBucketWidth > GreenBucketWidth){
				 this.bucketWidth = RedBucketWidth;
			}
			
		// 	this.bucketWidth =(GreenBucketWidth+RedBucketWidth)/2;
		    // Get maxes 
		    maxGreen = 0;
		    maxRed=0;
		    for (int i = 0; i < green.length; i++) {
		        if (green[i] > maxGreen) { maxGreen = green[i]; }
		    }
		    for (int i = 0; i < red.length; i++) {
		        if (red[i] > maxRed) { maxRed = red[i]; }
		    }
		    // Work out bins based on greenMax - bins go from 0 in binInterval steps to first binInterval above greenMax (.e.g 15 is max & interval is ten then it is 0,10,20)
		    // get top limit
		    // but if top bin would go above 255 then we ensure bins are centred such that 255 is top of last bin

		    double nTopBin = maxGreen - (maxGreen % bucketWidth) + bucketWidth;
		    if(maxRed > maxGreen){
		    	 nTopBin = maxRed - (maxRed % bucketWidth) + bucketWidth;
		    }

		    if (settings.debug > 0) { IJ.log("DistributionChart - top bin: " + nTopBin); }


		    // NUmber of bins
		    nBinCount = (int) Math.floor((nTopBin / bucketWidth) + 1);
		    if (settings.debug > 0) { IJ.log("DistributionChart -  bin count: " + nBinCount); }

			
		    greenBinCounts = new double[nBinCount];
		    redBinCounts = new double[nBinCount];
		    Bins = new double[nBinCount];
		    double offset = 0;
		    // Now if last bin would go beyond 255, we work out an offset to shift everything down such that the top bin will
		    // go from 255-bucketWidth to 255
		    if ( bucketWidth * (nBinCount-1) + (bucketWidth / 2 ) > 256-(bucketWidth/2) ){
		    	offset = 256-(bucketWidth/2) - ( bucketWidth * (nBinCount-1) + (bucketWidth / 2 )  );
		    	if (settings.debug > 0){
		    		IJ.log("Mid Top bin would be " + (bucketWidth * (nBinCount-1) + (bucketWidth / 2 ))+"\nOffset will be " + offset);
		    	}
		    }
		    // Zero counts
		    for (int i = nBinCount-1; i >=0; i--) {
		        greenBinCounts[i] = 0;
		        redBinCounts[i] = 0;
		        Bins[i] = bucketWidth * i + bucketWidth / 2 + offset;
		        if (Bins[i]<0){Bins[i]=0 ;}
		    }

	        utils.GetFrequencyDistirubtion( Bins, green, greenBinCounts,bucketWidth);
	        utils.GetFrequencyDistirubtion( Bins, red, redBinCounts,bucketWidth);
	    	// Get max frequency counts
		    this.maxRedFrequency = utils.Max(redBinCounts);
		    this.maxGreenFrequency = utils.Max(greenBinCounts);

	        if(settings.debug>=3){
	          	IJ.log("\nBin,GreenCOunt,RedCount");
		    	for(int i=0;i<Bins.length;i++){
		    		IJ.log(Bins[i]+","+greenBinCounts[i]+","+redBinCounts[i]);
		    	}
		    	IJ.log("");
	        }
	        
	    	switch(type){
	    	case MULTI:
	    		BuildMultiChart();
	    		break;
	    	case DOUBLE_PLOT:
	    		DoublePlot();
	    		break;
	    	case ALL_SCATTER:
	    		AllScatterPlot();
	    		break;
	    	case ALL_INTENSITY:
	    		SortedIntensityPlot(true);
	    		break;
	    	case POSITIVE_INTENSITY:
	    		SortedIntensityPlot(false);
	    		break;
	    	case GREEN_FREQUENCY:
	    		NewFrequencyPlot("Frequency Plot (Green): ",new Color(0,255,0,128), _particles.GetGreen(true), s.greenThreshold);
	    		break;
	    	case RED_FREQUENCY:
	    		NewFrequencyPlot("Frequency Plot (Red): ",new Color(255,0,0,128), _particles.GetRed(true), s.redThreshold);
	    		//FrequencyPlot("Frequency Plot (Red): ",GetPosRed(_particles), s.redThreshold,new Color(255,0,0,128),GetNegRed(_particles));
	    		break;
	    	default: 
	    		break;
	    	}
		}

		public void setThreshold(int threshold){
			this._threshold = threshold;
			// Get new bins
			
			
			
		}
		
		public void NewFrequencyPlot(String key,Color color, double []values, double threshold){
			DistributionPlot dPlot = new DistributionPlot(settings, key,color, values, threshold,true);
	        JFrame frame = new JFrame(key);
	        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
	        // frame.setLayout(new GridLayout(0, 1));
	        JPanel panel = new JPanel(new BorderLayout());
	        JSlider thresholdSlider = new JSlider(0,255,(int)threshold) {
	        
	        		 public String getToolTipText(MouseEvent e) {
	        			 
	            return "Threshold: "+this.getValue();
	          }
	        	
		};
		
        JFreeChart chart = new JFreeChart(key, JFreeChart.DEFAULT_TITLE_FONT, dPlot.getPlot(), false);
        ChartPanel chartPanel = new ChartPanel(chart) {

	            @Override
	            public Dimension getPreferredSize() {
	                return new Dimension(800, 600);
	            }
	        };

	        
	        thresholdSlider.setMajorTickSpacing(10);
	        thresholdSlider.setMinorTickSpacing(1);
	        thresholdSlider.setPaintLabels(true);
	        thresholdSlider.setPaintTicks(true);
	        thresholdSlider.setToolTipText("Try again");
	        
	        thresholdSlider.addChangeListener(new HandleThresholdSlider(dPlot));
	        // Create chart and add panel to frame 
	        panel.setSize(800, 50);
	      //  panel.add(thresholdSlider);
	        // frame.add(panel);
	       // chartPanel.add(thresholdSlider);
	        //frame.add(chartPanel);
	        // frame.getContentPane().setLayout(mgr);;
	        JPanel p2 = new JPanel();
	        p2.setLayout(new BorderLayout());
	        p2.setBorder(BorderFactory.createEmptyBorder(5,40,5,40));
	        p2.add(thresholdSlider);
	        frame.getContentPane().add(chartPanel,BorderLayout.CENTER);
	        frame.getContentPane().add(p2,BorderLayout.PAGE_START);
	        frame.pack();
	        frame.setLocationRelativeTo(null);
	        frame.validate();
	        // SHow frame
	        frame.setVisible(true);

		}
		
		public void ShowTree(Component c, String s){
			IJ.log(s+"Component "+c.getClass().getName() );
			if (c instanceof Container){
				Container con = (Container) c;
				 IJ.log(s+"Child count: "+con.getComponentCount());
   			   for (int i=0;i<con.getComponentCount();i++){
   				ShowTree(con.getComponent(i),s+" ");
   			   }
			}
		}
			
		public void FrequencyPlot(String title,double [] values,double threshhold,Color color,double[]BelowThresholdValues){
	    	if(settings.debug > 0 ){IJ.log("\nFrequencyPlot");
	    	}
	    	
	    	double []Bins = GetBins(values);
	    	double []Counts = FillBins(Bins,values);
	    	double []CountsBelowThreshold = FillBins(Bins,BelowThresholdValues);
	    	
	        JFrame frame = new JFrame(title+settings.image.getTitle());
	        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
	        // frame.setLayout(new GridLayout(0, 1));
	        JPanel panel = new JPanel(new BorderLayout());
	        JSlider thresholdSlider = new JSlider(0,255,(int)threshhold) {
	        
	        		 public String getToolTipText(MouseEvent e) {
	        			 
	            return "Threshold: "+this.getValue();
	          }
	        	
		};
		
		 	XYPlot plot = new XYPlot();
	        JPanel chartPanel = createPanelFrequencyPlot(title, Bins,Counts,threshhold,color,CountsBelowThreshold,plot);
		 
	        thresholdSlider.setMajorTickSpacing(10);
	        thresholdSlider.setMinorTickSpacing(1);
	        thresholdSlider.setPaintLabels(true);
	        thresholdSlider.setPaintTicks(true);
	        thresholdSlider.setToolTipText("Try again");
	        
	      //  thresholdSlider.addChangeListener(new HandleThresholdSlider(this,plot,color));
	        // Create chart and add panel to frame 
	        panel.setSize(800, 50);
	      //  panel.add(thresholdSlider);
	        // frame.add(panel);
	       // chartPanel.add(thresholdSlider);
	        //frame.add(chartPanel);
	        // frame.getContentPane().setLayout(mgr);;
	        JPanel p2 = new JPanel();
	        p2.setLayout(new BorderLayout());
	        p2.setBorder(BorderFactory.createEmptyBorder(5,40,5,40));
	        p2.add(thresholdSlider);
	        frame.getContentPane().add(chartPanel,BorderLayout.CENTER);
	        frame.getContentPane().add(p2,BorderLayout.PAGE_START);
	        frame.pack();
	        frame.setLocationRelativeTo(null);
	        frame.validate();
	        // SHow frame
	        frame.setVisible(true);
		
			
		}

		public double[]  GetBins(double[] values, double threshold){
			
			double []Bins = null;
			
			return Bins;
		}
		
		public double[] GetBins(double[] values){
			
			if(settings.debug>0){
				IJ.log("\nGetBins on array length "+ values.length);
			}
			double [] Bins;
			
		    // Calculate the bins etc in case doing distributions
		    double BucketWidth=2*utils.iqr(values)/Math.pow(values.length, 1.0/3);
			if (settings.debug > 0){ IJ.log("bucketWidth = "+BucketWidth); }
			
			double maxValue = utils.Max(values);
			
		    // Work out bins based on Max value - bins go from 0 in binInterval steps to first binInterval above greenMax (.e.g 15 is max & interval is ten then it is 0,10,20)
		    // get top limit
		    // but if top bin would go above 255 then we ensure bins are centred such that 255 is top of last bin

		    double nTopBin = maxValue - (maxValue % BucketWidth) + BucketWidth;

		    if (settings.debug > 0) { IJ.log("Top bin: " + nTopBin); }

		    // NUmber of bins
		    int nBinCount = (int) Math.floor((nTopBin / BucketWidth) + 1);
		    if (settings.debug > 0) { IJ.log("DistributionChart -  bin count: " + nBinCount); }
			
		    Bins = new double[nBinCount];
		    double offset = 0;
		    // Now if last bin would go beyond 255, we work out an offset to shift everything down such that the top bin will
		    // go from 255-bucketWidth to 255
		    if ( BucketWidth * (nBinCount-1) + (BucketWidth / 2 ) > 256-(BucketWidth/2) ){
		    	offset = 256-(BucketWidth/2) - ( BucketWidth * (nBinCount-1) + (BucketWidth / 2 )  );
		    	if (settings.debug > 0){
		    		IJ.log("Mid Top bin would be " + (BucketWidth * (nBinCount-1) + (BucketWidth / 2 ))+"\nOffset will be " + offset);
		    	}
		    }
		    // Zero counts
		    for (int i = nBinCount-1; i >=0; i--) {
		        Bins[i] = BucketWidth * i + BucketWidth / 2 + offset;
		        if (Bins[i]<0){Bins[i]=0 ;}
		    }
			
			return Bins;
		}
		
		public double[] FillBins(double[] Bins,double []values){
			if(settings.debug>0){
				IJ.log("\nFillBins\nBin COunt: "+Bins.length+"\n# values:"+values.length);
			}
			double [] BinCounts = new double[Bins.length];
			double BucketWidth = Bins[Bins.length-1] - Bins[Bins.length-2];
			
			utils.GetFrequencyDistirubtion(Bins, values, BinCounts, BucketWidth);
			
			return BinCounts;
		}
		public JPanel createPanelFrequencyPlot(String key,double []Bins,double[]Counts,double threshhold, 
				                                Color color, double[] CountsBelowThreshold,  XYPlot plot){
	    	if(settings.debug > 0 ){
	    		IJ.log("\ncreatePanelFrequencyPlot: "+key);
	    	}
	    	// Get a panel to add the chart into
	        JPanel panel = new JPanel(new BorderLayout());
	        
	   	    // Prepare the dataset from the given Bins and Counts array
    	   //  final DefaultXYDataset xyData = new DefaultXYDataset();
    	    XYSeries primary = utils.XYSeriesFromArrays(key,Bins,Counts);
    	    XYSeries secondary = utils.XYSeriesFromArrays(key,Bins,CountsBelowThreshold);
    	    double [][] xy  = new double [2][];
    	    xy[0] = Bins;
    	    xy[1] = Counts;
    	    if (settings.debug>0){
    	    	IJ.log("\nBins length: "+Bins.length+"\nCOunts length:"+Counts.length);
    	    }
    	    
    	    XYSeriesCollection xyData = new XYSeriesCollection();
    	    XYSeriesCollection xy2Data = new XYSeriesCollection();

    	    xyData.addSeries(primary);
    	    
    	    if (settings.debug>0){
    	    	IJ.log("\nBins length: "+Bins.length+"\nCOuntsBelowThreshold:"+CountsBelowThreshold.length);
    	    }
    	    // final DefaultXYDataset xy2Data = new DefaultXYDataset();
    	    double [][] xy2  = new double [2][];
    	    xy2[0] = Bins;
    	    xy2[1] = CountsBelowThreshold;
    	    xy2Data.addSeries(secondary);
    	    
    	    double BucketWidth = Bins[Bins.length-1] -Bins[Bins.length-2];
    	    XYBarDataset dataset = new XYBarDataset(xyData, BucketWidth*0.9);
       	    XYBarDataset dataset2 = new XYBarDataset(xy2Data, BucketWidth*0.9);

			// Tooltip ?
			 XYToolTipGenerator xyToolTipGenerator = new XYToolTipGenerator()
			 {
			     public String generateToolTip(XYDataset dataset, int series, int item)
			     {
			         Number x1 = dataset.getX(series, item);
			         Number y1 = dataset.getY(series, item);
			         StringBuilder stringBuilder = new StringBuilder();
			         stringBuilder.append(String.format("<html><p style='color:#0000ff;'>%s</p>", dataset.getSeriesKey(series)));
			         stringBuilder.append(String.format("Mean Range:%.1f - %.1f<br/>", x1.intValue()-BucketWidth/2,x1.intValue()+BucketWidth/2));
			         stringBuilder.append(String.format("ROI COunt: '%d'", y1.intValue()));
			         stringBuilder.append("</html>");
			         return stringBuilder.toString();
			     }
			 };
			 
    	    XYBarRenderer renderer= new XYBarRenderer();
    	    renderer.setSeriesPaint(0, color);
    	  //  renderer.setSeriesPaint(1, Color.LIGHT_GRAY);
	        renderer.setDrawBarOutline(false);
	        renderer.setShadowVisible(false);
    	    renderer.setBarPainter( new StandardXYBarPainter());
    	    renderer.setBaseToolTipGenerator(xyToolTipGenerator);
    	    renderer.setMargin(0);
    	    // Create the plot
	        // XYPlot plot = new XYPlot(dataset, new NumberAxis("Mean Intensity"), new NumberAxis("ROI Count"),renderer);

    	    //construct the plot
    	   
    	    plot.setDataset(0, dataset);
    	    // xy2Data.
    	    plot.setDataset(1, dataset2);
    	    plot.setRenderer(0,renderer);
    	    
    	    if(settings.debug>0){
    	    	renderer =     (XYBarRenderer) plot.getRenderer(0);
    	    	IJ.log("\nRenderer 1\nMargin: "+renderer.getMargin());
    	    }
    	    
    	    XYBarRenderer renderer2= new XYBarRenderer();
    	    //renderer2.setSeriesPaint(0, color);
    	    renderer2.setSeriesPaint(0, Color.LIGHT_GRAY);
    	    renderer2.setDrawBarOutline(false);
    	    renderer2.setShadowVisible(false);
    	    renderer2.setBarPainter( new StandardXYBarPainter());
    	    renderer2.setBaseToolTipGenerator(xyToolTipGenerator);

    	    plot.setRenderer(1, renderer2);

    	    plot.setRangeAxis(0, new NumberAxis("Cout of ROI Above Threshold"));
    	    plot.setRangeAxis(1, new NumberAxis("Count of ROI Below Threshold"));
    	    plot.setDomainAxis(new NumberAxis("Mean Intensity"));

    	    //Map the data to the appropriate axis
    	    plot.mapDatasetToRangeAxis(0, 0);
    	    plot.mapDatasetToRangeAxis(1, 1);   
    	    
	     	ValueMarker Marker = new ValueMarker(threshhold);  // position is the value on the axis
	     	float []dash = {3};
			Marker.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1.0f, dash , 0.0f));
			Marker.setPaint(color);
			plot.addDomainMarker(Marker, Layer.FOREGROUND);

			// Now adjust count of ROI below axis - if it is smaller than count of ROI above, we set it the same
			double upper = plot.getRangeAxis(0).getUpperBound();
			if (upper > plot.getRangeAxis(1).getUpperBound() ) {
				plot.getRangeAxis(1).setUpperBound(upper);
			}
			
			// Make sure x axis starts at zero
			  plot.getDomainAxis().setLowerBound(0);
	        JFreeChart chart = new JFreeChart("", JFreeChart.DEFAULT_TITLE_FONT, plot, false);
            
	        ChartPanel chartPanel = new ChartPanel(chart) {

	            @Override
	            public Dimension getPreferredSize() {
	                return new Dimension(800, 600);
	            }
	        };

	        panel.add(chartPanel, BorderLayout.CENTER);
	
	        return panel;
		}
		
		public void SortedIntensityPlot(boolean bAll){
			
	    	if(settings.debug > 0 ){IJ.log("\nSortedIntensityPlot");
	    	}
	    	
	        JFrame frame = new JFrame("Sorted Intensity Plot ("+(bAll?"All":"Positive")+" ROI): "+settings.image.getTitle());
	        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
	        
	        // Get underlying data
	        if (bAll){
	        	
	        } else{
	    	    // Get only positivies (i.e. equal number of red and green as positives have to be both positive green and positive red)
				red = _particles.GetPosRed();
		    	green = _particles.GetPosGreen();
		    	settings.GetMax(_particles);	        }
	        	
	        // Create chart and add panel to frame 
	        frame.add(createPanelSortedIntensityPlot(bAll));
	        frame.pack();
	        frame.setLocationRelativeTo(null);
	        // SHow frame
	        frame.setVisible(true);
		}
		private JPanel createPanelSortedIntensityPlot(boolean bAll) {
	    	if (settings.debug>0){IJ.log("\ncreatePanelSortedIntensityPlot");}
	    	
	    	// Get a panel to add the chart into
	        JPanel panel = new JPanel(new BorderLayout());
	        
	        // Get x and y data sorted by increasing red values
	        Integer[] positions;	// Holds sorted indexes
	        
	        int i, j;

	        int n = 0;	// Number of points to be plotted
	        ParticleList pList = _particles;
	        
	        if(!bAll) {
	        	pList= _particles.GetPosRedList();
	        } else {
	        	pList = _particles;
	        }
	        
	    	positions = rankPositions(pList.GetArrayFromList("redval"));
	      
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

	        // We now have x (red) and y (green) in red sorted order with index as array of indices
	        // Need to get this now as a suitable dataset for the plot
	        final XYSeriesCollection dataset = new XYSeriesCollection();
	        final XYSeries seriesGreen = utils.XYSeriesFromArrays("Green",index,y);
	        final XYSeries seriesRed = utils.XYSeriesFromArrays("Red",index,x);
	        dataset.addSeries(seriesGreen);
	        dataset.addSeries(seriesRed);
	        
	        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();

	        // Set colors, shapes etc
	        renderer.setSeriesPaint(0, Color.GREEN);
	        renderer.setSeriesPaint(1, Color.RED);
	        Shape cross = ShapeUtilities.createDiagonalCross(3f, 0.5f);
	        Shape shape  = new Ellipse2D.Double(-1.5,-1.5,3,3);
	        renderer.setSeriesShape(0, cross);
	        renderer.setSeriesShape(1, shape);
	        renderer.setSeriesStroke(0, new BasicStroke(0.5f));
	        renderer.setSeriesStroke(1, new BasicStroke(0.5f));
	        XYPlot plot = new XYPlot(dataset, new NumberAxis("n"), new NumberAxis("Mean Intensity"), renderer);
	        
	        // If doing all set markers for thresholds
	        // Add a marker in correct color for the threshold
	     	ValueMarker redMarker = new ValueMarker(this.settings.redThreshold);  // position is the value on the axis
			float[] dash = { 5f };
			redMarker.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1.0f, dash , 0.0f));
			redMarker.setPaint(Color.RED);
			plot.addRangeMarker(redMarker, Layer.FOREGROUND);

	     	ValueMarker greenMarker = new ValueMarker(this.settings.greenThreshold);  // position is the value on the axis
			greenMarker.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1.0f, dash , 0.0f));
			greenMarker.setPaint(Color.GREEN);
			plot.addRangeMarker(greenMarker, Layer.FOREGROUND);
			
			plot.setSeriesRenderingOrder(SeriesRenderingOrder.FORWARD);
			
        	JFreeChart chart = new JFreeChart("", JFreeChart.DEFAULT_TITLE_FONT, plot, false);
            
	        ChartPanel chartPanel = new ChartPanel(chart) {

	            @Override
	            public Dimension getPreferredSize() {
	                return new Dimension(800, 400);
	            }
	        };

	        panel.add(chartPanel, BorderLayout.CENTER);

			return panel;
		}


		
		private void addMultilineTextAnnotations(XYPlot plot,double x, double y, String label, Color color) {
	        String[] lines = label.split("\n");
	        for(int i=0; i < lines.length;  i++){
	            XYTextAnnotation annotationLabel = new XYTextAnnotation(lines[i], 
	                    x,  y - (i+1) * XYTextAnnotation.DEFAULT_FONT.getSize());
	            annotationLabel.setPaint(color);
	            plot.addAnnotation(annotationLabel);
	        }
	}
		
		public void AllScatterPlot(){
	    	if(settings.debug > 0 ){IJ.log("\nAllScatterPlot");
    		}
	        JFrame frame = new JFrame("Scatter plot (All particles): "+settings.image.getTitle());
	        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
	        frame.add(createScatterPanel());
	        frame.pack();
	        frame.setLocationRelativeTo(null);
	        frame.setVisible(true);
	    }

		protected void BuildMultiChart(){
		    if (settings.debug > 0) { IJ.log("\nMultiChart - " + settings.image.getTitle());}
		    
		    settings.GetMax(_particles);

		    if (settings.debug > 0) { IJ.log("Max positive green: " + maxGreen + "\nMax positive red: " + maxRed); }

	        // create subplot 1...
		    if (settings.debug > 0) { IJ.log("Create green plot");}

			XYDataset data1 = GetIntervalXYDataset(Bins,greenBinCounts,null);
	        final XYItemRenderer renderer1 = new StandardXYItemRenderer();
	        final NumberAxis rangeAxis1 = new NumberAxis("Range 1");
	        final XYPlot subplot1 = new XYPlot(data1, null, rangeAxis1, renderer1);
	        subplot1.setRangeAxisLocation(AxisLocation.BOTTOM_OR_LEFT);
	        
	        final XYTextAnnotation annotation = new XYTextAnnotation("Hello!", 50.0, 10000.0);
	        annotation.setFont(new Font("SansSerif", Font.PLAIN, 9));
	        annotation.setRotationAngle(Math.PI / 4.0);
	        subplot1.addAnnotation(annotation);
	        
	        // create subplot 2...
		    if (settings.debug > 0) { IJ.log("Create red plot");}
	        final XYDataset data2 = GetIntervalXYDataset(Bins,null,redBinCounts);
	        final XYItemRenderer renderer2 = new StandardXYItemRenderer();
	        final NumberAxis rangeAxis2 = new NumberAxis("Range 2");
	        rangeAxis2.setAutoRangeIncludesZero(false);
	        final XYPlot subplot2 = new XYPlot(data2, null, rangeAxis2, renderer2);
	        subplot2.setRangeAxisLocation(AxisLocation.TOP_OR_LEFT);

	        // parent plot...
	        final CombinedDomainXYPlot plot = new CombinedDomainXYPlot(new NumberAxis("Domain"));
	        plot.setGap(10.0);
	        
	        // add the subplots...
	        plot.add(subplot1, 1);
	        plot.add(subplot2, 1);
	        plot.setOrientation(PlotOrientation.VERTICAL);

	        // return a new chart containing the overlaid plot...
//	        return new JFreeChart("CombinedDomainXYPlot Demo",    JFreeChart.DEFAULT_TITLE_FONT, plot, true);
	        DrawChart(plot);
		}
		
		private double maxYAxis = -1;JFreeChart maxChart = null;

		
	    private  JPanel createPanel(Color color, double[] x,double [] y, double [] values, String key, double maxValue, int series, double lowerBound, double Threshold) {
	    	if (settings.debug>0){IJ.log("createPanel "+key+": maxValue = "+maxValue+", lowerbound ="+lowerBound);}
	    	// Get a panel to add the chart into
	        JPanel p = new JPanel(new BorderLayout());
	        // Get a bar renderer and set it to paint in given color
	        XYBarRenderer renderer = new XYBarRenderer();
	        renderer.setSeriesPaint(0, color);
	        renderer.setDrawBarOutline(false);
	        renderer.setShadowVisible(false);
    	    renderer.setBarPainter( new StandardXYBarPainter());
    	    // Prepare the dataset from the given x and y array
    	    final DefaultXYDataset xyData = new DefaultXYDataset();
    	    double [][] xy  = new double [2][];
    	    xy[0] = x;
    	    xy[1]=y;
    	    xyData.addSeries(key, xy);
    	    XYBarDataset dataset = new XYBarDataset(xyData, this.bucketWidth < 5*0.9?this.bucketWidth*0.9:5);

    	    // Create the plot
	        XYPlot plot = new XYPlot(dataset, new NumberAxis("Mean Intensity"), new NumberAxis("ROI Count"), renderer);
	        // and maker sure Y axis (range) is vertical
	        plot.setOrientation(PlotOrientation.VERTICAL);
	        // Now build data set for fitted data
	        final DefaultXYDataset xyFitData = new DefaultXYDataset();
	        
	        CurveFitter cf = utils.GetFit(x,y,key+" (fit)",xyFitData, settings.debug);
	        // And fitted data to plot
	        plot.setDataset(1, xyFitData);
	        // and get rendered to draw it as a line with no shapes
	        final XYLineAndShapeRenderer renderer2 = new XYLineAndShapeRenderer(true,false);
	        renderer2.setSeriesPaint(series, color);
	        renderer2.setSeriesStroke(series,  new BasicStroke( 3 ));
	        renderer2.setSeriesShape(series, null);
	        renderer2.setSeriesShapesVisible(series,false);
	        plot.setRenderer(1, renderer2);
	        // Get fit parameters and draw observered and fitted results on chart
	        double[] params = cf.getParams();
	        
	        // Add a marker in correct color for the threshold
	     	ValueMarker marker = new ValueMarker(Threshold);  // position is the value on the axis
			float[] dash = { 10f };
			marker.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1.0f, dash , 0.0f));
			marker.setPaint(color);
			plot.addDomainMarker(marker, Layer.FOREGROUND);
			
	        // Offset the X axis (domain) if offset is not zero
	        NumberAxis xAxis = (NumberAxis) plot.getDomainAxis();
        	if (settings.debug>0){ IJ.log("Setting axis from "+lowerBound+" to "+(xAxis.getUpperBound()+lowerBound));}
        	// SHift lowerbound - note that we also shift upperbound to ensure scale stays the same
        	xAxis.setRange(lowerBound,255+lowerBound);

	        utils.AddMarkers(plot,utils.Mean(values),utils.StdDev(values,utils.Mean(values)), params[2], params[3],cf.getFitGoodness());

        	
        	JFreeChart chart = new JFreeChart("", JFreeChart.DEFAULT_TITLE_FONT, plot, false);
	              
	        double YAxisMax = chart.getXYPlot().getRangeAxis().getUpperBound();
	        if (maxChart != null){
	        	if (maxYAxis > YAxisMax){
	        		chart.getXYPlot().getRangeAxis().setUpperBound(maxYAxis);
	        	} else {
	        		maxChart.getXYPlot().getRangeAxis().setUpperBound(YAxisMax);
		        	maxYAxis = YAxisMax;
		        	maxChart = chart;
	        	}
	        	
	        } else {
	        	maxYAxis = YAxisMax;
	        	maxChart = chart;
	        }
	        ChartPanel chartPanel = new ChartPanel(chart) {

	            @Override
	            public Dimension getPreferredSize() {
	                return new Dimension(800, 400);
	            }
	        };

	        p.add(chartPanel, BorderLayout.CENTER);
	        return p;
	    }
	  
	    private JPanel createScatterPanel(){
	        JPanel panel = new JPanel(new BorderLayout());

	        JFreeChart chart = 	ChartFactory.createScatterPlot(settings.image.getTitle(), "Green Mean Intensity", "Red Mean Intensity", GetAllXY());
	        chart.setTitle(
	        		   new org.jfree.chart.title.TextTitle(settings.image.getTitle(),
	        		       new java.awt.Font("SansSerif", java.awt.Font.BOLD, 12)
	        		   ));
	        		
	        if (settings.debug > 0 ) { IJ.log("Chart created, getting XY plot");}
	        XYPlot plot = chart.getXYPlot();
	        if (settings.debug > 0 ) { IJ.log("XY plot got, getting renderer");}
	        XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) plot.getRenderer();
	        
	        renderer.setSeriesShape(0,new Ellipse2D.Double(-2d, -2d, 4d, 4d));
	        renderer.setSeriesShape(1,new Ellipse2D.Double(-2d, -2d, 4d, 4d));
	        renderer.setSeriesShape(2,new Ellipse2D.Double(-2d, -2d, 4d, 4d));
	        renderer.setSeriesShape(3,new Ellipse2D.Double(-2d, -2d, 4d, 4d));
	        renderer.setSeriesPaint(0,Color.GREEN);
	        renderer.setSeriesPaint(1,Color.RED);
	        renderer.setSeriesPaint(3,Color.MAGENTA);
	        renderer.setSeriesPaint(2,Color.ORANGE);
	        plot.setRenderer(0, renderer);
	        
	        plot.setBackgroundPaint(Color.white);
	        plot.setRangeGridlinePaint(Color.DARK_GRAY);
	        plot.setDomainGridlinePaint(Color.DARK_GRAY);
	        plot.getDomainAxis().setRange(0, 255);
	        plot.getRangeAxis().setRange(0, 255);
	    	ValueMarker redmarker = new ValueMarker(settings.greenThreshold);  // position red THreshold line marker on the axis
			float[] dash = { 10.0f };
			redmarker.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1.0f, dash , 0.0f));
			redmarker.setPaint(Color.GREEN);
			
			if (settings.debug > 0 ) { IJ.log("Adding redmarker for green threshold");}
		//	plot.addDomainMarker(redmarker, Layer.FOREGROUND);

			if (settings.debug > 0 ) { IJ.log("Creating marker for red threshold");}
	    	ValueMarker marker = new ValueMarker(settings.redThreshold);  // position red THreshold line marker on the axis
			marker.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1.0f, dash , 0.0f));
			marker.setPaint(Color.RED);
		
			plot.addRangeMarker(marker, Layer.FOREGROUND);

	        ChartPanel chartPanel = new ChartPanel(chart) {

	            @Override
	            public Dimension getPreferredSize() {
	                return new Dimension(600, 600);
	            }
	        };
	        chartPanel.setBackground( Color.WHITE );
	        panel.add(chartPanel, BorderLayout.CENTER);
	        return panel;
	    }



	    private XYSeriesCollection GetAllXY() {
	        XYSeriesCollection result = new XYSeriesCollection();
	        XYSeries series;
	        XYSeries green = new XYSeries("Green Only");
	        XYSeries red = new XYSeries("Red Only");
	        XYSeries both = new XYSeries("Both");
	        XYSeries empty = new XYSeries("Empty");
			for(int i=0;i<_particles.size();i++){
				Particle p = _particles.get(i);
				switch(p.GetStatus()){
				case "Both":
					series = both;
					break;
				case "Red Only":
					series = red;
					break;
				case "Green Only":
					series = green;
					break;
				default:
					series = empty;
					break;
				}
	            series.add(p.greenval(), p.redval());
	        }
			result.addSeries(green);
			result.addSeries(red);			
			result.addSeries(both);			
			result.addSeries(empty);
			return result;
		}


	    
		public void DoublePlot() {
			
			
	    	if(settings.debug > 0 ){IJ.log("\nDoublePlot");
	    		IJ.log("maxGreen = "+this.maxGreenFrequency+", maxRed = "+this.maxRedFrequency);}
	        JFrame frame = new JFrame("Mean Distribution: "+settings.image.getTitle());
	        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
	        frame.setLayout(new GridLayout(0, 1));
	        // Need to work out x axis offset so that means align - we offset either red or green to ensure X-axis remains positive
	        double redOffset=0, greenOffset=0;
	        if (utils.Mean(green)-utils.Mean(red) > 0) { 
	        	// Green greater than red, so offset red positively
	        	greenOffset = utils.Mean(green)-utils.Mean(red) ;
	        }
	        else {
	        	// Red greater than (or same) as green - offset green
	        	redOffset = utils.Mean(red) - utils.Mean(green);
	        }
	        // Create charts and add panels to frame 
	        frame.add(createPanel(new Color(0,255,0,128),Bins, greenBinCounts, green, "Green", this.maxGreenFrequency,0,greenOffset,s.greenThreshold));
	        frame.add(createPanel(new Color(255,0,0,128),Bins, redBinCounts, red, "Red",this.maxRedFrequency,1,redOffset,s.redThreshold));
	        frame.pack();
	        frame.setLocationRelativeTo(null);
	        // SHow frame
	        frame.setVisible(true);
	    }

	    private void DrawChart(XYPlot plot){
			  this.chart = new JFreeChart(_title, JFreeChart.DEFAULT_TITLE_FONT, plot, true);

		        final ChartPanel chartPanel = new ChartPanel(this.chart);
		        chartPanel.setPreferredSize(new Dimension(1000, 800));
		        chartPanel.setEnforceFileExtensions(false);
		        
				ImagePlus imp = IJ.createImage("New Distributions", "RGB", 1000, 800, 1);
				BufferedImage image = imp.getBufferedImage();
				this.chart.draw(image.createGraphics(),
				new Rectangle2D.Float(0, 0, imp.getWidth(), imp.getHeight()));
				imp.setImage(image);
				imp.show();

		}
	    
	    private  XYSeriesCollection generateData() {
	        XYSeriesCollection data = new XYSeriesCollection();
	        for (int i = 0; i < MAX; i++) {
	            data.addSeries(generateSeries("Series " + (i + 1)));
	        }
	        return data;
	    }
	    
	    private  XYSeries generateSeries(String key) {
	        XYSeries series = new XYSeries(key);
	        for (int i = 0; i < 16; i++) {
	            series.add(i, Math.abs(rand.nextGaussian()*20));
	        }
	        return series;
	    }
	    
	    protected void GetBinCounts(){
			IJ.log("\nGetBinCounts");
		    // Using Freedman–Diaconis' for bucketwidths
		    bucketWidth = 0;
		    if(bucketWidth==0) {
		        double[] arr;
	            arr = red;
	            
		        if (arr.length <= 0) {
		            IJ.log("Auto bin array is empty");
		            return;
		        }
		        if (settings.debug > 0) {
		            IJ.log("DistributionChart arr length = " + arr.length);
			        if (settings.debug > 3) {
			            IJ.log("\nDistributionChart arr:" );
			        	for(int idx=0;idx<arr.length;idx++){
			        		IJ.log(idx+","+arr[idx] );
			        	}
			        }	            
		        }
		        bucketWidth=2*utils.iqr(arr)/Math.pow(arr.length, 1.0/3);
		        if (settings.debug > 0 ) {
		        	IJ.log("IQR is "+utils.iqr(arr));
		        	IJ.log("Divisor is "+Math.pow(arr.length, 1/3));
		        	IJ.log("DistributionChart auto bin interval is "+bucketWidth); 
		        }
		    }
		    // Get maxes 
		    maxGreen = 0;
		    maxRed=0;
		    for (int i = 0; i < green.length; i++) {
		        if (green[i] > maxGreen) { maxGreen = green[i]; }
		    }
		    for (int i = 0; i < red.length; i++) {
		        if (red[i] > maxRed) { maxRed = red[i]; }
		    }
		    // Work out bins based on greenMax - bins go from 0 in binInterval steps to first binInterval above greenMax (.e.g 15 is max & interval is ten then it is 0,10,20)
		    // get top limit
		    // but if top bin would go above 255 then we ensure bins are centred such that 255 is top of last bin

		    double nTopBin = maxGreen - (maxGreen % bucketWidth) + bucketWidth;
		    if(maxRed > maxGreen){
		    	 nTopBin = maxRed - (maxRed % bucketWidth) + bucketWidth;
		    }

		    if (settings.debug > 0) { IJ.log("DistributionChart - top bin: " + nTopBin); }


		    // NUmber of bins
		    nBinCount = (int) Math.floor((nTopBin / bucketWidth) + 1);
		    if (settings.debug > 0) { IJ.log("DistributionChart -  bin count: " + nBinCount); }

			
		    greenBinCounts = new double[nBinCount];
		    redBinCounts = new double[nBinCount];
		    Bins = new double[nBinCount];
		    double offset = 0;
		    // Now if last bin would go beyond 255, we work out an offset to shift everything down such that the top bin will
		    // go from 255-bucketWidth to 255
		    if ( bucketWidth * (nBinCount-1) + (bucketWidth / 2 ) > 255-(bucketWidth/2) ){
		    	offset = 255-(bucketWidth/2) - ( bucketWidth * (nBinCount-1) + (bucketWidth / 2 )  );
		    	if (settings.debug > 0){
		    		IJ.log("Mid Top bin would be " + (bucketWidth * (nBinCount-1) + (bucketWidth / 2 ))+"\nOffset will be " + offset);
		    	}
		    }
		    // Zero counts
		    for (int i = 0; i < nBinCount; i++) {
		        greenBinCounts[i] = 0;
		        redBinCounts[i] = 0;
		        Bins[i] = bucketWidth * i + bucketWidth / 2 + offset;
		        if (Bins[i]<0){Bins[i]=bucketWidth / 2 ;}
		    }

		    //Now do counts 
		    for (int i = 0; i < green.length; i++) {
		        int nBinIndex = (int) Math.floor(green[i] / bucketWidth);
		        if (nBinIndex < nBinCount) {
		            greenBinCounts[nBinIndex]++;
		            if ( greenBinCounts[nBinIndex] > this.maxGreenFrequency) {
		                this.maxGreenFrequency =  greenBinCounts[nBinIndex];
		            }
		        }
		        nBinIndex = (int) Math.floor(red[i] / bucketWidth);
		        if (nBinIndex < nBinCount) {
		            redBinCounts[nBinIndex]++;
		            if (redBinCounts[nBinIndex]>maxRedFrequency){
		                maxRedFrequency= redBinCounts[nBinIndex];
		            }
		        }
		    }
		    
		    if (settings.debug > 0) {IJ.log("Max Green frequency = "+maxGreenFrequency +",  max Red Frequency = "+maxRedFrequency); }
		    
		    if (settings.debug >= 3  ){
		    	IJ.log("\nBin,GreenCOunt,RedCount");
		    	for(int i=0;i<Bins.length;i++){
		    		IJ.log(Bins[i]+","+greenBinCounts[i]+","+redBinCounts[i]);
		    	}
		    	IJ.log("");
		    }
		}

	    



		private IntervalXYDataset GetIntervalXYDataset(double[] bins, double[] greenBinCounts,double[] redBinCounts) {
			// TODO Auto-generated method stub
			double [][]xyGreen = new double[2][];
			double [][]xyRed = new double[2][];

			xyGreen[0] = bins;
			xyGreen[1] = greenBinCounts;
			xyRed[0] = bins;
			xyRed[1] = redBinCounts;
			
			final DefaultXYDataset dataset = new DefaultXYDataset();
		    
			if ( greenBinCounts!=null) {
				dataset.addSeries("Green", xyGreen);
			}
			if ( redBinCounts!=null) {
				dataset.addSeries("Red", xyRed);
			}
			return new XYBarDataset(dataset,5);
		}

	    public void SingleChart( ){

		    if (settings.debug > 0) { IJ.log("\nSingleChart - " + settings.image.getTitle());}
		    
		    settings.GetMax(_particles);

		    int nCount;
	        nCount = red.length;

		    if (settings.debug > 0) { IJ.log("Max positive green: " + maxGreen + "\nMax positive red: " + maxRed); }

		  IntervalXYDataset dataset = GetIntervalXYDataset(Bins,greenBinCounts,redBinCounts);
		  NumberAxis domainAxis = new NumberAxis("Mean");
		  domainAxis.setAutoRangeIncludesZero(true);

		  ValueAxis valueAxis = new NumberAxis("Frequency");

		  XYBarRenderer renderer = new XYBarRenderer();
		  plot = new XYPlot(dataset, domainAxis, valueAxis, renderer);
		  plot.setOrientation(PlotOrientation.VERTICAL);
		  plot.getDomainAxis().setAutoRange(true);
          renderer.setSeriesPaint(0, new Color(0,255,0,128));
          renderer.setSeriesPaint(1, new Color(255,0,0,128));
    	  renderer.setBarPainter( new StandardXYBarPainter());
	        
    	  ValueMarker marker = new ValueMarker(utils.Mean(green));  // position is the value on the axis
		  marker.setPaint(Color.GREEN);
		  float[] dash = {10};
		marker.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1.0f, dash , 0.0f));
		  //marker.setLabel("here"); // see JavaDoc for labels, colors, strokes
	    marker.setLabel("Green Mean = " + utils.Mean(green));
		
	      marker.setLabelAnchor(RectangleAnchor.TOP_LEFT);
	      marker.setLabelTextAnchor(TextAnchor.TOP_RIGHT);
		  plot.addDomainMarker(marker, Layer.FOREGROUND);
		  
		  marker = new ValueMarker(utils.Mean(red));  // position is the value on the axis
		  marker.setPaint(Color.RED);
		marker.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1.0f, dash , 0.0f));

		    marker.setLabel("Red Mean = " + utils.Mean(red)+"<br>StdDev = "+utils.StdDev(red,utils.Mean(red)));
	
		 marker.setLabelAnchor(RectangleAnchor.TOP_RIGHT);
		      marker.setLabelTextAnchor(TextAnchor.TOP_LEFT);
			  plot.addDomainMarker(marker, Layer.FOREGROUND);
		  
			  BasicMultiLineXYTextAnnotation newLabel = new BasicMultiLineXYTextAnnotation("Red Mean = " + utils.Mean(red)+"\nStdDev = "+utils.StdDev(red,utils.Mean(red)),utils.Mean(red),30)	;
			  newLabel.setTextAnchor(TextAnchor.TOP_LEFT);
	         plot.addAnnotation(newLabel);
	            
	            DrawChart(plot);
		}
		
	}
	
  