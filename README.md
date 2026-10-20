# 🔮 HexAnalyzer: Modern Desktop Binary & Hex File Analyzer

<p align="center">
  <img src="https://img.shields.io/badge/Java-11%20%7C%2017%20%7C%2021%20%7C%2024-007396?style=for-the-badge&logo=openjdk&logoColor=white" alt="Java Version" />
  <img src="https://img.shields.io/badge/UI-FlatLaf%20Dark-24272E?style=for-the-badge&logo=java&logoColor=white" alt="FlatLaf" />
  <img src="https://img.shields.io/badge/Platform-Windows%20(Native%20.exe)-0078D7?style=for-the-badge&logo=windows&logoColor=white" alt="Windows Native" />
  <img src="https://img.shields.io/badge/Build-Maven%20%7C%20Gradle-02569B?style=for-the-badge&logo=apache-maven&logoColor=white" alt="Build" />
  <img src="https://img.shields.io/badge/License-MIT-green?style=for-the-badge" alt="License" />
</p>

A complete, high-performance, dark-themed Desktop Binary and Hexadecimal File Analyzer built in Java using **Swing** and the **FlatLaf (`FlatDarkLaf`)** modern look-and-feel library. Designed for software engineers, reverse engineers, security researchers, and malware analysts who need an intuitive, responsive, and feature-rich inspection tool.

---

## 📑 Table of Contents

- [Key Features](#-key-features)
- [Application Architecture & UI Layout](#-application-architecture--ui-layout)
- [Executable & Distribution Options](#-executable--distribution-options)
- [Quick Start Guide](#-quick-start-guide)
- [Detailed Feature Breakdown](#-detailed-feature-breakdown)
  - [1. File Loader & Drag-and-Drop](#1-file-loader--drag-and-drop)
  - [2. Three-Column Hex & Binary Viewer](#2-three-column-hex--binary-viewer)
  - [3. In-Place Byte Editing & Patching](#3-in-place-byte-editing--patching)
  - [4. Smart File Signature Detector (Magic Bytes)](#4-smart-file-signature-detector-magic-bytes)
  - [5. Dynamic Data Inspector Side-Panel](#5-dynamic-data-inspector-side-panel)
  - [6. Visual Entropy Heatmap & Histogram](#6-visual-entropy-heatmap--histogram)
  - [7. Binary Diff & File Comparison](#7-binary-diff--file-comparison)
  - [8. Readable String Extractor](#8-readable-string-extractor)
  - [9. Code & Snippet Exporter](#9-code--snippet-exporter)
  - [10. Search & Pattern Highlighting](#10-search--pattern-highlighting)
- [Keyboard Shortcuts Cheatsheet](#-keyboard-shortcuts-cheatsheet)
- [Building from Source](#-building-from-source)
- [Project File Structure](#-project-file-structure)
- [Technical Specifications & Formulas](#-technical-specifications--formulas)
- [License](#-license)

---

## 🌟 Key Features

* **⚡ Ultra-Fast Virtualized Scrolling:** Effortlessly scrolls through large binaries with zero lag using O(1) viewport row calculations.
* **✍️ In-Place Interactive Editing:** Type hex digits or ASCII text directly to patch binaries; tracks dirty bytes in real-time with multi-level Undo/Redo (`Ctrl+Z` / `Ctrl+Y`).
* **🔮 Smart Magic Bytes Detector:** Automatically matches headers against an extensive dictionary of binary formats (Java `.class`, Java Serialized `.ser`, PE/EXE, ELF, PNG, JPEG, PDF, SQLite, WASM, and more).
* **🔬 Comprehensive Data Inspector:** Decodes selection into 8-bit Binary, Signed/Unsigned Int8, Int16, Int32, Int64, IEEE 754 Float/Double (Little & Big Endian), JVM Opcodes, and dynamic CRC32/Adler32 checksums.
* **📊 Visual Entropy Heatmap Strip:** Identifies code blocks, plain text, compressed sections, and encrypted payloads via interactive Shannon Entropy mapping.
* **📶 256-Byte Histogram:** Visualizes byte frequency distribution from `0x00` to `0xFF` with hover tooltips and metrics.
* **⚖️ Binary Diff & Side-by-Side Compare:** Spot byte modifications, insertions, and deletions between two files with similarity scoring and quick navigation.
* **📜 String Extractor:** Scans and collects readable ASCII/UTF-8 strings (minimum length filter, real-time filtering, export to text, double-click jump).
* **📋 Multi-Language Snippet Exporter:** Export selections as C/C++, Java, Python byte arrays, Base64, raw hex, or binary files.
* **🪟 Native Windows `.exe` Included:** Run directly via the 25 KB native launcher or the zero-dependency self-contained portable distribution.

---

## 🖥 Application Architecture & UI Layout

```
+---------------------------------------------------------------------------------------------------------+
|  [Open File]  [Load Sample v]  SampleBytecode.class *         [Java Class File (.class)]  [Hex Mode v]  |
|  [Undo] [Redo] [Save] [Export Snippet] [Edit: ON]            [Go to Offset]  [Search]                   |
+---------------------------------------------------------------------------------------------------------+
| [Search: Text | Hex ] [ Query Input                  ] [x] Match Case  [Find All] [< Prev] [Next >] [X] |
+-------------------------------------------------------------------+-------------------------------------+
| Tabs: [Hex/Binary View] [Strings] [Entropy] [Diff] [Overview]     |  DATA INSPECTOR                     |
+-------------------------------------------------------------------+-------------------------------------+
| OFFSET:   HEXADECIMAL BYTES:           ASCII DECODED:             |  Offset:    0x00000000 (0)          |
| 00000000: CA FE BA BE  00 00 00 37     ......7                    |  Selection: 4 bytes                 |
| 00000010: 00 08 07 00  02 01 00 1B     ........                   |  [ ] Show Little-Endian first       |
| 00000020: 63 6F 6D 2F  61 6E 61 6C     com/anal                   +-------------------------------------+
| 00000030: 79 7A 65 72  2F 53 61 6D     yzer/Sam                   | Type         | Big-End   | Lit-End  |
| 00000040: 70 6C 65 42  79 74 65 63     pleBytec                   | Binary       | 1100 1010 | 1100 1010|
| 00000050: 6F 64 65 07  00 04 01 00     ode.....                   | Char         | '.' (0xCA)| '.'      |
| 00000060: 10 6A 61 76  61 2F 6C 61     .java/la                   | Byte (Int8)  | -54       | -54      |
| 00000070: 6E 67 2F 4F  62 6A 65 63     ng/Objec                   | Unsigned Byte| 202       | 202      |
| 00000080: 74 01 00 0A  53 6F 75 72     t...Sour                   | Short (Int16)| -1282     | -254     |
| 00000090: 63 65 46 69  6C 65 01 00     ceFile..                   | Int (Int32)  |-889275714 | -109504...|
| 000000A0: 12 53 61 6D  70 6C 65 42     .SampleB                   | Float (32)   | -5.84e-37 | 3.32e-40 |
|                                                                   | JVM Opcode   | byte 0xCA | 0xCA     |
|                                                                   | Sel. CRC32   | 0x36D3A0A7| 919838887|
+-------------------------------------------------------------------+-------------------------------------+
| Offset: 0x00000000 (0) | Selection: 4 bytes | Modified: 0 bytes   | Size: 247 bytes | Mode: HEX (16 B/r)|
+---------------------------------------------------------------------------------------------------------+
```

---

## 📦 Executable & Distribution Options

This repository includes **two pre-compiled Windows execution targets**:

| Executable Target | Path | Size | Description |
|---|---|---|---|
| **Native GUI Launcher** | [`HexAnalyzer.exe`](file:///c:/vibe/New%20folder/HexAnalyzer.exe) | ~25 KB | 64-bit native Windows GUI executable with embedded icon. Automatically locates Java (local JRE, `JAVA_HOME`, registry, or PATH) and runs silently with no console window. Accepts file arguments and drag-and-drop. |
| **Self-Contained Portable App** | [`dist\HexAnalyzer\HexAnalyzer.exe`](file:///c:/vibe/New%20folder/dist/HexAnalyzer/HexAnalyzer.exe) | ~40 MB | Standalone portable bundle generated with `jpackage`. Contains an embedded stripped Java runtime—**runs on any Windows computer with zero Java installed!** |
| **Standalone Fat JAR** | [`HexAnalyzer.jar`](file:///c:/vibe/New%20folder/HexAnalyzer.jar) | ~1.3 MB | Cross-platform standalone JAR containing the full application and bundled FlatLaf library. |

---

## ⚡ Quick Start Guide

### Running on Windows:
1. **Option A (Instant Native Launcher):** Double-click `HexAnalyzer.exe`.
2. **Option B (Zero-Dependency Portable Bundle):** Double-click `dist\HexAnalyzer\HexAnalyzer.exe`.
3. **Option C (Batch Script):** Run `run.bat`.

### Running on Any OS (macOS, Linux, Windows):
```bash
java -jar HexAnalyzer.jar
# or specify a file to open immediately:
java -jar HexAnalyzer.jar /path/to/binary.bin
```

---

## 🔍 Detailed Feature Breakdown

### 1. File Loader & Drag-and-Drop
- **Native File Chooser:** Click **"Open File"** (`Ctrl+O`) to load files with standard dialog filtering (`*.*`, `.bin`, `.dat`, `.class`, `.ser`, `.exe`, `.elf`, etc.).
- **Drag-and-Drop:** Drag any file directly from Windows Explorer and drop it anywhere over the application window to open it instantly.
- **Built-in Sample Generator:** Click **"Load Sample"** to generate live, syntactically valid in-memory samples without needing external files:
  - Java Bytecode Class (`SampleBytecode.class` with `CA FE BA BE`)
  - Java Serialized Object (`UserData.ser` with `AC ED 00 05`)
  - PNG Image (`IconSample.png` with `89 50 4E 47...`)
  - ELF Linux Executable (`SampleBinary.elf` with `7F 45 4C 46...`)

---

### 2. Three-Column Hex & Binary Viewer
- **Column 1: Hex Offset:** Standard 8-digit hexadecimal address (`00000000:`, `00000010:`, etc.).
- **Column 2: Byte Representation:**
  - **Hex Mode:** 16 bytes per row formatted as `XX XX XX XX XX XX XX XX  XX XX XX XX XX XX XX XX` with an extra central gap after byte 7 for optimal human readability.
  - **Binary Mode:** Toggle to 8-bit binary strings (e.g. `11001010`) with 8 bytes per row for clear bit-level inspection.
- **Column 3: ASCII / UTF-8 Translation:** Printable characters ($32 \le \text{byte} \le 126$) are displayed as text; non-printable bytes are rendered as muted dots (`.`).
- **Synchronized Cursor & Spans:** Selecting or dragging across bytes in the hex column highlights the exact corresponding ASCII text simultaneously, and vice versa.
- **High-Performance Virtual Scrolling:** Renders only visible rows based on the viewport clip bounds, ensuring fluid 60+ FPS navigation on large files.

---

### 3. In-Place Byte Editing & Patching
- **Real-Time Mutation:**
  - In **Hex Mode**: Type hex digits (`0`–`9`, `a`–`f`) to modify bytes; provides real-time nibble guidance (`A_`).
  - In **ASCII Mode**: Click into the text column and type printable characters to overwrite bytes directly.
- **Visual Modification Markers:** Altered bytes are highlighted in warm amber/orange, and the window title displays an asterisk (`* (Modified)`).
- **Undo / Redo Stack:** Press `Ctrl+Z` to undo any change and `Ctrl+Y` to redo.
- **Save Back to Disk:**
  - `Ctrl+S`: Saves changes directly to the opened file.
  - `Ctrl+Shift+S`: Prompts for a new destination path to save a patched copy.
- **Edit Lock Mode:** Toggle the **"Edit: ON / OFF"** button to lock the document and prevent accidental edits.

---

### 4. Smart File Signature Detector (Magic Bytes)
Automatically inspects the initial byte headers and renders a stylish badge chip at the top:

| Magic Hex Bytes | Format Identified | Category |
|---|---|---|
| `CA FE BA BE` | **Java Class File (.class)** | JVM Executable |
| `AC ED 00 05` | **Java Serialized Object (.ser)** | JVM Data |
| `50 4B 03 04` | **ZIP / JAR / APK / DOCX / XLSX** | Archive |
| `7F 45 4C 46` | **ELF Executable / Shared Object** | Native Linux Binary |
| `4D 5A` | **DOS / Windows PE (.exe, .dll)** | Native Windows Binary |
| `89 50 4E 47 0D 0A 1A 0A` | **PNG Image (.png)** | Image |
| `FF D8 FF` | **JPEG Image (.jpg)** | Image |
| `47 49 46 38` | **GIF Image (.gif)** | Image |
| `25 50 44 46` | **PDF Document (.pdf)** | Document |
| `53 51 4C 69 74 65...` | **SQLite 3 Database (.sqlite, .db)** | Database |
| `00 61 73 6D` | **WebAssembly Binary (.wasm)** | Web Executable |
| `37 7A BC AF 27 1C` | **7-Zip Archive (.7z)** | Compressed Archive |
| `1F 8B 08` | **GZIP Compressed File (.gz)** | Compressed Archive |
| `52 61 72 21` | **RAR Archive (.rar)** | Compressed Archive |
| `42 4D` | **BMP Bitmap Image (.bmp)** | Image |
| `49 44 33` | **MP3 Audio with ID3v2** | Audio |
| `66 4C 61 43` | **FLAC Audio (.flac)** | Audio |
| `4F 67 67 53` | **Ogg Vorbis / Opus Container** | Audio/Video |
| `1A 45 DF A3` | **Matroska / WebM (.mkv, .webm)** | Video |
| `77 4F 46 46` | **WOFF Font (.woff)** | Web Font |
| `EF BB BF` | **UTF-8 Text (with BOM)** | Text Document |

---

### 5. Dynamic Data Inspector Side-Panel
Clicking any byte or selecting a range updates the inspector panel instantly:

- **8-bit Binary String:** Formatted with nibble separation (`1100 1010`).
- **Character:** Printable character with ASCII code (`'.' (0xCA)`).
- **Int8:** Signed ($-128$ to $127$) and Unsigned ($0$ to $255$).
- **Int16 (Short):** Signed ($-32,768$ to $32,767$) and Unsigned ($0$ to $65,535$) in Big-Endian & Little-Endian.
- **Int32 (Integer):** Signed and Unsigned ($0$ to $4,294,967,295$) in Big-Endian & Little-Endian.
- **Int64 (Long):** 64-bit signed integer in Big-Endian & Little-Endian.
- **Float (32-bit):** IEEE 754 single-precision floating point.
- **Double (64-bit):** IEEE 754 double-precision floating point.
- **JVM Opcode Disassembly:** Decodes byte instructions (e.g. `nop`, `aload_0`, `invokespecial`, `bipush`, `return`).
- **Range Checksums:** Dynamically calculates **CRC32** and **Adler32** checksums across any selected byte range.
- **One-Click Copy:** Double-click any row or click **"Copy Value"** to copy to clipboard.

---

### 6. Visual Entropy Heatmap & Histogram
Located in the **"Visual Entropy & Distribution"** tab:

1. **Interactive Block Entropy Heatmap Strip:**
   - Divides the binary data into configurable blocks ($64, 128, 256, 512, 1024$).
   - Calculates the local Shannon entropy for each block:
     $$H = -\sum_{i=0}^{255} p_i \log_2(p_i)$$
   - **Color Spectrum:**
     - 🟦 **Deep Blue / Cyan ($< 3.0$):** Repetitive bytes, zero-padding, tables.
     - 🟩 **Emerald Green ($3.0 - 5.5$):** Standard code, compiled bytecode, ASCII text.
     - 🟥 **Crimson / Red ($> 7.0$):** High entropy (compressed archives, encrypted data, packed obfuscation).
   - **Interactive Navigation:** Clicking any block on the heatmap jumps the Hex Viewer directly to that byte offset.
2. **256-Byte Frequency Distribution Bar Chart:**
   - Displays occurrence bars for each byte value from `0x00` through `0xFF`.
   - Highlighted categories: `0x00` Null bytes (Blue), `0x20`–`0x7E` ASCII characters (Green), `0x80`–`0xFF` high bytes (Orange).
   - Hovering over any bar shows exact count, character representation, and percentage of the total file.

---

### 7. Binary Diff & File Comparison
Located in the **"Binary Diff & Compare"** tab:

- Loads the current base file (File A) and allows selecting a secondary comparison file (File B).
- Performs a byte-by-byte alignment comparison:
  - Total mismatched bytes count.
  - Length delta (bytes added or missing in File B).
  - Overall file similarity percentage ($\%$).
- Filterable difference table displaying:
  - `Offset (Hex & Dec)`
  - `File A Value`
  - `File B Value`
  - `Diff Type (Modified, Missing in B, Added in B)`
- Double-clicking any difference or using the **"Next Diff" / "Prev Diff"** buttons scrolls the Hex Viewer directly to the mismatched offset.

---

### 8. Readable String Extractor
Located in the **"Extracted Strings"** tab:

- Scans the binary file and extracts all sequential printable ASCII / UTF-8 character sequences.
- Configurable minimum length filter (default: 4, adjustable from 2 to 128).
- Real-time search filter field (`TableRowSorter`).
- Double-clicking any string jumps directly to that string's address in the Hex Viewer.
- **"Export to .txt"** button exports all extracted strings with their hex/dec offsets to a text file.

---

### 9. Code & Snippet Exporter
Press `Ctrl+E` or click **"Export Snippet"** to convert the selected byte span (or entire file) into ready-to-paste source code:

* **C / C++ Array:**
  ```c
  // 4 bytes
  const unsigned char data[4] = {
      0xCA, 0xFE, 0xBA, 0xBE
  };
  ```
* **Java Array:**
  ```java
  // 4 bytes
  byte[] data = new byte[] {
      (byte) 0xCA, (byte) 0xFE, (byte) 0xBA, (byte) 0xBE
  };
  ```
* **Python Bytes:**
  ```python
  data = b"\xca\xfe\xba\xbe"
  ```
* **Raw Hex Stream:** `CA FE BA BE`
* **0x Hex List:** `0xCA, 0xFE, 0xBA, 0xBE`
* **Base64 String:** `yv66vg==`
* **Binary File:** Saves raw byte slice directly to a `.bin` file.

---

### 10. Search & Pattern Highlighting
- Press `Ctrl+F` to open the collapsible search bar.
- **Search Modes:**
  - **Text Mode:** Search for ASCII / UTF-8 strings (supports case-sensitive toggle).
  - **Hex Mode:** Search for hex sequences (e.g., `CA FE BA BE` or `cafe`).
- **Highlights:** Highlights all matching occurrences across the file in translucent gold/orange.
- **Navigation:** Navigate matches using `F3` (Next) and `Shift + F3` (Previous) with live status badge (`Match 2 of 14`).

---

## ⌨️ Keyboard Shortcuts Cheatsheet

| Shortcut | Action | Description |
|---|---|---|
| `Ctrl + O` | Open File | Opens file chooser dialog |
| `Ctrl + S` | Save File | Writes modifications directly to the active file |
| `Ctrl + Shift + S` | Save File As... | Prompts destination path to save a patched copy |
| `Ctrl + Z` | Undo | Undoes the last byte edit |
| `Ctrl + Y` | Redo | Redoes the previously undone edit |
| `Ctrl + E` | Export Snippet | Opens snippet exporter dialog for selected bytes |
| `Ctrl + F` | Toggle Search | Shows / hides the integrated search toolbar |
| `F3` | Find Next | Jumps to the next search match |
| `Shift + F3` | Find Previous | Jumps to the previous search match |
| `Ctrl + G` | Go to Offset | Prompts for a hex (`0x...`) or decimal offset to jump to |
| `Ctrl + B` | Toggle View Mode | Switches between Hex view and 8-bit Binary view |
| `Arrow Keys` | Move Cursor | Moves cursor 1 byte or 1 row in the viewer |
| `Shift + Arrows`| Range Selection | Expands or contracts the byte selection range |
| `Page Up / Down`| Page Navigation | Scrolls the viewer by 15 rows |
| `Home / End` | Row Start / End | Jumps to the beginning or end of the current row |
| `Ctrl + Home` | File Start | Jumps to offset `0x00000000` |
| `Ctrl + End` | File End | Jumps to the last byte of the file |

---

## 🛠 Building from Source

### Prerequisites
- JDK 11, 17, 21, or 24.
- FlatLaf dependency (`flatlaf-3.5.4.jar`).

### Build with Maven
```bash
# Compile and run
mvn clean compile exec:java -Dexec.mainClass="com.analyzer.hex.HexAnalyzerApp"

# Package into a standalone executable fat jar
mvn clean package
java -jar target/hex-analyzer-1.0.0.jar
```

### Build with Gradle
```bash
# Run application
gradle run

# Build standalone JAR
gradle jar
```

### Manual Compilation via javac (Command Line)
```bash
# 1. Compile source classes
javac -encoding UTF-8 -cp "lib/flatlaf-3.5.4.jar" -d bin src/main/java/com/analyzer/hex/*.java

# 2. Run the application
java -cp "lib/flatlaf-3.5.4.jar;bin" com.analyzer.hex.HexAnalyzerApp
```

### Build the Native Windows `.exe`
Run the included build script:
```cmd
build-exe.bat
```
*(Uses the built-in Microsoft .NET C# compiler `csc.exe` to create `HexAnalyzer.exe` with `app.ico` embedded)*

---

## 📁 Project File Structure

```
c:\vibe\New folder\
├── build-exe.bat                       # Native .exe build batch script
├── build.gradle                        # Gradle build configuration
├── pom.xml                             # Maven build configuration
├── README.md                           # Comprehensive documentation
├── DOCUMENTATION.md                    # Deep technical & architecture documentation
├── app.ico                             # Multi-resolution application icon
├── HexAnalyzer.exe                     # 25 KB native Windows GUI launcher
├── HexAnalyzer.jar                     # Standalone Fat JAR with FlatLaf bundled
├── HexAnalyzerLauncher.cs              # C# native launcher source code
├── run.bat                             # Windows launch script
├── lib/
│   └── flatlaf-3.5.4.jar               # FlatLaf Look-and-Feel library
├── dist/
│   └── HexAnalyzer/                    # Zero-dependency portable distribution
│       ├── HexAnalyzer.exe             # Portable standalone executable
│       ├── app/                        # Application JAR payload
│       └── runtime/                    # Embedded stripped Java runtime
├── src/
│   ├── main/java/com/analyzer/hex/
│   │   ├── HexAnalyzerApp.java         # Main window, menus, toolbars, shortcuts
│   │   ├── HexViewerComponent.java     # Virtualized 3-column viewer & editor
│   │   ├── HexDocument.java            # Binary model, undo/redo, search, entropy
│   │   ├── FileSignatureDetector.java  # Magic bytes signature matching engine
│   │   ├── DataInspectorPanel.java     # Right-hand decoded values inspector
│   │   ├── EntropyVisualizerPanel.java # Block entropy heatmap & byte histogram
│   │   ├── BinaryDiffPanel.java        # Side-by-side binary comparison tool
│   │   ├── StringExtractorPanel.java   # Printable ASCII/UTF-8 string extractor
│   │   ├── FileMetadataPanel.java      # Hashes, checksums, and file metrics
│   │   ├── SnippetExporter.java        # Code snippet and array export utility
│   │   └── SampleGenerator.java        # In-memory binary test samples generator
│   └── test/java/com/analyzer/hex/
│       └── VerifyAppTest.java          # Automated headless unit verification test
```

---

## 📐 Technical Specifications & Formulas

### 1. Shannon Entropy Formula
Entropy evaluates information density and randomness on a scale of $0.00$ to $8.00$ bits per byte:
$$H(X) = -\sum_{i=0}^{255} p(x_i) \log_2 p(x_i)$$
Where:
- $p(x_i) = \frac{\text{count}(x_i)}{N}$ is the probability of byte value $i$.
- $N$ is the block or file length.
- If $\text{count}(x_i) = 0$, $p(x_i) \log_2 p(x_i)$ is treated as $0$.

### 2. Endianness Representation
- **Big-Endian (Network Byte Order):** Most significant byte at lowest address:
  $$\text{Value} = \sum_{k=0}^{n-1} \text{byte}[k] \times 256^{n-1-k}$$
- **Little-Endian (x86/x64 Architecture):** Least significant byte at lowest address:
  $$\text{Value} = \sum_{k=0}^{n-1} \text{byte}[k] \times 256^k$$

### 3. IEEE 754 Floating Point Conversion
Decoded using `java.nio.ByteBuffer` with configured `ByteOrder`:
- **Single Precision (Float, 32-bit):** 1 sign bit, 8 exponent bits, 23 fraction bits.
- **Double Precision (Double, 64-bit):** 1 sign bit, 11 exponent bits, 52 fraction bits.

---

## 📄 License

This project is licensed under the **MIT License**. You are free to use, modify, and distribute this software for personal, academic, or commercial purposes.
