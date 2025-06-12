package campusPc;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamPanel;
import com.github.sarxos.webcam.WebcamResolution;
import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;

import javax.swing.*;
import java.awt.*;

import java.awt.image.BufferedImage;
import java.sql.*;
import java.util.concurrent.*;

public class ScannerPanel extends JPanel {
    private Webcam webcam; // Represents the physical webcam device.
    private WebcamPanel webcamPanel; // A Swing JPanel to display the live video feed.
    private JTextArea resultArea; // Displays scan results and messages.
    private JButton startBtn, stopBtn, backBtn; // Control buttons.
    private ScheduledExecutorService executor; // Manages a thread for periodic frame scanning.
    private JPanel parentPanel; // Reference to the main panel for CardLayout navigation.
    private boolean isScanning = false; // Flag to indicate if scanning is active.
    private long lastScanTime = 0; // Timestamp of the last successful scan.
    private final long SCAN_COOLDOWN = 3000; // Cooldown period (3 seconds) to prevent rapid re-scans.
    private JLabel statusLabel; // Displays real-time status messages to the user.

    public ScannerPanel(JPanel parentPanel) {
        this.parentPanel = parentPanel;
        setLayout(new BorderLayout(10, 20)); // Set main layout manager for the panel.
        setBackground(new Color(240, 240, 240)); // Set background color.

        // Initialize webcam device and its display panel.
        initializeWebcam();

        // Result area setup
        // Replace your current resultArea setup with:
        resultArea = new JTextArea(5, 20);
        resultArea.setEditable(false);
        resultArea.setFont(new Font("Arial", Font.PLAIN, 14)); // Simplified font.
        resultArea.setLineWrap(true);
        resultArea.setWrapStyleWord(true);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        buttonPanel.setBackground(new Color(240, 240, 240)); // Set background color.

        JScrollPane scrollPane = new JScrollPane(resultArea); // Make resultArea scrollable.
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        // Create buttons with predefined styles.
        startBtn = createStyledButton("Start Scanner", new Color(0, 120, 215));
        stopBtn = createStyledButton("Stop Scanner", new Color(215, 60, 0));
        backBtn = createStyledButton("Back to Menu", new Color(100, 100, 100));

        stopBtn.setEnabled(false); // Initially disable stop button.

        // Add action listeners to buttons.
        startBtn.addActionListener(e -> startScanning());
        stopBtn.addActionListener(e -> stopScanning());
        backBtn.addActionListener(e -> {
            stopScanning(); // Stop scanner before navigating away.
            CardLayout cl = (CardLayout) parentPanel.getLayout();
            cl.show(parentPanel, "MENU"); // Navigate back to the main menu.
        });

        buttonPanel.add(startBtn);
        buttonPanel.add(stopBtn);
        buttonPanel.add(backBtn);

        // Status label setup
        // In your constructor, add this after initializing components
        statusLabel = new JLabel("Scanner ready");
        statusLabel.setFont(new Font("Arial", Font.BOLD, 14)); // Simplified font.
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);

        // Arrange panels and components within the ScannerPanel.
        add(createHeaderPanel(), BorderLayout.NORTH); // Header at the top.
        add(webcamPanel, BorderLayout.CENTER); // Webcam feed in the center.
        add(scrollPane, BorderLayout.SOUTH); // Scan results below webcam.
        add(buttonPanel, BorderLayout.PAGE_END); // Control buttons at the very bottom.
        add(statusLabel, BorderLayout.PAGE_START); // Status label at the top.
    }

    /**
     * Initializes the default webcam and sets up the WebcamPanel for display.
     */
    private void initializeWebcam() {
        webcam = Webcam.getDefault(); // Get the default webcam.
        if (webcam == null) {
            JOptionPane.showMessageDialog(this,
                    "No webcam found!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        webcam.setViewSize(WebcamResolution.VGA.getSize()); // Set webcam resolution to VGA (640x480).
        webcamPanel = new WebcamPanel(webcam); // Create a panel to display the webcam feed.
        webcamPanel.setMirrored(true); // Mirror the image for user's convenience.
    }

    /**
     * Creates a JButton with specified text and background color.
     * @param text The text to display on the button.
     * @param color The background color of the button.
     * @return The styled JButton.
     */
    private JButton createStyledButton(String text, Color color) {
        JButton button = new JButton(text);
        button.setBackground(color); // Set background color.
        button.setForeground(Color.WHITE); // Set text color to white.
        button.setFocusPainted(false); // Remove focus border.
        button.setFont(new Font("Arial", Font.BOLD, 12)); // Simplified font.
        return button;
    }

    /**
     * Creates a header panel with a title for the ScannerPanel.
     * @return The header JPanel.
     */
    private JPanel createHeaderPanel() {
        JPanel header = new JPanel();
        header.setBackground(new Color(0, 120, 215)); // Set background color.

        JLabel title = new JLabel("CAMPUS PC EXIT SCANNER");
        title.setForeground(Color.WHITE); // Set text color to white.
        title.setFont(new Font("Arial", Font.BOLD, 18)); // Simplified font.

        header.add(title);
        return header;
    }

    /**
     * Starts the webcam scanning process.
     * Opens the webcam and schedules periodic frame scanning for QR codes.
     */
    private void startScanning() {
        if (isScanning) return; // Prevent multiple starts.

        if (webcam == null) { // Re-initialize webcam if it somehow became null.
            initializeWebcam();
            if (webcam == null) return; // Exit if initialization fails.
        }

        startBtn.setEnabled(false); // Disable start button.
        stopBtn.setEnabled(true);   // Enable stop button.
        isScanning = true;          // Set scanning flag.

        if (!webcam.isOpen()) {
            webcam.open(); // Open the webcam if not already open.
        }

        executor = Executors.newSingleThreadScheduledExecutor(); // Create a single-threaded executor.
        // Schedule scanFrame method to run every 100 milliseconds.
        executor.scheduleAtFixedRate(this::scanFrame, 0, 100, TimeUnit.MILLISECONDS);
    }

    /**
     * Scans a single frame from the webcam for a QR code.
     * Runs on a separate thread managed by the executor.
     */
    private void scanFrame() {
        if (!isScanning || !webcam.isOpen()) return; // Stop if scanning is halted or webcam is closed.

        BufferedImage image = webcam.getImage(); // Capture current frame.
        if (image == null) return; // Skip if no image is available.

        try {
            String result = scanQRCode(image); // Attempt to decode QR code from the image.
            if (result != null && !result.isEmpty()) {
                long currentTime = System.currentTimeMillis();
                // Apply cooldown to prevent multiple rapid scans of the same QR code.
                if (currentTime - lastScanTime > SCAN_COOLDOWN) {
                    lastScanTime = currentTime;
                    // Process the decoded result on the Event Dispatch Thread (EDT).
                    processScanResult(result);
                }
            }
        } catch (NotFoundException e) {
            // QR code not found in this frame, continue scanning.
        } catch (Exception e) {
            // Log other errors to the result area on the EDT.
            SwingUtilities.invokeLater(() -> {
                resultArea.append("ERROR: " + e.getMessage() + "\n");
            });
        }
    }

    /**
     * Processes the decoded QR code data.
     * Verifies student in database and updates UI. Runs on EDT.
     * @param qrData The string data decoded from the QR code.
     */
    private void processScanResult(String qrData) {
        SwingUtilities.invokeLater(() -> { // Ensure UI updates are on the EDT.
            try {
                String[] lines = qrData.split("\\n"); // Split QR data by newlines.

                // Basic validation of QR data format.
                if (lines.length < 6) {
                    resultArea.append("INVALID QR CODE FORMAT\n");
                    statusLabel.setText("Invalid QR format");
                    statusLabel.setForeground(Color.RED); // Set status text color to red.
                    return;
                }

                String studentId = lines[0].replace("ID: ", "").trim(); // Extract student ID.

                System.out.println("Checking student ID: " + studentId); // Debug output.

                if (verifyStudentInDatabase(studentId)) { // Check if student exists in DB.
                    String studentInfo = formatStudentInfo(qrData); // Format student info for display.
                    resultArea.append("VALID SCAN: " + studentInfo + "\n");
                    statusLabel.setText("Student verified: " + studentInfo);
                    statusLabel.setForeground(new Color(0, 150, 0)); // Set status text color to green.
                    Toolkit.getDefaultToolkit().beep(); // Play a system beep sound.
                } else {
                    // Student not found in database.
                    String notFoundMsg = "Student not found: " + studentId;
                    resultArea.append(notFoundMsg + "\n");
                    statusLabel.setText(notFoundMsg);
                    statusLabel.setForeground(Color.RED); // Set status text color to red.
                    Toolkit.getDefaultToolkit().beep(); // Play a system beep sound.
                    System.out.println(notFoundMsg); // Debug output.
                }
            } catch (SQLException ex) { // Handle database access errors.
                String errorMsg = "DATABASE ERROR: " + ex.getMessage();
                resultArea.append(errorMsg + "\n");
                statusLabel.setText(errorMsg);
                statusLabel.setForeground(Color.RED); // Set status text color to red.
                System.out.println(errorMsg); // Debug output.
            } catch (Exception ex) { // Handle other unexpected processing errors.
                String errorMsg = "PROCESSING ERROR: " + ex.getMessage();
                resultArea.append(errorMsg + "\n");
                statusLabel.setText(errorMsg);
                statusLabel.setForeground(Color.RED); // Set status text color to red.
                System.out.println(errorMsg); // Debug output.
            }
        });
    }

    /**
     * Stops the webcam scanning process and shuts down the executor.
     */
    private void stopScanning() {
        if (!isScanning) return; // Prevent stopping if already stopped.

        isScanning = false; // Set scanning flag to false.

        if (executor != null && !executor.isShutdown()) {
            executor.shutdown(); // Initiate a graceful shutdown of the executor.
            try {
                // Wait for tasks to complete for a short duration, then force shutdown if necessary.
                if (!executor.awaitTermination(800, TimeUnit.MILLISECONDS)) {
                    executor.shutdownNow(); // Force shutdown if tasks don't terminate.
                }
            } catch (InterruptedException e) {
                executor.shutdownNow(); // Force shutdown if interrupted during await.
            }
        }

        startBtn.setEnabled(true); // Re-enable start button.
        stopBtn.setEnabled(false); // Disable stop button.
    }

    /**
     * Decodes a QR code from a given BufferedImage using ZXing library.
     * @param image The BufferedImage to scan for a QR code.
     * @return The decoded string content of the QR code, or null if not found/error.
     * @throws NotFoundException if no QR code is found in the image.
     */
    private String scanQRCode(BufferedImage image) throws NotFoundException {
        LuminanceSource source = new BufferedImageLuminanceSource(image); // Convert image for ZXing.
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source)); // Binarize the image.

        MultiFormatReader reader = new MultiFormatReader(); // ZXing reader for multiple formats.
        try {
            Result result = reader.decode(bitmap); // Attempt to decode.
            return result.getText(); // Return decoded text.
        } catch (NotFoundException e) {
            throw e; // Re-throw NotFoundException.
        } catch (Exception e) {
            return null; // Return null for other decoding errors.
        }
    }

    /**
     * Verifies if a student with the given ID exists in the database.
     * @param studentId The ID of the student to verify.
     * @return true if the student exists, false otherwise.
     * @throws SQLException if a database access error occurs.
     */
    private boolean verifyStudentInDatabase(String studentId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) { // Get database connection.
            String sql = "SELECT * FROM students_data WHERE id = ?"; // SQL query to check student existence.
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, studentId); // Set student ID parameter.
                try (ResultSet rs = stmt.executeQuery()) { // Execute query.
                    return rs.next(); // True if a record is found.
                }
            }
        }
    }

    /**
     * Formats raw QR data into a readable string for display.
**/
    private String formatStudentInfo(String qrData) {
        String[] lines = qrData.split("\n"); // Split data into lines.
        // Format and return a concise student info string.
        return String.format("%s %s (ID: %s, PC: %s, PC Color: %s)",
                lines[1].replace("First Name: ", "").trim(),
                lines[2].replace("Last Name: ", "").trim(),
                lines[0].replace("ID: ", "").trim(),
                lines[5].replace("PC: ", "").trim(),
                lines[6].replace("Color: ", "").trim()
        );
    }
}