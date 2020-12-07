package newbank.server;

public class MicroLoan {
    /* --- Attributes --- */
    private String description;
    private String status;
    private double requestedAmount;
    private double paidAmount;
    private double annualInterestRate;
    private String borrowerID;
    private String lenderID;
    private int loanID;

    /* --- Methods --- */
    // Create a new MicroLoan object
    public MicroLoan(String borrowerID, String description,
                     double requestedAmount, double annualInterestRate, int loanID) {
        this.borrowerID = borrowerID;
        this.description = description;
        this.requestedAmount = requestedAmount;
        this.annualInterestRate = annualInterestRate;
        this.paidAmount = 0.0;
        this.lenderID = null;
        this.status = "Applied";
        this.loanID = loanID;
    }

    // Return displayable strings


    /* --- Getters and setters --- */
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public double getRequestedAmount() { return requestedAmount; }
    public void setRequestedAmount(double requestedAmount) { this.requestedAmount = requestedAmount; }
    public double getPaidAmount() { return paidAmount; }
    public void setPaidAmount(double paidAmount) { this.paidAmount = paidAmount; }
    public double getAnnualInterestRate() { return annualInterestRate; }
    public void setAnnualInterestRate(double interestRate) { this.annualInterestRate = interestRate; }
    public String getBorrowerID() { return borrowerID; }
    public void  setBorrowerID(String borrowerID) { this.borrowerID = borrowerID; }
    public String getLenderID() { return lenderID; }
    public void setLenderID() { this.lenderID = lenderID; }
    public String getStatus() { return status; };
    public void setStatus(String status) { this.status = status; }
    public int getLoanID() { return loanID;};
}
