package com.analyzer.hex;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.text.DecimalFormat;

/**
 * File Overview tab displaying comprehensive metadata, cryptographic hashes,
 * Shannon entropy, and byte composition statistics.
 */
public class FileMetadataPanel extends JPanel {

    private final JLabel fileNameLabel;
    private final JLabel filePathLabel;
    private final JLabel fileSizeLabel;
    private final JLabel signatureNameLabel;
    private final JLabel signatureCategoryLabel;
    private final JLabel signatureMimeLabel;
    private final JLabel signatureHexLabel;

    private final JTextField md5Field;
    private final JTextField sha1Field;
    private final JTextField sha256Field;

    private final JLabel entropyValueLabel;
    private final JProgressBar entropyBar;
    private final JLabel entropyDescLabel;

    private final JLabel nullBytesLabel;
    private final JLabel printableBytesLabel;
    private final JLabel nonPrintableBytesLabel;

    public FileMetadataPanel() {
        setLayout(new BorderLayout(12, 12));
        setBorder(new EmptyBorder(16, 20, 16, 20));

        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));

        // 1. Basic File Info
        contentPanel.add(createSectionHeader("General File Information"));
        JPanel infoGrid = new JPanel(new GridLayout(3, 2, 8, 6));
        infoGrid.setBorder(new EmptyBorder(6, 12, 14, 12));

        infoGrid.add(createFieldLabel("File Name:"));
        fileNameLabel = createValueLabel("-");
        infoGrid.add(fileNameLabel);

        infoGrid.add(createFieldLabel("File Path:"));
        filePathLabel = createValueLabel("-");
        infoGrid.add(filePathLabel);

        infoGrid.add(createFieldLabel("File Size:"));
        fileSizeLabel = createValueLabel("-");
        infoGrid.add(fileSizeLabel);
        contentPanel.add(infoGrid);

        // 2. Magic Signature Info
        contentPanel.add(createSectionHeader("File Signature & Format"));
        JPanel sigGrid = new JPanel(new GridLayout(4, 2, 8, 6));
        sigGrid.setBorder(new EmptyBorder(6, 12, 14, 12));

        sigGrid.add(createFieldLabel("Identified Format:"));
        signatureNameLabel = createValueLabel("-");
        signatureNameLabel.setFont(signatureNameLabel.getFont().deriveFont(Font.BOLD));
        sigGrid.add(signatureNameLabel);

        sigGrid.add(createFieldLabel("Category:"));
        signatureCategoryLabel = createValueLabel("-");
        sigGrid.add(signatureCategoryLabel);

        sigGrid.add(createFieldLabel("MIME Type:"));
        signatureMimeLabel = createValueLabel("-");
        sigGrid.add(signatureMimeLabel);

        sigGrid.add(createFieldLabel("Magic Bytes:"));
        signatureHexLabel = createValueLabel("-");
        sigGrid.add(signatureHexLabel);
        contentPanel.add(sigGrid);

        // 3. Cryptographic Hashes
        contentPanel.add(createSectionHeader("Cryptographic Checksums"));
        JPanel hashGrid = new JPanel(new GridBagLayout());
        hashGrid.setBorder(new EmptyBorder(6, 12, 14, 12));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(4, 4, 4, 4);

        md5Field = createHashField();
        sha1Field = createHashField();
        sha256Field = createHashField();

        addHashRow(hashGrid, gbc, 0, "MD5:", md5Field);
        addHashRow(hashGrid, gbc, 1, "SHA-1:", sha1Field);
        addHashRow(hashGrid, gbc, 2, "SHA-256:", sha256Field);
        contentPanel.add(hashGrid);

        // 4. Shannon Entropy
        contentPanel.add(createSectionHeader("Entropy & Randomness"));
        JPanel entropyPanel = new JPanel();
        entropyPanel.setLayout(new BoxLayout(entropyPanel, BoxLayout.Y_AXIS));
        entropyPanel.setBorder(new EmptyBorder(6, 12, 14, 12));

        JPanel barRow = new JPanel(new BorderLayout(10, 0));
        entropyValueLabel = new JLabel("0.00 / 8.00 bits");
        entropyValueLabel.setFont(new Font(Font.MONOSPACED, Font.BOLD, 13));

        entropyBar = new JProgressBar(0, 800);
        entropyBar.setPreferredSize(new Dimension(240, 18));
        entropyBar.setStringPainted(true);

        barRow.add(entropyValueLabel, BorderLayout.WEST);
        barRow.add(entropyBar, BorderLayout.CENTER);
        entropyPanel.add(barRow);

        entropyDescLabel = new JLabel("Calculates data distribution and compression/encryption state.");
        entropyDescLabel.setFont(new Font(Font.SANS_SERIF, Font.ITALIC, 11));
        entropyDescLabel.setForeground(new Color(150, 155, 165));
        entropyDescLabel.setBorder(new EmptyBorder(4, 0, 0, 0));
        entropyPanel.add(entropyDescLabel);
        contentPanel.add(entropyPanel);

        // 5. Byte Frequency Stats
        contentPanel.add(createSectionHeader("Byte Composition"));
        JPanel byteGrid = new JPanel(new GridLayout(3, 2, 8, 6));
        byteGrid.setBorder(new EmptyBorder(6, 12, 14, 12));

        byteGrid.add(createFieldLabel("Zero (0x00) Bytes:"));
        nullBytesLabel = createValueLabel("-");
        byteGrid.add(nullBytesLabel);

        byteGrid.add(createFieldLabel("Printable ASCII Bytes:"));
        printableBytesLabel = createValueLabel("-");
        byteGrid.add(printableBytesLabel);

        byteGrid.add(createFieldLabel("Non-Printable Bytes:"));
        nonPrintableBytesLabel = createValueLabel("-");
        byteGrid.add(nonPrintableBytesLabel);
        contentPanel.add(byteGrid);

        JScrollPane scrollPane = new JScrollPane(contentPanel);
        scrollPane.setBorder(null);
        add(scrollPane, BorderLayout.CENTER);
    }

    private JLabel createSectionHeader(String title) {
        JLabel l = new JLabel(title);
        l.setFont(l.getFont().deriveFont(Font.BOLD, 13f));
        l.setForeground(new Color(130, 185, 255));
        l.setBorder(new EmptyBorder(8, 0, 4, 0));
        return l;
    }

    private JLabel createFieldLabel(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(new Color(160, 165, 175));
        return l;
    }

    private JLabel createValueLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        return l;
    }

    private JTextField createHashField() {
        JTextField tf = new JTextField();
        tf.setEditable(false);
        tf.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        return tf;
    }

    private void addHashRow(JPanel panel, GridBagConstraints gbc, int row, String label, JTextField field) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0.0;
        panel.add(new JLabel(label), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        panel.add(field, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0.0;
        JButton copyBtn = new JButton("Copy");
        copyBtn.setMargin(new Insets(2, 6, 2, 6));
        copyBtn.addActionListener(e -> {
            String txt = field.getText();
            if (!txt.isEmpty()) {
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(txt), null);
            }
        });
        panel.add(copyBtn, gbc);
    }

    public void updateMetadata(HexDocument doc, FileSignatureDetector.DetectionResult sig) {
        if (doc == null || doc.getLength() == 0) {
            clear();
            return;
        }

        fileNameLabel.setText(doc.getFileName());
        filePathLabel.setText(doc.getFilePath().isEmpty() ? "(In-Memory Sample)" : doc.getFilePath());

        long sz = doc.getFileSize();
        DecimalFormat df = new DecimalFormat("#,###");
        String sizeFormatted;
        if (sz < 1024) {
            sizeFormatted = sz + " bytes";
        } else if (sz < 1024 * 1024) {
            sizeFormatted = String.format("%.2f KB (%s bytes)", sz / 1024.0, df.format(sz));
        } else {
            sizeFormatted = String.format("%.2f MB (%s bytes)", sz / (1024.0 * 1024.0), df.format(sz));
        }
        fileSizeLabel.setText(sizeFormatted);

        // Signature
        if (sig != null) {
            signatureNameLabel.setText(sig.getName());
            signatureCategoryLabel.setText(sig.getCategory());
            signatureMimeLabel.setText(sig.getMimeType());
            signatureHexLabel.setText(sig.getMatchedHex().isEmpty() ? "None" : sig.getMatchedHex());
        }

        // Asynchronous computation of hashes and entropy so UI remains 100% responsive
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            private HexDocument.HashInfo hashInfo;
            private double entropy;
            private int zeroCount;
            private int printableCount;
            private int otherCount;

            @Override
            protected Void doInBackground() {
                hashInfo = doc.computeHashes();
                entropy = doc.calculateEntropy();

                byte[] data = doc.getData();
                for (byte b : data) {
                    int u = b & 0xFF;
                    if (u == 0) zeroCount++;
                    else if (u >= 32 && u <= 126) printableCount++;
                    else otherCount++;
                }
                return null;
            }

            @Override
            protected void done() {
                if (hashInfo != null) {
                    md5Field.setText(hashInfo.md5);
                    sha1Field.setText(hashInfo.sha1);
                    sha256Field.setText(hashInfo.sha256);
                }

                entropyValueLabel.setText(String.format("%.4f / 8.0000", entropy));
                entropyBar.setValue((int) (entropy * 100));
                entropyBar.setString(String.format("%.2f / 8.00", entropy));

                if (entropy < 3.0) {
                    entropyDescLabel.setText("Low Entropy: Structured, plain text, or highly repetitive data.");
                } else if (entropy < 6.8) {
                    entropyDescLabel.setText("Medium Entropy: Standard binary executable, compiled bytecode, or structured file.");
                } else {
                    entropyDescLabel.setText("High Entropy (~8.0): Likely compressed, encrypted, or packed high-density data.");
                }

                int total = doc.getLength();
                DecimalFormat pctFormat = new DecimalFormat("0.0%");
                nullBytesLabel.setText(df.format(zeroCount) + " (" + pctFormat.format((double) zeroCount / total) + ")");
                printableBytesLabel.setText(df.format(printableCount) + " (" + pctFormat.format((double) printableCount / total) + ")");
                nonPrintableBytesLabel.setText(df.format(otherCount) + " (" + pctFormat.format((double) otherCount / total) + ")");
            }
        };
        worker.execute();
    }

    public void clear() {
        fileNameLabel.setText("-");
        filePathLabel.setText("-");
        fileSizeLabel.setText("-");
        signatureNameLabel.setText("-");
        signatureCategoryLabel.setText("-");
        signatureMimeLabel.setText("-");
        signatureHexLabel.setText("-");
        md5Field.setText("");
        sha1Field.setText("");
        sha256Field.setText("");
        entropyValueLabel.setText("0.00 / 8.00");
        entropyBar.setValue(0);
        nullBytesLabel.setText("-");
        printableBytesLabel.setText("-");
        nonPrintableBytesLabel.setText("-");
    }
}
