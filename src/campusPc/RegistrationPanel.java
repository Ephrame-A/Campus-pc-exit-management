package campusPc;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.nio.file.*;
import java.sql.*;

public class RegistrationPanel extends JPanel {
    private JTextField idField, fNameField, lNameField, pcNameField;
    private JComboBox<String> departmentCombo, campusCombo, colorCombo;
    private JButton registerBtn, backBtn;
    private JPanel parentPanel;

    public RegistrationPanel(JPanel parentPanel) {
        this.parentPanel = parentPanel;
        setLayout(new BorderLayout(10, 10)); // Maintain BorderLayout for main panel


        // Form panel with GridLayout
        JPanel formPanel = new JPanel(new GridLayout(7, 2, 10, 10)); // Maintain GridLayout for form fields

        // Add form fields using simplified helper method
        addPlainField(formPanel, "Student ID:", idField = new JTextField());
        addPlainField(formPanel, "First Name:", fNameField = new JTextField());
        addPlainField(formPanel, "Last Name:", lNameField = new JTextField());

        // Department combo
        formPanel.add(new JLabel("Department:")); // Simplified JLabel creation
        String[] departments = {"Computer Science", "Electrical Engineering", "Mechanical Engineering", "Business", "Arts"};
        departmentCombo = new JComboBox<>(departments);
        // Removed: styleComboBox(departmentCombo); // Remove styling method call
        formPanel.add(departmentCombo);

        // Campus combo
        formPanel.add(new JLabel("Campus:")); // Simplified JLabel creation
        String[] campuses = {"Main Campus", "CNCS", "Sefere Selam", "AAiT", "FBE"};
        campusCombo = new JComboBox<>(campuses);
        formPanel.add(campusCombo);

        addPlainField(formPanel, "PC Name:", pcNameField = new JTextField());

        // Color combo
        formPanel.add(new JLabel("Color:")); // Simplified JLabel creation
        String[] colors = {"Silver", "Blue", "Green", "Yellow", "Black", "White"};
        colorCombo = new JComboBox<>(colors);
        formPanel.add(colorCombo);

        // Button panel with FlowLayout
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10)); // Maintain FlowLayout for buttons

        registerBtn = new JButton("Register Student");
        backBtn = new JButton("Back to Menu");

        registerBtn.addActionListener(new RegisterListener());
        backBtn.addActionListener(e -> {
            CardLayout cl = (CardLayout) parentPanel.getLayout();
            cl.show(parentPanel, "MENU");
        });

        buttonPanel.add(registerBtn);
        buttonPanel.add(backBtn);

        // Layout with header
        add(createPlainHeaderPanel(), BorderLayout.NORTH); // Use simplified header creation
        add(formPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

    }

    private void addPlainField(JPanel panel, String labelText, JTextField field) {
        panel.add(new JLabel(labelText)); // Use plain JLabel

        panel.add(field);
    }

    private JPanel createPlainHeaderPanel() {
        JPanel header = new JPanel();

        JLabel title = new JLabel("STUDENT REGISTRATION");

        header.add(title);
        return header;
    }


    private class RegisterListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            String id = idField.getText().trim();
            String fName = fNameField.getText().trim();
            String lName = lNameField.getText().trim();
            String department = (String) departmentCombo.getSelectedItem();
            String campus = (String) campusCombo.getSelectedItem();
            String pcName = pcNameField.getText().trim();
            String color = (String) colorCombo.getSelectedItem();

            if (id.isEmpty() || fName.isEmpty() || lName.isEmpty() || pcName.isEmpty()) {
                showError("Please fill in all fields");
                return;
            }

            try (Connection conn = DBConnection.getConnection()) {
                // Check if student already exists
                if (studentExists(conn, id)) {
                    showError("Student ID already exists!");
                    return;
                }

                // Register new student
                registerStudent(conn, id, fName, lName, department, campus, pcName, color);

                // Generate QR code
                String qrData = generateQRData(id, fName, lName, department, campus, pcName, color);
                String filePath = generateQRCode(qrData, id);

                showSuccess(filePath);
                clearForm();

            } catch (SQLException | WriterException | IOException ex) {
                showError("Error: " + ex.getMessage());
            }
        }

        private boolean studentExists(Connection conn, String id) throws SQLException {
            String checkSql = "SELECT id FROM students_data WHERE id = ?";
            try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                checkStmt.setString(1, id);
                try (ResultSet rs = checkStmt.executeQuery()) {
                    return rs.next();
                }
            }
        }

        private void registerStudent(Connection conn, String id, String fName, String lName,
                                     String department, String campus, String pcName, String color)
                throws SQLException {
            String insertSql = "INSERT INTO students_data (id, f_name, l_name, department, campus, pcname, color) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                insertStmt.setString(1, id);
                insertStmt.setString(2, fName);
                insertStmt.setString(3, lName);
                insertStmt.setString(4, department);
                insertStmt.setString(5, campus);
                insertStmt.setString(6, pcName);
                insertStmt.setString(7, color);
                insertStmt.executeUpdate();
            }
        }

        private String generateQRData(String id, String fName, String lName,
                                      String department, String campus, String pcName, String color) {
            return String.format(
                    "ID: %s\nFirst Name: %s\nLast Name: %s\nDepartment: %s\nCampus: %s\nPC: %s\nColor: %s",
                    id, fName, lName, department, campus, pcName, color
            );
        }

        private String generateQRCode(String text, String id) throws WriterException, IOException {
            // Sanitize the student ID for filename use
            String sanitizedId = id.replaceAll("[^a-zA-Z0-9.-]", "_");

            // Define the base directory
            String baseDir = "C:\\Users\\DELL\\Desktop\\codeforces\\Campus Pc Exit Management\\Qr_Data";

            // Create directory if it doesn't exist
            Path directory = Paths.get(baseDir);
            if (!Files.exists(directory)) {
                Files.createDirectories(directory);
            }

            // Create the file path
            String fileName = sanitizedId + "_qr.png";
            Path filePath = directory.resolve(fileName);

            // Generate the QR code
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, 300, 300);

            // Write to file
            MatrixToImageWriter.writeToPath(bitMatrix, "PNG", filePath);

            return filePath.toString();
        }

        private void showSuccess(String filePath) {
            JOptionPane.showMessageDialog(RegistrationPanel.this,
                    "Registration successful!\nQR code saved as:\n" + filePath,
                    "Success", JOptionPane.INFORMATION_MESSAGE);
        }

        // Simplified showError without HTML
        private void showError(String message) {
            JOptionPane.showMessageDialog(RegistrationPanel.this,
                    "Error:\n" + message,
                    "Error", JOptionPane.ERROR_MESSAGE);
        }

        private void clearForm() {
            idField.setText("");
            fNameField.setText("");
            lNameField.setText("");
            pcNameField.setText("");
            departmentCombo.setSelectedIndex(0);
            campusCombo.setSelectedIndex(0);
            colorCombo.setSelectedIndex(0);
            idField.requestFocus();
        }
    }
}