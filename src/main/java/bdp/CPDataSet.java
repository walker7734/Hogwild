package bdp;

import bdp.hogwild_abstract.HogwildDataInstance;
import bdp.hogwild_abstract.HogwildDataSet;

import java.io.*;
import java.util.Random;
import java.util.Scanner;

public class CPDataSet extends HogwildDataSet {
    public static final int TRAINING_SIZE = 2335859;
    public static final int TESTING_SIZE = 1016552;
    private CPDataInstance[] dataPoints;
    private static int startingIndex;
    
    public CPDataSet(String filename, boolean training) throws IOException {
        super(filename, training);
        dataPoints = (training) ? new CPDataInstance[TRAINING_SIZE] : new CPDataInstance[TESTING_SIZE];
        startingIndex = 0;
        parseData();
    }


    private void parseData() throws IOException {

       int index = 1;
       dataPoints[0] = new CPDataInstance(readFromFile(0), isTraining);
       for (int i = 1; i < buff.length; i++) {
           if (buff[i-1] == '\n') {
               dataPoints[index] = new CPDataInstance(readFromFile(i), isTraining);
               index++;
           }
       }
    }

    @Override
    public HogwildDataInstance getInstanceAt(int index) {
        return dataPoints[index];
    }

    @Override
    public HogwildDataInstance getRandomInstance(boolean withReplacement) {
        int index;
        if (withReplacement) {
            index = (int) (Math.random() * (dataPoints.length - 1));
            return dataPoints[index];
        }

        startingIndex = (startingIndex == dataPoints.length - 1) ? 0 : startingIndex;
        index = startingIndex + (int) (Math.random() * (dataPoints.length - startingIndex - 1));

        CPDataInstance newData = dataPoints[index];
        CPDataInstance temp = dataPoints[startingIndex];
        dataPoints[startingIndex] = dataPoints[index];
        dataPoints[index] = temp;
        startingIndex++;
        return newData;

    }
    
    @Override
    public int getSize() {
       return dataPoints.length;
    }

    private String readFromFile(int position) {
        StringBuilder s = new StringBuilder();
        while (buff[position] != '\n') {
            s.append((char)buff[position]);
            position++;
        }
        return s.toString();
    }
}
