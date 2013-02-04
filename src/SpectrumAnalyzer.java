import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.IntervalXYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import javax.swing.*;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class SpectrumAnalyzer {

	/**
	 * An application using JFreeChart that visualizes the maximum and current signal strengths.
	 * 
 	 * It is designed for a custom built 2.4GHz radio spectrum analyzer built with an Atmega328 Microcontroller and CYWM6935 radio module.
 	 * The Arduino MCU can be programmed using the CYWM6935 module, found at https://github.com/wa5znu/CYWM6935/
 	 * 
 	 * For more information also see http://nurdspace.nl/Arduino_Radio_Spectrum_Analyzer
	 */
	
	private JFreeChart chart;
	private JFrame frame;
	private SerialReader reader;
	private Thread thread;
	private XYSeriesCollection dataset;
	
	class FrameListener extends WindowAdapter
	{
		public void windowClosing(WindowEvent e)
		{
			reader.close();
			JOptionPane.showMessageDialog(frame, "Exiting.");
		}
	}
	
	private XYSeriesCollection createDataset(){
		XYSeriesCollection ds = new XYSeriesCollection();
		
		XYSeries avg_series=new XYSeries("Average over 5 points");
	    ds.addSeries(avg_series);
	    
	    XYSeries max_series=new XYSeries("Maximum");
		ds.addSeries(max_series);
	    
	    return(ds);
	}
	
	private JFreeChart createChart(IntervalXYDataset dataset){
		chart = ChartFactory.createXYBarChart(
                null,       // chart title
                "Frequency (MHz)",
                false,
                "Signal strength (%)",    // range axis label
                dataset,                  // data
                PlotOrientation.VERTICAL, // orientation
                true,                     // include legend
                false,                     // tooltips?
                false                     // URLs?
            );
        
        //customize the plot somewhat
        XYPlot plot=(XYPlot) chart.getPlot();
        ValueAxis y_as = plot.getRangeAxis();
        ValueAxis x_as = plot.getDomainAxis();
        x_as.setRange(2400,2500);
        y_as.setRange(0,100);
        
        return(chart);
	}
	
	public SpectrumAnalyzer(){
		createGUI();
		frame.addWindowListener(new FrameListener());
		reader = new SerialReader("/dev/ttyUSB0",this);
		try{
			reader.connect();
		}
		catch (Exception e){
			JOptionPane.showMessageDialog(frame, "Unable to connect to ttyUSB0.");
		}
		System.out.println("Succesfully connected.");
		thread= new Thread(reader);
		thread.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
	        public void uncaughtException(Thread t, Throwable e) {
	        	JOptionPane.showMessageDialog(frame, "Something went wrong while reading data from the serial port.\n"+e.getMessage());
	        }
	    });
		thread.start();
		try{
			thread.join();
		}
		catch (InterruptedException e){
			JOptionPane.showMessageDialog(frame, "Reading interrupted.");
		}
	}
	
	//private 
	public void setMaxFrequencySignal(Double freq, Double strength){
		XYSeries max_series=dataset.getSeries(1);
		setValue(freq,strength,max_series);
		
	}
	public void setAvgFrequencySignal(Double freq, Double strength){
		XYSeries avg_series=dataset.getSeries(0);
		setValue(freq,strength,avg_series);
	}
	private void setValue(Double freq, Double strength, XYSeries series){
		Integer ind=series.indexOf(freq);
		if(ind<0){
			series.add(freq,strength);
		}
		else{
			series.update(freq,strength);
		}
	}
	
	private void createGUI(){
		try {
            // Set cross-platform Java L&F (also called "Metal")
			UIManager.setLookAndFeel(
					UIManager.getSystemLookAndFeelClassName());
		} 
		catch (UnsupportedLookAndFeelException e) {
			// handle exception
		}
		catch (ClassNotFoundException e) {
		       // handle exception
	    }
	    catch (InstantiationException e) {
	       // handle exception
	    }
	    catch (IllegalAccessException e) {
	       // handle exception
	    }

		frame = new JFrame("Spectrum Analyzer");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        dataset = createDataset();
        JFreeChart chart = createChart(dataset);
        ChartPanel panel = new ChartPanel(chart);
        
        JToolBar toolbar = createToolbar();
        
        JPanel mainPane = new JPanel();
        mainPane.setLayout(new BoxLayout(mainPane, BoxLayout.PAGE_AXIS));
        mainPane.add(toolbar);
        mainPane.add(panel);
        
        frame.getContentPane().add(mainPane);
        panel.setPreferredSize(new Dimension(1000,400));
        
        //Display the window.
        frame.pack();
        frame.setVisible(true);
	}
	private class ResetAction extends AbstractAction {
	    /**
		 * Perform the maxima reset action.
		 */
		private static final long serialVersionUID = -7144957379667269909L;
		public ResetAction() {
			  super("Reset maxima",null);
			  putValue(SHORT_DESCRIPTION, "Reset the maxima (blue bars) for all frequencies");
			}
		public void actionPerformed(ActionEvent e) {
			reader.resetMaxValues();
		}
	}
	private JToolBar createToolbar(){
		/*
		 * Create the toolbar
		 */
        JToolBar toolbar=new JToolBar();
        JButton button = new JButton(new ResetAction());
        toolbar.add(button);
        return(toolbar);
	}
	
	public static void main(String[] args) {
		new SpectrumAnalyzer();
	}

}
