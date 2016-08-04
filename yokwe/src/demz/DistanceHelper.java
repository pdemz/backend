package demz;

public class DistanceHelper {

	public static double getCrossTrackFromDriverAndRider(Driver driver, Rider rider){
		String[] driverStart = driver.getOrigin().split(",");
		String[] driverEnd = driver.getDestination().split(",");
		String[] riderStart = rider.getOrigin().split(",");
		
		//Convert the stored origin and destination strings into latlong doubles
		
		double lat1 = Double.parseDouble(driverStart[0]); 
		double lat2 = Double.parseDouble(driverEnd[0]);
		double lat3 = Double.parseDouble(riderStart[0]);
		double lon1 = Double.parseDouble(driverStart[1]);
		double lon2 = Double.parseDouble(driverEnd[1]);
		double lon3 = Double.parseDouble(riderStart[1]);
		
		//Return xtrack distance in miles
		return crossTrack(lat1,lat2,lat3,lon1,lon2,lon3)/1600;
		
	}
	
	//The third point is the rider's origin
	public static double crossTrack(double lat1, double lat2, double lat3, double lon1, double lon2, double lon3){

		//dxt = asin( sin(δ13) ⋅ sin(θ13−θ12) ) ⋅ R

		double rr = 6371000; //Earth's radius in meters

		double distance13 = angularDistance(lat1,lat3,lon1,lon3); //angular distance from driver start to rider start

		double bearing13 = bearing(lat1, lat3, lon1, lon3); //initial bearing from driver start to rider start
		double bearing12 = bearing(lat1, lat2, lon1, lon2); //initial bearing from driver start to driver end

		double crossTrack = Math.asin(Math.sin(distance13) * Math.sin(Math.toRadians(bearing13 - bearing12))) * rr;

		return crossTrack;

	}

	public static double bearing(double lat1, double lat2, double lon1, double lon2){
		/*var y = Math.sin(λ2-λ1) * Math.cos(φ2);
		  var x = Math.cos(φ1)*Math.sin(φ2) -
		  Math.sin(φ1)*Math.cos(φ2)*Math.cos(λ2-λ1);
		  var brng = Math.atan2(y, x).toDegrees();
		 */

		//Radians
		double phi1 = Math.toRadians(lat1);
		double phi2 = Math.toRadians(lat2);
		double gamma1 = Math.toRadians(lon1);
		double gamma2 = Math.toRadians(lon2);
		double y = Math.sin(gamma2-gamma1) * Math.cos(phi2);
		double x = Math.cos(phi1)*Math.sin(phi2) -
				Math.sin(phi1)*Math.cos(phi2)*Math.cos(gamma2-gamma1);
		double brng = Math.atan2(y, x);
		double degrees = Math.toDegrees(brng);

		return degrees;
	}

	public static double haversine(double lat1, double lat2, double lon1, double lon2){
		double rr = 6371000; //meters
		double dd = rr * angularDistance(lat1,lat2,lon1,lon2);

		return dd;
	}

	public static double angularDistance(double lat1, double lat2, double lon1, double lon2){
		double phi1 = Math.toRadians(lat1);
		double phi2 = Math.toRadians(lat2);
		double deltaPhi = Math.toRadians(lat2 - lat1);
		double deltaGamma = Math.toRadians(lon2 - lon1);

		double aa = (Math.sin(deltaPhi/2) * Math.sin(deltaPhi)/2) +
				Math.cos(phi1) * Math.cos(phi2) * Math.sin(deltaGamma/2) * Math.sin(deltaGamma/2);

		double cc = 2 * Math.atan2(Math.sqrt(aa), Math.sqrt(1-aa));

		return cc;
	}


}
