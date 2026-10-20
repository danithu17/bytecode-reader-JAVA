package com.analyzer.hex;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Side-panel that inspects selected byte(s) and displays decoded values
 * (8-bit binary, byte/ubyte, short, int, long, float, double in Big/Little Endian).
 */
public class DataInspectorPanel extends JPanel {

    private final JLabel offsetLabel;
    private final JLabel lengthLabel;
    private final JTable inspectorTable;
    private final DefaultTableModel tableModel;
    private final JCheckBox bigEndianToggle;
    private final JLabel copyNotificationLabel;

    private byte[] currentData;
    private int currentOffset = -1;
    private int currentLength = 1;

    public DataInspectorPanel() {
        setLayout(new BorderLayout(8, 8));
        setBorder(new EmptyBorder(12, 12, 12, 12));
        setPreferredSize(new Dimension(340, 500));
        setMinimumSize(new Dimension(280, 300));

        // Header Panel
        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));

        JLabel titleLabel = new JLabel("Data Inspector");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 15f));
        titleLabel.setForeground(new Color(130, 180, 255));

        JPanel metaPanel = new JPanel(new GridLayout(2, 2, 4, 4));
        metaPanel.setBorder(new EmptyBorder(8, 0, 8, 0));

        JLabel offsetTitle = new JLabel("Offset:");
        offsetTitle.setForeground(new Color(170, 175, 185));
        offsetLabel = new JLabel("0x00000000 (0)");
        offsetLabel.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

        JLabel lenTitle = new JLabel("Selection:");
        lenTitle.setForeground(new Color(170, 175, 185));
        lengthLabel = new JLabel("1 byte");
        lengthLabel.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

        metaPanel.add(offsetTitle);
        metaPanel.add(offsetLabel);
        metaPanel.add(lenTitle);
        metaPanel.add(lengthLabel);

        // Endianness Toggle bar
        JPanel controlBar = new JPanel(new BorderLayout());
        bigEndianToggle = new JCheckBox("Show Little-Endian first", false);
        bigEndianToggle.addActionListener(e -> updateDisplay());
        controlBar.add(bigEndianToggle, BorderLayout.WEST);

        headerPanel.add(titleLabel);
        headerPanel.add(Box.createVerticalStrut(4));
        headerPanel.add(metaPanel);
        headerPanel.add(controlBar);
        headerPanel.add(Box.createVerticalStrut(8));

        add(headerPanel, BorderLayout.NORTH);

        // Inspector Table
        String[] columns = {"Type", "Big-Endian", "Little-Endian"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        inspectorTable = new JTable(tableModel);
        inspectorTable.setRowHeight(26);
        inspectorTable.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        inspectorTable.getTableHeader().setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
        inspectorTable.setShowGrid(true);
        inspectorTable.setGridColor(new Color(55, 60, 68));
        inspectorTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.LEFT);
        inspectorTable.getColumnModel().getColumn(0).setPreferredWidth(90);
        inspectorTable.getColumnModel().getColumn(1).setPreferredWidth(120);
        inspectorTable.getColumnModel().getColumn(2).setPreferredWidth(120);

        // Double-click or right-click to copy cell value
        inspectorTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    copySelectedCell();
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(inspectorTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(60, 65, 75)));
        add(scrollPane, BorderLayout.CENTER);

        // Bottom Controls
        JPanel footerPanel = new JPanel(new BorderLayout(6, 6));
        footerPanel.setBorder(new EmptyBorder(8, 0, 0, 0));

        JButton copyBtn = new JButton("Copy Value");
        copyBtn.setToolTipText("Copy selected cell value to clipboard (or double-click row)");
        copyBtn.addActionListener(e -> copySelectedCell());

        copyNotificationLabel = new JLabel("Double-click row to copy value");
        copyNotificationLabel.setFont(new Font(Font.SANS_SERIF, Font.ITALIC, 11));
        copyNotificationLabel.setForeground(new Color(140, 145, 155));

        footerPanel.add(copyNotificationLabel, BorderLayout.CENTER);
        footerPanel.add(copyBtn, BorderLayout.EAST);

        add(footerPanel, BorderLayout.SOUTH);

        clear();
    }

    private void copySelectedCell() {
        int row = inspectorTable.getSelectedRow();
        int col = inspectorTable.getSelectedColumn();
        if (row >= 0 && col >= 0) {
            Object val = tableModel.getValueAt(row, col);
            if (val != null) {
                String str = val.toString();
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(str), null);
                copyNotificationLabel.setText("Copied: " + str);
                Timer t = new Timer(2000, e -> copyNotificationLabel.setText("Double-click row to copy value"));
                t.setRepeats(false);
                t.start();
            }
        }
    }

    public void clear() {
        this.currentData = null;
        this.currentOffset = -1;
        this.currentLength = 0;
        offsetLabel.setText("None");
        lengthLabel.setText("0 bytes");
        tableModel.setRowCount(0);
    }

    public void inspect(byte[] fullData, int offset, int length) {
        this.currentData = fullData;
        this.currentOffset = offset;
        this.currentLength = Math.max(1, length);

        if (fullData == null || offset < 0 || offset >= fullData.length) {
            clear();
            return;
        }

        offsetLabel.setText(String.format("0x%08X (%d)", offset, offset));
        lengthLabel.setText(length + (length == 1 ? " byte" : " bytes"));

        updateDisplay();
    }

    private void updateDisplay() {
        if (currentData == null || currentOffset < 0 || currentOffset >= currentData.length) {
            return;
        }

        tableModel.setRowCount(0);
        int available = currentData.length - currentOffset;

        // 1. Binary (8-bit)
        byte b0 = currentData[currentOffset];
        int ub0 = b0 & 0xFF;
        String binStr = String.format("%8s", Integer.toBinaryString(ub0)).replace(' ', '0');
        String binFormatted = binStr.substring(0, 4) + " " + binStr.substring(4);
        tableModel.addRow(new Object[]{"Binary (8-bit)", binFormatted, binFormatted});

        // 2. ASCII Character
        char c = HexDocument.getPrintableChar(b0);
        String charDisplay = "'" + c + "' (0x" + String.format("%02X", ub0) + ")";
        tableModel.addRow(new Object[]{"Char (ASCII)", charDisplay, charDisplay});

        // 3. Int8 (Signed / Unsigned)
        tableModel.addRow(new Object[]{"Byte (Int8)", String.valueOf(b0), String.valueOf(b0)});
        tableModel.addRow(new Object[]{"Unsigned Byte", String.valueOf(ub0), String.valueOf(ub0)});

        // 4. Short (16-bit)
        if (available >= 2) {
            short sBE = ByteBuffer.wrap(currentData, currentOffset, 2).order(ByteOrder.BIG_ENDIAN).getShort();
            short sLE = ByteBuffer.wrap(currentData, currentOffset, 2).order(ByteOrder.LITTLE_ENDIAN).getShort();
            int usBE = sBE & 0xFFFF;
            int usLE = sLE & 0xFFFF;

            tableModel.addRow(new Object[]{"Short (Int16)", sBE, sLE});
            tableModel.addRow(new Object[]{"Unsigned Int16", usBE, usLE});
        } else {
            tableModel.addRow(new Object[]{"Short (Int16)", "<needs 2 bytes>", "<needs 2 bytes>"});
        }

        // 5. Integer (32-bit)
        if (available >= 4) {
            int iBE = ByteBuffer.wrap(currentData, currentOffset, 4).order(ByteOrder.BIG_ENDIAN).getInt();
            int iLE = ByteBuffer.wrap(currentData, currentOffset, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
            long uiBE = iBE & 0xFFFFFFFFL;
            long uiLE = iLE & 0xFFFFFFFFL;

            tableModel.addRow(new Object[]{"Int (Int32)", iBE, iLE});
            tableModel.addRow(new Object[]{"Unsigned Int32", uiBE, uiLE});
        } else {
            tableModel.addRow(new Object[]{"Int (Int32)", "<needs 4 bytes>", "<needs 4 bytes>"});
        }

        // 6. Long (64-bit)
        if (available >= 8) {
            long lBE = ByteBuffer.wrap(currentData, currentOffset, 8).order(ByteOrder.BIG_ENDIAN).getLong();
            long lLE = ByteBuffer.wrap(currentData, currentOffset, 8).order(ByteOrder.LITTLE_ENDIAN).getLong();
            tableModel.addRow(new Object[]{"Long (Int64)", lBE, lLE});
        } else {
            tableModel.addRow(new Object[]{"Long (Int64)", "<needs 8 bytes>", "<needs 8 bytes>"});
        }

        // 7. Float (32-bit IEEE 754)
        if (available >= 4) {
            float fBE = ByteBuffer.wrap(currentData, currentOffset, 4).order(ByteOrder.BIG_ENDIAN).getFloat();
            float fLE = ByteBuffer.wrap(currentData, currentOffset, 4).order(ByteOrder.LITTLE_ENDIAN).getFloat();
            tableModel.addRow(new Object[]{"Float (32-bit)", formatFloat(fBE), formatFloat(fLE)});
        } else {
            tableModel.addRow(new Object[]{"Float (32-bit)", "<needs 4 bytes>", "<needs 4 bytes>"});
        }

        // 8. Double (64-bit IEEE 754)
        if (available >= 8) {
            double dBE = ByteBuffer.wrap(currentData, currentOffset, 8).order(ByteOrder.BIG_ENDIAN).getDouble();
            double dLE = ByteBuffer.wrap(currentData, currentOffset, 8).order(ByteOrder.LITTLE_ENDIAN).getDouble();
            tableModel.addRow(new Object[]{"Double (64-bit)", formatDouble(dBE), formatDouble(dLE)});
        } else {
            tableModel.addRow(new Object[]{"Double (64-bit)", "<needs 8 bytes>", "<needs 8 bytes>"});
        }

        // 9. Opcode Disassembly Preview
        String opcodeName = getJvmOpcodeName(ub0);
        tableModel.addRow(new Object[]{"JVM Opcode", opcodeName, "0x" + String.format("%02X", ub0)});

        // 10. Range Checksums (if selection > 1 byte)
        if (currentLength > 1) {
            int rangeLen = Math.min(currentLength, currentData.length - currentOffset);
            java.util.zip.CRC32 crc = new java.util.zip.CRC32();
            crc.update(currentData, currentOffset, rangeLen);
            tableModel.addRow(new Object[]{"Selection CRC32", String.format("0x%08X", crc.getValue()), String.valueOf(crc.getValue())});

            java.util.zip.Adler32 adler = new java.util.zip.Adler32();
            adler.update(currentData, currentOffset, rangeLen);
            tableModel.addRow(new Object[]{"Selection Adler32", String.format("0x%08X", adler.getValue()), String.valueOf(adler.getValue())});
        }
    }

    private String getJvmOpcodeName(int op) {
        switch (op) {
            case 0x00: return "nop";
            case 0x01: return "aconst_null";
            case 0x02: return "iconst_m1";
            case 0x03: return "iconst_0";
            case 0x04: return "iconst_1";
            case 0x05: return "iconst_2";
            case 0x06: return "iconst_3";
            case 0x07: return "iconst_4";
            case 0x08: return "iconst_5";
            case 0x09: return "lconst_0";
            case 0x0A: return "lconst_1";
            case 0x0B: return "fconst_0";
            case 0x0C: return "fconst_1";
            case 0x0E: return "dconst_0";
            case 0x0F: return "dconst_1";
            case 0x10: return "bipush <byte>";
            case 0x11: return "sipush <short>";
            case 0x12: return "ldc <index>";
            case 0x13: return "ldc_w <index>";
            case 0x14: return "ldc2_w <index>";
            case 0x15: return "iload <index>";
            case 0x16: return "lload <index>";
            case 0x17: return "fload <index>";
            case 0x18: return "dload <index>";
            case 0x19: return "aload <index>";
            case 0x2A: return "aload_0";
            case 0x2B: return "aload_1";
            case 0x2C: return "aload_2";
            case 0x2D: return "aload_3";
            case 0x36: return "istore <index>";
            case 0x37: return "lstore <index>";
            case 0x38: return "fstore <index>";
            case 0x39: return "dstore <index>";
            case 0x3A: return "astore <index>";
            case 0x4B: return "astore_0";
            case 0x4C: return "astore_1";
            case 0x4D: return "astore_2";
            case 0x4E: return "astore_3";
            case 0x60: return "iadd";
            case 0x64: return "isub";
            case 0x68: return "imul";
            case 0x6C: return "idiv";
            case 0x99: return "ifeq <branch>";
            case 0x9A: return "ifne <branch>";
            case 0x9B: return "iflt <branch>";
            case 0x9C: return "ifge <branch>";
            case 0x9D: return "ifgt <branch>";
            case 0x9E: return "ifle <branch>";
            case 0xA7: return "goto <branch>";
            case 0xAC: return "ireturn";
            case 0xAD: return "lreturn";
            case 0xAE: return "freturn";
            case 0xAF: return "dreturn";
            case 0xB0: return "areturn";
            case 0xB1: return "return";
            case 0xB2: return "getstatic <field>";
            case 0xB3: return "putstatic <field>";
            case 0xB4: return "getfield <field>";
            case 0xB5: return "putfield <field>";
            case 0xB6: return "invokevirtual <method>";
            case 0xB7: return "invokespecial <method>";
            case 0xB8: return "invokestatic <method>";
            case 0xB9: return "invokeinterface <method>";
            case 0xBA: return "invokedynamic <callsite>";
            case 0xBB: return "new <class>";
            case 0xBC: return "newarray <type>";
            case 0xBD: return "anewarray <class>";
            case 0xBE: return "arraylength";
            case 0xBF: return "athrow";
            case 0xC0: return "checkcast <class>";
            case 0xC1: return "instanceof <class>";
            case 0xC2: return "monitorenter";
            case 0xC3: return "monitorexit";
            default: return "byte 0x" + String.format("%02X", op);
        }
    }

    private String formatFloat(float f) {
        if (Float.isNaN(f)) return "NaN";
        if (Float.isInfinite(f)) return f > 0 ? "+Infinity" : "-Infinity";
        return String.format("%.6g", f);
    }

    private String formatDouble(double d) {
        if (Double.isNaN(d)) return "NaN";
        if (Double.isInfinite(d)) return d > 0 ? "+Infinity" : "-Infinity";
        return String.format("%.8g", d);
    }
}
