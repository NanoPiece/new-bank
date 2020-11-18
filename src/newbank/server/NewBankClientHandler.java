package newbank.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class NewBankClientHandler extends Thread {

	private NewBank bank;
	private BufferedReader in;
	private PrintWriter out;

	public NewBankClientHandler(Socket s) throws IOException {
		bank = NewBank.getBank();
		in = new BufferedReader(new InputStreamReader(s.getInputStream()));
		out = new PrintWriter(s.getOutputStream(), true);
	}

	public NewBankClientHandler() {
	}

	public void createUser() {
		try {
			out.println("Please create a username:");
			String userName = in.readLine();         // accept a username from customer
			Customer newCustomer = new Customer();       // create new customer
			newCustomer.addAccount(new Account("Main", 00.0));    // create a default account for the customer
			bank.customers.put(userName, newCustomer);        // add the customer to the list of customers and assign their username
		} catch (IOException e) {
			e.printStackTrace();
		}
		run();
	}

	public void createNewAccount(CustomerID customer){
		try {
			out.println("Please select an account type:\n");
			out.println("Current Account");
			out.println("Savings Account");
			String accountType = in.readLine();         // accept an account type from customer
			while (!(accountType.toUpperCase().equals("CURRENT ACCOUNT")) && (!(accountType.toUpperCase().equals("SAVINGS ACCOUNT")))){
				out.println("Please try again");          // ensure customer's entry is valid
				accountType = in.readLine();
			}
			bank.customers.get(customer.getKey()).addAccount(new Account(accountType, 00.0));    // create a new account for the customer
			out.println("Account Created");
		} catch (IOException e) {
			e.printStackTrace();
		}
		run();
	}

	public void run() {
		// keep getting requests from the client and processing them
		try {
			out.println("Please Choose From The Below Options:\n");      // offer log in or create account
			out.println("Log In");
			out.println("Create User");
			String customerAction = in.readLine();
			while (!(customerAction.toUpperCase().equals("CREATE USER")) && (!(customerAction.toUpperCase().equals("LOG IN")))){
				out.println("Please try again");          // ensure customer's entry is valid
				customerAction = in.readLine();
			}
			if (customerAction.toUpperCase().equals("CREATE USER")){
				createUser();                // direct to account creation where they will be able to choose a username and continue
			}
			if (customerAction.toUpperCase().equals("LOG IN")) {
				// ask for user name
				out.println("Enter Username");
			}
			String userName = in.readLine();
			// ask for password
			out.println("Enter Password");
			String password = in.readLine();
			out.println("Checking Details...");
			// authenticate user and get customer ID token from bank for use in subsequent requests
			CustomerID customer = bank.checkLogInDetails(userName, password);
			// if the user is authenticated then get requests from the user and process them 
			if(customer != null) {
				String response = bank.processRequest(customer, "SHOWMYACCOUNTS");
				out.println(response);
				out.println("Log In Successful. What do you want to do?");
				while(true) {
					String request = in.readLine();
					System.out.println("Request from " + customer.getKey());
					String responce = bank.processRequest(customer, request);
					out.println(responce);
				}
			}
			else {
				out.println("Log In Failed");
			}
		} catch (IOException e) {
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

}
