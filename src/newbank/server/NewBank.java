package newbank.server;

import java.util.HashMap;
import java.util.Scanner;

public class NewBank {

	private static final NewBank bank = new NewBank();
	private static HashMap<String,Customer> customers;

	public NewBank() {
		customers = new HashMap<>();
		addTestData();
	}

	private static void addTestData() {
		Customer bhagy = new Customer();
		bhagy.addAccount(new Account("Main", 1000.0));
		bhagy.addAccount(new Account("Second", 2000.0));
		bhagy.addAccount(new Account("very long string of accounts", 3000000.0));
		customers.put("Bhagy", bhagy);

		Customer christina = new Customer();
		christina.addAccount(new Account("Savings", 1500.0));
		customers.put("Christina", christina);

		Customer john = new Customer();
		john.addAccount(new Account("Checking", 250.0));
		customers.put("John", john);
	}

	public static NewBank getBank() {
		return bank;
	}

	public synchronized CustomerID checkLogInDetails(String userName, String password) {
		if(customers.containsKey(userName)) {
			return new CustomerID(userName);
		}
		return null;
	}

	// commands from the NewBank customer are processed in this method
	public synchronized String processRequest(CustomerID customer, String request) {
		if(customers.containsKey(customer.getKey())) {
			switch(request) {
				case "SHOWMYACCOUNTS" : return showMyAccounts(customer);
				default : return "FAIL";
			}
		}
		return "FAIL";
	}

	private String showMyAccounts(CustomerID customer) {
		return (customers.get(customer.getKey())).accountsToString();
	}

	public static void run()
	{
		NewBank nb=new NewBank();
		nb.addTestData();
		Scanner scn=new Scanner(System.in);
		String ch="";
		//ask for the name and get the customer back using the name by using the function get Index
		System.out.println("Enter name: ");
		String name=scn.nextLine();
		Customer id=getIndex(name);
		if(id==null)
		{
			System.out.println("No user exists!");
			return;
		}
		//do while loop which runs until user enters exit.
		do
		{
			showMenu();
			ch=scn.nextLine();
			//if user chooses 1 then ask for name of the money receiver and then check if he exists
			//then if he exists transfer the amount.
			if(ch.equalsIgnoreCase("1"))
			{
				System.out.print("Enter the Username of Reciever: ");
				String newP=scn.nextLine();
				Customer index=getIndex(newP);
				if(index==null)
				{
					System.out.println("No such user exists!");
					continue;
				}
				System.out.print("Enter the Amount to transfer:  ");
				double amount=scn.nextDouble();

				id.getAccounts().get(0).transfer(index.getAccounts().get(0), amount);
				scn.nextLine();
			}
			//check if user has 2 accounts if yes then transfer from first to second.
			else if(ch.equalsIgnoreCase("2"))
			{
				if(id.getAccounts().size()<2)
				{
					System.out.println("You dont have 2 accounts!");
					continue;
				}
				System.out.print("Enter the Amount to transfer to savings:  ");
				double amount=scn.nextDouble();

				id.getAccounts().get(0).transfer(id.getAccounts().get(1), amount);
				scn.nextLine();
			}
			//check if user has 2 accounts if yes then transfer from second to first.
			else if(ch.equalsIgnoreCase("3"))
			{
				if(id.getAccounts().size()<2)
				{
					System.out.println("You dont have 2 accounts!");
					continue;
				}
				System.out.print("Enter the Amount to transfer to Current:  ");
				double amount=scn.nextDouble();
				id.getAccounts().get(1).transfer(id.getAccounts().get(0), amount);
				scn.nextLine();
			}
			//if user enters 4 then quit the program.
			else if(!ch.equalsIgnoreCase("4"))
			{
				System.out.println("Wrong choice enter again.");
				scn.nextLine();
			}
		}
		while(!ch.equalsIgnoreCase("4"));
	}
	//show menu function.
	private static void showMenu()
	{
		System.out.println("1. Transfer\n2. Saving Account transfer\n3. Current Account Transfer\n4. Quit");
	}
	//get the index of the customer from customers hashset
	private static Customer getIndex(String newP)
	{
		return customers.getOrDefault(newP,null);
	}


}
