package com.tiamat.wemoSwitchLocalNetworkControl;

public class MainSwitchControl {

	public static void main(String[] args) {
		//Startup conditions and dialog
		System.out.println("Wemo switch control application starting ...");
		
		//Main application
		WemoDeviceTable deviceTable = new WemoDeviceTable();
		deviceTable.updateTable();
		
		System.out.println(deviceTable.table);
		
		WemoDevice lamp = deviceTable.table.get("Lamp");
		int deviceState = lamp.getBinaryState();
		int count = 100;
		while(count >= 0) {
			if(deviceState == 1) {
				deviceState = 0;
				lamp.setBinaryState(deviceState);
				
			} else if(deviceState == 0) {
				deviceState = 1;
				lamp.setBinaryState(deviceState);
				
			} else {
				System.out.println("ERROR: Unexpected device state!");
				
			}
			
			count--;
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		//Exit conditions and dialog
		System.out.println("Wemo switch control application closing ...");
	}

}
