
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

	public void createUser() {
		try {
			out.println("Please create a username:");
			String userName = in.readLine();         // accept a username from customer
			Customer newCustomer = new Customer();       // create new customer
			newCustomer.addAccount(new Account("Main", 00.0));    // create a default account for the customer
			bank.customers.put(userName, newCustomer);        // add the customer to the list of customers and assign their username
			out.println("User: '" + userName + "' Created");
		} catch (IOException e) {
			e.printStackTrace();
		}
		run();
	}

	public void run() {
		// keep getting requests from the client and processing them
		try {
				out.println("Please Choose From The Below Options:\n");      // offer log in or create account
				out.println("1. Log In");
				out.println("2. Create User");
				String customerAction = in.readLine();
				while (!(customerAction.equals("1")) && (!(customerAction.equals("2")))) {
					out.println("Please try again");          // ensure customer's entry is valid
					customerAction = in.readLine();
				}
				if (customerAction.equals("2")) {
					createUser();                // direct to account creation where they will be able to choose a username and continue
				}
				if (customerAction.equals("1")) {
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

				Customer current= bank.getIndex(userName);
				String response = bank.processRequest(customer, "1");
				out.println(response);
				out.println("Log In Successful. What do you want to do?");
				out.println(showMenu());

				Boolean exit = true;
				while(exit) {
					String request = in.readLine();
					System.out.println("Request from " + customer.getKey());
					if(request.equals("2")){
						out.println("Enter the Username of Reciever: ");
						String receiver = in.readLine();

						Customer index= bank.getIndex(receiver);
						if(index==null)
						{
							out.println("No user exists!");
							continue;
						}

						out.println("Enter the Amount to transfer:  ");
						String string_amount = in.readLine();
						Double amount = Double.valueOf(string_amount);
						current.getAccounts().get(0).transfer(index.getAccounts().get(0), amount);
					}
					else if (request.equals("3")){
						if(current.getAccounts().size()<2)
						{
							out.println("You dont have 2 accounts!");
							continue;
						}
						out.println("Enter the Amount to transfer to savings:  ");
						String string_amount = in.readLine();
						Double amount = Double.valueOf(string_amount);
						current.getAccounts().get(0).transfer(current.getAccounts().get(1), amount);
					}
					else if (request.equals("4")){
						if(current.getAccounts().size()<2)
						{
							out.println("You dont have 2 accounts!");
							continue;
						}
						out.println("Enter the Amount to transfer to Current:  ");
						String string_amount = in.readLine();
						Double amount = Double.valueOf(string_amount);
						current.getAccounts().get(1).transfer(current.getAccounts().get(0), amount);
					}
					else if (request.equals("5")){
						String quit = bank.processRequest(customer, request);
						out.println(quit);
						System.exit(0);
					}
					else if (request.equals("6")){
						try {
							out.println("Please select an account type:\n");
							out.println("1. Current Account");  // take account type
							out.println("2. Savings Account");
							String accountType = in.readLine();
							request += "," + accountType;
							while (!(accountType.equals("1")) && (!(accountType.equals("2")))) {
								out.println("Please try again");   // ensure customer's entry is valid
							}
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
					else if(!request.equalsIgnoreCase("6"))
					{
						out.println("Wrong choice enter again.");
					}
					String responce = bank.processRequest(customer, request);
					out.println(responce);
					out.println("Is there anything else that you would like to do?");
					out.println(showMenu());
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

	private String showMenu()
	{
		return "1. Show My Accounts\n2. Transfer\n3. Saving Account transfer\n4. Current Account Transfer\n5. Quit\n6. Create New Account";
	}

}