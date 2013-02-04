
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.TreeMap;

import gnu.io.CommPort;
import gnu.io.CommPortIdentifier;
import gnu.io.RXTXPort;
import gnu.io.SerialPort;

public class SerialReader implements Runnable{
	/*
	 * This class connects to a serial port, reads the frequency and signal strengths from it
	 * and updates the analyzer for the average and max signal frequency.
	 * 
	 * It is written to work together with a device powered by
	 * an ATMega 328 microcontroller and a CYWM6935 radio controller, but should work with any
	 * serial device which conforms to the specifications below.
	 * 
	 * It expects the device connected to the serial port to print the signal strength
	 * for each frequency on a separate line.
	 * Each line should contain two integers, separated by whitespace. The first integer should be
	 * the channel (amount of MHz above the 2400MHz). The second integer is the signal
	 * strength, where 32 is the maximum signal strength.
	 * 
	 * To use this library with any other serial device, only the ReadLine() method has to be overridden.
	 * 
	 * 
	 * @param 	portname	The name of the Serial port to open (On Linux with an FTDI cable, often /dev/ttyUSB0)	
	 * @param 	analyzer 	An instance of SpectrumAnalyzer, which visualizes the measured signal strengths
	 */
	private String portname;
	private BufferedReader reader;
	private SpectrumAnalyzer analyzer;
	private RXTXPort serialPort;
	private boolean stopReading;
	private TreeMap<Double,ArrayList<Double>> dataMap;
	private TreeMap<Double,Double> maxMap;

	
	public void connect() throws Exception{
		CommPortIdentifier portIdentifier = CommPortIdentifier.getPortIdentifier(portname);
		if(portIdentifier.isCurrentlyOwned()){
			throw new Exception("The commport is currently in use.");
		}
		CommPort port = portIdentifier.open(this.getClass().getName(),1000);
		if(! (port instanceof SerialPort)){
			throw new Exception("The port is not serial port.");
		}
		serialPort = (RXTXPort) port;
		serialPort.setSerialPortParams(57600,SerialPort.DATABITS_8,SerialPort.STOPBITS_1,SerialPort.PARITY_NONE);
		serialPort.enableReceiveTimeout(5000);
		reader = new BufferedReader(new InputStreamReader(serialPort.getInputStream()));
	}
	public void close(){
		System.out.println("Closing serial port");
		stopReading=true;
		serialPort.close();
	}
	private static Double calculateAverage(ArrayList<Double> list){
		Double total=0.0;
		for(Double i: list){
			total+=i;
		}
		Double avg=total/list.size();
		return(avg);
	}
	
	private Double[] readLine() throws IOException{
		String data;
		try{
			data=reader.readLine();
		}
		catch (IOException e){
			e.printStackTrace();
			throw new IOException(e.getMessage()+"\nIs your USB/serial cable connected properly?");
		}
		String data2=data.replaceAll("[^\\d\\s]","");
		if(data!=data2){
			data=data2;
		}
		System.out.println(data);
		String[] vals = data.split("\\s+");
		Double freq;
		Double strength;
		try{
			freq=Double.parseDouble(vals[0])+2400.0;
			strength=Double.parseDouble(vals[1]);
		}
		catch (NumberFormatException e){
			Double[] tempvals=readLine();
			freq=tempvals[0];
			strength=tempvals[1];
			System.out.println("Skipping line "+data+". Not valid integers.");
		}
		catch (ArrayIndexOutOfBoundsException e){
			Double[] tempvals=readLine();
			freq=tempvals[0];
			strength=tempvals[1];
			System.out.println("Skipping line "+data+". Only one value.");
		}
		if(freq>2500|freq<2400){
			Double[] tempvals=readLine();
			freq=tempvals[0];
			strength=tempvals[1];
			System.out.println("Skipping line "+data+". Frequency too high.");
		}
		if(strength>32){
			Double[] tempvals=readLine();
			freq=tempvals[0];
			strength=tempvals[1];
			System.out.println("Skipping line "+data+". Signal strength too high.");
		}
		return(new Double[] {freq,strength});
	}
	public void resetMaxValues(){
		for(Double k: maxMap.keySet()){
			maxMap.put(k, 0.0);
			analyzer.setMaxFrequencySignal(k,0.0);
		}
	}
	
	public void run() throws RuntimeException{
		stopReading=false;
		try{
			Double[] data=readLine();
			Double freq=data[0];
			Double strength=data[1];
			dataMap = new TreeMap<Double, ArrayList<Double>>();
			maxMap = new TreeMap<Double, Double>();
			while(data!=null & stopReading==false){
				if(dataMap.get(freq)==null){
					dataMap.put(freq, new ArrayList<Double>());
				}
				dataMap.get(freq).add(strength);
				if(dataMap.get(freq).size()==6){
					dataMap.get(freq).remove(0);
				}
				Double avg=calculateAverage(dataMap.get(freq))/32.0*100; //calculate the percentage of the absolute possible maximum (32)
				analyzer.setAvgFrequencySignal(freq,avg);
				if(maxMap.get(freq)==null){
					maxMap.put(freq, avg);
				}
				if(avg>=maxMap.get(freq)){
					maxMap.put(freq,avg);
					analyzer.setMaxFrequencySignal(freq,avg);
				}

				data=readLine();//get next data
				if(data[0]<freq){
					//stop after we loop around the frequencies
					//break;
				}
				freq=data[0];
				strength=data[1];
			}
			serialPort.close();
		}
		catch(Exception e){
			serialPort.close();
			e.printStackTrace();
			throw new RuntimeException(e.getMessage());
		}

	}
	public SerialReader(String portname, SpectrumAnalyzer analyzer){
		this.portname=portname;
		this.analyzer=analyzer;
	}
}
