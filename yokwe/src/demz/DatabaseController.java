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
	
	public String getDriverApnsToken(String driverID){
		String apnsToken = "";
		ResultSet rs;
		try {
			rs = stmt.executeQuery("SELECT * FROM driver WHERE id=" + driverID);
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
	
	public String getRiderApnsToken(String riderID){
		String apnsToken = "";
		try {
			ResultSet rs = stmt.executeQuery("SELECT * FROM rider WHERE id=" + riderID);
			if(rs.next()){
				apnsToken = rs.getString("apnsToken");
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return apnsToken;
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
	
	public void createTrip(String riderID, String driverID){
		try {
			
			String dOrigin = null;
			String dDestination = null;
			String rOrigin = null;
			String rDestination = null;
			long duration = 0;
			long tripTime = 0;
			long addedTime = 0;
			
			//Get addedTime from database
			ResultSet rs = stmt.executeQuery("SELECT * FROM awaitingResponse WHERE driverID=" + driverID);
			if(rs.next()){
				addedTime = rs.getLong("addedTime");
			}
			
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
			
			//Delete request from awaitingResponse
			stmt.executeUpdate("DELETE FROM awaitingResponse WHERE driverID ='"+ driverID +"';");
			
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
	}
	
	public void createRequest(String riderID, String driverID, String addedTime, String type){
		try {		
			//Store values into a new trip
			stmt.executeUpdate("REPLACE INTO awaitingResponse (riderID, driverID, requestType, addedTime) VALUES "
					+ "('" + riderID + "', '" + driverID + "', '" + type
					+ "', '" + Long.valueOf(addedTime) + "');");

		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
}
