package haneki.waveextractor;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Extractor {

    private static final byte[] RIFF_PATTERN = new byte[]{
        (byte) 0x52, // R
        (byte) 0x49, // I
        (byte) 0x46, // F
        (byte) 0x46  // F
    };

    private static final byte[] WAVE_PATTERN = new byte[]{
        (byte) 0x57, // W
        (byte) 0x41, // A
        (byte) 0x56, // V
        (byte) 0x45  // E
    };

    private static final byte[] FMT_BLOCK = new byte[]{
            (byte) 0x66, // f
            (byte) 0x6D, // m
            (byte) 0x74, // t
            (byte) 0x20, // space
            (byte) 0x10, // 16 -> size of meta
            (byte) 0x00,
            (byte) 0x00,
            (byte) 0x00
    };

    private static final byte[] DATA_BLOCK = new byte[]{
            (byte) 0x64, // d
            (byte) 0x61, // a
            (byte) 0x74, // t
            (byte) 0x61  // a
    };

    /**
     * file name extension pattern
     * for file name with ".wav"
     */
    private static final byte[] WAVE_FLAG_PATTERN = new byte[]{
            (byte) 0x2E, // dot
            (byte) 0x57, // W
            (byte) 0x41, // A
            (byte) 0x56, // V
            (byte) 0x00,
            (byte) 0x12, // flag
            (byte) 0x00, // flag
            (byte) 0x00, // flag
            (byte) 0x00  // flag
    };


    private static final int COPY_BUFFER_SIZE = 0x2000;
    private static final int EOF = 0xFFFFFFFF;

    private File sourceFile;
    private File outputDirectory;
    private DataInputStream source;

    public Extractor(File sourceFile, File outputDirectory) {
        this.sourceFile = sourceFile;
        this.outputDirectory = outputDirectory;
    }

    public int run() throws FileNotFoundException, IOException {
        
        source = new DataInputStream(new BufferedInputStream(new FileInputStream(sourceFile)));
        int extractedFileCount = 0;
        try {
            outter: for (;; extractedFileCount++) {
                byte[] sizeBuffer = new byte[4];
                byte[] metaDataBuffer = new byte[16];
                byte[] idkWhatIsThisBuffer = new byte[2];
                byte[] fileSizeBuffer = new byte[4];
                /**
                 * read in and set size
                 * size = 4
                 */
                source.read(sizeBuffer);
                ArrayList<Byte> fileNameBytes = new ArrayList<>();

                // check head and catch filename
                for (int p = 0;;) {
                    int b = source.read();
                    if (b == EOF){
                        break outter;
                    }
                    fileNameBytes.add((byte) b);
                    if (b == WAVE_FLAG_PATTERN[p]){
                        p++;
                        if (p==WAVE_FLAG_PATTERN.length){
                            break;
                        }
                    }else {
                        p = 0;
                    }
                }
                /**
                 * set filename(blockname)
                 * size is variable
                 */
                byte[] fileNameBuffer = new byte[fileNameBytes.size() - WAVE_FLAG_PATTERN.length];
                for (int i = 0; i < fileNameBuffer.length; i++) {
                    fileNameBuffer[i] = fileNameBytes.get(i);
                }
                String name = new String(fileNameBuffer, StandardCharsets.UTF_8);

                /**
                 * read in and set metadata
                 * size = 16
                 */
                source.read(metaDataBuffer);
                /**
                 * read in idkWhatIsThisBuffer
                 * size = 2
                 */
                source.read(idkWhatIsThisBuffer);

                long contentSize = ByteBuffer.wrap(sizeBuffer).order(ByteOrder.LITTLE_ENDIAN).getInt();
                long size = contentSize + RIFF_PATTERN.length + sizeBuffer.length;
                System.out.println(extractedFileCount + " : " + name + " (" + size + " bytes)");
                fileSizeBuffer = ByteBuffer.allocate(4).putInt((int) (contentSize + 36)).array();

                // Create the output file.
                File outputFile = new File(outputDirectory, name + ".wav");


                OutputStream output = new BufferedOutputStream(new FileOutputStream(outputFile));
                output.write(RIFF_PATTERN);//4
                output.write(fileSizeBuffer[3]);// -4-
                output.write(fileSizeBuffer[2]);
                output.write(fileSizeBuffer[1]);
                output.write(fileSizeBuffer[0]);
                output.write(WAVE_PATTERN);//4
                output.write(FMT_BLOCK);// 4+4
                output.write(metaDataBuffer);//16
                output.write(DATA_BLOCK);//4
                output.write(sizeBuffer);//4

                // Copy the wave data over to the output file in chunks.
                long remainingContentSize = contentSize;

                while (remainingContentSize > 0) {
                    byte[] waveBuffer;

                    if (remainingContentSize > COPY_BUFFER_SIZE) {
                        waveBuffer = new byte[COPY_BUFFER_SIZE];
                        remainingContentSize -= COPY_BUFFER_SIZE;
                    } else {
                        waveBuffer = new byte[(int) remainingContentSize];
                        remainingContentSize = 0;
                    }

                    source.readFully(waveBuffer);
                    output.write(waveBuffer);
                }
                output.close();
            }

        } catch (IOException ex) {
            Logger.getLogger(Extractor.class.getName()).log(Level.SEVERE, null, ex);
            source.close();
            throw ex;
        }

        return extractedFileCount;
    }
}
