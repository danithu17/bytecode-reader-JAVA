package com.analyzer.hex;

import com.formdev.flatlaf.FlatDarkLaf;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.text.DecimalFormat;
import java.util.List;

/**
 * Modern Desktop Binary & Hex File Analyzer Application
 * Built with Java Swing and FlatLaf Dark theme.
 */
public class HexAnalyzerApp extends JFrame {

    private HexDocument currentDocument;
    private HexViewerComponent hexViewer;
    private DataInspectorPanel dataInspectorPanel;
    private StringExtractorPanel stringExtractorPanel;
    private FileMetadataPanel fileMetadataPanel;
    private EntropyVisualizerPanel entropyVisualizerPanel;
    private BinaryDiffPanel binaryDiffPanel;
    private JavaClassStructurePanel javaClassStructurePanel;

    private JTabbedPane tabbedPane;
    private JLabel signatureBadge;
    private JLabel fileNameLabel;
    private JLabel statusOffsetLabel;
    private JLabel statusSelectionLabel;
    private JLabel statusFileSizeLabel;
    private JLabel statusModeLabel;
    private JLabel statusModifiedLabel;

    // Search bar components
    private JPanel searchBarPanel;
    private JTextField searchInputField;
    private JComboBox<String> searchTypeCombo;
    private JCheckBox matchCaseCheck;
    private JLabel searchStatusLabel;
    private JButton searchPrevBtn;
    private JButton searchNextBtn;

    // Toggle Buttons & Actions
    private JToggleButton hexBinaryToggle;
    private JToggleButton editModeToggle;
    private JButton undoBtn;
    private JButton redoBtn;
    private JButton saveBtn;

    public HexAnalyzerApp() {
        super("Binary & Hex File Analyzer");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1240, 800);
        setMinimumSize(new Dimension(880, 560));
        setLocationRelativeTo(null);

        initUI();
        initDragAndDrop();
        initShortcuts();

        // Load an initial demo sample (Java Class File) so the app opens with rich live data
        loadSampleJavaClass();
    }

    private void initUI() {
        JPanel rootPanel = new JPanel(new BorderLayout());

        // 1. Top Header & Toolbar
        JPanel topContainer = new JPanel();
        topContainer.setLayout(new BoxLayout(topContainer, BoxLayout.Y_AXIS));

        topContainer.add(createMenuBar());
        topContainer.add(createHeaderBar());
        topContainer.add(createSearchBar());

        rootPanel.add(topContainer, BorderLayout.NORTH);

        // 2. Center Content: SplitPane with Tabs on Left and Data Inspector on Right
        hexViewer = new HexViewerComponent();
        hexViewer.addSelectionListener((offset, length) -> {
            updateStatusSelection(offset, length);
            updateModificationStatus();
            if (currentDocument != null && offset >= 0) {
                dataInspectorPanel.inspect(currentDocument.getData(), offset, length);
            } else {
                dataInspectorPanel.clear();
            }
        });

        JScrollPane hexScrollPane = new JScrollPane(hexViewer);
        hexScrollPane.setBorder(null);
        hexScrollPane.getVerticalScrollBar().setUnitIncrement(20);

        stringExtractorPanel = new StringExtractorPanel();
        stringExtractorPanel.setOffsetNavigationHandler(offset -> {
            tabbedPane.setSelectedIndex(0); // Switch to Hex Viewer tab
            hexViewer.setSelection(offset, offset);
            hexViewer.scrollToOffset(offset);
            hexViewer.requestFocusInWindow();
        });

        entropyVisualizerPanel = new EntropyVisualizerPanel();
        entropyVisualizerPanel.setOffsetNavigationHandler(offset -> {
            tabbedPane.setSelectedIndex(0);
            hexViewer.setSelection(offset, offset);
            hexViewer.scrollToOffset(offset);
            hexViewer.requestFocusInWindow();
        });

        binaryDiffPanel = new BinaryDiffPanel();
        binaryDiffPanel.setOffsetNavigationHandler(offset -> {
            tabbedPane.setSelectedIndex(0);
            hexViewer.setSelection(offset, offset);
            hexViewer.scrollToOffset(offset);
            hexViewer.requestFocusInWindow();
        });

        javaClassStructurePanel = new JavaClassStructurePanel();
        fileMetadataPanel = new FileMetadataPanel();

        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        tabbedPane.addTab("Hex / Binary Viewer", hexScrollPane);
        tabbedPane.addTab("Java Bytecode & Class Inspector", javaClassStructurePanel);
        tabbedPane.addTab("Extracted Strings", stringExtractorPanel);
        tabbedPane.addTab("Visual Entropy & Distribution", entropyVisualizerPanel);
        tabbedPane.addTab("Binary Diff & Compare", binaryDiffPanel);
        tabbedPane.addTab("File Overview & Hashes", fileMetadataPanel);

        tabbedPane.addChangeListener(e -> {
            int idx = tabbedPane.getSelectedIndex();
            if (idx == 2) {
                entropyVisualizerPanel.setDocument(currentDocument);
            } else if (idx == 3) {
                binaryDiffPanel.setDocumentA(currentDocument);
            }
        });

        dataInspectorPanel = new DataInspectorPanel();

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, tabbedPane, dataInspectorPanel);
        splitPane.setResizeWeight(0.75);
        splitPane.setDividerSize(6);
        splitPane.setBorder(null);

        rootPanel.add(splitPane, BorderLayout.CENTER);

        // 3. Bottom Status Bar
        rootPanel.add(createStatusBar(), BorderLayout.SOUTH);

        setContentPane(rootPanel);
    }

    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        // File Menu
        JMenu fileMenu = new JMenu("File");
        fileMenu.setMnemonic(KeyEvent.VK_F);

        JMenuItem openItem = new JMenuItem("Open File...", KeyEvent.VK_O);
        openItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        openItem.addActionListener(e -> chooseAndOpenFile());
        fileMenu.add(openItem);

        JMenuItem saveItem = new JMenuItem("Save File", KeyEvent.VK_S);
        saveItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        saveItem.addActionListener(e -> saveCurrentFile(false));
        fileMenu.add(saveItem);

        JMenuItem saveAsItem = new JMenuItem("Save File As...");
        saveAsItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx() | KeyEvent.SHIFT_DOWN_MASK));
        saveAsItem.addActionListener(e -> saveCurrentFile(true));
        fileMenu.add(saveAsItem);

        fileMenu.addSeparator();

        JMenuItem exportSnippetItem = new JMenuItem("Export Snippet / Array...");
        exportSnippetItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        exportSnippetItem.addActionListener(e -> exportSelectedSnippet());
        fileMenu.add(exportSnippetItem);

        fileMenu.addSeparator();

        // Sample Files Submenu
        JMenu samplesMenu = new JMenu("Load Sample File");
        JMenuItem sampleClass = new JMenuItem("Java Class File (.class - CAFEBABE)");
        sampleClass.addActionListener(e -> loadSampleJavaClass());
        samplesMenu.add(sampleClass);

        JMenuItem sampleSer = new JMenuItem("Java Serialized Object (.ser - ACED0005)");
        sampleSer.addActionListener(e -> loadSampleSerializedObject());
        samplesMenu.add(sampleSer);

        JMenuItem samplePng = new JMenuItem("PNG Image (.png - 89504E47)");
        samplePng.addActionListener(e -> loadSamplePng());
        samplesMenu.add(samplePng);

        JMenuItem sampleElf = new JMenuItem("ELF Linux Binary (7F454C46)");
        sampleElf.addActionListener(e -> loadSampleElf());
        samplesMenu.add(sampleElf);

        fileMenu.add(samplesMenu);
        fileMenu.addSeparator();

        JMenuItem exitItem = new JMenuItem("Exit", KeyEvent.VK_X);
        exitItem.addActionListener(e -> System.exit(0));
        fileMenu.add(exitItem);

        menuBar.add(fileMenu);

        // Edit Menu
        JMenu editMenu = new JMenu("Edit");
        JMenuItem undoItem = new JMenuItem("Undo", KeyEvent.VK_U);
        undoItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        undoItem.addActionListener(e -> {
            hexViewer.undo();
            updateModificationStatus();
        });
        editMenu.add(undoItem);

        JMenuItem redoItem = new JMenuItem("Redo", KeyEvent.VK_R);
        redoItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Y, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        redoItem.addActionListener(e -> {
            hexViewer.redo();
            updateModificationStatus();
        });
        editMenu.add(redoItem);

        editMenu.addSeparator();

        JCheckBoxMenuItem editModeItem = new JCheckBoxMenuItem("Enable In-Place Editing", true);
        editModeItem.addActionListener(e -> {
            boolean edit = editModeItem.isSelected();
            hexViewer.setEditable(edit);
            editModeToggle.setSelected(edit);
            editModeToggle.setText(edit ? "Edit: ON" : "Edit: OFF");
        });
        editMenu.add(editModeItem);

        menuBar.add(editMenu);

        // View Menu
        JMenu viewMenu = new JMenu("View");
        JMenuItem toggleModeItem = new JMenuItem("Toggle Hex / Binary View");
        toggleModeItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_B, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        toggleModeItem.addActionListener(e -> hexBinaryToggle.doClick());
        viewMenu.add(toggleModeItem);

        JMenuItem gotoItem = new JMenuItem("Go to Offset...");
        gotoItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_G, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        gotoItem.addActionListener(e -> promptGoToOffset());
        viewMenu.add(gotoItem);

        menuBar.add(viewMenu);

        // Search Menu
        JMenu searchMenu = new JMenu("Search");
        JMenuItem findItem = new JMenuItem("Find in File...");
        findItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        findItem.addActionListener(e -> toggleSearchBar());
        searchMenu.add(findItem);

        JMenuItem findNextItem = new JMenuItem("Find Next");
        findNextItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0));
        findNextItem.addActionListener(e -> hexViewer.nextMatch());
        searchMenu.add(findNextItem);

        JMenuItem findPrevItem = new JMenuItem("Find Previous");
        findPrevItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F3, KeyEvent.SHIFT_DOWN_MASK));
        findPrevItem.addActionListener(e -> hexViewer.prevMatch());
        searchMenu.add(findPrevItem);

        menuBar.add(searchMenu);

        return menuBar;
    }

    private JPanel createHeaderBar() {
        JPanel header = new JPanel(new BorderLayout(12, 0));
        header.setBorder(new EmptyBorder(8, 14, 8, 14));
        header.setBackground(new Color(36, 39, 46));

        // Left: Open button, File info
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        leftPanel.setOpaque(false);

        JButton openBtn = new JButton("Open File");
        openBtn.putClientProperty("JButton.buttonType", "roundRect");
        openBtn.addActionListener(e -> chooseAndOpenFile());
        leftPanel.add(openBtn);

        // Samples Dropdown
        JButton samplesBtn = new JButton("Load Sample");
        samplesBtn.putClientProperty("JButton.buttonType", "roundRect");
        JPopupMenu samplesPopup = new JPopupMenu();

        JMenuItem m1 = new JMenuItem("Java Class File (.class)");
        m1.addActionListener(e -> loadSampleJavaClass());
        samplesPopup.add(m1);

        JMenuItem m2 = new JMenuItem("Java Serialized Object (.ser)");
        m2.addActionListener(e -> loadSampleSerializedObject());
        samplesPopup.add(m2);

        JMenuItem m3 = new JMenuItem("PNG Image (.png)");
        m3.addActionListener(e -> loadSamplePng());
        samplesPopup.add(m3);

        JMenuItem m4 = new JMenuItem("ELF Linux Executable");
        m4.addActionListener(e -> loadSampleElf());
        samplesPopup.add(m4);

        samplesBtn.addActionListener(e -> samplesPopup.show(samplesBtn, 0, samplesBtn.getHeight()));
        leftPanel.add(samplesBtn);

        fileNameLabel = new JLabel("No file loaded");
        fileNameLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        fileNameLabel.setForeground(new Color(230, 235, 245));
        leftPanel.add(Box.createHorizontalStrut(8));
        leftPanel.add(fileNameLabel);

        header.add(leftPanel, BorderLayout.WEST);

        // Center / Right: Signature Badge, Edit controls, Mode Toggles
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rightPanel.setOpaque(false);

        // Undo & Redo buttons
        undoBtn = new JButton("Undo");
        undoBtn.putClientProperty("JButton.buttonType", "roundRect");
        undoBtn.setToolTipText("Undo byte edit (Ctrl+Z)");
        undoBtn.addActionListener(e -> {
            hexViewer.undo();
            updateModificationStatus();
        });
        rightPanel.add(undoBtn);

        redoBtn = new JButton("Redo");
        redoBtn.putClientProperty("JButton.buttonType", "roundRect");
        redoBtn.setToolTipText("Redo byte edit (Ctrl+Y)");
        redoBtn.addActionListener(e -> {
            hexViewer.redo();
            updateModificationStatus();
        });
        rightPanel.add(redoBtn);

        saveBtn = new JButton("Save");
        saveBtn.putClientProperty("JButton.buttonType", "roundRect");
        saveBtn.setToolTipText("Save modified binary file (Ctrl+S)");
        saveBtn.addActionListener(e -> saveCurrentFile(false));
        rightPanel.add(saveBtn);

        JButton exportBtn = new JButton("Export Snippet");
        exportBtn.putClientProperty("JButton.buttonType", "roundRect");
        exportBtn.setToolTipText("Export selected bytes as C, Java, Python array or Base64");
        exportBtn.addActionListener(e -> exportSelectedSnippet());
        rightPanel.add(exportBtn);

        editModeToggle = new JToggleButton("Edit: ON", true);
        editModeToggle.putClientProperty("JButton.buttonType", "roundRect");
        editModeToggle.setToolTipText("Toggle In-Place Editing vs Read-Only");
        editModeToggle.addActionListener(e -> {
            boolean edit = editModeToggle.isSelected();
            editModeToggle.setText(edit ? "Edit: ON" : "Edit: OFF");
            hexViewer.setEditable(edit);
        });
        rightPanel.add(editModeToggle);

        signatureBadge = new JLabel("Unknown Binary");
        signatureBadge.setOpaque(true);
        signatureBadge.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        signatureBadge.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(70, 80, 95), 1, true),
                new EmptyBorder(4, 10, 4, 10)
        ));
        signatureBadge.setBackground(new Color(45, 52, 64));
        signatureBadge.setForeground(new Color(180, 190, 205));
        rightPanel.add(signatureBadge);

        // Hex / Binary toggle switch
        hexBinaryToggle = new JToggleButton("Hex Mode");
        hexBinaryToggle.putClientProperty("JButton.buttonType", "roundRect");
        hexBinaryToggle.addActionListener(e -> {
            boolean isBin = hexBinaryToggle.isSelected();
            hexBinaryToggle.setText(isBin ? "Binary (0101) Mode" : "Hex Mode");
            hexViewer.setBinaryMode(isBin);
            statusModeLabel.setText(isBin ? "Mode: BINARY (8 B/row)" : "Mode: HEX (16 B/row)");
        });
        rightPanel.add(hexBinaryToggle);

        JButton gotoBtn = new JButton("Go to Offset");
        gotoBtn.putClientProperty("JButton.buttonType", "roundRect");
        gotoBtn.addActionListener(e -> promptGoToOffset());
        rightPanel.add(gotoBtn);

        JButton searchToggleBtn = new JButton("Search");
        searchToggleBtn.putClientProperty("JButton.buttonType", "roundRect");
        searchToggleBtn.addActionListener(e -> toggleSearchBar());
        rightPanel.add(searchToggleBtn);

        header.add(rightPanel, BorderLayout.EAST);

        return header;
    }

    private JPanel createSearchBar() {
        searchBarPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        searchBarPanel.setBackground(new Color(42, 45, 54));
        searchBarPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 1, 0, new Color(55, 60, 72)));
        searchBarPanel.setVisible(false);

        searchBarPanel.add(new JLabel("Search:"));

        searchTypeCombo = new JComboBox<>(new String[]{"Text (ASCII / UTF-8)", "Hex Sequence (e.g. CA FE)"});
        searchTypeCombo.setPreferredSize(new Dimension(180, 26));
        searchBarPanel.add(searchTypeCombo);

        searchInputField = new JTextField(20);
        searchInputField.setPreferredSize(new Dimension(220, 26));
        searchInputField.putClientProperty("JTextField.placeholderText", "Enter pattern to find...");
        searchInputField.addActionListener(e -> executeSearch());
        searchBarPanel.add(searchInputField);

        matchCaseCheck = new JCheckBox("Match Case", false);
        matchCaseCheck.setOpaque(false);
        searchBarPanel.add(matchCaseCheck);

        JButton searchBtn = new JButton("Find All");
        searchBtn.addActionListener(e -> executeSearch());
        searchBarPanel.add(searchBtn);

        searchPrevBtn = new JButton("< Prev");
        searchPrevBtn.setEnabled(false);
        searchPrevBtn.addActionListener(e -> {
            hexViewer.prevMatch();
            updateSearchStatusLabel();
        });
        searchBarPanel.add(searchPrevBtn);

        searchNextBtn = new JButton("Next >");
        searchNextBtn.setEnabled(false);
        searchNextBtn.addActionListener(e -> {
            hexViewer.nextMatch();
            updateSearchStatusLabel();
        });
        searchBarPanel.add(searchNextBtn);

        searchStatusLabel = new JLabel("");
        searchStatusLabel.setForeground(new Color(170, 175, 185));
        searchBarPanel.add(searchStatusLabel);

        JButton closeSearchBtn = new JButton("X");
        closeSearchBtn.setMargin(new Insets(2, 6, 2, 6));
        closeSearchBtn.addActionListener(e -> searchBarPanel.setVisible(false));
        searchBarPanel.add(closeSearchBtn);

        return searchBarPanel;
    }

    private JPanel createStatusBar() {
        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBorder(new EmptyBorder(4, 12, 4, 12));
        statusBar.setBackground(new Color(26, 28, 33));

        JPanel leftStatus = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 0));
        leftStatus.setOpaque(false);

        statusOffsetLabel = new JLabel("Offset: -");
        statusOffsetLabel.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        statusOffsetLabel.setForeground(new Color(175, 180, 195));

        statusSelectionLabel = new JLabel("Selection: 0 bytes");
        statusSelectionLabel.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        statusSelectionLabel.setForeground(new Color(175, 180, 195));

        statusModifiedLabel = new JLabel("Modified: 0 bytes");
        statusModifiedLabel.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        statusModifiedLabel.setForeground(new Color(160, 165, 175));

        leftStatus.add(statusOffsetLabel);
        leftStatus.add(new JSeparator(SwingConstants.VERTICAL));
        leftStatus.add(statusSelectionLabel);
        leftStatus.add(new JSeparator(SwingConstants.VERTICAL));
        leftStatus.add(statusModifiedLabel);

        statusBar.add(leftStatus, BorderLayout.WEST);

        JPanel rightStatus = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 0));
        rightStatus.setOpaque(false);

        statusFileSizeLabel = new JLabel("Size: 0 bytes");
        statusFileSizeLabel.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        statusFileSizeLabel.setForeground(new Color(175, 180, 195));

        statusModeLabel = new JLabel("Mode: HEX (16 B/row)");
        statusModeLabel.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        statusModeLabel.setForeground(new Color(130, 180, 255));

        rightStatus.add(statusFileSizeLabel);
        rightStatus.add(new JSeparator(SwingConstants.VERTICAL));
        rightStatus.add(statusModeLabel);

        statusBar.add(rightStatus, BorderLayout.EAST);

        return statusBar;
    }

    private void initDragAndDrop() {
        setTransferHandler(new TransferHandler() {
            @Override
            public boolean canImport(TransferSupport support) {
                return support.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
            }

            @Override
            public boolean importData(TransferSupport support) {
                if (!canImport(support)) return false;
                try {
                    Transferable t = support.getTransferable();
                    @SuppressWarnings("unchecked")
                    List<File> files = (List<File>) t.getTransferData(DataFlavor.javaFileListFlavor);
                    if (files != null && !files.isEmpty()) {
                        loadFile(files.get(0));
                        return true;
                    }
                } catch (Exception ex) {
                    showError("Failed to open dropped file: " + ex.getMessage());
                }
                return false;
            }
        });
    }

    private void initShortcuts() {
        JRootPane root = getRootPane();
        InputMap im = root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = root.getActionMap();

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_F, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()), "toggleSearch");
        am.put("toggleSearch", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                toggleSearchBar();
            }
        });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_G, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()), "gotoOffset");
        am.put("gotoOffset", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                promptGoToOffset();
            }
        });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_S, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()), "saveFile");
        am.put("saveFile", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveCurrentFile(false);
            }
        });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_E, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()), "exportSnippet");
        am.put("exportSnippet", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                exportSelectedSnippet();
            }
        });
    }

    public void updateModificationStatus() {
        if (currentDocument == null) return;
        boolean mod = currentDocument.isModified();
        int modCount = currentDocument.getModifiedCount();

        if (mod) {
            setTitle("Binary Analyzer - " + currentDocument.getFileName() + " * (Modified)");
            fileNameLabel.setText(currentDocument.getFileName() + " *");
            statusModifiedLabel.setText("Modified: " + modCount + (modCount == 1 ? " byte" : " bytes"));
            statusModifiedLabel.setForeground(new Color(255, 167, 38));
        } else {
            setTitle("Binary Analyzer - " + currentDocument.getFileName());
            fileNameLabel.setText(currentDocument.getFileName());
            statusModifiedLabel.setText("Modified: 0 bytes");
            statusModifiedLabel.setForeground(new Color(160, 165, 175));
        }

        undoBtn.setEnabled(currentDocument.canUndo());
        redoBtn.setEnabled(currentDocument.canRedo());
    }

    public void saveCurrentFile(boolean saveAs) {
        if (currentDocument == null || currentDocument.getLength() == 0) return;

        File targetFile = null;
        if (!saveAs && currentDocument.getFilePath() != null && !currentDocument.getFilePath().isEmpty()) {
            targetFile = new File(currentDocument.getFilePath());
        }

        if (targetFile == null || saveAs) {
            JFileChooser chooser = new JFileChooser();
            chooser.setSelectedFile(new File(currentDocument.getFileName()));
            if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                targetFile = chooser.getSelectedFile();
            } else {
                return;
            }
        }

        try {
            currentDocument.saveToFile(targetFile);
            updateModificationStatus();
            JOptionPane.showMessageDialog(this, "File successfully saved to:\n" + targetFile.getAbsolutePath(), "Save Successful", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            showError("Failed to save file: " + ex.getMessage());
        }
    }

    public void exportSelectedSnippet() {
        if (currentDocument == null || currentDocument.getLength() == 0) return;
        int start = hexViewer.getSelectionStart();
        int end = hexViewer.getSelectionEnd();
        if (start < 0) {
            start = 0;
            end = Math.min(64, currentDocument.getLength() - 1);
        }
        int len = end - start + 1;
        SnippetExporter.showExportDialog(this, currentDocument.getData(), start, len);
    }

    public void chooseAndOpenFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Open Binary or Hex File");
        int res = chooser.showOpenDialog(this);
        if (res == JFileChooser.APPROVE_OPTION) {
            loadFile(chooser.getSelectedFile());
        }
    }

    public void loadFile(File file) {
        if (file == null || !file.exists()) {
            showError("File does not exist: " + (file != null ? file.getPath() : ""));
            return;
        }

        try {
            HexDocument doc = HexDocument.loadFromFile(file);
            setDocument(doc);
        } catch (Exception ex) {
            showError("Could not load file:\n" + ex.getMessage());
        }
    }

    public void setDocument(HexDocument doc) {
        this.currentDocument = doc;
        hexViewer.setDocument(doc);
        stringExtractorPanel.setDocument(doc);

        // Detect signature
        FileSignatureDetector.DetectionResult sig = FileSignatureDetector.detect(doc.getData());
        updateSignatureBadge(sig);

        // Update file info & metadata
        fileNameLabel.setText(doc.getFileName());
        setTitle("Binary Analyzer - " + doc.getFileName());

        DecimalFormat df = new DecimalFormat("#,###");
        statusFileSizeLabel.setText("Size: " + df.format(doc.getFileSize()) + " bytes");

        fileMetadataPanel.updateMetadata(doc, sig);
        javaClassStructurePanel.inspectClass(doc.getData());
        entropyVisualizerPanel.setDocument(doc);
        binaryDiffPanel.setDocumentA(doc);
        updateModificationStatus();

        // Reset search
        searchInputField.setText("");
        hexViewer.setSearchMatches(null, 1);
        searchStatusLabel.setText("");
        searchPrevBtn.setEnabled(false);
        searchNextBtn.setEnabled(false);
    }

    private void updateSignatureBadge(FileSignatureDetector.DetectionResult sig) {
        signatureBadge.setText(sig.getName());
        if (sig.isDetected()) {
            if (sig.getCategory().contains("Java")) {
                signatureBadge.setBackground(new Color(25, 75, 55));
                signatureBadge.setForeground(new Color(110, 235, 160));
                signatureBadge.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(50, 160, 100), 1, true),
                        new EmptyBorder(4, 10, 4, 10)
                ));
            } else if (sig.getCategory().contains("Executable")) {
                signatureBadge.setBackground(new Color(75, 45, 25));
                signatureBadge.setForeground(new Color(255, 180, 100));
                signatureBadge.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(180, 100, 40), 1, true),
                        new EmptyBorder(4, 10, 4, 10)
                ));
            } else {
                signatureBadge.setBackground(new Color(30, 60, 85));
                signatureBadge.setForeground(new Color(120, 205, 255));
                signatureBadge.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(60, 130, 200), 1, true),
                        new EmptyBorder(4, 10, 4, 10)
                ));
            }
        } else {
            signatureBadge.setBackground(new Color(45, 50, 60));
            signatureBadge.setForeground(new Color(160, 165, 175));
            signatureBadge.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(70, 75, 85), 1, true),
                    new EmptyBorder(4, 10, 4, 10)
            ));
        }
    }

    private void updateStatusSelection(int offset, int length) {
        if (offset < 0) {
            statusOffsetLabel.setText("Offset: -");
            statusSelectionLabel.setText("Selection: 0 bytes");
        } else {
            statusOffsetLabel.setText(String.format("Offset: 0x%08X (%d)", offset, offset));
            statusSelectionLabel.setText("Selection: " + length + (length == 1 ? " byte" : " bytes"));
        }
    }

    private void toggleSearchBar() {
        boolean visible = !searchBarPanel.isVisible();
        searchBarPanel.setVisible(visible);
        if (visible) {
            searchInputField.requestFocusInWindow();
            searchInputField.selectAll();
        }
    }

    private void executeSearch() {
        if (currentDocument == null || currentDocument.getLength() == 0) return;

        String query = searchInputField.getText().trim();
        if (query.isEmpty()) {
            hexViewer.setSearchMatches(null, 1);
            searchStatusLabel.setText("");
            searchPrevBtn.setEnabled(false);
            searchNextBtn.setEnabled(false);
            return;
        }

        int type = searchTypeCombo.getSelectedIndex();
        List<Integer> matches;
        int matchLen;

        if (type == 0) {
            // Text search
            boolean matchCase = matchCaseCheck.isSelected();
            matches = currentDocument.searchText(query, matchCase);
            matchLen = query.getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
        } else {
            // Hex sequence search
            matches = currentDocument.searchHex(query);
            String clean = query.replaceAll("[^0-9a-fA-F]", "");
            matchLen = Math.max(1, clean.length() / 2);
        }

        hexViewer.setSearchMatches(matches, matchLen);
        searchPrevBtn.setEnabled(!matches.isEmpty());
        searchNextBtn.setEnabled(!matches.isEmpty());

        updateSearchStatusLabel();
    }

    private void updateSearchStatusLabel() {
        int count = hexViewer.getSearchMatchCount();
        if (count == 0) {
            searchStatusLabel.setText("No matches found");
            searchStatusLabel.setForeground(new Color(255, 120, 120));
        } else {
            int current = hexViewer.getCurrentSearchMatchIndex() + 1;
            searchStatusLabel.setText(String.format("Match %d of %d", current, count));
            searchStatusLabel.setForeground(new Color(130, 230, 160));
        }
    }

    private void promptGoToOffset() {
        if (currentDocument == null || currentDocument.getLength() == 0) return;

        String input = JOptionPane.showInputDialog(this,
                "Enter byte offset (decimal or hex with 0x prefix):\nExample: 42 or 0x2A",
                "Go to Offset",
                JOptionPane.QUESTION_MESSAGE);

        if (input == null || input.trim().isEmpty()) return;

        try {
            input = input.trim();
            int offset;
            if (input.toLowerCase().startsWith("0x")) {
                offset = Integer.parseInt(input.substring(2), 16);
            } else {
                offset = Integer.parseInt(input);
            }

            if (offset < 0 || offset >= currentDocument.getLength()) {
                showError("Offset out of range! File range is 0 to " + (currentDocument.getLength() - 1));
                return;
            }

            hexViewer.setSelection(offset, offset);
            hexViewer.scrollToOffset(offset);
            hexViewer.requestFocusInWindow();
        } catch (NumberFormatException ex) {
            showError("Invalid offset format. Use standard decimal or hex (e.g. 0x1A).");
        }
    }

    // Sample loaders
    public void loadSampleJavaClass() {
        byte[] bytes = SampleGenerator.createJavaClassSample();
        HexDocument doc = new HexDocument(bytes, "SampleBytecode.class", "");
        setDocument(doc);
    }

    public void loadSampleSerializedObject() {
        byte[] bytes = SampleGenerator.createSerializedObjectSample();
        HexDocument doc = new HexDocument(bytes, "UserData.ser", "");
        setDocument(doc);
    }

    public void loadSamplePng() {
        byte[] bytes = SampleGenerator.createPngSample();
        HexDocument doc = new HexDocument(bytes, "IconSample.png", "");
        setDocument(doc);
    }

    public void loadSampleElf() {
        byte[] bytes = SampleGenerator.createElfSample();
        HexDocument doc = new HexDocument(bytes, "SampleBinary.elf", "");
        setDocument(doc);
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    public static void main(String[] args) {
        // Initialize FlatLaf Dark theme
        FlatDarkLaf.setup();

        SwingUtilities.invokeLater(() -> {
            HexAnalyzerApp app = new HexAnalyzerApp();
            if (args != null && args.length > 0) {
                File f = new File(args[0]);
                if (f.exists()) {
                    app.loadFile(f);
                }
            }
            app.setVisible(true);
        });
    }
}
