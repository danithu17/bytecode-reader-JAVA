package com.analyzer.hex;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Generates sample binary files (Java .class, Java serialized object .ser, PNG image, ELF, etc.)
 * for instant one-click testing and demonstration.
 */
public class SampleGenerator {

    public static class SamplePerson implements Serializable {
        private static final long serialVersionUID = 123456789L;
        private final String username;
        private final String role;
        private final int accessLevel;
        private final Date createdDate;
        private final Map<String, String> attributes;

        public SamplePerson(String username, String role, int accessLevel) {
            this.username = username;
            this.role = role;
            this.accessLevel = accessLevel;
            this.createdDate = new Date();
            this.attributes = new HashMap<>();
            this.attributes.put("department", "Engineering");
            this.attributes.put("status", "ACTIVE");
            this.attributes.put("location", "San Francisco, CA");
        }
    }

    /**
     * Generates a valid serialized Java object starting with AC ED 00 05.
     */
    public static byte[] createSerializedObjectSample() {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            SamplePerson person = new SamplePerson("alice_developer", "Senior Architect", 100);
            oos.writeObject(person);
            oos.flush();
            return baos.toByteArray();
        } catch (Exception e) {
            // Fallback manual bytes starting with AC ED 00 05
            return new byte[]{(byte) 0xAC, (byte) 0xED, 0x00, 0x05, 0x73, 0x72, 0x00, 0x0C, 'S', 'a', 'm', 'p', 'l', 'e', 'D', 'a', 't', 'a'};
        }
    }

    /**
     * Generates a valid minimal Java bytecode .class file starting with CA FE BA BE.
     */
    public static byte[] createJavaClassSample() {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);

            // Magic: 0xCAFEBABE
            dos.writeInt(0xCAFEBABE);
            // Minor version: 0, Major version: 55 (Java 11)
            dos.writeShort(0);
            dos.writeShort(55);

            // Constant pool count: 8 entries (index 1..7)
            dos.writeShort(8);

            // 1: CONSTANT_Class (index 2)
            dos.writeByte(7);
            dos.writeShort(2);

            // 2: CONSTANT_Utf8 ("com/analyzer/SampleBytecode")
            dos.writeByte(1);
            dos.writeUTF("com/analyzer/SampleBytecode");

            // 3: CONSTANT_Class (index 4)
            dos.writeByte(7);
            dos.writeShort(4);

            // 4: CONSTANT_Utf8 ("java/lang/Object")
            dos.writeByte(1);
            dos.writeUTF("java/lang/Object");

            // 5: CONSTANT_Utf8 ("SourceFile")
            dos.writeByte(1);
            dos.writeUTF("SourceFile");

            // 6: CONSTANT_Utf8 ("SampleBytecode.java")
            dos.writeByte(1);
            dos.writeUTF("SampleBytecode.java");

            // 7: CONSTANT_Utf8 ("<init>")
            dos.writeByte(1);
            dos.writeUTF("<init>");

            // Access flags: ACC_PUBLIC | ACC_SUPER (0x0021)
            dos.writeShort(0x0021);
            // This class: 1
            dos.writeShort(1);
            // Super class: 3
            dos.writeShort(3);
            // Interfaces count: 0
            dos.writeShort(0);
            // Fields count: 0
            dos.writeShort(0);
            // Methods count: 0
            dos.writeShort(0);
            // Attributes count: 0
            dos.writeShort(0);

            dos.flush();
            return baos.toByteArray();
        } catch (Exception e) {
            return new byte[]{(byte) 0xCA, (byte) 0xFE, (byte) 0xBA, (byte) 0xBE, 0x00, 0x00, 0x00, 0x37};
        }
    }

    /**
     * Generates a sample PNG file starting with 89 50 4E 47 0D 0A 1A 0A.
     */
    public static byte[] createPngSample() {
        byte[] header = new byte[]{
                (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
                // IHDR length: 13
                0x00, 0x00, 0x00, 0x0D,
                // 'I', 'H', 'D', 'R'
                'I', 'H', 'D', 'R',
                // Width: 16, Height: 16
                0x00, 0x00, 0x00, 0x10,
                0x00, 0x00, 0x00, 0x10,
                // Bit depth 8, ColorType 2 (RGB), Compression 0, Filter 0, Interlace 0
                0x08, 0x02, 0x00, 0x00, 0x00,
                // CRC (dummy)
                (byte) 0x90, 0x77, (byte) 0x97, (byte) 0xBA,
                // tEXt chunk
                0x00, 0x00, 0x00, 0x18,
                't', 'E', 'X', 't',
                'A', 'u', 't', 'h', 'o', 'r', 0x00, 'H', 'e', 'x', 'A', 'n', 'a', 'l', 'y', 'z', 'e', 'r', 'A', 'p', 'p',
                0x00, 0x00, 0x00, 0x00,
                // IEND chunk
                0x00, 0x00, 0x00, 0x00,
                'I', 'E', 'N', 'D',
                (byte) 0xAE, 0x42, 0x60, (byte) 0x82
        };
        return header;
    }

    /**
     * Generates an ELF Linux binary header sample starting with 7F 45 4C 46.
     */
    public static byte[] createElfSample() {
        byte[] elf = new byte[128];
        // Magic
        elf[0] = 0x7F;
        elf[1] = 'E';
        elf[2] = 'L';
        elf[3] = 'F';
        elf[4] = 2; // 64-bit
        elf[5] = 1; // Little endian
        elf[6] = 1; // ELF version
        elf[7] = 0; // System V ABI
        // Fill string table simulation
        String str = "/lib64/ld-linux-x86-64.so.2\0libc.so.6\0printf\0puts\0__libc_start_main\0";
        byte[] strBytes = str.getBytes(StandardCharsets.US_ASCII);
        System.arraycopy(strBytes, 0, elf, 32, Math.min(strBytes.length, elf.length - 32));
        return elf;
    }
}
