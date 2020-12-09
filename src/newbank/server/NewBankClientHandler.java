package newbank.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class NewBankClientHandler extends Thread{

	private NewBank bank;
	private BufferedReader in;
	private PrintWriter out;

	public NewBankClientHandler(Socket s) throws IOException {
		bank = NewBank.getBank();
		in = new BufferedReader(new InputStreamReader(s.getInputStream()));
		out = new PrintWriter(s.getOutputStream(), true);
	}


	public void run() {
		// keep getting requests from the client and processing them
		try {
			clearScreen();
			out.println("Welcome to New Bank\n\nPlease choose from the options below:\n");      // offer log in or create account
			out.println("1. Log In");
			out.println("2. Create User");
			String customerAction = in.readLine();

			while (!(customerAction.equals("1")) && (!(customerAction.equals("2")))) {
				out.println("Please try again");          // ensure customer's entry is valid
				customerAction = in.readLine();
			}
			if (customerAction.equals("2")) {
				out.println("What is your name?");
				String actualName = in.readLine();
				out.println("Please create a username:");
				String userName = in.readLine();
				out.println("Please create a password:");
				String password = in.readLine();

				String request = "CREATEACCOUNT" + "," + actualName + "," + userName + "," + password;
				String response = bank.processAccountCreationRequest(request);
				out.println(response);          // direct to account creation where they will be able to choose a username and continue
				while(response == "Password is not strong enough. Please create a new password:"){
					password = in.readLine();
					request = "CREATEACCOUNT" + "," + actualName + "," + userName + "," + password;
					response = bank.processAccountCreationRequest(request);
					out.println(response);
				}

				while (response == "The username already exists.\nPlease enter a unique username or type 'esc' to return to the menu screen.") {
					userName = in.readLine();
					if (userName.equals("esc")){
						out.println("Loading menu screen...\n");
						sleep();
						break;
					}
				}
				if (!userName.equals("esc")) {
					clearScreen();
					out.println("User: '" + userName + "' Created\n");
					out.println("Loading menu screen...\n");
					sleep();
					run();
				} else if (userName.equals("esc")){
					out.println("Loading menu screen...\n");
					sleep();
					run();
				}

			}
			if (customerAction.equals("1")) {
				clearScreen();
				out.println("Enter Username");              // ask for user name
			}
			String userName = in.readLine();
			// ask for password
			out.println("Enter Password");
			String password = in.readLine();
			out.println("Checking Details...");

			// authenticate user and get customer ID token from bank for use in subsequent requests
			CustomerID customer = bank.checkLogInDetails(userName, password);
			while (customer==null){
				out.println("Log In Failed. Please try again");
				out.println("Enter Username");
				userName = in.readLine();
				// ask for password
				out.println("Enter Password");
				password = in.readLine();
				out.println("Checking Details...");
				customer = bank.checkLogInDetails(userName, password);
			}
			// if the user is authenticated then get requests from the user and process them

			//Customer current= bank.getIndex(userName);
			out.println("Log In Successful. What do you want to do?");
			while(true) {
				out.println(showMenu());
				String request = in.readLine();
				if (request.equals("1")){
					clearScreen();
					String dashboard = bank.processRequest(customer, "1");
					out.println(dashboard);
				} else if (request.equals("2")){
					clearScreen();
					out.println("Enter the Account that you want to change the name for:  ");
					String accountName = SelectAccount(customer);

					// Request new account name from user
					out.println("Please type in the new name for your selected account.");
					String newAccountName = in.readLine();
					newAccountName = newAccountName.trim();

					request += "," + accountName + "," + newAccountName;

					String response = bank.processRequest(customer, request);
					out.println(response);
					returnToMenu();

				} else if(request.equals("3")){
					clearScreen();
					out.println("Enter the Username of Receiver: ");
					String receiver = in.readLine();

					out.println("Enter the Amount to transfer:  ");
					String amount_totransfer = in.readLine();

					out.println("Enter the Account that you want to transfer from:  ");
					String accountName = SelectAccount(customer);

					out.println("Please type in the 6-digit authentication number shown in your Google Authenticator App");
					String authNumber = in.readLine();

					request += "," + receiver + "," + amount_totransfer + "," + accountName + "," + authNumber;;

					String response = bank.processRequest(customer, request);
					out.println(response);
					returnToMenu();

				} else if (request.equals("4")){
					clearScreen();
					out.println("Enter the Account that you want to transfer from:  ");
					String account_from = SelectAccount(customer);

					out.println("Enter the Account that you want to transfer to:  ");
					String account_to = SelectAccount(customer);

					out.println("Enter the Amount to transfer:  ");
					String string_amount = in.readLine();

					out.println("Please type in the 6-digit authentication number shown in your Google Authenticator App");
					String authNumber = in.readLine();

					request += "," + account_from + "," + account_to + "," + string_amount + "," + authNumber;

					String response = bank.processRequest(customer, request);
					out.println(response);

				} else if (request.equals("5")){
					clearScreen();
					out.println("Please select an account type:\n");
					out.println("1. Current Account");  // take account type
					out.println("2. Savings Account");
					String accountType = in.readLine();
					request += "," + accountType;
					while (!(accountType.equals("1")) && (!(accountType.equals("2")))) {
						out.println("Please try again");   // ensure customer's entry is valid
					}
					String response = bank.processRequest(customer, request);
					out.println(response);

				} else if (request.equals("6")){
					out.println("Please type in the 6-digit authentication number shown in your Google Authenticator App");
					String authNumber = in.readLine();
					request += "," + authNumber;
					String response = bank.processRequest(customer, request);
					out.println(response);

				} else if (request.equals("7")){
					// cancel a scheduled transfer
					// show scheduled transfers
					out.println("Please type in the 6-digit authentication number shown in your Google Authenticator App");
					String authNumber = in.readLine();
					request = "6" + "," + authNumber;
					String response = bank.processRequest(customer, request);
					out.println(response);
					while (response.equals("Not able to show scheduled actions: Authentication fail")){
						out.println("Please type in the 6-digit authentication number shown in your Google Authenticator App");
						authNumber = in.readLine();
						request = "6" + "," + authNumber;
						response = bank.processRequest(customer, request);
						out.println(response);
					}
					// get id of transfer to be cancelled
					out.println("Enter number of transaction you wish to cancel (type 0 if there are no scheduled actions):");
					String cancelTransaction = in.readLine();
					request = "7" + "," + cancelTransaction + "," + authNumber;

					response = bank.processRequest(customer, request);
					out.println(response);
				} else if (request.equals("8")) {
					clearScreen();
					out.println("Logging out...");
					sleep();
					run();
				} else if (request.equals("9")){
					clearScreen();
					out.println("Thank you and have a nice day!");
					System.exit(0);
				} else if(!request.equalsIgnoreCase("6")) {
					clearScreen();
					out.println("Invalid Entry\n");
				} else {
					System.out.println("Request from " + customer.getKey());
					String responce = bank.processRequest(customer, request);
					out.println(responce);
					returnToMenu();
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		finally {
			try {
				in.close();
				out.close();
			} catch (IOException e) {
				e.printStackTrace();
				Thread.currentThread().interrupt();
			}
		}
	}

	private String showMenu()
	{
		return "1. Show My Accounts\n2. Change Account Names\n3. Transfer to another user\n4. Transfer to another owned account\n5. Create New Account\n6. Show scheduled transfers\n7. Cancel a scheduled transfer\n8. Log out\n9. Quit";
	}

	public void clearScreen() {
		out.print("\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n"); // utilised while this function will not work in IntelliJ
		out.print("\033[H\033[2J");
		out.flush();
	}

	private void returnToMenu() throws InterruptedException { ;
		out.println("\nReturning to menu screen...");
		sleep();
		clearScreen();
		showMenu();
	}
	private void sleep() throws InterruptedException {
		Thread.sleep(3000);
	}

	private String SelectAccount(CustomerID customer) throws Exception {
		//out.println("Enter the Account that you want to transfer from:  ");
		String selectableAccounts = bank.processRequest(customer, "DISPLAYSELECTABLEACCOUNTS");
		String option = "";
		String[] listOfSelections = selectableAccounts.split("\\n");
		boolean b = true;
		while (b){
			try{
				out.println(selectableAccounts);
				option = in.readLine();
				option = option.trim();
				while (Integer.parseInt(option) > listOfSelections.length ||
						Integer.parseInt(option) <= 0){
					out.println("Please select a valid option:");
					out.println(selectableAccounts);
					option = in.readLine();
					option = option.trim();
				}
				b = false;
			} catch (NumberFormatException | IOException ex) {
				out.println("Please enter an integer only!");
			}
		}
		// Retrieve selected account name
		String accountName = listOfSelections[Integer.parseInt(option)-1].substring(
				selectableAccounts.indexOf(". "));

		return accountName.substring(accountName.indexOf(" ")+1);
	}

}
