package demz;

import com.google.maps.GeoApiContext;
import com.google.maps.GeocodingApi;
import com.google.maps.model.GeocodingResult;
import com.google.maps.model.LatLng;

public class MapsHelper{
	
	public static String addressFromCoordinateString(String coordinateString){
        GeoApiContext context = new GeoApiContext().setApiKey("AIzaSyBrmvso2zVY_soF75Een6sI8sA5f0yGw5s");
		
        LatLng coordinates = getLatLngFromCoordinates(coordinateString);
        
		try {
			GeocodingResult[] results = GeocodingApi.reverseGeocode(context,
				    coordinates).await();
			System.out.println(results[0].formattedAddress);
			return results[0].formattedAddress;
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return null;
		
	}
	
	public static LatLng getLatLngFromCoordinates(String coordinates){
		String[] split = coordinates.split(",");
		LatLng ll = new LatLng(Double.parseDouble(split[0]), Double.parseDouble(split[1]));
		
		return ll;
		
	}
	
	
}