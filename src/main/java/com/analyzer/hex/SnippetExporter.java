package com.analyzer.hex;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Base64;

/**
 * Exporter utility for converting selected binary sequences into
 * C/C++, Java, Python byte arrays, Base64, raw Hex, or binary dump files.
 */
public class SnippetExporter {

    public static void showExportDialog(Component parent, byte[] fullData, int startOffset, int length) {
        if (fullData == null || fullData.length == 0) {
            JOptionPane.showMessageDialog(parent, "No data available to export.", "Export", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int start = Math.max(0, Math.min(startOffset, fullData.length - 1));
        int len = Math.max(1, Math.min(length, fullData.length - start));
        byte[] slice = new byte[len];
        System.arraycopy(fullData, start, slice, 0, len);

        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(parent), "Export Binary Snippet", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setSize(680, 480);
        dialog.setLocationRelativeTo(parent);

        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(14, 14, 14, 14));

        // Format selector toolbar
        JPanel topRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        topRow.add(new JLabel("Format:"));

        String[] formats = {
                "C / C++ Array",
                "Java byte[] Array",
                "Python bytes (b'...')",
                "Raw Hex Stream (CA FE ...)",
                "Hex with 0x (0xCA, 0xFE, ...)",
                "Base64 Encoded",
                "Raw Binary File (.bin)"
        };
        JComboBox<String> formatCombo = new JComboBox<>(formats);
        topRow.add(formatCombo);

        JLabel rangeLabel = new JLabel(String.format("Offset: 0x%08X | Length: %d bytes", start, len));
        rangeLabel.setForeground(new Color(160, 170, 185));
        topRow.add(Box.createHorizontalStrut(10));
        topRow.add(rangeLabel);

        panel.add(topRow, BorderLayout.NORTH);

        // Preview area
        JTextArea previewArea = new JTextArea();
        previewArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        previewArea.setEditable(false);
        previewArea.setLineWrap(true);
        previewArea.setWrapStyleWord(false);

        JScrollPane scrollPane = new JScrollPane(previewArea);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(60, 65, 75)));
        panel.add(scrollPane, BorderLayout.CENTER);

        // Update preview function
        Runnable updatePreview = () -> {
            int selected = formatCombo.getSelectedIndex();
            switch (selected) {
                case 0: // C/C++
                    previewArea.setText(toCArray(slice));
                    break;
                case 1: // Java
                    previewArea.setText(toJavaArray(slice));
                    break;
                case 2: // Python
                    previewArea.setText(toPythonBytes(slice));
                    break;
                case 3: // Raw Hex
                    previewArea.setText(toRawHex(slice, true));
                    break;
                case 4: // Hex 0x
                    previewArea.setText(to0xHex(slice));
                    break;
                case 5: // Base64
                    previewArea.setText(Base64.getEncoder().encodeToString(slice));
                    break;
                case 6: // Raw Binary
                    previewArea.setText("[Raw binary bytes: " + slice.length + " bytes]\nClick 'Save to File' to write raw binary .bin output.");
                    break;
            }
            previewArea.setCaretPosition(0);
        };

        formatCombo.addActionListener(e -> updatePreview.run());
        updatePreview.run();

        // Footer Buttons
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));

        JButton copyBtn = new JButton("Copy to Clipboard");
        copyBtn.addActionListener(e -> {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(previewArea.getText()), null);
            JOptionPane.showMessageDialog(dialog, "Copied snippet to clipboard!", "Copied", JOptionPane.INFORMATION_MESSAGE);
        });
        footer.add(copyBtn);

        JButton saveBtn = new JButton("Save to File...");
        saveBtn.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            int selected = formatCombo.getSelectedIndex();
            if (selected == 6) {
                fc.setSelectedFile(new File("snippet.bin"));
            } else {
                fc.setSelectedFile(new File("snippet.txt"));
            }

            if (fc.showSaveDialog(dialog) == JFileChooser.APPROVE_OPTION) {
                File out = fc.getSelectedFile();
                try {
                    if (selected == 6) {
                        try (FileOutputStream fos = new FileOutputStream(out)) {
                            fos.write(slice);
                        }
                    } else {
                        try (PrintWriter pw = new PrintWriter(new FileWriter(out))) {
                            pw.print(previewArea.getText());
                        }
                    }
                    JOptionPane.showMessageDialog(dialog, "Saved successfully to " + out.getName(), "Saved", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(dialog, "Failed to save: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        footer.add(saveBtn);

        JButton closeBtn = new JButton("Close");
        closeBtn.addActionListener(e -> dialog.dispose());
        footer.add(closeBtn);

        panel.add(footer, BorderLayout.SOUTH);

        dialog.setContentPane(panel);
        dialog.setVisible(true);
    }

    private static String toCArray(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("// %d bytes\n", bytes.length));
        sb.append(String.format("const unsigned char data[%d] = {\n    ", bytes.length));
        for (int i = 0; i < bytes.length; i++) {
            sb.append(String.format("0x%02X", bytes[i] & 0xFF));
            if (i < bytes.length - 1) sb.append(", ");
            if ((i + 1) % 12 == 0 && i < bytes.length - 1) {
                sb.append("\n    ");
            }
        }
        sb.append("\n};\n");
        return sb.toString();
    }

    private static String toJavaArray(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("// %d bytes\n", bytes.length));
        sb.append("byte[] data = new byte[] {\n    ");
        for (int i = 0; i < bytes.length; i++) {
            sb.append(String.format("(byte) 0x%02X", bytes[i] & 0xFF));
            if (i < bytes.length - 1) sb.append(", ");
            if ((i + 1) % 8 == 0 && i < bytes.length - 1) {
                sb.append("\n    ");
            }
        }
        sb.append("\n};\n");
        return sb.toString();
    }

    private static String toPythonBytes(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        sb.append("data = b\"");
        for (int i = 0; i < bytes.length; i++) {
            sb.append(String.format("\\x%02x", bytes[i] & 0xFF));
            if ((i + 1) % 16 == 0 && i < bytes.length - 1) {
                sb.append("\" \\\n       b\"");
            }
        }
        sb.append("\"\n");
        return sb.toString();
    }

    private static String toRawHex(byte[] bytes, boolean spaced) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            if (i > 0 && spaced) sb.append(" ");
            sb.append(String.format("%02X", bytes[i] & 0xFF));
            if (spaced && (i + 1) % 16 == 0) sb.append("\n");
        }
        return sb.toString();
    }

    private static String to0xHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append(String.format("0x%02X", bytes[i] & 0xFF));
            if ((i + 1) % 12 == 0) sb.append("\n");
        }
        return sb.toString();
    }
}
