package demz;

import com.stripe.*;
import com.stripe.exception.APIConnectionException;
import com.stripe.exception.APIException;
import com.stripe.exception.AuthenticationException;
import com.stripe.exception.CardException;
import com.stripe.exception.InvalidRequestException;
import com.stripe.model.Account;
import com.stripe.model.Charge;
import com.stripe.model.Customer;
import com.stripe.model.ExternalAccount;
import com.stripe.model.Token;
import com.stripe.model.Card;
import com.stripe.model.BankAccount;

import java.util.*;

public class StripeHelper {
	
	private String STRIPE_KEY = "sk_live_F1gOogzs0M1aFqkAzVralu0d";
	
	public String makePayment(String connectID, String customerID, int amount){
		Stripe.apiKey = STRIPE_KEY;
		
		try{
		
			Map<String, Object> chargeParams = new HashMap<String, Object>();
			
			int fee = (int) (amount * 0.10);
			
			chargeParams.put("amount", amount);
			chargeParams.put("currency", "usd");
			chargeParams.put("customer", customerID);
			chargeParams.put("destination", connectID);
			chargeParams.put("application_fee", fee);
			
			//chargeParams.put("description","");
			Charge cc = Charge.create(chargeParams);
			
			System.out.println(cc.toString());
			
			return cc.toString();
		
		} catch (AuthenticationException | InvalidRequestException | APIConnectionException | CardException
				| APIException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return null;
		
	}
	
	//Requires full name, address, birthday, last 4, tos acceptance date and ip
	public String createCustomer(String email, String paymentToken, String customerToken){
		Customer cc = null;
		try {
			
			Stripe.apiKey = STRIPE_KEY;

			Map<String, Object> customerParams = new HashMap<String, Object>();
			customerParams.put("description", "Customer for Atlas");
			customerParams.put("source", paymentToken);
			System.out.println("customer email: " + email);
			customerParams.put("email", email);
			
			//If a customer exists here, delete it
			if(customerToken != null){
				deleteCustomer(customerToken);
				
			}
			//Create a new customer
			cc = Customer.create(customerParams);

			System.out.println(cc.getId());
			return cc.getId();
			
		} catch (AuthenticationException | InvalidRequestException | APIConnectionException | CardException
				| APIException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	//Requires full name, address, birthday, last 4, tos acceptance date and ip
	public String createManagedAccount(String firstName, String lastName,
			String line1, String line2, String city, String state, 
			String zip, int day, int month, int year, String last4, String ip){
		
		try {
			Stripe.apiKey = STRIPE_KEY;
			
			Map<String, Object> accountParams = new HashMap<String, Object>();
			accountParams.put("managed", "true");
			accountParams.put("country", "US");
			accountParams.put("legal_entity[type]", "individual");
			accountParams.put("legal_entity[first_name]", firstName);
			accountParams.put("legal_entity[last_name]", lastName);
			accountParams.put("legal_entity[address][line1]", line1);
			accountParams.put("legal_entity[address][line2]", line2);
			accountParams.put("legal_entity[address][city]", city);
			accountParams.put("legal_entity[address][state]", state);
			accountParams.put("legal_entity[address][postal_code]", zip);
			accountParams.put("legal_entity[dob][day]", day);
			accountParams.put("legal_entity[dob][month]", month);
			accountParams.put("legal_entity[dob][year]", year);
			accountParams.put("legal_entity[ssn_last_4]", last4);
			
			Date d = new Date();
			accountParams.put("tos_acceptance[date]", (d.getTime()/1000));
			accountParams.put("tos_acceptance[ip]", ip);
			
			Account ac = Account.create(accountParams);
			
			return ac.getId();
			
		} catch (AuthenticationException | InvalidRequestException | APIConnectionException | CardException
				| APIException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	public String addBankAccount(String accountToken, String email, String name, String routingNum, String accountNum){
		try {
			
			Stripe.apiKey = STRIPE_KEY;
			
			//Get bank account token
			Map<String, Object> bankAccountParams = new HashMap<String, Object>();
			bankAccountParams.put("country", "US");
			bankAccountParams.put("currency", "usd");
			bankAccountParams.put("account_holder_type", "individual");
			bankAccountParams.put("account_holder_name", name);
			bankAccountParams.put("routing_number", routingNum);
			bankAccountParams.put("account_number", accountNum);
			
			//Create token
			Map<String, Object> tokenParams = new HashMap<String, Object>();
			tokenParams.put("bank_account", bankAccountParams);
			Token tt = Token.create(tokenParams);
			
			//Add token to account
			Map<String, Object> accountParams = new HashMap<String, Object>();
			//accountParams.put("external_account", tt.getBankAccount());
			accountParams.put("email", email);
			
			Account aa = Account.retrieve(accountToken, null);
			aa.update(accountParams);
			String returnString = aa.getExternalAccounts().create(tokenParams).toString();	
						
			return returnString;
			
		} catch (AuthenticationException | InvalidRequestException | APIConnectionException | CardException
				| APIException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	public String deleteCustomer(String customerToken){
		Customer cc = null;
		
		try {
			Stripe.apiKey = STRIPE_KEY;
			
			//Remove it from the database
			DatabaseController db = new DatabaseController();
			db.deleteCustomerToken(customerToken);
			
			//Delete from the stripe servers
			cc = Customer.retrieve(customerToken);
			cc.delete().toString();
			
			
		} catch (AuthenticationException | InvalidRequestException | APIConnectionException | CardException
				| APIException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	public String deleteBankAccount(String accountToken){
		try {
			
			Stripe.apiKey = STRIPE_KEY;
			
			Account aa = Account.retrieve(accountToken, null);
			//retrieve the bank account ID
			String bankID = aa.getExternalAccounts().getData().get(0).getId();
			
			//Delete the bank account and return the result
			return aa.getExternalAccounts().retrieve(bankID).delete().toString();

		} catch (AuthenticationException | InvalidRequestException | APIConnectionException | CardException
				| APIException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
		
	}
	
	public String getBankAccountInfo(String accountToken){
		try {
			
			Stripe.apiKey = STRIPE_KEY;
			
			Account aa = Account.retrieve(accountToken, null);
			//retrieve the bank account ID
			BankAccount bb = (BankAccount) aa.getExternalAccounts().getData().get(0);
			
			return bb.getBankName() + " ****" + bb.getLast4();

		} catch (AuthenticationException | InvalidRequestException | IndexOutOfBoundsException | APIConnectionException | CardException
				| APIException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	public String getCardInfo(String customerToken){
		try {
			
			Stripe.apiKey = STRIPE_KEY;
			
			Customer cc = Customer.retrieve(customerToken);
			//retrieve the card ID
			String cardToken = cc.getDefaultSource();
			Card card = (Card) cc.getSources().retrieve(cardToken);	
			
			return card.getBrand() + " ****" + card.getLast4();
			
		} catch (AuthenticationException | InvalidRequestException |  NullPointerException | IndexOutOfBoundsException | APIConnectionException | CardException
				| APIException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
}
