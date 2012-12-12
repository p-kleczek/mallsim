package sim.gui;

import java.awt.BorderLayout;

import javax.swing.ButtonGroup;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.BevelBorder;
import javax.swing.border.EmptyBorder;

import sim.MallSim;
import sim.control.GuiState;
import sim.control.GuiState.BackgroundPolicy;
import sim.control.Listeners;
import sim.control.GuiState.DrawTargetLinePolicy;
import sim.gui.actions.ExitAction;
import sim.model.Mall;
import javax.swing.JCheckBox;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.BoxLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import javax.swing.border.TitledBorder;
import javax.swing.JRadioButton;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import javax.swing.JSlider;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;
import javax.swing.JToggleButton;

@SuppressWarnings("serial")
public class MallFrame extends JFrame {

    private JPanel contentPane;
    private GUIBoard guiBoard;


    /**
     * Create the frame.
     */
    public MallFrame(Mall mall) {
        setTitle("MallSim");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setBounds(100, 100, 650, 500);

        JMenuBar menuBar = new JMenuBar();
        setJMenuBar(menuBar);

        JMenu mnFile = new JMenu("File");
        mnFile.setMnemonic('F');
        menuBar.add(mnFile);

        mnFile.add(new ExitAction());

        JMenu mnSimulation = new JMenu("Simulation");
        mnSimulation.setMnemonic('S');
        menuBar.add(mnSimulation);

        JMenuItem mntmLoadMall = new JMenuItem("Load mall...");
        mnSimulation.add(mntmLoadMall);

        JMenuItem mntmRestart = new JMenuItem("Restart");
        mntmRestart.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent arg0) {
                MallSim.runAlgoTest();
            }
        });
        mnSimulation.add(mntmRestart);

        JMenu mnHelp = new JMenu("Help");
        mnHelp.setMnemonic('H');
        menuBar.add(mnHelp);

        JMenuItem mntmAbout = new JMenuItem("About");
        mnHelp.add(mntmAbout);
        contentPane = new JPanel();
        contentPane.setBorder(new EmptyBorder(0, 0, 0, 0));
        setContentPane(contentPane);
        contentPane.setLayout(new BorderLayout(0, 0));

        JPanel statusBar = new JPanel();
        statusBar.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
        contentPane.add(statusBar, BorderLayout.SOUTH);

        JSplitPane splitPane = new JSplitPane();
        splitPane.setResizeWeight(1.0);
        contentPane.add(splitPane, BorderLayout.CENTER);
        splitPane.setDividerLocation(this.getBounds().width / 2);  // XXX: temp
        // splitPane.setDividerLocation(0.8); // XXX: docelowa wersja

        JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP);
        tabbedPane.setSelectedIndex(-1);
        splitPane.setRightComponent(tabbedPane);

        JPanel tabDisplay = new JPanel();
        tabbedPane.addTab("Display", null, tabDisplay, null);
        GridBagLayout gbl_tabDisplay = new GridBagLayout();
        gbl_tabDisplay.columnWidths = new int[] { 306, 0 };
        gbl_tabDisplay.rowHeights = new int[] { 0, 0, 0, 0, 0, 0, 0 };
        gbl_tabDisplay.columnWeights = new double[] { 1.0, Double.MIN_VALUE };
        gbl_tabDisplay.rowWeights = new double[] { 0.0, 0.0, 0.0, 0.0, 1.0, 1.0, Double.MIN_VALUE };
        tabDisplay.setLayout(gbl_tabDisplay);

        JPanel backgroundContentPanel = new JPanel();
        backgroundContentPanel.setBorder(new TitledBorder(null, "Background content", TitledBorder.LEADING, TitledBorder.TOP, null, null));
        GridBagConstraints gbc_backgroundContentPanel = new GridBagConstraints();
        gbc_backgroundContentPanel.insets = new Insets(0, 0, 5, 0);
        gbc_backgroundContentPanel.fill = GridBagConstraints.BOTH;
        gbc_backgroundContentPanel.gridx = 0;
        gbc_backgroundContentPanel.gridy = 0;
        tabDisplay.add(backgroundContentPanel, gbc_backgroundContentPanel);
        backgroundContentPanel.setLayout(new BoxLayout(backgroundContentPanel, BoxLayout.Y_AXIS));

        JRadioButton rdbtnBgNone = new JRadioButton("none");
        rdbtnBgNone.setActionCommand(BackgroundPolicy.NONE.name());
        rdbtnBgNone.addActionListener(Listeners.backgroundListener);
        backgroundContentPanel.add(rdbtnBgNone);

        JRadioButton rdbtnBgSocialField = new JRadioButton("social field");
        rdbtnBgSocialField.setActionCommand(BackgroundPolicy.SOCIAL_FIELD.name());
        rdbtnBgSocialField.addActionListener(Listeners.backgroundListener);
        backgroundContentPanel.add(rdbtnBgSocialField);

        JRadioButton rdbtnBgVisits = new JRadioButton("visits");
        rdbtnBgVisits.setActionCommand(BackgroundPolicy.VISITS.name());
        rdbtnBgVisits.addActionListener(Listeners.backgroundListener);
        backgroundContentPanel.add(rdbtnBgVisits);


        JRadioButton rdbtnBgMovement = new JRadioButton("movement algorithm");
        rdbtnBgMovement.setActionCommand(BackgroundPolicy.MOVEMENT_ALGORITHM.name());
        rdbtnBgMovement.addActionListener(Listeners.backgroundListener);
        backgroundContentPanel.add(rdbtnBgMovement);

        JRadioButton rdbtnBgFeatures = new JRadioButton("feature map");
        rdbtnBgFeatures.setActionCommand(BackgroundPolicy.FEATURES.name());
        rdbtnBgFeatures.addActionListener(Listeners.backgroundListener);
        backgroundContentPanel.add(rdbtnBgFeatures);

        ButtonGroup backgroundGroup = new ButtonGroup();
        backgroundGroup.add(rdbtnBgNone);
        backgroundGroup.add(rdbtnBgVisits);
        backgroundGroup.add(rdbtnBgSocialField);
        backgroundGroup.add(rdbtnBgMovement);
        backgroundGroup.add(rdbtnBgFeatures);

        switch (GuiState.backgroundPolicy) {
        case NONE:
            rdbtnBgNone.doClick();
            break;
        case SOCIAL_FIELD:
            rdbtnBgSocialField.doClick();
            break;
        case VISITS:
            rdbtnBgVisits.doClick();
            break;
        }

        ButtonGroup targetLinesGroup = new ButtonGroup();

        JPanel targetVectorsPanel = new JPanel();
        targetVectorsPanel.setBorder(new TitledBorder(null, "Target vectors", TitledBorder.LEADING, TitledBorder.TOP,
                null, null));
        GridBagConstraints gbc_targetVectorsPanel = new GridBagConstraints();
        gbc_targetVectorsPanel.insets = new Insets(0, 0, 5, 0);
        gbc_targetVectorsPanel.fill = GridBagConstraints.BOTH;
        gbc_targetVectorsPanel.gridx = 0;
        gbc_targetVectorsPanel.gridy = 2;
        tabDisplay.add(targetVectorsPanel, gbc_targetVectorsPanel);
        targetVectorsPanel.setLayout(new BoxLayout(targetVectorsPanel, BoxLayout.Y_AXIS));

        JRadioButton rdbtnNone = new JRadioButton("none");
        rdbtnNone.setActionCommand(DrawTargetLinePolicy.NONE.name());
        rdbtnNone.addActionListener(Listeners.targetLineListener);
        targetVectorsPanel.add(rdbtnNone);

        JRadioButton rdbtnSelection = new JRadioButton("selection");
        rdbtnSelection.setActionCommand(DrawTargetLinePolicy.SELECTION.name());
        rdbtnSelection.addActionListener(Listeners.targetLineListener);
        targetVectorsPanel.add(rdbtnSelection);

        JRadioButton rdbtnSelectionRoute = new JRadioButton("selection route");
        rdbtnSelectionRoute.setActionCommand(DrawTargetLinePolicy.SELECTION_ROUTE.name());
        rdbtnSelectionRoute.addActionListener(Listeners.targetLineListener);
        targetVectorsPanel.add(rdbtnSelectionRoute);

        JRadioButton rdbtnAll = new JRadioButton("all");
        rdbtnAll.setActionCommand(DrawTargetLinePolicy.ALL.name());
        rdbtnAll.addActionListener(Listeners.targetLineListener);

        targetVectorsPanel.add(rdbtnAll);
        targetLinesGroup.add(rdbtnNone);
        targetLinesGroup.add(rdbtnSelection);
        targetLinesGroup.add(rdbtnSelectionRoute);
        targetLinesGroup.add(rdbtnAll);

        JPanel speedPanel = new JPanel();
        speedPanel.setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null),
                "Animation speed [ms/f]", TitledBorder.LEADING, TitledBorder.TOP, null, null));
        GridBagConstraints gbc_speedPanel = new GridBagConstraints();
        gbc_speedPanel.insets = new Insets(0, 0, 5, 0);
        gbc_speedPanel.fill = GridBagConstraints.BOTH;
        gbc_speedPanel.gridx = 0;
        gbc_speedPanel.gridy = 3;
        tabDisplay.add(speedPanel, gbc_speedPanel);

        JSlider sldSimulationSpeed = new JSlider();
        sldSimulationSpeed.addChangeListener(new ChangeListener() {

            public void stateChanged(ChangeEvent e) {
                GuiState.animationSpeed = ((JSlider) e.getSource()).getValue();
            }
        });

        sldSimulationSpeed.setMajorTickSpacing(150);
        sldSimulationSpeed.setPaintLabels(true);
        sldSimulationSpeed.setPaintTicks(true);
        sldSimulationSpeed.setMaximum(1000);
        sldSimulationSpeed.setMinimum(50);
        speedPanel.add(sldSimulationSpeed);
        sldSimulationSpeed.setName("Simulation speed");
        sldSimulationSpeed.setValue(300);

        JToggleButton tglbtnPause = new JToggleButton("Pause");
        tglbtnPause.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                JToggleButton button = (JToggleButton) e.getSource();

                if (button.isSelected()) {
                    button.setText("Resume");
                    MallSim.getThread().suspend();
                } else {
                    button.setText("Pause");
                    MallSim.getThread().resume();
                }
            }
        });

        GridBagConstraints gbc_tglbtnPause = new GridBagConstraints();
        gbc_tglbtnPause.insets = new Insets(0, 0, 5, 0);
        gbc_tglbtnPause.gridx = 0;
        gbc_tglbtnPause.gridy = 4;
        tabDisplay.add(tglbtnPause, gbc_tglbtnPause);

        switch (GuiState.targetLinePolicy) {
        case NONE:
            rdbtnNone.doClick();
            break;
        case SELECTION:
            rdbtnSelection.doClick();
            break;
        case ALL:
            rdbtnAll.doClick();
            break;
        }

        JPanel panel = new JPanel();
        tabbedPane.addTab("Properties", null, panel, null);

        JTextArea txtrTuIdWszelkie = new JTextArea();
        txtrTuIdWszelkie.setEditable(false);
        txtrTuIdWszelkie.setRows(5);
        txtrTuIdWszelkie
                .setText("--- INFO ---\r\nTu idą wszelkie wyświetlane\r\nwłaściwości aktorów, atraktorów,\r\npłytek itp.");
        panel.add(txtrTuIdWszelkie);

        JScrollPane boardScrollPane = new JScrollPane();
        boardScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        boardScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        splitPane.setLeftComponent(boardScrollPane);

        JPanel boardPanel = new JPanel();
        boardScrollPane.setViewportView(boardPanel);

        guiBoard = new GUIBoard(mall.getBoard());
        boardPanel.add(guiBoard);
    }


    public GUIBoard getBoard() {
        return guiBoard;
    }
}
