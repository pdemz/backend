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
			String stripeToken = request.getParameter("token");
			String email = request.getParameter("email");
			
			dbController.updatePaymentInfo(id, stripeToken, email);
		}
		
		//When a user logs in, update accessToken and apnsToken, or store them with id if no user exists
		if (type.equals("storeUser")){
			String apns = request.getParameter("apnsToken").replace("<", "").replace(" ", "").replace(">", "");
			dbController.storeUser(id, accessToken, apns);
		}
		
		//updates about me section, for right now
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
