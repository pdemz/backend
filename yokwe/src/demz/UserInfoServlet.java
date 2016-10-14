package demz;

import java.io.IOException;
import java.util.ArrayList;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.maps.GeoApiContext;
import com.google.maps.GeocodingApi;
import com.google.maps.model.GeocodingResult;
import com.google.maps.model.*;

/**
 * Servlet implementation class UserInfoServlet
 */
@WebServlet("/users")
public class UserInfoServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private DatabaseController dbController;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public UserInfoServlet() {
        super();
        
        dbController = new DatabaseController();
        
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub        
        String type = request.getParameter("type");

        GeoApiContext context = new GeoApiContext().setApiKey("AIzaSyC5BL3tnMx8WrCabEGg6Ebx--f6fDraHzg");
        
        if(type.equals("queue")){
        	printQueues(response);
        }
        
        //Get an arraylist of all the users from the database
        ArrayList<User> allUsers = dbController.getAllUsers();
        
        //Print out all of the relevant info - one user per line
        for (User uu: allUsers){
        	if (type.equals("all")){
            	response.getWriter().println(uu.id + "\t\t" + uu.email + "\t\t" + uu.phone);

        	}
        	
        	else if(type.equals("email")){
        		if (uu.email != null){
        			response.getWriter().println(uu.email);
        		}
        	}
        	
        }

	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		doGet(request, response);
	}
	
	private void printQueues(HttpServletResponse response) throws IOException{
        GeoApiContext context = new GeoApiContext().setApiKey("AIzaSyBrmvso2zVY_soF75Een6sI8sA5f0yGw5s");
		
    	ArrayList<Rider> riderList = dbController.getAllRiders();
    	ArrayList<Driver> driverList = dbController.getAllDrivers();
    	
    	response.getWriter().println("Rider queue: " + riderList.size() + " total \n");
    	
    	for(Rider rider: riderList){
    		LatLng riderOrigin = MapsHelper.getLatLngFromCoordinates(rider.getOrigin());
    		LatLng riderDestination = MapsHelper.getLatLngFromCoordinates(rider.getDestination());
    		
    		GeocodingResult[] results;
    		response.getWriter().print(rider.getID() + "\t");
    		
			try {
				results = GeocodingApi.reverseGeocode(context,
					    riderOrigin).await();
				System.out.println(results[0].formattedAddress);
        		response.getWriter().print(results[0].formattedAddress + " to ");
        		
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			try {
				results = GeocodingApi.reverseGeocode(context,
					    riderDestination).await();
				System.out.println(results[0].formattedAddress);
        		response.getWriter().print(results[0].formattedAddress + "\n\n");
        		
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    			
    	}
    	response.getWriter().println("----------------------");    	
    	response.getWriter().println("Driver queue: " + driverList.size() + " total \n");
    	
    	for(Driver driver: driverList){
    		LatLng driverOrigin = MapsHelper.getLatLngFromCoordinates(driver.getOrigin());
    		LatLng driverDestination = MapsHelper.getLatLngFromCoordinates(driver.getDestination());
    		
    		GeocodingResult[] results;
    		response.getWriter().print(driver.getID() + "\t");
    		
			try {
				results = GeocodingApi.reverseGeocode(context,
					    driverOrigin).await();
				System.out.println(results[0].formattedAddress);
        		response.getWriter().print(results[0].formattedAddress + " to ");
        		
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			try {
				results = GeocodingApi.reverseGeocode(context,
					    driverDestination).await();
				System.out.println(results[0].formattedAddress);
        		response.getWriter().print(results[0].formattedAddress + "\n\n");
        		
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    			
    	}
	}

}
