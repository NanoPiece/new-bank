
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
			// ask for user name
			out.println("Enter Username");
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
					else if(!request.equalsIgnoreCase("5"))
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
		return "1. Show My Accounts\n2. Transfer\n3. Saving Account transfer\n4. Current Account Transfer\n5. Quit";
	}

}