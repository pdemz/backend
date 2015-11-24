package demz;

import com.google.maps.DirectionsApi;
import com.google.maps.GeoApiContext;
import com.google.maps.GeocodingApi;
import com.google.maps.model.*;

//Smooth Cat Rider
public class Rider {
	private int id = 0;
	private int driverID = 0;
	private String name = "";
	
	//Destinations are saved in string and latitude/longitude format
	private String strOrig;
	private String strDest;
	private LatLng origin;
	private LatLng destination;
	
	public Rider(int newId, String newName, String newOrigin, String newDest){
	
		id = newId;
		name = newName;
		strOrig = newOrigin;
		strDest = newDest;
		
		//Get origin and destination coordinates. There's probably a better way to do this.
		GeoApiContext context = new GeoApiContext().setApiKey("AIzaSyBrmvso2zVY_soF75Een6sI8sA5f0yGw5s");

		DirectionsRoute[] routes;
		try {
			routes = DirectionsApi.newRequest(context)
					.origin(newOrigin)
					.destination(newDest).await();
			
			origin = routes[0].legs[0].startLocation;
			destination = routes[0].legs[0].endLocation;
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public int getID(){
		return id;
	}
	
	public int getDriverID(){
		return driverID;
	}
	
	public String getName(){
		return name;
	}
	
	public String getOrigin(){
		return strOrig;
	}
	
	public String getDestination(){
		return strDest;
	}
	
	
}