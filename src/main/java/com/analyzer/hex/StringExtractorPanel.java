package com.analyzer.hex;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Dedicated tab panel for extracting, filtering, and navigating readable strings
 * found inside the binary file.
 */
public class StringExtractorPanel extends JPanel {

    private final JSpinner minLengthSpinner;
    private final JTextField filterField;
    private final JTable stringTable;
    private final DefaultTableModel tableModel;
    private final TableRowSorter<DefaultTableModel> rowSorter;
    private final JLabel countLabel;
    private final JButton exportButton;

    private HexDocument currentDocument;
    private List<HexDocument.ExtractedString> extractedStrings = new ArrayList<>();
    private Consumer<Integer> offsetNavigationHandler;

    public StringExtractorPanel() {
        setLayout(new BorderLayout(8, 8));
        setBorder(new EmptyBorder(12, 12, 12, 12));

        // Control Toolbar
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));

        toolbar.add(new JLabel("Min Length:"));
        minLengthSpinner = new JSpinner(new SpinnerNumberModel(4, 2, 128, 1));
        minLengthSpinner.setPreferredSize(new Dimension(60, 26));
        toolbar.add(minLengthSpinner);

        JButton reextractBtn = new JButton("Rescan Strings");
        reextractBtn.addActionListener(e -> rescan());
        toolbar.add(reextractBtn);

        toolbar.add(Box.createHorizontalStrut(16));
        toolbar.add(new JLabel("Filter:"));
        filterField = new JTextField(18);
        filterField.putClientProperty("JTextField.placeholderText", "Search strings...");
        toolbar.add(filterField);

        exportButton = new JButton("Export to .txt");
        exportButton.addActionListener(e -> exportStrings());
        exportButton.setEnabled(false);
        toolbar.add(exportButton);

        countLabel = new JLabel("No strings extracted");
        countLabel.setForeground(new Color(160, 165, 175));
        toolbar.add(Box.createHorizontalStrut(10));
        toolbar.add(countLabel);

        add(toolbar, BorderLayout.NORTH);

        // Table
        String[] columns = {"#", "Offset (Hex)", "Offset (Dec)", "Length", "Extracted String"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 0 || columnIndex == 2 || columnIndex == 3) {
                    return Integer.class;
                }
                return String.class;
            }
        };

        rowSorter = new TableRowSorter<>(tableModel);
        stringTable = new JTable(tableModel);
        stringTable.setRowSorter(rowSorter);
        stringTable.setRowHeight(24);
        stringTable.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        stringTable.getTableHeader().setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        stringTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        stringTable.setShowGrid(true);
        stringTable.setGridColor(new Color(55, 60, 68));

        stringTable.getColumnModel().getColumn(0).setPreferredWidth(50);
        stringTable.getColumnModel().getColumn(1).setPreferredWidth(100);
        stringTable.getColumnModel().getColumn(2).setPreferredWidth(90);
        stringTable.getColumnModel().getColumn(3).setPreferredWidth(70);
        stringTable.getColumnModel().getColumn(4).setPreferredWidth(500);

        DefaultTableCellRenderer monoRenderer = new DefaultTableCellRenderer();
        monoRenderer.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        stringTable.getColumnModel().getColumn(1).setCellRenderer(monoRenderer);
        stringTable.getColumnModel().getColumn(4).setCellRenderer(monoRenderer);

        // Double-click row to navigate to Hex Viewer offset
        stringTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    jumpToSelectedOffset();
                }
            }
        });

        // Filter text field dynamic listener
        filterField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                applyFilter();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                applyFilter();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                applyFilter();
            }
        });

        JScrollPane scrollPane = new JScrollPane(stringTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(60, 65, 75)));
        add(scrollPane, BorderLayout.CENTER);

        // Footer instructions
        JPanel footer = new JPanel(new BorderLayout());
        JLabel hintLabel = new JLabel("Double-click a string row to jump directly to its offset in the Hex Viewer.");
        hintLabel.setFont(new Font(Font.SANS_SERIF, Font.ITALIC, 11));
        hintLabel.setForeground(new Color(130, 140, 155));
        hintLabel.setBorder(new EmptyBorder(6, 4, 2, 4));

        JButton jumpBtn = new JButton("Go to in Hex Viewer");
        jumpBtn.addActionListener(e -> jumpToSelectedOffset());
        footer.add(hintLabel, BorderLayout.CENTER);
        footer.add(jumpBtn, BorderLayout.EAST);
        add(footer, BorderLayout.SOUTH);
    }

    public void setOffsetNavigationHandler(Consumer<Integer> handler) {
        this.offsetNavigationHandler = handler;
    }

    private void jumpToSelectedOffset() {
        int selectedRow = stringTable.getSelectedRow();
        if (selectedRow >= 0 && offsetNavigationHandler != null) {
            int modelRow = stringTable.convertRowIndexToModel(selectedRow);
            int offset = (int) tableModel.getValueAt(modelRow, 2);
            offsetNavigationHandler.accept(offset);
        }
    }

    public void setDocument(HexDocument doc) {
        this.currentDocument = doc;
        rescan();
    }

    public void rescan() {
        if (currentDocument == null || currentDocument.getLength() == 0) {
            tableModel.setRowCount(0);
            extractedStrings.clear();
            countLabel.setText("No file loaded");
            exportButton.setEnabled(false);
            return;
        }

        int minLen = (Integer) minLengthSpinner.getValue();
        extractedStrings = currentDocument.extractStrings(minLen);

        tableModel.setRowCount(0);
        for (int i = 0; i < extractedStrings.size(); i++) {
            HexDocument.ExtractedString s = extractedStrings.get(i);
            tableModel.addRow(new Object[]{
                    i + 1,
                    s.getOffsetHex(),
                    s.getOffset(),
                    s.getLength(),
                    s.getValue()
            });
        }

        countLabel.setText("Extracted " + extractedStrings.size() + " strings (min len " + minLen + ")");
        exportButton.setEnabled(!extractedStrings.isEmpty());
        applyFilter();
    }

    private void applyFilter() {
        String text = filterField.getText().trim();
        if (text.isEmpty()) {
            rowSorter.setRowFilter(null);
        } else {
            rowSorter.setRowFilter(RowFilter.regexFilter("(?i)" + java.util.regex.Pattern.quote(text), 4));
        }
    }

    private void exportStrings() {
        if (extractedStrings.isEmpty()) return;

        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File("extracted_strings.txt"));
        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File outFile = fc.getSelectedFile();
            try (PrintWriter pw = new PrintWriter(new FileWriter(outFile))) {
                pw.println("# Extracted Strings from " + (currentDocument != null ? currentDocument.getFileName() : ""));
                pw.println("# Offset(Hex)\tOffset(Dec)\tLength\tString");
                for (HexDocument.ExtractedString s : extractedStrings) {
                    pw.println(s.getOffsetHex() + "\t" + s.getOffset() + "\t" + s.getLength() + "\t" + s.getValue());
                }
                JOptionPane.showMessageDialog(this,
                        "Successfully exported " + extractedStrings.size() + " strings to:\n" + outFile.getAbsolutePath(),
                        "Export Successful", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Failed to export strings: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
