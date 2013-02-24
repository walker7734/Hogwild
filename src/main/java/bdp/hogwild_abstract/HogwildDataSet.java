package bdp.hogwild_abstract;

import java.io.*;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public abstract class HogwildDataSet {
    protected String filename;
    protected boolean isTraining;
    protected MappedByteBuffer dataFile;
    protected byte[] buff;
    public BufferedReader reader;

    
    public HogwildDataSet (String filename, boolean training) throws IOException {
        if (filename == null) {
            throw new IllegalArgumentException("HogwildDataSet: filename cannot be null");
        }

        this.filename = filename;
        isTraining = training;
        setupReader();
    }

    private void setupReader() throws IOException {
        Path p = FileSystems.getDefault().getPath(filename);
        FileChannel f = new RandomAccessFile(filename, "r").getChannel();
        dataFile = f.map(FileChannel.MapMode.READ_ONLY, 0, Files.size(p));
        buff = new byte[(int)Files.size(p)];
        dataFile.get(buff);
        reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(buff)));
    }
    
    public abstract HogwildDataInstance getInstanceAt(int index);
    public abstract HogwildDataInstance getRandomInstance(boolean withReplacement);
    public abstract int getSize();
}
