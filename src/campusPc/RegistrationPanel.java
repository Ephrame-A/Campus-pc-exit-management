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
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;

public class RegistrationPanel extends JPanel {
    private JTextField idField, fNameField, lNameField, pcNameField;
    private JComboBox<String> departmentCombo, campusCombo, colorCombo;
    private JButton registerBtn, backBtn;
    private JPanel parentPanel;

    public RegistrationPanel(JPanel parentPanel) {
        this.parentPanel = parentPanel;
        setLayout(new BorderLayout(10, 10));
        setBackground(new Color(240, 240, 240));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Form panel with better styling
        JPanel formPanel = new JPanel(new GridLayout(7, 2, 10, 10));
        formPanel.setBackground(new Color(240, 240, 240));

        // Styled form fields
        addStyledField(formPanel, "Student ID:", idField = new JTextField());
        addStyledField(formPanel, "First Name:", fNameField = new JTextField());
        addStyledField(formPanel, "Last Name:", lNameField = new JTextField());

        // Department combo with styling
        formPanel.add(createStyledLabel("Department:"));
        String[] departments = {"Computer Science", "Electrical Engineering", "Mechanical Engineering", "Business", "Arts"};
        departmentCombo = new JComboBox<>(departments);
        styleComboBox(departmentCombo);
        formPanel.add(departmentCombo);

        // Campus combo with styling
        formPanel.add(createStyledLabel("Campus:"));
        String[] campuses = {"Main Campus", "North Campus", "South Campus", "East Campus", "West Campus"};
        campusCombo = new JComboBox<>(campuses);
        styleComboBox(campusCombo);
        formPanel.add(campusCombo);

        addStyledField(formPanel, "PC Name:", pcNameField = new JTextField());

        // Color combo with styling
        formPanel.add(createStyledLabel("Color:"));
        String[] colors = {"Red", "Blue", "Green", "Yellow", "Black", "White"};
        colorCombo = new JComboBox<>(colors);
        styleComboBox(colorCombo);
        formPanel.add(colorCombo);

        // Button panel with better styling
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        buttonPanel.setBackground(new Color(240, 240, 240));

        registerBtn = createStyledButton("Register Student", new Color(0, 120, 215));
        backBtn = createStyledButton("Back to Menu", new Color(100, 100, 100));

        registerBtn.addActionListener(new RegisterListener());
        backBtn.addActionListener(e -> {
            CardLayout cl = (CardLayout) parentPanel.getLayout();
            cl.show(parentPanel, "MENU");
        });

        buttonPanel.add(registerBtn);
        buttonPanel.add(backBtn);

        // Layout with header
        add(createHeaderPanel(), BorderLayout.NORTH);
        add(formPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        // Add hover effects
        addButtonHoverEffect(registerBtn, new Color(0, 100, 190));
        addButtonHoverEffect(backBtn, new Color(80, 80, 80));
    }

    private void addStyledField(JPanel panel, String label, JTextField field) {
        panel.add(createStyledLabel(label));
        field.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        panel.add(field);
    }

    private JLabel createStyledLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.BOLD, 14));
        return label;
    }

    private JButton createStyledButton(String text, Color color) {
        JButton button = new JButton(text);
        button.setBackground(color);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setFont(new Font("Segoe UI", Font.BOLD, 12));
        button.setBorder(BorderFactory.createEmptyBorder(5, 15, 5, 15));
        return button;
    }

    private void styleComboBox(JComboBox<String> combo) {
        combo.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        combo.setBackground(Color.WHITE);
        combo.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
    }

    private JPanel createHeaderPanel() {
        JPanel header = new JPanel();
        header.setBackground(new Color(0, 120, 215));
        header.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel title = new JLabel("STUDENT REGISTRATION");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));

        header.add(title);
        return header;
    }

    private void addButtonHoverEffect(JButton button, Color hoverColor) {
        Color originalColor = button.getBackground();

        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(hoverColor);
            }

            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(originalColor);
            }
        });
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
            String checkSql = "SELECT id FROM students WHERE id = ?";
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
            String insertSql = "INSERT INTO students (id, f_name, l_name, department, campus, pcname, color) " +
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
                    "<html><b>Registration successful!</b><br>QR code saved as:<br>" + filePath + "</html>",
                    "Success", JOptionPane.INFORMATION_MESSAGE);
        }

        private void showError(String message) {
            JOptionPane.showMessageDialog(RegistrationPanel.this,
                    "<html><b>Error:</b><br>" + message + "</html>",
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
        }}}

