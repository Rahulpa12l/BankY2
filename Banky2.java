import java.sql.*;
import java.util.Scanner;

 public class BankY2 {
    private static final String DB_URL = "jdbc:Orecle:Driver:OrecleDriver";

    public static void main(String[] args) {
        try (Connection conn = DriverManager.getConnection(DB_URL,"Scott","tiger")) {
            createTables(conn);
            runBankingSystem(conn);
        } catch (SQLException e) {
            System.out.println("Database connection error: " + e.getMessage());
        }
    }

    private static void createTables(Connection conn) throws SQLException {
        String createAccountsTable = "CREATE TABLE IF NOT EXISTS accounts (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "name TEXT NOT NULL," +
                "balance REAL NOT NULL DEFAULT 0.0);";
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(createAccountsTable);
        }
    }

    private static void runBankingSystem(Connection conn) {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.println("\nBankY Menu:");
            System.out.println("1. Create Account");
            System.out.println("2. Deposit Funds");
            System.out.println("3. Withdraw Funds");
            System.out.println("4. Transfer Funds");
            System.out.println("5. Exit");
            System.out.print("Choose an option: ");

            int choice = scanner.nextInt();
            scanner.nextLine(); // Consume newline

            switch (choice) {
                case 1:
                    createAccount(conn, scanner);
                    break;
                case 2:
                    depositFunds(conn, scanner);
                    break;
                case 3:
                    withdrawFunds(conn, scanner);
                    break;
                case 4:
                    transferFunds(conn, scanner);
                    break;
                case 5:
                    System.out.println("Exiting BankY. Goodbye!");
                    return;
                default:
                    System.out.println("Invalid choice. Please try again.");
            }
        }
    }

    private static void createAccount(Connection conn, Scanner scanner) {
        System.out.print("Enter your name: ");
        String name = scanner.nextLine();
        
        String insertAccount = "INSERT INTO accounts (name, balance) VALUES (?, 0.0);";
        try (PreparedStatement pstmt = conn.prepareStatement(insertAccount)) {
            pstmt.setString(1, name);
            pstmt.executeUpdate();
            System.out.println("Account created successfully for " + name);
        } catch (SQLException e) {
            System.out.println("Error creating account: " + e.getMessage());
        }
    }

    private static void depositFunds(Connection conn, Scanner scanner) {
        System.out.print("Enter account ID: ");
        int accountId = scanner.nextInt();
        System.out.print("Enter amount to deposit: ");
        double amount = scanner.nextDouble();

        String updateBalance = "UPDATE accounts SET balance = balance + ? WHERE id = ?;";
        try (PreparedStatement pstmt = conn.prepareStatement(updateBalance)) {
            pstmt.setDouble(1, amount);
            pstmt.setInt(2, accountId);
            int rowsUpdated = pstmt.executeUpdate();

            if (rowsUpdated > 0) {
                System.out.println("Deposit successful.");
            } else {
                System.out.println("Account not found.");
            }
        } catch (SQLException e) {
            System.out.println("Error depositing funds: " + e.getMessage());
        }
    }

    private static void withdrawFunds(Connection conn, Scanner scanner) {
        System.out.print("Enter account ID: ");
        int accountId = scanner.nextInt();
        System.out.print("Enter amount to withdraw: ");
        double amount = scanner.nextDouble();

        String checkBalance = "SELECT balance FROM accounts WHERE id = ?;";
        String updateBalance = "UPDATE accounts SET balance = balance - ? WHERE id = ?;";
        try (PreparedStatement checkStmt = conn.prepareStatement(checkBalance);
             PreparedStatement updateStmt = conn.prepareStatement(updateBalance)) {
            checkStmt.setInt(1, accountId);
            ResultSet rs = checkStmt.executeQuery();

            if (rs.next()) {
                double currentBalance = rs.getDouble("balance");
                if (currentBalance >= amount) {
                    updateStmt.setDouble(1, amount);
                    updateStmt.setInt(2, accountId);
                    updateStmt.executeUpdate();
                    System.out.println("Withdrawal successful.");
                } else {
                    System.out.println("Insufficient balance.");
                }
            } else {
                System.out.println("Account not found.");
            }
        } catch (SQLException e) {
            System.out.println("Error withdrawing funds: " + e.getMessage());
        }
    }

    private static void transferFunds(Connection conn, Scanner scanner) {
        System.out.print("Enter source account ID: ");
        int sourceAccountId = scanner.nextInt();
        System.out.print("Enter destination account ID: ");
        int destAccountId = scanner.nextInt();
        System.out.print("Enter amount to transfer: ");
        double amount = scanner.nextDouble();

        String checkBalance = "SELECT balance FROM accounts WHERE id = ?;";
        String withdrawFunds = "UPDATE accounts SET balance = balance - ? WHERE id = ?;";
        String depositFunds = "UPDATE accounts SET balance = balance + ? WHERE id = ?;";

        try (PreparedStatement checkStmt = conn.prepareStatement(checkBalance);
             PreparedStatement withdrawStmt = conn.prepareStatement(withdrawFunds);
             PreparedStatement depositStmt = conn.prepareStatement(depositFunds)) {
            conn.setAutoCommit(false); // Start transaction

            // Check source account balance
            checkStmt.setInt(1, sourceAccountId);
            ResultSet rs = checkStmt.executeQuery();

            if (rs.next()) {
                double currentBalance = rs.getDouble("balance");
                if (currentBalance >= amount) {
                    // Withdraw from source
                    withdrawStmt.setDouble(1, amount);
                    withdrawStmt.setInt(2, sourceAccountId);
                    withdrawStmt.executeUpdate();

                    // Deposit to destination
                    depositStmt.setDouble(1, amount);
                    depositStmt.setInt(2, destAccountId);
                    depositStmt.executeUpdate();

                    conn.commit(); // Commit transaction
                    System.out.println("Transfer successful.");
                } else {
                    System.out.println("Insufficient balance.");
                    conn.rollback();
                }
            } else {
                System.out.println("Source account not found.");
                conn.rollback();
            }
        } catch (SQLException e) {
            System.out.println("Error transferring funds: " + e.getMessage());
            try {
                conn.rollback();
            } catch (SQLException rollbackEx) {
                System.out.println("Error during rollback: " + rollbackEx.getMessage());
            }
        } finally {
            try {
                conn.setAutoCommit(true);
            } catch (SQLException e) {
                System.out.println("Error resetting auto-commit: " + e.getMessage());
            }
        }
    }
}
