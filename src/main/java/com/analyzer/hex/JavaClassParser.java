package com.analyzer.hex;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Robust Java Class File bytecode parser according to the JVM Specification (SE 8 - 24).
 * Parses magic (0xCAFEBABE), versions, Constant Pool, Access Flags, Class/Super Names,
 * Fields, Methods, and disassembles method bytecode instructions.
 */
public class JavaClassParser {

    public static class ConstantPoolItem {
        public final int index;
        public final String tag;
        public final String value;

        public ConstantPoolItem(int index, String tag, String value) {
            this.index = index;
            this.tag = tag;
            this.value = value;
        }
    }

    public static class FieldInfo {
        public final String accessFlags;
        public final String name;
        public final String descriptor;

        public FieldInfo(String accessFlags, String name, String descriptor) {
            this.accessFlags = accessFlags;
            this.name = name;
            this.descriptor = descriptor;
        }
    }

    public static class MethodInfo {
        public final String accessFlags;
        public final String name;
        public final String descriptor;
        public final List<String> disassembledInstructions;

        public MethodInfo(String accessFlags, String name, String descriptor, List<String> instructions) {
            this.accessFlags = accessFlags;
            this.name = name;
            this.descriptor = descriptor;
            this.disassembledInstructions = instructions != null ? instructions : new ArrayList<>();
        }
    }

    public static class ParsedClassFile {
        public boolean validClass = false;
        public String javaVersion = "Unknown";
        public int majorVersion;
        public int minorVersion;
        public String accessFlags = "";
        public String className = "";
        public String superClassName = "";
        public final List<String> interfaces = new ArrayList<>();
        public final List<ConstantPoolItem> constantPool = new ArrayList<>();
        public final List<FieldInfo> fields = new ArrayList<>();
        public final List<MethodInfo> methods = new ArrayList<>();
        public String errorMessage = "";
    }

    public static ParsedClassFile parse(byte[] data) {
        ParsedClassFile result = new ParsedClassFile();
        if (data == null || data.length < 10) {
            result.errorMessage = "File too small to be a valid Java class file.";
            return result;
        }

        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(data))) {
            int magic = in.readInt();
            if (magic != 0xCAFEBABE) {
                result.errorMessage = "Invalid magic bytes: 0x" + Integer.toHexString(magic).toUpperCase() + " (Expected 0xCAFEBABE)";
                return result;
            }

            result.minorVersion = in.readUnsignedShort();
            result.majorVersion = in.readUnsignedShort();
            result.javaVersion = getJavaVersionName(result.majorVersion);

            // Constant Pool
            int cpCount = in.readUnsignedShort();
            Object[] cpTags = new Object[cpCount];
            Object[] cpValues = new Object[cpCount];

            for (int i = 1; i < cpCount; i++) {
                int tag = in.readUnsignedByte();
                switch (tag) {
                    case 1: // CONSTANT_Utf8
                        String utf8 = in.readUTF();
                        cpTags[i] = "Utf8";
                        cpValues[i] = utf8;
                        result.constantPool.add(new ConstantPoolItem(i, "Utf8", utf8));
                        break;
                    case 3: // CONSTANT_Integer
                        int intVal = in.readInt();
                        cpTags[i] = "Integer";
                        cpValues[i] = intVal;
                        result.constantPool.add(new ConstantPoolItem(i, "Integer", String.valueOf(intVal)));
                        break;
                    case 4: // CONSTANT_Float
                        float fVal = in.readFloat();
                        cpTags[i] = "Float";
                        cpValues[i] = fVal;
                        result.constantPool.add(new ConstantPoolItem(i, "Float", String.valueOf(fVal)));
                        break;
                    case 5: // CONSTANT_Long
                        long lVal = in.readLong();
                        cpTags[i] = "Long";
                        cpValues[i] = lVal;
                        result.constantPool.add(new ConstantPoolItem(i, "Long", String.valueOf(lVal)));
                        i++; // Longs occupy two entries in CP
                        break;
                    case 6: // CONSTANT_Double
                        double dVal = in.readDouble();
                        cpTags[i] = "Double";
                        cpValues[i] = dVal;
                        result.constantPool.add(new ConstantPoolItem(i, "Double", String.valueOf(dVal)));
                        i++; // Doubles occupy two entries in CP
                        break;
                    case 7: // CONSTANT_Class
                        int nameIdx = in.readUnsignedShort();
                        cpTags[i] = "Class";
                        cpValues[i] = nameIdx;
                        result.constantPool.add(new ConstantPoolItem(i, "Class", "#" + nameIdx));
                        break;
                    case 8: // CONSTANT_String
                        int strIdx = in.readUnsignedShort();
                        cpTags[i] = "String";
                        cpValues[i] = strIdx;
                        result.constantPool.add(new ConstantPoolItem(i, "String", "#" + strIdx));
                        break;
                    case 9: // CONSTANT_Fieldref
                    case 10: // CONSTANT_Methodref
                    case 11: // CONSTANT_InterfaceMethodref
                        int cIdx = in.readUnsignedShort();
                        int ntIdx = in.readUnsignedShort();
                        String rTag = (tag == 9) ? "Fieldref" : (tag == 10 ? "Methodref" : "InterfaceMethodref");
                        cpTags[i] = rTag;
                        cpValues[i] = new int[]{cIdx, ntIdx};
                        result.constantPool.add(new ConstantPoolItem(i, rTag, "class #" + cIdx + ", nameAndType #" + ntIdx));
                        break;
                    case 12: // CONSTANT_NameAndType
                        int nIdx = in.readUnsignedShort();
                        int dIdx = in.readUnsignedShort();
                        cpTags[i] = "NameAndType";
                        cpValues[i] = new int[]{nIdx, dIdx};
                        result.constantPool.add(new ConstantPoolItem(i, "NameAndType", "name #" + nIdx + ", descriptor #" + dIdx));
                        break;
                    case 15: // CONSTANT_MethodHandle
                        int refKind = in.readUnsignedByte();
                        int refIdx = in.readUnsignedShort();
                        cpTags[i] = "MethodHandle";
                        result.constantPool.add(new ConstantPoolItem(i, "MethodHandle", "kind " + refKind + ", #" + refIdx));
                        break;
                    case 16: // CONSTANT_MethodType
                        int mtIdx = in.readUnsignedShort();
                        cpTags[i] = "MethodType";
                        result.constantPool.add(new ConstantPoolItem(i, "MethodType", "#" + mtIdx));
                        break;
                    case 17: // CONSTANT_Dynamic
                    case 18: // CONSTANT_InvokeDynamic
                        int bsmIdx = in.readUnsignedShort();
                        int dyntIdx = in.readUnsignedShort();
                        String dyTag = (tag == 17) ? "Dynamic" : "InvokeDynamic";
                        cpTags[i] = dyTag;
                        result.constantPool.add(new ConstantPoolItem(i, dyTag, "bootstrap #" + bsmIdx + ", nameAndType #" + dyntIdx));
                        break;
                    case 19: // CONSTANT_Module
                    case 20: // CONSTANT_Package
                        int mpIdx = in.readUnsignedShort();
                        result.constantPool.add(new ConstantPoolItem(i, tag == 19 ? "Module" : "Package", "#" + mpIdx));
                        break;
                    default:
                        // Unknown tag, break out safely
                        break;
                }
            }

            // Access Flags
            int access = in.readUnsignedShort();
            result.accessFlags = decodeAccessFlags(access, true);

            // This Class
            int thisClassIdx = in.readUnsignedShort();
            result.className = resolveClassName(thisClassIdx, cpTags, cpValues);

            // Super Class
            int superClassIdx = in.readUnsignedShort();
            result.superClassName = resolveClassName(superClassIdx, cpTags, cpValues);

            // Interfaces
            int ifaceCount = in.readUnsignedShort();
            for (int i = 0; i < ifaceCount; i++) {
                int ifaceIdx = in.readUnsignedShort();
                result.interfaces.add(resolveClassName(ifaceIdx, cpTags, cpValues));
            }

            // Fields
            int fieldCount = in.readUnsignedShort();
            for (int i = 0; i < fieldCount; i++) {
                int fAccess = in.readUnsignedShort();
                int fNameIdx = in.readUnsignedShort();
                int fDescIdx = in.readUnsignedShort();
                String fName = resolveUtf8(fNameIdx, cpValues);
                String fDesc = resolveUtf8(fDescIdx, cpValues);
                result.fields.add(new FieldInfo(decodeAccessFlags(fAccess, false), fName, fDesc));

                // Skip attributes
                int attrCount = in.readUnsignedShort();
                for (int a = 0; a < attrCount; a++) {
                    in.readUnsignedShort(); // attr name
                    int attrLen = in.readInt();
                    in.skipBytes(attrLen);
                }
            }

            // Methods & Bytecode Disassembly
            int methodCount = in.readUnsignedShort();
            for (int i = 0; i < methodCount; i++) {
                int mAccess = in.readUnsignedShort();
                int mNameIdx = in.readUnsignedShort();
                int mDescIdx = in.readUnsignedShort();
                String mName = resolveUtf8(mNameIdx, cpValues);
                String mDesc = resolveUtf8(mDescIdx, cpValues);
                List<String> instructions = new ArrayList<>();

                int attrCount = in.readUnsignedShort();
                for (int a = 0; a < attrCount; a++) {
                    int attrNameIdx = in.readUnsignedShort();
                    int attrLen = in.readInt();
                    String attrName = resolveUtf8(attrNameIdx, cpValues);

                    if ("Code".equals(attrName)) {
                        int maxStack = in.readUnsignedShort();
                        int maxLocals = in.readUnsignedShort();
                        int codeLen = in.readInt();
                        byte[] codeBytes = new byte[codeLen];
                        in.readFully(codeBytes);

                        instructions.add(String.format("// Max Stack: %d, Max Locals: %d, Code Length: %d bytes", maxStack, maxLocals, codeLen));
                        instructions.addAll(disassembleCode(codeBytes));

                        // Skip exception table & sub attributes
                        int excLen = in.readUnsignedShort();
                        in.skipBytes(excLen * 8);
                        int subAttrCount = in.readUnsignedShort();
                        for (int sa = 0; sa < subAttrCount; sa++) {
                            in.readUnsignedShort();
                            int sLen = in.readInt();
                            in.skipBytes(sLen);
                        }
                    } else {
                        in.skipBytes(attrLen);
                    }
                }
                result.methods.add(new MethodInfo(decodeAccessFlags(mAccess, false), mName, mDesc, instructions));
            }

            result.validClass = true;
        } catch (Exception e) {
            result.errorMessage = "Parsing note: " + e.getMessage();
        }

        return result;
    }

    private static String resolveClassName(int classIdx, Object[] tags, Object[] values) {
        if (classIdx <= 0 || classIdx >= values.length || values[classIdx] == null) return "None";
        if (values[classIdx] instanceof Integer) {
            int nameIdx = (Integer) values[classIdx];
            return resolveUtf8(nameIdx, values);
        }
        return "#" + classIdx;
    }

    private static String resolveUtf8(int utf8Idx, Object[] values) {
        if (utf8Idx <= 0 || utf8Idx >= values.length || values[utf8Idx] == null) return "";
        return String.valueOf(values[utf8Idx]);
    }

    private static String decodeAccessFlags(int flags, boolean isClass) {
        StringBuilder sb = new StringBuilder();
        if ((flags & 0x0001) != 0) sb.append("public ");
        if ((flags & 0x0002) != 0) sb.append("private ");
        if ((flags & 0x0004) != 0) sb.append("protected ");
        if ((flags & 0x0008) != 0) sb.append("static ");
        if ((flags & 0x0010) != 0) sb.append("final ");
        if ((flags & 0x0020) != 0) sb.append(isClass ? "super " : "synchronized ");
        if ((flags & 0x0200) != 0) sb.append("interface ");
        if ((flags & 0x0400) != 0) sb.append("abstract ");
        if ((flags & 0x1000) != 0) sb.append("synthetic ");
        if ((flags & 0x2000) != 0) sb.append("annotation ");
        if ((flags & 0x4000) != 0) sb.append("enum ");
        return sb.toString().trim();
    }

    private static String getJavaVersionName(int major) {
        switch (major) {
            case 45: return "Java 1.1 (45.3)";
            case 46: return "Java 1.2 (46.0)";
            case 47: return "Java 1.3 (47.0)";
            case 48: return "Java 1.4 (48.0)";
            case 49: return "Java 5 (49.0)";
            case 50: return "Java 6 (50.0)";
            case 51: return "Java 7 (51.0)";
            case 52: return "Java 8 (52.0)";
            case 53: return "Java 9 (53.0)";
            case 54: return "Java 10 (54.0)";
            case 55: return "Java 11 LTS (55.0)";
            case 56: return "Java 12 (56.0)";
            case 57: return "Java 13 (57.0)";
            case 58: return "Java 14 (58.0)";
            case 59: return "Java 15 (59.0)";
            case 60: return "Java 16 (60.0)";
            case 61: return "Java 17 LTS (61.0)";
            case 62: return "Java 18 (62.0)";
            case 63: return "Java 19 (63.0)";
            case 64: return "Java 20 (64.0)";
            case 65: return "Java 21 LTS (65.0)";
            case 66: return "Java 22 (66.0)";
            case 67: return "Java 23 (67.0)";
            case 68: return "Java 24 (68.0)";
            default: return "Java (Major " + major + ")";
        }
    }

    /**
     * Disassembles raw bytecode instructions into human-readable assembly lines with offsets.
     */
    public static List<String> disassembleCode(byte[] code) {
        List<String> list = new ArrayList<>();
        if (code == null || code.length == 0) return list;

        int pc = 0;
        while (pc < code.length) {
            int op = code[pc] & 0xFF;
            int startPc = pc;
            String mnemonic = getOpcodeMnemonic(op);

            int opLength = getOpcodeLength(op, code, pc);
            StringBuilder operands = new StringBuilder();

            if (opLength == 2 && pc + 1 < code.length) {
                operands.append(" ").append(code[pc + 1] & 0xFF);
            } else if (opLength == 3 && pc + 2 < code.length) {
                int val = ((code[pc + 1] & 0xFF) << 8) | (code[pc + 2] & 0xFF);
                operands.append(" #").append(val);
            }

            list.add(String.format("  %04d:  %-18s%s", startPc, mnemonic, operands));
            pc += Math.max(1, opLength);
        }
        return list;
    }

    private static int getOpcodeLength(int op, byte[] code, int pc) {
        switch (op) {
            case 0x10: case 0x12: case 0x15: case 0x16: case 0x17: case 0x18:
            case 0x19: case 0x36: case 0x37: case 0x38: case 0x39: case 0x3A:
            case 0xBC: case 0xA9:
                return 2;
            case 0x11: case 0x13: case 0x14: case 0x99: case 0x9A: case 0x9B:
            case 0x9C: case 0x9D: case 0x9E: case 0x9F: case 0xA0: case 0xA1:
            case 0xA2: case 0xA3: case 0xA4: case 0xA5: case 0xA6: case 0xA7:
            case 0xA8: case 0xB2: case 0xB3: case 0xB4: case 0xB5: case 0xB6:
            case 0xB7: case 0xB8: case 0xBB: case 0xBD: case 0xC0: case 0xC1:
            case 0xC6: case 0xC7:
                return 3;
            case 0x84: // iinc
                return 3;
            case 0xB9: // invokeinterface
            case 0xBA: // invokedynamic
                return 5;
            default:
                return 1;
        }
    }

    private static String getOpcodeMnemonic(int op) {
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
            case 0x0D: return "fconst_2";
            case 0x0E: return "dconst_0";
            case 0x0F: return "dconst_1";
            case 0x10: return "bipush";
            case 0x11: return "sipush";
            case 0x12: return "ldc";
            case 0x13: return "ldc_w";
            case 0x14: return "ldc2_w";
            case 0x15: return "iload";
            case 0x16: return "lload";
            case 0x17: return "fload";
            case 0x18: return "dload";
            case 0x19: return "aload";
            case 0x1A: return "iload_0";
            case 0x1B: return "iload_1";
            case 0x1C: return "iload_2";
            case 0x1D: return "iload_3";
            case 0x1E: return "lload_0";
            case 0x1F: return "lload_1";
            case 0x20: return "lload_2";
            case 0x21: return "lload_3";
            case 0x22: return "fload_0";
            case 0x23: return "fload_1";
            case 0x24: return "fload_2";
            case 0x25: return "fload_3";
            case 0x26: return "dload_0";
            case 0x27: return "dload_1";
            case 0x28: return "dload_2";
            case 0x29: return "dload_3";
            case 0x2A: return "aload_0";
            case 0x2B: return "aload_1";
            case 0x2C: return "aload_2";
            case 0x2D: return "aload_3";
            case 0x2E: return "iaload";
            case 0x2F: return "laload";
            case 0x30: return "faload";
            case 0x31: return "daload";
            case 0x32: return "aaload";
            case 0x33: return "baload";
            case 0x34: return "caload";
            case 0x35: return "saload";
            case 0x36: return "istore";
            case 0x37: return "lstore";
            case 0x38: return "fstore";
            case 0x39: return "dstore";
            case 0x3A: return "astore";
            case 0x3B: return "istore_0";
            case 0x3C: return "istore_1";
            case 0x3D: return "istore_2";
            case 0x3E: return "istore_3";
            case 0x3F: return "lstore_0";
            case 0x40: return "lstore_1";
            case 0x41: return "lstore_2";
            case 0x42: return "lstore_3";
            case 0x43: return "fstore_0";
            case 0x44: return "fstore_1";
            case 0x45: return "fstore_2";
            case 0x46: return "fstore_3";
            case 0x47: return "dstore_0";
            case 0x48: return "dstore_1";
            case 0x49: return "dstore_2";
            case 0x4A: return "dstore_3";
            case 0x4B: return "astore_0";
            case 0x4C: return "astore_1";
            case 0x4D: return "astore_2";
            case 0x4E: return "astore_3";
            case 0x57: return "pop";
            case 0x58: return "pop2";
            case 0x59: return "dup";
            case 0x60: return "iadd";
            case 0x61: return "ladd";
            case 0x62: return "fadd";
            case 0x63: return "dadd";
            case 0x64: return "isub";
            case 0x65: return "lsub";
            case 0x66: return "fsub";
            case 0x67: return "dsub";
            case 0x68: return "imul";
            case 0x69: return "lmul";
            case 0x6A: return "fmul";
            case 0x6B: return "dmul";
            case 0x6C: return "idiv";
            case 0x6D: return "ldiv";
            case 0x6E: return "fdiv";
            case 0x6F: return "ddiv";
            case 0x99: return "ifeq";
            case 0x9A: return "ifne";
            case 0x9B: return "iflt";
            case 0x9C: return "ifge";
            case 0x9D: return "ifgt";
            case 0x9E: return "ifle";
            case 0x9F: return "if_icmpeq";
            case 0xA0: return "if_icmpne";
            case 0xA1: return "if_icmplt";
            case 0xA2: return "if_icmpge";
            case 0xA3: return "if_icmpgt";
            case 0xA4: return "if_icmple";
            case 0xA7: return "goto";
            case 0xAC: return "ireturn";
            case 0xAD: return "lreturn";
            case 0xAE: return "freturn";
            case 0xAF: return "dreturn";
            case 0xB0: return "areturn";
            case 0xB1: return "return";
            case 0xB2: return "getstatic";
            case 0xB3: return "putstatic";
            case 0xB4: return "getfield";
            case 0xB5: return "putfield";
            case 0xB6: return "invokevirtual";
            case 0xB7: return "invokespecial";
            case 0xB8: return "invokestatic";
            case 0xB9: return "invokeinterface";
            case 0xBA: return "invokedynamic";
            case 0xBB: return "new";
            case 0xBC: return "newarray";
            case 0xBD: return "anewarray";
            case 0xBE: return "arraylength";
            case 0xBF: return "athrow";
            case 0xC0: return "checkcast";
            case 0xC1: return "instanceof";
            default: return "opcode_0x" + String.format("%02X", op);
        }
    }
}
