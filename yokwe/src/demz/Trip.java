package demz;

import com.google.maps.DirectionsApi;
import com.google.maps.GeoApiContext;
import com.google.maps.model.DirectionsRoute;

public class Trip {
	public int id;
	public int requesteeTripID;
	public Rider rider;
	public Driver driver;
	public String riderID;
	public String driverID;
	public String dOrigin;
	public String dDestination;
	public String rOrigin;
	public String rDestination;
	public String mutualFriends = "0";
	public String status; //Possible statuses are "waiting", "leg1", "leg2". Completed trips will be stored in the history table
	public long duration; //Seconds
	public long riderDuration;
	public long distance; //Meters
	public int price; //cents
	public double addedTime; //Seconds
	
	//This is the type of "trip" (a trip is built as soon as a ride request is made). Different types are "request", "pendingResponse", and "trip".
	//Sort of combining all the old objects.
	public String type = "trip";  
	
	//This is either going to be "ride" or "drive"
	public String requestType;
	
	public void addTripInfo(){
		//access the API
		GeoApiContext context = new GeoApiContext().setApiKey("###");
				
		DirectionsRoute[] routes;
		
		try {
			routes = DirectionsApi.newRequest(context).origin(dOrigin)
					.destination(dDestination).waypoints(rOrigin, rDestination)
					.await();

			duration = routes[0].legs[0].duration.inSeconds + routes[0].legs[1].duration.inSeconds
					+ routes[0].legs[2].duration.inSeconds;

			distance = routes[0].legs[0].distance.inMeters + routes[0].legs[1].distance.inMeters
					+ routes[0].legs[2].distance.inMeters;

			riderDuration = (int) (routes[0].legs[1].duration.inSeconds/60);
			int riderDistance = (int) (routes[0].legs[1].distance.inMeters/1609.344);
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
}
