package com.analyzer.hex;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.zip.Adler32;
import java.util.zip.CRC32;

/**
 * Encapsulates the binary data of a file, providing indexing, slicing,
 * in-place editing with undo/redo, string extraction, pattern searching,
 * block entropy calculation, and range checksums.
 */
public class HexDocument {

    private byte[] data;
    private final byte[] originalData;
    private String fileName;
    private String filePath;
    private boolean modified = false;

    // Undo / Redo Stacks
    public static class EditAction {
        public final int offset;
        public final byte oldValue;
        public final byte newValue;

        public EditAction(int offset, byte oldValue, byte newValue) {
            this.offset = offset;
            this.oldValue = oldValue;
            this.newValue = newValue;
        }
    }

    private final Deque<EditAction> undoStack = new ArrayDeque<>();
    private final Deque<EditAction> redoStack = new ArrayDeque<>();
    private final BitSet modifiedBytes = new BitSet();

    public static class ExtractedString {
        private final int offset;
        private final int length;
        private final String value;
        private final boolean isUtf8;

        public ExtractedString(int offset, int length, String value, boolean isUtf8) {
            this.offset = offset;
            this.length = length;
            this.value = value;
            this.isUtf8 = isUtf8;
        }

        public int getOffset() {
            return offset;
        }

        public int getLength() {
            return length;
        }

        public String getValue() {
            return value;
        }

        public boolean isUtf8() {
            return isUtf8;
        }

        public String getOffsetHex() {
            return String.format("0x%08X", offset);
        }
    }

    public static class HashInfo {
        public final String md5;
        public final String sha1;
        public final String sha256;
        public final long crc32;
        public final long adler32;

        public HashInfo(String md5, String sha1, String sha256, long crc32, long adler32) {
            this.md5 = md5;
            this.sha1 = sha1;
            this.sha256 = sha256;
            this.crc32 = crc32;
            this.adler32 = adler32;
        }
    }

    public HexDocument(byte[] data, String fileName, String filePath) {
        this.data = data != null ? data : new byte[0];
        this.originalData = Arrays.copyOf(this.data, this.data.length);
        this.fileName = fileName != null ? fileName : "Untitled";
        this.filePath = filePath != null ? filePath : "";
    }

    public static HexDocument loadFromFile(File file) throws IOException {
        long len = file.length();
        if (len > 150L * 1024 * 1024) {
            throw new IOException("File too large (" + (len / (1024 * 1024)) + " MB). Maximum supported size in memory is 150 MB.");
        }
        byte[] bytes = new byte[(int) len];
        try (FileInputStream fis = new FileInputStream(file)) {
            int totalRead = 0;
            while (totalRead < len) {
                int read = fis.read(bytes, totalRead, (int) (len - totalRead));
                if (read == -1) break;
                totalRead += read;
            }
        }
        return new HexDocument(bytes, file.getName(), file.getAbsolutePath());
    }

    public void saveToFile(File file) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(data);
        }
        this.fileName = file.getName();
        this.filePath = file.getAbsolutePath();
        this.modified = false;
        this.modifiedBytes.clear();
        this.undoStack.clear();
        this.redoStack.clear();
    }

    public boolean isModified() {
        return modified;
    }

    public boolean isByteModified(int index) {
        return modifiedBytes.get(index);
    }

    public int getModifiedCount() {
        return modifiedBytes.cardinality();
    }

    /**
     * Mutates a single byte at offset and records an undo action.
     */
    public boolean setByte(int offset, byte newValue) {
        if (offset < 0 || offset >= data.length) return false;
        byte oldVal = data[offset];
        if (oldVal == newValue) return false;

        data[offset] = newValue;
        undoStack.push(new EditAction(offset, oldVal, newValue));
        redoStack.clear();

        if (offset < originalData.length && data[offset] == originalData[offset]) {
            modifiedBytes.clear(offset);
        } else {
            modifiedBytes.set(offset);
        }

        modified = !modifiedBytes.isEmpty();
        return true;
    }

    public boolean canUndo() {
        return !undoStack.isEmpty();
    }

    public boolean canRedo() {
        return !redoStack.isEmpty();
    }

    public int undo() {
        if (!canUndo()) return -1;
        EditAction act = undoStack.pop();
        data[act.offset] = act.oldValue;
        redoStack.push(act);

        if (act.offset < originalData.length && data[act.offset] == originalData[act.offset]) {
            modifiedBytes.clear(act.offset);
        } else {
            modifiedBytes.set(act.offset);
        }
        modified = !modifiedBytes.isEmpty();
        return act.offset;
    }

    public int redo() {
        if (!canRedo()) return -1;
        EditAction act = redoStack.pop();
        data[act.offset] = act.newValue;
        undoStack.push(act);

        if (act.offset < originalData.length && data[act.offset] == originalData[act.offset]) {
            modifiedBytes.clear(act.offset);
        } else {
            modifiedBytes.set(act.offset);
        }
        modified = !modifiedBytes.isEmpty();
        return act.offset;
    }

    public byte[] getData() {
        return data;
    }

    public int getLength() {
        return data.length;
    }

    public String getFileName() {
        return fileName;
    }

    public String getFilePath() {
        return filePath;
    }

    public long getFileSize() {
        return data.length;
    }

    public int getRowCount(int bytesPerRow) {
        if (data.length == 0) return 0;
        return (data.length + bytesPerRow - 1) / bytesPerRow;
    }

    public byte getByte(int index) {
        if (index < 0 || index >= data.length) {
            return 0;
        }
        return data[index];
    }

    public byte[] getBytes(int offset, int length) {
        if (offset < 0 || offset >= data.length || length <= 0) {
            return new byte[0];
        }
        int actualLen = Math.min(length, data.length - offset);
        byte[] slice = new byte[actualLen];
        System.arraycopy(data, offset, slice, 0, actualLen);
        return slice;
    }

    public static char getPrintableChar(byte b) {
        int unsigned = b & 0xFF;
        if (unsigned >= 32 && unsigned <= 126) {
            return (char) unsigned;
        }
        return '.';
    }

    /**
     * Scans and extracts all readable ASCII/UTF-8 strings.
     */
    public List<ExtractedString> extractStrings(int minLength) {
        List<ExtractedString> results = new ArrayList<>();
        if (data.length == 0 || minLength <= 0) return results;

        StringBuilder currentStr = new StringBuilder();
        int startOffset = -1;

        for (int i = 0; i < data.length; i++) {
            int b = data[i] & 0xFF;
            boolean isPrintable = (b >= 32 && b <= 126) || b == '\t' || b == '\n' || b == '\r';

            if (isPrintable) {
                if (currentStr.length() == 0) {
                    startOffset = i;
                }
                currentStr.append((char) b);
            } else {
                if (currentStr.length() >= minLength) {
                    results.add(new ExtractedString(startOffset, currentStr.length(), currentStr.toString(), false));
                }
                currentStr.setLength(0);
                startOffset = -1;
            }
        }

        if (currentStr.length() >= minLength) {
            results.add(new ExtractedString(startOffset, currentStr.length(), currentStr.toString(), false));
        }

        return results;
    }

    /**
     * Search for ASCII / UTF-8 text in the binary data.
     */
    public List<Integer> searchText(String query, boolean matchCase) {
        List<Integer> matches = new ArrayList<>();
        if (query == null || query.isEmpty() || data.length == 0) return matches;

        byte[] pattern = query.getBytes(StandardCharsets.UTF_8);
        if (!matchCase) {
            String lowerQuery = query.toLowerCase();
            byte[] lowerPattern = lowerQuery.getBytes(StandardCharsets.UTF_8);
            int pLen = pattern.length;

            for (int i = 0; i <= data.length - pLen; i++) {
                boolean match = true;
                for (int j = 0; j < pLen; j++) {
                    int b1 = Character.toLowerCase((char) (data[i + j] & 0xFF));
                    int b2 = Character.toLowerCase((char) (lowerPattern[j] & 0xFF));
                    if (b1 != b2) {
                        match = false;
                        break;
                    }
                }
                if (match) {
                    matches.add(i);
                }
            }
        } else {
            int pLen = pattern.length;
            for (int i = 0; i <= data.length - pLen; i++) {
                boolean match = true;
                for (int j = 0; j < pLen; j++) {
                    if (data[i + j] != pattern[j]) {
                        match = false;
                        break;
                    }
                }
                if (match) {
                    matches.add(i);
                }
            }
        }

        return matches;
    }

    /**
     * Search for raw hex byte sequence (e.g., "CA FE BA BE" or "cafe").
     */
    public List<Integer> searchHex(String hexQuery) {
        List<Integer> matches = new ArrayList<>();
        if (hexQuery == null || hexQuery.trim().isEmpty() || data.length == 0) return matches;

        String clean = hexQuery.replaceAll("[^0-9a-fA-F]", "");
        if (clean.length() == 0 || clean.length() % 2 != 0) {
            return matches;
        }

        int pLen = clean.length() / 2;
        byte[] pattern = new byte[pLen];
        for (int i = 0; i < pLen; i++) {
            pattern[i] = (byte) Integer.parseInt(clean.substring(i * 2, i * 2 + 2), 16);
        }

        for (int i = 0; i <= data.length - pLen; i++) {
            boolean match = true;
            for (int j = 0; j < pLen; j++) {
                if (data[i + j] != pattern[j]) {
                    match = false;
                    break;
                }
            }
            if (match) {
                matches.add(i);
            }
        }

        return matches;
    }

    /**
     * Computes the Shannon entropy of the file data (0.00 to 8.00).
     */
    public double calculateEntropy() {
        return calculateRangeEntropy(0, data.length);
    }

    public double calculateRangeEntropy(int start, int length) {
        if (data.length == 0 || length <= 0) return 0.0;
        int end = Math.min(data.length, start + length);
        int total = end - start;
        if (total <= 0) return 0.0;

        int[] freq = new int[256];
        for (int i = start; i < end; i++) {
            freq[data[i] & 0xFF]++;
        }

        double entropy = 0.0;
        for (int count : freq) {
            if (count > 0) {
                double p = (double) count / total;
                entropy -= p * (Math.log(p) / Math.log(2));
            }
        }
        return entropy;
    }

    /**
     * Computes block entropy array for visual heatmap display.
     */
    public double[] computeBlockEntropy(int blockCount) {
        if (data.length == 0 || blockCount <= 0) return new double[0];
        blockCount = Math.min(blockCount, data.length);
        double[] entropyMap = new double[blockCount];
        int blockSize = Math.max(1, data.length / blockCount);

        for (int i = 0; i < blockCount; i++) {
            int start = i * blockSize;
            int len = (i == blockCount - 1) ? (data.length - start) : blockSize;
            entropyMap[i] = calculateRangeEntropy(start, len);
        }
        return entropyMap;
    }

    /**
     * Computes byte frequency histogram (counts per byte 0-255).
     */
    public int[] computeByteFrequencies() {
        int[] freq = new int[256];
        for (byte b : data) {
            freq[b & 0xFF]++;
        }
        return freq;
    }

    /**
     * Computes MD5, SHA-1, SHA-256, CRC32, and Adler32 hashes.
     */
    public HashInfo computeHashes() {
        return computeRangeHashes(0, data.length);
    }

    public HashInfo computeRangeHashes(int offset, int length) {
        if (data.length == 0 || length <= 0) {
            return new HashInfo("-", "-", "-", 0, 0);
        }
        int actualLen = Math.min(length, data.length - offset);

        CRC32 crc = new CRC32();
        crc.update(data, offset, actualLen);

        Adler32 adler = new Adler32();
        adler.update(data, offset, actualLen);

        try {
            MessageDigest md5Digest = MessageDigest.getInstance("MD5");
            MessageDigest sha1Digest = MessageDigest.getInstance("SHA-1");
            MessageDigest sha256Digest = MessageDigest.getInstance("SHA-256");

            md5Digest.update(data, offset, actualLen);
            sha1Digest.update(data, offset, actualLen);
            sha256Digest.update(data, offset, actualLen);

            return new HashInfo(
                    bytesToHex(md5Digest.digest()),
                    bytesToHex(sha1Digest.digest()),
                    bytesToHex(sha256Digest.digest()),
                    crc.getValue(),
                    adler.getValue()
            );
        } catch (Exception e) {
            return new HashInfo("Error", "Error", "Error", crc.getValue(), adler.getValue());
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
