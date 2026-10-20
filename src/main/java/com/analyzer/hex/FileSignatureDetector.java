package com.analyzer.hex;

import java.util.ArrayList;
import java.util.List;

/**
 * Detects file formats using known magic bytes / byte signatures.
 */
public class FileSignatureDetector {

    public static class Signature {
        private final String name;
        private final String category;
        private final String mimeType;
        private final byte[] magicBytes;
        private final int offset; // Match offset, usually 0

        public Signature(String name, String category, String mimeType, int[] magicHex) {
            this(name, category, mimeType, 0, magicHex);
        }

        public Signature(String name, String category, String mimeType, int offset, int[] magicHex) {
            this.name = name;
            this.category = category;
            this.mimeType = mimeType;
            this.offset = offset;
            this.magicBytes = new byte[magicHex.length];
            for (int i = 0; i < magicHex.length; i++) {
                this.magicBytes[i] = (byte) (magicHex[i] & 0xFF);
            }
        }

        public String getName() {
            return name;
        }

        public String getCategory() {
            return category;
        }

        public String getMimeType() {
            return mimeType;
        }

        public byte[] getMagicBytes() {
            return magicBytes;
        }

        public int getOffset() {
            return offset;
        }

        public String getMagicHexFormatted() {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < magicBytes.length; i++) {
                if (i > 0) sb.append(" ");
                sb.append(String.format("%02X", magicBytes[i]));
            }
            return sb.toString();
        }

        public boolean matches(byte[] data) {
            if (data == null || data.length < offset + magicBytes.length) {
                return false;
            }
            for (int i = 0; i < magicBytes.length; i++) {
                if (data[offset + i] != magicBytes[i]) {
                    return false;
                }
            }
            return true;
        }
    }

    public static class DetectionResult {
        private final String name;
        private final String category;
        private final String mimeType;
        private final String matchedHex;
        private final boolean detected;

        public DetectionResult(String name, String category, String mimeType, String matchedHex, boolean detected) {
            this.name = name;
            this.category = category;
            this.mimeType = mimeType;
            this.matchedHex = matchedHex;
            this.detected = detected;
        }

        public static DetectionResult unknown(byte[] data) {
            String initialHex = "";
            if (data != null && data.length > 0) {
                int len = Math.min(data.length, 4);
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < len; i++) {
                    if (i > 0) sb.append(" ");
                    sb.append(String.format("%02X", data[i]));
                }
                initialHex = sb.toString();
            }
            return new DetectionResult("Unknown Binary", "Generic", "application/octet-stream", initialHex, false);
        }

        public String getName() {
            return name;
        }

        public String getCategory() {
            return category;
        }

        public String getMimeType() {
            return mimeType;
        }

        public String getMatchedHex() {
            return matchedHex;
        }

        public boolean isDetected() {
            return detected;
        }
    }

    private static final List<Signature> SIGNATURES = new ArrayList<>();

    static {
        // Core requested signatures
        SIGNATURES.add(new Signature("Java Class File (.class)", "JVM Executable", "application/java-vm",
                new int[]{0xCA, 0xFE, 0xBA, 0xBE}));
        SIGNATURES.add(new Signature("Java Serialized Object (.ser)", "JVM Data", "application/x-java-serialized-object",
                new int[]{0xAC, 0xED, 0x00, 0x05}));
        SIGNATURES.add(new Signature("ZIP / JAR Archive", "Archive", "application/zip",
                new int[]{0x50, 0x4B, 0x03, 0x04}));
        SIGNATURES.add(new Signature("ZIP Empty Archive", "Archive", "application/zip",
                new int[]{0x50, 0x4B, 0x05, 0x06}));
        SIGNATURES.add(new Signature("ZIP Spanned Archive", "Archive", "application/zip",
                new int[]{0x50, 0x4B, 0x07, 0x08}));

        // Native executables
        SIGNATURES.add(new Signature("ELF Executable / Shared Object", "Native Executable", "application/x-elf",
                new int[]{0x7F, 0x45, 0x4C, 0x46}));
        SIGNATURES.add(new Signature("DOS / Windows PE Executable (.exe, .dll)", "Native Executable", "application/x-dosexec",
                new int[]{0x4D, 0x5A}));
        SIGNATURES.add(new Signature("Mach-O Binary (32-bit LE)", "Native Executable", "application/x-mach-binary",
                new int[]{0xCE, 0xFA, 0xED, 0xFE}));
        SIGNATURES.add(new Signature("Mach-O Binary (64-bit LE)", "Native Executable", "application/x-mach-binary",
                new int[]{0xCF, 0xFA, 0xED, 0xFE}));
        SIGNATURES.add(new Signature("WebAssembly Binary (.wasm)", "Web Executable", "application/wasm",
                new int[]{0x00, 0x61, 0x73, 0x6D}));

        // Images
        SIGNATURES.add(new Signature("PNG Image (.png)", "Image", "image/png",
                new int[]{0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A}));
        SIGNATURES.add(new Signature("JPEG Image (.jpg)", "Image", "image/jpeg",
                new int[]{0xFF, 0xD8, 0xFF}));
        SIGNATURES.add(new Signature("GIF Image (.gif - GIF87a)", "Image", "image/gif",
                new int[]{0x47, 0x49, 0x46, 0x38, 0x37, 0x61}));
        SIGNATURES.add(new Signature("GIF Image (.gif - GIF89a)", "Image", "image/gif",
                new int[]{0x47, 0x49, 0x46, 0x38, 0x39, 0x61}));
        SIGNATURES.add(new Signature("BMP Image (.bmp)", "Image", "image/bmp",
                new int[]{0x42, 0x4D}));
        SIGNATURES.add(new Signature("TIFF Image (Little Endian)", "Image", "image/tiff",
                new int[]{0x49, 0x49, 0x2A, 0x00}));
        SIGNATURES.add(new Signature("TIFF Image (Big Endian)", "Image", "image/tiff",
                new int[]{0x4D, 0x4D, 0x00, 0x2A}));
        SIGNATURES.add(new Signature("WebP / RIFF Image", "Image", "image/webp",
                new int[]{0x52, 0x49, 0x46, 0x46}));

        // Documents & Databases
        SIGNATURES.add(new Signature("PDF Document (.pdf)", "Document", "application/pdf",
                new int[]{0x25, 0x50, 0x44, 0x46}));
        SIGNATURES.add(new Signature("SQLite 3 Database (.sqlite, .db)", "Database", "application/vnd.sqlite3",
                new int[]{0x53, 0x51, 0x4C, 0x69, 0x74, 0x65, 0x20, 0x66, 0x6F, 0x72, 0x6D, 0x61, 0x74, 0x20, 0x33, 0x00}));

        // Compression / Archives
        SIGNATURES.add(new Signature("RAR Archive (.rar v5)", "Archive", "application/x-rar-compressed",
                new int[]{0x52, 0x61, 0x72, 0x21, 0x1A, 0x07, 0x01, 0x00}));
        SIGNATURES.add(new Signature("RAR Archive (.rar v1.5-4)", "Archive", "application/x-rar-compressed",
                new int[]{0x52, 0x61, 0x72, 0x21, 0x1A, 0x07, 0x00}));
        SIGNATURES.add(new Signature("7-Zip Archive (.7z)", "Archive", "application/x-7z-compressed",
                new int[]{0x37, 0x7A, 0xBC, 0xAF, 0x27, 0x1C}));
        SIGNATURES.add(new Signature("GZIP Compressed File (.gz)", "Archive", "application/gzip",
                new int[]{0x1F, 0x8B, 0x08}));
        SIGNATURES.add(new Signature("BZIP2 Compressed File (.bz2)", "Archive", "application/x-bzip2",
                new int[]{0x42, 0x5A, 0x68}));
        SIGNATURES.add(new Signature("XZ Compressed File (.xz)", "Archive", "application/x-xz",
                new int[]{0xFD, 0x37, 0x7A, 0x58, 0x5A, 0x00}));
        SIGNATURES.add(new Signature("Zstandard Compressed File (.zst)", "Archive", "application/zstd",
                new int[]{0x28, 0xB5, 0x2F, 0xFD}));

        // Audio / Video
        SIGNATURES.add(new Signature("FLAC Audio (.flac)", "Audio", "audio/flac",
                new int[]{0x66, 0x4C, 0x61, 0x43}));
        SIGNATURES.add(new Signature("Ogg Vorbis / Opus (.ogg)", "Audio", "audio/ogg",
                new int[]{0x4F, 0x67, 0x67, 0x53}));
        SIGNATURES.add(new Signature("MP3 Audio with ID3v2", "Audio", "audio/mpeg",
                new int[]{0x49, 0x44, 0x33}));
        SIGNATURES.add(new Signature("Matroska / WebM Video (.mkv, .webm)", "Video", "video/x-matroska",
                new int[]{0x1A, 0x45, 0xDF, 0xA3}));
        SIGNATURES.add(new Signature("MP4 / QuickTime Video", "Video", "video/mp4", 4,
                new int[]{0x66, 0x74, 0x79, 0x70}));

        // Fonts
        SIGNATURES.add(new Signature("WOFF Font (.woff)", "Font", "font/woff",
                new int[]{0x77, 0x4F, 0x46, 0x46}));
        SIGNATURES.add(new Signature("WOFF2 Font (.woff2)", "Font", "font/woff2",
                new int[]{0x77, 0x4F, 0x46, 0x32}));
        SIGNATURES.add(new Signature("OpenType Font (.otf)", "Font", "font/otf",
                new int[]{0x4F, 0x54, 0x54, 0x4F}));

        // Text BOMs / Scripts
        SIGNATURES.add(new Signature("UTF-8 Text (with BOM)", "Text", "text/plain",
                new int[]{0xEF, 0xBB, 0xBF}));
        SIGNATURES.add(new Signature("UTF-16 Big Endian Text", "Text", "text/plain",
                new int[]{0xFE, 0xFF}));
        SIGNATURES.add(new Signature("UTF-16 Little Endian Text", "Text", "text/plain",
                new int[]{0xFF, 0xFE}));
        SIGNATURES.add(new Signature("Unix Script (Shebang #!)", "Script", "text/x-script",
                new int[]{0x23, 0x21}));
    }

    public static DetectionResult detect(byte[] data) {
        if (data == null || data.length == 0) {
            return new DetectionResult("Empty File", "None", "application/x-empty", "", false);
        }

        for (Signature sig : SIGNATURES) {
            if (sig.matches(data)) {
                return new DetectionResult(
                        sig.getName(),
                        sig.getCategory(),
                        sig.getMimeType(),
                        sig.getMagicHexFormatted(),
                        true
                );
            }
        }

        return DetectionResult.unknown(data);
    }
}
