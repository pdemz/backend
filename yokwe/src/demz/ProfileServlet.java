package demz;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.Gson;

/**
 * Servlet implementation class ProfileServlet
 */
@WebServlet("/profile")
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
		
		if(!type.equals("storeUser")){
			//Authentication
			if(id != null && !FacebookHelper.authenticated(accessToken, id)){
				return;

			}else{
				String email = request.getParameter("email");
				String password = request.getParameter("password");

				if(!dbController.authenticateWithEmailAndPassword(email, password)){
					response.sendError(400, "Invalid credentials");
					return;
				}

				User uu = dbController.getUserWithEmail(email);
				id = uu.id;
			}
		}
		
		if (type.equals("updatePaymentInfo")){
			String paymentToken = request.getParameter("token");
			String email = request.getParameter("email");
			
			//Retrieve user
			User uu = dbController.getUser(id);
			StripeHelper sh = new StripeHelper();
			
			//Create a customer token and store it in db with user
			String customerToken = sh.createCustomer(email, paymentToken, uu.customerToken);
			dbController.updatePaymentInfo(id, customerToken, uu.accountToken, email);
			
			response.getWriter().println(customerToken);
			
		}else if(type.equals("getUser")){
			User uu = dbController.getUser(id);
			Gson gson = new Gson();
			String json = gson.toJson(uu);
			
			response.getWriter().print(json);
		
		//When a user logs in, update all their info or create a new user
		}else if (type.equals("storeUser")){
			User uu = new User();
			uu.id = id;
			uu.accessToken = accessToken;
			String apns = request.getParameter("apnsToken");
			if (apns != null)
				uu.apnsToken = apns.replace("<", "").replace(" ", "").replace(">", "");
			uu.aboutMe = request.getParameter("aboutMe");
			uu.email = request.getParameter("email");
			uu.phone = request.getParameter("phone");
			uu.customerToken = request.getParameter("customerToken");
			uu.accountToken = request.getParameter("accountToken");
			uu.password = request.getParameter("password");
					
			dbController.storeUser(uu);
			
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
			String firstName = request.getParameter("firstName");
			String lastName = request.getParameter("lastName");
			int day = Integer.parseInt(request.getParameter("day"));
			int month = Integer.parseInt(request.getParameter("month"));
			int year = Integer.parseInt(request.getParameter("year"));
			String line1Param = request.getParameter("line1");
			String line1 = line1Param.replace("+", " ");
			String line2 = request.getParameter("line2");
			String city = request.getParameter("city");
			String state = request.getParameter("state");
			String zip = request.getParameter("zip");
			String last4 = request.getParameter("last4");
			
			//Generate account token
			StripeHelper sh = new StripeHelper();
			String accountToken = sh.createManagedAccount(firstName, lastName, line1, line2, city, 
					state, zip, day, month, year, last4, ip);
			
			//Store account token with user
			User uu = new User();
			uu.accountToken = accountToken;
			uu.id = id;
			dbController.storeUser(uu);
			
			response.getWriter().println(accountToken);
			
			
		}else if (type.equals("addBankAccount")){
			StripeHelper sh = new StripeHelper();
			String accountToken = dbController.getUser(id).accountToken;
			String email = request.getParameter("email");
			String nameParam = request.getParameter("name");
			String name = nameParam.replace("+", " ");
			String routingNum = request.getParameter("routingNum");
			String accountNum = request.getParameter("accountNum");
			
			String returnString = sh.addBankAccount(accountToken, email, name, routingNum, accountNum);
			
			response.getWriter().print(returnString);
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
