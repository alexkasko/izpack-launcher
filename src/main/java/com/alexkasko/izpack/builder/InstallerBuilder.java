package com.alexkasko.izpack.builder;

import com.izforge.izpack.compiler.CompilerConfig;
import org.apache.commons.cli.*;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;

import static java.lang.System.out;
import static org.apache.commons.io.FileUtils.*;
import static org.apache.commons.io.IOUtils.closeQuietly;

/**
 * User: alexkasko
 * Date: 11/18/12
 */
public class InstallerBuilder {
    private static final String VERSION = "IzPack Installer Builder Utility 1.0";
    private static final String HELP_OPTION = "help";
    private static final String VERSION_OPTION = "version";
    private static final String IZPACK_XML = "izpack-xml";
    private static final String IZPACK_COPMPRESS = "izpack-compress";
    private static final String IZPACK_DEFAULT_INSTALL_DIR = "izpack-default-install-dir";
    private static final String IZPACK_LICENSE_FILE = "izpack-license-file";

    public static void main(String[] args) throws Exception {
        Options options = new Options();
        try {
            options.addOption("h", HELP_OPTION, false, "show this page");
            options.addOption("v", VERSION_OPTION, false, "show version");
            options.addOption("x", IZPACK_XML, true, "izpack.xml descriptor file");
            options.addOption("d", IZPACK_DEFAULT_INSTALL_DIR, true, "default install directory'");
            options.addOption("c", IZPACK_COPMPRESS, true, "installer compress option: 'raw' (default), 'deflate' and 'bzip2'");
            options.addOption("l", IZPACK_LICENSE_FILE, true, "license file");
            CommandLine cline = new GnuParser().parse(options, args);
            if (cline.hasOption(VERSION_OPTION)) {
                out.println(VERSION);
            } else if (cline.hasOption(HELP_OPTION)) {
                throw new ParseException("Printing help page:");
            } else if (2 == cline.getArgs().length &&
                    cline.hasOption(IZPACK_XML) &&
                    cline.hasOption(IZPACK_DEFAULT_INSTALL_DIR) &&
                    cline.hasOption(IZPACK_LICENSE_FILE)) {
                File inputFile = new File(cline.getArgs()[0]);
                if(!(inputFile.exists() && inputFile.isDirectory())) throw new IOException("Invalid input dir: '" + inputFile.getAbsolutePath() + "'");
                File outputFile = new File(cline.getArgs()[1]);
                if(outputFile.exists()) throw new IOException("Output file exists: '" + outputFile.getAbsolutePath() + "'");
                File ixFile = new File(cline.getOptionValue(IZPACK_XML));
                if(!(ixFile.exists() && ixFile.isFile())) throw new IOException("Invalid izpack.xml: '" + ixFile.getAbsolutePath() + "'");
                String compress = cline.hasOption(IZPACK_COPMPRESS) ? cline.getOptionValue(IZPACK_COPMPRESS) : "raw";
                String defaultInstallDir = cline.getOptionValue(IZPACK_DEFAULT_INSTALL_DIR);
                File licenseFile = new File(cline.getOptionValue(IZPACK_LICENSE_FILE));
                if(!(licenseFile.exists() && licenseFile.isFile())) throw new IOException("Invalid license file: '" + licenseFile.getAbsolutePath() + "'");
                buildInstaller(inputFile, outputFile, ixFile, defaultInstallDir, compress, licenseFile);
            } else {
                throw new ParseException("Incorrect arguments received!");
            }
        } catch (ParseException e) {
            HelpFormatter formatter = new HelpFormatter();
            out.println(e.getMessage());
            out.println(VERSION);
            formatter.printHelp("java -jar izpack-builder.jar -x izpack.xml -d 'c:\\myapp'\n" +
                    "-l license.txt [-c (deflate|bzip2)]\n" +
                    "input.dir output.jar", options);
        }
    }

    private static void buildInstaller(File input, File output, File izpackXml, String defaultInstallDir,
                                       String compress, File licenseFile) throws Exception {
        PrintStream console = System.out;
        PrintStream log = null;
        File workDir = null;
        try {
            // redirect izPack output to file
            File appDir = appDir();
            log = openLog(appDir);
            System.setOut(log);
            workDir = workDir(appDir);
            writeDefaultInstallString(workDir, defaultInstallDir);
            copyLicense(workDir, licenseFile);
            copyUninstall(appDir, workDir);
            copyDirectoryToDirectory(input, workDir);
            // run compiler
            System.out.println("Starting IzPack, see output in izpack-builder.log ...");
            CompilerConfig compilerConfig = new CompilerConfig(izpackXml.getAbsolutePath(), workDir.getAbsolutePath(),
                    "standard", output.getAbsolutePath(), compress, null);
            CompilerConfig.setIzpackHome(workDir.getAbsolutePath());
            compilerConfig.executeCompiler();
            System.out.println("Installer built successfully: '" + output + "'");
        } finally {
            System.setOut(console);
            closeQuietly(log);
            deleteQuietly(workDir);
        }
    }

    private static File appDir() throws URISyntaxException {
        URI uri = InstallerBuilder.class.getProtectionDomain().getCodeSource().getLocation().toURI();
        return new File(uri).getParentFile();
    }

    private static PrintStream openLog(File appDir) throws URISyntaxException, FileNotFoundException, UnsupportedEncodingException {
        File log = new File(appDir, "izpack-builder.log");
        return new PrintStream(new FileOutputStream(log, true), true, "UTF-8");
    }

    private static File workDir(File appDir) throws IOException {
        File workDir = new File(appDir, "izpack/work");
        boolean res = workDir.mkdirs();
        if(!res) throw new IOException("Cannot create directory: '" + workDir + "'");
        return workDir;
    }

    private static void writeDefaultInstallString(File workDir, String value) throws IOException {
        File file = new File(workDir, "default-install-dir.txt");
        writeStringToFile(file, value, "UTF-8");
    }

    private static void copyLicense(File workDir, File licenseFile) throws IOException {
        File target = new File(workDir, "license.txt");
        copyFile(licenseFile, target);
    }

    private static void copyUninstall(File appDir, File workDir) throws IOException {
        File uninstallDir = new File(appDir, "izpack/uninstall");
        copyDirectoryToDirectory(uninstallDir, workDir);
    }
}
