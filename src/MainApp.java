

import javax.swing.*;
import java.awt.*;
import campusPc.*;

public class MainApp {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            DBConnection.initializeDatabase();

            JFrame frame = new JFrame("Campus PC Management System");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(800, 600);
            frame.setLocationRelativeTo(null); // Center the frame on screen

            // Create card layout for switching panels
            JPanel mainPanel = new JPanel(new CardLayout());

            // Use a JPanel for the menu, set its layout to GridBagLayout for centering buttons.
            JPanel menuPanel = new JPanel(new GridBagLayout());
            menuPanel.setBackground(new Color(230, 240, 250)); // Light blue background

            // Create buttons
            JButton registerBtn = new JButton("Student Registration");
            JButton scanBtn = new JButton("PC Exit Scanner");

            // Customize button appearance (size, font, colors, etc.)
            customizeMenuButton(registerBtn);
            customizeMenuButton(scanBtn);

            // Instantiate other panels
            RegistrationPanel registrationPanel = new RegistrationPanel(mainPanel);
            ScannerPanel scannerPanel = new ScannerPanel(mainPanel);

            // Add action listeners for navigation
            registerBtn.addActionListener(e -> {
                CardLayout cl = (CardLayout) mainPanel.getLayout();
                cl.show(mainPanel, "REGISTER"); // Show the registration panel
            });

            scanBtn.addActionListener(e -> {
                CardLayout cl = (CardLayout) mainPanel.getLayout();
                cl.show(mainPanel, "SCAN"); // Show the scanner panel
            });

            // Add buttons to the menuPanel using GridBagLayout
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0; // Place components in the first (and only) column
            gbc.gridy = 0; // Start at the first row
            gbc.insets = new Insets(20, 0, 20, 0); // Add more vertical padding between buttons
            gbc.anchor = GridBagConstraints.CENTER; // Center the buttons within their grid cells

            // Add the register button
            menuPanel.add(registerBtn, gbc);

            // Move to the next row for the scan button
            gbc.gridy = 1;
            menuPanel.add(scanBtn, gbc);

            // Add all panels to the main CardLayout panel
            mainPanel.add(menuPanel, "MENU");
            mainPanel.add(registrationPanel, "REGISTER");
            mainPanel.add(scannerPanel, "SCAN");

            // Show the menu panel first when the application starts
            CardLayout cl = (CardLayout) mainPanel.getLayout();
            cl.show(mainPanel, "MENU");

            // Add the mainPanel to the frame and make it visible
            frame.add(mainPanel);
            frame.setVisible(true);
        });

    }
    /**
     * Applies consistent styling to the menu buttons.
     * This method is kept to easily customize button size, font, and colors.
     * @param button The JButton to style.
     */
    private static void customizeMenuButton(JButton button) {
        button.setPreferredSize(new Dimension(250, 70)); // Slightly larger buttons for menu
        button.setFont(new Font("Arial", Font.BOLD, 20)); // Larger font
        button.setBackground(new Color(0, 102, 204)); // A darker blue for contrast
        button.setForeground(Color.WHITE); // White text
        button.setFocusPainted(false); // Remove focus border

    }
}