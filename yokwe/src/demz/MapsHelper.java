package demz;

import com.google.maps.GeoApiContext;
import com.google.maps.GeocodingApi;
import com.google.maps.model.AddressComponent;
import com.google.maps.model.AddressComponentType;
import com.google.maps.model.GeocodingResult;
import com.google.maps.model.LatLng;

public class MapsHelper{
	
	public static String addressFromCoordinateString(String coordinateString){
        GeoApiContext context = new GeoApiContext().setApiKey("###");
		
        LatLng coordinates = getLatLngFromCoordinates(coordinateString);
        
		try {
			GeocodingResult[] results = GeocodingApi.reverseGeocode(context,
				    coordinates).await();
			return results[0].formattedAddress;
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return null;
		
	}
	
	public static String localityFromCoordinateString(String coordinateString){
        GeoApiContext context = new GeoApiContext().setApiKey("###");
		
        LatLng coordinates = getLatLngFromCoordinates(coordinateString);
        
		try {
			GeocodingResult[] results = GeocodingApi.reverseGeocode(context,
				    coordinates).await();
			
			AddressComponent[] ac = results[0].addressComponents;
			
			for (AddressComponent comp : ac){
				for (AddressComponentType type: comp.types){
					System.out.println(type);
					
					if(type.toString().equals("LOCALITY")){
						System.out.println(comp.longName);
						return comp.longName;
					}
				}
				
			}
			
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
