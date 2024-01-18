package com.neurotec.samples;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.Box.Filler;
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
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;

import com.neurotec.biometrics.NBiometricStatus;
import com.neurotec.biometrics.NFinger;
import com.neurotec.biometrics.NSubject;
import com.neurotec.biometrics.NSubject.FingerCollection;
import com.neurotec.biometrics.swing.NFingerView;
import com.neurotec.biometrics.swing.NFingerViewBase.ShownImage;
import com.neurotec.samples.swing.ImageThumbnailFileChooser;
import com.neurotec.samples.util.Utils;
import com.neurotec.swing.NViewZoomSlider;
import com.neurotec.util.NIndexPair;
import com.neurotec.util.concurrent.CompletionHandler;

public final class VerifyFinger extends BasePanel implements ActionListener {

	// ===========================================================
	// Private static fields
	// ===========================================================

	private static final long serialVersionUID = 1L;

	private static final String SUBJECT_LEFT = "left";
	private static final String SUBJECT_RIGHT = "right";

	private static final String LEFT_LABEL_TEXT = "Image or template left: ";
	private static final String RIGHT_LABEL_TEXT = "Image or template right: ";

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
	private JPanel mainPanel;
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
	}

	// ===========================================================
	// Private methods
	// ===========================================================

	private void loadItem(String position) throws IOException {
		fileChooser.setMultiSelectionEnabled(false);
		if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
			resetMatedMinutiaeOnViews();
			verifyLabel.setText("");
			NSubject subjectTmp = null;
			NFinger finger = null;
			try {
				subjectTmp = NSubject.fromFile(fileChooser.getSelectedFile().getAbsolutePath());
				FingerCollection fingers = subjectTmp.getFingers();
				if (fingers.isEmpty()) {
					subjectTmp = null;
					throw new IllegalArgumentException("Template contains no finger records.");
				}
				finger = fingers.get(0);
				templateCreationHandler.completed(NBiometricStatus.OK, position);
			} catch (UnsupportedOperationException e) {
				// Ignore. UnsupportedOperationException means file is not a valid template.
			}

			// If file is not a template, try to load it as an image.
			if (subjectTmp == null) {
				finger = new NFinger();
				finger.setFileName(fileChooser.getSelectedFile().getAbsolutePath());
				subjectTmp = new NSubject();
				subjectTmp.getFingers().add(finger);
				updateFingersTools();
				FingersTools.getInstance().getClient().createTemplate(subjectTmp, position, templateCreationHandler);
			}

			if (SUBJECT_LEFT.equals(position)) {
				subjectLeft = subjectTmp;
				leftLabel.setText(fileChooser.getSelectedFile().getAbsolutePath());
				viewLeft.setFinger(finger);
			} else if (SUBJECT_RIGHT.equals(position)) {
				subjectRight = subjectTmp;
				rightLabel.setText(fileChooser.getSelectedFile().getAbsolutePath());
				viewRight.setFinger(finger);
			} else {
				throw new AssertionError("Unknown subject position: " + position);
			}
		}
	}

	private void verify() {
		updateFingersTools();
		FingersTools.getInstance().getClient().verify(subjectLeft, subjectRight, null, verificationHandler);
	}

	private void clear() {
		viewLeft.setFinger(null);
		viewRight.setFinger(null);
		subjectLeft.clear();
		subjectRight.clear();
		updateControls();
		verifyLabel.setText(" ");
		leftLabel.setText(LEFT_LABEL_TEXT);
		rightLabel.setText(RIGHT_LABEL_TEXT);
	}

	private void resetMatedMinutiaeOnViews() {
		viewLeft.setMatedMinutiae(new NIndexPair[0]);
		viewRight.setMatedMinutiae(new NIndexPair[0]);
		viewLeft.setTree(new NIndexPair[0]);
		viewRight.setTree(new NIndexPair[0]);
	}

	// ===========================================================
	// Protected methods
	// ===========================================================

	@Override
	protected void initGUI() {
		GridBagConstraints gridBagConstraints;
		setLayout(new BorderLayout());

		farComboBox = new JComboBox<>();
		fileChooser = new ImageThumbnailFileChooser();
		fileChooser.setIcon(Utils.createIconImage("images/Logo16x16.png"));

		panelLicensing = new LicensingPanel(requiredLicenses, optionalLicenses);
		add(panelLicensing, java.awt.BorderLayout.NORTH);

		mainPanel = new JPanel();
		mainPanel.setLayout(new BorderLayout());
		add(mainPanel, BorderLayout.CENTER);
		{
			northPanel = new JPanel();
			mainPanel.add(northPanel, BorderLayout.NORTH);
			{
				leftOpenButton = new JButton();
				leftOpenButton.setText("Open");
				leftOpenButton.addActionListener(this);
				northPanel.add(leftOpenButton);
			}
			{
				farPanel = new JPanel();
				farPanel.setBorder(BorderFactory.createTitledBorder(null, "Matching FAR", TitledBorder.CENTER, TitledBorder.DEFAULT_POSITION));
				farPanel.setLayout(new GridBagLayout());
				northPanel.add(farPanel);
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
					defaultButton.setText("Default");
					defaultButton.addActionListener(this);
					gridBagConstraints = new GridBagConstraints();
					gridBagConstraints.gridx = 1;
					gridBagConstraints.gridy = 0;
					gridBagConstraints.insets = new Insets(3, 3, 3, 3);
					farPanel.add(defaultButton, gridBagConstraints);
				}
			}
			{
				rightOpenButton = new JButton();
				rightOpenButton.setText("Open");
				rightOpenButton.addActionListener(this);
				northPanel.add(rightOpenButton);
			}
		}
		{
			centerPanel = new JPanel();
			centerPanel.setLayout(new GridLayout(1, 2, 5, 0));
			mainPanel.add(centerPanel, BorderLayout.CENTER);
			{
				leftScrollPane = new JScrollPane();
				leftScrollPane.setPreferredSize(new Dimension(200, 200));
				centerPanel.add(leftScrollPane);
				{
					viewLeft = new NFingerView();
					viewLeft.setAutofit(true);
					viewLeft.addMouseListener(new MouseAdapter() {
						@Override
						public void mouseClicked(MouseEvent ev) {
							super.mouseClicked(ev);
							if (ev.getButton() == MouseEvent.BUTTON3) {
								cbLeftShowBinarized.doClick();
							}
						}
					});
					leftScrollPane.setViewportView(viewLeft);
				}
			}
			{
				rightScrollPane = new JScrollPane();
				rightScrollPane.setPreferredSize(new Dimension(200, 200));
				centerPanel.add(rightScrollPane);
				{
					viewRight = new NFingerView();
					viewRight.setAutofit(true);
					viewRight.addMouseListener(new MouseAdapter() {
						@Override
						public void mouseClicked(MouseEvent ev) {
							super.mouseClicked(ev);
							if (ev.getButton() == MouseEvent.BUTTON3) {
								cbLeftShowBinarized.doClick();
							}
						}
					});
					rightScrollPane.setViewportView(viewRight);
				}
			}
		}
		{
			southPanel = new JPanel();
			southPanel.setLayout(new BorderLayout());
			mainPanel.add(southPanel, BorderLayout.SOUTH);
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
							cbLeftShowBinarized.setText("Show binarized image");
							cbLeftShowBinarized.addActionListener(this);
							leftBinarizedPanel.add(cbLeftShowBinarized, BorderLayout.CENTER);
						}
					}
					{
						rightBinarizedPanel = new JPanel();
						rightBinarizedPanel.setLayout(new BorderLayout());
						showBinarizedPanel.add(rightBinarizedPanel, BorderLayout.EAST);
						{
							viewRightZoomSlider = new NViewZoomSlider();
							viewRightZoomSlider.setView(viewRight);
							rightBinarizedPanel.add(viewRightZoomSlider, BorderLayout.EAST);
						}
						{
							cbRightShowBinarized = new JCheckBox();
							cbRightShowBinarized.setText("Show binarized image");
							cbRightShowBinarized.addActionListener(this);
							rightBinarizedPanel.add(cbRightShowBinarized, BorderLayout.CENTER);
						}
					}
				}
				{
					clearButtonPanel = new JPanel();
					imageControlsPanel.add(clearButtonPanel, BorderLayout.CENTER);
					{
						clearButton = new JButton();
						clearButton.setText("Clear images");
						clearButton.addActionListener(this);
						clearButtonPanel.add(clearButton);
					}
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
					verifyButton.setText("Verify");
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
		cbRightShowBinarized.doClick();
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
			SwingUtilities.invokeLater(() -> { JOptionPane.showMessageDialog(this, "FAR is not valid. Using default value.", "Error", JOptionPane.ERROR_MESSAGE); });
		}
	}

	// ===========================================================
	// Package private methods
	// ===========================================================

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
				verify();
			} else if (ev.getSource() == leftOpenButton) {
				loadItem(SUBJECT_LEFT);
			} else if (ev.getSource() == rightOpenButton) {
				loadItem(SUBJECT_RIGHT);
			} else if (ev.getSource() == clearButton) {
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
			SwingUtilities.invokeLater(() -> { JOptionPane.showMessageDialog(this, e, "Error", JOptionPane.ERROR_MESSAGE); });
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
						SwingUtilities.invokeLater(() -> { JOptionPane.showMessageDialog(VerifyFinger.this, "Template was not created: " + status, "Error", JOptionPane.WARNING_MESSAGE); });
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
			SwingUtilities.invokeLater(new Runnable() {

				@Override
				public void run() {
					if (status == NBiometricStatus.OK) {
						int score = getLeft().getMatchingResults().get(0).getScore();
						String msg = "Score of matched templates: " + score;
						updateLabel(msg);
						SwingUtilities.invokeLater(() -> { JOptionPane.showMessageDialog(VerifyFinger.this, msg, "Match", JOptionPane.PLAIN_MESSAGE); });

						NIndexPair[] matedMinutiae = getLeft().getMatchingResults().get(0).getMatchingDetails().getFingers().get(0).getMatedMinutiae();

						viewLeft.setMatedMinutiaIndex(0);
						viewLeft.setMatedMinutiae(matedMinutiae);

						viewRight.setMatedMinutiaIndex(1);
						viewRight.setMatedMinutiae(matedMinutiae);

						viewLeft.prepareTree();
						viewRight.setTree(viewLeft.getTree());
					} else {
						SwingUtilities.invokeLater(() -> { JOptionPane.showMessageDialog(VerifyFinger.this, "Templates didn't match.", "No match", JOptionPane.WARNING_MESSAGE); });
					}
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

}
