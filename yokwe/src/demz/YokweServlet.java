package demz;

import java.io.*;
import java.util.ArrayList;
import java.net.MalformedURLException;
import java.net.URL;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletContext;

import java.sql.ResultSet;
import java.sql.SQLException;

import com.notnoop.apns.APNS;
import com.notnoop.apns.ApnsService;

import com.google.maps.GeoApiContext;
import com.google.maps.DirectionsApi;
import com.google.maps.model.DirectionsRoute;
import com.google.gson.*;

import org.json.simple.*;

import org.apache.commons.dbutils.*;

/**
 * Servlet implementation class YokweServlet
 */
@WebServlet("/YokweServlet")
public class YokweServlet extends HttpServlet {
	
	private ArrayList<Driver> drivers= new ArrayList<Driver>();
	private ArrayList<Rider> riders = new ArrayList<Rider>();
	private DatabaseController dbController = new DatabaseController();
	URL resourceURL;
	
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public YokweServlet() {
        super();
        // TODO Auto-generated constructor stub
        
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		doPost(request, response);
		
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		
		String type = request.getParameter("type");
		String userID = request.getParameter("userID");
		String accessToken = request.getParameter("accessToken");
		
		ServletContext context = request.getSession().getServletContext();
		resourceURL = context.getResource("/WEB-INF/certificate.p12");
			
		if (type.equals("rideRequest"))
			rideHandler(request, response, userID, accessToken);
		else if (type.equals("driveRequest"))
			driveHandler(request, response, userID, accessToken);
		
		//Once a selection is made, notify the selectee, and update the DB to reflect the match
		else if(type.equals("driverSelection"))
			driverSelection(request.getParameter("driverID"), userID, request.getParameter("addedTime"));
		else if(type.contentEquals("riderSelection"))
			riderSelection(request.getParameter("riderID"), userID, request.getParameter("addedTime"));
		
		//Once the selectee accepts the ride, create a trip and delete the request
		else if(type.equals("accept")){
			String requesterID = request.getParameter("requesterID");
			String requestType = request.getParameter("requestType");
			accept(requesterID, userID, requestType);
		}
		else if(type.equals("rideReject")){
			String requesterID = request.getParameter("requesterID");
			dbController.deletePendingResponse(requesterID, userID);
			
			//Notify requester that they were rejected
			rideRejectionNotification(requesterID);
		}else if(type.equals("driveReject")){
			String requesterID = request.getParameter("requesterID");
			dbController.deletePendingResponse(requesterID, userID);
			
			//Notify requester that they were rejected
			driveRejectionNotification(requesterID);
		}
		
		//If trip is cancelled, notify the other party and delete the trip
		else if(type.equals("end")){
			String riderID = request.getParameter("riderID");
			String driverID = request.getParameter("driverID");
			endTrip(riderID, driverID);
		
		}
		//When app polls the server, it will check to see all pending info, send info accordingly:
		//send If they have any trips currently, otherwise
		//send Pending responses if they have any
		//If not, send requests in the queue

		//Need authentication with accessToken here.
		else if(type.equals("update"))
			response.getWriter().print(getUpdate(userID));
		
	}
	
	private void accept(String requesterID, String requesteeID, String type){
		//Create trip and delete pending reponse
		dbController.createTrip(requesterID, requesteeID);
		
		//Send notification to requester
		if(type.equals("drive"))
			driveRequestAcceptance(requesterID);
		else
			rideRequestAcceptance(requesterID);
		
		
	}
	
	private void endTrip(String riderID, String driverID){
		dbController.removeTrip(riderID, driverID);
		endTripNotification(riderID);
		endTripNotification(driverID);
		
	}
	
	//Check user status in the system
	private String getUpdate(String userID) {
		Trip trip;
		
		if((trip = dbController.getTrip(userID)) != null){
			trip.mutualFriends = FacebookHelper.test(trip.driver.accessToken, trip.rider.getID());

			Gson gson = new Gson();
			String json = gson.toJson(trip);
			
			return json;
		}
		
		pendingResponse result;
		
		//Check for offers to return to user
		if((result = dbController.getPendingResponses(userID)) != null){

			JSONObject obj = new JSONObject();

			if(result.type.equals("drive")){
				RideRequest rr = dbController.getRideRequest(userID);

				obj.put("mutualFriends", FacebookHelper.test(dbController.getAccessToken(result.requesterID), result.requesteeID));
				obj.put("type", "driveOffer");
				obj.put("driverID", result.requesterID);
				obj.put("driverAccessToken", dbController.getAccessToken(result.requesterID));
				obj.put("origin", rr.origin);
				obj.put("destination", rr.destination);
				obj.put("duration", rr.duration);

				return obj.toJSONString();
			}else{
				//We need driver s/e as well as that of the rider. We also need total time of trip, which means getting
				//the driveRequest time and the addedTime from the pendingResponse

				ArrayList<String> dr = dbController.getDriveRequest(userID);
				RideRequest rr = dbController.getRideRequest(result.requesterID);
				obj.put("mutualFriends", FacebookHelper.test(dbController.getAccessToken(result.requesterID), result.requesteeID));
				obj.put("type", "rideRequest");
				obj.put("riderID", result.requesterID);
				obj.put("riderOrigin", rr.origin);
				obj.put("riderDestination", rr.destination);
				obj.put("addedTime", result.addedTime);
				obj.put("driverOrigin", dr.get(1));
				obj.put("driverDestination", dr.get(2));
				obj.put("driverDuration", dr.get(4));
				obj.put("accessToken", dbController.getAccessToken(result.requesterID));

				return obj.toJSONString();

			}

		}
		
		else
			return null;
		/*
		else{
			return dbController.getQueueRequests(userID);
		}
		*/
	}
	
	private void endTripNotification(String userID){
		//Get driver apnsToken from database
		String deviceToken = dbController.getUserApnsToken(userID);
		
		if (deviceToken != null && deviceToken.length() > 1){
			ApnsService service =
				    APNS.newService()
				    .withCert(resourceURL.getPath(), "presten2")
				    .withSandboxDestination()
				    .build();
			
			String payload = APNS.newPayload().alertBody("Your trip has completed.").build();
			service.push(deviceToken, payload);

			System.out.println("A notification should have been sent to rider.");
		}
		
	}
	
	private void rideRejectionNotification(String userID){
		//Get driver apnsToken from database
		String deviceToken = dbController.getUserApnsToken(userID);
		
		if (deviceToken != null && deviceToken.length() > 1){
			ApnsService service =
				    APNS.newService()
				    .withCert(resourceURL.getPath(), "presten2")
				    .withSandboxDestination()
				    .build();
			
			String payload = APNS.newPayload().alertBody("Your ride request was denied.").build();
			service.push(deviceToken, payload);

			System.out.println("A notification should have been sent to rider.");
		}
		
	}
	
	private void driveRejectionNotification(String userID){
		//Get driver apnsToken from database
		String deviceToken = dbController.getUserApnsToken(userID);
		
		if (deviceToken != null && deviceToken.length() > 1){
			ApnsService service =
				    APNS.newService()
				    .withCert(resourceURL.getPath(), "presten2")
				    .withSandboxDestination()
				    .build();
			
			String payload = APNS.newPayload().alertBody("Your offer to drive was denied.").build();
			service.push(deviceToken, payload);

			System.out.println("A notification should have been sent to driver.");
		}
		
	}
	
	//Notify driver that a rider accepted their drive request
	private void driveRequestAcceptance(String driverID){
		//Get driver apnsToken from database
		String deviceToken = dbController.getUserApnsToken(driverID);
		
		if (deviceToken != null && deviceToken.length() > 1){
			ApnsService service =
				    APNS.newService()
				    .withCert(resourceURL.getPath(), "presten2")
				    .withSandboxDestination()
				    .build();
			
			String payload = APNS.newPayload().alertBody("A rider has accepted your offer to drive them.").build();
			service.push(deviceToken, payload);

			System.out.println("A notification should have been sent to driver.");
		}
		
	}
	
	//Notify rider that a driver accepted their ride request
	private void rideRequestAcceptance(String riderID){
		//Get driver apnsToken from database
		String deviceToken = dbController.getUserApnsToken(riderID);
		
		if (deviceToken != null && deviceToken.length() > 1){
			ApnsService service =
				    APNS.newService()
				    .withCert(resourceURL.getPath(), "presten2")
				    .withSandboxDestination()
				    .build();
			
			String payload = APNS.newPayload().alertBody("A driver has accepted your request to ride with them.").build();
			service.push(deviceToken, payload);

			System.out.println("A notification should have been sent to rider.");
		}
		
	}
	
	//Notify driver that a rider has requested them
	private void driverSelection(String driverID, String riderID, String addedTime){
		
		//Create pendingResponse using info from rider and driver
		dbController.createPendingResponse(riderID, driverID, addedTime, "ride");
		
		//Get driver apnsToken from database
		String deviceToken = dbController.getUserApnsToken(driverID);
		
		if (deviceToken != null && deviceToken.length() > 1){
			ApnsService service =
				    APNS.newService()
				    .withCert(resourceURL.getPath(), "presten2")
				    .withSandboxDestination()
				    .build();
			
			String payload = APNS.newPayload().alertBody("You have received a ride request.").build();
			service.push(deviceToken, payload);

			System.out.println("A notification should have been sent to driver.");
		}
		
	}
	
	//Notify rider that they have been requested by a driver
	private void riderSelection(String riderID, String driverID, String addedTime){
		
		//Create trip using info from rider and driver
		dbController.createPendingResponse(driverID, riderID, addedTime, "drive");
		
		//Get driver apnsToken from database
		String deviceToken = dbController.getUserApnsToken(riderID);
		
		if (deviceToken != null && deviceToken.length() > 1){
			ApnsService service =
				    APNS.newService()
				    .withCert(resourceURL.getPath(), "presten2")
				    .withSandboxDestination()
				    .build();
			
			String payload = APNS.newPayload().alertBody("You have been offered a ride.").build();
			service.push(deviceToken, payload);

			System.out.println("A notification should have been sent to rider.");
		}
		
	}
	
	private void rideHandler(HttpServletRequest request, HttpServletResponse response, String userID, String accessToken) throws ServletException, IOException {
		
		String origin = request.getParameter("origin");
		String destination = request.getParameter("destination");	
		String apns = request.getParameter("apnsToken");
		Rider rider = new Rider(userID, accessToken, apns, origin, destination, null);
		
		storeRider(rider); //Store the ride request in the database
		loadDrivers(); //Load the drivers into the driver list
		
		//Prints the user IDs of the drivers to the response
		String drivers = rideMatch(rider);
		response.getWriter().print(drivers);
		
	}
	
	private void driveHandler(HttpServletRequest request, HttpServletResponse response, String userID, String accessToken) throws ServletException, IOException {
		
		String origin = request.getParameter("origin");
		String destination = request.getParameter("destination");	
		int limit = Integer.parseInt(request.getParameter("limit"));
		String apns = request.getParameter("apnsToken");
		Driver driver = new Driver(userID, accessToken, apns, limit, origin, destination);
		
		storeDriver(driver);
		loadRiders(); //Load the riders into the rider list
		
		//Prints the user IDs of the drivers to the response
		String riders = driveMatch(driver);
		response.getWriter().print(riders);
		
	}
	
	private long getAddedTime(Driver driver, Rider rider){
		GeoApiContext context = new GeoApiContext().setApiKey("AIzaSyC5BL3tnMx8WrCabEGg6Ebx--f6fDraHzg");
		DirectionsRoute[] routes;
		long seconds;
		long addedTime;

		//Get the route distance for when the driver picks up the rider
		try {
			routes = DirectionsApi.newRequest(context).origin(driver.getOrigin())
					.destination(driver.getDestination()).waypoints(rider.getOrigin(), rider.getDestination())
					.await();

			seconds = routes[0].legs[0].duration.inSeconds + routes[0].legs[1].duration.inSeconds
					+ routes[0].legs[2].duration.inSeconds;
			addedTime = seconds - driver.getDuration();
			
			return addedTime/60;		

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return 0;
	}
	
	private void storeRider(Rider rider){
		dbController.storeRider(rider);
		
	}
	
	private void storeDriver(Driver driver){
		dbController.storeDriver(driver);
		
	}
	
	private String rideMatch(Rider rider){
		
		String returnString = "";
		
		//In order to access the API
		GeoApiContext context = new GeoApiContext().setApiKey("AIzaSyC5BL3tnMx8WrCabEGg6Ebx--f6fDraHzg");
		
		DirectionsRoute[] routes;
		long seconds;

		// For every driver in the system, check if the rider is within their
		// set limit
		for (Driver driver : drivers) {
			try {
				//Get the route distance for when the driver picks up the rider
				routes = DirectionsApi.newRequest(context).origin(driver.getOrigin())
						.destination(driver.getDestination()).waypoints(rider.getOrigin(), rider.getDestination())
						.await();

				seconds = routes[0].legs[0].duration.inSeconds + routes[0].legs[1].duration.inSeconds
						+ routes[0].legs[2].duration.inSeconds;
				
				// This is checking to see that the amount of time the driver must go out of their way
				// is less than their set limit
				if (seconds - driver.getDuration() <= driver.getLimit() * 60) {
					String accessToken = dbController.getAccessToken(driver.getID());
					
					int addedTime = (int)((seconds - driver.getDuration())/60);
					//userID;accessToken;addedTime_
					returnString += driver.getID() + ";" + accessToken + ";" + addedTime + ";" + FacebookHelper.test(accessToken, rider.getID()) + "_";
				}

			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

		return returnString;
		
	}
	
	private String driveMatch(Driver driver){
		
		String returnString = "";
		
		//In order to access the API
		GeoApiContext context = new GeoApiContext().setApiKey("AIzaSyC5BL3tnMx8WrCabEGg6Ebx--f6fDraHzg");
		
		DirectionsRoute[] routes;
		long seconds;

		// For every driver in the system, check if the rider is within their
		// set limit
		for (Rider rider : riders) {
			try {
				//Get the route distance for when the driver picks up the rider
				routes = DirectionsApi.newRequest(context).origin(driver.getOrigin())
						.destination(driver.getDestination()).waypoints(rider.getOrigin(), rider.getDestination())
						.await();

				seconds = routes[0].legs[0].duration.inSeconds + routes[0].legs[1].duration.inSeconds
						+ routes[0].legs[2].duration.inSeconds;
				
				// This is checking to see that the amount of time the driver must go out of their way
				// is less than their set limit
				if (seconds - driver.getDuration() <= driver.getLimit() * 60) {
					int addedTime = (int)((seconds-driver.getDuration())/60);
					String accessToken = dbController.getAccessToken(rider.getID());
					
					//id;accessToken;origin;destination;addedTime_
					returnString += rider.getID() + ";" + accessToken + ";" 
							+ rider.getOrigin() + ";" + rider.getDestination() + ";" + addedTime + ";" + FacebookHelper.test(driver.accessToken, rider.getID()) + "_";

				}

			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

		return returnString;
		
	}
	
	private void loadDrivers(){
		drivers = dbController.getAllDrivers();

	}
	
	private void loadRiders(){
		riders = dbController.getAllRiders();
		
	}

}
