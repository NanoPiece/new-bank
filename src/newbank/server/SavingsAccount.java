package newbank.server;

public class SavingsAccount extends Account {

    public Boolean isSavingsAccount = true;

    public SavingsAccount(String username, double amount) {
        super(username, amount);
        this.accountName = username;
        this.openingBalance = amount;
    }

    public Boolean isSavingsAccount() {return this.isSavingsAccount;}
}
