package com.tiamat.wemoSwitchLocalNetworkControl;

import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.regex.*;

public class WemoDeviceTable {
	//Member variables ==========================================================================//
	private final int SSDP_CLIENT_TIMEOUT = 1500; //SSDP timeout (ms)
	
	private final String ssdpAddressString = "239.255.255.250"; //SSDP discovery IP
	private final int ssdpPort = 1900; //SSDP discovery port
	private InetAddress ssdpAddress;
	private final String ssdpWemoRequestMessage //Wemo SSDP request message
			= "M-SEARCH * HTTP/1.1" + "\r\n"
			+ "ST: urn:Belkin:service:basicevent:1" + "\r\n"
			+ "MX: 2" + "\r\n"
			+ "MAN: \"ssdp:discover\"" + "\r\n"
			+ "HOST: " + ssdpAddressString + ":" + ssdpPort + "\r\n"
			+ "\r\n";
	
	public volatile HashMap<String, WemoDevice> deviceTable = new HashMap<String, WemoDevice>();
	
	//Constructors ==============================================================================//
	public WemoDeviceTable() {
		try {
			this.ssdpAddress = InetAddress.getByName(ssdpAddressString);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
	}
	
	//Overridden methods ========================================================================//
	
	//Custom Methods ============================================================================//
	public void updateTable() {
		System.out.println("Table update starting ...");
		
		//UDP variables
		byte[] txData = new byte[1024];
		byte[] rxData = new byte[1024];
		
		//Establish client socket for SSDP
		try(DatagramSocket clientSocket = new DatagramSocket();) {
			clientSocket.setSoTimeout(SSDP_CLIENT_TIMEOUT); //Set client socket timeout (ms)
			
			//Send SSDP request
			txData = ssdpWemoRequestMessage.getBytes();
			DatagramPacket txPacket = new DatagramPacket(txData, txData.length, ssdpAddress, ssdpPort);
			clientSocket.send(txPacket);
			
			//Accept responses from devices
			boolean acceptingDevices = true;
			while(acceptingDevices) {
				try {
				//Receive SSDP response
				DatagramPacket rxPacket = new DatagramPacket(rxData, rxData.length);
				clientSocket.receive(rxPacket);
				
				//Fetch device information
				WemoDevice wemoDevice = fetchWemoData(rxPacket);
				if(wemoDevice != null) {
					
					//Debug only. Embedded device control at poll
					if(wemoDevice.getFriendlyName().contentEquals("Lamp")) {
						//System.out.println(wemoDevice); //Debug, print device info
						if(wemoDevice.getBinaryState() == 1) {
							wemoDevice.setBinaryState(0);
							
						} else if (wemoDevice.getBinaryState() == 0) {
							wemoDevice.setBinaryState(1);
							
						} else {
							System.out.println("ERROR: Unexpected Wemo binary state!");
							
						}
						
					}
				}
				
				} catch (SocketTimeoutException e) { //Exit when responses timeout
					acceptingDevices = false;
					
				}
				
			}
			
		} catch (SocketException e) {
			e.printStackTrace();
			
		} catch (IOException e) {
			e.printStackTrace();
			
		}
		
	}
	
	private WemoDevice fetchWemoData(DatagramPacket rxPacket) {	
		/* ========================================================================================
		Fetches Weomo data from device by reception datagram packet
		Returns WemoDevice object by finding the following parameters:
			deviceIP = Device IP address
			devicePort = Device port
			deviceFriendlyName = Device friendly name
			
			deviceType = device type //Currently unused. Only three types supported*:
				lightswitch = belkin switch
				controllee = Belkin outlet
				dimmer = Belkin dimmer switch
				
				*not all services supported in this revision
		
		If parameters cannot be resolved, return null
		If parameters identify unsupported device type, return null
		======================================================================================== */
		
		//Device variables
		String deviceSetupXML = "";
		InetAddress deviceIP = rxPacket.getAddress();
		int devicePort = 0;
		String deviceType = "";
		String deviceFriendlyName = "";
		
		//Read response data to buffer
		byte[] rxData = new byte[1024];
		rxData = rxPacket.getData();
		String stringRxData = new String(rxData);
		
		//Search response string for setup.xml location
		Pattern pattern = Pattern.compile("LOCATION: (.+?)\r\nOPT", Pattern.DOTALL);
		Matcher matcher = pattern.matcher(stringRxData);
		if(matcher.find()) {
			deviceSetupXML = matcher.group(1);
			
			//Search device setup xml for port
			pattern = Pattern.compile("http:/" + deviceIP.toString() + ":(.+?)/setup.xml");
			matcher = pattern.matcher(deviceSetupXML);
			if(matcher.find()) {
				devicePort = Integer.parseInt(matcher.group(1));
				
			}
			
		}
		
		//Fetch data from setup.xml
		try {
			URL setupURL = new URL(deviceSetupXML);
			URLConnection setupURLConnection = setupURL.openConnection();		
			BufferedReader setupXMLInput = new BufferedReader(new InputStreamReader(setupURLConnection.getInputStream()));
			
			String inputLine = setupXMLInput.readLine();
			while(inputLine != null) {
				if(inputLine.contains("<deviceType>")) {
					//Search for device type
					pattern = Pattern.compile("<deviceType>urn:Belkin:device:(.+?):1</deviceType>");
					matcher = pattern.matcher(inputLine);
					if(matcher.find()) {
						deviceType = matcher.group(1);
						
					}
					
				} else if (inputLine.contains("<friendlyName>")) {
					//Search for friendly name
					pattern = Pattern.compile("<friendlyName>(.+?)</friendlyName>");
					matcher = pattern.matcher(inputLine);
					if(matcher.find()) {
						deviceFriendlyName = matcher.group(1);
						
					}
					
				} else {
					
				}
				
				inputLine = setupXMLInput.readLine();
				
			}
			
			//Create Wemo device from read data
			if((deviceIP != null) && (devicePort != 0) && (deviceFriendlyName != "") && (deviceType != "")) {
				WemoDevice wemoDevice = new WemoDevice(deviceIP, devicePort, deviceFriendlyName, deviceType);
				
				return wemoDevice;
				
			} else {
				
				return null;
			}
			
		} catch (MalformedURLException e) {
			e.printStackTrace();
			return null;
			
		} catch (IOException e) {
			e.printStackTrace();
			return null;
			
		}
		
	}

}
