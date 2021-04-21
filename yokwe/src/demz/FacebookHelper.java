package demz;

import com.restfb.*;
import com.restfb.types.User;
import com.restfb.Connection;
import com.restfb.DefaultFacebookClient;
import com.restfb.json.JsonObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Map;

import com.fasterxml.jackson.databind.*;

public class FacebookHelper {
	private static final String myAccessToken = "###";
	private static final String myAppSecret = "###";
	
	public static String getNumberOfMutualFriends(String accessToken, String userID){	
		
		try {
			FacebookClient fbClient = new DefaultFacebookClient(accessToken, myAppSecret);
			JsonObject fbMutualFriendsRequest = fbClient.fetchObject(userID, JsonObject.class, Parameter.with("fields", "context.fields(all_mutual_friends)"));
			
			return fbMutualFriendsRequest.getJsonObject("context").getJsonObject("all_mutual_friends").getJsonObject("summary").getString("total_count").toString();

		} catch (Exception ex) {
			ex.printStackTrace();
		}
		
		return "0";
	}
	
	public static boolean authenticated(String accessToken, String userID){
		try {
			URL url = new URL("https://graph.facebook.com/me?fields=id&access_token=" + accessToken);
			Map<String, Object> map = new ObjectMapper().readValue(url, Map.class);
			if (userID.equals(map.get("id"))){
				return true;
			}

		} catch (Exception ex) {
			ex.printStackTrace();
		}
		
		return false;
	}
}
