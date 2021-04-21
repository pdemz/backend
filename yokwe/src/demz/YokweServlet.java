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
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
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
		String type = request.getParameter("type");
		String userID = request.getParameter("userID");
		String accessToken = request.getParameter("accessToken");
		
		//PhoneHelper ph = new PhoneHelper("###", "send", null);
		
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
		else if(type.equals("driverSelection")){
			driverSelection(request.getParameter("driverID"), userID,
					request.getParameter("addedTime"), Integer.parseInt(request.getParameter("price")), request);
		}
		else if(type.equals("riderSelection")){
			String price = request.getParameter("price");
			riderSelection(request.getParameter("riderID"), userID,
					request.getParameter("addedTime"), Integer.parseInt(request.getParameter("price")), request);
		}
		
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
			
		//Purely for verifying phone numbers	
		}else if(type.equals("sms")){
			String code = null;
			String number = request.getParameter("number");
			String action = request.getParameter("action");
			
			if (action.equals("verify")){
				code = request.getParameter("code");
			}
			
			PhoneHelper pp = new PhoneHelper(number, action, code);
			boolean verified = pp.handle();
			
			JSONObject json = new JSONObject();
			json.put("verified", verified);
			response.getWriter().print(json.toString());
			
		//Delete the trip or request from the DB	
		}else if(type.equals("deleteTrip")){
			String tripID = request.getParameter("tripID");
			
			//Remove the trip from the DB
			dbController.deleteTrip(tripID, userID);
			
			
		}else if(type.equals("getTrips")){
		//Get all trips and requests from the queue
			response.getWriter().print(getTripList(userID));
			
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
	
	//Return all trips for a given user that are not active or awaiting the user's response
	private String getTripList(String userID){
		ArrayList<TripStatusObject> tripOverviews = new ArrayList<TripStatusObject>();
		
		ArrayList<Trip> trips = dbController.getAllTrips(userID);

		//Build the list of info to return. It won't just be trips - that will be too much on the front end
		//to, from, status, mode, userID, accessToken
		//Statuses will be customized on the front end because thats where the Facebook api calls are made
		
		//Just send the whole trip over. Also start storing name of origin and destination in db
		for(Trip trip : trips){
			TripStatusObject tripOverview = new TripStatusObject();
			
			//User is the rider
			if(trip.riderID != null && trip.riderID.equals(userID)){
				tripOverview.userID = trip.driverID;
				tripOverview.accessToken = dbController.getAccessToken(trip.driverID);
				tripOverview.mode = "Riding";
				tripOverview.originName = MapsHelper.localityFromCoordinateString(trip.rOrigin);
				tripOverview.destinationName = MapsHelper.localityFromCoordinateString(trip.rDestination);
				tripOverview.status = trip.status;
				tripOverview.tripID = trip.id;
				
				
			//User is the driver
			}else if(trip.driverID != null && trip.driverID.equals(userID)){
				tripOverview.userID = trip.riderID;
				tripOverview.accessToken = dbController.getAccessToken(trip.riderID);
				tripOverview.mode = "Driving";
				tripOverview.originName = MapsHelper.localityFromCoordinateString(trip.dOrigin);
				tripOverview.destinationName = MapsHelper.localityFromCoordinateString(trip.dDestination);
				tripOverview.status = trip.status;
				tripOverview.tripID = trip.id;
				
			}
			
			tripOverviews.add(tripOverview);
			
		}
		
		Gson gson = new Gson();
		return gson.toJson(tripOverviews);
		
	}
	
	//This determines what screen will be displayed when the user opens the app, if their action is required or they are on trip
	private String getUpdateWithTripsDesign(String userID){
		
		//First thing we want is for the user to review the last person they rode with
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
		
		//Figure out what the user's current state is if they are on trip or if there is a request awaiting their response.
		Trip trip;
		
		if((trip = dbController.getActiveTrip(userID)) != null){
			trip.rider = dbController.getRider(trip.riderID);
			trip.driver = dbController.getDriver(trip.driverID);
			
			if(trip.driver.accessToken != null){
				trip.mutualFriends = FacebookHelper.getNumberOfMutualFriends(trip.driver.accessToken, trip.rider.getID());
			}
			
			Gson gson = new Gson();
			return gson.toJson(trip);
			
		}
		
		//If the user has a ride/drive request awaiting their response, show it. Finish this - will require changing the call in the DB
		if((trip = dbController.getPendingResponse(userID)) != null){
			trip.rider = dbController.getRider(trip.riderID);
			trip.driver = dbController.getDriver(trip.driverID);
			
			if(trip.driver.accessToken != null){
				trip.mutualFriends = FacebookHelper.getNumberOfMutualFriends(trip.driver.accessToken, trip.rider.getID());
			}
			
			Gson gson = new Gson();
			return gson.toJson(trip);
			
		}
		
		return null;
		
		
	}
	
	//Send data to client to tell it what screen to display on startup
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
		
		//If the user is on trip, return the trip. Status should not equal "waiting"
		Trip trip;
		
		if((trip = dbController.getTrip(userID)) != null){
			if(trip.driver.accessToken != null){
				trip.mutualFriends = FacebookHelper.getNumberOfMutualFriends(trip.driver.accessToken, trip.rider.getID());
			}
			
			Gson gson = new Gson();
			String tripJson = gson.toJson(trip);
			
			return tripJson;
		}
		
		pendingResponse result;
		
		//Check for offers to return to user
		if((result = dbController.getPendingResponses(userID)) != null){

			JSONObject obj = new JSONObject();

			if(result.type.equals("drive")){
				RideRequest rr = dbController.getRideRequest(userID);
				String accessToken = dbController.getAccessToken(result.requesterID);
				if(accessToken != null){
					obj.put("mutualFriends", FacebookHelper.getNumberOfMutualFriends(accessToken, result.requesteeID));
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
					obj.put("mutualFriends", FacebookHelper.getNumberOfMutualFriends(accessToken, result.requesteeID));
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

		}else
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
		sendNotification(userID, "You have completed your trip.");

		
	}
	
	private void rideRejectionNotification(String userID){
		sendNotification(userID, "Your ride request was denied.");
		
	}
	
	private void driveRejectionNotification(String userID){
		sendNotification(userID, "Your offer to drive was denied.");
		
	}
	
	//Notify driver that a rider accepted their drive request
	private void driveRequestAcceptance(String driverID){
		sendNotification(driverID, "A rider has accepted your offer to drive them.");
		
	}
	
	//Notify rider that a driver accepted their ride request
	private void rideRequestAcceptance(String riderID){
		sendNotification(riderID, "A driver has accepted your request to ride with them.");
		
	}
	
	//Notify driver that a rider has requested them. The rider selected the driver and is the one submitting this http request
	private void driverSelection(String driverID, String riderID, String addedTime, int price, HttpServletRequest request){
		
		System.out.println("added time == " + addedTime);
		
		//Create pendingResponse using info from rider and driver
		dbController.createPendingResponse(riderID, driverID, addedTime, price, "ride");		
		sendNotification(driverID, "You have received a ride request.");		
		
		//Create a trip from the attached stuff
		Trip trip = new Trip();
		trip.riderID = riderID;
		trip.driverID = driverID;
		trip.id = Integer.parseInt(request.getParameter("tripID"));
		trip.addedTime = Double.parseDouble(addedTime);
		trip.price = price;
		trip.requesteeTripID = Integer.parseInt(request.getParameter("requesteeTripID"));
		trip.requestType = "ride";
		trip.status = "pendingResponse";
		
		Trip riderTrip = new Trip();
		Trip driverTrip = new Trip();
		
		driverTrip = dbController.getTrip(trip.requesteeTripID);
		riderTrip = dbController.getTrip(trip.id);
		
		trip.dOrigin = driverTrip.dOrigin;
		trip.dDestination = driverTrip.dDestination;
		trip.rOrigin = riderTrip.rOrigin;
		trip.rDestination = riderTrip.rDestination;
		trip.riderDuration = riderTrip.duration;
		
		//recalculate and add distance and duration info to trip
		trip.addTripInfo();
		
		dbController.updateTrip(trip);
		
		
	}
	
	//Notify rider that they have been requested by a driver
	private void riderSelection(String riderID, String driverID, String addedTime, int price, HttpServletRequest request){
		
		//This is being phased out
		//Create pendingResponse using info from rider and driver -- 
		dbController.createPendingResponse(driverID, riderID, addedTime, price, "drive");
		
		//Create trip object from parameters
		Trip trip = new Trip();
		trip.riderID = riderID;
		trip.driverID = driverID;
		trip.id = Integer.parseInt(request.getParameter("tripID"));
		trip.addedTime = Integer.parseInt(addedTime);
		trip.price = price;
		trip.requesteeTripID = Integer.parseInt(request.getParameter("requesteeTripID"));
		trip.requestType = "drive";
		trip.status = "pendingResponse";
		
		Trip riderTrip = new Trip();
		Trip driverTrip = new Trip();
		
		driverTrip = dbController.getTrip(trip.id);
		riderTrip = dbController.getTrip(trip.requesteeTripID);
		
		trip.dOrigin = driverTrip.dOrigin;
		trip.dDestination = driverTrip.dDestination;
		trip.rOrigin = riderTrip.rOrigin;
		trip.rDestination = riderTrip.rDestination;
		trip.riderDuration = riderTrip.duration;
		
		//recalculate and add distance and duration info to trip (probably could/should recalculate price here too)
		trip.addTripInfo();
		
		dbController.updateTrip(trip);
		
		sendNotification(riderID, "You have been offered a ride.");
		
	}
	
	private void rideHandler(HttpServletRequest request, HttpServletResponse response, String userID, String accessToken) throws ServletException, IOException {
		
		System.out.println("Got into ride handler");
		
		String origin = request.getParameter("origin");
		String destination = request.getParameter("destination");	
		String apns = request.getParameter("apnsToken");
		
		Rider rider = new Rider(userID, accessToken, apns, origin, destination, null);
		
		//Create a new trip here
		Trip trip = dbController.createTripFromRideRequest(rider);
		System.out.println(trip.id);
		
		storeRider(rider); //Store the ride request in the database
		loadDrivers(); //Load the drivers into the driver list
		
		//Prints the user IDs of the drivers to the response
		String drivers = rideMatchWithTrips(trip);
		response.getWriter().print(trip.id + "_" + drivers);
		
	}
	
	private void driveHandler(HttpServletRequest request, HttpServletResponse response, String userID, String accessToken) throws ServletException, IOException {
		
		String origin = request.getParameter("origin");
		String destination = request.getParameter("destination");	
		int limit = Integer.parseInt(request.getParameter("limit"));
		String apns = request.getParameter("apnsToken");
		Driver driver = new Driver(userID, accessToken, apns, limit, origin, destination);
		
		//Create a new trip
		Trip trip = dbController.createTripFromDriveRequest(driver);
		
		storeDriver(driver);
		loadRiders(); //Load the riders into the rider list
		
		//Prints the user IDs of the drivers to the response
		String riders = driveMatchWithTrips(trip);
		response.getWriter().print(trip.id + "_" + riders);
		
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
	
private String driveMatchWithTrips(Trip driverTrip){
		
		String returnString = "";
		
		ArrayList<Trip> rideRequests = dbController.getAllRideRequests();
		
		//In order to access the API
		GeoApiContext context = new GeoApiContext().setApiKey("###");
		
		DirectionsRoute[] routes;
		long seconds;
		long distance;
		
		// For every trip in the system with status "driveRequest", check if the rider is within 100 miles of track. This heuristic will be decreased as the userbase grows.
		for (Trip riderTrip : rideRequests) {
			if(Math.abs(DistanceHelper.crossTrack(riderTrip, driverTrip)) < 100){
				try {
					//Get the route distance for when the driver picks up the rider
					routes = DirectionsApi.newRequest(context).origin(driverTrip.dOrigin)
							.destination(driverTrip.dDestination).waypoints(riderTrip.rOrigin, riderTrip.rDestination)
							.await();

					seconds = routes[0].legs[0].duration.inSeconds + routes[0].legs[1].duration.inSeconds
							+ routes[0].legs[2].duration.inSeconds;

					distance = routes[0].legs[0].distance.inMeters + routes[0].legs[1].distance.inMeters
							+ routes[0].legs[2].distance.inMeters;

					int riderTime = (int) (routes[0].legs[1].duration.inSeconds/60);
					int riderDistance = (int) (routes[0].legs[1].distance.inMeters/1609.344);

					// This is checking to see that the amount of time the driver must go out of their way
					// is less than their set limit (for now it is set to 30 minutes for everyone)
					if (seconds - driverTrip.duration <= 30 * 60 && !driverTrip.driverID.equals(riderTrip.riderID)) {
						User uu = dbController.getUser(riderTrip.riderID);

						//return addedTime in minutes
						int addedTime = (int)((seconds - driverTrip.duration)/60);

						int addedDistance = (int)(distance/1609.344 - driverTrip.distance);
						
						System.out.println(addedTime + " " + addedDistance + " " + riderTime + " " + riderDistance);
						int price = getPrice(addedTime, addedDistance, riderTime, riderDistance);

						String mutualFriends = "0";
						if (uu.accessToken != null){
							mutualFriends = FacebookHelper.getNumberOfMutualFriends(uu.accessToken, driverTrip.riderID);	
						}

						//userID;accessToken;addedTime;mutualFriends;price_
						returnString += riderTrip.riderID + ";" + uu.accessToken + ";" + addedTime + ";" + mutualFriends + ";" + price + ";" + uu.aboutMe + ";" + uu.name + ";"  + riderTrip.id + "_";
					}

				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}

		return returnString;
		
	}
	private String rideMatchWithTrips(Trip riderTrip){
		
		String returnString = "";
		
		ArrayList<Trip> driveRequests = dbController.getAllDriveRequests();
		
		//In order to access the API
		GeoApiContext context = new GeoApiContext().setApiKey("###");
		
		DirectionsRoute[] routes;
		long seconds;
		long distance;
		
		// For every trip in the system with status "driveRequest", check if the rider is within 100 miles of track. This heuristic will be decreased as the userbase grows.
		for (Trip driverTrip : driveRequests) {			
			if(Math.abs(DistanceHelper.crossTrack(riderTrip, driverTrip)) < 100){
				try {
					//Get the route distance for when the driver picks up the rider
					routes = DirectionsApi.newRequest(context).origin(driverTrip.dOrigin)
							.destination(driverTrip.dDestination).waypoints(riderTrip.rOrigin, riderTrip.rDestination)
							.await();
					
					System.out.println(driverTrip.dOrigin);
					System.out.println(driverTrip.dDestination);

					seconds = routes[0].legs[0].duration.inSeconds + routes[0].legs[1].duration.inSeconds
							+ routes[0].legs[2].duration.inSeconds;

					distance = routes[0].legs[0].distance.inMeters + routes[0].legs[1].distance.inMeters
							+ routes[0].legs[2].distance.inMeters;

					int riderTime = (int) (routes[0].legs[1].duration.inSeconds/60);
					int riderDistance = (int) (routes[0].legs[1].distance.inMeters/1609.344);

					// This is checking to see that the amount of time the driver must go out of their way
					// is less than their set limit (for now it is set to 30 minutes for everyone)
					if (seconds - driverTrip.duration <= (30 * 60) && !driverTrip.driverID.equals(riderTrip.riderID)) {
						User uu = dbController.getUser(driverTrip.driverID);
						
						//return addedTime in minutes
						int addedTime = (int)((seconds - driverTrip.duration)/60);

						//return distance in miles (seconds and meters are too precise for the app's design)
						int addedDistance = (int)(distance/1609.344 - driverTrip.distance);

						System.out.println(addedTime + " " + addedDistance + " " + riderTime + " " + riderDistance);
						int price = getPrice(addedTime, addedDistance, riderTime, riderDistance);

						String mutualFriends = "0";
						if (uu.accessToken != null){
							mutualFriends = FacebookHelper.getNumberOfMutualFriends(uu.accessToken, riderTrip.riderID);	
						}

						//userID;accessToken;addedTime;mutualFriends;price_
						returnString += driverTrip.driverID + ";" + uu.accessToken + ";" + addedTime + ";" + mutualFriends + ";" + price
								+ ";" + uu.aboutMe + ";" + uu.name + ";" + driverTrip.id + "_";
					}

				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					
				}
			}
		}

		return returnString;
		
	}
	
	private String rideMatch(Rider rider){
		
		String returnString = "";
		
		//In order to access the API
		GeoApiContext context = new GeoApiContext().setApiKey("###");
		
		DirectionsRoute[] routes;
		long seconds;
		long distance;
		
		// For every driver in the system, check if the rider is within their
		// set limit
		for (Driver driver : drivers) {
			if(Math.abs(DistanceHelper.getCrossTrackFromDriverAndRider(driver, rider)) < 100){
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
							mutualFriends = FacebookHelper.getNumberOfMutualFriends(uu.accessToken, rider.getID());	
						}

						//userID;accessToken;addedTime;mutualFriends;price_
						returnString += driver.getID() + ";" + uu.accessToken + ";" + addedTime + ";" + mutualFriends + ";" + price + ";" + uu.aboutMe + ";" + uu.name + "_";
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
		GeoApiContext context = new GeoApiContext().setApiKey("###");

		DirectionsRoute[] routes;
		long seconds;
		long distance;

		// For every driver in the system, check if the rider is within their
		// set limit
		for (Rider rider : riders) {
			if(Math.abs(DistanceHelper.getCrossTrackFromDriverAndRider(driver, rider)) < 100){
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
							mutualFriends = FacebookHelper.getNumberOfMutualFriends(driver.accessToken, rider.getID());	
							System.out.println("Mutual friends: " + mutualFriends);
						}

						//id;accessToken;origin;destination;addedTime;mutualFriends;price_
						returnString += rider.getID() + ";" + uu.accessToken + ";" 
								+ rider.getOrigin() + ";" + rider.getDestination() + ";" + addedTime + ";" + mutualFriends + ";" + price + ";" + uu.aboutMe + ";" + uu.name + "_";

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
		return addedTime*50 + addedDistance*50 + riderTime*10 + riderDistance*10; 
	}
	
	private void loadDrivers(){
		drivers = dbController.getAllDrivers();

	}
	
	private void loadRiders(){
		riders = dbController.getAllRiders();
		
	}
	
	//Send notification to user
	private void sendNotification(String userID, String message){
		
		//Get user apnsToken from database
		String deviceToken = dbController.getUserApnsToken(userID);
		
		if (deviceToken != null && deviceToken.length() > 1){
			ApnsService service;
			try {
				
				//Get apns certificate from S3 bucket
				BasicAWSCredentials credentials = new BasicAWSCredentials("###", "###");
				AmazonS3 s3Client = new AmazonS3Client(credentials);        
				S3Object object = s3Client.getObject(
				                  new GetObjectRequest("###", "###"));
				InputStream objectData = object.getObjectContent();
				
				service = APNS.newService()
				.withCert(objectData, "###")
				//.withCert(resourceURL.getPath(), "###")
				//.withCert(new FileInputStream("/home/demz/Downloads/backup stuff/certificate.p12"), "###")
				.withProductionDestination()
				.build();
				
				String payload = APNS.newPayload().alertBody(message).badge(1).sound("default").build();
				service.push(deviceToken, payload);
				
				objectData.close();

			} catch (InvalidSSLConfig | IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			System.out.println("A notification should have been sent.");
		}
		
	}

}
