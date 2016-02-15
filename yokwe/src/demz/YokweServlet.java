package demz;

import java.io.*;
import java.util.ArrayList;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.sql.ResultSet;
import java.sql.SQLException;

import com.notnoop.apns.APNS;
import com.notnoop.apns.ApnsService;
import com.google.maps.GeoApiContext;
import com.google.maps.DirectionsApi;
import com.google.maps.model.DirectionsRoute;

/**
 * Servlet implementation class YokweServlet
 */
@WebServlet("/YokweServlet")
public class YokweServlet extends HttpServlet {
	
	private ArrayList<Driver> drivers= new ArrayList<Driver>();
	private ArrayList<Rider> riders = new ArrayList<Rider>();
	private DatabaseController dbController = new DatabaseController();
	
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
		String type = request.getParameter("type");
		String userID = request.getParameter("userID");
		String accessToken = request.getParameter("accessToken");
			
		if (type.equals("rideRequest"))
			rideHandler(request, response, userID, accessToken);
		else if (type.equals("driveRequest"))
			driveHandler(request, response, userID, accessToken);
		
		//Once a selection is made, notify the selectee, and update the DB to reflect the match
		else if(type.equals("driverSelection"))
			driverSelectionNotification(request.getParameter("driverID"), userID);
		else if(type.contentEquals("riderSelection"))
			riderSelectionNotification(request.getParameter("riderID"), userID);
		
		
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		FacebookHelper.test();
		
		String type = request.getParameter("type");
		String userID = request.getParameter("userID");
		String accessToken = request.getParameter("accessToken");
			
		if (type.equals("rideRequest"))
			rideHandler(request, response, userID, accessToken);
		else if (type.equals("driveRequest"))
			driveHandler(request, response, userID, accessToken);
		
		//Once a selection is made, notify the selectee, and update the DB to reflect the match
		else if(type.equals("driverSelection"))
			driverSelectionNotification(request.getParameter("driverID"), userID);
		else if(type.contentEquals("riderSelection"))
			riderSelectionNotification(request.getParameter("riderID"), userID);
		
		//Once the selectee accepts the ride, update driver availability
		else if(type.equals("accept"))
			accept(userID);
		
		//If ride is declined or canceled, rider.driverID = NULL and driver.available = true
		//Notify client. On notification received, display a view to alert what happened. If driverID == null and driver.available == true
		//Always go back to homescreen
		else if(type.equals("cancel"))
			cancel(userID);
		
		//When app polls the server, check if they have any requests and update other info
		else if(type.equals("update"))
			update(request, response);
		

	}
	
	private void accept(String userID){
		dbController.makeUnavailable(userID);
		
	}
	
	private void cancel(String userID){
		dbController.reset(userID);
	}
	
	//Check if user has received a request
	private void update(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		//will return type;userID;accessToken;origin;destination;driver.available;addedTime
		System.out.println("UID: " + request.getParameter("userID"));
		String dbString = dbController.getPartner(request.getParameter("userID"));
		String[] split = dbString.split(";");
		
		if(split[0].equals("rider")){
			Rider rider = dbController.getRider(split[1]);
			Driver driver = dbController.getDriver(request.getParameter("userID"));
			String responseString = dbString+";"+getAddedTime(driver,rider);
			response.getWriter().print(responseString);
			System.out.println(responseString);
			
		}else if(split[0].equals("driver")){
			Driver driver = dbController.getDriver(split[1]);
			Rider rider = dbController.getRider(request.getParameter("userID"));
			String responseString = dbString+";"+getAddedTime(driver,rider);
			response.getWriter().print(responseString);
			System.out.println(responseString);
			
		}else
			response.getWriter().print("nothing");	
		
	}
	
	//Notify driver that a ride has been requested
	private void driverSelectionNotification(String driverID, String userID){
		//Store driverID with rider row
		dbController.updateDriverID(userID, driverID);
		//Get apnsToken from database
		String deviceToken = dbController.getDriverApnsToken(driverID);
		
		if (deviceToken != null && deviceToken.length() > 1){
			ApnsService service =
				    APNS.newService()
				    .withCert("/home/ubuntu/jetty-distribution-9.3.5.v20151012/webapps/certificate.p12", "presten2")
				    .withProductionDestination()
				    .build();
			
			String payload = APNS.newPayload().alertBody("You have received a ride request.").build();
			service.push(deviceToken, payload);
			
			String deviceTokenTwo = dbController.getRiderApnsToken(userID);
			System.out.println("This token was retrieved: " + deviceTokenTwo);
			String payloadz = APNS.newPayload().alertBody("You just caused a push notification to be sent.").build();
			service.push(deviceTokenTwo, payloadz);
			
			System.out.println("A token should have been sent to both the rider and driver.");
		}
		
	}
	
	//Notify rider that a driver has been found
	private void riderSelectionNotification(String riderID, String driverID){
		//Update driverID for rider
		dbController.updateDriverID(riderID, driverID);
		
		//Get apnsToken from database
		String deviceToken = dbController.getRiderApnsToken(riderID);
		
		if (deviceToken != null && deviceToken.length() > 1){
			ApnsService service =
				    APNS.newService()
				    .withCert("/home/ubuntu/jetty-distribution-9.3.5.v20151012/webapps/certificate.p12", "presten2")
				    .withProductionDestination()
				    .build();
			
			String payload = APNS.newPayload().alertBody("A driver is available.").build();
			service.push(deviceToken, payload);
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
					
					//userID;accessToken_
					returnString += driver.getID() + ";" + accessToken + "_";
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
							+ rider.getOrigin() + ";" + rider.getDestination() + ";" + addedTime + "_";
				}

			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

		return returnString;
		
	}
	
	private void loadDrivers(){
		
		ArrayList<Driver> driverList= new ArrayList<Driver>();
		
		try {
			
			ResultSet rs = dbController.getAllDrivers();

			while (rs.next()) {
				
				String id = rs.getString("driverID");
	            int limit = rs.getInt("timeLimit");
	            String origin = rs.getString("origin");
	            String destination = rs.getString("destination");
	            long duration = rs.getLong("duration");

				Driver newb = new Driver(id, limit, origin, destination, duration);
				driverList.add(newb);
			}
			
			rs.close();
			drivers = driverList;
			System.out.println("Drivers added successfully.");

		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	
	private void loadRiders(){
		
		ArrayList<Rider> riderList= new ArrayList<Rider>();
		
		try {
			
			ResultSet rs = dbController.getAllRiders();

			while (rs.next()) {
				
				String id = rs.getString("riderID");
	            String origin = rs.getString("origin");
	            String destination = rs.getString("destination");

				Rider newb = new Rider(id, null, null, origin, destination, null);
				riderList.add(newb);
			}
			
			rs.close();
			riders = riderList;
			System.out.println("Riders added successfully.");

		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
