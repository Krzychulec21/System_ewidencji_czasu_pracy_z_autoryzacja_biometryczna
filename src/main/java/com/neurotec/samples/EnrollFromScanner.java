package com.neurotec.samples;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.border.BevelBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.SoftBevelBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.neurotec.biometrics.NBiometricCaptureOption;
import com.neurotec.biometrics.NBiometricOperation;
import com.neurotec.biometrics.NBiometricStatus;
import com.neurotec.biometrics.NBiometricTask;
import com.neurotec.biometrics.NFinger;
import com.neurotec.biometrics.NSubject;
import com.neurotec.biometrics.swing.NFingerView;
import com.neurotec.biometrics.swing.NFingerViewBase.ShownImage;
import com.neurotec.devices.NDevice;
import com.neurotec.devices.NDeviceManager;
import com.neurotec.devices.NDeviceType;
import com.neurotec.devices.NFingerScanner;
import com.neurotec.images.NImages;
import com.neurotec.io.NFile;
import com.neurotec.samples.util.Utils;
import com.neurotec.swing.NViewZoomSlider;
import com.neurotec.util.concurrent.CompletionHandler;

public final class EnrollFromScanner extends BasePanel implements ActionListener {

	// ===========================================================
	// Private static fields
	// ===========================================================

	private static final long serialVersionUID = 1L;

	// ===========================================================
	// Private fields
	// ===========================================================

	private NSubject subject;
	private final NDeviceManager deviceManager;
	private boolean scanning;

	private final CaptureCompletionHandler captureCompletionHandler = new CaptureCompletionHandler();

	private NFingerView view;
	private JFileChooser fcImage;
	private JFileChooser fcTemplate;
	private File oldImageFile;
	private File oldTemplateFile;

	private JButton btnCancel;
	private JButton btnForce;
	private JButton btnRefresh;
	private JButton btnSaveImage;
	private JButton btnSaveTemplate;
	private JButton btnScan;
	private JCheckBox cbAutomatic;
	private JCheckBox cbShowBinarized;
	private JLabel lblInfo;
	private JPanel panelButtons;
	private JPanel panelInfo;
	private JPanel panelMain;
	private JPanel panelSave;
	private JPanel panelScanners;
	private JPanel panelSouth;
	private JList<NDevice> scannerList;
	private JScrollPane scrollPane;
	private JScrollPane scrollPaneList;

	// ===========================================================
	// Public constructor
	// ===========================================================

	public EnrollFromScanner() {
		super();
		requiredLicenses = new ArrayList<String>();
		requiredLicenses.add("Biometrics.FingerExtraction");
		requiredLicenses.add("Devices.FingerScanners");
		optionalLicenses = new ArrayList<String>();
		optionalLicenses.add("Images.WSQ");

		FingersTools.getInstance().getClient().setUseDeviceManager(true);
		deviceManager = FingersTools.getInstance().getClient().getDeviceManager();
		deviceManager.setDeviceTypes(EnumSet.of(NDeviceType.FINGER_SCANNER));
		deviceManager.initialize();
	}

	// ===========================================================
	// Private methods
	// ===========================================================

	private void startCapturing() {
		lblInfo.setText("");
		if (FingersTools.getInstance().getClient().getFingerScanner() == null) {
			SwingUtilities.invokeLater(() -> { JOptionPane.showMessageDialog(this, "Please select scanner from the list.", "No scanner selected", JOptionPane.PLAIN_MESSAGE); });
			return;
		}

		// Create a finger.
		NFinger finger = new NFinger();

		// Set Manual capturing mode if automatic isn't selected.
		if (!cbAutomatic.isSelected()) {
			finger.setCaptureOptions(EnumSet.of(NBiometricCaptureOption.MANUAL));
		}

		// Add finger to subject and finger view.
		subject = new NSubject();
		subject.getFingers().add(finger);
		view.setFinger(finger);
		view.setShownImage(ShownImage.ORIGINAL);

		// Begin capturing.
		NBiometricTask task = FingersTools.getInstance().getClient().createTask(EnumSet.of(NBiometricOperation.CAPTURE, NBiometricOperation.CREATE_TEMPLATE), subject);
		FingersTools.getInstance().getClient().performTask(task, null, captureCompletionHandler);
		scanning = true;
		updateControls();
	}

	private void saveTemplate() throws IOException {
		if (subject != null) {
			if (oldTemplateFile != null) {
				fcTemplate.setSelectedFile(oldTemplateFile);
			}
			if (fcTemplate.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
				oldTemplateFile = fcTemplate.getSelectedFile();
				NFile.writeAllBytes(fcTemplate.getSelectedFile().getAbsolutePath(), subject.getTemplateBuffer());
			}
		}
	}

	private void saveImage() throws IOException {
		if (subject != null) {
			if (oldImageFile != null) {
				fcImage.setSelectedFile(oldImageFile);
			}
			if (fcImage.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
				oldImageFile = fcImage.getSelectedFile();
				if (cbShowBinarized.isSelected()) {
					subject.getFingers().get(0).getBinarizedImage().save(fcImage.getSelectedFile().getAbsolutePath());
				} else {
					subject.getFingers().get(0).getImage().save(fcImage.getSelectedFile().getAbsolutePath());
				}
			}
		}
	}

	private void updateShownImage() {
		if (cbShowBinarized.isSelected()) {
			view.setShownImage(ShownImage.RESULT);
		} else {
			view.setShownImage(ShownImage.ORIGINAL);
		}
	}

	// ===========================================================
	// Package private methods
	// ===========================================================

	void updateStatus(String status) {
		lblInfo.setText(status);
	}

	NSubject getSubject() {
		return subject;
	}

	NFingerScanner getSelectedScanner() {
		return (NFingerScanner) scannerList.getSelectedValue();
	}

	// ===========================================================
	// Protected methods
	// ===========================================================

	@Override
	protected void initGUI() {
		setLayout(new BorderLayout());

		panelLicensing = new LicensingPanel(requiredLicenses, optionalLicenses);
		add(panelLicensing, java.awt.BorderLayout.NORTH);

		panelMain = new JPanel();
		panelMain.setLayout(new BorderLayout());
		add(panelMain, BorderLayout.CENTER);
		{
			panelScanners = new JPanel();
			panelScanners.setBorder(BorderFactory.createTitledBorder("Scanners list"));
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
					btnRefresh.setText("Refresh list");
					btnRefresh.addActionListener(this);
					panelButtons.add(btnRefresh);
				}
				{
					btnScan = new JButton();
					btnScan.setText("Scan");
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
					btnForce.setText("Force");
					btnForce.addActionListener(this);
					panelButtons.add(btnForce);
				}
				{
					cbAutomatic = new JCheckBox();
					cbAutomatic.setSelected(true);
					cbAutomatic.setText("Scan automatically");
					panelButtons.add(cbAutomatic);
				}
			}
		}
		{
			scrollPane = new JScrollPane();
			panelMain.add(scrollPane, BorderLayout.CENTER);
			{
				view = new NFingerView();
				view.setShownImage(ShownImage.RESULT);
				view.setAutofit(true);
				view.addMouseListener(new MouseAdapter() {
					@Override
					public void mouseClicked(MouseEvent ev) {
						super.mouseClicked(ev);
						if (ev.getButton() == MouseEvent.BUTTON3) {
							cbShowBinarized.doClick();
						}
					}

				});
				scrollPane.setViewportView(view);
			}
		}
		{
			panelSouth = new JPanel();
			panelSouth.setLayout(new BorderLayout());
			panelMain.add(panelSouth, BorderLayout.SOUTH);
			{
				panelInfo = new JPanel();
				panelInfo.setBorder(new SoftBevelBorder(BevelBorder.LOWERED));
				panelInfo.setLayout(new GridLayout(1, 1));
				panelSouth.add(panelInfo, BorderLayout.NORTH);
				{
					lblInfo = new JLabel();
					lblInfo.setText(" ");
					panelInfo.add(lblInfo);
				}
			}
			{
				panelSave = new JPanel();
				panelSave.setLayout(new FlowLayout(FlowLayout.LEADING));
				panelSouth.add(panelSave, BorderLayout.WEST);
				{
					btnSaveImage = new JButton();
					btnSaveImage.setText("Save image");
					btnSaveImage.setEnabled(false);
					btnSaveImage.addActionListener(this);
					panelSave.add(btnSaveImage);
				}
				{
					btnSaveTemplate = new JButton();
					btnSaveTemplate.setText("Save template");
					btnSaveTemplate.setEnabled(false);
					btnSaveTemplate.addActionListener(this);
					panelSave.add(btnSaveTemplate);
				}
				{
					cbShowBinarized = new JCheckBox();
					cbShowBinarized.setSelected(true);
					cbShowBinarized.setText("Show binarized image");
					cbShowBinarized.addActionListener(this);
					panelSave.add(cbShowBinarized);

				}
			}
			{
				NViewZoomSlider zoomSlider = new NViewZoomSlider();
				zoomSlider.setView(view);
				panelSouth.add(zoomSlider, BorderLayout.EAST);
			}
		}

		fcImage = new JFileChooser();
		fcImage.setFileFilter(new Utils.ImageFileFilter(NImages.getSaveFileFilter()));
		fcTemplate = new JFileChooser();
	}

	@Override
	protected void setDefaultValues() {
		// No default values.
	}

	@Override
	protected void updateControls() {
		btnScan.setEnabled(!scanning);
		btnCancel.setEnabled(scanning);
		btnForce.setEnabled(scanning);
		btnRefresh.setEnabled(!scanning);
		btnSaveTemplate.setEnabled(!scanning && (subject != null) && (subject.getStatus() == NBiometricStatus.OK));
		btnSaveImage.setEnabled(!scanning && (subject != null) && (subject.getStatus() == NBiometricStatus.OK));
		cbShowBinarized.setEnabled(!scanning);
		cbAutomatic.setEnabled(!scanning);
	}

	@Override
	protected void updateFingersTools() {
		FingersTools.getInstance().getClient().reset();
		FingersTools.getInstance().getClient().setUseDeviceManager(true);
		FingersTools.getInstance().getClient().setFingersReturnBinarizedImage(true);
	}

	// ===========================================================
	// Public methods
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

	// ===========================================================
	// Event handling
	// ===========================================================

	@Override
	public void actionPerformed(ActionEvent ev) {
		try {
			if (ev.getSource() == btnRefresh) {
				updateScannerList();
			} else if (ev.getSource() == btnScan) {
				startCapturing();
			} else if (ev.getSource() == btnCancel) {
				cancelCapturing();
			} else if (ev.getSource() == btnForce) {
				FingersTools.getInstance().getClient().force();
			} else if (ev.getSource() == btnSaveImage) {
				saveImage();
			} else if (ev.getSource() == btnSaveTemplate) {
				saveTemplate();
			} else if (ev.getSource() == cbShowBinarized) {
				updateShownImage();
			}
		} catch (Exception e) {
			e.printStackTrace();
			SwingUtilities.invokeLater(() -> { JOptionPane.showMessageDialog(this, e.toString(), "Error", JOptionPane.ERROR_MESSAGE); });
		}
	}

	// ===========================================================
	// Inner classes
	// ===========================================================


	private class CaptureCompletionHandler implements CompletionHandler<NBiometricTask, Object> {

		@Override
		public void completed(final NBiometricTask result, final Object attachment) {
			SwingUtilities.invokeLater(new Runnable() {

				@Override
				public void run() {
					scanning = false;
					updateShownImage();
					if (result.getStatus() == NBiometricStatus.OK) {
						updateStatus("Quality: " + getSubject().getFingers().get(0).getObjects().get(0).getQuality());
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
					updateShownImage();
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
