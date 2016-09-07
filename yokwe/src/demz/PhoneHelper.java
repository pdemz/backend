package demz;

import com.google.gson.*;
import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.AmazonSNSClientBuilder;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.sns.model.CreateTopicRequest;
import com.amazonaws.services.sns.model.CreateTopicResult;
import com.amazonaws.services.sns.model.SubscribeRequest;
import com.amazonaws.services.sns.model.PublishRequest;
import com.amazonaws.services.sns.model.PublishResult;
import com.amazonaws.services.sns.model.DeleteTopicRequest;
import com.amazonaws.auth.*;


//This class takes in parameters of a phone verification
//returns a JSON response with either a verification code or whether or not verification was successful
public class PhoneHelper{
	
	String number;
	String type;
	String code;
	
	public PhoneHelper(String newNumber, String newType, String newCode){
		number = newNumber;
		type = newType;
		code = newCode;
		handle();
	}
	
	//Handles the verification step and returns the proper JSON string for the app
	private boolean handle(){
		
		//Send random 4 digit number to phone and store that number in the database
		if (type.equals("send")){
			//generate a 4 digit integer
	        int randomPIN = (int)(Math.random()*9000)+1000;
	        code = String.valueOf(randomPIN);
	        
	        //Send the code to the phone
	        sendSMSMessage(code, number);
	        
		}else if(type.equals("verify")){
			
			//get the code from the database
			
			//if equal 
			return true;
		}
		
		return false;
		
	}

	private void sendSMSMessage(String message, 
			String phoneNumber) {
			BasicAWSCredentials credentials = new BasicAWSCredentials("AKIAJMAG65OXWWYGREDA", "mPsfVzS3cJ33e5C0oGRGcUOQV7sF02m9sABsJBCc");
			AmazonSNSClient snsClient = AmazonSNSClientBuilder.standard()
					.withCredentials(new AWSStaticCredentialsProvider(credentials)).build();
	        PublishResult result = snsClient.publish(new PublishRequest()
	                        .withMessage(message)
	                        .withPhoneNumber(phoneNumber));
	        System.out.println(result);

	}
	
}