package com.analyzer.hex;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

/**
 * Dedicated visual JVM Bytecode and Class Structure Inspector tab.
 * Displays parsed Constant Pool, Fields, Methods, and real disassembled JVM opcodes.
 */
public class JavaClassStructurePanel extends JPanel {

    private final JLabel versionLabel;
    private final JLabel classNameLabel;
    private final JLabel superClassNameLabel;
    private final JLabel accessFlagsLabel;

    private final DefaultListModel<String> methodsListModel;
    private final JList<String> methodsList;
    private final JTextArea bytecodeTextArea;

    private final JTable constantPoolTable;
    private final DefaultTableModel constantPoolModel;

    private final JTable fieldsTable;
    private final DefaultTableModel fieldsModel;

    private JavaClassParser.ParsedClassFile currentParsed;

    public JavaClassStructurePanel() {
        setLayout(new BorderLayout(8, 8));
        setBorder(new EmptyBorder(12, 14, 12, 14));

        // 1. Top Class Overview Header Card
        JPanel headerCard = new JPanel(new GridLayout(2, 2, 10, 6));
        headerCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(60, 65, 78), 1, true),
                new EmptyBorder(10, 12, 10, 12)
        ));
        headerCard.setBackground(new Color(32, 35, 43));

        versionLabel = createHeaderValue("-");
        classNameLabel = createHeaderValue("-");
        superClassNameLabel = createHeaderValue("-");
        accessFlagsLabel = createHeaderValue("-");

        headerCard.add(createLabeledPanel("Class Name:", classNameLabel));
        headerCard.add(createLabeledPanel("Java Version:", versionLabel));
        headerCard.add(createLabeledPanel("Super Class:", superClassNameLabel));
        headerCard.add(createLabeledPanel("Access Flags:", accessFlagsLabel));

        add(headerCard, BorderLayout.NORTH);

        // 2. Center: Tabbed views for Methods/Bytecode, Constant Pool, Fields
        JTabbedPane innerTabs = new JTabbedPane();
        innerTabs.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));

        // Sub-Tab A: Methods & Disassembled Bytecode
        JPanel methodsPanel = new JPanel(new BorderLayout(8, 8));
        methodsPanel.setBorder(new EmptyBorder(8, 8, 8, 8));

        methodsListModel = new DefaultListModel<>();
        methodsList = new JList<>(methodsListModel);
        methodsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        methodsList.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

        JScrollPane methodsScroll = new JScrollPane(methodsList);
        methodsScroll.setPreferredSize(new Dimension(280, 200));
        methodsScroll.setBorder(BorderFactory.createTitledBorder("Declared Methods"));

        bytecodeTextArea = new JTextArea();
        bytecodeTextArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        bytecodeTextArea.setEditable(false);
        bytecodeTextArea.setBackground(new Color(25, 27, 33));
        bytecodeTextArea.setForeground(new Color(140, 225, 175));

        JScrollPane bytecodeScroll = new JScrollPane(bytecodeTextArea);
        bytecodeScroll.setBorder(BorderFactory.createTitledBorder("Disassembled JVM Bytecode Instructions"));

        JSplitPane methodSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, methodsScroll, bytecodeScroll);
        methodSplit.setDividerLocation(300);
        methodsPanel.add(methodSplit, BorderLayout.CENTER);

        methodsList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                showSelectedMethodBytecode();
            }
        });

        innerTabs.addTab("Methods & Bytecode Disassembly", methodsPanel);

        // Sub-Tab B: Constant Pool Table
        String[] cpCols = {"#", "Tag Type", "Resolved Value / References"};
        constantPoolModel = new DefaultTableModel(cpCols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        constantPoolTable = new JTable(constantPoolModel);
        constantPoolTable.setRowHeight(22);
        constantPoolTable.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        constantPoolTable.getTableHeader().setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        constantPoolTable.getColumnModel().getColumn(0).setPreferredWidth(60);
        constantPoolTable.getColumnModel().getColumn(1).setPreferredWidth(120);
        constantPoolTable.getColumnModel().getColumn(2).setPreferredWidth(450);

        JScrollPane cpScroll = new JScrollPane(constantPoolTable);
        innerTabs.addTab("Constant Pool Table", cpScroll);

        // Sub-Tab C: Fields
        String[] fCols = {"Access Flags", "Field Name", "Type Descriptor"};
        fieldsModel = new DefaultTableModel(fCols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        fieldsTable = new JTable(fieldsModel);
        fieldsTable.setRowHeight(22);
        fieldsTable.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        fieldsTable.getTableHeader().setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        JScrollPane fieldsScroll = new JScrollPane(fieldsTable);
        innerTabs.addTab("Class Fields", fieldsScroll);

        add(innerTabs, BorderLayout.CENTER);
    }

    private JPanel createLabeledPanel(String title, JLabel valueLabel) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        p.setOpaque(false);
        JLabel titleLbl = new JLabel(title);
        titleLbl.setForeground(new Color(150, 160, 175));
        p.add(titleLbl);
        p.add(valueLabel);
        return p;
    }

    private JLabel createHeaderValue(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font(Font.MONOSPACED, Font.BOLD, 12));
        l.setForeground(new Color(130, 200, 255));
        return l;
    }

    public void inspectClass(byte[] data) {
        currentParsed = JavaClassParser.parse(data);

        if (!currentParsed.validClass) {
            clear();
            classNameLabel.setText("(Not a valid Java Class file: " + currentParsed.errorMessage + ")");
            return;
        }

        versionLabel.setText(currentParsed.javaVersion);
        classNameLabel.setText(currentParsed.className.isEmpty() ? "(Anonymous/None)" : currentParsed.className);
        superClassNameLabel.setText(currentParsed.superClassName.isEmpty() ? "java/lang/Object" : currentParsed.superClassName);
        accessFlagsLabel.setText(currentParsed.accessFlags.isEmpty() ? "package-private" : currentParsed.accessFlags);

        // Populate Methods
        methodsListModel.clear();
        for (JavaClassParser.MethodInfo m : currentParsed.methods) {
            methodsListModel.addElement((m.accessFlags.isEmpty() ? "" : m.accessFlags + " ") + m.name + m.descriptor);
        }
        if (!currentParsed.methods.isEmpty()) {
            methodsList.setSelectedIndex(0);
        } else {
            bytecodeTextArea.setText("// No methods found in class");
        }

        // Populate Constant Pool
        constantPoolModel.setRowCount(0);
        for (JavaClassParser.ConstantPoolItem item : currentParsed.constantPool) {
            constantPoolModel.addRow(new Object[]{"#" + item.index, item.tag, item.value});
        }

        // Populate Fields
        fieldsModel.setRowCount(0);
        for (JavaClassParser.FieldInfo f : currentParsed.fields) {
            fieldsModel.addRow(new Object[]{f.accessFlags, f.name, f.descriptor});
        }
    }

    private void showSelectedMethodBytecode() {
        int idx = methodsList.getSelectedIndex();
        if (currentParsed != null && idx >= 0 && idx < currentParsed.methods.size()) {
            JavaClassParser.MethodInfo m = currentParsed.methods.get(idx);
            StringBuilder sb = new StringBuilder();
            sb.append("// Method: ").append(m.name).append("\n");
            sb.append("// Signature: ").append(m.descriptor).append("\n");
            sb.append("// Flags: ").append(m.accessFlags).append("\n\n");

            if (m.disassembledInstructions.isEmpty()) {
                sb.append("  [No Code attribute / abstract or native method]\n");
            } else {
                for (String line : m.disassembledInstructions) {
                    sb.append(line).append("\n");
                }
            }
            bytecodeTextArea.setText(sb.toString());
            bytecodeTextArea.setCaretPosition(0);
        }
    }

    public void clear() {
        versionLabel.setText("-");
        classNameLabel.setText("-");
        superClassNameLabel.setText("-");
        accessFlagsLabel.setText("-");
        methodsListModel.clear();
        bytecodeTextArea.setText("");
        constantPoolModel.setRowCount(0);
        fieldsModel.setRowCount(0);
        currentParsed = null;
    }
}
