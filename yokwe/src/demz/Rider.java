package demz;

import com.google.maps.DirectionsApi;
import com.google.maps.GeoApiContext;
import com.google.maps.model.*;

//Smooth Cat Rider
public class Rider {
	private String id;
	private String accessToken;
	private String origin;
	private String destination;
	private String apnsToken;
	private String driverID;
	
	//When a rider is recalled from database
	public Rider(String newId, String newOrigin, String newDest){
		
		id = newId;
		origin = newOrigin;
		destination = newDest;
	}
	
	public Rider(String newId, String newAccessToken, String newApnsToken, String newOrigin, String newDest, String newDriverID){
	
		accessToken = newAccessToken;
		id = newId;
		origin = newOrigin;
		destination = newDest;
		apnsToken = newApnsToken.replace("<", "").replace(" ", "").replace(">", "");
		driverID = newDriverID;

		//Get origin and destination coordinates. There's probably a better way to do this.
		GeoApiContext context = new GeoApiContext().setApiKey("AIzaSyBrmvso2zVY_soF75Een6sI8sA5f0yGw5s");

		DirectionsRoute[] routes;
		try {
			routes = DirectionsApi.newRequest(context)
					.origin(newOrigin)
					.destination(newDest).await();
			
			origin = routes[0].legs[0].startLocation.toString();
			destination = routes[0].legs[0].endLocation.toString();
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public String getAccessToken(){
		return accessToken;
	}
	public String getID(){
		return id;
	}
	
	public String getOrigin(){
		return origin;
	}
	
	public String getDestination(){
		return destination;
	}
	
	public String getApnsToken(){
		return apnsToken;
	}
	
	public String getDriverID(){
		return driverID;
	}
	
	
}