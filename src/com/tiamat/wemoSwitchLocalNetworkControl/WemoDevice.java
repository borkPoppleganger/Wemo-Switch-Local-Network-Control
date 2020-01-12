package com.tiamat.wemoSwitchLocalNetworkControl;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.regex.*;

public class WemoDevice {
	/*=============================================================================================
	Current version supports only device types:
		lightswitch = belkin switch
		controllee = Belkin outlet
		dimmer = Belkin dimmer switch
	=============================================================================================*/

	//Member variables ==========================================================================//
	private InetAddress deviceIP;
	private String deviceIPString;
	private int devicePort;
	private String deviceFriendlyName;
	private String deviceType;
	
	private final String soapXMLPretext
		= "<?xml version=\"1.0\" encoding=\"utf-8\"?>" + "\r\n"
		+ "<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\" s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">" + "\r\n"
		+ "<s:Body>" + "\r\n";
	
	private final String soapXMLPosttext
		= "</s:Body>" + "\r\n"
		+ "</s:Envelope>";
	
	
	//Constructors ==============================================================================//
	public WemoDevice(InetAddress deviceIP, int devicePort, String deviceFriendlyName, String deviceType) {
		this.deviceIP = deviceIP;
		this.devicePort = devicePort;
		this.deviceFriendlyName = deviceFriendlyName;
		this.deviceType = deviceType;
		//IP address to string w/o "/" prefix
		String ipString = deviceIP.toString();
		this.deviceIPString = ipString.substring(1, ipString.length());
		
	}
	
	//Overridden methods/Common methods =========================================================//
	@Override
	public String toString() {
		return "WemoDevice [deviceIP=" + deviceIP + ", devicePort=" + devicePort + ", deviceFriendlyName="
				+ deviceFriendlyName + ", deviceType=" + deviceType + "]";
	}
	
	public InetAddress getAddress() {
		return deviceIP;
	}

	public int getPort() {
		return devicePort;
	}

	public String getFriendlyName() {
		return deviceFriendlyName;
	}

	public String getDeviceType() {
		return deviceType;
	}
	
	//Custom Methods ============================================================================//
	public int getBinaryState() {
		/*
		Gets the binary state of the device:
			0 - Off
			1 - On
		Returns int binary state
		*/
		
		String bodyElement
			= "<u:GetBinaryState xmlns:u=\"urn:Belkin:service:basicevent:1\">" + "\r\n"
			+ "</u:GetBinaryState>" + "\r\n";
		
		return binaryRequest(bodyElement, "#GetBinaryState");
		
	}
	
	public void setBinaryState(int binaryState) {
		/*
		Sets the binary state of the device:
			0 - Off
			1 - On
		*/
		
		String bodyElement 
			= "<u:SetBinaryState xmlns:u=\"urn:Belkin:service:basicevent:1\">" + "\r\n"
			+ "<BinaryState>" + binaryState + "</BinaryState>" + "\r\n"
			+ "</u:SetBinaryState>" + "\r\n";
		
		binaryRequest(bodyElement, "#SetBinaryState");

	}
	
	private int binaryRequest(String bodyElement, String soapActionSubstring) {
		/*
		Binary state request. Supports both get and set methods
		Returns binary state line from device http response
		*/
		
		//Prepare SOAP request xml contents
		String urlParameters = soapXMLPretext + bodyElement + soapXMLPosttext;
		byte[] postData       = urlParameters.getBytes( StandardCharsets.UTF_8 );
		int    postDataLength = postData.length;
		String request        = "http://" + deviceIPString + ":" + devicePort + "/upnp/control/basicevent1";
		
		//HTTP SOAP request to device
		try {
			//Set up URL-HTTP-SOAP request
			URL url = new URL( request );
			HttpURLConnection conn= (HttpURLConnection) url.openConnection();           
			conn.setDoOutput( true );
			conn.setInstanceFollowRedirects( false );
			conn.setRequestMethod( "POST" );
			conn.setRequestProperty("Host", (deviceIPString + ":" + devicePort));
			conn.setRequestProperty("Content-Type", "text/xml");
			conn.setRequestProperty( "charset", "utf-8");
			conn.setRequestProperty("SOAPAction", ("\"urn:Belkin:service:basicevent:1" + soapActionSubstring + "\""));
			conn.setRequestProperty("Content-Length", Integer.toString( postDataLength ));
			conn.setUseCaches( false );
			
			//Send request
			DataOutputStream wr = new DataOutputStream( conn.getOutputStream());
			wr.write( postData );
			
			//Recieve response (Note: event if not used, this is required else device will error)
			DataInputStream ir = new DataInputStream( conn.getInputStream());
			
			String inputLine = ir.readLine();
			while(inputLine != null) {
				if(inputLine.contains("<BinaryState>")) {
					//Search response for binary state
					Pattern pattern = Pattern.compile("<BinaryState>(.+?)</BinaryState>");
					Matcher matcher = pattern.matcher(inputLine);
					if(matcher.find()) {
						return Integer.parseInt(matcher.group(1));
					} else {
						return -1;
						
					}
					
				}
				
			inputLine = ir.readLine();
				
			}
			
			return -1;
			
		} catch (MalformedURLException e) {
			e.printStackTrace();
			return -1;
			
		} catch (IOException e) {
			e.printStackTrace();
			return -1;
			
		}
		
	}
}
