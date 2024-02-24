package com.neurotec.samples;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;

import javax.swing.*;
import javax.swing.Box.Filler;
import javax.swing.border.BevelBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.SoftBevelBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;

import com.neurotec.biometrics.*;
import com.neurotec.biometrics.swing.NFingerView;
import com.neurotec.biometrics.swing.NFingerViewBase.ShownImage;
import com.neurotec.devices.NDevice;
import com.neurotec.devices.NDeviceManager;
import com.neurotec.devices.NDeviceType;
import com.neurotec.devices.NFingerScanner;
import com.neurotec.samples.swing.ImageThumbnailFileChooser;
import com.neurotec.samples.util.Utils;
import com.neurotec.swing.NViewZoomSlider;
import com.neurotec.util.NIndexPair;
import com.neurotec.util.concurrent.CompletionHandler;
import hibernate.dao.EmployeeDao;
import hibernate.dao.EntryTimeDao;
import hibernate.dao.ExitTimeDao;
import hibernate.entity.Employee;
import hibernate.entity.EntryTime;
import hibernate.entity.ExitTime;

public final class VerifyFinger extends BasePanel implements ActionListener {

	// ===========================================================
	// Private static fields
	// ===========================================================

	private static final long serialVersionUID = 1L;

	private static final String SUBJECT_LEFT = "left";
	private static final String SUBJECT_RIGHT = "right";

	private static final String LEFT_LABEL_TEXT = "Image or template left: ";
	private static final String RIGHT_LABEL_TEXT = "Image or template right: ";

	// WLASNE //
	private static final String[] employeesTableColumns = {
			"ID",
			"Imie",
			"Nazwisko",
	};

	public List<Employee> employeeList;

	public Object[][] employees;

	// ===========================================================
	// Private fields
	// ===========================================================

	private NSubject subjectLeft;
	private NSubject subjectRight;
	private NFingerView viewLeft;
	private NFingerView viewRight;
	private NViewZoomSlider viewLeftZoomSlider;
	private NViewZoomSlider viewRightZoomSlider;
	private ImageThumbnailFileChooser fileChooser;

	private final TemplateCreationHandler templateCreationHandler = new TemplateCreationHandler();
	private final VerificationHandler verificationHandler = new VerificationHandler();

	private JCheckBox cbLeftShowBinarized;
	private JCheckBox cbRightShowBinarized;
	private JPanel centerPanel;
	private JButton clearButton;
	private JPanel clearButtonPanel;
	private JButton defaultButton;
	private JComboBox<String> farComboBox;
	private JPanel farPanel;
	private Filler filler1;
	private Filler filler2;
	private Filler filler3;
	private JPanel imageControlsPanel;
	private JLabel leftLabel;
	private JButton leftOpenButton;
	private JScrollPane leftScrollPane;
	private JPanel panelMain;
	private JPanel northPanel;
	private JLabel rightLabel;
	private JButton rightOpenButton;
	private JScrollPane rightScrollPane;
	private JPanel showBinarizedPanel;
	private JPanel southPanel;
	private JButton verifyButton;
	private JLabel verifyLabel;
	private JPanel verifyPanel;
	private JPanel leftBinarizedPanel;
	private JPanel rightBinarizedPanel;

	// WLASNE //
	private JTable employeesTable;

	//krzycha
	private final NDeviceManager deviceManager;
	private boolean scanning;
	private final CaptureCompletionHandler captureCompletionHandler = new CaptureCompletionHandler();
	private NFingerView view; // Może wymagać dostosowania do Twojego GUI
	private JButton btnCancel;
	private JButton btnForce;
	private JButton btnRefresh;
	private JButton btnScan;
	private JCheckBox cbAutomatic;
	private JLabel lblInfo;
	private JPanel panelButtons;
	private JPanel panelScanners;
	private JList<NDevice> scannerList;
	private JScrollPane scrollPaneList;
	private JPanel panelInfo;

	// ===========================================================
	// Public constructor
	// ===========================================================

	public VerifyFinger() {
		super();
		requiredLicenses = new ArrayList<String>();
		requiredLicenses.add("Biometrics.FingerExtraction");
		requiredLicenses.add("Biometrics.FingerMatching");
		optionalLicenses = new ArrayList<String>();
		optionalLicenses.add("Images.WSQ");

		subjectLeft = new NSubject();
		subjectRight = new NSubject();

		FingersTools.getInstance().getClient().setUseDeviceManager(true);
		deviceManager = FingersTools.getInstance().getClient().getDeviceManager();
		deviceManager.setDeviceTypes(EnumSet.of(NDeviceType.FINGER_SCANNER));
		deviceManager.initialize();

		EmployeeDao dao = new EmployeeDao();
		employeeList = dao.getEmployees();
	}

	// ===========================================================
	// Package private methods
	// ===========================================================

	void updateStatus(String status) {
		lblInfo.setText(status);
	}

	NSubject getSubject() {
		return subjectLeft;
	}

	NFingerScanner getSelectedScanner() {
		return (NFingerScanner) scannerList.getSelectedValue();
	}

	// ===========================================================
	// Private methods
	// ===========================================================

	public void updateScannerList() {
		DefaultListModel<NDevice> model = (DefaultListModel<NDevice>) scannerList.getModel();
		model.clear();
		for (NDevice device : deviceManager.getDevices()) {
			model.addElement(device);
		}
		NFingerScanner scanner = (NFingerScanner) FingersTools.getInstance().getClient().getFingerScanner();
		if ((scanner == null) && (model.getSize() > 0)) {
			scannerList.setSelectedIndex(0);
		} else if (scanner != null) {
			scannerList.setSelectedValue(scanner, true);
		}
	}

	public void cancelCapturing() {
		FingersTools.getInstance().getClient().cancel();
	}

	private void updateTable() {
		EmployeeDao dao = new EmployeeDao();
		this.employeeList = dao.getEmployees();
		employees = new Object[employeeList.size()][3];

		for (int i = 0; i < employeeList.size(); i++) {
			employees[i][0] = employeeList.get(i).getId();
			employees[i][1] = employeeList.get(i).getFirstName();
			employees[i][2] = employeeList.get(i).getLastName();
		}

		DefaultTableModel model = new DefaultTableModel(employees, employeesTableColumns);
		employeesTable.setModel(model);
		employeesTable.updateUI();
	}


	private void startCapturing() {
		lblInfo.setText("");
		if (FingersTools.getInstance().getClient().getFingerScanner() == null) {
			SwingUtilities.invokeLater(() -> {
				JOptionPane.showMessageDialog(this, "Proszę wybrać skaner z listy.", "Nie wybrano skanera", JOptionPane.PLAIN_MESSAGE);
			});
			return;
		}

		// Resetuj NSubject i widok przed rozpoczęciem nowego skanowania
		subjectLeft.clear();
		viewLeft.setFinger(null);
		viewLeft.setShownImage(ShownImage.ORIGINAL);

		// Utwórz nowy odcisk palca
		NFinger finger = new NFinger();

		// Ustaw opcje skanowania
		if (!cbAutomatic.isSelected()) {
			finger.setCaptureOptions(EnumSet.of(NBiometricCaptureOption.MANUAL));
		}

		// Dodaj odcisk do NSubject i widoku
		subjectLeft.getFingers().add(finger);
		viewLeft.setFinger(finger);
		viewLeft.setShownImage(ShownImage.ORIGINAL);

		// Rozpocznij skanowanie
		NBiometricTask task = FingersTools.getInstance().getClient().createTask(EnumSet.of(NBiometricOperation.CAPTURE, NBiometricOperation.CREATE_TEMPLATE), subjectLeft);
		FingersTools.getInstance().getClient().performTask(task, null, captureCompletionHandler);
		scanning = true;
		updateControls();
	}


//	private void loadItem(String position) throws IOException {
//		fileChooser.setMultiSelectionEnabled(false);
//		if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
//			resetMatedMinutiaeOnViews();
//			verifyLabel.setText("");
//			NSubject subjectTmp = null;
//			NFinger finger = null;
//			try {
//				subjectTmp = NSubject.fromFile(fileChooser.getSelectedFile().getAbsolutePath());
//				FingerCollection fingers = subjectTmp.getFingers();
//				if (fingers.isEmpty()) {
//					subjectTmp = null;
//					throw new IllegalArgumentException("Template contains no finger records.");
//				}
//				finger = fingers.get(0);
//				templateCreationHandler.completed(NBiometricStatus.OK, position);
//			} catch (UnsupportedOperationException e) {
//				// Ignore. UnsupportedOperationException means file is not a valid template.
//			}
//
//			// If file is not a template, try to load it as an image.
//			if (subjectTmp == null) {
//				finger = new NFinger();
//				finger.setFileName(fileChooser.getSelectedFile().getAbsolutePath());
//				subjectTmp = new NSubject();
//				subjectTmp.getFingers().add(finger);
//				updateFingersTools();
//				FingersTools.getInstance().getClient().createTemplate(subjectTmp, position, templateCreationHandler);
//			}
//
//			if (SUBJECT_LEFT.equals(position)) {
//				subjectLeft = subjectTmp;
//				leftLabel.setText(fileChooser.getSelectedFile().getAbsolutePath());
//				viewLeft.setFinger(finger);
//			} else if (SUBJECT_RIGHT.equals(position)) {
//				subjectRight = subjectTmp;
//				rightLabel.setText(fileChooser.getSelectedFile().getAbsolutePath());
//				viewRight.setFinger(finger);
//			} else {
//				throw new AssertionError("Unknown subject position: " + position);
//			}
//		}
//	}

	private void verify() {
		updateFingersTools();
		FingersTools.getInstance().getClient().verify(subjectLeft, subjectRight, null, verificationHandler);
	}

	private void clear() {
		viewLeft.setFinger(null);
//		viewRight.setFinger(null);
		subjectLeft.clear();
//		subjectRight.clear();
		updateControls();
		verifyLabel.setText(" ");
		leftLabel.setText(LEFT_LABEL_TEXT);
		rightLabel.setText(RIGHT_LABEL_TEXT);
	}



	private void resetMatedMinutiaeOnViews() {
		viewLeft.setMatedMinutiae(new NIndexPair[0]);
//		viewRight.setMatedMinutiae(new NIndexPair[0]);
		viewLeft.setTree(new NIndexPair[0]);
//		viewRight.setTree(new NIndexPair[0]);
	}

	// ===========================================================
	// Protected methods
	// ===========================================================

	@Override
	protected void initGUI() {
		GridBagConstraints gridBagConstraints;
		setLayout(new BorderLayout());

		farComboBox = new JComboBox<>();
//		fileChooser = new ImageThumbnailFileChooser();
//		fileChooser.setIcon(Utils.createIconImage("images/Logo16x16.png"));

		panelLicensing = new LicensingPanel(requiredLicenses, optionalLicenses);
		add(panelLicensing, java.awt.BorderLayout.NORTH);

		panelMain = new JPanel();
		panelMain.setLayout(new BorderLayout());
		add(panelMain, BorderLayout.CENTER);
		{
			northPanel = new JPanel();
			panelMain.add(northPanel, BorderLayout.NORTH);
//			{
//				leftOpenButton = new JButton();
//				leftOpenButton.setText("Open");
//				leftOpenButton.addActionListener(this);
//				northPanel.add(leftOpenButton);
//			}
			panelScanners = new JPanel();
			panelScanners.setBorder(BorderFactory.createTitledBorder("Lista skanerów"));
			panelScanners.setLayout(new BorderLayout());
			panelMain.add(panelScanners, BorderLayout.NORTH);
			{
				scrollPaneList = new JScrollPane();
				scrollPaneList.setPreferredSize(new Dimension(0, 90));
				panelScanners.add(scrollPaneList, BorderLayout.CENTER);
				{
					scannerList = new JList<>();
					scannerList.setModel(new DefaultListModel<>());
					scannerList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
					scannerList.setBorder(LineBorder.createBlackLineBorder());
					scannerList.addListSelectionListener(new ScannerSelectionListener());
					scrollPaneList.setViewportView(scannerList);

				}
			}
			{
				panelButtons = new JPanel();
				panelButtons.setLayout(new FlowLayout(FlowLayout.LEADING));
				panelScanners.add(panelButtons, BorderLayout.SOUTH);
				{
					btnRefresh = new JButton();
					btnRefresh.setText("Odśwież listę");
					btnRefresh.addActionListener(this);
					panelButtons.add(btnRefresh);
				}
				{
					btnScan = new JButton();
					btnScan.setText("Skanuj");
					btnScan.addActionListener(this);
					panelButtons.add(btnScan);
				}
				{
					btnCancel = new JButton();
					btnCancel.setText("Cancel");
					btnCancel.setEnabled(false);
					btnCancel.addActionListener(this);
					panelButtons.add(btnCancel);
				}
				{
					btnForce = new JButton();
					btnForce.setText("Odswiez");
					btnForce.addActionListener(this);
					panelButtons.add(btnForce);
				}
				{
					cbAutomatic = new JCheckBox();
					cbAutomatic.setSelected(true);
					cbAutomatic.setText("Skanuj automatycznie");
					panelButtons.add(cbAutomatic);
				}
				{
					farPanel = new JPanel();
					farPanel.setBorder(BorderFactory.createTitledBorder(null, "Stopień FAR", TitledBorder.CENTER, TitledBorder.DEFAULT_POSITION));
					farPanel.setLayout(new GridBagLayout());
					panelButtons.add(farPanel);
					{
						char c = new DecimalFormatSymbols().getPercent();
						DefaultComboBoxModel<String> model = (DefaultComboBoxModel<String>) farComboBox.getModel();
						NumberFormat nf = NumberFormat.getNumberInstance();
						nf.setMaximumFractionDigits(5);
						model.addElement(nf.format(0.1) + c);
						model.addElement(nf.format(0.01) + c);
						model.addElement(nf.format(0.001) + c);
						farComboBox.setSelectedIndex(1);
						farComboBox.setEditable(true);
						farComboBox.setModel(model);
						gridBagConstraints = new GridBagConstraints();
						gridBagConstraints.gridx = 0;
						gridBagConstraints.gridy = 0;
						gridBagConstraints.insets = new Insets(5, 5, 5, 5);
						farPanel.add(farComboBox, gridBagConstraints);
					}
					{
						defaultButton = new JButton();
						defaultButton.setText("Domyślny");
						defaultButton.addActionListener(this);
						gridBagConstraints = new GridBagConstraints();
						gridBagConstraints.gridx = 1;
						gridBagConstraints.gridy = 0;
						gridBagConstraints.insets = new Insets(3, 3, 3, 3);
						farPanel.add(defaultButton, gridBagConstraints);
					}
				}
			}

//			{
//				rightOpenButton = new JButton();
//				rightOpenButton.setText("Open");
//				rightOpenButton.addActionListener(this);
//				northPanel.add(rightOpenButton);
//			}
		}
		{
			centerPanel = new JPanel();
			centerPanel.setLayout(new GridLayout(1, 2, 5, 0));
			panelMain.add(centerPanel, BorderLayout.CENTER);
			{
				leftScrollPane = new JScrollPane();
				leftScrollPane.setPreferredSize(new Dimension(200, 200));
				centerPanel.add(leftScrollPane);
				{
					viewLeft = new NFingerView();
					viewLeft.setShownImage(ShownImage.RESULT);
					viewLeft.setAutofit(true);
					leftScrollPane.setViewportView(viewLeft);
				}
			}
			{
				rightScrollPane = new JScrollPane();
				rightScrollPane.setPreferredSize(new Dimension(200, 200));
				rightScrollPane.setBorder(BorderFactory.createTitledBorder("Pracownicy"));
				centerPanel.add(rightScrollPane);
				{
					employees = new Object[employeeList.size()][3];

					employeesTable = new JTable(employees, employeesTableColumns);
					employeesTable.setDefaultEditor(Object.class, null);
					employeesTable.setFillsViewportHeight(true);
					employeesTable.setVisible(true);
					rightScrollPane.setViewportView(employeesTable);

					updateTable();

					employeesTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
						@Override
						public void valueChanged(ListSelectionEvent event) {
							if (employeesTable.getSelectedRow() > -1) {
								// print first column value from selected row
                                try {
									String pathToFingerprint = employeeList.get(employeesTable.getSelectedRow()).getFingerprint();
                                    subjectRight = NSubject.fromFile(pathToFingerprint);
									rightLabel.setText(pathToFingerprint);
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
							}
						}
					});
//					viewRight = new NFingerView();
//					viewRight.setAutofit(true);
//					viewRight.addMouseListener(new MouseAdapter() {
//						@Override
//						public void mouseClicked(MouseEvent ev) {
//							super.mouseClicked(ev);
//							if (ev.getButton() == MouseEvent.BUTTON3) {
//								cbLeftShowBinarized.doClick();
//							}
//						}
//					});
//					rightScrollPane.setViewportView(viewRight);
				}
			}
		}
		{
			southPanel = new JPanel();
			southPanel.setLayout(new BorderLayout());
			panelMain.add(southPanel, BorderLayout.SOUTH);
			{
				imageControlsPanel = new JPanel();
				imageControlsPanel.setLayout(new BorderLayout());
				southPanel.add(imageControlsPanel, BorderLayout.NORTH);
				{
					showBinarizedPanel = new JPanel();
					showBinarizedPanel.setLayout(new BorderLayout());
					imageControlsPanel.add(showBinarizedPanel, BorderLayout.NORTH);
					{
						leftBinarizedPanel = new JPanel();
						leftBinarizedPanel.setLayout(new BorderLayout());
						showBinarizedPanel.add(leftBinarizedPanel, BorderLayout.WEST);
						{
							viewLeftZoomSlider = new NViewZoomSlider();
							viewLeftZoomSlider.setView(viewLeft);
							leftBinarizedPanel.add(viewLeftZoomSlider, BorderLayout.WEST);
						}
						{
							cbLeftShowBinarized = new JCheckBox();
							cbLeftShowBinarized.setText("Pokaz przetworzony obraz");
							cbLeftShowBinarized.addActionListener(this);
							leftBinarizedPanel.add(cbLeftShowBinarized, BorderLayout.CENTER);
						}
					}
//					{
//						rightBinarizedPanel = new JPanel();
//						rightBinarizedPanel.setLayout(new BorderLayout());
//						showBinarizedPanel.add(rightBinarizedPanel, BorderLayout.EAST);
//						{
//							viewRightZoomSlider = new NViewZoomSlider();
//							viewRightZoomSlider.setView(viewRight);
//							rightBinarizedPanel.add(viewRightZoomSlider, BorderLayout.EAST);
//						}
//						{
//							cbRightShowBinarized = new JCheckBox();
//							cbRightShowBinarized.setText("Show binarized image");
//							cbRightShowBinarized.addActionListener(this);
//							rightBinarizedPanel.add(cbRightShowBinarized, BorderLayout.CENTER);
//						}
//					}
				}
				{
					clearButtonPanel = new JPanel();
					imageControlsPanel.add(clearButtonPanel, BorderLayout.CENTER);
					{
						clearButton = new JButton();
						clearButton.setText("Wyczyść zdjęcie");
						clearButton.addActionListener(this);
						clearButtonPanel.add(clearButton);
					}
				}
			}
			{
				panelInfo = new JPanel();
				panelInfo.setBorder(new SoftBevelBorder(BevelBorder.LOWERED));
				panelInfo.setLayout(new GridLayout(1, 1));
				southPanel.add(panelInfo, BorderLayout.SOUTH);
				{
					lblInfo = new JLabel();
					lblInfo.setText(" ");
					panelInfo.add(lblInfo);
				}
			}
			{
				verifyPanel = new JPanel();
				verifyPanel.setLayout(new BoxLayout(verifyPanel, BoxLayout.Y_AXIS));
				southPanel.add(verifyPanel, BorderLayout.WEST);
				{
					leftLabel = new JLabel();
					leftLabel.setText(LEFT_LABEL_TEXT);
					verifyPanel.add(leftLabel);
				}
				{
					filler1 = new Filler(new Dimension(0, 3), new Dimension(0, 3), new Dimension(32767, 3));
					verifyPanel.add(filler1);
				}
				{
					rightLabel = new JLabel();
					rightLabel.setText(RIGHT_LABEL_TEXT);
					verifyPanel.add(rightLabel);
				}
				{
					filler2 = new Filler(new Dimension(0, 3), new Dimension(0, 3), new Dimension(32767, 3));
					verifyPanel.add(filler2);
				}
				{
					verifyButton = new JButton();
					verifyButton.setText("Weryfikacja");
					verifyButton.setEnabled(false);
					verifyButton.addActionListener(this);
					verifyPanel.add(verifyButton);
				}
				{
					filler3 = new Filler(new Dimension(0, 3), new Dimension(0, 3), new Dimension(32767, 3));
					verifyPanel.add(filler3);
				}
				{
					verifyLabel = new JLabel();
					verifyLabel.setText("     ");
					verifyPanel.add(verifyLabel);
				}
			}
		}
		cbLeftShowBinarized.doClick();
//		cbRightShowBinarized.doClick();
	}

	@Override
	protected void setDefaultValues() {
		farComboBox.setSelectedItem(Utils.matchingThresholdToString(FingersTools.getInstance().getDefaultClient().getMatchingThreshold()));
	}

	@Override
	protected void updateControls() {
		if (subjectLeft.getFingers().isEmpty()
			|| (subjectLeft.getFingers().get(0).getObjects().get(0).getTemplate() == null)
			|| subjectRight.getFingers().isEmpty()
			|| (subjectRight.getFingers().get(0).getObjects().get(0).getTemplate() == null)) {
			verifyButton.setEnabled(false);
		} else {
			verifyButton.setEnabled(true);
		}
	}

	@Override
	protected void updateFingersTools() {
		FingersTools.getInstance().getClient().reset();
		FingersTools.getInstance().getClient().setFingersReturnBinarizedImage(true);
		FingersTools.getInstance().getClient().setMatchingWithDetails(true);
		try {
			FingersTools.getInstance().getClient().setMatchingThreshold(Utils.matchingThresholdFromString(farComboBox.getSelectedItem().toString()));
		} catch (ParseException e) {
			e.printStackTrace();
			FingersTools.getInstance().getClient().setMatchingThreshold(FingersTools.getInstance().getDefaultClient().getMatchingThreshold());
			farComboBox.setSelectedItem(Utils.matchingThresholdToString(FingersTools.getInstance().getDefaultClient().getMatchingThreshold()));
			SwingUtilities.invokeLater(() -> { JOptionPane.showMessageDialog(this, "FAR jest nieprawidłowy. Zmiana na wartość domyślną.", "Error", JOptionPane.ERROR_MESSAGE); });
		}
	}

	// ===========================================================
	// Package private methods
	// ===========================================================

	private void registerAttendance(String type) {
		int selectedRow = employeesTable.getSelectedRow();
		if (selectedRow != -1) {
			Employee selectedEmployee = employeeList.get(selectedRow);

			if ("wejście".equals(type)) {
				EntryTime entryTime = new EntryTime(selectedEmployee, LocalDateTime.now());
				new EntryTimeDao().addEntryTime(entryTime);
				JOptionPane.showMessageDialog(this, "Zarejestrowano wejście pracownika: " + selectedEmployee.getFirstName() + " " + selectedEmployee.getLastName());
			} else if ("wyjście".equals(type)) {
				ExitTime exitTime = new ExitTime(selectedEmployee, LocalDateTime.now());
				new ExitTimeDao().addExitTime(exitTime);
				JOptionPane.showMessageDialog(this, "Zarejestrowano wyjście pracownika: " + selectedEmployee.getFirstName() + " " + selectedEmployee.getLastName());
			}
		} else {
			JOptionPane.showMessageDialog(this, "Nie wybrano pracownika z listy.", "Błąd", JOptionPane.ERROR_MESSAGE);
		}
	}

	void updateLabel(String msg) {
		verifyLabel.setText(msg);
	}

	NSubject getLeft() {
		return subjectLeft;
	}

	NSubject getRight() {
		return subjectRight;
	}

	// ===========================================================
	// Event handling
	// ===========================================================

	@Override
	public void actionPerformed(ActionEvent ev) {
		try {
			if (ev.getSource() == defaultButton) {
				farComboBox.setSelectedItem(Utils.matchingThresholdToString(FingersTools.getInstance().getDefaultClient().getMatchingThreshold()));
			} else if (ev.getSource() == verifyButton) {
				// zmienione verify jak sa dwa obiekty dopiero dziala (chyba xd)
				if (subjectLeft != null && subjectLeft.getStatus() == NBiometricStatus.OK && subjectRight != null) {
					verify();
				} else {
					JOptionPane.showMessageDialog(this, "Najpierw zeskanuj odcisk palca i wybierz szablon pracownika.", "Brak danych do weryfikacji", JOptionPane.WARNING_MESSAGE);
				}
			}
//			else if (ev.getSource() == leftOpenButton) {
//				loadItem(SUBJECT_LEFT);
//			} else if (ev.getSource() == rightOpenButton) {
//				loadItem(SUBJECT_RIGHT); }

			//przekopiowane z enrollFromScanner od
			else if (ev.getSource() == btnRefresh) {
				updateScannerList();
			} else if (ev.getSource() == btnScan) {
				startCapturing();
			} else if (ev.getSource() == btnCancel) {
				cancelCapturing();
			} else if (ev.getSource() == btnForce) {
//				FingersTools.getInstance().getClient().force();
				updateTable();
			}
			// przekopiowane do
			else if (ev.getSource() == clearButton) {
				clear();
			} else if (ev.getSource() == cbLeftShowBinarized) {
				if (cbLeftShowBinarized.isSelected()) {
					viewLeft.setShownImage(ShownImage.RESULT);
				} else {
					viewLeft.setShownImage(ShownImage.ORIGINAL);
				}
			} else if (ev.getSource() == cbRightShowBinarized) {
				if (cbRightShowBinarized.isSelected()) {
					viewRight.setShownImage(ShownImage.RESULT);
				} else {
					viewRight.setShownImage(ShownImage.ORIGINAL);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			SwingUtilities.invokeLater(() -> { JOptionPane.showMessageDialog(this, e, "Błąd", JOptionPane.ERROR_MESSAGE); });
		}
	}

	// ===========================================================
	// Inner classes
	// ===========================================================

	private class TemplateCreationHandler implements CompletionHandler<NBiometricStatus, String> {



		@Override
		public void completed(final NBiometricStatus status, final String subject) {
			SwingUtilities.invokeLater(new Runnable() {

				@Override
				public void run() {
					if (status != NBiometricStatus.OK) {
						SwingUtilities.invokeLater(() -> { JOptionPane.showMessageDialog(VerifyFinger.this, "Obraz nie został utworzony" + status, "Błąd", JOptionPane.WARNING_MESSAGE); });
					}
					updateControls();
				}

			});
		}

		@Override
		public void failed(final Throwable th, final String subject) {
			SwingUtilities.invokeLater(new Runnable() {

				@Override
				public void run() {
					showError(th);
				}

			});
		}

	}

	private class VerificationHandler implements CompletionHandler<NBiometricStatus, String> {

		@Override
		public void completed(final NBiometricStatus status, final String subject) {
			SwingUtilities.invokeLater(() -> {
				if (status == NBiometricStatus.OK) {
					int score = getLeft().getMatchingResults().get(0).getScore();
					String msg = "Wynik porównania odcisków: " + score;
					updateLabel(msg);

					NIndexPair[] matedMinutiae = getLeft().getMatchingResults().get(0).getMatchingDetails().getFingers().get(0).getMatedMinutiae();
					viewLeft.setMatedMinutiaIndex(0);
					viewLeft.setMatedMinutiae(matedMinutiae);
					viewLeft.prepareTree();

					// Dodajemy przyciski do dialogu
					Object[] options = {"Wejście", "Wyjście"};
					int choice = JOptionPane.showOptionDialog(
							VerifyFinger.this,
							"Wynik porównania odcisków: " + score + "\nWybierz akcję:",
							"Weryfikacja",
							JOptionPane.YES_NO_OPTION,
							JOptionPane.PLAIN_MESSAGE,
							null,
							options,
							options[0]
					);

					if (choice == JOptionPane.YES_OPTION) {
						registerAttendance("wejście");
					} else if (choice == JOptionPane.NO_OPTION) {
						registerAttendance("wyjście");
					}

				} else {
					JOptionPane.showMessageDialog(VerifyFinger.this, "Odcisk palca nie pasuje do wybranego pracownika.", "Brak powiązania", JOptionPane.WARNING_MESSAGE);
				}
			});
		}

		@Override
		public void failed(final Throwable th, final String subject) {
			SwingUtilities.invokeLater(new Runnable() {

				@Override
				public void run() {
					showError(th);
				}

			});
		}

	}


//	private void updateShownImage() {
//		if (cbShowBinarized.isSelected()) {
//			view.setShownImage(ShownImage.RESULT);
//		} else {
//			view.setShownImage(ShownImage.ORIGINAL);
//		}
//	}

	private class CaptureCompletionHandler implements CompletionHandler<NBiometricTask, Object> {

		@Override
		public void completed(final NBiometricTask result, final Object attachment) {
			SwingUtilities.invokeLater(new Runnable() {

				@Override
				public void run() {
					scanning = false;
					//updateShownImage();
					if (result.getStatus() == NBiometricStatus.OK) {
						updateStatus("Jakość: " + getSubject().getFingers().get(0).getObjects().get(0).getQuality());
					} else {
						updateStatus(result.getStatus().toString());
					}
					updateControls();
				}

			});
		}

		@Override
		public void failed(final Throwable th, final Object attachment) {
			SwingUtilities.invokeLater(new Runnable() {

				@Override
				public void run() {
					scanning = false;
					//updateShownImage();
					showError(th);
					updateControls();
				}

			});
		}

	}
	private class ScannerSelectionListener implements ListSelectionListener {

		@Override
		public void valueChanged(ListSelectionEvent e) {
			FingersTools.getInstance().getClient().setFingerScanner(getSelectedScanner());
		}

	}

}
