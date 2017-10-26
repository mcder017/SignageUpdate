package SignageUpdate;

import java.awt.EventQueue;

import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import java.awt.BorderLayout;

import javax.swing.JPanel;

import java.awt.GridBagLayout;

import javax.swing.JTextPane;
import javax.swing.JButton;

import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.Font;

import javax.swing.JTextArea;
import javax.swing.Box;

import java.awt.Component;

import javax.swing.JSeparator;

import java.awt.Color;

import javax.swing.border.EtchedBorder;
import javax.swing.AbstractAction;

import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import javax.swing.Action;
import javax.swing.JScrollPane;

public class MainWindow {

	private JFrame frame;
	private final static boolean realActions = true;	// if false, do not call system commands (copy, erase, etc.), just display what command would be sent
	
	private boolean quietRefresh = false;	// should message be displayed when signage computer file status refresh is complete?
	private boolean quietLoadConfig = false;	// should message be displayed when config file is loaded?
	
	private final String newLine = System.lineSeparator();
	
	private final String pathSeparator = "\\";
	private final String pathWrapper = "\"";
	private final char wildcardChar1 = '*';
	private final char wildcardChar2 = '?';

	public static String argConfigFile = null;	// requested configuration file passed to program in initial arguments
	private String configFilename = "SignageUpdate.cfg";
	private final String configFileHeader = "Signage Update Configuration File v1";	// required prefix on header line to be acceptable file (if extend config, then can add letters at end...)
	
	// ============Configurable paths and filenames==================================
	private String announcementFolder = "\\\\SBS-FIRST\\av\\Power Point"; 
	private String eventFolder = "\\\\SBS-FIRST\\av\\Power Point"; 
	private String layoutFolder = "\\\\SBS-FIRST\\av\\Power Point"; 
	private String signageFolder = "\\\\CS-DISPLAY\\USERS\\projector\\Documents"; 
	
	private String remoteRebootComputerFilename = "pleasereboot.txt";
	private String remoteRelaunchFilename = "pleaserelaunchshow.txt";
	private String remoteRelaunchNoEventsFilename = "pleaserelaunchnoevents.txt";
	
	private String announcementsFilename = "signage.pptx";
	private String eventsTodayFilename = "events_today.pptx";
	private String tickerLayoutFilename = "ticker_layout.pptx";
	
	private String newAnnouncementsFilename = "signagenew.pptx";
	
	private String[] eventDailyFilenames = {	// in order by index; see index list below
			"events_sun.pptx",
			"events_mon.pptx",
			"events_tue.pptx",
			"events_wed.pptx",
			"events_thu.pptx",
			"events_fri.pptx",
			"events_sat.pptx"			
			};
	// ==============================================
	private final int sundayIndex = 0;
	private final int mondayIndex = 1;
	private final int tuesdayIndex = 2;
	private final int wednesdayIndex = 3;
	private final int thursdayIndex = 4;
	private final int fridayIndex = 5;
	private final int saturdayIndex = 6;
		
	private final String noFileLabel = "(none)";	// displayed in status area when a particular file is not present
	private final String fullscreenLayoutText = "Full screen announcements    (the best Design/Page Setup fit for full screen Announcements file is 16:9 widescreen, or 10\" x 5.63\"; otherwise there will likely be black bars on the top and bottom of the screen)";
	private final String tickerLayoutText = "Announcements+Events ticker (the best Design/Page Setup fit for partial-screen Announcements and Events files depends on the ticker_layout file, e.g. 10\" x 4.13\" announcements and 10\" x 1.5\" events; otherwise there will likely be black bars on the sides of the screen)";
		
	private final Action actionExit = new SwingActionExit();
	private final Action actionLoadConfig = new SwingActionLoadConfig();
	private final Action actionSaveConfig = new SwingActionSaveConfig();
	private final Action actionCopyTickerLayout = new SwingActionCopyTickerLayout();
	private final Action actionReboot = new SwingActionRebootSignage();
	private final Action actionRefreshDisplay = new SwingActionRefreshDisplay();
	private final Action actionRelaunchNormal = new SwingActionRelaunchNow();
	private final Action actionRelaunchEraseCurrEvent = new SwingActionRelaunchEraseCurrentEvent();
	private final Action actionCopyAnnouncements = new SwingActionCopyAnnouncements();
	private final Action actionCopyEvents = new SwingActionCopyEvents();
	private final Action actionEraseEvents = new SwingActionEraseEvents();
	
	private Map<JTextPane, JTextPane> fileStatusMap = new HashMap<JTextPane,JTextPane>();	// display of file names to display of modification date/time info from signage computer
	private JTextArea textAreaCurrentScreenLayout;
	private JDialog waitDialog;
	
	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {	
		if (args.length > 0) {
			MainWindow.argConfigFile = args[0];	// save requested config file path+filename
		}
		
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					MainWindow window = new MainWindow();	// handles reading of configuration file if found
					
					window.frame.setTitle("Signage Update" + (realActions ? "" : " - TEST MODE"));
					window.frame.setVisible(true);
					
					// if config file was specified but not found, report warning
					if (argConfigFile != null) {
						File localConfigFile = new File(argConfigFile);
						if (!localConfigFile.exists()) {
					    	JOptionPane.showMessageDialog(window.frame, "Unable to find config file:\n"+MainWindow.argConfigFile, "Load config file failed", JOptionPane.WARNING_MESSAGE);
						}
					}
					
					// quietly initialize the displayed status information
					final boolean origQuietRefresh = window.isQuietRefresh();
					window.setQuietRefresh(true);
					window.refreshStatusNow();   
					window.setQuietRefresh(origQuietRefresh);
					
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 */
	public MainWindow() {
		// quietly read the configuration file if present in arg[0] specified file
		if (argConfigFile != null) {
			File localConfigFile = new File(argConfigFile);
			if (localConfigFile.exists()) {					
				final boolean origQuietLoad = isQuietLoadConfig();
				setQuietLoadConfig(true);
				loadConfigFile(localConfigFile);   
				setQuietLoadConfig(origQuietLoad);
			}		
			else {
				// config file specified but not found; no error reported here
			}
		}
		
		initialize();
	}
	
	/**
	 * Call only after the window is displayed
	 */
	public void refreshStatusNow() {
		actionRefreshDisplay.actionPerformed(null);
	}

	/**
	 * @param quietRefresh the quietRefresh to set
	 */
	void setQuietRefresh(boolean quietRefresh) {
		this.quietRefresh = quietRefresh;
	}

	/**
	 * @return the quietRefresh
	 */
	boolean isQuietRefresh() {
		return quietRefresh;
	}

	/**
	 * @return the quietLoadConfig
	 */
	boolean isQuietLoadConfig() {
		return quietLoadConfig;
	}

	/**
	 * @param quietLoadConfig the quietLoadConfig to set
	 */
	void setQuietLoadConfig(boolean quietLoadConfig) {
		this.quietLoadConfig = quietLoadConfig;
	}

	/**
	 * @return the configFilename
	 */
	String getConfigFilename() {
		return configFilename;
	}

	/**
	 * @param configFilename the configFilename to set
	 */
	void setConfigFilename(String configFilename) {
		this.configFilename = configFilename;
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frame = new JFrame();
		frame.setBounds(100, 100, 792, 718);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		JMenuBar menuBar = new JMenuBar();
		frame.setJMenuBar(menuBar);
		
		JMenu mnFile = new JMenu("File");
		menuBar.add(mnFile);
		
		JMenuItem mntmExit = new JMenuItem("Exit");
		mntmExit.setAction(actionExit);
		mnFile.add(mntmExit);
		
		JMenu mnOptions = new JMenu("Options");
		menuBar.add(mnOptions);
		
		JMenuItem mntmDefaultPaths = new JMenuItem("About loading config file...");
		mntmDefaultPaths.setAction(actionLoadConfig);
		mnOptions.add(mntmDefaultPaths);
		
		JMenuItem mntmSaveCopyOf = new JMenuItem("Save copy of config file...");
		mntmSaveCopyOf.setAction(actionSaveConfig);
		mnOptions.add(mntmSaveCopyOf);
		
		JMenuItem mntmCopyNewTicker = new JMenuItem("Copy new ticker layout file...");
		mntmCopyNewTicker.setAction(actionCopyTickerLayout);
		mnOptions.add(mntmCopyNewTicker);
		
		JMenuItem mntmRebootSignageComputer = new JMenuItem("Reboot signage computer...");
		mntmRebootSignageComputer.setAction(actionReboot);
		mnOptions.add(mntmRebootSignageComputer);
		frame.getContentPane().setLayout(new BorderLayout(0, 0));
		
		JPanel SignagePCStatusPanel = new JPanel();
		frame.getContentPane().add(SignagePCStatusPanel);
		SignagePCStatusPanel.setLayout(new BorderLayout(0, 0));
		
		JPanel panelSouth = new JPanel();
		panelSouth.setBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null));
		panelSouth.setBackground(Color.WHITE);
		JScrollPane scrollPanePanelSouth = new JScrollPane(panelSouth);
		SignagePCStatusPanel.add(scrollPanePanelSouth, BorderLayout.CENTER);

		GridBagLayout gbl_panelSouth = new GridBagLayout();
		gbl_panelSouth.columnWidths = new int[]{205, 136, 22, 165, 74, 0};
		gbl_panelSouth.rowHeights = new int[]{3, 0, 0, 3, 0, 0, 0, 3, 0, 0, 3, 0, 0, 0, 0, 0, 0, 0, 0, 3, 0, 0};
		gbl_panelSouth.columnWeights = new double[]{0.0, 0.0, 0.0, 0.0, 1.0, Double.MIN_VALUE};
		gbl_panelSouth.rowWeights = new double[]{0.0, 1.0, 1.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, Double.MIN_VALUE};
		panelSouth.setLayout(gbl_panelSouth);
		
		JTextPane txtpnSignagecomputerstatustext = new JTextPane();
		txtpnSignagecomputerstatustext.setFont(new Font("Tahoma", Font.BOLD, 14));
		txtpnSignagecomputerstatustext.setText("Signage Computer Status");
		GridBagConstraints gbc_txtpnSignagecomputerstatustext = new GridBagConstraints();
		gbc_txtpnSignagecomputerstatustext.gridwidth = 2;
		gbc_txtpnSignagecomputerstatustext.anchor = GridBagConstraints.NORTHWEST;
		gbc_txtpnSignagecomputerstatustext.insets = new Insets(0, 0, 5, 5);
		gbc_txtpnSignagecomputerstatustext.gridx = 0;
		gbc_txtpnSignagecomputerstatustext.gridy = 0;
		panelSouth.add(txtpnSignagecomputerstatustext, gbc_txtpnSignagecomputerstatustext);
		
		JButton btnStatusRefresh = new JButton("Refresh (re-read files)");
		btnStatusRefresh.setAction(actionRefreshDisplay);
		btnStatusRefresh.setFont(new Font("Tahoma", Font.ITALIC, 11));
		GridBagConstraints gbc_btnStatusRefresh = new GridBagConstraints();
		gbc_btnStatusRefresh.insets = new Insets(0, 0, 5, 5);
		gbc_btnStatusRefresh.gridx = 3;
		gbc_btnStatusRefresh.gridy = 0;
		panelSouth.add(btnStatusRefresh, gbc_btnStatusRefresh);
		
		JTextPane txtpnCurrentScreenLayout = new JTextPane();
		txtpnCurrentScreenLayout.setFont(new Font("Tahoma", Font.BOLD, 11));
		txtpnCurrentScreenLayout.setText("Current screen layout:");
		GridBagConstraints gbc_txtpnCurrentScreenLayout = new GridBagConstraints();
		gbc_txtpnCurrentScreenLayout.insets = new Insets(0, 0, 5, 5);
		gbc_txtpnCurrentScreenLayout.fill = GridBagConstraints.BOTH;
		gbc_txtpnCurrentScreenLayout.gridx = 0;
		gbc_txtpnCurrentScreenLayout.gridy = 1;
		panelSouth.add(txtpnCurrentScreenLayout, gbc_txtpnCurrentScreenLayout);
		
		textAreaCurrentScreenLayout = new JTextArea();
		textAreaCurrentScreenLayout.setEditable(false);
		textAreaCurrentScreenLayout.setFont(new Font("Tahoma", Font.PLAIN, 11));
		textAreaCurrentScreenLayout.setWrapStyleWord(true);
		textAreaCurrentScreenLayout.setLineWrap(true);
		textAreaCurrentScreenLayout.setText(fullscreenLayoutText);
		GridBagConstraints gbc_txtrFullScreenAnnouncements = new GridBagConstraints();
		gbc_txtrFullScreenAnnouncements.gridheight = 2;
		gbc_txtrFullScreenAnnouncements.gridwidth = 4;
		gbc_txtrFullScreenAnnouncements.insets = new Insets(0, 0, 5, 0);
		gbc_txtrFullScreenAnnouncements.fill = GridBagConstraints.BOTH;
		gbc_txtrFullScreenAnnouncements.gridx = 1;
		gbc_txtrFullScreenAnnouncements.gridy = 1;
		panelSouth.add(textAreaCurrentScreenLayout, gbc_txtrFullScreenAnnouncements);
		
		JTextPane txtpnFile = new JTextPane();
		txtpnFile.setFont(new Font("Tahoma", Font.BOLD, 11));
		txtpnFile.setText("File");
		GridBagConstraints gbc_txtpnFile = new GridBagConstraints();
		gbc_txtpnFile.anchor = GridBagConstraints.NORTHWEST;
		gbc_txtpnFile.insets = new Insets(0, 0, 5, 5);
		gbc_txtpnFile.gridx = 1;
		gbc_txtpnFile.gridy = 4;
		panelSouth.add(txtpnFile, gbc_txtpnFile);
		
		JTextPane txtpnContentSavedDatetime = new JTextPane();
		txtpnContentSavedDatetime.setFont(new Font("Tahoma", Font.BOLD, 11));
		txtpnContentSavedDatetime.setText("Date/time content saved (or \"None\" if file not present)");
		GridBagConstraints gbc_txtpnContentSavedDatetime = new GridBagConstraints();
		gbc_txtpnContentSavedDatetime.gridwidth = 2;
		gbc_txtpnContentSavedDatetime.anchor = GridBagConstraints.NORTHWEST;
		gbc_txtpnContentSavedDatetime.insets = new Insets(0, 0, 5, 0);
		gbc_txtpnContentSavedDatetime.gridx = 3;
		gbc_txtpnContentSavedDatetime.gridy = 4;
		panelSouth.add(txtpnContentSavedDatetime, gbc_txtpnContentSavedDatetime);
		
		JTextPane txtpnSignageSlides = new JTextPane();
		txtpnSignageSlides.setFont(new Font("Tahoma", Font.BOLD, 10));
		txtpnSignageSlides.setText("Announcements (currently shown)");
		GridBagConstraints gbc_txtpnSignageSlides = new GridBagConstraints();
		gbc_txtpnSignageSlides.insets = new Insets(0, 0, 5, 5);
		gbc_txtpnSignageSlides.anchor = GridBagConstraints.NORTHWEST;
		gbc_txtpnSignageSlides.gridx = 0;
		gbc_txtpnSignageSlides.gridy = 5;
		panelSouth.add(txtpnSignageSlides, gbc_txtpnSignageSlides);
		
		JTextPane txtpnAnnouncementsFilename = new JTextPane();
		txtpnAnnouncementsFilename.setFont(new Font("Tahoma", Font.PLAIN, 10));
		txtpnAnnouncementsFilename.setText(announcementsFilename);
		GridBagConstraints gbc_txtpnSignageFilename = new GridBagConstraints();
		gbc_txtpnSignageFilename.anchor = GridBagConstraints.WEST;
		gbc_txtpnSignageFilename.insets = new Insets(0, 0, 5, 5);
		gbc_txtpnSignageFilename.fill = GridBagConstraints.VERTICAL;
		gbc_txtpnSignageFilename.gridx = 1;
		gbc_txtpnSignageFilename.gridy = 5;
		panelSouth.add(txtpnAnnouncementsFilename, gbc_txtpnSignageFilename);
		
		JTextPane txtpnAnnouncementsDateTime = new JTextPane();
		txtpnAnnouncementsDateTime.setFont(new Font("Tahoma", Font.PLAIN, 10));
		txtpnAnnouncementsDateTime.setText("(none)");
		GridBagConstraints gbc_txtpnSignageDateTime = new GridBagConstraints();
		gbc_txtpnSignageDateTime.gridwidth = 2;
		gbc_txtpnSignageDateTime.anchor = GridBagConstraints.WEST;
		gbc_txtpnSignageDateTime.insets = new Insets(0, 0, 5, 0);
		gbc_txtpnSignageDateTime.fill = GridBagConstraints.VERTICAL;
		gbc_txtpnSignageDateTime.gridx = 3;
		gbc_txtpnSignageDateTime.gridy = 5;
		panelSouth.add(txtpnAnnouncementsDateTime, gbc_txtpnSignageDateTime);
		fileStatusMap.put(txtpnAnnouncementsFilename,txtpnAnnouncementsDateTime);
		
		JTextPane txtpnEventsCurrent = new JTextPane();
		txtpnEventsCurrent.setFont(new Font("Tahoma", Font.BOLD, 10));
		txtpnEventsCurrent.setText("Events (currently shown)");
		GridBagConstraints gbc_txtpnEventsCurrent = new GridBagConstraints();
		gbc_txtpnEventsCurrent.anchor = GridBagConstraints.WEST;
		gbc_txtpnEventsCurrent.insets = new Insets(0, 0, 5, 5);
		gbc_txtpnEventsCurrent.fill = GridBagConstraints.VERTICAL;
		gbc_txtpnEventsCurrent.gridx = 0;
		gbc_txtpnEventsCurrent.gridy = 6;
		panelSouth.add(txtpnEventsCurrent, gbc_txtpnEventsCurrent);
		
		JTextPane txtpnEventsCurrentFilename = new JTextPane();
		txtpnEventsCurrentFilename.setFont(new Font("Tahoma", Font.PLAIN, 10));
		txtpnEventsCurrentFilename.setText(eventsTodayFilename);
		GridBagConstraints gbc_txtpnEventsCurrentFilename = new GridBagConstraints();
		gbc_txtpnEventsCurrentFilename.anchor = GridBagConstraints.WEST;
		gbc_txtpnEventsCurrentFilename.insets = new Insets(0, 0, 5, 5);
		gbc_txtpnEventsCurrentFilename.fill = GridBagConstraints.VERTICAL;
		gbc_txtpnEventsCurrentFilename.gridx = 1;
		gbc_txtpnEventsCurrentFilename.gridy = 6;
		panelSouth.add(txtpnEventsCurrentFilename, gbc_txtpnEventsCurrentFilename);

		
		JTextPane txtpnEventsCurrentDateTime = new JTextPane();
		txtpnEventsCurrentDateTime.setFont(new Font("Tahoma", Font.PLAIN, 10));
		txtpnEventsCurrentDateTime.setText("(none)");
		GridBagConstraints gbc_txtpnEventsCurrentDateTime = new GridBagConstraints();
		gbc_txtpnEventsCurrentDateTime.gridwidth = 2;
		gbc_txtpnEventsCurrentDateTime.anchor = GridBagConstraints.WEST;
		gbc_txtpnEventsCurrentDateTime.insets = new Insets(0, 0, 5, 0);
		gbc_txtpnEventsCurrentDateTime.fill = GridBagConstraints.VERTICAL;
		gbc_txtpnEventsCurrentDateTime.gridx = 3;
		gbc_txtpnEventsCurrentDateTime.gridy = 6;
		panelSouth.add(txtpnEventsCurrentDateTime, gbc_txtpnEventsCurrentDateTime);
		fileStatusMap.put(txtpnEventsCurrentFilename,txtpnEventsCurrentDateTime);
		
		JTextPane txtpnAnnouncementsAtNext = new JTextPane();
		txtpnAnnouncementsAtNext.setFont(new Font("Tahoma", Font.BOLD, 11));
		txtpnAnnouncementsAtNext.setText("Announcements at next relaunch");
		GridBagConstraints gbc_txtpnAnnouncementsAtNext = new GridBagConstraints();
		gbc_txtpnAnnouncementsAtNext.anchor = GridBagConstraints.WEST;
		gbc_txtpnAnnouncementsAtNext.gridwidth = 2;
		gbc_txtpnAnnouncementsAtNext.insets = new Insets(0, 0, 5, 5);
		gbc_txtpnAnnouncementsAtNext.fill = GridBagConstraints.VERTICAL;
		gbc_txtpnAnnouncementsAtNext.gridx = 0;
		gbc_txtpnAnnouncementsAtNext.gridy = 8;
		panelSouth.add(txtpnAnnouncementsAtNext, gbc_txtpnAnnouncementsAtNext);
		
		JTextPane txtpnifNoNew = new JTextPane();
		txtpnifNoNew.setFont(new Font("Tahoma", Font.ITALIC, 11));
		txtpnifNoNew.setText("if no new file, then the current announcements file is kept unchanged");
		GridBagConstraints gbc_txtpnifNoNew = new GridBagConstraints();
		gbc_txtpnifNoNew.gridwidth = 2;
		gbc_txtpnifNoNew.insets = new Insets(0, 0, 5, 0);
		gbc_txtpnifNoNew.fill = GridBagConstraints.BOTH;
		gbc_txtpnifNoNew.gridx = 3;
		gbc_txtpnifNoNew.gridy = 8;
		panelSouth.add(txtpnifNoNew, gbc_txtpnifNoNew);
		
		JTextPane txtpnNewAnnouncements = new JTextPane();
		txtpnNewAnnouncements.setFont(new Font("Tahoma", Font.PLAIN, 10));
		txtpnNewAnnouncements.setText("Announcements (new)");
		GridBagConstraints gbc_txtpnNewSignage = new GridBagConstraints();
		gbc_txtpnNewSignage.insets = new Insets(0, 0, 5, 5);
		gbc_txtpnNewSignage.fill = GridBagConstraints.BOTH;
		gbc_txtpnNewSignage.gridx = 0;
		gbc_txtpnNewSignage.gridy = 9;
		panelSouth.add(txtpnNewAnnouncements, gbc_txtpnNewSignage);
		
		JTextPane txtpnNewAnnouncementsFilename = new JTextPane();
		txtpnNewAnnouncementsFilename.setFont(new Font("Tahoma", Font.PLAIN, 10));
		txtpnNewAnnouncementsFilename.setText(newAnnouncementsFilename);
		GridBagConstraints gbc_txtpnNewSignageFilename = new GridBagConstraints();
		gbc_txtpnNewSignageFilename.insets = new Insets(0, 0, 5, 5);
		gbc_txtpnNewSignageFilename.fill = GridBagConstraints.BOTH;
		gbc_txtpnNewSignageFilename.gridx = 1;
		gbc_txtpnNewSignageFilename.gridy = 9;
		panelSouth.add(txtpnNewAnnouncementsFilename, gbc_txtpnNewSignageFilename);
		
		JTextPane txtpnNewAnnouncementsDateTime = new JTextPane();
		txtpnNewAnnouncementsDateTime.setFont(new Font("Tahoma", Font.PLAIN, 10));
		txtpnNewAnnouncementsDateTime.setText("(none)");
		GridBagConstraints gbc_txtpnNewSignageDateTime = new GridBagConstraints();
		gbc_txtpnNewSignageDateTime.insets = new Insets(0, 0, 5, 5);
		gbc_txtpnNewSignageDateTime.fill = GridBagConstraints.BOTH;
		gbc_txtpnNewSignageDateTime.gridx = 3;
		gbc_txtpnNewSignageDateTime.gridy = 9;
		panelSouth.add(txtpnNewAnnouncementsDateTime, gbc_txtpnNewSignageDateTime);
		fileStatusMap.put(txtpnNewAnnouncementsFilename, txtpnNewAnnouncementsDateTime);
		
		JTextPane txtpnQueuedEvents = new JTextPane();
		txtpnQueuedEvents.setFont(new Font("Tahoma", Font.BOLD, 11));
		txtpnQueuedEvents.setText("Events at next relaunch");
		GridBagConstraints gbc_txtpnQueuedEvents = new GridBagConstraints();
		gbc_txtpnQueuedEvents.anchor = GridBagConstraints.WEST;
		gbc_txtpnQueuedEvents.insets = new Insets(0, 0, 5, 5);
		gbc_txtpnQueuedEvents.fill = GridBagConstraints.VERTICAL;
		gbc_txtpnQueuedEvents.gridx = 0;
		gbc_txtpnQueuedEvents.gridy = 11;
		panelSouth.add(txtpnQueuedEvents, gbc_txtpnQueuedEvents);
		
		JTextPane txtpnRelaunchExplanation = new JTextPane();
		txtpnRelaunchExplanation.setFont(new Font("Tahoma", Font.ITALIC, 11));
		txtpnRelaunchExplanation.setText("relaunches occur nightly, and when \"Relaunch\" is clicked above");
		GridBagConstraints gbc_txtpnRelaunchExplanation = new GridBagConstraints();
		gbc_txtpnRelaunchExplanation.anchor = GridBagConstraints.WEST;
		gbc_txtpnRelaunchExplanation.gridwidth = 2;
		gbc_txtpnRelaunchExplanation.insets = new Insets(0, 0, 5, 0);
		gbc_txtpnRelaunchExplanation.fill = GridBagConstraints.VERTICAL;
		gbc_txtpnRelaunchExplanation.gridx = 3;
		gbc_txtpnRelaunchExplanation.gridy = 11;
		panelSouth.add(txtpnRelaunchExplanation, gbc_txtpnRelaunchExplanation);
		
		JTextPane txtpnNextSunday = new JTextPane();
		txtpnNextSunday.setFont(new Font("Tahoma", Font.PLAIN, 10));
		txtpnNextSunday.setText("Sunday");
		GridBagConstraints gbc_txtpnNextSunday = new GridBagConstraints();
		gbc_txtpnNextSunday.anchor = GridBagConstraints.WEST;
		gbc_txtpnNextSunday.insets = new Insets(0, 0, 5, 5);
		gbc_txtpnNextSunday.fill = GridBagConstraints.VERTICAL;
		gbc_txtpnNextSunday.gridx = 0;
		gbc_txtpnNextSunday.gridy = 12;
		panelSouth.add(txtpnNextSunday, gbc_txtpnNextSunday);
		
		JTextPane txtpnEventsSunFilename = new JTextPane();
		txtpnEventsSunFilename.setFont(new Font("Tahoma", Font.PLAIN, 10));
		txtpnEventsSunFilename.setText(eventDailyFilenames[sundayIndex]);
		GridBagConstraints gbc_txtpnEventsSunFilename = new GridBagConstraints();
		gbc_txtpnEventsSunFilename.anchor = GridBagConstraints.WEST;
		gbc_txtpnEventsSunFilename.insets = new Insets(0, 0, 5, 5);
		gbc_txtpnEventsSunFilename.fill = GridBagConstraints.VERTICAL;
		gbc_txtpnEventsSunFilename.gridx = 1;
		gbc_txtpnEventsSunFilename.gridy = 12;
		panelSouth.add(txtpnEventsSunFilename, gbc_txtpnEventsSunFilename);
		
		JTextPane txtpnEventsSunDateTime = new JTextPane();
		txtpnEventsSunDateTime.setFont(new Font("Tahoma", Font.PLAIN, 10));
		txtpnEventsSunDateTime.setText("(none)");
		GridBagConstraints gbc_txtpnEventsSunDateTime = new GridBagConstraints();
		gbc_txtpnEventsSunDateTime.gridwidth = 2;
		gbc_txtpnEventsSunDateTime.anchor = GridBagConstraints.WEST;
		gbc_txtpnEventsSunDateTime.insets = new Insets(0, 0, 5, 0);
		gbc_txtpnEventsSunDateTime.fill = GridBagConstraints.VERTICAL;
		gbc_txtpnEventsSunDateTime.gridx = 3;
		gbc_txtpnEventsSunDateTime.gridy = 12;
		panelSouth.add(txtpnEventsSunDateTime, gbc_txtpnEventsSunDateTime);
		fileStatusMap.put(txtpnEventsSunFilename, txtpnEventsSunDateTime);
		
		JTextPane txtpnNextMonday = new JTextPane();
		txtpnNextMonday.setFont(new Font("Tahoma", Font.PLAIN, 10));
		txtpnNextMonday.setText("Monday");
		GridBagConstraints gbc_txtpnNextMonday = new GridBagConstraints();
		gbc_txtpnNextMonday.anchor = GridBagConstraints.WEST;
		gbc_txtpnNextMonday.insets = new Insets(0, 0, 5, 5);
		gbc_txtpnNextMonday.fill = GridBagConstraints.VERTICAL;
		gbc_txtpnNextMonday.gridx = 0;
		gbc_txtpnNextMonday.gridy = 13;
		panelSouth.add(txtpnNextMonday, gbc_txtpnNextMonday);
		
		JTextPane txtpnEventsMonFilename = new JTextPane();
		txtpnEventsMonFilename.setFont(new Font("Tahoma", Font.PLAIN, 10));
		txtpnEventsMonFilename.setText(eventDailyFilenames[mondayIndex]);
		GridBagConstraints gbc_txtpnEventsMonFilename = new GridBagConstraints();
		gbc_txtpnEventsMonFilename.anchor = GridBagConstraints.WEST;
		gbc_txtpnEventsMonFilename.insets = new Insets(0, 0, 5, 5);
		gbc_txtpnEventsMonFilename.fill = GridBagConstraints.VERTICAL;
		gbc_txtpnEventsMonFilename.gridx = 1;
		gbc_txtpnEventsMonFilename.gridy = 13;
		panelSouth.add(txtpnEventsMonFilename, gbc_txtpnEventsMonFilename);
		
		JTextPane txtpnEventsMonDateTime = new JTextPane();
		txtpnEventsMonDateTime.setFont(new Font("Tahoma", Font.PLAIN, 10));
		txtpnEventsMonDateTime.setText("(none)");
		GridBagConstraints gbc_txtpnEventsMonDateTime = new GridBagConstraints();
		gbc_txtpnEventsMonDateTime.gridwidth = 2;
		gbc_txtpnEventsMonDateTime.anchor = GridBagConstraints.WEST;
		gbc_txtpnEventsMonDateTime.insets = new Insets(0, 0, 5, 0);
		gbc_txtpnEventsMonDateTime.fill = GridBagConstraints.VERTICAL;
		gbc_txtpnEventsMonDateTime.gridx = 3;
		gbc_txtpnEventsMonDateTime.gridy = 13;
		panelSouth.add(txtpnEventsMonDateTime, gbc_txtpnEventsMonDateTime);
		fileStatusMap.put(txtpnEventsMonFilename, txtpnEventsMonDateTime);

		JTextPane txtpnNextTuesday = new JTextPane();
		txtpnNextTuesday.setFont(new Font("Tahoma", Font.PLAIN, 10));
		txtpnNextTuesday.setText("Tuesday");
		GridBagConstraints gbc_txtpnNextTuesday = new GridBagConstraints();
		gbc_txtpnNextTuesday.anchor = GridBagConstraints.WEST;
		gbc_txtpnNextTuesday.insets = new Insets(0, 0, 5, 5);
		gbc_txtpnNextTuesday.fill = GridBagConstraints.VERTICAL;
		gbc_txtpnNextTuesday.gridx = 0;
		gbc_txtpnNextTuesday.gridy = 14;
		panelSouth.add(txtpnNextTuesday, gbc_txtpnNextTuesday);
		
		JTextPane txtpnEventsTueFilename = new JTextPane();
		txtpnEventsTueFilename.setFont(new Font("Tahoma", Font.PLAIN, 10));
		txtpnEventsTueFilename.setText(eventDailyFilenames[tuesdayIndex]);
		GridBagConstraints gbc_txtpnEventsTueFilename = new GridBagConstraints();
		gbc_txtpnEventsTueFilename.anchor = GridBagConstraints.WEST;
		gbc_txtpnEventsTueFilename.insets = new Insets(0, 0, 5, 5);
		gbc_txtpnEventsTueFilename.fill = GridBagConstraints.VERTICAL;
		gbc_txtpnEventsTueFilename.gridx = 1;
		gbc_txtpnEventsTueFilename.gridy = 14;
		panelSouth.add(txtpnEventsTueFilename, gbc_txtpnEventsTueFilename);
		
		JTextPane txtpnEventsTueDateTime = new JTextPane();
		txtpnEventsTueDateTime.setFont(new Font("Tahoma", Font.PLAIN, 10));
		txtpnEventsTueDateTime.setText("(none)");
		GridBagConstraints gbc_txtpnEventsTueDateTime = new GridBagConstraints();
		gbc_txtpnEventsTueDateTime.gridwidth = 2;
		gbc_txtpnEventsTueDateTime.anchor = GridBagConstraints.WEST;
		gbc_txtpnEventsTueDateTime.insets = new Insets(0, 0, 5, 0);
		gbc_txtpnEventsTueDateTime.fill = GridBagConstraints.VERTICAL;
		gbc_txtpnEventsTueDateTime.gridx = 3;
		gbc_txtpnEventsTueDateTime.gridy = 14;
		panelSouth.add(txtpnEventsTueDateTime, gbc_txtpnEventsTueDateTime);
		fileStatusMap.put(txtpnEventsTueFilename, txtpnEventsTueDateTime);

		JTextPane txtpnNextWednesday = new JTextPane();
		txtpnNextWednesday.setFont(new Font("Tahoma", Font.PLAIN, 10));
		txtpnNextWednesday.setText("Wednesday");
		GridBagConstraints gbc_txtpnNextWednesday = new GridBagConstraints();
		gbc_txtpnNextWednesday.anchor = GridBagConstraints.WEST;
		gbc_txtpnNextWednesday.insets = new Insets(0, 0, 5, 5);
		gbc_txtpnNextWednesday.fill = GridBagConstraints.VERTICAL;
		gbc_txtpnNextWednesday.gridx = 0;
		gbc_txtpnNextWednesday.gridy = 15;
		panelSouth.add(txtpnNextWednesday, gbc_txtpnNextWednesday);
		
		JTextPane txtpnEventsWedFilename = new JTextPane();
		txtpnEventsWedFilename.setFont(new Font("Tahoma", Font.PLAIN, 10));
		txtpnEventsWedFilename.setText(eventDailyFilenames[wednesdayIndex]);
		GridBagConstraints gbc_txtpnEventsWedFilename = new GridBagConstraints();
		gbc_txtpnEventsWedFilename.anchor = GridBagConstraints.WEST;
		gbc_txtpnEventsWedFilename.insets = new Insets(0, 0, 5, 5);
		gbc_txtpnEventsWedFilename.fill = GridBagConstraints.VERTICAL;
		gbc_txtpnEventsWedFilename.gridx = 1;
		gbc_txtpnEventsWedFilename.gridy = 15;
		panelSouth.add(txtpnEventsWedFilename, gbc_txtpnEventsWedFilename);
		
		JTextPane txtpnEventsWedDateTime = new JTextPane();
		txtpnEventsWedDateTime.setFont(new Font("Tahoma", Font.PLAIN, 10));
		txtpnEventsWedDateTime.setText("(none)");
		GridBagConstraints gbc_txtpnEventsWedDateTime = new GridBagConstraints();
		gbc_txtpnEventsWedDateTime.gridwidth = 2;
		gbc_txtpnEventsWedDateTime.anchor = GridBagConstraints.WEST;
		gbc_txtpnEventsWedDateTime.insets = new Insets(0, 0, 5, 0);
		gbc_txtpnEventsWedDateTime.fill = GridBagConstraints.VERTICAL;
		gbc_txtpnEventsWedDateTime.gridx = 3;
		gbc_txtpnEventsWedDateTime.gridy = 15;
		panelSouth.add(txtpnEventsWedDateTime, gbc_txtpnEventsWedDateTime);
		fileStatusMap.put(txtpnEventsWedFilename, txtpnEventsWedDateTime);

		JTextPane txtpnNextThursday = new JTextPane();
		txtpnNextThursday.setFont(new Font("Tahoma", Font.PLAIN, 10));
		txtpnNextThursday.setText("Thursday");
		GridBagConstraints gbc_txtpnNextThursday = new GridBagConstraints();
		gbc_txtpnNextThursday.anchor = GridBagConstraints.WEST;
		gbc_txtpnNextThursday.insets = new Insets(0, 0, 5, 5);
		gbc_txtpnNextThursday.fill = GridBagConstraints.VERTICAL;
		gbc_txtpnNextThursday.gridx = 0;
		gbc_txtpnNextThursday.gridy = 16;
		panelSouth.add(txtpnNextThursday, gbc_txtpnNextThursday);
		
		JTextPane txtpnEventsThuFilename = new JTextPane();
		txtpnEventsThuFilename.setFont(new Font("Tahoma", Font.PLAIN, 10));
		txtpnEventsThuFilename.setText(eventDailyFilenames[thursdayIndex]);
		GridBagConstraints gbc_txtpnEventsThuFilename = new GridBagConstraints();
		gbc_txtpnEventsThuFilename.anchor = GridBagConstraints.WEST;
		gbc_txtpnEventsThuFilename.insets = new Insets(0, 0, 5, 5);
		gbc_txtpnEventsThuFilename.fill = GridBagConstraints.VERTICAL;
		gbc_txtpnEventsThuFilename.gridx = 1;
		gbc_txtpnEventsThuFilename.gridy = 16;
		panelSouth.add(txtpnEventsThuFilename, gbc_txtpnEventsThuFilename);
		
		JTextPane txtpnEventsThuDateTime = new JTextPane();
		txtpnEventsThuDateTime.setFont(new Font("Tahoma", Font.PLAIN, 10));
		txtpnEventsThuDateTime.setText("(none)");
		GridBagConstraints gbc_txtpnEventsThuDateTime = new GridBagConstraints();
		gbc_txtpnEventsThuDateTime.gridwidth = 2;
		gbc_txtpnEventsThuDateTime.anchor = GridBagConstraints.WEST;
		gbc_txtpnEventsThuDateTime.insets = new Insets(0, 0, 5, 0);
		gbc_txtpnEventsThuDateTime.fill = GridBagConstraints.VERTICAL;
		gbc_txtpnEventsThuDateTime.gridx = 3;
		gbc_txtpnEventsThuDateTime.gridy = 16;
		panelSouth.add(txtpnEventsThuDateTime, gbc_txtpnEventsThuDateTime);
		fileStatusMap.put(txtpnEventsThuFilename, txtpnEventsThuDateTime);

		JTextPane txtpnNextFriday = new JTextPane();
		txtpnNextFriday.setFont(new Font("Tahoma", Font.PLAIN, 10));
		txtpnNextFriday.setText("Friday");
		GridBagConstraints gbc_txtpnNextFriday = new GridBagConstraints();
		gbc_txtpnNextFriday.anchor = GridBagConstraints.WEST;
		gbc_txtpnNextFriday.insets = new Insets(0, 0, 5, 5);
		gbc_txtpnNextFriday.fill = GridBagConstraints.VERTICAL;
		gbc_txtpnNextFriday.gridx = 0;
		gbc_txtpnNextFriday.gridy = 17;
		panelSouth.add(txtpnNextFriday, gbc_txtpnNextFriday);
		
		JTextPane txtpnEventsFriFilename = new JTextPane();
		txtpnEventsFriFilename.setFont(new Font("Tahoma", Font.PLAIN, 10));
		txtpnEventsFriFilename.setText(eventDailyFilenames[fridayIndex]);
		GridBagConstraints gbc_txtpnEventsFriFilename = new GridBagConstraints();
		gbc_txtpnEventsFriFilename.anchor = GridBagConstraints.WEST;
		gbc_txtpnEventsFriFilename.insets = new Insets(0, 0, 5, 5);
		gbc_txtpnEventsFriFilename.fill = GridBagConstraints.VERTICAL;
		gbc_txtpnEventsFriFilename.gridx = 1;
		gbc_txtpnEventsFriFilename.gridy = 17;
		panelSouth.add(txtpnEventsFriFilename, gbc_txtpnEventsFriFilename);
		
		JTextPane txtpnEventsFriDateTime = new JTextPane();
		txtpnEventsFriDateTime.setFont(new Font("Tahoma", Font.PLAIN, 10));
		txtpnEventsFriDateTime.setText("(none)");
		GridBagConstraints gbc_txtpnEventsFriDateTime = new GridBagConstraints();
		gbc_txtpnEventsFriDateTime.gridwidth = 2;
		gbc_txtpnEventsFriDateTime.anchor = GridBagConstraints.WEST;
		gbc_txtpnEventsFriDateTime.insets = new Insets(0, 0, 5, 0);
		gbc_txtpnEventsFriDateTime.fill = GridBagConstraints.VERTICAL;
		gbc_txtpnEventsFriDateTime.gridx = 3;
		gbc_txtpnEventsFriDateTime.gridy = 17;
		panelSouth.add(txtpnEventsFriDateTime, gbc_txtpnEventsFriDateTime);
		fileStatusMap.put(txtpnEventsFriFilename, txtpnEventsFriDateTime);

		JTextPane txtpnNextSaturday = new JTextPane();
		txtpnNextSaturday.setFont(new Font("Tahoma", Font.PLAIN, 10));
		txtpnNextSaturday.setText("Saturday");
		GridBagConstraints gbc_txtpnNextSaturday = new GridBagConstraints();
		gbc_txtpnNextSaturday.anchor = GridBagConstraints.WEST;
		gbc_txtpnNextSaturday.insets = new Insets(0, 0, 5, 5);
		gbc_txtpnNextSaturday.fill = GridBagConstraints.VERTICAL;
		gbc_txtpnNextSaturday.gridx = 0;
		gbc_txtpnNextSaturday.gridy = 18;
		panelSouth.add(txtpnNextSaturday, gbc_txtpnNextSaturday);
		
		JTextPane txtpnEventsSatFilename = new JTextPane();
		txtpnEventsSatFilename.setFont(new Font("Tahoma", Font.PLAIN, 10));
		txtpnEventsSatFilename.setText(eventDailyFilenames[saturdayIndex]);
		GridBagConstraints gbc_txtpnEventsSatFilename = new GridBagConstraints();
		gbc_txtpnEventsSatFilename.anchor = GridBagConstraints.WEST;
		gbc_txtpnEventsSatFilename.insets = new Insets(0, 0, 5, 5);
		gbc_txtpnEventsSatFilename.fill = GridBagConstraints.VERTICAL;
		gbc_txtpnEventsSatFilename.gridx = 1;
		gbc_txtpnEventsSatFilename.gridy = 18;
		panelSouth.add(txtpnEventsSatFilename, gbc_txtpnEventsSatFilename);
		
		JTextPane txtpnEventsSatDateTime = new JTextPane();
		txtpnEventsSatDateTime.setFont(new Font("Tahoma", Font.PLAIN, 10));
		txtpnEventsSatDateTime.setText("(none)");
		GridBagConstraints gbc_txtpnEventsSatDateTime = new GridBagConstraints();
		gbc_txtpnEventsSatDateTime.gridwidth = 2;
		gbc_txtpnEventsSatDateTime.anchor = GridBagConstraints.WEST;
		gbc_txtpnEventsSatDateTime.insets = new Insets(0, 0, 5, 0);
		gbc_txtpnEventsSatDateTime.fill = GridBagConstraints.VERTICAL;
		gbc_txtpnEventsSatDateTime.gridx = 3;
		gbc_txtpnEventsSatDateTime.gridy = 18;
		panelSouth.add(txtpnEventsSatDateTime, gbc_txtpnEventsSatDateTime);
		fileStatusMap.put(txtpnEventsSatFilename, txtpnEventsSatDateTime);

		JTextPane txtpnSignageeventsLayout = new JTextPane();
		txtpnSignageeventsLayout.setFont(new Font("Tahoma", Font.PLAIN, 10));
		txtpnSignageeventsLayout.setText("Announcements+Events Ticker Layout");
		GridBagConstraints gbc_txtpnSignageeventsLayout = new GridBagConstraints();
		gbc_txtpnSignageeventsLayout.anchor = GridBagConstraints.WEST;
		gbc_txtpnSignageeventsLayout.insets = new Insets(0, 0, 0, 5);
		gbc_txtpnSignageeventsLayout.fill = GridBagConstraints.VERTICAL;
		gbc_txtpnSignageeventsLayout.gridx = 0;
		gbc_txtpnSignageeventsLayout.gridy = 20;
		panelSouth.add(txtpnSignageeventsLayout, gbc_txtpnSignageeventsLayout);
		
		JTextPane txtpnLayoutFilename = new JTextPane();
		txtpnLayoutFilename.setFont(new Font("Tahoma", Font.PLAIN, 10));
		txtpnLayoutFilename.setText(tickerLayoutFilename);
		GridBagConstraints gbc_txtpnLayoutFilename = new GridBagConstraints();
		gbc_txtpnLayoutFilename.anchor = GridBagConstraints.WEST;
		gbc_txtpnLayoutFilename.insets = new Insets(0, 0, 0, 5);
		gbc_txtpnLayoutFilename.fill = GridBagConstraints.VERTICAL;
		gbc_txtpnLayoutFilename.gridx = 1;
		gbc_txtpnLayoutFilename.gridy = 20;
		panelSouth.add(txtpnLayoutFilename, gbc_txtpnLayoutFilename);
		
		JTextPane txtpnLayoutDateTime = new JTextPane();
		txtpnLayoutDateTime.setFont(new Font("Tahoma", Font.PLAIN, 10));
		txtpnLayoutDateTime.setText("(none)");
		GridBagConstraints gbc_txtpnLayoutDateTime = new GridBagConstraints();
		gbc_txtpnLayoutDateTime.gridwidth = 2;
		gbc_txtpnLayoutDateTime.anchor = GridBagConstraints.NORTHWEST;
		gbc_txtpnLayoutDateTime.gridx = 3;
		gbc_txtpnLayoutDateTime.gridy = 20;
		panelSouth.add(txtpnLayoutDateTime, gbc_txtpnLayoutDateTime);
		fileStatusMap.put(txtpnLayoutFilename, txtpnLayoutDateTime);
		
		JPanel panelActions = new JPanel();
		SignagePCStatusPanel.add(panelActions, BorderLayout.NORTH);
		panelActions.setLayout(new BorderLayout(0, 0));
		
		Box verticalBoxRelaunch = Box.createVerticalBox();
		panelActions.add(verticalBoxRelaunch, BorderLayout.CENTER);
		verticalBoxRelaunch.setBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null));
		verticalBoxRelaunch.setAlignmentX(Component.CENTER_ALIGNMENT);
		
		JButton btnRelaunchWithEvents = new JButton("Relaunch Signage Now");
		btnRelaunchWithEvents.setAction(actionRelaunchNormal);
		btnRelaunchWithEvents.setAlignmentX(Component.CENTER_ALIGNMENT);
		btnRelaunchWithEvents.setFont(new Font("Tahoma", Font.BOLD, 14));
		verticalBoxRelaunch.add(btnRelaunchWithEvents);
		
		JTextArea textAreaRelaunchNow = new JTextArea();
		textAreaRelaunchNow.setEditable(false);
		textAreaRelaunchNow.setWrapStyleWord(true);
		textAreaRelaunchNow.setText("Relaunch signage.  \nIf a new announcements file is present, the announcements will be updated (otherwise left unchanged).  \nIf a new events file for the current day of week is present, the events will be updated (otherwise left unchanged).  \nWhen the relaunch is complete, the signage computer looks to see if there is an events file (old or new) in place.  If so, the ticker layout is displayed; otherwise a full screen announcements file is displayed.");
		textAreaRelaunchNow.setLineWrap(true);
		textAreaRelaunchNow.setFont(new Font("Tahoma", Font.PLAIN, 11));
		verticalBoxRelaunch.add(textAreaRelaunchNow);
		
		JTextPane txtpnAutomaticRelaunchesAre = new JTextPane();
		txtpnAutomaticRelaunchesAre.setText("FYI - Automatic relaunches are also scheduled nightly, to update events.");
		verticalBoxRelaunch.add(txtpnAutomaticRelaunchesAre);
		
		JButton btnRelaunchEraseCurrentEvents = new JButton("Relaunch after erasing currently shown events...");
		btnRelaunchEraseCurrentEvents.setAction(actionRelaunchEraseCurrEvent);
		btnRelaunchEraseCurrentEvents.setFont(new Font("Tahoma", Font.ITALIC, 11));
		btnRelaunchEraseCurrentEvents.setAlignmentX(Component.CENTER_ALIGNMENT);
		verticalBoxRelaunch.add(btnRelaunchEraseCurrentEvents);
		
		JTextArea textAreaRelaunchWithErase = new JTextArea();
		textAreaRelaunchWithErase.setEditable(false);
		textAreaRelaunchWithErase.setWrapStyleWord(true);
		textAreaRelaunchWithErase.setFont(new Font("Tahoma", Font.PLAIN, 11));
		textAreaRelaunchWithErase.setText("Relaunch signage, after erasing any currently shown events.  \nThe result will be full screen \"Announcements only\" signage unless there is an events file for the current day of the week already copied onto the signage computer.");
		textAreaRelaunchWithErase.setLineWrap(true);
		verticalBoxRelaunch.add(textAreaRelaunchWithErase);
		
		Component verticalGlue_1 = Box.createVerticalGlue();
		verticalBoxRelaunch.add(verticalGlue_1);
		
		JPanel panelCopyErase = new JPanel();
		panelActions.add(panelCopyErase, BorderLayout.WEST);
		panelCopyErase.setBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null));
		
		Box verticalBox = Box.createVerticalBox();
		panelCopyErase.add(verticalBox);
		
		JButton btnCopyNewAnnouncements = new JButton("Copy New Announcement File...");
		btnCopyNewAnnouncements.setAction(actionCopyAnnouncements);
		btnCopyNewAnnouncements.setFont(new Font("Tahoma", Font.BOLD, 14));
		verticalBox.add(btnCopyNewAnnouncements);
		
		Component verticalStrut_3 = Box.createVerticalStrut(15);
		verticalBox.add(verticalStrut_3);
		
		JButton btnCopyNewEvents = new JButton("Copy New Event File(s)...");
		btnCopyNewEvents.setAction(actionCopyEvents);
		btnCopyNewEvents.setFont(new Font("Tahoma", Font.BOLD, 14));
		verticalBox.add(btnCopyNewEvents);
		
		Component verticalGlue_2 = Box.createVerticalGlue();
		verticalBox.add(verticalGlue_2);
		
		Component verticalStrut = Box.createVerticalStrut(20);
		verticalBox.add(verticalStrut);
		
		JButton btnEraseEventFiles = new JButton("Erase Event Files...");
		btnEraseEventFiles.setAction(actionEraseEvents);
		btnEraseEventFiles.setFont(new Font("Tahoma", Font.ITALIC, 11));
		verticalBox.add(btnEraseEventFiles);
		
		JSeparator separator = new JSeparator();
		panelCopyErase.add(separator);
		
		// searching for the signage computer can take some time... display a "please wait" dialog
    	JOptionPane waitOption = new JOptionPane("Please wait... searching for signage computer...    \n" + signageFolder, JOptionPane.INFORMATION_MESSAGE);
    	waitDialog = waitOption.createDialog("Please wait... searching for signage computer...");
		waitDialog.setModal(false);
	}
	
	private boolean isExistSignageFolder() {
		File testFolder = new File(signageFolder);
		return testFolder.exists();
	}
	
	private void reportNoSignageFolder() {
		JOptionPane.showMessageDialog(frame, "Unable to access signage computer [" + signageFolder + "]", "Unable to access signage folder", JOptionPane.ERROR_MESSAGE);				
	}
	
	private String getModDateTime(String filename) {
		return getModDateTime(new File(signageFolder + pathSeparator + filename));
	}
	
	private String getModDateTime(File f) {		
		if (f.isDirectory() || !f.exists()) {
			return noFileLabel;
		}
		
		Date d = new Date(f.lastModified());
		DateFormat df = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
		return df.format(d);
	}
	
	public void loadConfigFile(File configFile) {
		/**
		 * File content sequence and format must be matched to saveConfigFile
		 */
		if (!configFile.exists()) {
	    	JOptionPane.showMessageDialog(frame, "Unable to find config file:\n"+configFile.getAbsolutePath(), "Load config file failed", JOptionPane.WARNING_MESSAGE);
	    	return;
		}
		
		try {
			FileReader fileReader = new FileReader(configFile);
			BufferedReader bufferedReader = new BufferedReader(fileReader);
			String line;

			// check for header
			if ((line = bufferedReader.readLine()) == null || !line.startsWith(configFileHeader)) {
		    	JOptionPane.showMessageDialog(frame, "Configuration file header line missing, expected:\n"+configFileHeader, "Unrecognized config file", JOptionPane.WARNING_MESSAGE);
		    	fileReader.close();
		    	return;
			}
			
			if ((line = bufferedReader.readLine()) != null) {
				announcementFolder = line;
			}
			else exitFromPartialConfig();
			
			if ((line = bufferedReader.readLine()) != null) {
				eventFolder = line;
			}
			else exitFromPartialConfig();
			
			if ((line = bufferedReader.readLine()) != null) {
				layoutFolder = line;
			}
			else exitFromPartialConfig();
			
			if ((line = bufferedReader.readLine()) != null) {
				signageFolder = line;
			}
			else exitFromPartialConfig();
			
			if ((line = bufferedReader.readLine()) != null) {
				remoteRebootComputerFilename = line;
			}
			else exitFromPartialConfig();
			
			if ((line = bufferedReader.readLine()) != null) {
				remoteRelaunchFilename = line;
			}
			else exitFromPartialConfig();
			
			if ((line = bufferedReader.readLine()) != null) {
				remoteRelaunchNoEventsFilename = line;
			}
			else exitFromPartialConfig();
			
			if ((line = bufferedReader.readLine()) != null) {
				announcementsFilename = line;
			}
			else exitFromPartialConfig();
			
			if ((line = bufferedReader.readLine()) != null) {
				eventsTodayFilename = line;
			}
			else exitFromPartialConfig();
			
			if ((line = bufferedReader.readLine()) != null) {
				tickerLayoutFilename = line;
			}
			else exitFromPartialConfig();
			
			if ((line = bufferedReader.readLine()) != null) {
				newAnnouncementsFilename = line;
			}
			else exitFromPartialConfig();
			
			for (int dayIndex=0; dayIndex < eventDailyFilenames.length; dayIndex++) {
				if ((line = bufferedReader.readLine()) != null) {
					eventDailyFilenames[dayIndex] = line;
				}
				else exitFromPartialConfig();				
			}

			fileReader.close();
		} catch (IOException e) {
			exitFromPartialConfig();
	    	return;
		}
	    
	    if (!quietLoadConfig) {
	    	JOptionPane.showMessageDialog(frame, "Read config file ok.\n"+configFile.getAbsolutePath());
	    }
	}
	
	public void saveConfigFile(File configFile) {
		/**
		 * File content sequence and format must be matched to loadConfigFile
		 */
		try {
			FileWriter fileWriter = new FileWriter(configFile);

			// write header
			fileWriter.write(configFileHeader + newLine);
			
			// write contents
			fileWriter.write(announcementFolder + newLine);
			fileWriter.write(eventFolder + newLine);
			fileWriter.write(layoutFolder + newLine);
			fileWriter.write(signageFolder + newLine);
			fileWriter.write(remoteRebootComputerFilename + newLine);
			fileWriter.write(remoteRelaunchFilename + newLine);
			fileWriter.write(remoteRelaunchNoEventsFilename + newLine);
			fileWriter.write(announcementsFilename + newLine);
			fileWriter.write(eventsTodayFilename + newLine);
			fileWriter.write(tickerLayoutFilename + newLine);
			fileWriter.write(newAnnouncementsFilename + newLine);
			
			for (int dayIndex=0; dayIndex < eventDailyFilenames.length; dayIndex++) {
				fileWriter.write(eventDailyFilenames[dayIndex] + newLine);
			}

			fileWriter.close();
		} catch (IOException e) {
	    	JOptionPane.showMessageDialog(frame, "Error while writing to requested config file:\n"+configFile.getAbsolutePath(), "Write error", JOptionPane.ERROR_MESSAGE);
	    	return;
		}
	    
    	JOptionPane.showMessageDialog(frame, "Wrote config file ok:\n"+configFile.getAbsolutePath());
	}
	
	private void exitFromPartialConfig() {
    	JOptionPane.showMessageDialog(frame, "Failed to complete loading of config file\nProgram exiting to avoid continuing with scrambled configuration.", "Partial load of config file", JOptionPane.ERROR_MESSAGE);
    	System.exit(0);		
	}
	
	private class SwingActionRefreshDisplay extends AbstractAction {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		public SwingActionRefreshDisplay() {
			putValue(NAME, "Refresh (re-read files)");
			putValue(SHORT_DESCRIPTION, "Re-check which files are on the signage computer");
		}
		public void actionPerformed(ActionEvent e) {
	    	waitDialog.setVisible(true);	    	
			if (!isExistSignageFolder()) {
				waitDialog.setVisible(false);
				reportNoSignageFolder();
				return;
			}
			waitDialog.setVisible(false);

			Iterator<Entry<JTextPane, JTextPane>> it = fileStatusMap.entrySet().iterator();
		    while (it.hasNext()) {
		        Map.Entry<JTextPane,JTextPane> pair = (Map.Entry<JTextPane,JTextPane>)it.next();
		        
		        final String filename = pair.getKey().getText();		
		        final String dateTime = getModDateTime(filename);
		        pair.getValue().setText(dateTime);
		    }
		    
		    // update the description of what screen layout is currently active, based on how the signage computer scripts work
		    // (if the announcements, events-today, and ticker-layout files are all present, then ticker layout is active)
		    final File announcementsFile = new File(signageFolder + pathSeparator + announcementsFilename);
		    final File eventsTodayFile = new File(signageFolder + pathSeparator + eventsTodayFilename);
		    final File tickerLayoutFile = new File(signageFolder + pathSeparator + tickerLayoutFilename);
		    if (announcementsFile.exists() && eventsTodayFile.exists() && tickerLayoutFile.exists()) {
		    	textAreaCurrentScreenLayout.setText(tickerLayoutText);
		    }
		    else {
		    	textAreaCurrentScreenLayout.setText(fullscreenLayoutText);
		    }
		    
		    if (!quietRefresh) {
		    	JOptionPane.showMessageDialog(frame, "Read ok from signage computer.");
		    }
		}
	}
	
	private class SwingActionRelaunchNow extends AbstractAction {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		public SwingActionRelaunchNow() {
			putValue(NAME, "Relaunch Signage Now");
			putValue(SHORT_DESCRIPTION, "Stop, update, and restart the slideshow(s) on the signage computer");
		}
		public void actionPerformed(ActionEvent e) {
	    	waitDialog.setVisible(true);	    	
			if (!isExistSignageFolder()) {
				waitDialog.setVisible(false);
				reportNoSignageFolder();
				return;
			}
			waitDialog.setVisible(false);
			
			ProcessBuilder pb = new ProcessBuilder("cmd", "/c", "echo ... > " + pathWrapper + remoteRelaunchFilename + pathWrapper);
			File dir = new File(signageFolder);
			pb.directory(dir);
			try {
				if (realActions) {
					pb.start();
				}
				else {
					JOptionPane.showMessageDialog(frame, "[" + pb.directory().toString() + "] " + pb.command().toString());
				}
				JOptionPane.showMessageDialog(frame, "Complete!  Signage relaunch should finish shortly.");
			} catch (IOException e1) {
				JOptionPane.showMessageDialog(frame, "Error writing to signage computer [" + signageFolder + "]", "Unable to command relaunch", JOptionPane.ERROR_MESSAGE);
			}
		}
	}
	
	private class SwingActionRebootSignage extends AbstractAction {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		public SwingActionRebootSignage() {
			putValue(NAME, "Reboot signage Computer...");
			putValue(SHORT_DESCRIPTION, "Reboot signage computer, update signage and events files, and restart the slideshow(s)");
		}
		public void actionPerformed(ActionEvent e) {
	    	waitDialog.setVisible(true);	    	
			if (!isExistSignageFolder()) {
				waitDialog.setVisible(false);
				reportNoSignageFolder();
				return;
			}
			waitDialog.setVisible(false);
			
			ProcessBuilder pb = new ProcessBuilder("cmd", "/c", "echo ... > " + pathWrapper + remoteRebootComputerFilename + pathWrapper);
			File dir = new File(signageFolder);
			pb.directory(dir);
			try {
				if (realActions) {
					pb.start();
				}
				else {
					JOptionPane.showMessageDialog(frame, "[" + pb.directory().toString() + "] " + pb.command().toString());
				}
				JOptionPane.showMessageDialog(frame, "Complete!  Signage computer should reboot shortly.");
			} catch (IOException e1) {
				JOptionPane.showMessageDialog(frame, "Error writing to signage computer [" + signageFolder + "]", "Unable to command reboot", JOptionPane.ERROR_MESSAGE);
			}
		}
	}
	
	private class SwingActionRelaunchEraseCurrentEvent extends AbstractAction {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		public SwingActionRelaunchEraseCurrentEvent() {
			putValue(NAME, "Relaunch after erasing currently shown events...");
			putValue(SHORT_DESCRIPTION, "Erase the current events file, then stop, update, and restart signage slideshow(s)");
		}
		public void actionPerformed(ActionEvent e) {
	    	waitDialog.setVisible(true);	    	
			if (!isExistSignageFolder()) {
				waitDialog.setVisible(false);
				reportNoSignageFolder();
				return;
			}
			waitDialog.setVisible(false);
			
			ProcessBuilder pb = new ProcessBuilder("cmd", "/c", "echo ... > " + pathWrapper + remoteRelaunchNoEventsFilename + pathWrapper);
			File dir = new File(signageFolder);
			pb.directory(dir);
			try {
				if (realActions) {
					pb.start();
				}
				else {
					JOptionPane.showMessageDialog(frame, "[" + pb.directory().toString() + "] " + pb.command().toString());
				}
				JOptionPane.showMessageDialog(frame, "Complete!  Clearing current events, plus signage relaunch, should finish shortly.");
			} catch (IOException e1) {
				JOptionPane.showMessageDialog(frame, "Error writing to signage computer [" + signageFolder + "]", "Unable to command erase and relaunch", JOptionPane.ERROR_MESSAGE);
			}
		}
	}
	
	private class SwingActionCopyTickerLayout extends AbstractAction {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		public SwingActionCopyTickerLayout() {
			putValue(NAME, "Copy new ticker layout file...");
			putValue(SHORT_DESCRIPTION, "Select new ticker layout file and copy it to signage computer");
		}
		public void actionPerformed(ActionEvent e) {
	    	waitDialog.setVisible(true);	    	
			if (!isExistSignageFolder()) {
				waitDialog.setVisible(false);
				reportNoSignageFolder();
				return;
			}
			waitDialog.setVisible(false);
			
			if (textAreaCurrentScreenLayout.getText().equals(tickerLayoutText)) {
				JOptionPane.showMessageDialog(frame, "The ticker layout appears to be active on the signage computer.\nYou can only change the layout when it is NOT active.\n\n1. Erase the event file for the current day-of-week from the signage computer (if present)\n2. Click \"Relaunch after erasing currently shown events\" button.", "Ticker layout active", JOptionPane.WARNING_MESSAGE);
				return;
			}
			
			File testFolder = new File(layoutFolder);
			if (!testFolder.exists()) {
				JOptionPane.showMessageDialog(frame, "Default ticker layout folder: " + layoutFolder + "\nnot found.", "Folder not found", JOptionPane.INFORMATION_MESSAGE);
			}
			
			JFileChooser fc = new JFileChooser((testFolder.exists() ? testFolder.getAbsolutePath() : ""));
			fc.setDialogTitle("Select ticker layout file to copy to signage computer");
			fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
			fc.setMultiSelectionEnabled(false);
		    fc.setFileFilter(new ExtensionFileFilter("pptx", new String[] { "pptx" })); // no ppt since we copy to a fixed destination file name.pptx		    

			int returnVal = fc.showDialog(frame, "Copy to signage");
			switch (returnVal) {
				case JFileChooser.APPROVE_OPTION: 
					String tickerFilename = null;
					try {
						tickerFilename = fc.getSelectedFile().getCanonicalPath();
					} catch (Exception e1) {
						JOptionPane.showMessageDialog(frame, "Unable to access selected file.  No file copied.", "Error on read", JOptionPane.ERROR_MESSAGE);
						return;
					}
					
					final String command = "copy " + pathWrapper + tickerFilename + pathWrapper + " " + tickerLayoutFilename;
					ProcessBuilder pb = new ProcessBuilder("cmd", "/c", command);
					File dir = new File(signageFolder);
					pb.directory(dir);
					try {
						if (realActions) {
							pb.start();
						}
						else {
							JOptionPane.showMessageDialog(frame, "[" + pb.directory().toString() + "] " + pb.command().toString());
						}
					} catch (IOException e1) {
						JOptionPane.showMessageDialog(frame, "Error copying to signage computer [" + signageFolder + "]", "Unable to copy", JOptionPane.ERROR_MESSAGE);
					}
					
					
					JOptionPane.showMessageDialog(frame, "Ticker layout file: " + tickerFilename + "\ncopied to signage computer: " + tickerLayoutFilename + "\n(but signage not yet relaunched).", "Announcements file copied", JOptionPane.INFORMATION_MESSAGE);
					break;
				case JFileChooser.CANCEL_OPTION:
					JOptionPane.showMessageDialog(frame, "No file copied.", "Cancelled", JOptionPane.INFORMATION_MESSAGE);
					break;
				default:	// error
					JOptionPane.showMessageDialog(frame, "Error while selecting file (nothing copied)", "Error during file selection", JOptionPane.ERROR_MESSAGE);
					break;
			}
		}
	}
	
	private class SwingActionCopyAnnouncements extends AbstractAction {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		public SwingActionCopyAnnouncements() {
			putValue(NAME, "Copy New Announcement File...");
			putValue(SHORT_DESCRIPTION, "Select new announcement file and copy it to signage computer");
		}
		public void actionPerformed(ActionEvent e) {
	    	waitDialog.setVisible(true);	    	
			if (!isExistSignageFolder()) {
				waitDialog.setVisible(false);
				reportNoSignageFolder();
				return;
			}
			waitDialog.setVisible(false);
			
			File testFolder = new File(announcementFolder);
			if (!testFolder.exists()) {
				JOptionPane.showMessageDialog(frame, "Default announcements folder: " + announcementFolder + "\nnot found.", "Folder not found", JOptionPane.INFORMATION_MESSAGE);
			}
			
			JFileChooser fc = new JFileChooser((testFolder.exists() ? testFolder.getAbsolutePath() : ""));
			fc.setDialogTitle("Select announcements file to copy to signage computer");
			fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
			fc.setMultiSelectionEnabled(false);
		    fc.setFileFilter(new ExtensionFileFilter("pptx", new String[] { "pptx" })); // no ppt since we copy to a fixed destination file name.pptx		    

			int returnVal = fc.showDialog(frame, "Copy to signage");
			switch (returnVal) {
				case JFileChooser.APPROVE_OPTION: 
					String announcementFilename = null;
					try {
						announcementFilename = fc.getSelectedFile().getCanonicalPath();
					} catch (Exception e1) {
						JOptionPane.showMessageDialog(frame, "Unable to access selected file.  No file copied.", "Error on read", JOptionPane.ERROR_MESSAGE);
						return;
					}
					
					final String command = "copy " + pathWrapper + announcementFilename + pathWrapper + " " + newAnnouncementsFilename;
					ProcessBuilder pb = new ProcessBuilder("cmd", "/c", command);
					File dir = new File(signageFolder);
					pb.directory(dir);
					try {
						if (realActions) {
							pb.start();
						}
						else {
							JOptionPane.showMessageDialog(frame, "[" + pb.directory().toString() + "] " + pb.command().toString());
						}
					} catch (IOException e1) {
						JOptionPane.showMessageDialog(frame, "Error copying to signage computer [" + signageFolder + "]", "Unable to copy", JOptionPane.ERROR_MESSAGE);
					}
					
					
					JOptionPane.showMessageDialog(frame, "Announcements file: " + announcementFilename + "\ncopied to signage computer: " + newAnnouncementsFilename + "\n(but signage not yet relaunched).", "Announcements file copied", JOptionPane.INFORMATION_MESSAGE);
					break;
				case JFileChooser.CANCEL_OPTION:
					JOptionPane.showMessageDialog(frame, "No file copied.", "Cancelled", JOptionPane.INFORMATION_MESSAGE);
					break;
				default:	// error
					JOptionPane.showMessageDialog(frame, "Error while selecting file (nothing copied)", "Error during file selection", JOptionPane.ERROR_MESSAGE);
					break;
			}
		}
	}
	
	private class SwingActionCopyEvents extends AbstractAction {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		public SwingActionCopyEvents() {
			putValue(NAME, "Copy New Event File(s)...");
			putValue(SHORT_DESCRIPTION, "Select one or more new daily event file(s) and copy to signage computer");
		}
		public void actionPerformed(ActionEvent e) {
	    	waitDialog.setVisible(true);	    	
			if (!isExistSignageFolder()) {
				waitDialog.setVisible(false);
				reportNoSignageFolder();
				return;
			}
			waitDialog.setVisible(false);
			
			File testFolder = new File(eventFolder);
			if (!testFolder.exists()) {
				JOptionPane.showMessageDialog(frame, "Default events folder: " + eventFolder + "\nnot found.", "Folder not found", JOptionPane.INFORMATION_MESSAGE);
			}
			
			JFileChooser fc = new JFileChooser((testFolder.exists() ? testFolder.getAbsolutePath() : ""));
			fc.setDialogTitle("Select daily event file(s) to copy to signage computer");
			fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
			fc.setMultiSelectionEnabled(true);
		    fc.setFileFilter(new ListedFileFilter("events_<DayOfWeek>.pptx", eventDailyFilenames)); // specific files only, based on what is used by signage computer's script		    

			int returnVal = fc.showDialog(frame, "Copy to signage");
			switch (returnVal) {
				case JFileChooser.APPROVE_OPTION: 
					File[] eventFilenamesArray = fc.getSelectedFiles();
					int filesCopied = 0;
					
					// copy each selected file to the signage computer
					for (File eventFile : eventFilenamesArray) {
						String eventFilename = null;
						try {
							eventFilename = eventFile.getCanonicalPath();	// name with path
						} catch (Exception e1) {
							JOptionPane.showMessageDialog(frame, "Unable to access selected file #" + (filesCopied+1) + "\n(not copied)", "Error on read", JOptionPane.ERROR_MESSAGE);
							continue;	// try next file
						}
						String nameOnly = eventFile.getName();					// name only (for pasting into "current directory" on signage computer)
					
						final String command = "copy " + pathWrapper + eventFilename + pathWrapper + " " + nameOnly;
						ProcessBuilder pb = new ProcessBuilder("cmd", "/c", command);
						File dir = new File(signageFolder);
						pb.directory(dir);
						try {
							if (realActions) {
								pb.start();
							}
							else {
								JOptionPane.showMessageDialog(frame, "[" + pb.directory().toString() + "] " + pb.command().toString());
							}
							filesCopied++;
						} catch (IOException e1) {
							JOptionPane.showMessageDialog(frame, "Error copying to signage computer [" + signageFolder + "]", "Unable to copy", JOptionPane.ERROR_MESSAGE);
						}
					}					
					
					JOptionPane.showMessageDialog(frame, "" + filesCopied + " events file" + (eventFilenamesArray.length > 1 ? "s" : "") + " successfully copied to signage computer\n" + signageFolder + "\n(but signage not yet relaunched)", "Event file(s) copied", JOptionPane.INFORMATION_MESSAGE);
					break;
				case JFileChooser.CANCEL_OPTION:
					JOptionPane.showMessageDialog(frame, "No file(s) copied.", "Cancelled", JOptionPane.INFORMATION_MESSAGE);
					break;
				default:	// error
					JOptionPane.showMessageDialog(frame, "Error while selecting files (nothing copied)", "Error during file selection", JOptionPane.ERROR_MESSAGE);
					break;
			}
		}
	}
	
	private class SwingActionEraseEvents extends AbstractAction {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		public SwingActionEraseEvents() {
			putValue(NAME, "Erase Event File(s)...");
			putValue(SHORT_DESCRIPTION, "Select one or more daily event file(s) and erase from signage computer");
		}
		public void actionPerformed(ActionEvent e) {
	    	waitDialog.setVisible(true);	    	
			if (!isExistSignageFolder()) {
				waitDialog.setVisible(false);
				reportNoSignageFolder();
				return;
			}
			waitDialog.setVisible(false);
			
			JFileChooser fc = new JFileChooser(signageFolder);
			fc.setDialogTitle("Select daily event file(s) to erase from signage computer");
			fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
			fc.setMultiSelectionEnabled(true);
		    fc.setFileFilter(new ListedFileFilter("events_<DayOfWeek>.pptx", eventDailyFilenames)); // specific files only, based on what is used by signage computer's script		    

			int returnVal = fc.showDialog(frame, "Erase from signage");
			switch (returnVal) {
				case JFileChooser.APPROVE_OPTION: 
					File[] eventFilenamesArray = fc.getSelectedFiles();
					int filesErased = 0;
					
					// copy each selected file to the signage computer
					for (File eventFile : eventFilenamesArray) {
						String nameOnly = eventFile.getName();					// name only (in "current directory" on signage computer)
						if (nameOnly.indexOf(wildcardChar1) != -1 || nameOnly.indexOf(wildcardChar2) != -1) {
							JOptionPane.showMessageDialog(frame, "Wildcard characters not allowed in filename [" + nameOnly + "]", "Unable to erase", JOptionPane.ERROR_MESSAGE);
						}
					
						final String command = "erase " + nameOnly;
						ProcessBuilder pb = new ProcessBuilder("cmd", "/c", command);
						File dir = new File(signageFolder);
						pb.directory(dir);
						try {
							if (realActions) {
								pb.start();
							}
							else {
								JOptionPane.showMessageDialog(frame, "[" + pb.directory().toString() + "] " + pb.command().toString());
							}
							filesErased++;
						} catch (IOException e1) {
							JOptionPane.showMessageDialog(frame, "Error erasing file from signage computer [" + signageFolder + "]", "Unable to erase", JOptionPane.ERROR_MESSAGE);
						}
					}					
					
					JOptionPane.showMessageDialog(frame, "" + filesErased + " events file" + (eventFilenamesArray.length > 1 ? "s" : "") + " erased from signage computer\n" + signageFolder, "Event file(s) erased", JOptionPane.INFORMATION_MESSAGE);
					break;
				case JFileChooser.CANCEL_OPTION:
					JOptionPane.showMessageDialog(frame, "No file(s) erased.", "Cancelled", JOptionPane.INFORMATION_MESSAGE);
					break;
				default:	// error
					JOptionPane.showMessageDialog(frame, "Error while selecting files (nothing erased)", "Error during file selection", JOptionPane.ERROR_MESSAGE);
					break;
			}
		}
	}
	
	private class SwingActionExit extends AbstractAction {
	/**
	 * 
	 */
		private static final long serialVersionUID = 1L;
		public SwingActionExit() {
			putValue(NAME, "Exit");
			putValue(SHORT_DESCRIPTION, "Exit the program (without relaunching or rebooting signage computer)");
		}
		public void actionPerformed(ActionEvent e) {
			System.exit(0);
		}
	}
	
	private class SwingActionLoadConfig extends AbstractAction {
	/**
	 * 
	 */
		private static final long serialVersionUID = 1L;
		public SwingActionLoadConfig() {
			putValue(NAME, "About loading a config file...");
			putValue(SHORT_DESCRIPTION, "Describes how to specify a configuration file");
		}
		public void actionPerformed(ActionEvent e) {
			JOptionPane.showMessageDialog(frame, 
					"The first argument given on the command line that runs this program, if present,\nis the path to a configuration file e.g. signageUpdate.cfg\nYou can create a starting template by using the \"Save config file\" menu option.", 
					"About loading a configuration...", JOptionPane.INFORMATION_MESSAGE);
		}
	}

	private class SwingActionSaveConfig extends AbstractAction {
	/**
	 * 
	 */
		private static final long serialVersionUID = 1L;
		public SwingActionSaveConfig() {
			putValue(NAME, "Save a copy of config file...");
			putValue(SHORT_DESCRIPTION, "Save current configuration to an editable file (see \"About loading...\")");
		}
		public void actionPerformed(ActionEvent e) {
			JFileChooser fc = new JFileChooser();	// "current" local directory
			fc.setDialogTitle("Save signage configuration file to local file...");
			fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
			fc.setMultiSelectionEnabled(false);
		    fc.setFileFilter(new ExtensionFileFilter("cfg", new String[] { "cfg" }));	
		    fc.setSelectedFile(new File(configFilename));	// default file

			int returnVal = fc.showDialog(frame, "Save config");
			switch (returnVal) {
				case JFileChooser.APPROVE_OPTION: 					
					if (fc.getSelectedFile().exists()) {
						if (JOptionPane.showConfirmDialog(frame, "Config file exists - ok to overwrite?", "Overwrite file?", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) {
							return;
						}
					}
					
					saveConfigFile(fc.getSelectedFile());
					break;
				case JFileChooser.CANCEL_OPTION:
					// no action
					break;
				default:	// error
					JOptionPane.showMessageDialog(frame, "Error while selecting file (nothing saved)", "Error during file selection", JOptionPane.ERROR_MESSAGE);
					break;
			}		
		}
	}
}
