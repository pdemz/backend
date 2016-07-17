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
import com.notnoop.exceptions.InvalidSSLConfig;
import com.google.maps.GeoApiContext;
import com.google.maps.DirectionsApi;
import com.google.maps.model.DirectionsRoute;
import com.google.gson.*;

import org.json.simple.*;

import org.apache.commons.dbutils.*;

/**
 * Servlet implementation class YokweServlet
 */
@WebServlet("/atlas")
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
		
		//Authentication
		if(userID != null && !FacebookHelper.authenticated(accessToken, userID)){
			return;

		}else if(userID == null){
			String email = request.getParameter("email");
			String password = request.getParameter("password");

			if(!dbController.authenticateWithEmailAndPassword(email, password)){
				response.sendError(400, "Invalid credentials");
				if(type.equals("authenticateEmail")){
					response.getWriter().print("fail");
				}
				return;
			}

			if(type.equals("authenticateEmail")){
				response.getWriter().print("success");
				return;
			}
			
			User uu = dbController.getUserWithEmail(email);
			userID = uu.id;
			
		}
					
		if (type.equals("rideRequest"))
			rideHandler(request, response, userID, accessToken);
		
		else if (type.equals("driveRequest"))
			driveHandler(request, response, userID, accessToken);
		
		//Once a selection is made, notify the selectee, and update the DB to reflect the match
		else if(type.equals("driverSelection"))
			driverSelection(request.getParameter("driverID"), userID,
					request.getParameter("addedTime"), Integer.parseInt(request.getParameter("price")));
		
		else if(type.equals("riderSelection"))
			riderSelection(request.getParameter("riderID"), userID,
					request.getParameter("addedTime"), Integer.parseInt(request.getParameter("price")));
		
		//Once the selectee accepts the ride, create a trip and delete the request
		else if(type.equals("accept")){
			String requesterID = request.getParameter("requesterID");
			String requestType = request.getParameter("requestType");
			accept(requesterID, userID, requestType);
		}
		
		else if(type.equals("rideReject")){
			String requesterID = request.getParameter("requesterID");
			dbController.deletePendingResponse(requesterID, userID);
			rideRejectionNotification(requesterID);
			
		}else if(type.equals("driveReject")){
			String requesterID = request.getParameter("requesterID");
			dbController.deletePendingResponse(requesterID, userID);
			driveRejectionNotification(requesterID);
		}
		//If trip is ended, notify the other party, make the transaction and delete the trip
		else if(type.equals("end")){
			String riderID = request.getParameter("riderID");
			String driverID = request.getParameter("driverID");
			response.getWriter().print(makePayment(riderID, driverID));
			endTrip(riderID, driverID);
				
		}
		//If trip is canceled, notify the other party and delete the trip. No transaction is made
		else if(type.equals("cancel")){
			String riderID = request.getParameter("riderID");
			String driverID = request.getParameter("driverID");
			cancelTrip(riderID, driverID, userID);
		
		}else if(type.equals("pickUp")){
			String riderID = request.getParameter("riderID");
			pickUp(riderID, userID);
		}else if(type.equals("start")){
			String riderID = request.getParameter("riderID");
			startTrip(riderID, userID);

		}else if(type.equals("crossTrack")){
			//Double ct = DistanceHelper.bearing(Math.toRadians(20), Math.toRadians(35), Math.toRadians(-172), Math.toRadians(-135));
			//Double ct = DistanceHelper.crossTrack(39.158168, 39.158168, 39.952584, -75.524368, -75.524368, -75.165222);
			Double ib = DistanceHelper.bearing(50, 50, 5, 3);
			response.getWriter().println(ib);
		}
		
		//When app polls the server, it will check to see all pending info, send info accordingly:
		//send If they have any trips currently, otherwise
		//send Pending responses if they have any
		//If not, send requests in the queue

		//Need authentication with accessToken here eventually.
		else if(type.equals("update")){
			response.getWriter().print(getUpdate(userID));
		}
		else if(type.equals("review")){
			Review rr = new Review();
			rr.reviewerID = userID;
			rr.userID = request.getParameter("revieweeID");
			rr.stars = Integer.parseInt(request.getParameter("stars"));
			rr.review = request.getParameter("review");
			rr.type = request.getParameter("reviewType");
			
			dbController.createReview(rr);
			
		}
		else if(type.equals("apns")){
			String message = request.getParameter("message");
			sendNotification(userID, message);
		}
	
	}
	
	private void accept(String requesterID, String requesteeID, String type){
		//Create trip and delete pending reponse
		dbController.createTrip(requesterID, requesteeID);
				
		//Send notification to requester
		if(type.equals("drive")){
			driveRequestAcceptance(requesterID);
		}
		else{
			rideRequestAcceptance(requesterID);
		}
		
		
	}
	
	private void startTrip(String riderID, String driverID){
		dbController.updateTripStatus(driverID, riderID, "trip", "leg1");
		sendNotification(riderID, "The driver has started the trip and is en route.");
		
	}
	
	private void pickUp(String riderID, String driverID){
		dbController.updateTripStatus(driverID, riderID, "trip", "leg2");
		sendNotification(riderID, "The driver has arrived.");
		
	}
	
	private void endTrip(String riderID, String driverID){
		dbController.updateTripStatus(driverID, riderID, "history", "completed");
		dbController.removeTrip(riderID, driverID);
		endTripNotification(riderID);
		endTripNotification(driverID);
		
	}
	
	private void cancelTrip(String riderID, String driverID, String senderID){
		dbController.updateTripStatus(driverID, riderID, "history", "cancelled");
		dbController.removeTrip(riderID, driverID);
		if(riderID.equals(senderID)){
			sendNotification(driverID, "The rider cancelled your trip.");
			sendNotification(riderID, "Your trip has been cancelled successfully.");
			
		}
		else{
			sendNotification(riderID, "The driver cancelled your trip.");
			sendNotification(driverID, "Your trip has been cancelled successfully.");

		}
		
	}
	
	//Check user status in the system
	private String getUpdate(String userID) {
		String[] reviewInfo;
		if((reviewInfo = dbController.getIncompleteReview(userID)) != null){
			//Returns a string array of size 2, the reviewee id and what the reviewee was acting as (rider or driver)
			JSONObject obj = new JSONObject();
			
			User reviewee = dbController.getUser(reviewInfo[0]);
			
			obj.put("revieweeID", reviewInfo[0]);
			obj.put("accessToken", reviewee.accessToken);
			obj.put("review_type", reviewInfo[1]);
			obj.put("type", "review");
			
			return obj.toJSONString();
			
		}
		
		Trip trip;
		
		if((trip = dbController.getTrip(userID)) != null){
			trip.mutualFriends = "0";
			if(trip.driver.accessToken != null){
				trip.mutualFriends = FacebookHelper.test(trip.driver.accessToken, trip.rider.getID());
			}
			
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
				String accessToken = dbController.getAccessToken(result.requesterID);
				if(accessToken != null){
					obj.put("mutualFriends", FacebookHelper.test(accessToken, result.requesteeID));
				}else{
					obj.put("mutualFriends",  "0");
				}
				obj.put("type", "driveOffer");
				obj.put("driverID", result.requesterID);
				obj.put("driverAccessToken", dbController.getAccessToken(result.requesterID));
				obj.put("origin", rr.origin);
				obj.put("destination", rr.destination);
				obj.put("duration", rr.duration);
				obj.put("price", result.price);

				return obj.toJSONString();
			}else{
				//We need driver s/e as well as that of the rider. We also need total time of trip, which means getting
				//the driveRequest time and the addedTime from the pendingResponse

				ArrayList<String> dr = dbController.getDriveRequest(userID);
				RideRequest rr = dbController.getRideRequest(result.requesterID);
				String accessToken = dbController.getAccessToken(result.requesterID);
				if(accessToken != null){
					obj.put("mutualFriends", FacebookHelper.test(accessToken, result.requesteeID));
				}else{
					obj.put("mutualFriends",  "0");
				}
				obj.put("type", "rideRequest");
				obj.put("riderID", result.requesterID);
				obj.put("riderOrigin", rr.origin);
				obj.put("riderDestination", rr.destination);
				obj.put("addedTime", result.addedTime);
				obj.put("driverOrigin", dr.get(1));
				obj.put("driverDestination", dr.get(2));
				obj.put("driverDuration", dr.get(4));
				obj.put("accessToken", accessToken);
				obj.put("price", result.price);

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
	
	private String makePayment(String riderID, String driverID){
		
		//Get customer and connect tokens from db along with trip price
		User rider = dbController.getUser(riderID);
		User driver = dbController.getUser(driverID);
		Trip tt = dbController.getTrip(riderID);
		
		//Make the payment
		StripeHelper sh = new StripeHelper();
		return sh.makePayment(driver.accountToken, rider.customerToken, tt.price);
		
	}
	
	private void endTripNotification(String userID){
		//Get driver apnsToken from database
		String deviceToken = dbController.getUserApnsToken(userID);
		
		if (deviceToken != null && deviceToken.length() > 1){
			ApnsService service =
				    APNS.newService()
				    .withCert(resourceURL.getPath(), "presten2")
				    .withProductionDestination()
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
				    .withProductionDestination()
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
				    .withProductionDestination()
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
				    .withProductionDestination()
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
				    .withProductionDestination()
				    .build();
			
			String payload = APNS.newPayload().alertBody("A driver has accepted your request to ride with them.").build();
			service.push(deviceToken, payload);

			System.out.println("A notification should have been sent to rider.");
		}
		
	}
	
	//Notify driver that a rider has requested them
	private void driverSelection(String driverID, String riderID, String addedTime, int price){
		
		//Create pendingResponse using info from rider and driver
		dbController.createPendingResponse(riderID, driverID, addedTime, price, "ride");
		
		//Get driver apnsToken from database
		String deviceToken = dbController.getUserApnsToken(driverID);
		
		if (deviceToken != null && deviceToken.length() > 1){
			ApnsService service =
				    APNS.newService()
				    .withCert(resourceURL.getPath(), "presten2")
				    .withProductionDestination()
				    .build();
			
			String payload = APNS.newPayload().alertBody("You have received a ride request.").build();
			service.push(deviceToken, payload);

			System.out.println("A notification should have been sent to driver.");
		}
		
	}
	
	//Notify rider that they have been requested by a driver
	private void riderSelection(String riderID, String driverID, String addedTime, int price){
		
		//Create trip using info from rider and driver
		dbController.createPendingResponse(driverID, riderID, addedTime, price, "drive");
		
		//Get driver apnsToken from database
		String deviceToken = dbController.getUserApnsToken(riderID);
		
		if (deviceToken != null && deviceToken.length() > 1){
			ApnsService service =
				    APNS.newService()
				    .withCert(resourceURL.getPath(), "presten2")
				    .withProductionDestination()
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
		
		System.out.println("Here's your sign: " + accessToken);

		
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
		long distance;

		// For every driver in the system, check if the rider is within their
		// set limit
		for (Driver driver : drivers) {
			if(Math.abs(DistanceHelper.getCrossTrackFromDriverAndRider(driver, rider)) < 30){
				try {
					//Get the route distance for when the driver picks up the rider
					routes = DirectionsApi.newRequest(context).origin(driver.getOrigin())
							.destination(driver.getDestination()).waypoints(rider.getOrigin(), rider.getDestination())
							.await();

					seconds = routes[0].legs[0].duration.inSeconds + routes[0].legs[1].duration.inSeconds
							+ routes[0].legs[2].duration.inSeconds;

					distance = routes[0].legs[0].distance.inMeters + routes[0].legs[1].distance.inMeters
							+ routes[0].legs[2].distance.inMeters;

					int riderTime = (int) (routes[0].legs[1].duration.inSeconds/60);
					int riderDistance = (int) (routes[0].legs[1].distance.inMeters/1609.344);

					// This is checking to see that the amount of time the driver must go out of their way
					// is less than their set limit
					if (seconds - driver.getDuration() <= driver.getLimit() * 60 && !driver.getID().equals(rider.getID())) {
						User uu = dbController.getUser(driver.getID());

						int addedTime = (int)((seconds - driver.getDuration())/60);
						//int riderTime = (int) (seconds/60 - addedTime);

						int addedDistance = (int)(distance/1609.344 - driver.getDistance());
						//int riderDistance = driver.getDistance() - addedDistance;

						System.out.println(addedTime + " " + addedDistance + " " + riderTime + " " + riderDistance);
						int price = getPrice(addedTime, addedDistance, riderTime, riderDistance);

						String mutualFriends = "0";
						if (uu.accessToken != null){
							mutualFriends = FacebookHelper.test(uu.accessToken, rider.getID());	
						}

						//userID;accessToken;addedTime;mutualFriends;price_
						returnString += driver.getID() + ";" + uu.accessToken + ";" + addedTime + ";" + mutualFriends + ";" + price + ";" + uu.aboutMe + "_";
					}

				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
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
		long distance;

		// For every driver in the system, check if the rider is within their
		// set limit
		for (Rider rider : riders) {
			if(Math.abs(DistanceHelper.getCrossTrackFromDriverAndRider(driver, rider)) < 30){
				try {
					//Get the route distance for when the driver picks up the rider
					routes = DirectionsApi.newRequest(context).origin(driver.getOrigin())
							.destination(driver.getDestination()).waypoints(rider.getOrigin(), rider.getDestination())
							.await();

					seconds = routes[0].legs[0].duration.inSeconds + routes[0].legs[1].duration.inSeconds
							+ routes[0].legs[2].duration.inSeconds;

					distance = routes[0].legs[0].distance.inMeters + routes[0].legs[1].distance.inMeters
							+ routes[0].legs[2].distance.inMeters;

					int riderTime = (int) (routes[0].legs[1].duration.inSeconds/60);
					int riderDistance = (int) (routes[0].legs[1].distance.inMeters/1609.344);

					// This is checking to see that the amount of time the driver must go out of their way
					// is less than their set limit
					if (seconds - driver.getDuration() <= driver.getLimit() * 60 && !driver.getID().equals(rider.getID())) {
						int addedTime = (int)((seconds-driver.getDuration())/60);
						//int riderTime = (int) (seconds/60 - addedTime);
						int addedDistance = (int)(distance/1609.344 - driver.getDistance());
						//int riderDistance = driver.getDistance() - addedDistance;

						User uu  = dbController.getUser(rider.getID());

						System.out.println(addedTime + " " + addedDistance + " " + riderTime + " " + riderDistance);
						int price = getPrice(addedTime, addedDistance, riderTime, riderDistance);

						String mutualFriends = "0";
						if (uu.accessToken != null){
							mutualFriends = FacebookHelper.test(uu.accessToken, rider.getID());	
						}

						//id;accessToken;origin;destination;addedTime;mutualFriends;price_
						returnString += rider.getID() + ";" + uu.accessToken + ";" 
								+ rider.getOrigin() + ";" + rider.getDestination() + ";" + addedTime + ";" + mutualFriends + ";" + price + ";" + uu.aboutMe + "_";

					}

				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		return returnString;

	}
	
	private int getPrice(int addedTime, int addedDistance, int riderTime, int riderDistance){
		//miles and minutes
		return addedTime*20 + addedDistance*80 + riderTime*4 + riderDistance*16; 
	}
	
	private void loadDrivers(){
		drivers = dbController.getAllDrivers();

	}
	
	private void loadRiders(){
		riders = dbController.getAllRiders();
		
	}
	
	//Send notification to user
	private void sendNotification(String userID, String message){
		//Get driver apnsToken from database
		String deviceToken = dbController.getUserApnsToken(userID);
		
		if (deviceToken != null && deviceToken.length() > 1){
			ApnsService service;
			try {
				service = APNS.newService()
				.withCert(resourceURL.getPath(), "presten2")
				//.withCert(new FileInputStream("/home/demz/Downloads/backup stuff/certificate.p12"), "presten2")
				.withProductionDestination()
				.build();
				
				String payload = APNS.newPayload().alertBody(message).badge(1).build();
				service.push(deviceToken, payload);
			} catch (InvalidSSLConfig e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			System.out.println("A notification should have been sent.");
		}
		
	}

}
