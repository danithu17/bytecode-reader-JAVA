using System;
using System.Diagnostics;
using System.IO;
using System.Windows.Forms;
using Microsoft.Win32;

namespace HexAnalyzer
{
    static class Program
    {
        [STAThread]
        static void Main(string[] args)
        {
            try
            {
                string appDir = AppDomain.CurrentDomain.BaseDirectory;
                string jarPath = Path.Combine(appDir, "HexAnalyzer.jar");

                if (!File.Exists(jarPath))
                {
                    MessageBox.Show(
                        "Cannot find HexAnalyzer.jar in:\n" + appDir,
                        "Hex Analyzer Error",
                        MessageBoxButtons.OK,
                        MessageBoxIcon.Error);
                    return;
                }

                string javaw = FindJavaw(appDir);
                if (string.IsNullOrEmpty(javaw) || !File.Exists(javaw))
                {
                    DialogResult res = MessageBox.Show(
                        "Java Runtime Environment (JRE or JDK 11+) was not detected on this system.\n\n" +
                        "Would you like to visit adoptium.net to download Java?",
                        "Java Not Found",
                        MessageBoxButtons.YesNo,
                        MessageBoxIcon.Warning);

                    if (res == DialogResult.Yes)
                    {
                        Process.Start(new ProcessStartInfo("https://adoptium.net/") { UseShellExecute = true });
                    }
                    return;
                }

                // Prepare arguments
                string arguments = "-jar \"" + jarPath + "\"";
                if (args != null && args.Length > 0)
                {
                    foreach (string arg in args)
                    {
                        arguments += " \"" + arg + "\"";
                    }
                }

                ProcessStartInfo psi = new ProcessStartInfo();
                psi.FileName = javaw;
                psi.Arguments = arguments;
                psi.WorkingDirectory = appDir;
                psi.UseShellExecute = false;
                psi.CreateNoWindow = true;

                Process.Start(psi);
            }
            catch (Exception ex)
            {
                MessageBox.Show(
                    "Error launching Hex Analyzer:\n" + ex.Message,
                    "Startup Error",
                    MessageBoxButtons.OK,
                    MessageBoxIcon.Error);
            }
        }

        private static string FindJavaw(string appDir)
        {
            // 1. Check local bundled JRE
            string localJre = Path.Combine(appDir, "jre", "bin", "javaw.exe");
            if (File.Exists(localJre)) return localJre;

            string localJdk = Path.Combine(appDir, "jdk", "bin", "javaw.exe");
            if (File.Exists(localJdk)) return localJdk;

            // 2. Check JAVA_HOME
            string javaHome = Environment.GetEnvironmentVariable("JAVA_HOME");
            if (!string.IsNullOrEmpty(javaHome))
            {
                string path = Path.Combine(javaHome, "bin", "javaw.exe");
                if (File.Exists(path)) return path;
            }

            // 3. Check known default Program Files paths
            string[] knownLocations = {
                @"C:\Program Files\Java\jdk-24\bin\javaw.exe",
                @"C:\Program Files\Java\jdk-21\bin\javaw.exe",
                @"C:\Program Files\Java\jdk-17\bin\javaw.exe",
                @"C:\Program Files\Java\jdk-11\bin\javaw.exe"
            };
            foreach (string p in knownLocations)
            {
                if (File.Exists(p)) return p;
            }

            // 4. Check Windows Registry
            string regJava = GetJavaFromRegistry(@"SOFTWARE\JavaSoft\JDK") ??
                             GetJavaFromRegistry(@"SOFTWARE\JavaSoft\Java Development Kit") ??
                             GetJavaFromRegistry(@"SOFTWARE\JavaSoft\Java Runtime Environment");
            if (!string.IsNullOrEmpty(regJava) && File.Exists(regJava))
            {
                return regJava;
            }

            // 5. Check PATH environment variable
            string pathEnv = Environment.GetEnvironmentVariable("PATH");
            if (!string.IsNullOrEmpty(pathEnv))
            {
                string[] dirs = pathEnv.Split(';');
                foreach (string dir in dirs)
                {
                    string candidate = Path.Combine(dir.Trim(), "javaw.exe");
                    if (File.Exists(candidate)) return candidate;
                }
            }

            return null;
        }

        private static string GetJavaFromRegistry(string keyPath)
        {
            try
            {
                using (RegistryKey root = Registry.LocalMachine.OpenSubKey(keyPath))
                {
                    if (root != null)
                    {
                        string currentVer = root.GetValue("CurrentVersion") as string;
                        if (!string.IsNullOrEmpty(currentVer))
                        {
                            using (RegistryKey verKey = root.OpenSubKey(currentVer))
                            {
                                if (verKey != null)
                                {
                                    string javaHome = verKey.GetValue("JavaHome") as string;
                                    if (!string.IsNullOrEmpty(javaHome))
                                    {
                                        string exe = Path.Combine(javaHome, "bin", "javaw.exe");
                                        if (File.Exists(exe)) return exe;
                                    }
                                }
                            }
                        }
                    }
                }
            }
            catch { }
            return null;
        }
    }
}
