package demz;

import com.google.maps.*;
import com.google.maps.model.*;

public class Driver {

	public String id;
	public String accountToken;
	public String accessToken;
	public String aboutMe;
	public String phone;
	private int limit = 0;
	private int distance = 0; //distance in miles
	private long duration = 0; //duration in seconds
	private String origin; //Origin and destination stored as coordinate string
	private String destination;
	private String apnsToken;
	
	public Driver(){
		
	}
	
	//Constructor for when a driver is recalled from the database, rather than added into it
	public Driver(String newID, int newLimit, String newOrigin, String newDestination, long newDuration, int newDistance, String newAccountToken){
		id = newID;
		limit = newLimit;
		origin = newOrigin;
		destination = newDestination;
		duration = newDuration;
		distance = newDistance;
		accountToken = newAccountToken;
		
	}
	
	public Driver(String newId, String newAccessToken, String newApnsToken, int newLimit, String newOrigin, String newDestination){	
		id = newId;
		limit = newLimit;
		accessToken = newAccessToken;
		
		if (newApnsToken != null){
			apnsToken = newApnsToken.replace("<", "").replace(" ", "").replace(">", "");
		}
		
		//Now get the time of the route, in seconds, with no detours
		GeoApiContext context = new GeoApiContext().setApiKey("AIzaSyBrmvso2zVY_soF75Een6sI8sA5f0yGw5s");

		DirectionsRoute[] routes;
		try {
			routes = DirectionsApi.newRequest(context)
					.origin(newOrigin)
					.destination(newDestination).await();

			duration = routes[0].legs[0].duration.inSeconds;
			distance = (int) (routes[0].legs[0].distance.inMeters/1609.344);
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
	
	public int getLimit(){
		return limit;
	}
	
	public long getDuration(){
		return duration;
	}
	
	public String getApnsToken(){
		return apnsToken;
	}
	public int getDistance(){
		return distance;
	}
	
}