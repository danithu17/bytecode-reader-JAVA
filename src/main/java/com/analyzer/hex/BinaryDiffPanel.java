package com.analyzer.hex;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Binary Diff & File Comparison Panel.
 * Compares two binary files byte-by-byte and provides an interactive difference table.
 */
public class BinaryDiffPanel extends JPanel {

    public static class DiffEntry {
        public final int offset;
        public final byte byteA;
        public final byte byteB;
        public final String type;

        public DiffEntry(int offset, byte byteA, byte byteB, String type) {
            this.offset = offset;
            this.byteA = byteA;
            this.byteB = byteB;
            this.type = type;
        }
    }

    private HexDocument documentA;
    private HexDocument documentB;
    private final List<DiffEntry> diffEntries = new ArrayList<>();
    private Consumer<Integer> offsetNavigationHandler;

    private final JLabel fileALabel;
    private final JLabel fileBLabel;
    private final JLabel summaryLabel;
    private final JTable diffTable;
    private final DefaultTableModel tableModel;
    private final JButton nextDiffBtn;
    private final JButton prevDiffBtn;

    public BinaryDiffPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(12, 16, 12, 16));

        // Top Control Header
        JPanel topHeader = new JPanel();
        topHeader.setLayout(new BoxLayout(topHeader, BoxLayout.Y_AXIS));

        JPanel fileSelectionRow = new JPanel(new GridLayout(2, 1, 4, 6));

        JPanel rowA = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 2));
        rowA.add(new JLabel("Base File (A):"));
        fileALabel = new JLabel("(No file loaded)");
        fileALabel.setFont(new Font(Font.MONOSPACED, Font.BOLD, 12));
        fileALabel.setForeground(new Color(130, 185, 255));
        rowA.add(fileALabel);
        fileSelectionRow.add(rowA);

        JPanel rowB = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 2));
        rowB.add(new JLabel("Compare File (B):"));
        fileBLabel = new JLabel("(None selected)");
        fileBLabel.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        rowB.add(fileBLabel);

        JButton selectBBtn = new JButton("Select File B...");
        selectBBtn.addActionListener(e -> chooseFileB());
        rowB.add(selectBBtn);

        fileSelectionRow.add(rowB);
        topHeader.add(fileSelectionRow);

        // Summary Bar
        JPanel summaryBar = new JPanel(new BorderLayout(10, 0));
        summaryBar.setBorder(new EmptyBorder(6, 4, 6, 4));

        summaryLabel = new JLabel("Select a second file to compare differences.");
        summaryLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        summaryLabel.setForeground(new Color(175, 180, 190));
        summaryBar.add(summaryLabel, BorderLayout.CENTER);

        JPanel navBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        prevDiffBtn = new JButton("< Prev Diff");
        prevDiffBtn.setEnabled(false);
        prevDiffBtn.addActionListener(e -> navigateDiff(-1));

        nextDiffBtn = new JButton("Next Diff >");
        nextDiffBtn.setEnabled(false);
        nextDiffBtn.addActionListener(e -> navigateDiff(1));

        navBtns.add(prevDiffBtn);
        navBtns.add(nextDiffBtn);
        summaryBar.add(navBtns, BorderLayout.EAST);

        topHeader.add(summaryBar);
        add(topHeader, BorderLayout.NORTH);

        // Center Differences Table
        String[] columns = {"#", "Offset (Hex)", "Offset (Dec)", "File A Byte", "File B Byte", "Diff Type"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        diffTable = new JTable(tableModel);
        diffTable.setRowHeight(24);
        diffTable.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        diffTable.getTableHeader().setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        diffTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        diffTable.setShowGrid(true);
        diffTable.setGridColor(new Color(55, 60, 68));

        diffTable.getColumnModel().getColumn(0).setPreferredWidth(50);
        diffTable.getColumnModel().getColumn(1).setPreferredWidth(100);
        diffTable.getColumnModel().getColumn(2).setPreferredWidth(90);
        diffTable.getColumnModel().getColumn(3).setPreferredWidth(140);
        diffTable.getColumnModel().getColumn(4).setPreferredWidth(140);
        diffTable.getColumnModel().getColumn(5).setPreferredWidth(100);

        DefaultTableCellRenderer rendererA = new DefaultTableCellRenderer();
        rendererA.setForeground(new Color(255, 140, 140));
        diffTable.getColumnModel().getColumn(3).setCellRenderer(rendererA);

        DefaultTableCellRenderer rendererB = new DefaultTableCellRenderer();
        rendererB.setForeground(new Color(140, 230, 160));
        diffTable.getColumnModel().getColumn(4).setCellRenderer(rendererB);

        diffTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    jumpToSelectedDiff();
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(diffTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(60, 65, 75)));
        add(scrollPane, BorderLayout.CENTER);

        // Footer hint
        JLabel footerHint = new JLabel("Double-click any difference to jump directly to its offset in the Hex Viewer.");
        footerHint.setFont(new Font(Font.SANS_SERIF, Font.ITALIC, 11));
        footerHint.setForeground(new Color(140, 145, 155));
        add(footerHint, BorderLayout.SOUTH);
    }

    public void setOffsetNavigationHandler(Consumer<Integer> handler) {
        this.offsetNavigationHandler = handler;
    }

    public void setDocumentA(HexDocument docA) {
        this.documentA = docA;
        if (docA != null) {
            fileALabel.setText(docA.getFileName() + " (" + docA.getLength() + " bytes)");
        } else {
            fileALabel.setText("(No file loaded)");
        }
        recomputeDiff();
    }

    private void chooseFileB() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Select Secondary Binary File to Compare");
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                this.documentB = HexDocument.loadFromFile(fc.getSelectedFile());
                fileBLabel.setText(documentB.getFileName() + " (" + documentB.getLength() + " bytes)");
                recomputeDiff();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Failed to load File B:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public void recomputeDiff() {
        tableModel.setRowCount(0);
        diffEntries.clear();

        if (documentA == null || documentB == null) {
            summaryLabel.setText("Select a second file to compare differences.");
            nextDiffBtn.setEnabled(false);
            prevDiffBtn.setEnabled(false);
            return;
        }

        byte[] dataA = documentA.getData();
        byte[] dataB = documentB.getData();

        int maxLen = Math.max(dataA.length, dataB.length);
        int minLen = Math.min(dataA.length, dataB.length);
        int matchCount = 0;

        for (int i = 0; i < maxLen; i++) {
            if (i < minLen) {
                if (dataA[i] == dataB[i]) {
                    matchCount++;
                } else {
                    diffEntries.add(new DiffEntry(i, dataA[i], dataB[i], "Modified"));
                }
            } else if (i < dataA.length) {
                diffEntries.add(new DiffEntry(i, dataA[i], (byte) 0, "Missing in B"));
            } else {
                diffEntries.add(new DiffEntry(i, (byte) 0, dataB[i], "Added in B"));
            }
        }

        double similarity = maxLen > 0 ? ((double) matchCount / maxLen) * 100.0 : 100.0;
        DecimalFormat df = new DecimalFormat("#,###");

        summaryLabel.setText(String.format(
                "Differences: %s bytes | Matching: %s bytes | Similarity: %.2f%%",
                df.format(diffEntries.size()), df.format(matchCount), similarity));

        for (int i = 0; i < diffEntries.size(); i++) {
            DiffEntry d = diffEntries.get(i);
            String valA = (d.offset < dataA.length) ? formatByte(d.byteA) : "<EOF>";
            String valB = (d.offset < dataB.length) ? formatByte(d.byteB) : "<EOF>";

            tableModel.addRow(new Object[]{
                    i + 1,
                    String.format("0x%08X", d.offset),
                    d.offset,
                    valA,
                    valB,
                    d.type
            });
        }

        nextDiffBtn.setEnabled(!diffEntries.isEmpty());
        prevDiffBtn.setEnabled(!diffEntries.isEmpty());
    }

    private String formatByte(byte b) {
        int u = b & 0xFF;
        char c = HexDocument.getPrintableChar(b);
        return String.format("0x%02X ('%c')", u, c);
    }

    private void jumpToSelectedDiff() {
        int row = diffTable.getSelectedRow();
        if (row >= 0 && row < diffEntries.size() && offsetNavigationHandler != null) {
            int offset = diffEntries.get(row).offset;
            offsetNavigationHandler.accept(offset);
        }
    }

    private void navigateDiff(int direction) {
        if (diffEntries.isEmpty()) return;
        int cur = diffTable.getSelectedRow();
        int next = cur + direction;
        if (next < 0) next = diffEntries.size() - 1;
        if (next >= diffEntries.size()) next = 0;

        diffTable.setRowSelectionInterval(next, next);
        diffTable.scrollRectToVisible(diffTable.getCellRect(next, 0, true));
        jumpToSelectedDiff();
    }
}
