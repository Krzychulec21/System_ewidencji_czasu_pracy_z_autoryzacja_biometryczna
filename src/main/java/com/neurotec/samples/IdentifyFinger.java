package com.neurotec.samples;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import com.neurotec.biometrics.NBiometricOperation;
import com.neurotec.biometrics.NBiometricStatus;
import com.neurotec.biometrics.NBiometricTask;
import com.neurotec.biometrics.NFinger;
import com.neurotec.biometrics.NMatchingResult;
import com.neurotec.biometrics.NSubject;
import com.neurotec.biometrics.NSubject.FingerCollection;
import com.neurotec.biometrics.swing.NFingerView;
import com.neurotec.biometrics.swing.NFingerViewBase.ShownImage;
import com.neurotec.samples.swing.ImageThumbnailFileChooser;
import com.neurotec.samples.util.Utils;
import com.neurotec.swing.NViewZoomSlider;
import com.neurotec.util.concurrent.CompletionHandler;

public final class IdentifyFinger extends BasePanel implements ActionListener {

	// ===========================================================
	// Private static fields
	// ===========================================================

	private static final long serialVersionUID = 1L;

	// ===========================================================
	// Private fields
	// ===========================================================

	private NSubject subject;
	private final List<NSubject> subjects;
	private NFingerView view;

	private final EnrollHandler enrollHandler = new EnrollHandler();
	private final IdentificationHandler identificationHandler = new IdentificationHandler();
	private final TemplateCreationHandler templateCreationHandler = new TemplateCreationHandler();

	private ImageThumbnailFileChooser fcGallery;
	private ImageThumbnailFileChooser fcProbe;

	private JButton btnFarDefault;
	private JButton btnIdentify;
	private JButton btnOpenProbe;
	private JButton btnOpenTemplates;
	private JButton btnQualityDefault;
	private JComboBox<String> cbFar;
	private JCheckBox cbShowBinarized;
	private JPanel centerPanel;
	private JPanel identifyControlsPanel;
	private JPanel farPanel;
	private JLabel fileLabel;
	private JPanel identificationNorthPanel;
	private JPanel identificationPanel;
	private JPanel identifyButtonPanel;
	private JPanel imagePanel;
	private JLabel lblCount;
	private JPanel mainPanel;
	private JPanel northPanel;
	private JPanel openImagePanel;
	private JPanel qualityPanel;
	private JPanel resultsPanel;
	private JScrollPane scrollPane;
	private JScrollPane scrollPaneResults;
	private JPanel settingsPanel;
	private JSpinner spinnerQuality;
	private JTable tableResults;
	private JLabel templatesLabel;

	// ===========================================================
	// Public constructor
	// ===========================================================

	public IdentifyFinger() {
		super();
		subjects = new ArrayList<NSubject>();
		requiredLicenses = new ArrayList<String>();
		requiredLicenses.add("Biometrics.FingerExtraction");
		requiredLicenses.add("Biometrics.FingerMatching");
		optionalLicenses = new ArrayList<String>();
		optionalLicenses.add("Images.WSQ");
	}

	// ===========================================================
	// Private methods
	// ===========================================================

	private void openTemplates() throws IOException {
		if (fcGallery.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
			((DefaultTableModel) tableResults.getModel()).setRowCount(0);
			subjects.clear();

			// Create subjects from selected templates.
			for (File file : fcGallery.getSelectedFiles()) {
				NSubject s = NSubject.fromFile(file.getAbsolutePath());
				s.setId(file.getName());
				subjects.add(s);
			}
			lblCount.setText(String.valueOf(subjects.size()));
		}
		updateControls();
	}

	private void openProbe() throws IOException {
		if (fcProbe.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
			((DefaultTableModel) tableResults.getModel()).setRowCount(0);
			fileLabel.setText("");
			subject = null;
			NFinger finger = null;
			try {
				subject = NSubject.fromFile(fcProbe.getSelectedFile().getAbsolutePath());
				subject.setId(fcProbe.getSelectedFile().getName());
				FingerCollection fingers = subject.getFingers();
				if (fingers.isEmpty()) {
					subject = null;
					throw new IllegalArgumentException("Template contains no finger records.");
				}
				finger = fingers.get(0);
				templateCreationHandler.completed(NBiometricStatus.OK, null);
			} catch (UnsupportedOperationException e) {
				// Ignore. UnsupportedOperationException means file is not a valid template.
			}

			// If file is not a template, try to load it as an image.
			if (subject == null) {
				finger = new NFinger();
				finger.setFileName(fcProbe.getSelectedFile().getAbsolutePath());
				subject = new NSubject();
				subject.setId(fcProbe.getSelectedFile().getName());
				subject.getFingers().add(finger);
				updateFingersTools();
				FingersTools.getInstance().getClient().createTemplate(subject, null, templateCreationHandler);
			}

			view.setFinger(finger);
			fileLabel.setText(fcProbe.getSelectedFile().getAbsolutePath());
		}
	}

	private void identify() {
		if ((subject != null) && !subjects.isEmpty()) {
			((DefaultTableModel) tableResults.getModel()).setRowCount(0);
			updateFingersTools();

			// Clean earlier data before proceeding, enroll new data
			FingersTools.getInstance().getClient().clear();

			// Create enrollment task.
			NBiometricTask enrollmentTask = new NBiometricTask(EnumSet.of(NBiometricOperation.ENROLL));

			// Add subjects to be enrolled.
			for (NSubject s : subjects) {
				enrollmentTask.getSubjects().add(s);
			}

			// Enroll subjects.
			FingersTools.getInstance().getClient().performTask(enrollmentTask, null, enrollHandler);
		}
	}

	// ===========================================================
	// Protected methods
	// ===========================================================

	@Override
	protected void initGUI() {
		setLayout(new BorderLayout());
		GridBagConstraints gridBagConstraints;

		panelLicensing = new LicensingPanel(requiredLicenses, optionalLicenses);
		add(panelLicensing, java.awt.BorderLayout.NORTH);

		mainPanel = new JPanel();
		mainPanel.setLayout(new BorderLayout());
		add(mainPanel, BorderLayout.CENTER);
		{
			centerPanel = new JPanel();
			centerPanel.setLayout(new BorderLayout());
			mainPanel.add(centerPanel, BorderLayout.CENTER);
			{
				northPanel = new JPanel();
				northPanel.setBorder(BorderFactory.createTitledBorder("Templates loading"));
				northPanel.setLayout(new FlowLayout(FlowLayout.LEADING));
				centerPanel.add(northPanel, BorderLayout.NORTH);
				{
					btnOpenTemplates = new JButton();
					btnOpenTemplates.setText("Load");
					btnOpenTemplates.addActionListener(this);
					northPanel.add(btnOpenTemplates);
				}
				{
					templatesLabel = new JLabel();
					templatesLabel.setText("Templates loaded: ");
					northPanel.add(templatesLabel);
				}
				{
					lblCount = new JLabel();
					lblCount.setText("0");
					northPanel.add(lblCount);
				}
			}
			{
				imagePanel = new JPanel();
				imagePanel.setBorder(BorderFactory.createTitledBorder("Image / template for identification"));
				imagePanel.setLayout(new BorderLayout());
				centerPanel.add(imagePanel, BorderLayout.CENTER);
				{
					scrollPane = new JScrollPane();
					imagePanel.add(scrollPane, BorderLayout.CENTER);
					{
						view = new NFingerView();
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
					openImagePanel = new JPanel();
					openImagePanel.setLayout(new FlowLayout(FlowLayout.LEADING));
					imagePanel.add(openImagePanel, BorderLayout.PAGE_START);
					{
						btnOpenProbe = new JButton();
						btnOpenProbe.setText("Open");
						btnOpenProbe.addActionListener(this);
						openImagePanel.add(btnOpenProbe);
					}
					{
						fileLabel = new JLabel();
						openImagePanel.add(fileLabel);
					}
				}
			}
		}
		{
			identificationPanel = new JPanel();
			identificationPanel.setBorder(BorderFactory.createTitledBorder(""));
			identificationPanel.setLayout(new BorderLayout());
			mainPanel.add(identificationPanel, BorderLayout.SOUTH);
			{
				identificationNorthPanel = new JPanel();
				identificationNorthPanel.setLayout(new BorderLayout());
				identificationPanel.add(identificationNorthPanel, BorderLayout.NORTH);
				{
					identifyButtonPanel = new JPanel();
					identifyButtonPanel.setLayout(new BoxLayout(identifyButtonPanel, BoxLayout.Y_AXIS));
					identificationNorthPanel.add(identifyButtonPanel, BorderLayout.WEST);
					{
						identifyControlsPanel = new JPanel();
						identifyControlsPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
						identifyButtonPanel.add(identifyControlsPanel);
						{
							btnIdentify = new JButton();
							btnIdentify.setText("Identify");
							btnIdentify.addActionListener(this);
							identifyControlsPanel.add(btnIdentify);
						}
						{
							cbShowBinarized = new JCheckBox();
							cbShowBinarized.setText("Show binarized image");
							cbShowBinarized.addActionListener(this);
							identifyControlsPanel.add(cbShowBinarized);
						}
					}
					{
						NViewZoomSlider imageZoomSlider = new NViewZoomSlider();
						imageZoomSlider.setView(view);
						identifyButtonPanel.add(imageZoomSlider);
					}
				}
				{
					settingsPanel = new JPanel();
					settingsPanel.setLayout(new FlowLayout(FlowLayout.TRAILING));
					identificationNorthPanel.add(settingsPanel, BorderLayout.EAST);
					{
						qualityPanel = new JPanel();
						qualityPanel.setBorder(BorderFactory.createTitledBorder("Quality threshold"));
						qualityPanel.setLayout(new GridBagLayout());
						settingsPanel.add(qualityPanel);
						{
							spinnerQuality = new JSpinner(new SpinnerNumberModel(39, 0, 100, 1));
							spinnerQuality.setModel(new SpinnerNumberModel(Byte.valueOf((byte) 0), Byte.valueOf((byte) 0), Byte.valueOf((byte) 100), Byte.valueOf((byte) 1)));
							gridBagConstraints = new GridBagConstraints();
							gridBagConstraints.gridx = 0;
							gridBagConstraints.gridy = 1;
							gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
							gridBagConstraints.insets = new Insets(5, 5, 5, 5);
							qualityPanel.add(spinnerQuality, gridBagConstraints);
						}
						{
							btnQualityDefault = new JButton();
							btnQualityDefault.setText("Default");
							btnQualityDefault.addActionListener(this);
							gridBagConstraints = new GridBagConstraints();
							gridBagConstraints.gridx = 1;
							gridBagConstraints.gridy = 1;
							gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
							gridBagConstraints.insets = new Insets(5, 5, 5, 5);
							qualityPanel.add(btnQualityDefault, gridBagConstraints);
						}
					}
					{
						farPanel = new JPanel();
						farPanel.setBorder(BorderFactory.createTitledBorder("Matching FAR"));
						farPanel.setLayout(new GridBagLayout());
						settingsPanel.add(farPanel);
						{
							cbFar = new JComboBox<>();
							char c = new DecimalFormatSymbols().getPercent();
							DefaultComboBoxModel<String> model = (DefaultComboBoxModel<String>) cbFar.getModel();
							NumberFormat nf = NumberFormat.getNumberInstance();
							nf.setMaximumFractionDigits(5);
							model.addElement(nf.format(0.1) + c);
							model.addElement(nf.format(0.01) + c);
							model.addElement(nf.format(0.001) + c);
							cbFar.setSelectedIndex(1);
							cbFar.setEditable(true);
							cbFar.setModel(model);
							gridBagConstraints = new GridBagConstraints();
							gridBagConstraints.gridx = 0;
							gridBagConstraints.gridy = 0;
							gridBagConstraints.anchor = GridBagConstraints.LINE_START;
							gridBagConstraints.insets = new Insets(5, 5, 5, 5);
							farPanel.add(cbFar, gridBagConstraints);
						}
						{
							btnFarDefault = new JButton();
							btnFarDefault.setText("Default");
							btnFarDefault.addActionListener(this);
							gridBagConstraints = new GridBagConstraints();
							gridBagConstraints.gridx = 1;
							gridBagConstraints.gridy = 0;
							gridBagConstraints.insets = new Insets(5, 5, 5, 5);
							farPanel.add(btnFarDefault, gridBagConstraints);
						}
					}
				}
			}
		}
		{
			resultsPanel = new JPanel();
			resultsPanel.setBorder(BorderFactory.createTitledBorder("Results"));
			resultsPanel.setLayout(new GridLayout(1, 1));
			mainPanel.add(resultsPanel, BorderLayout.EAST);
			{
				scrollPaneResults = new JScrollPane();
				scrollPaneResults.setPreferredSize(new Dimension(200, 0));
				resultsPanel.add(scrollPaneResults);
				{
					tableResults = new JTable();
					tableResults.setModel(new DefaultTableModel(
							new Object[][] {},
							new String[] {"ID", "Score"}) {

						private final Class<?>[] types = new Class<?>[] {String.class, Integer.class};
						private final boolean[] canEdit = new boolean[] {false, false};

						@Override
						public Class<?> getColumnClass(int columnIndex) {
							return types[columnIndex];
						}

						@Override
						public boolean isCellEditable(int rowIndex, int columnIndex) {
							return canEdit[columnIndex];
						}

					});
					DefaultTableCellRenderer leftRenderer = new DefaultTableCellRenderer();
					leftRenderer.setHorizontalAlignment(SwingConstants.LEFT);
					tableResults.getColumnModel().getColumn(1).setCellRenderer(leftRenderer);
					scrollPaneResults.setViewportView(tableResults);
				}
			}
		}

		fcProbe = new ImageThumbnailFileChooser();
		fcProbe.setIcon(Utils.createIconImage("images/Logo16x16.png"));
		fcGallery = new ImageThumbnailFileChooser();
		fcGallery.setIcon(Utils.createIconImage("images/Logo16x16.png"));
		fcGallery.setMultiSelectionEnabled(true);

		cbShowBinarized.doClick();
	}

	@Override
	protected void setDefaultValues() {
		spinnerQuality.setValue(FingersTools.getInstance().getDefaultClient().getFingersQualityThreshold());
		cbFar.setSelectedItem(Utils.matchingThresholdToString(FingersTools.getInstance().getDefaultClient().getMatchingThreshold()));
	}

	@Override
	protected void updateControls() {
		btnIdentify.setEnabled(!subjects.isEmpty() && (subject != null) && ((subject.getStatus() == NBiometricStatus.OK) || (subject.getStatus() == NBiometricStatus.NONE)));
	}

	@Override
	public void updateFingersTools() {
		FingersTools.getInstance().getClient().reset();
		FingersTools.getInstance().getClient().setFingersReturnBinarizedImage(true);
		FingersTools.getInstance().getClient().setFingersQualityThreshold((Byte) spinnerQuality.getValue());
		try {
			FingersTools.getInstance().getClient().setMatchingThreshold(Utils.matchingThresholdFromString(cbFar.getSelectedItem().toString()));
		} catch (ParseException e) {
			e.printStackTrace();
			FingersTools.getInstance().getClient().setMatchingThreshold(FingersTools.getInstance().getDefaultClient().getMatchingThreshold());
			cbFar.setSelectedItem(Utils.matchingThresholdToString(FingersTools.getInstance().getDefaultClient().getMatchingThreshold()));
			SwingUtilities.invokeLater(() -> { JOptionPane.showMessageDialog(this, "FAR is not valid. Using default value.", "Error", JOptionPane.ERROR_MESSAGE); });
		}
	}

	// ===========================================================
	// Package private methods
	// ===========================================================

	NSubject getSubject() {
		return subject;
	}

	void setSubject(NSubject subject) {
		this.subject = subject;
	}

	List<NSubject> getSubjects() {
		return subjects;
	}

	void appendIdentifyResult(String name, int score) {
		((DefaultTableModel) tableResults.getModel()).addRow(new Object[] {name, score});
	}

	void prependIdentifyResult(String name, int score) {
		((DefaultTableModel) tableResults.getModel()).insertRow(0, new Object[] {name, score});
	}

	// ===========================================================
	// Event handling
	// ===========================================================

	@Override
	public void actionPerformed(ActionEvent ev) {
		try {
			if (ev.getSource() == btnOpenTemplates) {
				openTemplates();
			} else if (ev.getSource() == btnOpenProbe) {
				openProbe();
			} else if (ev.getSource() == btnQualityDefault) {
				spinnerQuality.setValue(FingersTools.getInstance().getDefaultClient().getFingersQualityThreshold());
			} else if (ev.getSource() == btnFarDefault) {
				cbFar.setSelectedItem(Utils.matchingThresholdToString(FingersTools.getInstance().getDefaultClient().getMatchingThreshold()));
			} else if (ev.getSource() == btnIdentify) {
				identify();
			} else if (ev.getSource() == cbShowBinarized) {
				if (cbShowBinarized.isSelected()) {
					view.setShownImage(ShownImage.RESULT);
				} else {
					view.setShownImage(ShownImage.ORIGINAL);
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
		public void completed(final NBiometricStatus status, final Object attachment) {
			SwingUtilities.invokeLater(new Runnable() {

				@Override
				public void run() {
					updateControls();
					if (status != NBiometricStatus.OK) {
						setSubject(null);
						SwingUtilities.invokeLater(() -> { JOptionPane.showMessageDialog(IdentifyFinger.this, "Template was not created: " + status, "Error", JOptionPane.WARNING_MESSAGE); });
					}
				}

			});
		}

		@Override
		public void failed(final Throwable th, final Object attachment) {
			SwingUtilities.invokeLater(new Runnable() {

				@Override
				public void run() {
					updateControls();
					showError(th);
				}

			});
		}

	}

	private class EnrollHandler implements CompletionHandler<NBiometricTask, Object> {

		@Override
		public void completed(final NBiometricTask task, final Object attachment) {
			if (task.getStatus() == NBiometricStatus.OK) {

				// Identify current subject in enrolled ones.
				FingersTools.getInstance().getClient().identify(getSubject(), null, identificationHandler);
			} else {
				SwingUtilities.invokeLater(() -> { JOptionPane.showMessageDialog(IdentifyFinger.this, "Enrollment failed: " + task.getStatus(), "Error", JOptionPane.WARNING_MESSAGE); });
			}
		}

		@Override
		public void failed(final Throwable th, final Object attachment) {
			SwingUtilities.invokeLater(new Runnable() {

				@Override
				public void run() {
					updateControls();
					showError(th);
				}

			});
		}

	}

	private class IdentificationHandler implements CompletionHandler<NBiometricStatus, Object> {

		@Override
		public void completed(final NBiometricStatus status, final Object attachment) {
			SwingUtilities.invokeLater(new Runnable() {

				@Override
				public void run() {
					if ((status == NBiometricStatus.OK) || (status == NBiometricStatus.MATCH_NOT_FOUND)) {

						// Match subjects.
						for (NSubject s : getSubjects()) {
							boolean match = false;
							for (NMatchingResult result : getSubject().getMatchingResults()) {
								if (s.getId().equals(result.getId())) {
									match = true;
									prependIdentifyResult(result.getId(), result.getScore());
									break;
								}
							}
							if (!match) {
								appendIdentifyResult(s.getId(), 0);
							}
						}
					} else {
						SwingUtilities.invokeLater(() -> { JOptionPane.showMessageDialog(IdentifyFinger.this, "Identification failed: " + status, "Error", JOptionPane.WARNING_MESSAGE); });
					}
				}

			});
		}

		@Override
		public void failed(final Throwable th, final Object attachment) {
			SwingUtilities.invokeLater(new Runnable() {

				@Override
				public void run() {
					updateControls();
					showError(th);
				}

			});
		}

	}

}
