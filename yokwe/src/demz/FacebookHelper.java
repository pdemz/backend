package demz;

import com.restfb.*;
import com.restfb.types.User;
import com.restfb.Connection;
import com.restfb.DefaultFacebookClient;
import com.restfb.json.JsonObject;

public class FacebookHelper {
	private static final String myAccessToken = "CAAXOKxZCZA7LkBAKCBZBN06rWH8IwnOTR1HOd30OO0YsBbL2Yjmx9YfDDzHrAxHBvk963t1s7daSOyQjL1koyT7xJp8dTWEvwffcWLlSZC2ppZCoOvR777h4uiidpeQ1QQIaJNpdxMOeXHD0JzxKLNxD3iNaZAQIZARBOP0fABXOJz7v260EpXVVsF806DbmpZBjNoRQq49AnWLKa2Lm8k5CMNIADRShKj0ZD";
	private static final String myAppSecret = "89f8c77f0a5ac4d40c549f381fce999b";
	
	public static String test(String accessToken, String userID){
		FacebookClient fbClient = new DefaultFacebookClient(accessToken, myAppSecret);
		JsonObject pooper = fbClient.fetchObject(userID, JsonObject.class, Parameter.with("fields", "context.fields(mutual_friends)"));
		return pooper.getJsonObject("context").getJsonObject("mutual_friends").getJsonObject("summary").getString("total_count").toString();
		
	}
}
