package demz;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.mysql.jdbc.jdbc2.optional.*;

import com.google.maps.GeoApiContext;
import com.google.maps.GeocodingApi;
import com.google.maps.DirectionsApi;
import com.google.maps.model.DirectionsRoute;
import com.google.maps.model.GeocodingResult;

/**
 * Servlet implementation class YokweServlet
 */
@WebServlet("/YokweServlet")
public class YokweServlet extends HttpServlet {
	
	private ArrayList<Driver> drivers= new ArrayList<Driver>();
	
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

		String id = request.getParameter("id");
		String origin = request.getParameter("origin");
		String destination = request.getParameter("destination");	
		Rider rider = new Rider(id, origin, destination);
		
		loadDrivers();
		
		String driverNames = rideMatch(rider);
		response.getWriter().println(driverNames);
		response.getWriter().println("Done.");
		
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		
		String id = request.getParameter("id");
		String origin = request.getParameter("origin");
		String destination = request.getParameter("destination");	
		Rider rider = new Rider(id, origin, destination);
		
		loadDrivers();
		
		//Prints in response the user IDs of the drivers
		String drivers = rideMatch(rider);
		response.getWriter().println(drivers);
		
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
				if ((seconds - driver.getDuration()) <= driver.getLimit() * 60) {
					returnString += driver.getID() + ";";
					System.out.println(Math.ceil((seconds - driver.getDuration()) / 60));
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
			
			System.out.println("Got here first");
			MysqlDataSource dataSource = new MysqlDataSource();
			dataSource.setUser("demz");
			dataSource.setPassword("Iheartnewyork!1");
			dataSource.setServerName("myfirstdatabase.cgrwwpjxf5ev.us-west-2.rds.amazonaws.com");
			dataSource.setPort(3306);
			//dataSource.setDatabaseName("myfirstdatabase");

			Connection conn = dataSource.getConnection();
			java.sql.Statement stmt = conn.createStatement();
			stmt.executeQuery("USE demzdb");
			ResultSet rs = stmt.executeQuery("SELECT * FROM driver");

			while (rs.next()) {
				
				String id = rs.getString("id");
	            int limit = rs.getInt("timeLimit");
	            String origin = rs.getString("origin");
	            String destination = rs.getString("destination");

				Driver newb = new Driver(id, limit, origin, destination);
				driverList.add(newb);
			}
			
			rs.close();
			stmt.close();
			conn.close();
			drivers = driverList;
			System.out.println("Drivers added successfully.");

		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
