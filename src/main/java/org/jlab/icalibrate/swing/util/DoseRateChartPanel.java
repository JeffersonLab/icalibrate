package org.jlab.icalibrate.swing.util;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.geom.Rectangle2D;
import javax.swing.JPanel;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYShapeAnnotation;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.function.Function2D;
import org.jfree.data.general.DatasetUtilities;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jlab.icalibrate.model.ChartDataset;

/**
 * A JPanel for displaying a JFreeChart dose rate vs current chart with linear fit and setpoint.
 *
 * @author ryans
 */
public class DoseRateChartPanel extends JPanel {

  /** The chart. */
  private JFreeChart chart;

  /** The dataset. */
  private ChartDataset dataset;

  /** The annotation. */
  private XYShapeAnnotation annotation;

  private static final Color ANNOTATION_COLOR = new Color(255, 100, 100, 31);

  /** Create a new DoseRateChartPanel. */
  public DoseRateChartPanel() {}

  /**
   * Get the selected ChartDataset.
   *
   * @return The ChartDataset
   */
  public ChartDataset getDataset() {
    return dataset;
  }

  /**
   * Set the ChartDataset to be displayed.
   *
   * @param dataset The dataset
   * @param currentUnits The current units
   */
  public void setDataset(ChartDataset dataset, String currentUnits) {

    this.removeAll();

    this.dataset = dataset;

    if (dataset != null) {
      this.chart =
          ChartFactory.createScatterPlot(
              dataset.getMeasuredDataset().getIonChamber().getFullName(), // title
              "Beam Current (" + currentUnits + ")", // x axis label
              "Dose Rate (rads/hr)", // y axis label
              dataset.getSeriesData(), // points
              PlotOrientation.VERTICAL,
              true, // include legend
              true, // tooltips
              false // urls
              );
      this.chart.getXYPlot().getRangeAxis().setAutoRangeMinimumSize(2.0);

      ChartPanel chartPanel = new ChartPanel(this.chart);
      this.setLayout(new BorderLayout());
      this.add(chartPanel, BorderLayout.CENTER);

      // Only hightlight fit range if not using all datapoints
      if (dataset.getMinIndex() != 0
          || dataset.getMaxIndex()
              != dataset.getMeasuredDataset().getMeasurementList().size() - 1) {
        double x = dataset.getSeriesData().getSeries(0).getX(dataset.getMinIndex()).doubleValue();
        double width =
            dataset.getSeriesData().getSeries(0).getX(dataset.getMaxIndex()).doubleValue();
        double y = dataset.getSeriesData().getSeries(0).getMinY();
        double height = dataset.getSeriesData().getSeries(0).getMaxY();
        annotation =
            new XYShapeAnnotation(
                new Rectangle2D.Double(x, y, width - x, height - y), null, null, ANNOTATION_COLOR);
        this.chart.getXYPlot().addAnnotation(annotation);
      }

      drawFit();
    }

    this.repaint();
    this.revalidate();
  }

  /**
   * Draw the fit on the chart. This is automatically called after setDataset, but needs to be
   * manually called each time after changing the dataset's fit parameters.
   */
  public void drawFit() {

    // XYSeries series = dataset.getSeriesData().getSeries(0);
    // double minX = series.getX(dataset.getMinIndex()).doubleValue();
    // double maxX = series.getX(dataset.getMaxIndex()).doubleValue();
    XYSeries series0 = dataset.getSeriesData().getSeries(0); // Samples
    XYSeries series1 = dataset.getSeriesData().getSeries(1); // Setpoint
    double minX = 0;
    double maxX = Math.max(series0.getMaxX(), series1.getMaxX());

    Function2D func;
    String fitLabel;
    XYDataset fitData;

    if (dataset.isLogarithmicSelected()) {
      fitLabel = "Logarithmic Fit";
      func = dataset.getLogarithmicFit();
    } else {
      fitLabel = "Linear Fit";
      func = dataset.getLinearFit();
    }

    fitData = DatasetUtilities.sampleFunction2D(func, minX, maxX, 100, fitLabel);

    this.chart.getXYPlot().setDataset(1, fitData);
    XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(true, false);
    renderer.setSeriesPaint(0, Color.YELLOW);
    this.chart.getXYPlot().setRenderer(1, renderer);

    if (annotation != null) {
      this.chart.getXYPlot().removeAnnotation(annotation);
    }

    // Only hightlight fit range if not using all datapoints
    if (dataset.getMinIndex() != 0
        || dataset.getMaxIndex() != dataset.getMeasuredDataset().getMeasurementList().size() - 1) {
      double x = dataset.getSeriesData().getSeries(0).getX(dataset.getMinIndex()).doubleValue();
      double width = dataset.getSeriesData().getSeries(0).getX(dataset.getMaxIndex()).doubleValue();
      double y = dataset.getSeriesData().getSeries(0).getMinY();
      double height = dataset.getSeriesData().getSeries(0).getMaxY();
      annotation =
          new XYShapeAnnotation(
              new Rectangle2D.Double(x, y, width - x, height - y), null, null, ANNOTATION_COLOR);
      this.chart.getXYPlot().addAnnotation(annotation);
    }
  }

  /**
   * Get the JFreeChart.
   *
   * @return The chart
   */
  public JFreeChart getChart() {
    return chart;
  }
}
