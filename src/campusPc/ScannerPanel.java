package campusPc;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamPanel;
import com.github.sarxos.webcam.WebcamResolution;
import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.sql.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ScannerPanel extends JPanel {
    private Webcam webcam;
    private WebcamPanel webcamPanel;
    private JTextArea resultArea;
    private JButton startBtn, stopBtn, backBtn;
    private ScheduledExecutorService executor;
    private JPanel parentPanel;
    private boolean isScanning = false;
    private long lastScanTime = 0;
    private final long SCAN_COOLDOWN = 3000; // 3 seconds between scans
    private JLabel statusLabel;
    public ScannerPanel(JPanel parentPanel) {
        this.parentPanel = parentPanel;
        setLayout(new BorderLayout(10, 10));
        setBackground(new Color(240, 240, 240));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Initialize webcam
        initializeWebcam();

        //Result area with better styling// Replace your current resultArea setup with:
        resultArea = new JTextArea(5, 20);
        resultArea.setEditable(false);
        resultArea.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        resultArea.setLineWrap(true);
        resultArea.setWrapStyleWord(true);
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        buttonPanel.setBackground(new Color(240, 240, 240));
        JScrollPane scrollPane = new JScrollPane();
        scrollPane.setBorder(BorderFactory.createTitledBorder("Scan Results"));
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        startBtn = createStyledButton("Start Scanner", new Color(0, 120, 215));
        stopBtn = createStyledButton("Stop Scanner", new Color(215, 60, 0));
        backBtn = createStyledButton("Back to Menu", new Color(100, 100, 100));

        stopBtn.setEnabled(false);

        startBtn.addActionListener(e -> startScanning());
        stopBtn.addActionListener(e -> stopScanning());
        backBtn.addActionListener(e -> {
            stopScanning();
            CardLayout cl = (CardLayout) parentPanel.getLayout();
            cl.show(parentPanel, "MENU");
        });

        buttonPanel.add(startBtn);
        buttonPanel.add(stopBtn);
        buttonPanel.add(backBtn);

        // Layout with header
        add(createHeaderPanel(), BorderLayout.NORTH);
        add(webcamPanel, BorderLayout.CENTER);
        add(scrollPane, BorderLayout.SOUTH);
        add(buttonPanel, BorderLayout.PAGE_END);
        // Add this as a class field


// In your constructor, add this after initializing components
        statusLabel = new JLabel("Scanner ready");
        statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        add(statusLabel, BorderLayout.NORTH); // Or wherever fits your layout
    }

    private void initializeWebcam() {
        webcam = Webcam.getDefault();
        if (webcam == null) {
            JOptionPane.showMessageDialog(this,
                    "No webcam found!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        webcam.setViewSize(WebcamResolution.VGA.getSize());
        webcamPanel = new WebcamPanel(webcam);
        webcamPanel.setFPSDisplayed(true);
        webcamPanel.setDisplayDebugInfo(false);
        webcamPanel.setImageSizeDisplayed(false);
        webcamPanel.setMirrored(true);
        webcamPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0, 120, 215)),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
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

    private JPanel createHeaderPanel() {
        JPanel header = new JPanel();
        header.setBackground(new Color(0, 120, 215));
        header.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel title = new JLabel("CAMPUS PC EXIT SCANNER");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));

        header.add(title);
        return header;
    }

    private void startScanning() {
        if (isScanning) return;

        // Initialize webcam if not already done
        if (webcam == null) {
            initializeWebcam();
            if (webcam == null) return; // initialization failed
        }

        startBtn.setEnabled(false);
        stopBtn.setEnabled(true);
        isScanning = true;

        if (!webcam.isOpen()) {
            webcam.open();
        }

        executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleAtFixedRate(this::scanFrame, 0, 100, TimeUnit.MILLISECONDS);
    }

    private void scanFrame() {
        if (!isScanning || !webcam.isOpen()) return;

        BufferedImage image = webcam.getImage();
        if (image == null) return;

        try {
            String result = scanQRCode(image);
            if (result != null && !result.isEmpty()) {
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastScanTime > SCAN_COOLDOWN) {
                    lastScanTime = currentTime;
                    processScanResult(result);
                }
            }
        } catch (NotFoundException e) {
            // QR code not found, continue scanning
        } catch (Exception e) {
            SwingUtilities.invokeLater(() -> {
                resultArea.append("ERROR: " + e.getMessage() + "\n");
            });
        }
    }

    private void processScanResult(String qrData) {
        SwingUtilities.invokeLater(() -> {
            try {
                String[] lines = qrData.split("\\n");

                if (lines.length < 6) {
                    resultArea.append("INVALID QR CODE FORMAT\n");
                    statusLabel.setText("Invalid QR format");
                    statusLabel.setForeground(Color.RED);
                    return;
                }

                String studentId = lines[0].replace("ID: ", "").trim();

                // Debug output
                System.out.println("Checking student ID: " + studentId);

                if (verifyStudentInDatabase(studentId)) {
                    String studentInfo = formatStudentInfo(qrData);
                    resultArea.append("VALID SCAN: " + studentInfo + "\n");
                    statusLabel.setText("Student verified: " + studentInfo);
                    statusLabel.setForeground(new Color(0, 150, 0)); // Green for success
                    Toolkit.getDefaultToolkit().beep();
                } else {
                    // This block should execute when student not found
                    String notFoundMsg = "Student not found: " + studentId;
                    resultArea.append(notFoundMsg + "\n");
                    statusLabel.setText(notFoundMsg);
                    statusLabel.setForeground(Color.RED);
                    Toolkit.getDefaultToolkit().beep(); // Add beep for not found too
                    System.out.println(notFoundMsg); // Debug output
                }
            } catch (SQLException ex) {
                String errorMsg = "DATABASE ERROR: " + ex.getMessage();
                resultArea.append(errorMsg + "\n");
                statusLabel.setText(errorMsg);
                statusLabel.setForeground(Color.RED);
                System.out.println(errorMsg); // Debug output
            } catch (Exception ex) {
                String errorMsg = "PROCESSING ERROR: " + ex.getMessage();
                resultArea.append(errorMsg + "\n");
                statusLabel.setText(errorMsg);
                statusLabel.setForeground(Color.RED);
                System.out.println(errorMsg); // Debug output
            }
        });
    }

    private void stopScanning() {
        if (!isScanning) return;

        isScanning = false;

        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(800, TimeUnit.MILLISECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
            }
        }

        // Don't close the webcam - just stop scanning
        startBtn.setEnabled(true);
        stopBtn.setEnabled(false);
    }

    private String scanQRCode(BufferedImage image) throws NotFoundException {
        LuminanceSource source = new BufferedImageLuminanceSource(image);
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

        MultiFormatReader reader = new MultiFormatReader();
        try {
            Result result = reader.decode(bitmap);
            return result.getText();
        } catch (NotFoundException e) {
            throw e;
        } catch (Exception e) {
            return null;
        }
    }

    private boolean verifyStudentInDatabase(String studentId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            String sql = "SELECT * FROM students WHERE id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, studentId);
                try (ResultSet rs = stmt.executeQuery()) {
                    return rs.next();
                }
            }
        }
    }

    private String formatStudentInfo(String qrData) {
        String[] lines = qrData.split("\n");
        return String.format("%s %s (ID: %s, PC: %s)",
                lines[1].replace("First Name: ", "").trim(),
                lines[2].replace("Last Name: ", "").trim(),
                lines[0].replace("ID: ", "").trim(),
                lines[5].replace("PC: ", "").trim()
        );
    }


}
