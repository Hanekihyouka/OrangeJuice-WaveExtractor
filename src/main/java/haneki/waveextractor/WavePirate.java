package haneki.waveextractor;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public class WavePirate {

    public static void main(String[] args) {

        // Determine all parameters.
        File sourceFile = null;
        File outputDirectory = null;

        if (args.length >= 1) {
            sourceFile = new File(args[0]);
            outputDirectory = new File(args.length >= 2 ? args[1] : ".");
        }

        // Display the usage text if the arguments are incorrect.
        if (sourceFile == null || outputDirectory == null) {
            System.out.println("usage: source-file [output-directory]");
            return;
        }

        // Check the arguments for validity.
        if (outputDirectory.exists() && !outputDirectory.isDirectory()) {
            System.out.println(outputDirectory.getPath() + " is not a directory.");
            return;
        }

        // Create the output directory if missing.
        if (!outputDirectory.exists() && !outputDirectory.mkdirs()) {
            System.out.println(outputDirectory.getPath() + " could not be created.");
            return;
        }

        // Start extracting.
        try {
            Extractor extractor = new Extractor(sourceFile, outputDirectory);
            int extractedFileCount = extractor.run();
            System.out.println("" + extractedFileCount + " files extracted");
        } catch (FileNotFoundException ex) {
            System.out.println(sourceFile.getPath() + " is not readable.");
        } catch (IOException ex) {
            System.out.println(sourceFile.getPath() + " could not be read correctly due to internal errors.");
        }
    }
}
