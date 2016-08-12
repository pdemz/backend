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
	private static final String myAccessToken = "CAAXOKxZCZA7LkBAKCBZBN06rWH8IwnOTR1HOd30OO0YsBbL2Yjmx9YfDDzHrAxHBvk963t1s7daSOyQjL1koyT7xJp8dTWEvwffcWLlSZC2ppZCoOvR777h4uiidpeQ1QQIaJNpdxMOeXHD0JzxKLNxD3iNaZAQIZARBOP0fABXOJz7v260EpXVVsF806DbmpZBjNoRQq49AnWLKa2Lm8k5CMNIADRShKj0ZD";
	private static final String myAppSecret = "89f8c77f0a5ac4d40c549f381fce999b";
	
	public static String getNumberOfMutualFriends(String accessToken, String userID){		
		try {
			FacebookClient fbClient = new DefaultFacebookClient(accessToken, myAppSecret);
			JsonObject pooper = fbClient.fetchObject(userID, JsonObject.class, Parameter.with("fields", "context.fields(all_mutual_friends)"));
			return pooper.getJsonObject("context").getJsonObject("all_mutual_friends").getJsonObject("summary").getString("total_count").toString();

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
