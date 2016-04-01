package demz;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.sql.PreparedStatement;

import org.apache.commons.dbutils.DbUtils;

import com.mysql.jdbc.jdbc2.optional.*;


public class DatabaseController {
	
	private MysqlDataSource dataSource;
	
	public DatabaseController(){
		dataSource = new MysqlDataSource();
		dataSource.setUser("demz");
		dataSource.setPassword("Iheartnewyork!1");
		dataSource.setServerName("myfirstdatabase.cgrwwpjxf5ev.us-west-2.rds.amazonaws.com");
		dataSource.setDatabaseName("demzdb");
		dataSource.setPort(3306);
		
	}
	
	public void makeUnavailable(String userID){
		ResultSet rs = null;
		java.sql.Statement stmt = null;
		Connection conn = null;
		
		try {
			conn = dataSource.getConnection();
			stmt = conn.createStatement();
			rs = stmt.executeQuery("SELECT * FROM rider WHERE id=" + userID);
			if(rs.next())
				stmt.executeUpdate("UPDATE driver SET available=false WHERE id=" + rs.getString("driverID"));
			else
				stmt.executeUpdate("UPDATE driver SET available=false WHERE id=" + userID);
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}finally{
			DbUtils.closeQuietly(rs);
			DbUtils.closeQuietly(stmt);
			DbUtils.closeQuietly(conn);
		}
	}
	
	public void reset(String userID){
		ResultSet rs = null;
		java.sql.Statement stmt = null;
		Connection conn = null;
		
		try {
			conn = dataSource.getConnection();
			stmt = conn.createStatement();
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
		}finally{
			DbUtils.closeQuietly(rs);
			DbUtils.closeQuietly(stmt);
			DbUtils.closeQuietly(conn);

		}
		
	}
	
	public void storeRider(Rider rider){
		java.sql.Statement stmt = null;
		Connection conn = null;
		
		try {
			conn = dataSource.getConnection();
			stmt = conn.createStatement();
			stmt.executeUpdate("INSERT INTO rideRequest (riderID, origin, destination, duration) VALUES "
					+ "('" + rider.getID() + "', '" + rider.getOrigin() 
					+ "', '" + rider.getDestination() + "', '" + rider.getDuration() + "') ON DUPLICATE KEY UPDATE origin='" 
					+ rider.getOrigin() + "', destination='" + rider.getDestination() + "', duration='" + rider.getDuration() + "';");
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}finally{
			DbUtils.closeQuietly(stmt);
			DbUtils.closeQuietly(conn);

		}
		
	}
	
	public void storeDriver(Driver driver){
		java.sql.Statement stmt = null;
		Connection conn = null;
		
		try {
			conn = dataSource.getConnection();
			stmt = conn.createStatement();

			stmt.executeUpdate("INSERT INTO driveRequest (timeLimit, driverId, origin, destination, duration, distance) VALUES "
					+ "('" + driver.getLimit() + "', '" + driver.getID() + "', '" + driver.getOrigin() 
					+ "', '" + driver.getDestination() + "', '" + driver.getDuration() + "', '" + driver.getDistance() + "') ON DUPLICATE KEY UPDATE origin='" 
					+ driver.getOrigin() + "', destination='" + driver.getDestination() + "', timeLimit='" + driver.getLimit() 
					+ "', duration='" + driver.getDuration() + "', distance='" + driver.getDistance() + "';");
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}finally{
			DbUtils.closeQuietly(stmt);
			DbUtils.closeQuietly(conn);

		}
		
	}
	
	public String getUserApnsToken(String userID){
		String apnsToken = "";
		ResultSet rs = null;
		java.sql.Statement stmt = null;
		Connection conn = null;
		try {
			conn = dataSource.getConnection();
			stmt = conn.createStatement();
			rs = stmt.executeQuery("SELECT * FROM user WHERE id=" + userID);
			if(rs.next()){
				apnsToken = rs.getString("apnsToken");
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}finally{
			DbUtils.closeQuietly(rs);
			DbUtils.closeQuietly(stmt);
			DbUtils.closeQuietly(conn);

		}
		
		return apnsToken;
	}
	
	public User getUser(String userID){
		ResultSet rs = null;
		java.sql.Statement stmt = null;
		Connection conn = null;
		try {
			conn = dataSource.getConnection();
			stmt = conn.createStatement();
			rs = stmt.executeQuery("SELECT * FROM user WHERE id=" + userID);
			if(rs.next()){
				User uu = new User();
				uu.aboutMe = rs.getString("aboutMe");
				uu.accessToken = rs.getString("accessToken");
				uu.apnsToken = rs.getString("apnsToken");
				uu.email = rs.getString("email");
				uu.id = userID;
				uu.phone = rs.getString("phone");
				uu.customerToken = rs.getString("customerToken");
				uu.accountToken = rs.getString("accountToken");
				
				System.out.println("definitely returned a user here");

				return uu;
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}finally{
			DbUtils.closeQuietly(rs);
			DbUtils.closeQuietly(stmt);
			DbUtils.closeQuietly(conn);

		}
		
		
		return null;
	}
	
	public String getPartner(String userID){
		//will return type;userID;accessToken;origin;destination;addedTime
		String partnerID = "nothing";
		Boolean available = true;
		ResultSet rs = null;
		ResultSet rsx = null;
		java.sql.Statement stmt = null;
		Connection conn = null;
		try {
			conn = dataSource.getConnection();
			stmt = conn.createStatement();
			rs = stmt.executeQuery("SELECT * FROM rider WHERE id=" + userID);
			if(rs.next()){
				partnerID = rs.getString("driverID");
				if (partnerID == null){
					return "nothing";
				}
				//If the driver is available, do the following
				Driver driver = getDriver(partnerID);
				DbUtils.closeQuietly(rs);
				DbUtils.closeQuietly(stmt);
				
				rs = stmt.executeQuery("SELECT * FROM driver WHERE id=" + partnerID);
				if (rs.next())
					available = rs.getBoolean("available");
				
				DbUtils.closeQuietly(rs);
				DbUtils.closeQuietly(stmt);
				
				if(available == true)
					return "driver;"+driver.getID()+";"+driver.getAccessToken()+";"+driver.getOrigin()+";"+driver.getDestination()+";true"; 
				//Otherwise, indicate that the driver.available==false, meaning they are already paired
				else
					return "driver;"+driver.getID()+";"+driver.getAccessToken()+";"+driver.getOrigin()+";"+driver.getDestination()+";false"; 

			}else{
				DbUtils.closeQuietly(stmt);
				stmt = conn.createStatement();
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
		}finally{
			DbUtils.closeQuietly(rs);
			DbUtils.closeQuietly(stmt);
			DbUtils.closeQuietly(conn);

		}
		
		return partnerID;
	}
	
	public Driver getDriver(String userID){
		Driver driver = null;
		ResultSet rs = null;
		java.sql.Statement stmt = null;
		Connection conn = null;
		try {
			conn = dataSource.getConnection();
			stmt = conn.createStatement();
			System.out.println("This is the userID being sent in: "+ userID );
			rs = stmt.executeQuery("SELECT * FROM driver WHERE id=" + userID);
			if(rs.next()){
				driver = new Driver(userID, rs.getString("accessToken"), rs.getString("apnsToken"), rs.getInt("timeLimit"), rs.getString("origin"), rs.getString("destination"));
			}
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}finally{
			DbUtils.closeQuietly(rs);
			DbUtils.closeQuietly(stmt);
			DbUtils.closeQuietly(conn);

		}
		
		return driver;
		
	}
	
	public Rider getRider(String userID){
		Rider rider = null;
		ResultSet rs = null;
		java.sql.Statement stmt = null;
		Connection conn = null;
		try {
			conn = dataSource.getConnection();
			stmt = conn.createStatement();
			rs = stmt.executeQuery("SELECT * FROM rider WHERE id=" + userID);
			if(rs.next()){
				//public Rider(String newId, String newAccessToken, String newApnsToken, String newOrigin, String newDest, String newDriverID)
				rider = new Rider(userID, rs.getString("accessToken"), rs.getString("apnsToken"), rs.getString("origin"), rs.getString("destination"), rs.getString("driverID"));
			}
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}finally{
			DbUtils.closeQuietly(rs);
			DbUtils.closeQuietly(stmt);
			DbUtils.closeQuietly(conn);

		}
		
		return rider;
		
	}
	
	public String getAccessToken(String userID){
		String accessToken = null;
		ResultSet rs = null;
		java.sql.Statement stmt = null;
		Connection conn = null;
		try {
			conn = dataSource.getConnection();
			stmt = conn.createStatement();
			rs = stmt.executeQuery("SELECT * FROM user WHERE id=" + userID);
			if(rs.next()){
				accessToken = rs.getString("accessToken");
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}finally{
			DbUtils.closeQuietly(rs);
			DbUtils.closeQuietly(stmt);
			DbUtils.closeQuietly(conn);

		}
		
		return accessToken;
		
	}
	
	public ArrayList<Driver> getAllDrivers(){
		ArrayList<Driver> driverList= new ArrayList<Driver>();		
		ResultSet rs = null;
		java.sql.Statement stmt = null;
		Connection conn = null;
		
		try{
			
			conn = dataSource.getConnection();
			stmt = conn.createStatement();
			rs = stmt.executeQuery("SELECT * FROM driveRequest");
			
			while (rs.next()) {
				String id = rs.getString("driverID");
				int limit = rs.getInt("timeLimit");
				String origin = rs.getString("origin");
				String destination = rs.getString("destination");
				long duration = rs.getLong("duration");
				int distance = rs.getInt("distance");

				Driver newb = new Driver(id, limit, origin, destination, duration, distance, null);
				driverList.add(newb);
			}
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			
		}finally{
			DbUtils.closeQuietly(rs);
			DbUtils.closeQuietly(stmt);
			DbUtils.closeQuietly(conn);

		}
		
		return driverList;
		
	}
	
	public ArrayList<Rider> getAllRiders(){
		ResultSet rs = null;
		java.sql.Statement stmt = null;
		Connection conn = null;
		ArrayList<Rider> riderList= new ArrayList<Rider>();
		
		try{
			conn = dataSource.getConnection();
			stmt = conn.createStatement();
			rs = stmt.executeQuery("SELECT * FROM rideRequest");
			
			while (rs.next()) {

				String id = rs.getString("riderID");
				String origin = rs.getString("origin");
				String destination = rs.getString("destination");
				Long duration = rs.getLong("duration");

				Rider newb = new Rider(id, origin, destination, duration, null);
				riderList.add(newb);
			}
			
		}catch (SQLException e) {
			e.printStackTrace();
		}finally{
			DbUtils.closeQuietly(rs);
			DbUtils.closeQuietly(stmt);
			DbUtils.closeQuietly(conn);

		}
		
		return riderList;
	}
	
	public void updateDriverID(String riderID, String driverID){
		java.sql.Statement stmt = null;
		Connection conn = null;
		try {
			conn = dataSource.getConnection();
			stmt = conn.createStatement();
			stmt.executeUpdate("UPDATE rider SET driverID='"+driverID+"' WHERE id=" + riderID);
		} catch (SQLException e) {
			e.printStackTrace();
		}finally{
			DbUtils.closeQuietly(stmt);
			DbUtils.closeQuietly(conn);

		}
	}
	
	public String getProfile(String userID, String accessToken){
		java.sql.Statement stmt = null;
		Connection conn = null;
		ResultSet rs = null;
		try {
			conn = dataSource.getConnection();
			stmt = conn.createStatement();
			rs = stmt.executeQuery("SELECT * FROM user WHERE id='" + userID 
					+ "';");
			if(rs.next()){
				String aboutMe = rs.getString("aboutMe");
				String phone = rs.getString("phone");
				return aboutMe + ";" + phone;
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}finally{
			DbUtils.closeQuietly(rs);
			DbUtils.closeQuietly(stmt);
			DbUtils.closeQuietly(conn);

		}
		
		return null;
	}
	
	public String[] getProfileWithID(String userID){
		java.sql.Statement stmt = null;
		Connection conn = null;
		ResultSet rs = null;
		try {
			conn = dataSource.getConnection();
			stmt = conn.createStatement();
			rs = stmt.executeQuery("SELECT * FROM user WHERE id='" + userID + "';");
			if(rs.next()){
				String aboutMe = rs.getString("aboutMe");
				String phone = rs.getString("phone");
				
				return new String[] {aboutMe, phone};
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}finally{
			DbUtils.closeQuietly(rs);
			DbUtils.closeQuietly(stmt);
			DbUtils.closeQuietly(conn);

		}
		
		return null;
	}
	
	public void updateProfile(String aboutMe, String userID, String accessToken){
		aboutMe = aboutMe.replaceAll("'", "''");
		java.sql.Statement stmt = null;
		Connection conn = null;
		try {
			conn = dataSource.getConnection();
			stmt = conn.createStatement();
			stmt.executeUpdate("UPDATE user SET aboutMe='"+ aboutMe +"' WHERE id='" 
					+ userID + "' AND accessToken='"
					+ accessToken +"';");
			
		} catch (SQLException e) {
			e.printStackTrace();
		}finally{
			DbUtils.closeQuietly(stmt);
			DbUtils.closeQuietly(conn);

		}
	}
	
	public void updatePaymentInfo(String userID, String customerToken, String accountToken, String email){
		java.sql.PreparedStatement pstmt = null;
		Connection conn = null;
		try {
			conn = dataSource.getConnection();
			pstmt = conn.prepareStatement("UPDATE user SET customerToken = ?, accountToken = ?, email = ? WHERE id = ?");

			pstmt.setString(1, customerToken);
			pstmt.setString(2, accountToken);
			pstmt.setString(3, email);
			pstmt.setString(4, userID);
			pstmt.executeUpdate();
			
		} catch (SQLException e) {
			e.printStackTrace();
		}finally{
			DbUtils.closeQuietly(pstmt);
			DbUtils.closeQuietly(conn);
		}
	}
	
	//Verbose, but probably the best way to handle this monster of a statement
	public void storeUser(User uu){
		PreparedStatement pstmt = null;
		Connection conn = null;
		try{
			conn = dataSource.getConnection();
			String insertQuery = "INSERT INTO "
					+ "user (id, accessToken, apnsToken, aboutMe, email, phone, customerToken, accountToken)"
					+ " VALUES (?, ?, ?, ?, ?, ?, ?, ?)"
					+ "ON DUPLICATE KEY UPDATE "
					+ "accessToken=IFNULL(?,accessToken),"
					+ "apnsToken=IFNULL(?,apnsToken),"
					+ "aboutMe=IFNULL(?,aboutMe),"
					+ "email=IFNULL(?,email),"
					+ "phone=IFNULL(?,phone),"
					+ "customerToken=IFNULL(?, customerToken),"
					+ "accountToken=IFNULL(?, accountToken)";
			
			//Now do the 15 sets
			pstmt = conn.prepareStatement(insertQuery);
			pstmt.setString(1, uu.id);
			pstmt.setString(2, uu.accessToken);
			pstmt.setString(3, uu.apnsToken);
			pstmt.setString(4, uu.aboutMe);
			pstmt.setString(5, uu.email);
			pstmt.setString(6, uu.phone);
			pstmt.setString(7, uu.customerToken);
			pstmt.setString(8, uu.accountToken);
			pstmt.setString(9, uu.accessToken);
			pstmt.setString(10, uu.apnsToken);
			pstmt.setString(11, uu.aboutMe);
			pstmt.setString(12, uu.email);
			pstmt.setString(13, uu.phone);
			pstmt.setString(14, uu.customerToken);
			pstmt.setString(15, uu.accountToken);
			
			pstmt.executeUpdate();
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}finally{
			DbUtils.closeQuietly(pstmt);
			DbUtils.closeQuietly(conn);

		}
	}
	
	public boolean getUserExistance(String userId){
		java.sql.Statement stmt = null;
		Connection conn = null;
		ResultSet rs = null;
		try {
			conn = dataSource.getConnection();
			stmt = conn.createStatement();
			rs = stmt.executeQuery("SELECT * FROM user WHERE id='" + userId + "';");
			if(rs.next()){
				return true;
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}finally{
			DbUtils.closeQuietly(rs);
			DbUtils.closeQuietly(stmt);
			DbUtils.closeQuietly(conn);

		}
		
		return false;
	}
	
	public void updatePhone(String userID, String phone){
		java.sql.Statement stmt = null;
		Connection conn = null;
		try {
			conn = dataSource.getConnection();
			stmt = conn.createStatement();
			stmt.executeUpdate("UPDATE user SET phone='"+ phone +"' WHERE id='" 
					+ userID + "';");
			
		} catch (SQLException e) {
			e.printStackTrace();
		}finally{
			DbUtils.closeQuietly(stmt);
			DbUtils.closeQuietly(conn);

		}
	}
	
	public void createTrip(String requesterID, String requesteeID){
		java.sql.Statement stmt = null;
		ResultSet rs = null;
		Connection conn = null;
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
			int price = 0;
			
			conn = dataSource.getConnection();
			stmt = conn.createStatement();
			//Get added time and determine which is driver and which is rider
			rs = stmt.executeQuery("SELECT * FROM pendingResponse WHERE requesteeID='" + requesteeID
					+ "' AND requesterID='"+requesterID+"';");
			if(rs.next()){
				addedTime = rs.getLong("addedTime");
				price = rs.getInt("price");
				String type = rs.getString("requestType");
				
				if( type.equals("drive")){
					driverID = requesterID;
					riderID = requesteeID;
				}else{
					driverID = requesteeID;
					riderID = requesterID;
				}
			}
			DbUtils.closeQuietly(rs);
			DbUtils.closeQuietly(stmt);
			
			System.out.println(driverID);
			System.out.print("driver");
			
			//Get origin and destination from driver
			stmt = conn.createStatement();
			rs = stmt.executeQuery("SELECT * FROM driveRequest WHERE driverID=" + driverID);
			if(rs.next()){
				dOrigin = rs.getString("origin");
				dDestination = rs.getString("destination");
				duration = rs.getLong("duration");
				
				//Get total trip time by adding driver duration and addedTime
				tripTime = duration/60 + addedTime;
			}
			
			DbUtils.closeQuietly(rs);
			DbUtils.closeQuietly(stmt);
			
			//Get origin and destination from rider
			stmt = conn.createStatement();
			rs = stmt.executeQuery("SELECT * FROM rideRequest WHERE riderID=" + riderID);
			if(rs.next()){
				rOrigin = rs.getString("origin");
				rDestination = rs.getString("destination");
			}
			DbUtils.closeQuietly(rs);
			DbUtils.closeQuietly(stmt);
			
			stmt = conn.createStatement();
			
			//Store values into a new trip
			System.out.println(driverID);
			System.out.print("driver");
			
			stmt.executeUpdate("INSERT INTO trip (riderID, driverID, dOrigin, dDestination, rOrigin, rDestination, duration, price) VALUES "
					+ "('" + riderID + "', '" + driverID + "', '" + dOrigin 
					+ "', '" + dDestination + "', '" + rOrigin + "', '" 
					+ rDestination + "', '" + tripTime + "', '" + price + "');");
			
			//Delete pendingResponse, now that a trip has been created. This is what it's all about, baby!
			deletePendingResponse(requesterID, requesteeID);
			
			//Eventually set ride and drive request to unavailable right here
			
		} catch (SQLException e) {
			e.printStackTrace();
		}finally{
			DbUtils.closeQuietly(rs);
			DbUtils.closeQuietly(stmt);
			DbUtils.closeQuietly(conn);

		}
		
	}
	
	public void createPendingResponse(String requesterID, String requesteeID, String addedTime, int price, String type){
		java.sql.Statement stmt = null;
		Connection conn = null;
		
		try {
			conn = dataSource.getConnection();
			stmt = conn.createStatement();
			
			//Store values into a new trip
			stmt.executeUpdate("REPLACE INTO pendingResponse (requesterID, requesteeID, requestType, addedTime, price) VALUES "
					+ "('" + requesterID + "', '" + requesteeID + "', '" + type
					+ "', '" + (long)Double.parseDouble(addedTime) + "', '" + price + "');");

		} catch (SQLException e) {
			e.printStackTrace();
		}finally{
			DbUtils.closeQuietly(stmt);
			DbUtils.closeQuietly(conn);
		}
	}
	
	public void removeTrip(String riderID, String driverID){
		java.sql.Statement stmt = null;
		Connection conn = null;
		try{
			conn = dataSource.getConnection();
			stmt = conn.createStatement();
			stmt.executeUpdate("DELETE FROM trip WHERE riderID ='"+ riderID +"' AND driverID='"+ driverID +"';");
		} catch (SQLException e){
			e.printStackTrace();
		}finally{
			DbUtils.closeQuietly(stmt);
			DbUtils.closeQuietly(conn);

		}
	}
	
	public void deletePendingResponse(String requesterID, String requesteeID){
		java.sql.Statement stmt = null;
		Connection conn = null;
		try{
			conn = dataSource.getConnection();
			stmt = conn.createStatement();
			stmt.executeUpdate("DELETE FROM pendingResponse WHERE requesteeID ='"+ requesteeID +"' AND requesterID='"+ requesterID +"';");
		} catch (SQLException e){
			e.printStackTrace();
		}finally{
			DbUtils.closeQuietly(stmt);
			DbUtils.closeQuietly(conn);

		}
	}
	
	public void deleteRideRequest(String riderID){
		java.sql.Statement stmt = null;
		ResultSet rs = null;
		Connection conn = null;
		try{
			conn = dataSource.getConnection();
			stmt = conn.createStatement();
			stmt.executeUpdate("DELETE FROM rideRequest WHERE riderID ='"+ riderID +"';");
		} catch (SQLException e){
			e.printStackTrace();
		}finally{
			DbUtils.closeQuietly(rs);
			DbUtils.closeQuietly(stmt);
			DbUtils.closeQuietly(conn);

		}
	}
	
	public void deleteDriveRequest(String driverID){
		java.sql.Statement stmt = null;
		Connection conn = null;
		try{
			conn = dataSource.getConnection();
			stmt = conn.createStatement();
			stmt.executeUpdate("DELETE FROM driveRequest WHERE driverID ='"+ driverID +"';");
		} catch (SQLException e){
			e.printStackTrace();
		}finally{
			DbUtils.closeQuietly(stmt);
			DbUtils.closeQuietly(conn);

		}
	}
	
	//Returns trip user is currently active in, returns null if none exist
	public Trip getTrip(String userID){
		Trip trip = new Trip();
		java.sql.Statement stmt = null;
		Connection conn = null;
		ResultSet rs = null;
		
		try {
			conn = dataSource.getConnection();
			stmt = conn.createStatement();
			rs = stmt.executeQuery("SELECT * FROM trip WHERE riderID='" + userID + "';");
			if(!rs.isBeforeFirst()){
				DbUtils.closeQuietly(stmt);
				stmt = conn.createStatement();
				DbUtils.closeQuietly(rs);
				rs = stmt.executeQuery("SELECT * FROM trip WHERE driverID='" + userID + "';");
			}

			if(rs.next()){
				trip.duration = rs.getLong("duration");
				trip.price = rs.getInt("price");
				String riderID = rs.getString("riderID");
				String driverID = rs.getString("driverID");
				RideRequest rr = getRideRequest(riderID);
				DriveOffer dr = getDriveOffer(driverID);
				
				trip.rider = new Rider(riderID, rr.origin, rr.destination, rr.duration, getUser(riderID).customerToken);
				trip.driver = new Driver(driverID, 30, dr.origin, dr.destination, dr.duration, dr.distance, getUser(driverID).accountToken);
				
				trip.rider.accessToken = getAccessToken(riderID);
				trip.driver.accessToken = getAccessToken(driverID);
				
				String[] riderInfo = getProfileWithID(riderID);
				String[] driverInfo = getProfileWithID(driverID);

				trip.rider.aboutMe = riderInfo[0];
				trip.rider.phone = riderInfo[1];
				trip.driver.aboutMe = driverInfo[0];
				trip.driver.phone = driverInfo[1];
				
				return trip;
			}
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}finally{
			DbUtils.closeQuietly(rs);
			DbUtils.closeQuietly(stmt);
			DbUtils.closeQuietly(conn);

		}
		
		return null;
	}
	
	//Returns any requests waiting for the users response, returns null if none exist
	public pendingResponse getPendingResponses(String userID){
		java.sql.Statement stmt = null;
		Connection conn = null;
		ResultSet rs = null;
		pendingResponse pResponse = new pendingResponse();
		
		try {
			conn = dataSource.getConnection();
			stmt = conn.createStatement();
			rs = stmt.executeQuery("SELECT * FROM pendingResponse WHERE requesteeID='" + userID + "';");
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
		}finally{
			DbUtils.closeQuietly(rs);
			DbUtils.closeQuietly(stmt);
			DbUtils.closeQuietly(conn);

		}
		
		return null;
	}
	
	//Returns any requests in the queue, returns empty string if none exist, null if error occurred
	public RideRequest getRideRequest(String userID){
		RideRequest rideRequest = new RideRequest();
		
		java.sql.Statement stmt = null;
		Connection conn = null;
		ResultSet rs = null;
		
		try {
			conn = dataSource.getConnection();
			stmt = conn.createStatement();
			rs = stmt.executeQuery("SELECT * FROM rideRequest WHERE riderID='" + userID + "';");
			if(rs.next()){
				rideRequest.origin = rs.getString("origin");
				rideRequest.destination = rs.getString("destination");
				rideRequest.duration = rs.getLong("duration");
				
				System.out.println("This duration was retrieved: " + rs.getLong("duration"));

				
				return rideRequest;	
			}
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}finally{
			DbUtils.closeQuietly(rs);
			DbUtils.closeQuietly(stmt);
			DbUtils.closeQuietly(conn);

		}
		
		return null;
	}
	
	//Returns any requests in the queue, returns empty string if none exist, null if error occurred
	public DriveOffer getDriveOffer(String userID){
		DriveOffer dr = new DriveOffer();
		
		java.sql.Statement stmt = null;
		Connection conn = null;
		ResultSet rs = null;
		try {
			conn = dataSource.getConnection();
			stmt = conn.createStatement();
			rs = stmt.executeQuery("SELECT * FROM driveRequest WHERE driverID='" + userID + "';");
			if(rs.next()){
				dr.origin = rs.getString("origin");
				dr.destination = rs.getString("destination");
				dr.duration = rs.getLong("duration");
				
				return dr;	
			}
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}finally{
			DbUtils.closeQuietly(rs);
			DbUtils.closeQuietly(stmt);
			DbUtils.closeQuietly(conn);

		}
		
		return null;
	}
	
	public ArrayList<String> getDriveRequest(String userID){
		ArrayList<String> dr = new ArrayList<String>();
		java.sql.Statement stmt = null;
		Connection conn = null;
		ResultSet rs = null;
		try {
			conn = dataSource.getConnection();
			stmt = conn.createStatement();
			rs = stmt.executeQuery("SELECT * FROM driveRequest WHERE driverID='" + userID + "';");
			if(rs.next()){
				int count = rs.getMetaData().getColumnCount();
				for(int i = 1; i <= count; i++){
					dr.add(rs.getObject(i).toString());
				}
				
				return dr;	
			}
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}finally{
			DbUtils.closeQuietly(rs);
			DbUtils.closeQuietly(stmt);
			DbUtils.closeQuietly(conn);
		}
		
		return null;
	}
}
