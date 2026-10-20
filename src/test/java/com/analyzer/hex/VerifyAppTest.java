package com.analyzer.hex;

import java.util.List;

public class VerifyAppTest {

    public static void main(String[] args) {
        System.out.println("Starting HexAnalyzer Comprehensive Verification Tests...");

        // 1. Test Magic Signatures
        byte[] classBytes = SampleGenerator.createJavaClassSample();
        FileSignatureDetector.DetectionResult res1 = FileSignatureDetector.detect(classBytes);
        assertCondition(res1.isDetected(), "Java Class detection flag");
        assertCondition(res1.getName().contains("Java Class"), "Java Class name detection: " + res1.getName());

        byte[] serBytes = SampleGenerator.createSerializedObjectSample();
        FileSignatureDetector.DetectionResult res2 = FileSignatureDetector.detect(serBytes);
        assertCondition(res2.isDetected(), "Java Serialized Object detection flag");
        assertCondition(res2.getName().contains("Java Serialized"), "Java Serialized name detection: " + res2.getName());

        byte[] pngBytes = SampleGenerator.createPngSample();
        FileSignatureDetector.DetectionResult res3 = FileSignatureDetector.detect(pngBytes);
        assertCondition(res3.isDetected() && res3.getName().contains("PNG"), "PNG detection: " + res3.getName());

        byte[] elfBytes = SampleGenerator.createElfSample();
        FileSignatureDetector.DetectionResult res4 = FileSignatureDetector.detect(elfBytes);
        assertCondition(res4.isDetected() && res4.getName().contains("ELF"), "ELF detection: " + res4.getName());

        byte[] unknownBytes = new byte[]{0x11, 0x22, 0x33, 0x44};
        FileSignatureDetector.DetectionResult res5 = FileSignatureDetector.detect(unknownBytes);
        assertCondition(!res5.isDetected() && res5.getName().contains("Unknown"), "Unknown detection: " + res5.getName());

        // 2. Test Document & String Extraction
        HexDocument doc = new HexDocument(classBytes, "Sample.class", "");
        assertCondition(doc.getLength() > 0, "Doc length > 0");
        List<HexDocument.ExtractedString> strings = doc.extractStrings(4);
        assertCondition(!strings.isEmpty(), "Extracted strings not empty: count=" + strings.size());

        // 3. Test Searching
        List<Integer> hexMatches = doc.searchHex("CA FE BA BE");
        assertCondition(!hexMatches.isEmpty() && hexMatches.get(0) == 0, "Hex search for CAFEBABE at offset 0");

        List<Integer> textMatches = doc.searchText("java/lang/Object", false);
        assertCondition(!textMatches.isEmpty(), "Text search for java/lang/Object");

        // 4. Test Entropy and Hashes
        double entropy = doc.calculateEntropy();
        assertCondition(entropy > 0.0 && entropy <= 8.0, "Entropy in valid range: " + entropy);

        HexDocument.HashInfo hashes = doc.computeHashes();
        assertCondition(hashes.md5 != null && hashes.md5.length() == 32, "MD5 hash length 32: " + hashes.md5);
        assertCondition(hashes.sha256 != null && hashes.sha256.length() == 64, "SHA-256 length 64: " + hashes.sha256);
        assertCondition(hashes.crc32 != 0, "CRC32 non-zero: " + hashes.crc32);

        // 5. Test Block Entropy for Heatmap
        double[] blocks = doc.computeBlockEntropy(16);
        assertCondition(blocks.length == 16, "Block entropy count == 16");

        // 6. Test In-Place Editing, Dirty tracking, Undo & Redo
        assertCondition(!doc.isModified(), "Initial doc not modified");
        byte origVal = doc.getByte(0);
        doc.setByte(0, (byte) 0xAA);
        assertCondition(doc.isModified(), "Doc marked modified after setByte");
        assertCondition(doc.isByteModified(0), "Byte 0 marked modified");
        assertCondition(doc.getByte(0) == (byte) 0xAA, "Byte 0 mutated to 0xAA");
        assertCondition(doc.canUndo(), "Doc canUndo() is true");

        doc.undo();
        assertCondition(!doc.isModified(), "Doc not modified after undo to original");
        assertCondition(doc.getByte(0) == origVal, "Byte 0 restored after undo");
        assertCondition(doc.canRedo(), "Doc canRedo() is true");

        doc.redo();
        assertCondition(doc.isModified(), "Doc modified after redo");
        assertCondition(doc.getByte(0) == (byte) 0xAA, "Byte 0 restored to 0xAA after redo");
        doc.undo(); // restore clean

        // 7. Test Binary Diff Engine
        HexDocument docDiff = new HexDocument(new byte[]{(byte) 0xCA, (byte) 0xFE, (byte) 0x00, (byte) 0x00}, "B.bin", "");
        BinaryDiffPanel diffPanel = new BinaryDiffPanel();
        diffPanel.setDocumentA(doc);

        // Headless components
        System.setProperty("java.awt.headless", "true");
        EntropyVisualizerPanel entropyPanel = new EntropyVisualizerPanel();
        entropyPanel.setDocument(doc);

        System.out.println("ALL COMPREHENSIVE TESTS PASSED SUCCESSFULLY!");
    }

    private static void assertCondition(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError("TEST FAILED: " + message);
        }
        System.out.println("  [PASS] " + message);
    }
}
