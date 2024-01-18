package com.neurotec.samples;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;

import com.neurotec.biometrics.NBiometricStatus;
import com.neurotec.biometrics.NFinger;
import com.neurotec.biometrics.NSubject;
import com.neurotec.biometrics.swing.NFingerView;
import com.neurotec.biometrics.swing.NFingerViewBase.ShownImage;
import com.neurotec.images.NImage;
import com.neurotec.images.NImages;
import com.neurotec.io.NFile;
import com.neurotec.samples.swing.ImageThumbnailFileChooser;
import com.neurotec.samples.util.Utils;
import com.neurotec.swing.NViewZoomSlider;
import com.neurotec.util.concurrent.CompletionHandler;

public final class EnrollFromImage extends BasePanel implements ActionListener {

	// ===========================================================
	// Private static fields
	// ===========================================================

	private static final long serialVersionUID = 1L;

	// ===========================================================
	// Private fields
	// ===========================================================

	private NSubject subject;

	private final TemplateCreationHandler templateCreationHandler = new TemplateCreationHandler();

	private NFingerView viewImage;
	private NFingerView viewFinger;
	private ImageThumbnailFileChooser fcOpen;
	private JFileChooser fcSaveTemplate;
	private ImageThumbnailFileChooser fcSave;
	private File oldTemplateFile;
	private File oldImageFile;

	private JPanel actionPanel;
	private JButton btnDefault;
	private JButton btnExtract;
	private JButton btnOpenImage;
	private JButton btnSaveImage;
	private JButton btnSaveTemplate;
	private JCheckBox cbShowBinarized;
	private JCheckBox cbDetectLiveness;
	private JLabel lblQuality;
	private JPanel leftPanel;
	private JPanel optionsPanel;
	private JPanel openImagePanel;
	private JPanel rightPanel;
	private JScrollPane scrollPaneFinger;
	private JScrollPane scrollPaneImage;
	private JPanel southPanel;
	private JPanel northPanel;
	private JSpinner spinnerThreshold;
	private JSplitPane splitPane;
	private JLabel tresholdLabel;

	// ===========================================================
	// Public constructor
	// ===========================================================

	public EnrollFromImage() {
		super();
		requiredLicenses = new ArrayList<String>();
		requiredLicenses.add("Biometrics.FingerExtraction");
		optionalLicenses = new ArrayList<String>();
		optionalLicenses.add("Images.WSQ");
	}

	// ===========================================================
	// Private methods
	// ===========================================================

	private void openImage() throws IOException {
		if (fcOpen.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
			NFinger finger = new NFinger();
			finger.setImage(NImage.fromFile(fcOpen.getSelectedFile().getAbsolutePath()));
			viewImage.setFinger(finger);
			viewFinger.setFinger(null);
			subject = null;
			lblQuality.setText("");
			updateControls();
			createTemplate();
		}
	}

	private void createTemplate() {
		subject = new NSubject();
		NFinger finger = new NFinger();
		finger.setImage(viewImage.getFinger().getImage());
		subject.getFingers().add(finger);
		updateFingersTools();
		FingersTools.getInstance().getClient().createTemplate(subject, null, templateCreationHandler);
	}

	private void saveTemplate() throws IOException {
		if (subject == null) {
			return;
		}
		if (oldTemplateFile != null) {
			fcSaveTemplate.setSelectedFile(oldTemplateFile);
		}
		if (fcSaveTemplate.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
			oldTemplateFile = fcSaveTemplate.getSelectedFile();
			String fileName = fcSaveTemplate.getSelectedFile().getAbsolutePath();
			NFile.writeAllBytes(fileName, subject.getTemplateBuffer());
		}
	}

	private void saveImage() throws IOException {
		if (subject == null) {
			return;
		}
		if (oldImageFile != null) {
			fcSave.setSelectedFile(oldImageFile);
		}
		if (fcSave.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
			oldImageFile = fcSave.getSelectedFile();
			String fileName = fcSave.getSelectedFile().getAbsolutePath();
			if (cbShowBinarized.isSelected()) {
				subject.getFingers().get(0).getBinarizedImage().save(fileName);
			} else {
				subject.getFingers().get(0).getImage().save(fileName);
			}
		}
	}

	private void updateTemplateCreationStatus(boolean created) {
		if (created) {
			viewFinger.setFinger(subject.getFingers().get(0));
			lblQuality.setText(String.format("Quality: %d", (subject.getFingers().get(0).getObjects().get(0).getQuality() & 0xFF)));
		} else {
			viewFinger.setFinger(null);
			lblQuality.setText("");
		}
		updateControls();
	}

	// ===========================================================
	// Protected methods
	// ===========================================================

	@Override
	protected void initGUI() {
		GridBagConstraints gridBagConstraints;
		setLayout(new BorderLayout());
		{
			northPanel = new JPanel();
			northPanel.setLayout(new BorderLayout());
			add(northPanel, java.awt.BorderLayout.NORTH);
			{
				panelLicensing = new LicensingPanel(requiredLicenses, optionalLicenses);
				northPanel.add(panelLicensing, java.awt.BorderLayout.NORTH);
			}
			{
				{
					openImagePanel = new JPanel();
					openImagePanel.setLayout(new GridBagLayout());
					northPanel.add(openImagePanel, java.awt.BorderLayout.WEST);
					{
						btnOpenImage = new JButton();
						btnOpenImage.setText("Open image");
						btnOpenImage.addActionListener(this);
						gridBagConstraints = new GridBagConstraints();
						gridBagConstraints.gridx = 0;
						gridBagConstraints.gridy = 0;
						gridBagConstraints.anchor = GridBagConstraints.LINE_START;
						gridBagConstraints.insets = new Insets(5, 5, 5, 5);
						openImagePanel.add(btnOpenImage, gridBagConstraints);
					}
				}
			}
			{
				optionsPanel = new JPanel();
				optionsPanel.setLayout(new GridBagLayout());
				northPanel.add(optionsPanel, BorderLayout.EAST);
				{
					cbDetectLiveness = new JCheckBox();
					cbDetectLiveness.setText("Detect Liveness");
					cbDetectLiveness.setSelected(FingersTools.getInstance().getClient().isFingersDetectLiveness());
					optionsPanel.add(cbDetectLiveness);
				}
				{
					spinnerThreshold = new JSpinner();
					spinnerThreshold.setModel(new SpinnerNumberModel(Byte.valueOf((byte) 0), Byte.valueOf((byte) 0), Byte.valueOf((byte) 100), Byte.valueOf((byte) 1)));
					spinnerThreshold.setPreferredSize(new Dimension(50, 20));
					gridBagConstraints = new GridBagConstraints();
					gridBagConstraints.gridx = 1;
					gridBagConstraints.gridy = 0;
					gridBagConstraints.insets = new Insets(5, 5, 5, 5);
					optionsPanel.add(spinnerThreshold, gridBagConstraints);
				}
				{
					btnDefault = new JButton();
					btnDefault.setText("Default");
					btnDefault.addActionListener(this);
					gridBagConstraints = new GridBagConstraints();
					gridBagConstraints.gridx = 2;
					gridBagConstraints.gridy = 0;
					gridBagConstraints.insets = new Insets(5, 5, 5, 5);
					optionsPanel.add(btnDefault, gridBagConstraints);
				}
				{
					btnExtract = new JButton();
					btnExtract.setText("Extract features");
					btnExtract.setEnabled(false);
					btnExtract.addActionListener(this);
					gridBagConstraints = new GridBagConstraints();
					gridBagConstraints.gridx = 3;
					gridBagConstraints.gridy = 0;
					gridBagConstraints.insets = new Insets(5, 5, 5, 5);
					optionsPanel.add(btnExtract, gridBagConstraints);
				}
			}
		}
		{
			splitPane = new JSplitPane();
			splitPane.setResizeWeight(0.5);
			add(splitPane, BorderLayout.CENTER);
			{
				leftPanel = new JPanel();
				leftPanel.setLayout(new BorderLayout());
				splitPane.setLeftComponent(leftPanel);
				{
					scrollPaneImage = new JScrollPane();
					scrollPaneImage.setMinimumSize(new Dimension(100, 100));
					scrollPaneImage.setPreferredSize(new Dimension(200, 200));
					leftPanel.add(scrollPaneImage, BorderLayout.CENTER);
					{
						viewImage = new NFingerView();
						viewImage.setAutofit(true);
						scrollPaneImage.setViewportView(viewImage);
					}
					{
						NViewZoomSlider imageZoomSlider = new NViewZoomSlider();
						imageZoomSlider.setView(viewImage);
						leftPanel.add(imageZoomSlider, BorderLayout.SOUTH);
					}
				}
			}
			{
				rightPanel = new JPanel();
				rightPanel.setLayout(new BorderLayout());
				splitPane.setRightComponent(rightPanel);
				{
					scrollPaneFinger = new JScrollPane();
					scrollPaneFinger.setMinimumSize(new Dimension(100, 100));
					scrollPaneFinger.setPreferredSize(new Dimension(200, 200));
					rightPanel.add(scrollPaneFinger, BorderLayout.CENTER);
					{
						viewFinger = new NFingerView();
						viewFinger.setShownImage(ShownImage.RESULT);
						viewFinger.setAutofit(true);
						viewFinger.addMouseListener(new MouseAdapter() {
							@Override
							public void mouseClicked(MouseEvent ev) {
								super.mouseClicked(ev);
								if (ev.getButton() == MouseEvent.BUTTON3) {
									cbShowBinarized.doClick();
								}
							}
						});
						scrollPaneFinger.setViewportView(viewFinger);
					}
					{
						NViewZoomSlider fingerZoomSlider = new NViewZoomSlider();
						fingerZoomSlider.setView(viewFinger);
						rightPanel.add(fingerZoomSlider, BorderLayout.SOUTH);
					}
				}
			}
		}
		{
			southPanel = new JPanel();
			southPanel.setLayout(new BorderLayout());
			add(southPanel, BorderLayout.SOUTH);
			{
				actionPanel = new JPanel();
				southPanel.add(actionPanel, BorderLayout.EAST);
				{
					lblQuality = new JLabel();
					southPanel.add(lblQuality, BorderLayout.WEST);
				}
				{
					cbShowBinarized = new JCheckBox();
					cbShowBinarized.setText("Show binarized image");
					cbShowBinarized.setSelected(true);
					cbShowBinarized.addActionListener(this);
					actionPanel.add(cbShowBinarized);
				}
				{
					btnSaveImage = new JButton();
					btnSaveImage.setText("Save image");
					btnSaveImage.setEnabled(false);
					btnSaveImage.addActionListener(this);
					actionPanel.add(btnSaveImage);
				}
				{
					btnSaveTemplate = new JButton();
					btnSaveTemplate.setText("Save template");
					btnSaveTemplate.setEnabled(false);
					btnSaveTemplate.addActionListener(this);
					actionPanel.add(btnSaveTemplate);
				}
			}
		}
		fcOpen = new ImageThumbnailFileChooser();
		fcOpen.setIcon(Utils.createIconImage("images/Logo16x16.png"));
		fcOpen.setFileFilter(new Utils.ImageFileFilter(NImages.getOpenFileFilter()));
		fcSaveTemplate = new JFileChooser();
		fcSave = new ImageThumbnailFileChooser();
		fcSave.setIcon(Utils.createIconImage("images/Logo16x16.png"));
		fcSave.setFileFilter(new Utils.ImageFileFilter(NImages.getSaveFileFilter()));
	}

	@Override
	protected void setDefaultValues() {
		spinnerThreshold.setValue(FingersTools.getInstance().getDefaultClient().getFingersQualityThreshold());
	}

	@Override
	protected void updateControls() {
		btnExtract.setEnabled((viewImage.getFinger() != null) && (viewImage.getFinger().getImage() != null));
		btnSaveImage.setEnabled((subject != null) && (subject.getStatus() == NBiometricStatus.OK));
		btnSaveTemplate.setEnabled((subject != null) && (subject.getStatus() == NBiometricStatus.OK));
		cbShowBinarized.setEnabled((subject != null) && (subject.getStatus() == NBiometricStatus.OK));
	}

	@Override
	protected void updateFingersTools() {
		FingersTools.getInstance().getClient().reset();
		FingersTools.getInstance().getClient().setFingersReturnBinarizedImage(true);
		FingersTools.getInstance().getClient().setFingersQualityThreshold((Byte) spinnerThreshold.getValue());
		FingersTools.getInstance().getClient().setFingersDetectLiveness(cbDetectLiveness.isSelected());
		FingersTools.getInstance().getClient().setFingersLivenessConfidenceThreshold((byte) 63);
	}

	// ===========================================================
	// Event handling
	// ===========================================================

	@Override
	public void actionPerformed(ActionEvent ev) {
		try {
			if (ev.getSource() == btnDefault) {
				spinnerThreshold.setValue(FingersTools.getInstance().getDefaultClient().getFingersQualityThreshold());
			} else if (ev.getSource() == btnOpenImage) {
				openImage();
			} else if (ev.getSource() == btnExtract) {
				createTemplate();
			} else if (ev.getSource() == btnSaveTemplate) {
				saveTemplate();
			} else if (ev.getSource() == btnSaveImage) {
				saveImage();
			} else if (ev.getSource() == cbShowBinarized) {
				if (cbShowBinarized.isSelected()) {
					viewFinger.setShownImage(ShownImage.RESULT);
				} else {
					viewFinger.setShownImage(ShownImage.ORIGINAL);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			SwingUtilities.invokeLater(() -> { JOptionPane.showMessageDialog(this, e, "Error", JOptionPane.ERROR_MESSAGE); });
			updateControls();
		}
	}

	// ===========================================================
	// Inner classes
	// ===========================================================

	private class TemplateCreationHandler implements CompletionHandler<NBiometricStatus, Object> {

		@Override
		public void completed(final NBiometricStatus result, final Object attachment) {
			SwingUtilities.invokeLater(new Runnable() {

				@Override
				public void run() {
					if (result == NBiometricStatus.OK) {
						updateTemplateCreationStatus(true);
					} else if (result == NBiometricStatus.BAD_OBJECT) {
						SwingUtilities.invokeLater(() -> { JOptionPane.showMessageDialog(EnrollFromImage.this, "Finger image quality is too low."); });
						updateTemplateCreationStatus(false);
					} else {
						SwingUtilities.invokeLater(() -> { JOptionPane.showMessageDialog(EnrollFromImage.this, result); });
						updateTemplateCreationStatus(false);
					}
				}
			});
		}

		@Override
		public void failed(final Throwable th, final Object attachment) {
			SwingUtilities.invokeLater(new Runnable() {

				@Override
				public void run() {
					showError(th);
					updateTemplateCreationStatus(false);
				}

			});
		}

	}

}
