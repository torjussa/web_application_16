package WebApplication;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;
import java.util.regex.Pattern;

import javax.mail.MessagingException;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * Servlet implementation class ControllerServlet
 */
@WebServlet("/ControllerServlet")
public class ControllerServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
   
	EmailSender es = new EmailSender();
	mySQLconnection sql = new mySQLconnection();
	
	//All publications
	ArrayList<PublicationBean> allPublications = sql.getPublications("");

    public ControllerServlet() {
        super();
        // TODO Auto-generated constructor stub
    }

    
    // init
    public void init(ServletConfig config) throws ServletException {
    
    	
    	super.init(config);
    }
    
    
    
	// DO GET
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		HttpSession session = request.getSession();
		session.setAttribute("wrongPassword", false);
		

					
	}


	
	
	
	// DO POST
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		

		HttpSession session = request.getSession();
		session.setAttribute("wrongPassword", false);
		
		String action = request.getParameter("action");
		 
		System.out.println("requested action: " + action);
		
		RequestDispatcher requestdispatcher;
		
		// Checks parameter 'action'
		switch(action){
		
		
		case "Sign in":
			
			String username  = request.getParameter("loginUsername");
			String password = request.getParameter("loginPass");
			
			//If username exists
			if (!isAvailableUsername(username)){
				UserBean loginUser = sql.getUserInfo(username);			
				
				
				
				// Checks for username and password up against db
				if(loginUser.getPassword().equals(password)){ 
					

					if (loginUser.getActivated() ==0){
						session.setAttribute("loginError", "User is not activated, please check your email.");
						session.setAttribute("wrongPassword", true);
						requestdispatcher = request.getRequestDispatcher("/signIn.jsp");
						requestdispatcher.forward(request, response);
					}
					else if (loginUser.getBanned() ==1){
						
						session.setAttribute("wrongPassword", true);
						session.setAttribute("loginError", "User is banned");
						requestdispatcher = request.getRequestDispatcher("/signIn.jsp");
						requestdispatcher.forward(request, response);
					}
					else{
						// Sets current User and his cart form db
						session.setAttribute("currentUser", loginUser);
						session.setAttribute("currentUsername", loginUser.getUsername());
						session.setAttribute("cart", sql.getCart(username));

						session.setAttribute("visualSearch","");
						

					
						
						session.setAttribute("wrongPassword", false);
						
						//Redirects to search.jsp/HOME page
						requestdispatcher = request.getRequestDispatcher("/search.jsp");
						requestdispatcher.forward(request, response);
					}
				}else{
					session.setAttribute("wrongPassword", true);
					session.setAttribute("loginError", "Username and password does not match");
					requestdispatcher = request.getRequestDispatcher("/signIn.jsp");
					requestdispatcher.forward(request, response);
					
				}
			}
			else{
				
				session.setAttribute("wrongPassword", true);
				session.setAttribute("loginError", "Username and password does not match");
				requestdispatcher = request.getRequestDispatcher("/signIn.jsp");
				requestdispatcher.forward(request, response);
			}
			
			
			break;
			
		case "simpleSearch":
			String searchWord = request.getParameter("search");
			
			ArrayList<PublicationBean> results = sql.getPublications(searchWord);
			session.setAttribute("searchResults", results);
			session.setAttribute("lastPage", ((results.size() - (results.size()%10))/10));

			session.setAttribute("currentPage", 0);
			session.setAttribute("searchWordPres", "'" + searchWord + "'");
			requestdispatcher = request.getRequestDispatcher("/results.jsp");
			requestdispatcher.forward(request, response);
			
			break;
			
			
		case "advancedSearch":
			String firstName = request.getParameter("authorFirstName");
			String lastName = request.getParameter("authorLastName");
			String title = request.getParameter("title");
			String year = request.getParameter("year");
			String type = request.getParameter("pubType");
			
			ArrayList<PublicationBean> advancedResults = sql.getPublicationsAdvanced(type, title, firstName, lastName, year);
			session.setAttribute("lastPage", ((advancedResults.size() - (advancedResults.size()%10))/10));
			session.setAttribute("searchResults", advancedResults);
			session.setAttribute("currentPage", 0);
			session.setAttribute("searchWordPres", "Advanced search");
			

			
			requestdispatcher = request.getRequestDispatcher("/results.jsp");
			requestdispatcher.forward(request, response);
			
			break;
			
			
		case "addPublication":
			PublicationBean newPub = new PublicationBean();
			
			UserBean currUser = (UserBean)session.getAttribute("currentUser");
			username = currUser.getUsername();
			
			String[] fullName = request.getParameter("author").split(" ");
			
			lastName = fullName[fullName.length - 1];
			firstName = "";
			for (int i = 0; i < fullName.length-1; i++) {
				firstName+=fullName[i] + " ";
			}
			
			newPub.setLastname(lastName);
			newPub.setFirstname(firstName);
			newPub.setAuthorid(sql.getAuthorIdByName(firstName, lastName));
			newPub.setPrice(request.getParameter("price"));
			newPub.setTitle(request.getParameter("title"));
			newPub.setType(request.getParameter("pubType"));
			newPub.setYear(request.getParameter("year"));
			
			sql.addPublication(newPub, username);
			requestdispatcher = request.getRequestDispatcher("/myProfilePublications.jsp");
			requestdispatcher.forward(request, response);
			
			break;
			
		case "addAuthor":
			
			
			firstName = request.getParameter("authorFirstName");
			lastName = request.getParameter("authorLastName");
		
			
			sql.addAuthor(firstName, lastName);
			
			requestdispatcher = request.getRequestDispatcher("/addBook.jsp");
			requestdispatcher.forward(request, response);
			
			
			
			
			break;
			
			
		case "Create new user":
			
			// Redirects to register.jsp
			requestdispatcher = request.getRequestDispatcher("/register.jsp");
			requestdispatcher.forward(request, response);
			
			break;
			
		case "Forgot password":
			session.setAttribute("getPasswordPressed", false);
			requestdispatcher = request.getRequestDispatcher("/forgotPassword.jsp");
			requestdispatcher.forward(request, response);
			
			break;
			
			
		case "Buy items":
			ArrayList<PublicationBean> buyCart = sql.getCart((String)session.getAttribute("currentUsername"));
			for (PublicationBean b : buyCart){
				b.incrementNumsold();
				sql.updatePublicationBean(b);
				//TODO: send mail til selger, trenger sql metode for å få USER på PublicationBean(Han som selger)
				UserBean usrbean = sql.getSeller(b);
				try {
					EmailSender.sendEmail(usrbean.getEmail(), "Hi, \n\nA copy of your "+b.getType()+ " "+b.getTitle()+" just got sold!\n\nCongratulations!");
				} catch (Exception e) {
					e.printStackTrace();
				}
				
			}
			UserBean ub =(UserBean) session.getAttribute("currentUser");
			sql.deleteCart(ub.getUsername());
			
			session.setAttribute("cart", new ArrayList<PublicationBean>());
			
			requestdispatcher = request.getRequestDispatcher("/shoppingCart.jsp");
			requestdispatcher.forward(request, response);
			break;
	
			
		case "Confirm Changes":
		
			boolean okChange = true;
			session.setAttribute("updateProfileError", "");
			
			if (!isValidFirstName(request.getParameter("fname"))){
				session.setAttribute("updateProfileError", "Not valid firstname, only letters a-z.");
				okChange=false;
			}
			else if (!isValidLastName(request.getParameter("lname"))){
				session.setAttribute("updateProfileError", "Not valid lastname, only letters a-z.");
				okChange=false;
			}
			
			else if (!isValidDOB(request.getParameter("bDate"))){
				session.setAttribute("updateProfileError", "Not valid date of birth, ex: 01031989.");
				okChange=false;
			}
			else if(!isValidAddress(request.getParameter("address"))){
				session.setAttribute("updateProfileError", "Address is not valid, please try again.");
				okChange=false;
			}
			else if (!isValidEmailAddress(request.getParameter("email"))){
				session.setAttribute("updateProfileError", "Not valid email, ex: john@gmail.com.");
				okChange=false;
			}
			else if (!isValidPassword(request.getParameter("pass"))){
				session.setAttribute("updateProfileError", "Not a valid password. Must be minimum"
						+ " six characters long.");
				okChange=false;
			}
			
			System.out.println(okChange + " "+ session.getAttribute("updateProfileError"));
			if(okChange){
				UserBean newUpdateUser = (UserBean)session.getAttribute("currentUser");
				
				newUpdateUser.setUsername(newUpdateUser.getUsername());
				
				newUpdateUser.setAddress(request.getParameter("address"));
				
				newUpdateUser.setCreditCard(request.getParameter("creditCardNr"));
				newUpdateUser.setEmail(request.getParameter("email"));
				newUpdateUser.setDateOfBirth(request.getParameter("bDate"));
				newUpdateUser.setFirstname(request.getParameter("fname"));
				newUpdateUser.setLastname(request.getParameter("lname"));
				newUpdateUser.setPassword(request.getParameter("pass"));
				
				//System.out.println("usr: " + newUpdateUser.getUsername() + "\nadress: " + newUpdateUser.getAddress() + "\nemail: "
				//		+ newUpdateUser.getEmail() + "\nDoB: " + newUpdateUser.getDateOfBirth()
				//);
			
				sql.updateUser(newUpdateUser);
				}
				requestdispatcher = request.getRequestDispatcher("/myProfile.jsp");
				requestdispatcher.forward(request, response);
				//TODO: vis registration error
				break;
			
		
		case "Register user":
			
			boolean ok = true;
			session.setAttribute("registrationError", "");
			
			if (!isAvailableUsername(request.getParameter("username"))){
				session.setAttribute("registrationError", "Not valid username, already taken.");
				ok=false;
			}
			else if(!isValidUsername(request.getParameter("username"))){
				session.setAttribute("registrationError", "Not valid username, only letters a-z and numbers.");
				ok=false;
			}
			else if (!isValidFirstName(request.getParameter("fname"))){
				session.setAttribute("registrationError", "Not valid firstname, only letters a-z.");
				ok=false;
			}
			else if (!isValidLastName(request.getParameter("lname"))){
				session.setAttribute("registrationError", "Not valid lastname, only letters a-z.");
				ok=false;
			}
			
			else if (!isValidDOB(request.getParameter("bDate"))){
				session.setAttribute("registrationError", "Not valid date of birth, ex: 01031989.");
				ok=false;
			}
			//Check Address here!
			else if(!isValidAddress(request.getParameter("adress"))){
				session.setAttribute("registrationError", "Address is not valid, please try again.");
				ok=false;
			}
			else if (!isAvailableEmailAddress(request.getParameter("email"))){
				session.setAttribute("registrationError", "Email already exists. Please try another one.");
				ok = false;
			}
			else if (!isValidEmailAddress(request.getParameter("email"))){
				session.setAttribute("registrationError", "Not valid email, ex: john@gmail.com.");
				ok=false;
			}
			else if (!isValidRepeatedEmail(request.getParameter("emailRep"), request.getParameter("email"))){
				session.setAttribute("registrationError", "Email addresses should be equal, please try again.");
				ok=false;
			}
			else if (!isValidPassword(request.getParameter("pass"))){
				session.setAttribute("registrationError", "Not a valid password. Must be minimum"
						+ " six characters long.");
				ok=false;
			}
			else if(!isValidRepeatedPassword(request.getParameter("pass"), request.getParameter("passRep"))){
				session.setAttribute("registrationError", "Passwords not equal, please try again.");
				ok=false;
			}
			
			
			System.out.println(ok + " "+ session.getAttribute("registrationError"));
			if(ok){
				UserBean newCurrentUser = new UserBean();
				
				session.setAttribute("currentUser", newCurrentUser);
				newCurrentUser.setUsername(request.getParameter("username"));
				
				newCurrentUser.setAddress(request.getParameter("adress"));
				
				newCurrentUser.setCreditCard(request.getParameter("creditCardNr"));
				newCurrentUser.setEmail(request.getParameter("email"));
				newCurrentUser.setDateOfBirth(request.getParameter("bDate"));
				newCurrentUser.setFirstname(request.getParameter("fname"));
				newCurrentUser.setLastname(request.getParameter("lname"));
				newCurrentUser.setPassword(request.getParameter("pass"));
				
				String confirmLink = getConfirmationLink(request.getParameter("username"));
				String hash = confirmLink.substring(confirmLink.length()-8);
				
				
				newCurrentUser.setConfirmationHash(hash);
//				System.out.println("usr: " + newCurrentUser.getUsername() + "\nadress: " + newCurrentUser.getAddress() + "\nemail: "
//						+ newCurrentUser.getEmail() + "\nDoB: " + newCurrentUser.getDateOfBirth()
//				);
			
				sql.setUserBean(newCurrentUser);
				
				
				
				
				
			try {
				EmailSender.sendEmail(request.getParameter("email"), "Hi and welcome to DBL, \n Please click the below link to confirm your email and create your account\n\n" + confirmLink + "\n\nRegards, \nDBL team :)");
			} catch (Exception e) {
				// TODO Auto-generated catch block
				
				e.printStackTrace();
			}
			}
			requestdispatcher = request.getRequestDispatcher("/register.jsp");
			requestdispatcher.forward(request, response);
			//TODO: vis registration error
			
			
			
			break;
			
			
		case "Sign in here": // Button in confirmationPage.jsp

			//Sends user to signIn.jsp
			requestdispatcher = request.getRequestDispatcher("/signIn.jsp");
			requestdispatcher.forward(request, response);	
			
			break;
			
			
		case "Get password":
			
			String emailForgot = request.getParameter("forgottenPwMail");
			
			UserBean userForgot = sql.getUserInfoFromEmail(emailForgot);
			if (sql.getEmails().contains(emailForgot)){
				
				try {
					EmailSender.sendEmail(emailForgot, "Hi,\n\n\nYour password is: \n\n" + userForgot.getPassword() + "\n\nRegards,\nTeam dbl.\n\n#secure");
				} catch (Exception e) {
					e.printStackTrace();
				}
			
			}
			session.setAttribute("getPasswordPressed", true);
			requestdispatcher = request.getRequestDispatcher("/forgotPassword.jsp");
			requestdispatcher.forward(request, response);
		
			break;
			
			
		case "Administrator login":
			String admUsername  = request.getParameter("loginUsername");
			String admPassword = request.getParameter("loginPass");
			
			//If username exists
			if (!isAvailableUsername(admUsername)){
				UserBean loginUser = sql.getUserInfo(admUsername);			
				
				
				
				// Checks for username and password up against db
				if(loginUser.getPassword().equals(admPassword) && loginUser.getAdmin()==1){ 
					session.setAttribute("adm", true);
					requestdispatcher = request.getRequestDispatcher("/adminHome.jsp");
					requestdispatcher.forward(request, response);
				}
				else{requestdispatcher = request.getRequestDispatcher("/adminLogin.jsp");
				requestdispatcher.forward(request, response);}
			}
			else{requestdispatcher = request.getRequestDispatcher("/adminLogin.jsp");
			requestdispatcher.forward(request, response);}
			
			break;
		
		case "User DB":
			ArrayList<String> allUsernames = sql.getUsernames();
			ArrayList<UserBean> allUsers = new ArrayList<UserBean>();
			Collections.sort(allUsernames.subList(1, allUsernames.size()));
			for (String u :allUsernames){
				allUsers.add(sql.getUserInfo(u));
				
				
			}
			session.setAttribute("allUsers", allUsers);
			requestdispatcher = request.getRequestDispatcher("/adminUserDB.jsp");
			requestdispatcher.forward(request, response);
			break;
			
			
			
		case "Publication DB":
			ArrayList<PublicationBean> allPublications = sql.getPublications("");
			session.setAttribute("allPublications", allPublications);
			requestdispatcher = request.getRequestDispatcher("/adminPublicationDB.jsp");
			requestdispatcher.forward(request, response);
			
			
			break;
			
		case "Sort by #Removed":
			ArrayList<PublicationBean> nspr = (ArrayList<PublicationBean>)session.getAttribute("allPublications");
			
			Collections.sort(nspr, (p2, p1) -> p1.getNumremoved() - p2.getNumremoved());
			
			session.setAttribute("allPublications", nspr);
			requestdispatcher = request.getRequestDispatcher("/adminPublicationDB.jsp");
			requestdispatcher.forward(request, response);
			break;
			
			
		case "Sort by #Sold":
			ArrayList<PublicationBean> nsp = (ArrayList<PublicationBean>)session.getAttribute("allPublications");
			
			Collections.sort(nsp, (p2, p1) -> p1.getNumsold() - p2.getNumsold());
			
			session.setAttribute("allPublications", nsp);
			requestdispatcher = request.getRequestDispatcher("/adminPublicationDB.jsp");
			requestdispatcher.forward(request, response);
			break;
			
			
		case "Toggle for sale":
			String admPubCheckboxValues[] = request.getParameterValues("publCheckbox");
			
			ArrayList<PublicationBean> allPubs2 = (ArrayList<PublicationBean>)session.getAttribute("allPublications");
			for (String str: admPubCheckboxValues){
				if (allPubs2.get(Integer.parseInt(str)).getSale() == 0){
					allPubs2.get(Integer.parseInt(str)).setSale(1);
					sql.updatePublicationBean(allPubs2.get(Integer.parseInt(str)));
				}else if(allPubs2.get(Integer.parseInt(str)).getSale() == 1){
					allPubs2.get(Integer.parseInt(str)).setSale(0);
					sql.updatePublicationBean(allPubs2.get(Integer.parseInt(str)));
				}
				
			}
			session.setAttribute("allPublications", allPubs2);
			requestdispatcher = request.getRequestDispatcher("/adminPublicationDB.jsp");
			requestdispatcher.forward(request, response);
			
			break;
			
		
			
			
		case "Toggle banned":
			String admCheckboxValues[] = request.getParameterValues("userCheckbox");
			
			ArrayList<UserBean> allUsers2 = (ArrayList<UserBean>)session.getAttribute("allUsers");
			for (String str: admCheckboxValues){
				if (allUsers2.get(Integer.parseInt(str)).getBanned() == 0){
					allUsers2.get(Integer.parseInt(str)).setBanned(1);
					sql.updateUser(allUsers2.get(Integer.parseInt(str)));
				}else if(allUsers2.get(Integer.parseInt(str)).getBanned() == 1){
					allUsers2.get(Integer.parseInt(str)).setBanned(0);
					sql.updateUser(allUsers2.get(Integer.parseInt(str)));
				}
				
			}
			session.setAttribute("allUsers", allUsers2);
			requestdispatcher = request.getRequestDispatcher("/adminUserDB.jsp");
			requestdispatcher.forward(request, response);
			
			break;
			
		
		case "Remove from Cart":
			
			String checkboxValues[] = request.getParameterValues("cartcheckbox");
			
			ArrayList<PublicationBean> cart = (ArrayList<PublicationBean>)session.getAttribute("cart");
			
			
			if(!(checkboxValues==null)){
				
				int i=0;
				for (String s:checkboxValues){
					PublicationBean toRemove = cart.get((Integer.parseInt(s)-i));
					cart.remove((Integer.parseInt(s)-i));
					toRemove.incrementNumremoved();
					sql.updatePublicationBean(toRemove);
					i++;
					
					
				}
				UserBean userBean = (UserBean)session.getAttribute("currentUser");
				
				sql.addCart(userBean.getUsername(), cart);
			}
			
			requestdispatcher = request.getRequestDispatcher("/shoppingCart.jsp");
			requestdispatcher.forward(request, response);
			break;
			
		case "Remove publication from database":
			
			String myPubCheckboxValues[] = request.getParameterValues("myPubCheckbox");
			ArrayList<PublicationBean> pubs = sql.getPublicationsAddedByUser((String)session.getAttribute("currentUsername"));
			int i=0;
			if(!(myPubCheckboxValues == null)){
				for (String s : myPubCheckboxValues){
					PublicationBean toRemove = pubs.get(Integer.parseInt(s)-i);
					sql.deletePublication(toRemove);
					i++;
				}
			}
			
			requestdispatcher = request.getRequestDispatcher("/myProfilePublications.jsp");
			requestdispatcher.forward(request, response);
			
			break;
			
		case "Toggle 'for sale' value":
		
			String pauseCheckboxValues[] = request.getParameterValues("myPubCheckbox");
			ArrayList<PublicationBean> pubs1 = sql.getPublicationsAddedByUser((String)session.getAttribute("currentUsername"));
			if(!(pauseCheckboxValues == null)){
				for (String s : pauseCheckboxValues){
					
					PublicationBean toPause = pubs1.get(Integer.parseInt(s));
					toPause.setSale((toPause.getSale() == 0) ? 1 : 0);
					sql.updatePublicationBean(toPause);
					
					
				}
			}
			requestdispatcher = request.getRequestDispatcher("/myProfilePublications.jsp");
			requestdispatcher.forward(request, response);
			
			break;
		
			
		case "Add to cart":
			String resultCheckboxValues[] = request.getParameterValues("resultsCheckbox");
			int cPage = (Integer)session.getAttribute("currentPage");
			
			UserBean usr = (UserBean)session.getAttribute("currentUser");
			ArrayList<PublicationBean> oldcart = sql.getCart(usr.getUsername());
			ArrayList<PublicationBean> searchRes = (ArrayList<PublicationBean>)session.getAttribute("searchResults");
			if(!(resultCheckboxValues==null)){
				
				for (String s:resultCheckboxValues){
					oldcart.add(searchRes.get(Integer.parseInt(s) + 10*cPage));
					
					
					
					
				}
			}
			session.setAttribute("cart", oldcart);
			sql.addCart(usr.getUsername(), oldcart);
			requestdispatcher = request.getRequestDispatcher("/shoppingCart.jsp");
			requestdispatcher.forward(request, response);
			break;
		
		case "Add to Cart":
			String randomCheckboxValues[] = request.getParameterValues("randomCheckbox");
			
			UserBean usr1 = (UserBean)session.getAttribute("currentUser");
			ArrayList<PublicationBean> oldcart1 = sql.getCart(usr1.getUsername());
			ArrayList<PublicationBean> rancart = (ArrayList<PublicationBean>)session.getAttribute("randomList");
			if(!(randomCheckboxValues==null)){
				
				for (String s:randomCheckboxValues){
					
					oldcart1.add(rancart.get(Integer.parseInt(s)));
					
					
					
					
				}
			}
			session.setAttribute("cart", oldcart1);
			sql.addCart(usr1.getUsername(), oldcart1);
			requestdispatcher = request.getRequestDispatcher("/shoppingCart.jsp");
			requestdispatcher.forward(request, response);
			
			
			break;
			
		case "Next page":
			int x = (Integer)session.getAttribute("currentPage");
			session.setAttribute("currentPage", x+1);
			
			requestdispatcher = request.getRequestDispatcher("/results.jsp");
			requestdispatcher.forward(request, response);
			break;
			
		case "Previous page":
			int y = (Integer)session.getAttribute("currentPage");
			session.setAttribute("currentPage", y-1);
			
			requestdispatcher = request.getRequestDispatcher("/results.jsp");
			requestdispatcher.forward(request, response);
			break;
			
			
		case "AddSinglePublicationToCart":
			PublicationBean newSinglePub = (PublicationBean)session.getAttribute("publicationId");
			ArrayList<PublicationBean> newCart = sql.getCart((String)session.getAttribute("currentUsername"));
			newCart.add(newSinglePub);
			sql.addCart((String)session.getAttribute("currentUsername"), newCart);
			session.setAttribute("cart", newCart);
			
			requestdispatcher = request.getRequestDispatcher("/shoppingCart.jsp");
			requestdispatcher.forward(request, response);
			break;
		
		case "visualSearch":
			String vs = request.getParameter("visualSearch");
			session.setAttribute("visualSearch", vs); 
			
			requestdispatcher = request.getRequestDispatcher("/graph.jsp");
			requestdispatcher.forward(request, response);
			break;
		}
		

		
		doGet(request, response);
	}

	
	
	
	
	
	// HELP METHODS
	
	
	//Check if username is valid
	public boolean isValidUsername(String username){
        if(!username.matches("^[a-zA-Z0-9]*$")){
            return false;
        }
		return true;
	}
	//Check if username already exists 
	public boolean isAvailableUsername(String username){
		if (sql.getUsernames().contains(username)) {
			return false;
		}
		return true;
	}
	//Check if firstname is valid
	public boolean isValidFirstName(String firstname){
		boolean result = true;
		if (firstname.isEmpty()){
			result = false;
		}
		return result;
	}
	//Check if lastname is valid
	public boolean isValidLastName(String lastname){
		boolean result = true;
		if (lastname.isEmpty() || lastname.contains("[0-9]*@!#%&/§∞€£™|[]≈{}¶‰¢¥®¡Ÿ()=?+^:;,§")){
			result = false;
		}
		return result;
	}
	// Check if birthdate is valid
	public boolean isValidDOB(String dob){
		if(!(dob.trim().length()==8)){
			return false;
		}
		if(!dob.matches("[0-9]+")){
			return false;
		}
		if(  (Integer.parseInt(dob.substring(0,2)) >31) || 
				(Integer.parseInt(dob.substring(2,4)) >12) ||
				Integer.parseInt(dob.substring(4,8)) >2016 || 
				Integer.parseInt(dob.substring(4,8)) <1900) {
					return false;
				}
		return true;
	}
	//Check if address is valid
	public boolean isValidAddress(String address){
		boolean result = true;
		if (address.isEmpty()){
			result = false;
		}
		return result;
	}
	//Check if email is valid
	public boolean isValidEmailAddress(String email) {
        boolean result = true;
        try {
           InternetAddress emailAddr = new InternetAddress(email);
           emailAddr.validate();
        } catch (AddressException ex) {
           result = false;
        }
        return result;
    }
	//Checks if email already exists
	public boolean isAvailableEmailAddress(String email){
		boolean result = true;
		ArrayList<String> emails = sql.getEmails();
		if (emails.contains(email)) {
			result = false;
		}
		return result;
	}
	//Check if repeated email is equal to the first one 
	public boolean isValidRepeatedEmail(String repeatedEmail, String email){
		boolean result = true;
		if (!repeatedEmail.equals(email)){
			result = false;
		}
		return result;
	}
	//Check if password is long enough 
	public boolean isValidPassword(String password){
		boolean result = true;
		if (password.length()<6){
			result = false;
		}
		return result;
	}
	//Check if repeated password is equal to the first one
	public boolean isValidRepeatedPassword(String repeatedPassword, String password){
		boolean result = true;
		if (!repeatedPassword.equals(password)){
			result = false;
		}
		return result;
	}
	
	
	public String getConfirmationLink(String username){
		
		
		String confirmationLinkHash = getRandomString(8);
		String res = "http://localhost:8080/Web-Applications-Engineering/confirmationPage.jsp?user=" + username + "&hash=" + confirmationLinkHash;
		
		return res;
	}
	
	
	public String getRandomString(int len){
		char[] chars = "abcdefghijklmnopqrstuvwxyz".toCharArray();
		StringBuilder sb = new StringBuilder();
		Random random = new Random();
		for (int i = 0; i < len; i++) {
		    char c = chars[random.nextInt(chars.length)];
		    sb.append(c);
		}
		return sb.toString();
	}
	
	
	
	
	
	
}
