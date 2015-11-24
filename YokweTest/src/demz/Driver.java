package demz;

import com.google.maps.*;
import com.google.maps.model.*;

public class Driver {

	
	private int id = 0;
	private int riderId = 0;
	private int limit = 0;
	private long routeTime = 0;
	private String name = "";
	
	//Origin and destination stored as string and LatLng
	private LatLng origin;
	private LatLng destination;
	public String strOrig;
	public String strDest;
	
	public Driver(int newId, int newLimit, String newName, String newOrigin, String newDestination){	
		id = newId;
		limit = newLimit;
		name = newName;
		strOrig = newOrigin;
		strDest = newDestination;
			
		//Now get the time of the route, in seconds, with no detours
		GeoApiContext context = new GeoApiContext().setApiKey("AIzaSyBrmvso2zVY_soF75Een6sI8sA5f0yGw5s");

		DirectionsRoute[] routes;
		try {
			routes = DirectionsApi.newRequest(context)
					.origin(newOrigin)
					.destination(newDestination).await();
			
			routeTime = routes[0].legs[0].duration.inSeconds;
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
	
	public int getRiderID(){
		return riderId;
	}
	
	public String getName(){
		return name;
	}
	
	public LatLng getOrigin(){
		return origin;
	}
	
	public LatLng getDestination(){
		return destination;
	}
	
	public int getLimit(){
		return limit;
	}
	
	public long getRouteTime(){
		return routeTime;
	}
	
}