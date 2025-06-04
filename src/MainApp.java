
import campusPc.*;
import javax.swing.*;
import java.awt.*;

public class MainApp {
    public static void main(String[] args) {
        // Load JDBC driver explicitly
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            JOptionPane.showMessageDialog(null,
                    "MySQL JDBC Driver not found! Add the JAR to your lib folder.",
                    "Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }


        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Campus PC Management System");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(800, 600);

            // Create card layout for switching panels
            JPanel mainPanel = new JPanel(new CardLayout());

            // Create panels
            JPanel menuPanel = new JPanel(new GridLayout(2, 1, 10, 10));
            JButton registerBtn = new JButton("Student Registration");
            JButton scanBtn = new JButton("PC Exit Scanner");

            RegistrationPanel registrationPanel = new campusPc.RegistrationPanel(mainPanel);
            ScannerPanel scannerPanel = new ScannerPanel(mainPanel);

            // Add action listeners
            registerBtn.addActionListener(e -> {
                CardLayout cl = (CardLayout) mainPanel.getLayout();
                cl.show(mainPanel, "REGISTER");
            });

            scanBtn.addActionListener(e -> {
                CardLayout cl = (CardLayout) mainPanel.getLayout();
                cl.show(mainPanel, "SCAN");
            });

            // Build menu panel
            menuPanel.add(registerBtn);
            menuPanel.add(scanBtn);

            // Add all panels to card layout
            mainPanel.add(menuPanel, "MENU");
            mainPanel.add(registrationPanel, "REGISTER");
            mainPanel.add(scannerPanel, "SCAN");

            // Show menu first
            CardLayout cl = (CardLayout) mainPanel.getLayout();
            cl.show(mainPanel, "MENU");

            frame.add(mainPanel);
            frame.setVisible(true);
        });
    }
}
