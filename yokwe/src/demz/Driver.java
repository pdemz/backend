package demz;

import com.google.maps.*;
import com.google.maps.model.*;

public class Driver {

	
	private String id;
	private int limit = 0;
	
	//duration in seconds
	private long duration = 0;
	
	//Origin and destination stored as string and LatLng
	private String origin;
	private String destination;
	
	public Driver(String newId, int newLimit, String newOrigin, String newDestination){	
		id = newId;
		limit = newLimit;
		
		//Now get the time of the route, in seconds, with no detours
		GeoApiContext context = new GeoApiContext().setApiKey("AIzaSyBrmvso2zVY_soF75Een6sI8sA5f0yGw5s");

		DirectionsRoute[] routes;
		try {
			routes = DirectionsApi.newRequest(context)
					.origin(newOrigin)
					.destination(newDestination).await();

			duration = routes[0].legs[0].duration.inSeconds;
			origin = routes[0].legs[0].startLocation.toString();
			destination = routes[0].legs[0].endLocation.toString();

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


		
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
	
}