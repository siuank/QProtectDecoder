package me.siuank.deobf.qpdec;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Enumeration;
import java.util.concurrent.Callable;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@CommandLine.Command(name = "QPDecoder", version = "v1.0.0", mixinStandardHelpOptions = true, showDefaultValues = true, description = "Decode QP encoded string")
public class QPDecoder implements Callable<Integer> {
    public static Logger logger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);

    @CommandLine.Option(names = {"-i", "--input"}, description = "Input obfuscated jar file", required = true)
    public File input;
    @CommandLine.Option(names = {"-o", "--output"}, description = "Output target file", required = true)
    public File output;

    @Override
    public Integer call() {
        try {
            assert !input.isDirectory() : "Input file must be a jar file!";

            run(input, output);
        } catch (Throwable e) {
            e.printStackTrace(System.err);
            return 1;
        }
        return 0;
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new QPDecoder()).execute(args);
        System.exit(exitCode);
    }

    public static void run(File fileInput, File fileOutput) throws Throwable {
        try (JarFile inputFile = new JarFile(fileInput);
             ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(Files.newOutputStream(fileOutput.toPath())))
        ) {
            Enumeration<JarEntry> entries = inputFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                try (InputStream inputStream = inputFile.getInputStream(entry)) {
                    byte[] src = new byte[inputStream.available()];
                    //noinspection ResultOfMethodCallIgnored
                    inputStream.read(src);
                    if (entry.getName().endsWith(".class")) {
                        ClassReader classReader = new ClassReader(src);
                        ClassNode node = new ClassNode();
                        classReader.accept(node, ClassReader.SKIP_FRAMES);

                        acceptClass(node);

                        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
                        node.accept(writer);
                        writeEntry(out, entry.getName(), writer.toByteArray());
                        logger.info("Decoded class: {}", entry.getName());
                    } else {
                        writeEntry(out, entry.getName(), src);
                    }
                }
                out.flush();
            }
        }
    }

    public static void writeEntry(ZipOutputStream outJar, String name, byte[] value) throws IOException {
        ZipEntry newEntry = new ZipEntry(name);

        outJar.putNextEntry(newEntry);
        outJar.write(value);
    }

    public static void acceptClass(ClassNode node) {
        node.methods.forEach(QPDecoder::acceptMethod);
        QPFlowRemover.removeFlow(node);
    }

    public static void acceptMethod(MethodNode node) {
        QPIndyCleaner.cleanInvokeDynamicCall(node);
        QPStringDecrypter.runDecryptLogic(node);
    }
}
