package org.jlab.icalibrate.swing.generated;

import gov.aps.jca.dbr.DBR;
import gov.aps.jca.dbr.DBRType;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import org.jlab.icalibrate.ICalibrateApp;
import org.jlab.icalibrate.epics.ChannelManager;
import org.jlab.icalibrate.epics.PvListener;
import org.jlab.icalibrate.model.ChartDataset;
import org.jlab.icalibrate.model.Hall;
import org.jlab.icalibrate.model.HallCalibrationDataset;
import org.jlab.icalibrate.model.IonChamber;
import org.jlab.icalibrate.model.IonChamberDataset;
import org.jlab.icalibrate.model.CreateNewDatasetParameters;
import org.jlab.icalibrate.model.DoseRateMeasurement;
import org.jlab.icalibrate.swing.ModalWaitFrame;
import org.jlab.icalibrate.swing.action.ExportEpicsAction;
import org.jlab.icalibrate.swing.action.ExportSnapAction;
import org.jlab.icalibrate.swing.action.NewDatasetAction;
import org.jlab.icalibrate.swing.action.OpenModifyDataDialogAction;
import org.jlab.icalibrate.swing.action.listener.ExportElogActionListener;
import org.jlab.icalibrate.swing.action.listener.OpenHCDActionListener;
import org.jlab.icalibrate.swing.action.listener.PromptUnsavedThenContinueActionListener;
import org.jlab.icalibrate.swing.action.listener.SaveHCDThenContinueActionListener;
import org.jlab.icalibrate.swing.generated.dialog.HelpDialog;
import org.jlab.icalibrate.swing.generated.dialog.CreateDatasetProgressDialog;
import org.jlab.icalibrate.swing.generated.dialog.ChooseAndModifySetpointDialog;
import org.jlab.icalibrate.swing.generated.dialog.ModifySampleDataDialog;
import org.jlab.icalibrate.swing.table.model.DoseRateTableModel;
import org.jlab.icalibrate.swing.table.model.SetpointTableModel;
import org.jlab.icalibrate.swing.table.model.SetpointTableModel.SetpointRow;
import org.jlab.icalibrate.swing.table.renderer.UnselectableDoseRateCellRenderer;
import org.jlab.icalibrate.swing.util.DoseRateChartPanel;
import org.jlab.icalibrate.swing.table.renderer.MeasuredCurrentCellRenderer;
import org.jlab.icalibrate.swing.table.renderer.UnselectableCellRenderer;
import org.jlab.icalibrate.swing.generated.wizard.Wizard;
import org.jlab.icalibrate.swing.generated.wizard.WizardPage;
import org.jlab.icalibrate.swing.generated.wizard.page.HallAndOptionsPage;
import org.jlab.icalibrate.swing.generated.wizard.page.IonChamberPage;
import org.jlab.icalibrate.swing.generated.wizard.page.LaserTargetBeamPage;
import org.jlab.icalibrate.swing.generated.wizard.page.ReviewPage;

/**
 * The main iCalibrate main GUI window, which allows users to control the
 * calibration process and allows users to manage calibration files.
 *
 * Layout was done using Netbeans Matisse Swing GUI builder.
 *
 * @author ryans
 */
public class ICalibrateFrame extends ModalWaitFrame implements PvListener {

    private static final Logger LOGGER = Logger.getLogger(
            ICalibrateFrame.class.getName());

    private final HelpDialog helpDialog = new HelpDialog(this);
    private final ModifySampleDataDialog modifyDoseRateDialog = new ModifySampleDataDialog(this);
    private Wizard<CreateNewDatasetParameters> newDatasetWizard;
    private final ChooseAndModifySetpointDialog modifySetpointDialog
            = new ChooseAndModifySetpointDialog(this);
    private final CreateDatasetProgressDialog progressDialog = new CreateDatasetProgressDialog(this);

    private final SetpointTableModel setpointTableModel = new SetpointTableModel();
    private final DoseRateTableModel doseRateTableModel = new DoseRateTableModel();
    private final DoseRateChartPanel chartPanel = new DoseRateChartPanel();
    private HallCalibrationDataset dataset = null;
    private List<ChartDataset> chartDatasetList = null;
    private boolean persisted = false;
    private String filename;
    private final ChannelManager channelManager;
    private final Map<String, Integer> pvToRowIndexMap = new HashMap<>();
    private final HashMap<Hall, String[]> negativeTargetsMap = new HashMap<>();

    /**
     * Create a new ICalibrateFrame.
     *
     * @param channelManager The EPICS channel manager
     */
    public ICalibrateFrame(ChannelManager channelManager) {
        this.channelManager = channelManager;
        initComponents();
        initActions();
        initTableFormat();
        initMyComponents();
        initWizard();
        
        boolean writeAllowed = "true".equals(ICalibrateApp.APP_PROPERTIES.getProperty(
                    "WRITE_ALLOWED"));
        
        if(!writeAllowed) {
            newDatasetWizard.setTitle(newDatasetWizard.getTitle() + " (Read-Only)");
        }
    }

    private void initActions() {       
        openMenuItem.addActionListener(new PromptUnsavedThenContinueActionListener(this,
                new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                OpenHCDActionListener openAction = new OpenHCDActionListener(ICalibrateFrame.this);
                openAction.actionPerformed(e);
            }
        }));

        exitMenuItem.addActionListener(new PromptUnsavedThenContinueActionListener(this,
                new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                doExit();
            }
        }));

        closeMenuItem.addActionListener(new PromptUnsavedThenContinueActionListener(this,
                new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                doClose();
            }
        }));

        newDatasetMenuItem.addActionListener(new PromptUnsavedThenContinueActionListener(this,
                new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                newDatasetWizard.reset();
                newDatasetWizard.pack();
                newDatasetWizard.setLocationRelativeTo(ICalibrateFrame.this);
                newDatasetWizard.setVisible(true);
            }
        }));

        saveMenuItem.addActionListener(new SaveHCDThenContinueActionListener(this, null));

        exportEpicsMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                modifySetpointDialog.setData(setpointTableModel.getRows());
                modifySetpointDialog.setSaveAction(new ExportEpicsAction(
                        ICalibrateFrame.this));
                modifySetpointDialog.pack();
                modifySetpointDialog.setLocationRelativeTo(ICalibrateFrame.this);
                modifySetpointDialog.setVisible(true);
            }
        });

        exportSnapMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                modifySetpointDialog.setData(setpointTableModel.getRows());
                modifySetpointDialog.setSaveAction(new ExportSnapAction(ICalibrateFrame.this));
                modifySetpointDialog.pack();
                modifySetpointDialog.setLocationRelativeTo(ICalibrateFrame.this);
                modifySetpointDialog.setVisible(true);
            }
        });

        exportelogMenuItem.addActionListener(new ExportElogActionListener(this));

        OpenModifyDataDialogAction openModifyDataDialogAction = new OpenModifyDataDialogAction(chartPanel, modifyDoseRateDialog, this);

        dataMenuItem.setAction(openModifyDataDialogAction);
        modifyDataButton.setAction(openModifyDataDialogAction);

        aboutMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                helpDialog.pack();
                helpDialog.setLocationRelativeTo(ICalibrateFrame.this);
                helpDialog.setVisible(true);
            }
        });

        setpointTableModel.addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                int row = e.getFirstRow();
                int column = e.getColumn();
                if (column == 3) {
                    TableModel model = (TableModel) e.getSource();
                    Boolean checked = (Boolean) model.getValueAt(row, column);

                    int index = setpointTable.getSelectedRow();
                    ChartDataset selected = chartDatasetList.get(index);

                    selected.setLogarithmicSelected(checked);
                    recalculateFit();
                }
            }
        });

        /*linearFitToggle.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent ev) {

                int index = setpointTable.getSelectedRow();
                ChartDataset selected = chartDatasetList.get(index);

                if (ev.getStateChange() == ItemEvent.SELECTED) {
                    linearFitToggle.setText("Logarithmic Fit");
                    fitEquationDescription.setText("Logarithmic Fit Equation: ");
                    selected.setLogarithmicSelected(true);
                } else if (ev.getStateChange() == ItemEvent.DESELECTED) {
                    linearFitToggle.setText("Linear Fit");
                    fitEquationDescription.setText("Linear Fit Equation: ");
                    selected.setLogarithmicSelected(false);
                }
                recalculateFit();
            }
        });*/
    }

    private void initWizard() {
        List<WizardPage> pageList = new ArrayList<>();

        CreateNewDatasetParameters params = new CreateNewDatasetParameters(channelManager);

        newDatasetWizard = new Wizard<>(this, params, new NewDatasetAction(this, progressDialog,
                params));

        HallAndOptionsPage hallPage = new HallAndOptionsPage(newDatasetWizard);
        pageList.add(hallPage);

        LaserTargetBeamPage laserPage = new LaserTargetBeamPage(newDatasetWizard);
        pageList.add(laserPage);

        IonChamberPage icPage = new IonChamberPage(newDatasetWizard);
        pageList.add(icPage);
        
        ReviewPage reviewPage = new ReviewPage(newDatasetWizard);
        pageList.add(reviewPage);

        newDatasetWizard.setPageList(pageList);

        newDatasetWizard.setTitle("New Hall Calibration Dataset");
    }

    private void initMyComponents() {
        noSampleFileLoaded();

        chartHolderPanel.add(chartPanel, BorderLayout.CENTER);

        marginSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                marginSpinner.setValue(marginSlider.getValue());
                recalculateSetpointAll();
            }
        });

        marginSpinner.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                marginSlider.setValue((Integer) marginSpinner.getValue());
            }
        });

        negativeMarginCheckbox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                recalculateSetpointAll();
            }
        });

        currentSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                currentSpinner.setValue(currentSlider.getValue());
                recalculateSetpointAll();
            }
        });

        currentSpinner.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                currentSlider.setValue((Integer) currentSpinner.getValue());
            }
        });

        marginSlider.setValue(10);
        currentSlider.setValue(5);

        setpointTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent event) {
                if (!event.getValueIsAdjusting()) {
                    handleSelectIonChamber();
                }
            }
        });

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent evt) {
                PromptUnsavedThenContinueActionListener promptAction
                        = new PromptUnsavedThenContinueActionListener(
                                ICalibrateFrame.this,
                                new ActionListener() {
                            @Override
                            public void actionPerformed(ActionEvent e) {
                                doExit();
                            }
                        });

                promptAction.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED,
                        "Exit"));
            }
        });
    }

    private void doExit() {
        /*helpDialog.dispose();
            waitDialog.dispose();
            newDatasetWizard.dispose();
            modifySetpointDialog.dispose();
            progressDialog.dispose();
            laserDialog.dispose();*/

        doClose(); // Stop monitoring

        ICalibrateFrame.this.dispose();
        //System.gc();
        synchronized (ICalibrateFrame.this) {
            this.notifyAll();
        }
    }

    private void initDoseRateTableLayout() {
        UnselectableDoseRateCellRenderer unselectableDoseRateCellRenderer
                = new UnselectableDoseRateCellRenderer();

        MeasuredCurrentCellRenderer currentCellRenderer = new MeasuredCurrentCellRenderer();

        TableColumnModel detailTableColumnModel = detailTable.getColumnModel();

        detailTableColumnModel.getColumn(0).setCellRenderer(currentCellRenderer); // Current
        detailTableColumnModel.getColumn(1).setCellRenderer(unselectableDoseRateCellRenderer); // Dose Rate       

        detailTableColumnModel.getColumn(0).setPreferredWidth(125); // Existing
        detailTableColumnModel.getColumn(1).setPreferredWidth(125); // Calculated  

        detailTableColumnModel.getColumn(0).setMinWidth(125); // Existing
        detailTableColumnModel.getColumn(1).setMinWidth(125); // Calculated  
    }

    private void initTableFormat() {
        setpointTable.getTableHeader().setReorderingAllowed(false);
        setpointTable.getTableHeader().setResizingAllowed(false);

        detailTable.getTableHeader().setReorderingAllowed(false);
        detailTable.getTableHeader().setResizingAllowed(false);
        detailTable.setRowSelectionAllowed(false);
        detailTable.setFocusable(false);

        TableColumnModel setpointTableColumnModel = setpointTable.getColumnModel();

        UnselectableCellRenderer unselectableRenderer = new UnselectableCellRenderer();

        UnselectableDoseRateCellRenderer unselectableDoseRateCellRenderer
                = new UnselectableDoseRateCellRenderer();

        // Set header height due to multi-line headers
        detailTable.getTableHeader().setPreferredSize(new Dimension(125, 50));
        setpointTable.getTableHeader().setPreferredSize(new Dimension(125, 50));

        // Right align results table number columns          
        setpointTableColumnModel.getColumn(0).setCellRenderer(unselectableRenderer); // Name
        setpointTableColumnModel.getColumn(1).setCellRenderer(unselectableDoseRateCellRenderer); // Existing        
        setpointTableColumnModel.getColumn(2).setCellRenderer(unselectableDoseRateCellRenderer); // Calculated
        //setpointTableColumnModel.getColumn(3).setCellRenderer(); // Logarithmic

        // Set results table column width       
        setpointTableColumnModel.getColumn(0).setPreferredWidth(125); // Name
        setpointTableColumnModel.getColumn(1).setPreferredWidth(125); // Existing
        setpointTableColumnModel.getColumn(2).setPreferredWidth(125); // Calculated    
        setpointTableColumnModel.getColumn(3).setPreferredWidth(50); // Logarithmic 

        setpointTableColumnModel.getColumn(0).setMinWidth(125); // Name
        setpointTableColumnModel.getColumn(1).setMinWidth(125); // Existing
        setpointTableColumnModel.getColumn(2).setMinWidth(125); // Calculated       
        setpointTableColumnModel.getColumn(3).setMinWidth(50); // Logarithmic

        //setpointTableColumnModel.getColumn(0).setMaxWidth(125); // Name
        setpointTableColumnModel.getColumn(1).setMaxWidth(125); // Existing
        setpointTableColumnModel.getColumn(2).setMaxWidth(125); // Calculated   
        setpointTableColumnModel.getColumn(3).setMaxWidth(50); // Logarithmic

        initDoseRateTableLayout();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        samplePanel = new javax.swing.JPanel();
        setpointPanel = new javax.swing.JPanel();
        setpointScrollPane = new javax.swing.JScrollPane();
        setpointTable = new javax.swing.JTable();
        settingsPanel = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        currentUnitsLabel = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        marginSlider = new javax.swing.JSlider();
        marginSpinner = new javax.swing.JSpinner();
        jSeparator1 = new javax.swing.JSeparator();
        currentSlider = new javax.swing.JSlider();
        currentSpinner = new javax.swing.JSpinner();
        negativeMarginCheckbox = new javax.swing.JCheckBox();
        detailPanel = new javax.swing.JPanel();
        jPanel3 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        detailTable = new javax.swing.JTable();
        chartHolderPanel = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        fitEquationLabel = new javax.swing.JLabel();
        fitEquationDescription = new javax.swing.JLabel();
        modifyDataButton = new javax.swing.JButton();
        r2Description = new javax.swing.JLabel();
        r2Label = new javax.swing.JLabel();
        jPanel1 = new javax.swing.JPanel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        hallLabel = new javax.swing.JLabel();
        targetLabel = new javax.swing.JLabel();
        calibratedDateLabel = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        calibratedByLabel = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        jLabel10 = new javax.swing.JLabel();
        passLabel = new javax.swing.JLabel();
        noteLabel = new javax.swing.JLabel();
        menuBar = new javax.swing.JMenuBar();
        fileMenu = new javax.swing.JMenu();
        newDatasetMenuItem = new javax.swing.JMenuItem();
        openMenuItem = new javax.swing.JMenuItem();
        jSeparator4 = new javax.swing.JPopupMenu.Separator();
        saveMenuItem = new javax.swing.JMenuItem();
        closeMenuItem = new javax.swing.JMenuItem();
        jSeparator2 = new javax.swing.JPopupMenu.Separator();
        exportelogMenuItem = new javax.swing.JMenuItem();
        exportSnapMenuItem = new javax.swing.JMenuItem();
        exportEpicsMenuItem = new javax.swing.JMenuItem();
        jSeparator3 = new javax.swing.JPopupMenu.Separator();
        exitMenuItem = new javax.swing.JMenuItem();
        editMenu = new javax.swing.JMenu();
        dataMenuItem = new javax.swing.JMenuItem();
        helpMenu = new javax.swing.JMenu();
        aboutMenuItem = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("iCalibrate");
        setMinimumSize(new java.awt.Dimension(1150, 875));
        setName("calibrateFrame"); // NOI18N

        setpointPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Trip Setpoints"));

        setpointTable.setModel(setpointTableModel);
        setpointTable.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        setpointScrollPane.setViewportView(setpointTable);

        javax.swing.GroupLayout setpointPanelLayout = new javax.swing.GroupLayout(setpointPanel);
        setpointPanel.setLayout(setpointPanelLayout);
        setpointPanelLayout.setHorizontalGroup(
            setpointPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(setpointPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(setpointScrollPane)
                .addContainerGap())
        );
        setpointPanelLayout.setVerticalGroup(
            setpointPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(setpointPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(setpointScrollPane, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                .addContainerGap())
        );

        settingsPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Calculation Parameters"));

        jLabel1.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        jLabel1.setText("Beam Current:");

        jLabel2.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        jLabel2.setText("Margin:");

        currentUnitsLabel.setText("microAmps");

        jLabel6.setText("%");

        marginSlider.setMajorTickSpacing(10);
        marginSlider.setMaximum(50);
        marginSlider.setMinorTickSpacing(1);
        marginSlider.setPaintLabels(true);
        marginSlider.setPaintTicks(true);
        marginSlider.setToolTipText("");
        marginSlider.setValue(0);

        marginSpinner.setModel(new javax.swing.SpinnerNumberModel(0, 0, 50, 1));

        currentSlider.setMajorTickSpacing(10);
        currentSlider.setMinorTickSpacing(5);
        currentSlider.setPaintLabels(true);
        currentSlider.setPaintTicks(true);
        currentSlider.setValue(0);

        currentSpinner.setModel(new javax.swing.SpinnerNumberModel(0, 0, 100, 1));
        currentSpinner.setMinimumSize(new java.awt.Dimension(75, 20));
        currentSpinner.setName(""); // NOI18N
        currentSpinner.setPreferredSize(new java.awt.Dimension(75, 20));

        negativeMarginCheckbox.setText("Negative");

        javax.swing.GroupLayout settingsPanelLayout = new javax.swing.GroupLayout(settingsPanel);
        settingsPanel.setLayout(settingsPanelLayout);
        settingsPanelLayout.setHorizontalGroup(
            settingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(settingsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(settingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jSeparator1, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(marginSlider, javax.swing.GroupLayout.DEFAULT_SIZE, 314, Short.MAX_VALUE)
                    .addComponent(currentSlider, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(settingsPanelLayout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(currentSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, 75, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(currentUnitsLabel)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(settingsPanelLayout.createSequentialGroup()
                        .addComponent(jLabel2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(marginSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, 55, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel6)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(negativeMarginCheckbox)))
                .addContainerGap())
        );
        settingsPanelLayout.setVerticalGroup(
            settingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(settingsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(settingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(currentUnitsLabel)
                    .addComponent(jLabel1)
                    .addComponent(currentSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(currentSlider, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(33, 33, 33)
                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addGroup(settingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(marginSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel2)
                    .addComponent(jLabel6)
                    .addComponent(negativeMarginCheckbox))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(marginSlider, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(46, 46, 46))
        );

        detailPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Fit and Data"));

        detailTable.setModel(doseRateTableModel);
        detailTable.setRowSelectionAllowed(false);
        jScrollPane1.setViewportView(detailTable);

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                .addGap(0, 0, Short.MAX_VALUE)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 361, javax.swing.GroupLayout.PREFERRED_SIZE))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 273, Short.MAX_VALUE)
        );

        chartHolderPanel.setLayout(new java.awt.BorderLayout());

        fitEquationLabel.setText("y = mx + b");

        fitEquationDescription.setText("Linear Fit Equation:");
        fitEquationDescription.setToolTipText("");

        modifyDataButton.setText("Modify Data");

        r2Description.setText("<html>R<sup>2</sup>:</html>");
        r2Description.setToolTipText("");

        r2Label.setText("0");

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(0, 0, Short.MAX_VALUE)
                .addComponent(modifyDataButton))
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(r2Description, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(fitEquationDescription))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(fitEquationLabel)
                    .addComponent(r2Label))
                .addGap(0, 0, Short.MAX_VALUE))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                .addComponent(modifyDataButton)
                .addGap(17, 17, 17)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(fitEquationLabel)
                    .addComponent(fitEquationDescription))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(r2Description, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(r2Label))
                .addContainerGap(99, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout detailPanelLayout = new javax.swing.GroupLayout(detailPanel);
        detailPanel.setLayout(detailPanelLayout);
        detailPanelLayout.setHorizontalGroup(
            detailPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(detailPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(chartHolderPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(detailPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        detailPanelLayout.setVerticalGroup(
            detailPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, detailPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(detailPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(detailPanelLayout.createSequentialGroup()
                        .addComponent(chartHolderPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addContainerGap())
                    .addGroup(detailPanelLayout.createSequentialGroup()
                        .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
        );

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder("Dataset Properties"));

        jLabel3.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        jLabel3.setText("Hall:");

        jLabel4.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        jLabel4.setText("Target:");

        jLabel7.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        jLabel7.setText("Calibrated On:");

        hallLabel.setText("Unknown");

        targetLabel.setText("Unknown");
        targetLabel.setVerticalAlignment(javax.swing.SwingConstants.TOP);

        calibratedDateLabel.setText("Unknown");

        jLabel5.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        jLabel5.setText("Calibrated By:");

        calibratedByLabel.setText("Unknown");

        jLabel8.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        jLabel8.setText("Pass:");

        jLabel10.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        jLabel10.setText("Note:");

        passLabel.setText("Unknown");

        noteLabel.setText("Unknown");
        noteLabel.setVerticalAlignment(javax.swing.SwingConstants.TOP);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel5)
                    .addComponent(jLabel7)
                    .addComponent(jLabel10)
                    .addComponent(jLabel8)
                    .addComponent(jLabel4)
                    .addComponent(jLabel3))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(targetLabel)
                    .addComponent(hallLabel)
                    .addComponent(passLabel)
                    .addComponent(noteLabel)
                    .addComponent(calibratedDateLabel)
                    .addComponent(calibratedByLabel))
                .addContainerGap(169, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(hallLabel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4)
                    .addComponent(targetLabel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel8)
                    .addComponent(passLabel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel10)
                    .addComponent(noteLabel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel7)
                    .addComponent(calibratedDateLabel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel5)
                    .addComponent(calibratedByLabel))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout samplePanelLayout = new javax.swing.GroupLayout(samplePanel);
        samplePanel.setLayout(samplePanelLayout);
        samplePanelLayout.setHorizontalGroup(
            samplePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, samplePanelLayout.createSequentialGroup()
                .addGroup(samplePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(detailPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(samplePanelLayout.createSequentialGroup()
                        .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(setpointPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(settingsPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        samplePanelLayout.setVerticalGroup(
            samplePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(samplePanelLayout.createSequentialGroup()
                .addGroup(samplePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(setpointPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(settingsPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(detailPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        fileMenu.setText("File");

        newDatasetMenuItem.setText("New...");
        fileMenu.add(newDatasetMenuItem);

        openMenuItem.setText("Open...");
        fileMenu.add(openMenuItem);
        fileMenu.add(jSeparator4);

        saveMenuItem.setText("Save...");
        fileMenu.add(saveMenuItem);

        closeMenuItem.setText("Close");
        fileMenu.add(closeMenuItem);
        fileMenu.add(jSeparator2);

        exportelogMenuItem.setText("Export to elog...");
        exportelogMenuItem.setToolTipText("");
        fileMenu.add(exportelogMenuItem);

        exportSnapMenuItem.setText("Export Snap File...");
        fileMenu.add(exportSnapMenuItem);

        exportEpicsMenuItem.setText("Export to EPICS...");
        fileMenu.add(exportEpicsMenuItem);
        fileMenu.add(jSeparator3);

        exitMenuItem.setText("Exit");
        fileMenu.add(exitMenuItem);

        menuBar.add(fileMenu);

        editMenu.setText("Edit");

        dataMenuItem.setText("Modify Data");
        dataMenuItem.setToolTipText("");
        editMenu.add(dataMenuItem);

        menuBar.add(editMenu);

        helpMenu.setText("Help");

        aboutMenuItem.setText("About");
        helpMenu.add(aboutMenuItem);

        menuBar.add(helpMenu);

        setJMenuBar(menuBar);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(samplePanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(samplePanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void sampleFileLoaded() {
        samplePanel.setVisible(true);
        closeMenuItem.setEnabled(true);
        editMenu.setEnabled(true);
        exportelogMenuItem.setEnabled(true);
        exportSnapMenuItem.setEnabled(true);
        exportEpicsMenuItem.setEnabled(true);
        saveMenuItem.setEnabled(!persisted);
    }

    private void noSampleFileLoaded() {
        samplePanel.setVisible(false);
        closeMenuItem.setEnabled(false);
        editMenu.setEnabled(false);
        exportelogMenuItem.setEnabled(false);
        exportSnapMenuItem.setEnabled(false);
        exportEpicsMenuItem.setEnabled(false);
        saveMenuItem.setEnabled(false);
    }

    /**
     * Set the HallCalibrationDataset.
     *
     * @param dataset The dataset
     * @param filename The file name of the dataset
     * @param persisted Whether the dataset has been persisted to file or not
     */
    public void setDataset(HallCalibrationDataset dataset, String filename, boolean persisted) {
        this.dataset = dataset;
        this.persisted = persisted;
        this.filename = filename;

        hallLabel.setText(dataset.getHall().name());
        targetLabel.setText("<html>" + dataset.getTarget() + "</html>");
        passLabel.setText(dataset.getPass());
        noteLabel.setText("<html>" + dataset.getNote() + "</html>");

        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MMM-dd HH:mm");
        calibratedDateLabel.setText(formatter.format(dataset.getCalibratedDate()));
        calibratedByLabel.setText(dataset.getCalibratedBy());

        LinkedHashSet<SetpointRow> rows = new LinkedHashSet<>();

        chartDatasetList = new ArrayList<>();

        initHallParameters(dataset.getHall());

        int current = currentSlider.getValue();
        int margin = marginSlider.getValue();

        Set<String> pvSet = new HashSet<>();

        if (dataset.getMeasuredDoseRateDataset() != null) {
            int rowIndex = 0;

            for (IonChamberDataset icDataset : dataset.getMeasuredDoseRateDataset()) {
                ChartDataset data = new ChartDataset(icDataset, current, margin);

                data.setLogarithmicSelected(true);

                chartDatasetList.add(data);

                String pv = icDataset.getIonChamber().getDoseRateSetpointReadPvName();
                pvSet.add(pv);
                pvToRowIndexMap.put(pv, rowIndex++);

                IonChamber ic = icDataset.getIonChamber();
                Double existingDoseRate = null;
                double calculatedDoseRate = data.getSetpoint();
                if (Double.isNaN(calculatedDoseRate)) {
                    calculatedDoseRate = 0.0;
                }
                SetpointRow row = new SetpointRow(ic, existingDoseRate, calculatedDoseRate);
                rows.add(row);
            }

            if (channelManager != null) {
                channelManager.addPvs(this, pvSet);
            }
        }

        setpointTableModel.setRows(rows);

        selectIonChamberInSetpointTable(0);
        //handleSelectIonChamber(); // Already triggered by above

        setpointTableModel.setLogAll();

        String target = dataset.getTarget();

        if (target == null) {
            target = "";
        }

        target = target.trim();

        String[] negativeTargets = negativeTargetsMap.get(dataset.getHall());

        if (negativeTargets == null) {
            negativeTargets = new String[0];
        }

        negativeMarginCheckbox.setSelected(Arrays.asList(negativeTargets).contains(target));

        updateTitleBar();
        sampleFileLoaded();
    }

    private void updateTitleBar() {
        String titleBar = filename;

        if (!persisted) {
            titleBar = titleBar + "*";
        }

        this.setTitle(titleBar + " - iCalibrate");
    }

    /**
     * Update the sample data with the specified values.
     *
     * @param data The new data
     */
    public void updateSampleData(List<DoseRateMeasurement> data) {

        Collections.sort(data);

        setStateSaved(false);

        int index = setpointTable.getSelectedRow();
        ChartDataset selected = chartDatasetList.get(index);
        int current = currentSlider.getValue();
        int margin = marginSlider.getValue();
        boolean logarithmic = selected.isLogarithmicSelected();
        IonChamber chamber = selected.getMeasuredDataset().getIonChamber();
        IonChamberDataset ds = new IonChamberDataset(chamber, data);
        selected = new ChartDataset(ds, current, margin);
        selected.setLogarithmicSelected(logarithmic);
        chartDatasetList.set(index, selected);
        String currentUnits = currentUnitsLabel.getText();
        chartPanel.setDataset(selected, currentUnits);
        dataset.getMeasuredDoseRateDataset().set(index, ds);
        handleSelectIonChamber();
        recalculateFit();
    }

    private void updateEquation(ChartDataset dataset) {
        String equation = dataset.getFitEquation();
        String r2 = dataset.getRSquareLabel();

        fitEquationLabel.setText(equation);
        r2Label.setText(r2);

        if (dataset.isLogarithmicSelected()) { // Logarithmic
            fitEquationDescription.setText("Logarithmic Fit Equation: ");
        } else { // Linear
            fitEquationDescription.setText("Linear Fit Equation: ");
        }
    }

    private void recalculateFit() {
        if (chartDatasetList != null) {
            int index = setpointTable.getSelectedRow();
            if (index != -1 && chartDatasetList.size() > index) {
                ChartDataset selected = chartDatasetList.get(index);

                updateEquation(selected);

                int current = currentSlider.getValue();
                int margin = marginSlider.getValue();

                recalculateSetpoint(selected, index, current, margin);
            }
        }
    }

    private void recalculateSetpoint(ChartDataset ds, int index, int current, int margin) {
        ds.updateSetpointParameters(current, margin);

        //System.out.println(ds.getMeasuredDataset().getIonChamber().getFullName() + " logarithmic: " + ds.isLogarithmicSelected());
        chartPanel.drawFit();

        double calculatedDoseRate = ds.getSetpoint();
        if (Double.isNaN(calculatedDoseRate) || calculatedDoseRate < 0) {
            calculatedDoseRate = 0.0;
        }
        setpointTableModel.setValueAt(calculatedDoseRate, index, 2);
    }

    private void recalculateSetpointAll() {
        if (chartDatasetList != null) {
            int current = currentSlider.getValue();
            int margin = marginSlider.getValue();
            boolean negative = negativeMarginCheckbox.isSelected();

            if (negative) {
                margin = margin * -1;
            }

            for (int i = 0; i < chartDatasetList.size(); i++) {

                ChartDataset ds = chartDatasetList.get(i);

                //System.out.println("recalculateSetpointAll: " + ds.getMeasuredDataset().getIonChamber().getFullName() + " logarithmic: " + ds.isLogarithmicSelected());                           
                recalculateSetpoint(ds, i, current, margin);
            }
        }
    }

    private void handleSelectIonChamber() {
        int index = setpointTable.getSelectedRow();
        String currentUnits = currentUnitsLabel.getText();
        if (index != -1 && chartDatasetList.size() > index) {
            ChartDataset selected = chartDatasetList.get(index);
            doseRateTableModel.setRows(new LinkedHashSet<>(
                    selected.getMeasuredDataset().getMeasurementList()));

            //System.out.println("handleSelectIonChamber: " + selected.getMeasuredDataset().getIonChamber().getFullName() + " logarithmic: " + selected.isLogarithmicSelected());
            chartPanel.setDataset(selected, currentUnits);

            updateEquation(selected);
        } else {
            doseRateTableModel.setRows(new LinkedHashSet<>());
            chartPanel.setDataset(null, null);
            fitEquationLabel.setText("y = mx + b");
        }
    }

    /**
     * Selects an ion chamber in the setpoint table.
     *
     * @param rowIndex The cavity to select
     */
    private void selectIonChamberInSetpointTable(int rowIndex) {
        setpointTable.getSelectionModel().setSelectionInterval(rowIndex,
                rowIndex);
        setpointTable.scrollRectToVisible(
                setpointTable.getCellRect(rowIndex, 0,
                        true));
    }

    private void doClose() {
        noSampleFileLoaded();
        dataset = null;
        chartDatasetList = null;
        setTitle("iCalibrate");
        if (!pvToRowIndexMap.isEmpty() && channelManager != null) {
            channelManager.clearPvs(this, pvToRowIndexMap.keySet());
            pvToRowIndexMap.clear();
        }
    }

    /**
     * Set the current parameter used to calculate setpoints.
     *
     * @param current The current
     */
    public void setCurrentParameter(int current) {
        currentSlider.setValue(current);
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JMenuItem aboutMenuItem;
    private javax.swing.JLabel calibratedByLabel;
    private javax.swing.JLabel calibratedDateLabel;
    private javax.swing.JPanel chartHolderPanel;
    private javax.swing.JMenuItem closeMenuItem;
    private javax.swing.JSlider currentSlider;
    private javax.swing.JSpinner currentSpinner;
    private javax.swing.JLabel currentUnitsLabel;
    private javax.swing.JMenuItem dataMenuItem;
    private javax.swing.JPanel detailPanel;
    private javax.swing.JTable detailTable;
    private javax.swing.JMenu editMenu;
    private javax.swing.JMenuItem exitMenuItem;
    private javax.swing.JMenuItem exportEpicsMenuItem;
    private javax.swing.JMenuItem exportSnapMenuItem;
    private javax.swing.JMenuItem exportelogMenuItem;
    private javax.swing.JMenu fileMenu;
    private javax.swing.JLabel fitEquationDescription;
    private javax.swing.JLabel fitEquationLabel;
    private javax.swing.JLabel hallLabel;
    private javax.swing.JMenu helpMenu;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JPopupMenu.Separator jSeparator2;
    private javax.swing.JPopupMenu.Separator jSeparator3;
    private javax.swing.JPopupMenu.Separator jSeparator4;
    private javax.swing.JSlider marginSlider;
    private javax.swing.JSpinner marginSpinner;
    private javax.swing.JMenuBar menuBar;
    private javax.swing.JButton modifyDataButton;
    private javax.swing.JCheckBox negativeMarginCheckbox;
    private javax.swing.JMenuItem newDatasetMenuItem;
    private javax.swing.JLabel noteLabel;
    private javax.swing.JMenuItem openMenuItem;
    private javax.swing.JLabel passLabel;
    private javax.swing.JLabel r2Description;
    private javax.swing.JLabel r2Label;
    private javax.swing.JPanel samplePanel;
    private javax.swing.JMenuItem saveMenuItem;
    private javax.swing.JPanel setpointPanel;
    private javax.swing.JScrollPane setpointScrollPane;
    private javax.swing.JTable setpointTable;
    private javax.swing.JPanel settingsPanel;
    private javax.swing.JLabel targetLabel;
    // End of variables declaration//GEN-END:variables

    /**
     * Closes the currently loaded dataset. This method is safe to use even if
     * no dataset is loaded. This method will not prompt the user to save any
     * unsaved changes so should be used with care.
     */
    public void closeHallCalibrationDataset() {
        doClose();
    }

    @Override
    public void notifyPvInfo(String pv, boolean couldConnect, DBRType type, Integer count,
            String[] enumLabels) {
        //LOGGER.log(Level.FINEST, "PV info: {0}, {1}", new Object[] {pv, type});

        //EventQueue.invokeLater(new Runnable() {});
    }

    @Override
    public void notifyPvUpdate(String pv, DBR dbr) {
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                //LOGGER.log(Level.FINEST, "PV update: {0}", pv);

                Double value = null;

                if (dbr != null && dbr.isDOUBLE()) {
                    value = ((gov.aps.jca.dbr.DOUBLE) dbr).getDoubleValue()[0];
                } else {
                    LOGGER.log(Level.WARNING, "Value is null or not a double");
                }

                Integer rowIndex = pvToRowIndexMap.get(pv);

                if (rowIndex > -1 && rowIndex < setpointTableModel.getRowCount()) {
                    setpointTableModel.setValueAt(value, rowIndex, 1);
                }
            }
        });
    }

    /**
     * Return the EPICS channel manager.
     *
     * @return The channel manager
     */
    public ChannelManager getChannelManager() {
        return channelManager;
    }

    /**
     * Return the current Hall Calibration Dataset filename.
     *
     * @return The filename
     */
    public String getFilename() {
        return filename;
    }

    /**
     * Return the chart dataset list.
     *
     * @return The chart dataset list
     */
    public List<ChartDataset> getChartDatasetList() {
        return chartDatasetList;
    }

    /**
     * Return the ChooseAndModifySetpointDialog.
     *
     * @return The ChooseAndModifySetpointDialog
     */
    public ChooseAndModifySetpointDialog getModifySetpointDialog() {
        return modifySetpointDialog;
    }

    private void initHallParameters(Hall hall) {
        if (hall != null) {
            String currentUnits = null;
            Integer maxCurrent = null;
            Integer maxMargin = null;
            String[] negativeTargets = null;

            try {
                switch (hall) {
                    case A:
                        currentUnits = ICalibrateApp.APP_PROPERTIES.getProperty(
                                "HALLA_CURRENT_UNITS");
                        maxCurrent = Integer.parseInt(ICalibrateApp.APP_PROPERTIES.getProperty(
                                "HALLA_MAX_CURRENT"));
                        maxMargin = Integer.parseInt(ICalibrateApp.APP_PROPERTIES.getProperty(
                                "HALLA_MAX_MARGIN"));
                        negativeTargets = ICalibrateApp.APP_PROPERTIES.getProperty("HALLA_NEGATIVE_MARGIN_TARGET_CSV").split(",");
                        break;
                    case C:
                        currentUnits = ICalibrateApp.APP_PROPERTIES.getProperty(
                                "HALLC_CURRENT_UNITS");
                        maxCurrent = Integer.parseInt(ICalibrateApp.APP_PROPERTIES.getProperty(
                                "HALLC_MAX_CURRENT"));
                        maxMargin = Integer.parseInt(ICalibrateApp.APP_PROPERTIES.getProperty(
                                "HALLC_MAX_MARGIN"));
                        negativeTargets = ICalibrateApp.APP_PROPERTIES.getProperty("HALLC_NEGATIVE_MARGIN_TARGET_CSV").split(",");
                        break;
                    case D:
                        currentUnits = ICalibrateApp.APP_PROPERTIES.getProperty(
                                "HALLD_CURRENT_UNITS");
                        maxCurrent = Integer.parseInt(ICalibrateApp.APP_PROPERTIES.getProperty(
                                "HALLD_MAX_CURRENT"));
                        maxMargin = Integer.parseInt(ICalibrateApp.APP_PROPERTIES.getProperty(
                                "HALLD_MAX_MARGIN"));
                        negativeTargets = ICalibrateApp.APP_PROPERTIES.getProperty("HALLD_NEGATIVE_MARGIN_TARGET_CSV").split(",");
                        break;
                    default:
                        throw new IllegalArgumentException("Hall must be one of A, C, D");
                }

                if (currentUnits == null) {
                    throw new IllegalArgumentException(
                            "Current units not specified in configuration");
                }

                if (maxCurrent == null) {
                    throw new IllegalArgumentException("Max current not specified in configuration");
                }

                if (maxMargin == null) {
                    throw new IllegalArgumentException("Max margin not specified in configuration");
                }

                if (negativeTargets != null) {
                    negativeTargetsMap.put(hall, negativeTargets);
                }

                int numMajorTicks = 8; 
                
                if(maxCurrent >= 1000) {
                    numMajorTicks = 6;
                }
                
                int major = maxCurrent / numMajorTicks;
                int minor = 0; // none
                if (maxCurrent <= 100) {
                    minor = (int) (major / 2.0d);
                    if ((major % 2.0d) != 0) {
                        minor = (int) (major / 3.0d);
                    }
                }
                currentSlider.setMaximum(maxCurrent);
                currentSlider.setMajorTickSpacing(major);
                currentSlider.setMinorTickSpacing(minor);
                currentSlider.setLabelTable(currentSlider.createStandardLabels(major));
                ((SpinnerNumberModel) currentSpinner.getModel()).setMaximum(maxCurrent);

                major = (int) (maxMargin / 10.0d);
                minor = 0; // none
                if (maxMargin <= 100) {
                    minor = (int) (major / 2.0d);
                    if ((major % 2.0d) != 0) {
                        minor = (int) (major / 3.0d);
                    }
                }
                marginSlider.setMaximum(maxMargin);
                marginSlider.setMajorTickSpacing(major);
                marginSlider.setMinorTickSpacing(minor);
                marginSlider.setLabelTable(marginSlider.createStandardLabels(major));
                ((SpinnerNumberModel) marginSpinner.getModel()).setMaximum(maxMargin);

                currentUnitsLabel.setText(currentUnits);

                doseRateTableModel.setCurrentUnits(currentUnits);
                initDoseRateTableLayout();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, e.getMessage(),
                        "Unable to read configuration file", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Return true if there is unsaved data.
     *
     * @return true if unsaved data, false otherwise
     */
    public boolean isUnsavedData() {
        return (!persisted && dataset != null);
    }

    /**
     * Return the HallCalibrationDataset.
     *
     * @return The HallCalibrationDataset
     */
    public HallCalibrationDataset getDataset() {
        return dataset;
    }

    /**
     * Sets the persisted state
     *
     * @param persisted true or false
     */
    public void setStateSaved(boolean persisted) {
        this.persisted = persisted;
        saveMenuItem.setEnabled(!persisted);

        updateTitleBar();
    }

    /**
     * Return the current units.
     *
     * @return The units
     */
    public String getCurrentUnits() {
        return currentUnitsLabel.getText();
    }

    /**
     * Return the current.
     *
     * @return The current
     */
    public int getCurrent() {
        return currentSlider.getValue();
    }

    /**
     * Return the margin.
     *
     * @return The margin
     */
    public int getMargin() {
        return marginSlider.getValue();
    }

    /**
     * Return the signed margin.
     *
     * @return The signed margin
     */
    public int getSignedMargin() {
        int value = marginSlider.getValue();
        
        if(negativeMarginCheckbox.isSelected()) {
            value = value * -1;
        }
        
        return value;
    }

    /**
     * Return the setpoint for the row in the setpoint table at the provided index.
     *
     * @param index The setpoint table index
     * @return The setpoint value
     */
    public Double getControlSystemSetpoint(int index) {
        return (Double) setpointTableModel.getValueAt(index, 1);
    }
}
