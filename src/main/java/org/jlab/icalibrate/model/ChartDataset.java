package org.jlab.icalibrate.model;

import java.text.DecimalFormat;
import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.jfree.data.function.Function2D;
import org.jfree.data.function.LineFunction2D;
import org.jfree.data.statistics.Regression;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

/**
 * Models a JFreeChart dataset capable of providing a fit.
 *
 * @author ryans
 */
public class ChartDataset {

    private final IonChamberDataset measuredDataset;
    private final XYSeriesCollection seriesData;
    private LineFunction2D linearFit;
    private final LineFunction2D logarithmicFit;
    private final SimpleRegression linearRegression = new SimpleRegression();
    private final SimpleRegression logLinearRegression = new SimpleRegression();
    private int minIndex = 0;
    private int maxIndex = 0;
    private boolean logarithmicSelected = false;

    /**
     * Create a new ChartDataset given the measured measuredDataset seriesData
     * and initial current and margin.
     *
     * Note: The units for current are generally uA or nA, and it is up to the
     * caller to ensure they match whatever was used when measuring.
     *
     * @param measuredDataset The ion chamber measured dose rate seriesData
     * @param current The initial current for the linearFit computation
     * @param margin The initial margin (percent) for the linearFit computation
     */
    public ChartDataset(IonChamberDataset measuredDataset, int current, int margin) {
        this.measuredDataset = measuredDataset;

        seriesData = new XYSeriesCollection();
        XYSeries series = new XYSeries("Samples");
        seriesData.addSeries(series);

        for (DoseRateMeasurement measurement : measuredDataset.getMeasurementList()) {
            double x = measurement.getCurrent();
            double y = measurement.getDoseRateRadsPerHour();
            series.add(x, y);
            linearRegression.addData(x, y);
            if (x > 0) { // zero current results in log(0) = undefined; negative current is bad too...
                logLinearRegression.addData(Math.log(x), y);
            }
        }

        maxIndex = series.getItemCount() - 1;

        double yIntercept = linearRegression.getIntercept();
        double slope = linearRegression.getSlope();

        linearFit = new LineFunction2D(yIntercept, slope);

        double logIntercept = logLinearRegression.getIntercept();
        double logSlope = logLinearRegression.getSlope();

        logarithmicFit = new LineFunction2D(logIntercept, logSlope) {
            @Override
            public double getValue(double x) {
                double m = logSlope;
                double b = logIntercept;
                double y;

                if (x == 0) { // ln(x) where x = 0 is undefined and Lim(ln(0)) = -Infinity doesn't show up on graph real good
                    y = Double.NaN;
                } else {
                    y = (m * Math.log(x)) + b;
                }

                //System.out.println("y = m * ln(x) + b: " + y + " = " + m + " * ln(" + x + ") + " + b);
                return y;
            }
        };

        double x = current;
        double y = linearFit.getValue(x) * (100.0d + margin) / 100.0d;

        series = new XYSeries("Setpoint");
        series.add(x, y);
        seriesData.addSeries(series);
    }

    /**
     * Update parameters defining the extent (range) of the dataset to fit.
     *
     * @param min The minimum dataset index (inclusive)
     * @param max The maximum dataset index (inclusive)
     */
    public void updateFitParameters(int min, int max) {
        //System.out.println("updateFitParams: min: " + min);
        //System.out.println("updateFitParams: max: " + max);

        this.minIndex = min;
        this.maxIndex = max;

        XYSeries fitSubset;
        XYSeriesCollection tmp = new XYSeriesCollection();
        try {
            fitSubset = seriesData.getSeries(0).createCopy(minIndex, maxIndex);
            fitSubset.setKey("Subset");
            tmp.addSeries(fitSubset);
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException("Series Subsets Not Supported", e);
        }

        if (fitSubset.getItemCount() > 1) {
            double[] regressionParameters = Regression.getOLSRegression(tmp, 0);

            double yIntercept = regressionParameters[0];
            double slope = regressionParameters[1];

            linearFit = new LineFunction2D(yIntercept, slope);
        }
    }

    /**
     * Recalculate the linear linearFit given updated current and margin.
     *
     * @param current The updated current
     * @param margin The updated margin (percent)
     */
    public void updateSetpointParameters(int current, int margin) {
        Function2D fit;

        if (logarithmicSelected) {
            fit = logarithmicFit;
        } else {
            fit = linearFit;
        }

        seriesData.getSeries(1).clear();
        double x = current;
        double y = fit.getValue(x) * (100.0d + margin) / 100.0d;
        this.seriesData.getSeries(1).add(x, y);
    }

    /**
     * Return the measured dose rate data.
     *
     * @return The measured dose rate data
     */
    public IonChamberDataset getMeasuredDataset() {
        return measuredDataset;
    }

    /**
     * Return the JFreeChart series data.
     *
     * @return The series data
     */
    public XYSeriesCollection getSeriesData() {
        return seriesData;
    }

    /**
     * Return the JFreeChart linear fit function.
     *
     * @return The linear fit
     */
    public LineFunction2D getLinearFit() {
        return linearFit;
    }

    /**
     * Return the JFreeChart logarithmic fit.
     *
     * Note: It's actually not a line, but a curve, but I want to store the
     * linearized transformation slope and intercept here.
     *
     * @return The logarithmic fit
     */
    public LineFunction2D getLogarithmicFit() {
        return logarithmicFit;
    }

    /**
     * Check if user selected a logarithmic fit for this dataset.
     *
     * @return true if logarithmic, false for linear
     */
    public boolean isLogarithmicSelected() {
        return logarithmicSelected;
    }

    /**
     * Set fit type.
     *
     * @param logarithmicSelected true if logarithmic, false for linear
     */
    public void setLogarithmicSelected(boolean logarithmicSelected) {
        this.logarithmicSelected = logarithmicSelected;
    }

    /**
     * Return the calculated setpoint.
     *
     * @return The calculated setpoint
     */
    public double getSetpoint() {
        return seriesData.getSeries(1).getMaxY();
    }

    /**
     * The minimum index of the data to linearFit (inclusive).
     *
     * @return The min index
     */
    public int getMinIndex() {
        return minIndex;
    }

    /**
     * The maximum index of the data to linearFit (inclusive).
     *
     * @return The max index
     */
    public int getMaxIndex() {
        return maxIndex;
    }

    public String getFitEquation() {
        String equation;

        DecimalFormat formatter = new DecimalFormat("###,##0.00");

        double slope;
        double yIntercept;
        String sign = "+ ";

        if (isLogarithmicSelected()) { // Logarithmic
            slope = getLogarithmicFit().getSlope();
            yIntercept = getLogarithmicFit().getIntercept();

            if (Double.isNaN(slope) || Double.isNaN(yIntercept)) {
                equation = "None";
            } else {
                if (yIntercept < 0) {
                    sign = "- ";
                    yIntercept = Math.abs(yIntercept);
                }
                equation = ("y = " + formatter.format(slope) + " * ln(x) " + sign
                        + formatter.format(yIntercept));
            }
        } else { // Linear
            slope = getLinearFit().getSlope();
            yIntercept = getLinearFit().getIntercept();

            if (Double.isNaN(slope) || Double.isNaN(yIntercept)) {
                equation = "None";
            } else {
                if (yIntercept < 0) {
                    sign = "- ";
                    yIntercept = Math.abs(yIntercept);
                }
                equation = ("y = " + formatter.format(slope) + "x " + sign
                        + formatter.format(yIntercept));
            }
        }

        return equation;
    }

    /**
     * Return the R^2 label.
     *
     * @return The label
     */
    public String getRSquareLabel() {
        double r2;
        if (isLogarithmicSelected()) { // Logarithmic
            r2 = logLinearRegression.getRSquare();
        } else {
            r2 = linearRegression.getRSquare();
        }

        String label = "None";
        
        if(!Double.isNaN(r2)) {
            DecimalFormat formatter = new DecimalFormat("0.0");
            label = formatter.format(r2 * 100) + "%";
        }
        return label;
    }
}
