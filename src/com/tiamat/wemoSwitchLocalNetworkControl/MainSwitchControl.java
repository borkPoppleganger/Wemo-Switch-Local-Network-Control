package com.tiamat.wemoSwitchLocalNetworkControl;

public class MainSwitchControl {

	public static void main(String[] args) {
		//Startup conditions and dialog
		System.out.println("Wemo switch control application starting ...");
		
		//Main application
		WemoDeviceTable deviceTable = new WemoDeviceTable();
		deviceTable.updateTable();
		
		//Exit conditions and dialog
		System.out.println("Wemo switch control application closing ...");
	}

}
