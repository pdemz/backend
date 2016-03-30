package demz;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet implementation class ProfileServlet
 */
@WebServlet("/ProfileServlet")
public class ProfileServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private DatabaseController dbController = new DatabaseController();

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String type = request.getParameter("type");
		String id = request.getParameter("userID");
		String accessToken = request.getParameter("accessToken");
		
		if (type.equals("updatePaymentInfo")){
			String paymentToken = request.getParameter("token");
			String email = request.getParameter("email");
			
			//Retrieve user
			User uu = dbController.getUser(id);
			StripeHelper sh = new StripeHelper();
			
			//Create a customer token and store it in db with user
			String customerToken = sh.createCustomer(email, paymentToken, uu.customerToken);
			dbController.updatePaymentInfo(id, customerToken, uu.accountToken, email);
		}
		
		//When a user logs in, update accessToken and apnsToken, or store them with id if no user exists
		if (type.equals("storeUser")){
			String apns = request.getParameter("apnsToken").replace("<", "").replace(" ", "").replace(">", "");
			dbController.storeUser(id, accessToken, apns);
		}
		
		//updates 'about me' section, for right now
		else if (type.equals("updateProfile")){
			String aboutMe = request.getParameter("aboutMe");
			dbController.updateProfile(aboutMe, id, accessToken);
		
		//returns about me section, for right now
		}else if (type.equals("getProfile")){
			String returnString = dbController.getProfile(id, accessToken);
			response.getWriter().print(returnString);
			
		//returns true if the user is in the database, otherwise it returns false
		}else if (type.equals("doesUserExist")){
			boolean returnBool = dbController.getUserExistance(id);
			response.getWriter().print(returnBool);
			
		//updates the user phone number
		}else if (type.equals("updatePhone")){
			String phone = request.getParameter("phone");
			dbController.updatePhone(id, phone);
			
		}else if (type.equals("createStripeAccount")){
			String ip = request.getRemoteAddr();
			String email = request.getParameter("email");
			String firstName = request.getParameter("firstName");
			String lastName = request.getParameter("lastName");
			int day = Integer.parseInt(request.getParameter("day"));
			int month = Integer.parseInt(request.getParameter("month"));
			int year = Integer.parseInt(request.getParameter("year"));
			String line1 = request.getParameter("line1");
			String line2 = request.getParameter("line2");
			String city = request.getParameter("city");
			String state = request.getParameter("state");
			String zip = request.getParameter("zip");
			String last4 = request.getParameter("last4");
			
			StripeHelper sh = new StripeHelper();
			sh.createManagedAccount(email, firstName, lastName, line1, line2, city, state, zip, day, month, year, last4, ip);
			
		}else if (type.equals("addBankAccount")){
			StripeHelper sh = new StripeHelper();
			String accountToken = dbController.getUser(id).accountToken;
			String name = request.getParameter("name");
			String routingNum = request.getParameter("routingNum");
			String accountNum = request.getParameter("accountNum");
			
			sh.addBankAccount(accountToken, name, routingNum, accountNum);
		
		//Called by rider only
		}else if (type.equals("makePayment")){
			String driverID = request.getParameter("driverID");
			int amount = Integer.parseInt(request.getParameter("amount"));
			
			//Get customer and connect tokens from db
			User rider = dbController.getUser(id);
			User driver = dbController.getUser(driverID);
			
			StripeHelper sh = new StripeHelper();
			response.getWriter().println(sh.makePayment(driver.accountToken, rider.customerToken, amount));
		}
		
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		doGet(request, response);
	}

}
