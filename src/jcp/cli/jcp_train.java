// Copyright (C) 2014  Anders Gidenstam
// License: to be defined.
package jcp.cli;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Date;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Random;

import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.ObjectMatrix2D;

import jcp.cp.*;
import jcp.nc.*;
import jcp.io.*;

/**
 * Command line training tool for JCP.
 *
 * @author anders.gidenstam(at)hb.se
 */

public class jcp_train
{
    public jcp_train()
    {
        super();
    }

    public void run(String[] args)
        throws IOException
    {
        Random rand = new Random(new Date().getTime());

        double n = 0;

        DataSet full;
        DataSet training    = new DataSet();
        DataSet calibration = new DataSet();
        DataSet test        = new DataSet();

        // Load and create training and calibration sets.
        if (args.length != 1) {
            System.out.println("Usage: jcp_train <libsvm formatted data set>");
            System.exit(-1);
        }

        long t1 = System.currentTimeMillis();
        // FIXME: Only initial use case yet. Train & test.
        FileInputStream file;
        file = new FileInputStream(args[0]);

        full = new libsvmReader().read(file);
        file.close();
        long t2 = System.currentTimeMillis();

        System.out.println("Loaded the dataset " + args[0] + " containing " +
                           full.x.rows() + " instances with " +
                           full.x.columns() + " attributes.");
        System.out.println("Duration " + (double)(t2 - t1)/1000.0 + " sec.");

        SortedSet<Double> classSet = new TreeSet<Double>();
        for (int r = 0; r < full.x.rows(); r++) {
            if (!classSet.contains(full.y[r])) {
                classSet.add(full.y[r]);
            }
        }
        double[] classes = new double[classSet.size()];
        System.out.println("Classes: ");
        int i = 0;
        for (Double c : classSet.toArray(new Double[0])) {
            classes[i] = c;
            System.out.println("   " + classes[i]);
            i++;
        }

        full.random3Partition(training, calibration, test, 0.4, 0.1);
        long t3 = System.currentTimeMillis();
        System.out.println("Split the data set into training set, " +
                           training.x.rows() + " instances, " +
                           "calibration set, " +
                           calibration.x.rows() + " instances, " +
                           "and test set, " + test.x.rows() + " instances.");
        System.out.println("Duration " + (double)(t3 - t2)/1000.0 + " sec.");

        System.out.println("Training on " + training.x.rows() +
                           " instances and calibrating on " +
                           calibration.x.rows() +
                           " instances.");


        InductiveConformalClassifier icc =
            new InductiveConformalClassifier(classes);
        icc._nc =
            new SVMClassificationNonconformityFunction(classes);

        icc.fit(training.x, training.y, calibration.x, calibration.y);
        long t4 = System.currentTimeMillis();
        System.out.println("Training complete.");
        System.out.println("Duration " + (double)(t4 - t3)/1000.0 + " sec.");

        System.out.println("Testing accuracy on " + test.x.rows() +
                           " instances.");

        // Evaluation on the test set.
        ObjectMatrix2D pred = null;
        pred = icc.predict(test.x, 0.05);
        //System.out.println(pred);

        for (i = 0; i < pred.rows(); i++){
            int classIndex = classSet.headSet(test.y[i]).size();
            if ((Boolean)pred.get(i, classIndex)) {
                n += 1;
            }
        }

        System.out.println("Accuracy " + (n / test.y.length));
        long t5 = System.currentTimeMillis();
        System.out.println("Duration " + (double)(t5 - t4)/1000.0 + " sec.");
        System.out.println("Total Duration " + (double)(t5 - t1)/1000.0 +
                           " sec.");
    }

    public static void main(String[] args)
        throws IOException
    {
         new jcp_train().run(args);
    }
}