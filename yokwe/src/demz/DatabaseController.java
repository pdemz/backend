package demz;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import com.mysql.jdbc.jdbc2.optional.*;


public class DatabaseController {
	
	private MysqlDataSource dataSource;
	private Connection conn;
	java.sql.Statement stmt;
	
	public DatabaseController(){
		try {	
			dataSource = new MysqlDataSource();
			dataSource.setUser("demz");
			dataSource.setPassword("Iheartnewyork!1");
			dataSource.setServerName("myfirstdatabase.cgrwwpjxf5ev.us-west-2.rds.amazonaws.com");
			dataSource.setPort(3306);
			Connection conn = dataSource.getConnection();
			stmt = conn.createStatement();
			stmt.executeQuery("USE demzdb");
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void makeUnavailable(String userID){
		try {
			ResultSet rs;
			rs = stmt.executeQuery("SELECT * FROM rider WHERE id=" + userID);
			if(rs.next())
				stmt.executeUpdate("UPDATE driver SET available=false WHERE id=" + rs.getString("driverID"));
			else
				stmt.executeUpdate("UPDATE driver SET available=false WHERE id=" + userID);
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public void reset(String userID){
		try {
			ResultSet rs;
			rs = stmt.executeQuery("SELECT * FROM rider WHERE id="+userID);
			if(rs.next()){
				String driverID = rs.getString("driverID");
				stmt.executeUpdate("UPDATE driver SET available=true WHERE id=" + driverID);
				stmt.executeUpdate("UPDATE rider SET driverID=NULL WHERE driverID=" + driverID);
			}else
				stmt.executeUpdate("UPDATE driver SET available=true WHERE id=" + userID);
				stmt.executeUpdate("UPDATE rider SET driverID=NULL WHERE driverID=" + userID);
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public void storeRider(Rider rider){
		
		try {
			stmt.executeUpdate("INSERT INTO rider (id, accessToken, origin, destination, available, driverID, apnsToken) VALUES "
					+ "('" + rider.getID() + "', '" + rider.getAccessToken() + "', '" + rider.getOrigin() 
					+ "', '" + rider.getDestination() + "', 1, NULL, '" + rider.getApnsToken() + "') ON DUPLICATE KEY UPDATE origin='" 
					+ rider.getOrigin() + "', destination='" + rider.getDestination() + "', apnsToken='" + rider.getApnsToken() + "';");
			storeUser(rider.getID(), rider.getAccessToken(), rider.getApnsToken());
			stmt.executeUpdate("INSERT INTO rideRequest (riderId, origin, destination) VALUES "
					+ "('" + rider.getID() + "', '" + rider.getOrigin() 
					+ "', '" + rider.getDestination() + "') ON DUPLICATE KEY UPDATE origin='" 
					+ rider.getOrigin() + "', destination='" + rider.getDestination() + "';");
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public void storeDriver(Driver driver){
		
		try {
			stmt.executeUpdate("INSERT INTO driver (timeLimit, origin, destination, available, id, accessToken, apnsToken) VALUES (" 
					+ driver.getLimit() + ", '" + driver.getOrigin() + "', '" + driver.getDestination() + "', 1, '" + driver.getID() 
					+ "', '" + driver.getAccessToken() + "', '" + driver.getApnsToken() + "') ON DUPLICATE KEY UPDATE origin='" + driver.getOrigin() 
					+ "', destination='" + driver.getDestination() + "', apnsToken='" + driver.getApnsToken() +"';");

			storeUser(driver.getID(), driver.getAccessToken(), driver.getApnsToken());
			stmt.executeUpdate("INSERT INTO driveRequest (timeLimit, driverId, origin, destination, duration) VALUES "
					+ "('" + driver.getLimit() + "', '" + driver.getID() + "', '" + driver.getOrigin() 
					+ "', '" + driver.getDestination() + "', '" + driver.getDuration() + "') ON DUPLICATE KEY UPDATE origin='" 
					+ driver.getOrigin() + "', destination='" + driver.getDestination() + "', timeLimit='" + driver.getLimit() 
					+ "', duration='" + driver.getDuration() + "';");
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public String getUserApnsToken(String userID){
		String apnsToken = "";
		ResultSet rs;
		try {
			rs = stmt.executeQuery("SELECT * FROM user WHERE id=" + userID);
			if(rs.next()){
				apnsToken = rs.getString("apnsToken");
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return apnsToken;
	}
	
	public String getPartner(String userID){
		//will return type;userID;accessToken;origin;destination;addedTime
		String partnerID = "nothing";
		Boolean available = true;
		ResultSet rs;
		ResultSet rsx;
		try {
			rs = stmt.executeQuery("SELECT * FROM rider WHERE id=" + userID);
			if(rs.next()){
				partnerID = rs.getString("driverID");
				if (partnerID == null){
					return "nothing";
				}
				//If the driver is available, do the following
				Driver driver = getDriver(partnerID);
				
				rs = stmt.executeQuery("SELECT * FROM driver WHERE id=" + partnerID);
				if (rs.next())
					available = rs.getBoolean("available");
				if(available == true)
					return "driver;"+driver.getID()+";"+driver.getAccessToken()+";"+driver.getOrigin()+";"+driver.getDestination()+";true"; 
				//Otherwise, indicate that the driver.available==false, meaning they are already paired
				else
					return "driver;"+driver.getID()+";"+driver.getAccessToken()+";"+driver.getOrigin()+";"+driver.getDestination()+";false"; 
			}else{
				rsx = stmt.executeQuery("SELECT * FROM rider WHERE driverID=" + userID);
				if(rsx.next()){
					partnerID = rsx.getString("id");
					if (partnerID == null){
						return "nothing";
					}
					Rider rider = getRider(partnerID);
					rsx = stmt.executeQuery("SELECT * FROM driver WHERE id=" + userID);
					if (rsx.next())
						available = rsx.getBoolean("available");
					if(available == true)
						return "rider;"+rider.getID()+";"+rider.getAccessToken()+";"+rider.getOrigin()+";"+rider.getDestination()+";true";
					else
						return "rider;"+rider.getID()+";"+rider.getAccessToken()+";"+rider.getOrigin()+";"+rider.getDestination()+";false";
				}
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return partnerID;
	}
	
	public Driver getDriver(String userID){
		Driver driver = null;
		ResultSet rs;
		try {
			System.out.println("This is the userID being sent in: "+ userID );
			rs = stmt.executeQuery("SELECT * FROM driver WHERE id=" + userID);
			if(rs.next()){
				driver = new Driver(userID, rs.getString("accessToken"), rs.getString("apnsToken"), rs.getInt("timeLimit"), rs.getString("origin"), rs.getString("destination"));
			}
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return driver;
		
	}
	
	public Rider getRider(String userID){
		Rider rider = null;
		ResultSet rs;
		try {
			rs = stmt.executeQuery("SELECT * FROM rider WHERE id=" + userID);
			if(rs.next()){
				//public Rider(String newId, String newAccessToken, String newApnsToken, String newOrigin, String newDest, String newDriverID)
				rider = new Rider(userID, rs.getString("accessToken"), rs.getString("apnsToken"), rs.getString("origin"), rs.getString("destination"), rs.getString("driverID"));
			}
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return rider;
		
	}
	
	public String getAccessToken(String userID){
		String accessToken = null;
		try {
			ResultSet rs = stmt.executeQuery("SELECT * FROM user WHERE id=" + userID);
			if(rs.next()){
				accessToken = rs.getString("accessToken");
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return accessToken;
		
	}
	
	public ResultSet getAllDrivers() throws SQLException{
		ResultSet rs = stmt.executeQuery("SELECT * FROM driveRequest");
		return rs;
		
	}
	
	public ResultSet getAllRiders() throws SQLException{
		ResultSet rs = stmt.executeQuery("SELECT * FROM rideRequest");
		return rs;
	
	}
	
	public void close(){
		try {
			stmt.close();
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public void updateDriverID(String riderID, String driverID){
		try {
			stmt.executeUpdate("UPDATE rider SET driverID='"+driverID+"' WHERE id=" + riderID);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public String getProfile(String userID, String accessToken){
		try {
			ResultSet rs = stmt.executeQuery("SELECT * FROM user WHERE id='" + userID 
					+ "' AND accessToken='" + accessToken + "';");
			if(rs.next()){
				String aboutMe = rs.getString("aboutMe");
				String phone = rs.getString("phone");
				return aboutMe + ";" + phone;
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return null;
	}
	
	public void updateProfile(String aboutMe, String userID, String accessToken){
		aboutMe = aboutMe.replaceAll("'", "''");
		System.out.println(aboutMe);
		try {
			stmt.executeUpdate("UPDATE user SET aboutMe='"+ aboutMe +"' WHERE id='" 
					+ userID + "' AND accessToken='"
					+ accessToken +"';");
			
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public void storeUser(String id, String accessToken, String apnsToken){
		System.out.println("Store was called.");
		try{
			stmt.executeUpdate("INSERT INTO "
					+ "user (id, accessToken, apnsToken) VALUES ('" 
					+ id + "', '" + accessToken + "', '" + apnsToken + "')"
					+ "ON DUPLICATE KEY UPDATE "
					+ "id='" + id + "', accessToken='" + accessToken + "', apnsToken='" + apnsToken + "';");
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public boolean getUserExistance(String userId){
		try {
			ResultSet rs = stmt.executeQuery("SELECT * FROM user WHERE id='" + userId + "';");
			if(rs.next()){
				return true;
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return false;
	}
	
	public void updatePhone(String userID, String accessToken, String phone){
		try {
			stmt.executeUpdate("UPDATE user SET phone='"+ phone +"' WHERE id='" 
					+ userID + "' AND accessToken='"
					+ accessToken +"';");
			
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public void createTrip(String requesterID, String requesteeID){
		try {
			
			String dOrigin = null;
			String dDestination = null;
			String rOrigin = null;
			String rDestination = null;
			String driverID = null;
			String riderID = null;
			long duration = 0;
			long tripTime = 0;
			long addedTime = 0;
			
			//Get added time and determine which is driver and which is rider
			ResultSet rs = stmt.executeQuery("SELECT * FROM pendingResponse WHERE requesteeID=" + requesteeID
					+ "AND requesterID='"+requesterID+"';");
			if(rs.next()){
				addedTime = rs.getLong("addedTime");
				String type = rs.getString("type");
				
				if( type.equals("drive")){
					driverID = requesterID;
					riderID = requesteeID;
				}else{
					driverID = requesteeID;
					riderID = requesterID;
				}
			}
			
			deletePendingResponse(requesterID, requesteeID);
			
			//Get origin and destination from driver
			rs = stmt.executeQuery("SELECT * FROM driveRequest WHERE id=" + driverID);
			if(rs.next()){
				dOrigin = rs.getString("origin");
				dDestination = rs.getString("destination");
				duration = rs.getLong("duration");
				
				//Get total trip time by adding driver duration and addedTime
				tripTime = duration/60 + addedTime;
			}
			
			//Get origin and destination from rider
			rs = stmt.executeQuery("SELECT * FROM rideRequest WHERE id=" + riderID);
			if(rs.next()){
				rOrigin = rs.getString("origin");
				rDestination = rs.getString("destination");
			}
			
			//Store values into a new trip
			stmt.executeUpdate("INSERT INTO trip (riderID, driverID, dOrigin, dDestination, rOrigin, rDestination, duration) VALUES "
					+ "('" + riderID + "', '" + driverID + "', '" + dOrigin 
					+ "', '" + dDestination + "', '" + rOrigin + "', '" + rDestination + "', '" + tripTime + "');");
			
			//Delete requests, now that a trip has been created. This is what it's all about, baby!
			deleteRideRequest(riderID);
			deleteDriveRequest(driverID);
			
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
	}
	
	public void createPendingResponse(String requesterID, String requesteeID, String addedTime, String type){
		try {		
			//Store values into a new trip
			stmt.executeUpdate("REPLACE INTO pendingResponse (requesterID, requesteeID, requestType, addedTime) VALUES "
					+ "('" + requesterID + "', '" + requesteeID + "', '" + type
					+ "', '" + Long.valueOf(addedTime) + "');");

		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public void removeTrip(String riderID, String driverID){
		try{
			stmt.executeUpdate("DELETE FROM trip WHERE riderID ='"+ riderID +"' AND driverID='"+ driverID +"';");
		} catch (SQLException e){
			e.printStackTrace();
		}
	}
	
	public void deletePendingResponse(String requesterID, String requesteeID){
		try{
			stmt.executeUpdate("DELETE FROM pendingResponse WHERE requesteeID ='"+ requesteeID +"' AND requesterID='"+ requesterID +"';");
		} catch (SQLException e){
			e.printStackTrace();
		}
	}
	
	public void deleteRideRequest(String riderID){
		try{
			stmt.executeUpdate("DELETE FROM rideRequest WHERE riderID ='"+ riderID +"';");
		} catch (SQLException e){
			e.printStackTrace();
		}
	}
	
	public void deleteDriveRequest(String driverID){
		try{
			stmt.executeUpdate("DELETE FROM driveRequest WHERE driverID ='"+ driverID +"';");
		} catch (SQLException e){
			e.printStackTrace();
		}
	}
	
	//Returns trip user is currently active in, returns null if none exist
	public String getTrip(String userID){
		String returnString = "";
		try {
			ResultSet rs = stmt.executeQuery("SELECT * FROM trip WHERE riderID='" + userID + "';");
			if(!rs.isBeforeFirst())
				rs = stmt.executeQuery("SELECT * FROM trip WHERE driverID='" + userID + "';");

			if(rs.next()){
				int totalRows = rs.getMetaData().getColumnCount();
				for (int i = 0; i < totalRows; i++){
					returnString += rs.getObject(i+1) + ";";
				}
				
				return returnString;
			}
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return null;
	}
	
	//Returns any requests waiting for the users response, returns null if none exist
	public pendingResponse getPendingResponses(String userID){
		String returnString = "";
		pendingResponse pResponse = new pendingResponse();
		try {
			ResultSet rs = stmt.executeQuery("SELECT * FROM pendingResponse WHERE requesteeID='" + userID + "';");
			//if(!rs.isBeforeFirst())
			//	rs = stmt.executeQuery("SELECT * FROM pendingResponse WHERE requesteeID='" + userID + "';");
			
			if(rs.next()){
				pResponse.addedTime = rs.getLong("addedTime");
				pResponse.requesteeID = rs.getString("requesteeID");
				pResponse.requesterID = rs.getString("requesterID");
				pResponse.type = rs.getString("requestType");
				
				return pResponse;
			}
			
			return null;
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return null;
	}
	
	//Returns any requests in the queue, returns empty string if none exist, null if error occurred
	public RideRequest getRideRequest(String userID){
		RideRequest rideRequest = new RideRequest();
		try {
			ResultSet rs = stmt.executeQuery("SELECT * FROM rideRequest WHERE riderID='" + userID + "';");
			if(rs.next()){
				rideRequest.origin = rs.getString("origin");
				rideRequest.destination = rs.getString("destination");
				rideRequest.duration = rs.getLong("duration");
				
				return rideRequest;	
			}
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return null;
	}
}
