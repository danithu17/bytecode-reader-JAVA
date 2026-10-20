package com.analyzer.hex;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.DecimalFormat;
import java.util.function.Consumer;

/**
 * Visual Entropy Heatmap & 256-Byte Frequency Histogram Tab.
 * Provides visual identification of code vs compressed vs encrypted sections,
 * and byte distribution analysis.
 */
public class EntropyVisualizerPanel extends JPanel {

    private HexDocument currentDocument;
    private Consumer<Integer> offsetNavigationHandler;

    private final JLabel overallEntropyLabel;
    private final JComboBox<Integer> blockCountCombo;
    private final HeatmapCanvas heatmapCanvas;
    private final HistogramCanvas histogramCanvas;
    private final JLabel heatmapHoverLabel;
    private final JLabel histogramHoverLabel;

    public EntropyVisualizerPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(14, 16, 14, 16));

        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        JPanel titleRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));

        JLabel title = new JLabel("Visual Entropy & Byte Distribution");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 14f));
        title.setForeground(new Color(130, 185, 255));
        titleRow.add(title);

        overallEntropyLabel = new JLabel("Overall Entropy: 0.00 / 8.00");
        overallEntropyLabel.setFont(new Font(Font.MONOSPACED, Font.BOLD, 12));
        overallEntropyLabel.setForeground(new Color(200, 210, 225));
        titleRow.add(overallEntropyLabel);

        headerPanel.add(titleRow, BorderLayout.WEST);

        JPanel blockControl = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        blockControl.add(new JLabel("Resolution:"));
        blockCountCombo = new JComboBox<>(new Integer[]{64, 128, 256, 512, 1024});
        blockCountCombo.setSelectedItem(256);
        blockCountCombo.addActionListener(e -> updateHeatmap());
        blockControl.add(blockCountCombo);

        headerPanel.add(blockControl, BorderLayout.EAST);
        add(headerPanel, BorderLayout.NORTH);

        // Center split: Top is Heatmap, Bottom is Histogram
        JPanel chartsPanel = new JPanel(new GridLayout(2, 1, 10, 14));

        // 1. Heatmap Container
        JPanel heatmapBox = new JPanel(new BorderLayout(6, 6));
        heatmapBox.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(55, 60, 72)),
                "Entropy Heatmap Strip (Click any block to jump in Hex Viewer)"
        ));

        heatmapCanvas = new HeatmapCanvas();
        heatmapBox.add(heatmapCanvas, BorderLayout.CENTER);

        heatmapHoverLabel = new JLabel("Hover over blocks to inspect local entropy");
        heatmapHoverLabel.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        heatmapHoverLabel.setForeground(new Color(160, 165, 175));
        heatmapHoverLabel.setBorder(new EmptyBorder(4, 6, 2, 6));
        heatmapBox.add(heatmapHoverLabel, BorderLayout.SOUTH);

        chartsPanel.add(heatmapBox);

        // 2. Histogram Container
        JPanel histBox = new JPanel(new BorderLayout(6, 6));
        histBox.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(55, 60, 72)),
                "256-Byte Frequency Distribution (0x00 to 0xFF)"
        ));

        histogramCanvas = new HistogramCanvas();
        histBox.add(histogramCanvas, BorderLayout.CENTER);

        histogramHoverLabel = new JLabel("Hover over byte bars to see frequency and percentage");
        histogramHoverLabel.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        histogramHoverLabel.setForeground(new Color(160, 165, 175));
        histogramHoverLabel.setBorder(new EmptyBorder(4, 6, 2, 6));
        histBox.add(histogramHoverLabel, BorderLayout.SOUTH);

        chartsPanel.add(histBox);

        add(chartsPanel, BorderLayout.CENTER);
    }

    public void setOffsetNavigationHandler(Consumer<Integer> handler) {
        this.offsetNavigationHandler = handler;
    }

    public void setDocument(HexDocument doc) {
        this.currentDocument = doc;
        if (doc != null && doc.getLength() > 0) {
            double overall = doc.calculateEntropy();
            overallEntropyLabel.setText(String.format("Overall Entropy: %.4f / 8.0000", overall));
        } else {
            overallEntropyLabel.setText("Overall Entropy: -");
        }
        updateHeatmap();
        updateHistogram();
    }

    private void updateHeatmap() {
        if (currentDocument == null || currentDocument.getLength() == 0) {
            heatmapCanvas.setBlocks(new double[0], 0);
            return;
        }
        int blocks = (Integer) blockCountCombo.getSelectedItem();
        double[] entropy = currentDocument.computeBlockEntropy(blocks);
        heatmapCanvas.setBlocks(entropy, currentDocument.getLength());
    }

    private void updateHistogram() {
        if (currentDocument == null || currentDocument.getLength() == 0) {
            histogramCanvas.setFrequencies(new int[256], 0);
            return;
        }
        int[] freq = currentDocument.computeByteFrequencies();
        histogramCanvas.setFrequencies(freq, currentDocument.getLength());
    }

    /**
     * Interactive Canvas for the Entropy Heatmap
     */
    private class HeatmapCanvas extends JComponent {
        private double[] entropyBlocks = new double[0];
        private int totalDocLength = 0;
        private int hoveredIndex = -1;

        public HeatmapCanvas() {
            setPreferredSize(new Dimension(500, 110));
            setBackground(new Color(24, 26, 32));

            MouseAdapter ma = new MouseAdapter() {
                @Override
                public void mouseMoved(MouseEvent e) {
                    if (entropyBlocks.length == 0 || getWidth() <= 0) return;
                    int idx = (int) ((double) e.getX() / getWidth() * entropyBlocks.length);
                    if (idx >= 0 && idx < entropyBlocks.length) {
                        hoveredIndex = idx;
                        int blockSize = Math.max(1, totalDocLength / entropyBlocks.length);
                        int start = idx * blockSize;
                        int end = Math.min(totalDocLength - 1, (idx + 1) * blockSize - 1);
                        heatmapHoverLabel.setText(String.format(
                                "Block #%d | Range: 0x%08X - 0x%08X (%d bytes) | Local Entropy: %.4f",
                                idx + 1, start, end, (end - start + 1), entropyBlocks[idx]));
                        repaint();
                    }
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hoveredIndex = -1;
                    heatmapHoverLabel.setText("Hover over blocks to inspect local entropy");
                    repaint();
                }

                @Override
                public void mouseClicked(MouseEvent e) {
                    if (entropyBlocks.length == 0 || offsetNavigationHandler == null || getWidth() <= 0) return;
                    int idx = (int) ((double) e.getX() / getWidth() * entropyBlocks.length);
                    if (idx >= 0 && idx < entropyBlocks.length) {
                        int blockSize = Math.max(1, totalDocLength / entropyBlocks.length);
                        int targetOffset = idx * blockSize;
                        offsetNavigationHandler.accept(targetOffset);
                    }
                }
            };
            addMouseListener(ma);
            addMouseMotionListener(ma);
        }

        public void setBlocks(double[] blocks, int totalLen) {
            this.entropyBlocks = blocks != null ? blocks : new double[0];
            this.totalDocLength = totalLen;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            g2.setColor(new Color(22, 24, 30));
            g2.fillRect(0, 0, w, h);

            if (entropyBlocks.length == 0) {
                g2.setColor(new Color(110, 115, 130));
                g2.drawString("No data to visualize", w / 2 - 50, h / 2);
                g2.dispose();
                return;
            }

            double blockWidth = (double) w / entropyBlocks.length;
            int barAreaHeight = h - 24;

            for (int i = 0; i < entropyBlocks.length; i++) {
                double e = entropyBlocks[i];
                double x = i * blockWidth;
                double nextX = (i + 1) * blockWidth;
                int pw = Math.max(1, (int) Math.ceil(nextX - x));

                // Color based on entropy (0 = Navy Blue, 4 = Cyan/Green, 6.5 = Orange, 8 = Bright Red)
                Color c = getEntropyColor(e);
                g2.setColor(c);

                int barH = (int) ((e / 8.0) * barAreaHeight);
                int barY = barAreaHeight - barH;

                // Draw filled vertical bar
                g2.fillRect((int) x, barY, pw, barH);

                // Highlight hovered block
                if (i == hoveredIndex) {
                    g2.setColor(Color.WHITE);
                    g2.drawRect((int) x, barY - 1, pw, barH + 1);
                }
            }

            // Draw baseline & scale markers
            g2.setColor(new Color(60, 65, 78));
            g2.drawLine(0, barAreaHeight, w, barAreaHeight);

            g2.setColor(new Color(140, 145, 160));
            g2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));
            g2.drawString("0x00000000", 6, h - 6);
            String endStr = String.format("0x%08X (EOF)", Math.max(0, totalDocLength - 1));
            g2.drawString(endStr, w - g2.getFontMetrics().stringWidth(endStr) - 6, h - 6);

            // Legend labels
            g2.setColor(new Color(50, 130, 240));
            g2.drawString("■ Low (<3.0)", w / 2 - 120, h - 6);
            g2.setColor(new Color(50, 200, 120));
            g2.drawString("■ Code (3.0-5.5)", w / 2 - 30, h - 6);
            g2.setColor(new Color(255, 80, 50));
            g2.drawString("■ Packed/Encrypted (>7.0)", w / 2 + 75, h - 6);

            g2.dispose();
        }

        private Color getEntropyColor(double e) {
            float ratio = (float) Math.min(1.0, Math.max(0.0, e / 8.0));
            if (ratio < 0.35f) {
                // Low entropy: navy to cyan
                float sub = ratio / 0.35f;
                return new Color((int) (20 + sub * 20), (int) (60 + sub * 120), (int) (180 + sub * 60));
            } else if (ratio < 0.70f) {
                // Medium entropy: cyan to emerald / yellow
                float sub = (ratio - 0.35f) / 0.35f;
                return new Color((int) (40 + sub * 180), (int) (180 + sub * 40), (int) (100 - sub * 40));
            } else {
                // High entropy: orange to intense crimson
                float sub = (ratio - 0.70f) / 0.30f;
                return new Color((int) (220 + sub * 35), (int) (120 - sub * 80), (int) (40 - sub * 20));
            }
        }
    }

    /**
     * Interactive Canvas for the 256-Byte Frequency Histogram
     */
    private class HistogramCanvas extends JComponent {
        private int[] frequencies = new int[256];
        private int totalBytes = 0;
        private int hoveredByte = -1;

        public HistogramCanvas() {
            setPreferredSize(new Dimension(500, 120));
            setBackground(new Color(24, 26, 32));

            MouseAdapter ma = new MouseAdapter() {
                @Override
                public void mouseMoved(MouseEvent e) {
                    if (totalBytes <= 0 || getWidth() <= 0) return;
                    int b = (int) ((double) e.getX() / getWidth() * 256);
                    if (b >= 0 && b < 256) {
                        hoveredByte = b;
                        int count = frequencies[b];
                        double pct = (double) count / totalBytes * 100.0;
                        char c = HexDocument.getPrintableChar((byte) b);
                        DecimalFormat df = new DecimalFormat("#,###");
                        histogramHoverLabel.setText(String.format(
                                "Byte 0x%02X (%d | '%c') — Count: %s (%.2f%% of file)",
                                b, b, c, df.format(count), pct));
                        repaint();
                    }
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hoveredByte = -1;
                    histogramHoverLabel.setText("Hover over byte bars to see frequency and percentage");
                    repaint();
                }
            };
            addMouseListener(ma);
            addMouseMotionListener(ma);
        }

        public void setFrequencies(int[] freq, int total) {
            this.frequencies = freq != null ? freq : new int[256];
            this.totalBytes = total;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            g2.setColor(new Color(22, 24, 30));
            g2.fillRect(0, 0, w, h);

            if (totalBytes <= 0) {
                g2.setColor(new Color(110, 115, 130));
                g2.drawString("No data loaded", w / 2 - 40, h / 2);
                g2.dispose();
                return;
            }

            int maxFreq = 1;
            for (int f : frequencies) {
                if (f > maxFreq) maxFreq = f;
            }

            double barW = (double) w / 256.0;
            int chartH = h - 22;

            for (int b = 0; b < 256; b++) {
                int count = frequencies[b];
                double x = b * barW;
                int barHeight = (int) (((double) count / maxFreq) * chartH);
                int y = chartH - barHeight;

                // Color coding
                if (b == 0) {
                    g2.setColor(new Color(100, 120, 240)); // 0x00 Null
                } else if (b >= 32 && b <= 126) {
                    g2.setColor(new Color(70, 205, 145));  // Printable ASCII
                } else {
                    g2.setColor(new Color(230, 145, 60));  // Non-printable binary
                }

                int pw = Math.max(1, (int) Math.ceil(barW));
                g2.fillRect((int) x, y, pw, barHeight);

                if (b == hoveredByte) {
                    g2.setColor(Color.WHITE);
                    g2.drawRect((int) x, y - 1, pw, barHeight + 1);
                }
            }

            // Baseline
            g2.setColor(new Color(60, 65, 78));
            g2.drawLine(0, chartH, w, chartH);

            // Labels
            g2.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 10));
            g2.setColor(new Color(100, 120, 240));
            g2.drawString("0x00 (Null)", 6, h - 6);

            g2.setColor(new Color(70, 205, 145));
            g2.drawString("0x20..0x7E (Printable ASCII)", w / 3, h - 6);

            g2.setColor(new Color(230, 145, 60));
            g2.drawString("0xFF (High Byte)", w - 100, h - 6);

            g2.dispose();
        }
    }
}
