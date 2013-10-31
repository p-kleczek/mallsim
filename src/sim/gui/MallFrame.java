package sim.gui;

import java.awt.AWTException;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JToggleButton;
import javax.swing.ScrollPaneConstants;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.BevelBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileFilter;

import sim.MallSim;
import sim.control.GuiState;
import sim.control.GuiState.BackgroundPolicy;
import sim.control.GuiState.DrawTargetLinePolicy;
import sim.control.Listeners;
import sim.gui.actions.ExitAction;
import sim.gui.actions.PauseResumeAction;
import sim.model.Mall;
import sim.model.helpers.Rand;
import sim.util.video.VideoRecorder;

@SuppressWarnings("serial")
public class MallFrame extends JFrame {

	private JPanel contentPane;
	private GUIBoard guiBoard;
	private PropertiesTable propertiesTable;
	private SummaryTable summaryTable;
	private JScrollPane boardScrollPane;
	private final VideoRecorder videoRecorder;
	private JPanel boardPanel = new JPanel();

	private final static Logger LOGGER = Logger
			.getLogger(Logger.GLOBAL_LOGGER_NAME);

	/**
	 * Create the frame.
	 */
	public MallFrame(VideoRecorder aviRecorder) {
		this.videoRecorder = aviRecorder;

		setTitle("MallSim");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		Toolkit toolkit = Toolkit.getDefaultToolkit();
		Dimension screenSize = toolkit.getScreenSize();

		final double TRIM = 0.05; // odstep od krawedzi
		setBounds((int) (screenSize.width * TRIM),
				(int) (screenSize.height * TRIM),
				(int) (screenSize.getWidth() * (1 - 2 * TRIM)),
				(int) (screenSize.getHeight() * (1 - 2 * TRIM)));

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
		mntmLoadMall.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent arg0) {

				// XXX: tymczasowe
				File currentDirectory = new File("./data/malls");
				JFileChooser fc = new JFileChooser(currentDirectory);
				FileFilter filter = new FileFilter() {

					@Override
					public String getDescription() {
						return "MALL files";
					}

					@Override
					public boolean accept(File f) {
						return f.isDirectory()
								|| f.getName().matches(".*_map\\.bmp");
					}
				};
				fc.setFileFilter(filter);

				int returnVal = fc.showOpenDialog(null);

				if (returnVal == JFileChooser.APPROVE_OPTION) {
					MallSim.setMallName(fc.getSelectedFile().getPath());
					MallSim.runSimulation();
				}

			}
		});
		mnSimulation.add(mntmLoadMall);

		JMenuItem mntmRestart = new JMenuItem("Restart");
		mntmRestart.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent arg0) {
				Rand.reseed();
				MallSim.runSimulation();
			}
		});
		mnSimulation.add(mntmRestart);

		JCheckBoxMenuItem chckbxmntmPaused = new JCheckBoxMenuItem(
				new PauseResumeAction());
		mnSimulation.add(chckbxmntmPaused);

		JMenuItem mntmSeed = new JMenuItem("Seed");
		mntmSeed.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				String str = JOptionPane.showInputDialog(null, "Seed:",
						Rand.seed);

				try {
					Long newSeed = Long.valueOf(str);
					Rand.seed = newSeed;
					Rand.setSeed(newSeed);
				} catch (NumberFormatException e) {
					LOGGER.severe("Could not change seed (NumberFormatException)");
				}
			}
		});
		mnSimulation.add(mntmSeed);

		JCheckBoxMenuItem mntmTestMode = new JCheckBoxMenuItem("Test mode");
		mntmTestMode.addItemListener(new ItemListener() {

			@Override
			public void itemStateChanged(ItemEvent e) {
				GuiState.isTestMode = e.getStateChange() == ItemEvent.SELECTED;
			}
		});
		mnSimulation.add(mntmTestMode);

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
		statusBar.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null,
				null, null));
		contentPane.add(statusBar, BorderLayout.SOUTH);

		JSplitPane splitPane = new JSplitPane();
		splitPane.setResizeWeight(1.0);
		contentPane.add(splitPane, BorderLayout.CENTER);
		splitPane.setDividerLocation(0.8);

		JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP);
		tabbedPane.setSelectedIndex(-1);
		splitPane.setRightComponent(tabbedPane);

		createDisplayTab(tabbedPane);
		createPropertiesTab(splitPane, tabbedPane);
		createSummaryTab(tabbedPane);

		createBoardPanel(splitPane);

		tabbedPane.setSelectedIndex(2);

		setDefaults();
	}

	public void setMall(Mall mall) {
		boardPanel.removeAll();
		boardPanel.repaint();
		guiBoard = new GUIBoard(mall.getBoard());
		boardPanel.add(guiBoard);
	}

	private void createBoardPanel(JSplitPane splitPane) {
		boardScrollPane = new JScrollPane();
		boardScrollPane
				.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		boardScrollPane
				.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
		splitPane.setLeftComponent(boardScrollPane);

		boardScrollPane.setViewportView(boardPanel);
	}

	private void createSummaryTab(JTabbedPane tabbedPane) {
		JPanel propertiesPanel = new JPanel();
		tabbedPane.addTab("Summary", null, propertiesPanel, null);
		GridBagLayout gbl_propertiesPanel = new GridBagLayout();
		gbl_propertiesPanel.columnWidths = new int[] { 1, 0 };
		gbl_propertiesPanel.rowHeights = new int[] { 1, 0 };
		gbl_propertiesPanel.columnWeights = new double[] { 1.0,
				Double.MIN_VALUE };
		gbl_propertiesPanel.rowWeights = new double[] { 0.0, Double.MIN_VALUE };
		propertiesPanel.setLayout(gbl_propertiesPanel);

		boardScrollPane = new JScrollPane();
		boardScrollPane
				.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		boardScrollPane
				.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);

		summaryTable = new SummaryTable();
		GridBagConstraints gbc_propertiesTable = new GridBagConstraints();
		gbc_propertiesTable.fill = GridBagConstraints.HORIZONTAL;
		gbc_propertiesTable.gridx = 0;
		gbc_propertiesTable.gridy = 0;
		propertiesPanel.add(summaryTable, gbc_propertiesTable);
	}

	private void createDisplayTab(JTabbedPane tabbedPane) {
		JPanel tabDisplay = new JPanel();
		tabbedPane.addTab("Display", null, tabDisplay, null);
		GridBagLayout gbl_tabDisplay = new GridBagLayout();
		gbl_tabDisplay.columnWidths = new int[] { 306, 0 };
		gbl_tabDisplay.rowHeights = new int[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
		gbl_tabDisplay.columnWeights = new double[] { 1.0, Double.MIN_VALUE };
		gbl_tabDisplay.rowWeights = new double[] { 0.0, 0.0, 0.0, 0.0, 1.0,
				0.0, 1.0, 0.0, 1.0, Double.MIN_VALUE };
		tabDisplay.setLayout(gbl_tabDisplay);

		JPanel backgroundContentPanel = new JPanel();
		backgroundContentPanel.setBorder(new TitledBorder(null,
				"Background content", TitledBorder.LEADING, TitledBorder.TOP,
				null, null));
		GridBagConstraints gbc_backgroundContentPanel = new GridBagConstraints();
		gbc_backgroundContentPanel.insets = new Insets(0, 0, 5, 0);
		gbc_backgroundContentPanel.fill = GridBagConstraints.BOTH;
		gbc_backgroundContentPanel.gridx = 0;
		gbc_backgroundContentPanel.gridy = 0;
		tabDisplay.add(backgroundContentPanel, gbc_backgroundContentPanel);
		backgroundContentPanel.setLayout(new BoxLayout(backgroundContentPanel,
				BoxLayout.Y_AXIS));

		JRadioButton rdbtnBgNone = new JRadioButton("none");
		rdbtnBgNone.setActionCommand(BackgroundPolicy.NONE.name());
		rdbtnBgNone.addActionListener(Listeners.backgroundListener);
		backgroundContentPanel.add(rdbtnBgNone);

		JRadioButton rdbtnBgSocialField = new JRadioButton("social field");
		rdbtnBgSocialField.setActionCommand(BackgroundPolicy.SOCIAL_FIELD
				.name());
		rdbtnBgSocialField.addActionListener(Listeners.backgroundListener);
		backgroundContentPanel.add(rdbtnBgSocialField);

		JRadioButton rdbtnBgVisits = new JRadioButton("visits");
		rdbtnBgVisits.setActionCommand(BackgroundPolicy.VISITS.name());
		rdbtnBgVisits.addActionListener(Listeners.backgroundListener);
		backgroundContentPanel.add(rdbtnBgVisits);
		JRadioButton rdbtnBgMovement = new JRadioButton("movement algorithm");
		rdbtnBgMovement.setActionCommand(BackgroundPolicy.MOVEMENT_ALGORITHM
				.name());
		rdbtnBgMovement.addActionListener(Listeners.backgroundListener);
		backgroundContentPanel.add(rdbtnBgMovement);

		JRadioButton rdbtnBgFeatures = new JRadioButton("feature map");
		rdbtnBgFeatures.setActionCommand(BackgroundPolicy.FEATURES.name());
		rdbtnBgFeatures.addActionListener(Listeners.backgroundListener);
		backgroundContentPanel.add(rdbtnBgFeatures);

		JRadioButton rdbtnBgLanes = new JRadioButton("lanes");
		rdbtnBgLanes.setActionCommand(BackgroundPolicy.LANES.name());
		rdbtnBgLanes.addActionListener(Listeners.backgroundListener);
		backgroundContentPanel.add(rdbtnBgLanes);

		ButtonGroup backgroundGroup = new ButtonGroup();
		backgroundGroup.add(rdbtnBgNone);
		backgroundGroup.add(rdbtnBgVisits);
		backgroundGroup.add(rdbtnBgSocialField);
		backgroundGroup.add(rdbtnBgMovement);
		backgroundGroup.add(rdbtnBgFeatures);
		backgroundGroup.add(rdbtnBgLanes);

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
		case MOVEMENT_ALGORITHM:
			rdbtnBgMovement.doClick();
			break;
		case FEATURES:
			rdbtnBgFeatures.doClick();
			break;
		case LANES:
			rdbtnBgLanes.doClick();
			break;
		default:
			break;
		}

		ButtonGroup targetLinesGroup = new ButtonGroup();

		JPanel targetVectorsPanel = new JPanel();
		targetVectorsPanel.setBorder(new TitledBorder(null, "Target vectors",
				TitledBorder.LEADING, TitledBorder.TOP, null, null));
		GridBagConstraints gbc_targetVectorsPanel = new GridBagConstraints();
		gbc_targetVectorsPanel.insets = new Insets(0, 0, 5, 0);
		gbc_targetVectorsPanel.fill = GridBagConstraints.BOTH;
		gbc_targetVectorsPanel.gridx = 0;
		gbc_targetVectorsPanel.gridy = 2;
		tabDisplay.add(targetVectorsPanel, gbc_targetVectorsPanel);
		targetVectorsPanel.setLayout(new BoxLayout(targetVectorsPanel,
				BoxLayout.Y_AXIS));

		JRadioButton rdbtnNone = new JRadioButton("none");
		rdbtnNone.setActionCommand(DrawTargetLinePolicy.NONE.name());
		rdbtnNone.addActionListener(Listeners.targetLineListener);
		targetVectorsPanel.add(rdbtnNone);

		JRadioButton rdbtnSelection = new JRadioButton("selection");
		rdbtnSelection.setActionCommand(DrawTargetLinePolicy.SELECTION.name());
		rdbtnSelection.addActionListener(Listeners.targetLineListener);
		targetVectorsPanel.add(rdbtnSelection);

		JRadioButton rdbtnSelectionRoute = new JRadioButton("selection route");
		rdbtnSelectionRoute
				.setActionCommand(DrawTargetLinePolicy.SELECTION_ROUTE.name());
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
		speedPanel.setBorder(new TitledBorder(new EtchedBorder(
				EtchedBorder.LOWERED, null, null), "Animation speed [ms/f]",
				TitledBorder.LEADING, TitledBorder.TOP, null, null));
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
		sldSimulationSpeed.setValue(100);

		JToggleButton tglbtnPause = new JToggleButton("Pause");
		tglbtnPause.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				JToggleButton button = (JToggleButton) e.getSource();

				if (button.isSelected()) {
					button.setText("Resume");
					MallSim.setThreadState(true);
				} else {
					button.setText("Pause");
					MallSim.setThreadState(false);
				}
			}
		});
		GridBagConstraints gbc_tglbtnPause = new GridBagConstraints();
		gbc_tglbtnPause.insets = new Insets(0, 0, 5, 0);
		gbc_tglbtnPause.gridx = 0;
		gbc_tglbtnPause.gridy = 4;
		tabDisplay.add(tglbtnPause, gbc_tglbtnPause);

		JPanel panel = new JPanel();
		GridBagConstraints gbc_panel = new GridBagConstraints();
		gbc_panel.insets = new Insets(0, 0, 5, 0);
		gbc_panel.fill = GridBagConstraints.BOTH;
		gbc_panel.gridx = 0;
		gbc_panel.gridy = 6;
		tabDisplay.add(panel, gbc_panel);

		JToggleButton tglbtnRecord = new JToggleButton("Record");
		panel.add(tglbtnRecord);

		Component horizontalStrut = Box.createHorizontalStrut(20);
		panel.add(horizontalStrut);

		JLabel lblSpf = new JLabel("spf:");
		panel.add(lblSpf);

		JSpinner spinner = new JSpinner();
		spinner.setModel(new SpinnerNumberModel(2, 1, 10, 1));
		spinner.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				JSpinner s = (JSpinner) e.getSource();

				MallFrame.this.videoRecorder
						.setSimFramesPerAviFrame((Integer) s.getValue());
			}
		});
		panel.add(spinner);
		tglbtnRecord.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JToggleButton b = (JToggleButton) e.getSource();
				if (b.isSelected()) {
					try {
						MallFrame.this.videoRecorder.prepare();
					} catch (IOException e1) {
						e1.printStackTrace();
					} catch (AWTException e1) {
						e1.printStackTrace();
					}
				} else {
					try {
						MallFrame.this.videoRecorder.finish();
					} catch (IOException e1) {
						e1.printStackTrace();
					} catch (AWTException e1) {
						e1.printStackTrace();
					}
				}
			}
		});

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
		case SELECTION_ROUTE:
			break;
		default:
			break;
		}
	}

	private void createPropertiesTab(JSplitPane splitPane,
			JTabbedPane tabbedPane) {
		JPanel propertiesPanel = new JPanel();
		tabbedPane.addTab("Properties", null, propertiesPanel, null);
		GridBagLayout gbl_propertiesPanel = new GridBagLayout();
		gbl_propertiesPanel.columnWidths = new int[] { 1, 0 };
		gbl_propertiesPanel.rowHeights = new int[] { 1, 0 };
		gbl_propertiesPanel.columnWeights = new double[] { 1.0,
				Double.MIN_VALUE };
		gbl_propertiesPanel.rowWeights = new double[] { 0.0, Double.MIN_VALUE };
		propertiesPanel.setLayout(gbl_propertiesPanel);

		propertiesTable = new PropertiesTable();
		GridBagConstraints gbc_propertiesTable = new GridBagConstraints();
		gbc_propertiesTable.fill = GridBagConstraints.HORIZONTAL;
		gbc_propertiesTable.gridx = 0;
		gbc_propertiesTable.gridy = 0;
		propertiesPanel.add(propertiesTable, gbc_propertiesTable);
	}

	private void setDefaults() {
		GuiState.backgroundPolicy = BackgroundPolicy.LANES;
	}

	public GUIBoard getBoard() {
		return guiBoard;
	}

	public PropertiesTable getPropertiesTable() {
		return propertiesTable;
	}

	public SummaryTable getSummaryTable() {
		return summaryTable;
	}

	public JScrollPane getScrollPane() {
		return boardScrollPane;
	}

}
