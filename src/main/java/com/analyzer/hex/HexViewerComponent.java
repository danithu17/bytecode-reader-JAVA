package com.analyzer.hex;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * High-performance virtualized 3-column Hex and Binary viewer component.
 * Displays Offset, Hex/Binary, and ASCII representations with synchronized
 * selection, search highlighting, and keyboard navigation.
 */
public class HexViewerComponent extends JComponent implements Scrollable {

    public interface SelectionListener {
        void onSelectionChanged(int offset, int length);
    }

    private HexDocument document;
    private boolean binaryMode = false;
    private boolean editable = true;
    private int bytesPerRow = 16;

    // Selection state
    private int selectionStart = -1;
    private int selectionEnd = -1;
    private int cursorOffset = -1;
    private int hoverOffset = -1;
    private boolean editingHexHighNibble = true;
    private int hexInputBuffer = -1;
    private boolean focusInAscii = false;

    // Search matches
    private List<Integer> searchMatches = new ArrayList<>();
    private int currentSearchMatchIndex = -1;
    private int searchMatchLength = 1;

    // Layout metrics
    private Font monospacedFont;
    private int charWidth = 8;
    private int charHeight = 16;
    private int lineHeight = 20;
    private int ascent = 14;
    private final int padLeft = 14;
    private final int padTop = 10;
    private final int colGap = 24;

    // Selection listeners
    private final List<SelectionListener> selectionListeners = new ArrayList<>();

    // Palette Colors (Modern Dark Theme)
    private static final Color BG_COLOR = new Color(30, 32, 38);
    private static final Color OFFSET_COLOR = new Color(110, 118, 135);
    private static final Color HEX_BYTE_EVEN = new Color(225, 230, 240);
    private static final Color HEX_BYTE_ZERO = new Color(90, 95, 108);
    private static final Color ASCII_TEXT_COLOR = new Color(195, 205, 220);
    private static final Color ASCII_NON_PRINTABLE = new Color(95, 100, 115);
    private static final Color SELECTION_BG = new Color(50, 100, 180, 180);
    private static final Color SELECTION_BORDER = new Color(80, 150, 255);
    private static final Color HOVER_BORDER = new Color(75, 85, 105, 150);
    private static final Color SEARCH_MATCH_BG = new Color(220, 150, 30, 130);
    private static final Color CURRENT_MATCH_BG = new Color(255, 100, 50, 200);
    private static final Color MODIFIED_BYTE_COLOR = new Color(255, 167, 38);
    private static final Color MODIFIED_BYTE_BG = new Color(255, 110, 0, 45);
    private static final Color DIVIDER_LINE_COLOR = new Color(48, 52, 62);

    public HexViewerComponent() {
        setOpaque(true);
        setBackground(BG_COLOR);
        setFocusable(true);

        setupFont();
        setupMouseListeners();
        setupKeyListeners();
    }

    private void setupFont() {
        // Try to pick clean modern monospaced fonts
        String[] preferredFonts = {"JetBrains Mono", "Cascadia Code", "Consolas", "Courier New", Font.MONOSPACED};
        for (String fName : preferredFonts) {
            Font f = new Font(fName, Font.PLAIN, 13);
            if (f.getFamily().equalsIgnoreCase(fName) || fName.equals(Font.MONOSPACED)) {
                monospacedFont = f;
                break;
            }
        }
        if (monospacedFont == null) {
            monospacedFont = new Font(Font.MONOSPACED, Font.PLAIN, 13);
        }
        updateFontMetrics();
    }

    private void updateFontMetrics() {
        FontMetrics fm = getFontMetrics(monospacedFont);
        charWidth = Math.max(7, fm.charWidth('0'));
        charHeight = fm.getHeight();
        lineHeight = charHeight + 5;
        ascent = fm.getAscent();
        revalidatePreferredSize();
    }

    public void addSelectionListener(SelectionListener listener) {
        selectionListeners.add(listener);
    }

    private void notifySelection(int offset, int length) {
        for (SelectionListener l : selectionListeners) {
            l.onSelectionChanged(offset, length);
        }
    }

    public void setDocument(HexDocument document) {
        this.document = document;
        this.selectionStart = -1;
        this.selectionEnd = -1;
        this.cursorOffset = -1;
        this.hoverOffset = -1;
        this.searchMatches.clear();
        this.currentSearchMatchIndex = -1;

        if (document != null && document.getLength() > 0) {
            setSelection(0, 0);
        } else {
            notifySelection(-1, 0);
        }

        revalidatePreferredSize();
        repaint();
    }

    public HexDocument getDocument() {
        return document;
    }

    public void setBinaryMode(boolean binary) {
        if (this.binaryMode != binary) {
            this.binaryMode = binary;
            // 8 bytes in binary mode for optimal readability, 16 in hex mode
            this.bytesPerRow = binary ? 8 : 16;
            revalidatePreferredSize();
            repaint();
        }
    }

    public boolean isBinaryMode() {
        return binaryMode;
    }

    public void setSearchMatches(List<Integer> matches, int matchLength) {
        this.searchMatches = matches != null ? new ArrayList<>(matches) : new ArrayList<>();
        this.searchMatchLength = Math.max(1, matchLength);
        this.currentSearchMatchIndex = -1;
        if (!this.searchMatches.isEmpty()) {
            setCurrentSearchMatchIndex(0);
        } else {
            repaint();
        }
    }

    public void setCurrentSearchMatchIndex(int index) {
        if (searchMatches.isEmpty()) {
            currentSearchMatchIndex = -1;
            repaint();
            return;
        }
        if (index < 0) index = searchMatches.size() - 1;
        if (index >= searchMatches.size()) index = 0;

        currentSearchMatchIndex = index;
        int targetOffset = searchMatches.get(index);
        setSelection(targetOffset, targetOffset + searchMatchLength - 1);
        scrollToOffset(targetOffset);
        repaint();
    }

    public int getCurrentSearchMatchIndex() {
        return currentSearchMatchIndex;
    }

    public int getSearchMatchCount() {
        return searchMatches.size();
    }

    public void nextMatch() {
        if (!searchMatches.isEmpty()) {
            setCurrentSearchMatchIndex(currentSearchMatchIndex + 1);
        }
    }

    public void prevMatch() {
        if (!searchMatches.isEmpty()) {
            setCurrentSearchMatchIndex(currentSearchMatchIndex - 1);
        }
    }

    public void setSelection(int start, int end) {
        if (document == null || document.getLength() == 0) {
            selectionStart = -1;
            selectionEnd = -1;
            cursorOffset = -1;
            notifySelection(-1, 0);
            repaint();
            return;
        }

        int docLen = document.getLength();
        start = Math.max(0, Math.min(start, docLen - 1));
        end = Math.max(0, Math.min(end, docLen - 1));

        int actualStart = Math.min(start, end);
        int actualEnd = Math.max(start, end);

        this.selectionStart = actualStart;
        this.selectionEnd = actualEnd;
        this.cursorOffset = end;

        int length = actualEnd - actualStart + 1;
        notifySelection(actualStart, length);
        repaint();
    }

    public int getSelectionStart() {
        return selectionStart;
    }

    public int getSelectionEnd() {
        return selectionEnd;
    }

    public int getCursorOffset() {
        return cursorOffset;
    }

    public void scrollToOffset(int offset) {
        if (document == null || offset < 0 || offset >= document.getLength()) return;

        int row = offset / bytesPerRow;
        int y = padTop + row * lineHeight;
        Rectangle rect = new Rectangle(0, Math.max(0, y - lineHeight * 2), getWidth(), lineHeight * 5);
        scrollRectToVisible(rect);
    }

    private void revalidatePreferredSize() {
        int totalRows = document != null ? document.getRowCount(bytesPerRow) : 0;
        int height = padTop * 2 + Math.max(1, totalRows) * lineHeight;

        int offsetColWidth = 10 * charWidth;
        int byteColWidth = binaryMode ? (bytesPerRow * 9 + 2) * charWidth : (bytesPerRow * 3 + 2) * charWidth;
        int asciiColWidth = bytesPerRow * charWidth + 10;
        int width = padLeft * 2 + offsetColWidth + byteColWidth + asciiColWidth + colGap * 2;

        setPreferredSize(new Dimension(width, height));
        revalidate();
    }

    // Coordinates mapping
    private int getOffsetColStartX() {
        return padLeft;
    }

    private int getByteColStartX() {
        int offsetWidth = 10 * charWidth;
        return padLeft + offsetWidth + colGap;
    }

    private int getAsciiColStartX() {
        int byteWidth = binaryMode ? (bytesPerRow * 9 + 2) * charWidth : (bytesPerRow * 3 + 2) * charWidth;
        return getByteColStartX() + byteWidth + colGap;
    }

    private int offsetAtPoint(Point pt) {
        if (document == null || document.getLength() == 0) return -1;

        int row = (pt.y - padTop) / lineHeight;
        if (row < 0 || row >= document.getRowCount(bytesPerRow)) return -1;

        int byteStartX = getByteColStartX();
        int asciiStartX = getAsciiColStartX();

        // Check Byte column
        int byteCol = -1;
        if (!binaryMode) {
            // Hex mode: 3 chars per byte, extra space after byte 7
            int relX = pt.x - byteStartX;
            if (relX >= 0) {
                // Adjust for gap after byte 7
                int gapX = 8 * 3 * charWidth;
                if (relX >= gapX) {
                    relX -= charWidth;
                }
                byteCol = relX / (3 * charWidth);
            }
        } else {
            // Binary mode: 9 chars per byte (8 bits + 1 space)
            int relX = pt.x - byteStartX;
            if (relX >= 0) {
                byteCol = relX / (9 * charWidth);
            }
        }

        if (byteCol >= 0 && byteCol < bytesPerRow) {
            int off = row * bytesPerRow + byteCol;
            return Math.min(off, document.getLength() - 1);
        }

        // Check ASCII column
        int relAsciiX = pt.x - asciiStartX;
        if (relAsciiX >= 0) {
            int asciiCol = relAsciiX / charWidth;
            if (asciiCol >= 0 && asciiCol < bytesPerRow) {
                int off = row * bytesPerRow + asciiCol;
                return Math.min(off, document.getLength() - 1);
            }
        }

        return -1;
    }

    private void setupMouseListeners() {
        MouseAdapter adapter = new MouseAdapter() {
            private int dragAnchor = -1;

            @Override
            public void mousePressed(MouseEvent e) {
                requestFocusInWindow();
                int off = offsetAtPoint(e.getPoint());
                if (off != -1) {
                    dragAnchor = off;
                    focusInAscii = (e.getPoint().x >= getAsciiColStartX());
                    editingHexHighNibble = true;
                    hexInputBuffer = -1;

                    if (e.isShiftDown() && selectionStart != -1) {
                        setSelection(selectionStart, off);
                    } else {
                        setSelection(off, off);
                    }
                }
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                int off = offsetAtPoint(e.getPoint());
                if (off != -1 && dragAnchor != -1) {
                    setSelection(dragAnchor, off);
                    scrollToOffset(off);
                }
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                int off = offsetAtPoint(e.getPoint());
                if (off != hoverOffset) {
                    hoverOffset = off;
                    repaint();
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (hoverOffset != -1) {
                    hoverOffset = -1;
                    repaint();
                }
            }
        };

        addMouseListener(adapter);
        addMouseMotionListener(adapter);
    }

    public void setEditable(boolean editable) {
        this.editable = editable;
        repaint();
    }

    public boolean isEditable() {
        return editable;
    }

    public void undo() {
        if (document != null && document.canUndo()) {
            int off = document.undo();
            if (off >= 0) {
                setSelection(off, off);
                scrollToOffset(off);
            }
            repaint();
        }
    }

    public void redo() {
        if (document != null && document.canRedo()) {
            int off = document.redo();
            if (off >= 0) {
                setSelection(off, off);
                scrollToOffset(off);
            }
            repaint();
        }
    }

    private void setupKeyListeners() {
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                if (!editable || document == null || cursorOffset < 0 || cursorOffset >= document.getLength()) return;

                char ch = e.getKeyChar();
                if (e.isControlDown() || ch == KeyEvent.CHAR_UNDEFINED || ch == '\b') return;

                if (focusInAscii) {
                    if (ch >= 32 && ch <= 126) {
                        document.setByte(cursorOffset, (byte) ch);
                        notifySelection(cursorOffset, 1);
                        int next = Math.min(document.getLength() - 1, cursorOffset + 1);
                        setSelection(next, next);
                        scrollToOffset(next);
                        repaint();
                    }
                } else if (!binaryMode) {
                    int digit = Character.digit(ch, 16);
                    if (digit >= 0) {
                        if (editingHexHighNibble) {
                            hexInputBuffer = digit;
                            editingHexHighNibble = false;
                            repaint();
                        } else {
                            byte newByte = (byte) ((hexInputBuffer << 4) | digit);
                            document.setByte(cursorOffset, newByte);
                            notifySelection(cursorOffset, 1);
                            editingHexHighNibble = true;
                            hexInputBuffer = -1;
                            int next = Math.min(document.getLength() - 1, cursorOffset + 1);
                            setSelection(next, next);
                            scrollToOffset(next);
                            repaint();
                        }
                    }
                }
            }

            @Override
            public void keyPressed(KeyEvent e) {
                if (document == null || document.getLength() == 0) return;

                if (e.isControlDown()) {
                    if (e.getKeyCode() == KeyEvent.VK_Z) {
                        undo();
                        e.consume();
                        return;
                    } else if (e.getKeyCode() == KeyEvent.VK_Y) {
                        redo();
                        e.consume();
                        return;
                    }
                }

                int cur = cursorOffset != -1 ? cursorOffset : 0;
                int docLen = document.getLength();
                int next = cur;

                switch (e.getKeyCode()) {
                    case KeyEvent.VK_LEFT:
                        next = Math.max(0, cur - 1);
                        editingHexHighNibble = true;
                        hexInputBuffer = -1;
                        break;
                    case KeyEvent.VK_RIGHT:
                        next = Math.min(docLen - 1, cur + 1);
                        editingHexHighNibble = true;
                        hexInputBuffer = -1;
                        break;
                    case KeyEvent.VK_UP:
                        next = Math.max(0, cur - bytesPerRow);
                        editingHexHighNibble = true;
                        hexInputBuffer = -1;
                        break;
                    case KeyEvent.VK_DOWN:
                        next = Math.min(docLen - 1, cur + bytesPerRow);
                        editingHexHighNibble = true;
                        hexInputBuffer = -1;
                        break;
                    case KeyEvent.VK_PAGE_UP:
                        next = Math.max(0, cur - bytesPerRow * 15);
                        break;
                    case KeyEvent.VK_PAGE_DOWN:
                        next = Math.min(docLen - 1, cur + bytesPerRow * 15);
                        break;
                    case KeyEvent.VK_HOME:
                        if (e.isControlDown()) next = 0;
                        else next = (cur / bytesPerRow) * bytesPerRow;
                        break;
                    case KeyEvent.VK_END:
                        if (e.isControlDown()) next = docLen - 1;
                        else next = Math.min(docLen - 1, (cur / bytesPerRow) * bytesPerRow + (bytesPerRow - 1));
                        break;
                    default:
                        return;
                }

                if (e.isShiftDown()) {
                    setSelection(selectionStart != -1 ? selectionStart : cur, next);
                } else {
                    setSelection(next, next);
                }
                scrollToOffset(next);
                e.consume();
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setFont(monospacedFont);

        // Fill background
        g2.setColor(BG_COLOR);
        g2.fillRect(0, 0, getWidth(), getHeight());

        if (document == null || document.getLength() == 0) {
            g2.setColor(OFFSET_COLOR);
            g2.drawString("No file loaded. Drag & drop a binary file here or click 'Open File'.", padLeft + 20, padTop + 40);
            g2.dispose();
            return;
        }

        Rectangle clip = g2.getClipBounds();
        int totalRows = document.getRowCount(bytesPerRow);
        int docLen = document.getLength();

        // Calculate visible row bounds for high-performance virtual rendering
        int startRow = Math.max(0, (clip.y - padTop) / lineHeight);
        int endRow = Math.min(totalRows - 1, (clip.y + clip.height - padTop) / lineHeight + 1);

        int byteStartX = getByteColStartX();
        int asciiStartX = getAsciiColStartX();

        // Draw vertical column divider guides
        g2.setColor(DIVIDER_LINE_COLOR);
        g2.drawLine(byteStartX - colGap / 2, 0, byteStartX - colGap / 2, getHeight());
        g2.drawLine(asciiStartX - colGap / 2, 0, asciiStartX - colGap / 2, getHeight());

        for (int row = startRow; row <= endRow; row++) {
            int y = padTop + row * lineHeight;
            int textY = y + ascent;
            int rowOffset = row * bytesPerRow;

            // 1. Column 1: Hexadecimal Offset
            g2.setColor(OFFSET_COLOR);
            g2.drawString(String.format("%08X:", rowOffset), padLeft, textY);

            // 2. Column 2 & Column 3: Bytes (Hex/Binary) and ASCII Characters
            int bytesInThisRow = Math.min(bytesPerRow, docLen - rowOffset);

            for (int b = 0; b < bytesInThisRow; b++) {
                int byteOffset = rowOffset + b;
                byte rawByte = document.getByte(byteOffset);
                int uByte = rawByte & 0xFF;

                // Coordinates for this byte in Byte Column
                int byteX;
                int byteCellWidth;
                if (!binaryMode) {
                    int extraGap = (b >= 8) ? charWidth : 0;
                    byteX = byteStartX + (b * 3 * charWidth) + extraGap;
                    byteCellWidth = 2 * charWidth;
                } else {
                    byteX = byteStartX + (b * 9 * charWidth);
                    byteCellWidth = 8 * charWidth;
                }

                // Coordinates for this character in ASCII Column
                int asciiX = asciiStartX + (b * charWidth);
                int asciiCellWidth = charWidth;

                // Check search match highlight
                boolean isMatch = isByteInSearchMatch(byteOffset);
                boolean isCurrentMatch = isByteInCurrentSearchMatch(byteOffset);

                // Check selection
                boolean isSelected = (selectionStart != -1 && byteOffset >= selectionStart && byteOffset <= selectionEnd);
                boolean isHover = (hoverOffset == byteOffset);

                // Check if byte is modified
                boolean isModified = document.isByteModified(byteOffset);

                // Draw Highlights behind Hex & ASCII
                if (isCurrentMatch) {
                    g2.setColor(CURRENT_MATCH_BG);
                    g2.fillRoundRect(byteX - 2, y + 1, byteCellWidth + 4, lineHeight - 2, 4, 4);
                    g2.fillRoundRect(asciiX - 1, y + 1, asciiCellWidth + 2, lineHeight - 2, 3, 3);
                } else if (isMatch) {
                    g2.setColor(SEARCH_MATCH_BG);
                    g2.fillRoundRect(byteX - 2, y + 1, byteCellWidth + 4, lineHeight - 2, 4, 4);
                    g2.fillRoundRect(asciiX - 1, y + 1, asciiCellWidth + 2, lineHeight - 2, 3, 3);
                } else if (isSelected) {
                    g2.setColor(SELECTION_BG);
                    g2.fillRoundRect(byteX - 2, y + 1, byteCellWidth + 4, lineHeight - 2, 4, 4);
                    g2.fillRoundRect(asciiX - 1, y + 1, asciiCellWidth + 2, lineHeight - 2, 3, 3);

                    if (byteOffset == cursorOffset) {
                        g2.setColor(SELECTION_BORDER);
                        g2.drawRoundRect(byteX - 2, y + 1, byteCellWidth + 4, lineHeight - 2, 4, 4);
                        g2.drawRoundRect(asciiX - 1, y + 1, asciiCellWidth + 2, lineHeight - 2, 3, 3);
                    }
                } else if (isModified) {
                    g2.setColor(MODIFIED_BYTE_BG);
                    g2.fillRoundRect(byteX - 2, y + 1, byteCellWidth + 4, lineHeight - 2, 4, 4);
                    g2.fillRoundRect(asciiX - 1, y + 1, asciiCellWidth + 2, lineHeight - 2, 3, 3);
                } else if (isHover) {
                    g2.setColor(HOVER_BORDER);
                    g2.drawRoundRect(byteX - 2, y + 1, byteCellWidth + 4, lineHeight - 2, 4, 4);
                    g2.drawRoundRect(asciiX - 1, y + 1, asciiCellWidth + 2, lineHeight - 2, 3, 3);
                }

                // Render Byte Text (Hex or Binary)
                if (isSelected || isCurrentMatch) {
                    g2.setColor(Color.WHITE);
                } else if (isModified) {
                    g2.setColor(MODIFIED_BYTE_COLOR);
                } else if (uByte == 0) {
                    g2.setColor(HEX_BYTE_ZERO);
                } else {
                    g2.setColor(HEX_BYTE_EVEN);
                }

                if (!binaryMode) {
                    if (byteOffset == cursorOffset && !focusInAscii && !editingHexHighNibble) {
                        g2.setColor(new Color(255, 235, 100));
                        g2.drawString(String.format("%X_", hexInputBuffer), byteX, textY);
                    } else {
                        String hexStr = String.format("%02X", uByte);
                        g2.drawString(hexStr, byteX, textY);
                    }
                } else {
                    String binStr = String.format("%8s", Integer.toBinaryString(uByte)).replace(' ', '0');
                    g2.drawString(binStr, byteX, textY);
                }

                // Render ASCII Character
                char c = HexDocument.getPrintableChar(rawByte);
                if (isSelected || isCurrentMatch) {
                    g2.setColor(Color.WHITE);
                } else if (isModified) {
                    g2.setColor(MODIFIED_BYTE_COLOR);
                } else if (c == '.') {
                    g2.setColor(ASCII_NON_PRINTABLE);
                } else {
                    g2.setColor(ASCII_TEXT_COLOR);
                }
                g2.drawString(String.valueOf(c), asciiX, textY);
            }
        }

        g2.dispose();
    }

    private boolean isByteInSearchMatch(int offset) {
        if (searchMatches.isEmpty()) return false;
        for (int matchStart : searchMatches) {
            if (offset >= matchStart && offset < matchStart + searchMatchLength) {
                return true;
            }
        }
        return false;
    }

    private boolean isByteInCurrentSearchMatch(int offset) {
        if (currentSearchMatchIndex < 0 || currentSearchMatchIndex >= searchMatches.size()) {
            return false;
        }
        int curStart = searchMatches.get(currentSearchMatchIndex);
        return (offset >= curStart && offset < curStart + searchMatchLength);
    }

    // Scrollable implementation for smooth scrolling
    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return getPreferredSize();
    }

    @Override
    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
        return lineHeight;
    }

    @Override
    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
        return lineHeight * 10;
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        return false;
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
        return false;
    }
}
