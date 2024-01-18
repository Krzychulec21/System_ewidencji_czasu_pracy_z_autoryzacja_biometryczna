package com.neurotec.samples;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.OverlayLayout;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.neurotec.biometrics.NBiometricOperation;
import com.neurotec.biometrics.NBiometricStatus;
import com.neurotec.biometrics.NBiometricTask;
import com.neurotec.biometrics.NBiometricTypes;
import com.neurotec.biometrics.NFAttributes;
import com.neurotec.biometrics.NFPosition;
import com.neurotec.biometrics.NFinger;
import com.neurotec.biometrics.NSubject;
import com.neurotec.biometrics.client.NBiometricClient;
import com.neurotec.biometrics.swing.NFingerView;
import com.neurotec.images.NImages;
import com.neurotec.samples.swing.ImageThumbnailFileChooser;
import com.neurotec.samples.util.Utils;
import com.neurotec.swing.NViewZoomSlider;
import com.neurotec.util.NVersion;
import com.neurotec.util.concurrent.CompletionHandler;

public final class SegmentFingers extends BasePanel implements ActionListener, ListSelectionListener {

	// ===========================================================
	// Private static fields
	// ===========================================================

	private static final long serialVersionUID = 1L;

	private static final String POSITION_LABEL_TEXT = "Position: %s";
	private static final String QUALITY_LABEL_TEXT = "Quality: %s";
	private static final String CLASS_LABEL_TEXT = "Class: %s";

	// ===========================================================
	// Private fields
	// ===========================================================

	private List<JCheckBox> checkBoxList;
	private List<NFPosition> allPossibleMissingPositions;
	private NSubject subject;

	private final SegmentHandler segmentHandler = new SegmentHandler();

	private NFingerView viewMain;
	private NFingerView viewSegment1;
	private NFingerView viewSegment2;
	private NFingerView viewSegment3;
	private NFingerView viewSegment4;
	private NViewZoomSlider zoomSlider;

	private JButton btnOpen;
	private JButton btnSave;
	private JButton btnSegment;
	private JLabel lblMissingPositions;
	private JLabel lblPosition;
	private JLabel lblSegment3Position;
	private JLabel lblSegment3Class;
	private JLabel lblSegment3Quality;
	private JLabel lblSegment1Class;
	private JLabel lblSegment1Position;
	private JLabel lblSegment1Quality;
	private JLabel lblSegment2Class;
	private JLabel lblSegment2Position;
	private JLabel lblSegment2Quality;
	private JLabel lblSegment4Class;
	private JLabel lblSegment4Position;
	private JLabel lblSegment4Quality;
	private JLabel lblStatus;
	private JList<NFPosition> listPositions;
	private JPanel panelBottom;
	private JPanel panelCenter;
	private JPanel panelMain;
	private JPanel panelMissingPositions;
	private JPanel panelSegment1;
	private JPanel panelSegment1Labels;
	private JPanel panelSegment2;
	private JPanel panelSegment2Labels;
	private JPanel panelSegment3;
	private JPanel panelSegment3Labels;
	private JPanel panelSegment4;
	private JPanel panelSegment4Labels;
	private JPanel panelSegments;
	private JPanel panelStatus;
	private JPanel panelTop;
	private JPanel panelUpperButtons;
	private JPanel zoomToFitFitPanel;
	private JScrollPane spImage;
	private JScrollPane spMissingPositions;
	private JScrollPane spPositions;
	private JScrollPane spSegment1;
	private JScrollPane spSegment2;
	private JScrollPane spSegment3;
	private JScrollPane spSegment4;

	private ImageThumbnailFileChooser fcOpen;
	private ImageThumbnailFileChooser fcSave;

	// ===========================================================
	// Public constructor
	// ===========================================================

	public SegmentFingers() {
		super();
		requiredLicenses = new ArrayList<String>();
		requiredLicenses.add("Biometrics.FingerSegmentation");
		requiredLicenses.add("Biometrics.FingerQualityAssessmentBase");
		optionalLicenses = new ArrayList<String>();
		optionalLicenses.add("Images.WSQ");
	}

	// ===========================================================
	// Private methods
	// ===========================================================

	private void initFingerPositions() {
		DefaultListModel<NFPosition> model = new DefaultListModel<>();
		model.addElement(NFPosition.PLAIN_RIGHT_FOUR_FINGERS);
		model.addElement(NFPosition.PLAIN_LEFT_FOUR_FINGERS);
		model.addElement(NFPosition.PLAIN_THUMBS);
		model.addElement(NFPosition.LEFT_LITTLE_FINGER);
		model.addElement(NFPosition.LEFT_RING_FINGER);
		model.addElement(NFPosition.LEFT_MIDDLE_FINGER);
		model.addElement(NFPosition.LEFT_INDEX_FINGER);
		model.addElement(NFPosition.LEFT_THUMB);
		model.addElement(NFPosition.RIGHT_THUMB);
		model.addElement(NFPosition.RIGHT_INDEX_FINGER);
		model.addElement(NFPosition.RIGHT_MIDDLE_FINGER);
		model.addElement(NFPosition.RIGHT_RING_FINGER);
		model.addElement(NFPosition.RIGHT_LITTLE_FINGER);
		listPositions.setModel(model);

		listPositions.setSelectedIndex(0);
	}

	private void openImage() {
		if (fcOpen.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
			subject = new NSubject();
			NFinger finger = new NFinger();
			subject.getFingers().add(finger);
			finger.setFileName(fcOpen.getSelectedFile().getAbsolutePath());
			viewMain.setFinger(finger);
			lblStatus.setText("");
			updateControls();
			segment();
		}
	}

	private void segment() {
		clearSegmentInfo();
		subject.getFingers().get(0).setPosition((NFPosition) listPositions.getSelectedValue());
		subject.getMissingFingers().clear();
		for (int i = 0; i < checkBoxList.size(); i++) {
			if (checkBoxList.get(i).isSelected()) {
				subject.getMissingFingers().add(allPossibleMissingPositions.get(i));
			}
		}
		NBiometricClient client = FingersTools.getInstance().getClient();
		client.setFingersDeterminePatternClass(true);
		client.setFingersCalculateNFIQ(true);
		NBiometricTask task = client.createTask(EnumSet.of(NBiometricOperation.SEGMENT, NBiometricOperation.CREATE_TEMPLATE, NBiometricOperation.ASSESS_QUALITY), subject);
		client.performTask(task, null, segmentHandler);
	}

	private void showSegments() {
		int segmentsCount = subject.getFingers().size() - 1;
		if (segmentsCount > 0 && subject.getFingers().get(1).getStatus() == NBiometricStatus.OK) {
			setSegmentInfo(subject.getFingers().get(1), lblSegment1Position, lblSegment1Quality, lblSegment1Class, viewSegment1);
		}
		if (segmentsCount > 1 && subject.getFingers().get(2).getStatus() == NBiometricStatus.OK) {
			setSegmentInfo(subject.getFingers().get(2), lblSegment2Position, lblSegment2Quality, lblSegment2Class, viewSegment2);
		}
		if (segmentsCount > 2 && subject.getFingers().get(3).getStatus() == NBiometricStatus.OK) {
			setSegmentInfo(subject.getFingers().get(3), lblSegment3Position, lblSegment3Quality, lblSegment3Class, viewSegment3);
		}
		if (segmentsCount > 3 && subject.getFingers().get(4).getStatus() == NBiometricStatus.OK) {
			setSegmentInfo(subject.getFingers().get(4), lblSegment4Position, lblSegment4Quality, lblSegment4Class, viewSegment4);
		}
	}

	private void setSegmentInfo(NFinger finger, JLabel lblPosition, JLabel lblQuality, JLabel lblClass, NFingerView view) {
		String position = finger.getPosition().toString();
		String quality;
		String patternClass;
		if (finger.getObjects().isEmpty()) {
			quality = "";
			patternClass = "";
		} else {
			NFAttributes attributes = finger.getObjects().get(0);
			short nfiq10 = attributes.getNFIQ(new NVersion(1, 0));
			quality = String.valueOf(NBiometricTypes.NFIQ1ToNFIQQuality(nfiq10));
			patternClass = attributes.getPatternClass().toString();
		}
		lblPosition.setText(String.format(POSITION_LABEL_TEXT, position));
		lblQuality.setText(String.format(QUALITY_LABEL_TEXT, quality));
		lblClass.setText(String.format(CLASS_LABEL_TEXT, patternClass));
		view.setFinger(finger);
	}

	private void clearSegmentInfo() {
		viewSegment1.setFinger(null);
		viewSegment2.setFinger(null);
		viewSegment3.setFinger(null);
		viewSegment4.setFinger(null);
		lblSegment1Position.setText(String.format(POSITION_LABEL_TEXT, ""));
		lblSegment2Position.setText(String.format(POSITION_LABEL_TEXT, ""));
		lblSegment3Position.setText(String.format(POSITION_LABEL_TEXT, ""));
		lblSegment4Position.setText(String.format(POSITION_LABEL_TEXT, ""));
		lblSegment1Quality.setText(String.format(QUALITY_LABEL_TEXT, ""));
		lblSegment2Quality.setText(String.format(QUALITY_LABEL_TEXT, ""));
		lblSegment3Quality.setText(String.format(QUALITY_LABEL_TEXT, ""));
		lblSegment4Quality.setText(String.format(QUALITY_LABEL_TEXT, ""));
		lblSegment1Class.setText(String.format(CLASS_LABEL_TEXT, ""));
		lblSegment2Class.setText(String.format(CLASS_LABEL_TEXT, ""));
		lblSegment3Class.setText(String.format(CLASS_LABEL_TEXT, ""));
		lblSegment4Class.setText(String.format(CLASS_LABEL_TEXT, ""));
	}

	private void saveImages() throws IOException {
		if (fcSave.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
			for (int i = 1; i < subject.getFingers().size(); i++) {
				NFinger finger = subject.getFingers().get(i);
				if (finger.getStatus() == NBiometricStatus.OK) {
					String name = String.format("finger%d %s.png", i, finger.getPosition());
					File file = new File(fcSave.getSelectedFile(), name);
					finger.getImage().save(file.getAbsolutePath());
				}
			}
		}
	}

	private void addMissingPositionCheckbox(NFPosition position) {
		JCheckBox checkbox = new JCheckBox(position.name());
		checkbox.setBackground(Color.WHITE);
		checkBoxList.add(checkbox);
		allPossibleMissingPositions.add(position);
		panelMissingPositions.add(checkbox);
	}

	private void updateSegmentationStatus(NBiometricStatus status) {
		lblStatus.setText(String.format("Segmentation status: %s", status));
		if (status == NBiometricStatus.OK) {
			showSegments();
		} else {
			SwingUtilities.invokeLater(() -> { JOptionPane.showMessageDialog(this, "Segmentation failed: " + status, "Error", JOptionPane.WARNING_MESSAGE); });
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

		fcOpen = new ImageThumbnailFileChooser();
		fcOpen.setIcon(Utils.createIconImage("images/Logo16x16.png"));
		fcOpen.setFileFilter(new Utils.ImageFileFilter(NImages.getOpenFileFilter()));
		fcSave = new ImageThumbnailFileChooser();
		fcSave.setIcon(Utils.createIconImage("images/Logo16x16.png"));
		fcSave.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

		allPossibleMissingPositions = new ArrayList<NFPosition>();
		checkBoxList = new ArrayList<JCheckBox>();

		{
			panelLicensing = new LicensingPanel(requiredLicenses, optionalLicenses);
			add(panelLicensing, java.awt.BorderLayout.NORTH);
		}
		{
			panelMain = new JPanel();
			panelMain.setLayout(new BorderLayout());
			add(panelMain, BorderLayout.CENTER);
			{
				panelTop = new JPanel();
				GridBagLayout panelTopLayout = new GridBagLayout();
				panelTopLayout.columnWeights = new double[] {0.7, 0.3};
				panelTop.setLayout(panelTopLayout);
				panelMain.add(panelTop, BorderLayout.NORTH);
				{
					lblPosition = new JLabel();
					lblPosition.setText("Position");
					gridBagConstraints = new GridBagConstraints();
					gridBagConstraints.gridx = 0;
					gridBagConstraints.gridy = 0;
					gridBagConstraints.anchor = GridBagConstraints.LINE_START;
					panelTop.add(lblPosition, gridBagConstraints);
				}
				{
					lblMissingPositions = new JLabel();
					lblMissingPositions.setText("Missing positions");
					gridBagConstraints = new GridBagConstraints();
					gridBagConstraints.gridx = 1;
					gridBagConstraints.gridy = 0;
					gridBagConstraints.anchor = GridBagConstraints.LINE_START;
					panelTop.add(lblMissingPositions, gridBagConstraints);
				}
				{
					spPositions = new JScrollPane();
					spPositions.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
					listPositions = new JList<>();
					listPositions.addListSelectionListener(this);
					spPositions.setViewportView(listPositions);
					gridBagConstraints = new GridBagConstraints();
					gridBagConstraints.gridx = 0;
					gridBagConstraints.gridy = 1;
					gridBagConstraints.fill = GridBagConstraints.BOTH;
					gridBagConstraints.anchor = GridBagConstraints.PAGE_START;
					panelTop.add(spPositions, gridBagConstraints);
				}
				{
					spMissingPositions = new JScrollPane();
					gridBagConstraints = new GridBagConstraints();
					gridBagConstraints.gridx = 1;
					gridBagConstraints.gridy = 1;
					gridBagConstraints.fill = GridBagConstraints.BOTH;
					gridBagConstraints.anchor = GridBagConstraints.PAGE_START;
					panelTop.add(spMissingPositions, gridBagConstraints);
					{
						panelMissingPositions = new JPanel();
						panelMissingPositions.setBackground(new Color(255, 255, 255));
						panelMissingPositions.setLayout(new BoxLayout(panelMissingPositions, BoxLayout.Y_AXIS));
						spMissingPositions.setViewportView(panelMissingPositions);
					}
				}
				{
					panelUpperButtons = new JPanel();
					panelUpperButtons.setLayout(new FlowLayout(FlowLayout.LEFT));
					gridBagConstraints = new GridBagConstraints();
					gridBagConstraints.gridx = 0;
					gridBagConstraints.gridy = 2;
					gridBagConstraints.anchor = GridBagConstraints.LINE_START;
					panelTop.add(panelUpperButtons, gridBagConstraints);
					{
						btnOpen = new JButton();
						btnOpen.setText("Open image");
						btnOpen.addActionListener(this);
						panelUpperButtons.add(btnOpen);
					}
					{
						btnSegment = new JButton();
						btnSegment.setText("Segment");
						btnSegment.addActionListener(this);
						panelUpperButtons.add(btnSegment);
					}
				}
			}
			{
				panelCenter = new JPanel();
				panelCenter.setLayout(new GridBagLayout());
				panelMain.add(panelCenter, BorderLayout.CENTER);
				{
					spImage = new JScrollPane();
					gridBagConstraints = new GridBagConstraints();
					gridBagConstraints.fill = GridBagConstraints.BOTH;
					gridBagConstraints.weightx = 0.1;
					gridBagConstraints.weighty = 0.2;
					panelCenter.add(spImage, gridBagConstraints);
					{
						viewMain = new NFingerView();
						viewMain.setAutofit(true);
						spImage.setViewportView(viewMain);
					}
				}
				{
					panelSegments = new JPanel();
					panelSegments.setLayout(new GridLayout(1, 4, 3, 3));
					gridBagConstraints = new GridBagConstraints();
					gridBagConstraints.gridx = 0;
					gridBagConstraints.gridy = 1;
					gridBagConstraints.fill = GridBagConstraints.BOTH;
					gridBagConstraints.weightx = 0.1;
					gridBagConstraints.weighty = 0.1;
					panelCenter.add(panelSegments, gridBagConstraints);
					{
						spSegment1 = new JScrollPane();
						panelSegments.add(spSegment1);
						{
							panelSegment1 = new JPanel();
							panelSegment1.setLayout(new OverlayLayout(panelSegment1));
							spSegment1.setViewportView(panelSegment1);
							{
								panelSegment1Labels = new JPanel();
								panelSegment1Labels.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
								panelSegment1Labels.setOpaque(false);
								GridBagLayout panelSegment1LabelsLayout = new GridBagLayout();
								panelSegment1LabelsLayout.rowWeights = new double[] {1.0, 0.0, 0.0, 0.0};
								panelSegment1Labels.setLayout(panelSegment1LabelsLayout);
								panelSegment1.add(panelSegment1Labels);
								{
									lblSegment1Position = new JLabel();
									lblSegment1Position.setText(" ");
									lblSegment1Position.setOpaque(true);
									gridBagConstraints = new GridBagConstraints();
									gridBagConstraints.gridx = 1;
									gridBagConstraints.gridy = 1;
									gridBagConstraints.anchor = GridBagConstraints.LINE_START;
									panelSegment1Labels.add(lblSegment1Position, gridBagConstraints);
								}
								{
									lblSegment1Quality = new JLabel();
									lblSegment1Quality.setText(" ");
									lblSegment1Quality.setOpaque(true);
									gridBagConstraints = new GridBagConstraints();
									gridBagConstraints.gridx = 1;
									gridBagConstraints.gridy = 2;
									gridBagConstraints.anchor = GridBagConstraints.LINE_START;
									panelSegment1Labels.add(lblSegment1Quality, gridBagConstraints);
								}
								{
									lblSegment1Class = new JLabel();
									lblSegment1Class.setText(" ");
									lblSegment1Class.setOpaque(true);
									gridBagConstraints = new GridBagConstraints();
									gridBagConstraints.gridx = 1;
									gridBagConstraints.gridy = 3;
									gridBagConstraints.anchor = GridBagConstraints.LINE_START;
									panelSegment1Labels.add(lblSegment1Class, gridBagConstraints);
								}
							}
							{
								viewSegment1 = new NFingerView();
								viewSegment1.setAutofit(true);
								viewSegment1.setAllowHover(false);
								viewSegment1.setAllowSelection(false);
								panelSegment1.add(viewSegment1);
							}
						}
					}
					{
						spSegment2 = new JScrollPane();
						panelSegments.add(spSegment2);
						{
							panelSegment2 = new JPanel();
							panelSegment2.setLayout(new OverlayLayout(panelSegment2));
							spSegment2.setViewportView(panelSegment2);
							{
								panelSegment2Labels = new JPanel();
								panelSegment2Labels.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
								panelSegment2Labels.setOpaque(false);
								GridBagLayout panelSegment2LabelsLayout = new GridBagLayout();
								panelSegment2LabelsLayout.rowWeights = new double[] {1.0, 0.0, 0.0, 0.0};
								panelSegment2Labels.setLayout(panelSegment2LabelsLayout);
								panelSegment2.add(panelSegment2Labels);
								{
									lblSegment2Position = new JLabel();
									lblSegment2Position.setText(" ");
									lblSegment2Position.setOpaque(true);
									gridBagConstraints = new GridBagConstraints();
									gridBagConstraints.gridx = 1;
									gridBagConstraints.gridy = 1;
									gridBagConstraints.anchor = GridBagConstraints.LINE_START;
									panelSegment2Labels.add(lblSegment2Position, gridBagConstraints);
								}
								{
									lblSegment2Quality = new JLabel();
									lblSegment2Quality.setText(" ");
									lblSegment2Quality.setOpaque(true);
									gridBagConstraints = new GridBagConstraints();
									gridBagConstraints.gridx = 1;
									gridBagConstraints.gridy = 2;
									gridBagConstraints.anchor = GridBagConstraints.LINE_START;
									panelSegment2Labels.add(lblSegment2Quality, gridBagConstraints);
								}
								{
									lblSegment2Class = new JLabel();
									lblSegment2Class.setText(" ");
									lblSegment2Class.setOpaque(true);
									gridBagConstraints = new GridBagConstraints();
									gridBagConstraints.gridx = 1;
									gridBagConstraints.gridy = 3;
									gridBagConstraints.anchor = GridBagConstraints.LINE_START;
									panelSegment2Labels.add(lblSegment2Class, gridBagConstraints);
								}
							}
							{
								viewSegment2 = new NFingerView();
								viewSegment2.setAutofit(true);
								viewSegment2.setAllowHover(false);
								viewSegment2.setAllowSelection(false);
								panelSegment2.add(viewSegment2);
							}
						}
					}
					{
						spSegment3 = new JScrollPane();
						panelSegments.add(spSegment3);
						{
							panelSegment3 = new JPanel();
							panelSegment3.setLayout(new OverlayLayout(panelSegment3));
							spSegment3.setViewportView(panelSegment3);
							{
								panelSegment3Labels = new JPanel();
								panelSegment3Labels.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
								panelSegment3Labels.setOpaque(false);
								GridBagLayout panelSegment3LabelsLayout = new GridBagLayout();
								panelSegment3LabelsLayout.rowWeights = new double[] {1.0, 0.0, 0.0, 0.0};
								panelSegment3Labels.setLayout(panelSegment3LabelsLayout);
								panelSegment3.add(panelSegment3Labels);
								{
									lblSegment3Position = new JLabel();
									lblSegment3Position.setText(" ");
									lblSegment3Position.setOpaque(true);
									gridBagConstraints = new GridBagConstraints();
									gridBagConstraints.gridx = 1;
									gridBagConstraints.gridy = 1;
									gridBagConstraints.anchor = GridBagConstraints.LINE_START;
									panelSegment3Labels.add(lblSegment3Position, gridBagConstraints);
								}
								{
									lblSegment3Quality = new JLabel();
									lblSegment3Quality.setText(" ");
									lblSegment3Quality.setOpaque(true);
									gridBagConstraints = new GridBagConstraints();
									gridBagConstraints.gridx = 1;
									gridBagConstraints.gridy = 2;
									gridBagConstraints.anchor = GridBagConstraints.LINE_START;
									panelSegment3Labels.add(lblSegment3Quality, gridBagConstraints);
								}
								{
									lblSegment3Class = new JLabel();
									lblSegment3Class.setText(" ");
									lblSegment3Class.setOpaque(true);
									gridBagConstraints = new GridBagConstraints();
									gridBagConstraints.gridx = 1;
									gridBagConstraints.gridy = 3;
									gridBagConstraints.anchor = GridBagConstraints.LINE_START;
									panelSegment3Labels.add(lblSegment3Class, gridBagConstraints);
								}
							}
							{
								viewSegment3 = new NFingerView();
								viewSegment3.setAutofit(true);
								viewSegment3.setAllowHover(false);
								viewSegment3.setAllowSelection(false);
								panelSegment3.add(viewSegment3);
							}
						}
					}
					{
						spSegment4 = new JScrollPane();
						panelSegments.add(spSegment4);
						{
							panelSegment4 = new JPanel();
							panelSegment4.setLayout(new OverlayLayout(panelSegment4));
							spSegment4.setViewportView(panelSegment4);
							{
								panelSegment4Labels = new JPanel();
								panelSegment4Labels.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
								panelSegment4Labels.setOpaque(false);
								GridBagLayout panelSegment4LabelsLayout = new GridBagLayout();
								panelSegment4LabelsLayout.rowWeights = new double[] {1.0, 0.0, 0.0, 0.0};
								panelSegment4Labels.setLayout(panelSegment4LabelsLayout);
								panelSegment4.add(panelSegment4Labels);
								{
									lblSegment4Position = new JLabel();
									lblSegment4Position.setText(" ");
									lblSegment4Position.setOpaque(true);
									gridBagConstraints = new GridBagConstraints();
									gridBagConstraints.gridx = 1;
									gridBagConstraints.gridy = 1;
									gridBagConstraints.anchor = GridBagConstraints.LINE_START;
									panelSegment4Labels.add(lblSegment4Position, gridBagConstraints);
								}
								{
									lblSegment4Quality = new JLabel();
									lblSegment4Quality.setText(" ");
									lblSegment4Quality.setOpaque(true);
									gridBagConstraints = new GridBagConstraints();
									gridBagConstraints.gridx = 1;
									gridBagConstraints.gridy = 2;
									gridBagConstraints.anchor = GridBagConstraints.LINE_START;
									panelSegment4Labels.add(lblSegment4Quality, gridBagConstraints);
								}
								{
									lblSegment4Class = new JLabel();
									lblSegment4Class.setText(" ");
									lblSegment4Class.setOpaque(true);
									gridBagConstraints = new GridBagConstraints();
									gridBagConstraints.gridx = 1;
									gridBagConstraints.gridy = 3;
									gridBagConstraints.anchor = GridBagConstraints.LINE_START;
									panelSegment4Labels.add(lblSegment4Class, gridBagConstraints);
								}
							}
							{
								viewSegment4 = new NFingerView();
								viewSegment4.setAutofit(true);
								viewSegment4.setAllowHover(false);
								viewSegment4.setAllowSelection(false);
								panelSegment4.add(viewSegment4);
							}
						}
					}
				}
			}
			{
				panelBottom = new JPanel();
				panelBottom.setLayout(new BorderLayout());
				panelMain.add(panelBottom, BorderLayout.SOUTH);
				{
					panelStatus = new JPanel();
					panelStatus.setLayout(new FlowLayout(FlowLayout.LEFT));
					panelBottom.add(panelStatus, BorderLayout.WEST);
					{
						btnSave = new JButton();
						btnSave.setText("Save images");
						btnSave.addActionListener(this);
						panelStatus.add(btnSave);
					}
					{
						lblStatus = new JLabel();
						lblStatus.setText(" ");
						panelStatus.add(lblStatus);
					}
				}
				{
					zoomToFitFitPanel = new JPanel();
					zoomToFitFitPanel.setLayout(new BorderLayout());
					panelBottom.add(zoomToFitFitPanel, BorderLayout.EAST);
					{
						zoomSlider = new NViewZoomSlider();
						zoomSlider.setView(viewMain);
						zoomToFitFitPanel.add(zoomSlider);
					}
				}
			}
		}
		initFingerPositions();
	}

	@Override
	protected void setDefaultValues() {
		// No default values.
	}

	@Override
	protected void updateControls() {
		btnOpen.setEnabled(true);
		btnSegment.setEnabled((subject != null) && (subject.getFingers().get(0).getImage() != null));
		btnSave.setEnabled((subject != null) && (subject.getFingers().size() > 1));
	}

	@Override
	protected void updateFingersTools() {
		// Nothing to update.
	}

	// ===========================================================
	// Event handling
	// ===========================================================

	@Override
	public void actionPerformed(ActionEvent ev) {
		try {
			if (ev.getSource() == btnOpen) {
				openImage();
			} else if (ev.getSource() == btnSegment) {
				segment();
			} else if (ev.getSource() == btnSave) {
				saveImages();
			}
		} catch (Exception e) {
			e.printStackTrace();
			SwingUtilities.invokeLater(() -> { JOptionPane.showMessageDialog(this, e, "Error", JOptionPane.ERROR_MESSAGE); });
		}
	}

	@Override
	public void valueChanged(ListSelectionEvent e) {
		NFPosition position = (NFPosition) listPositions.getSelectedValue();
		panelMissingPositions.removeAll();
		checkBoxList.clear();
		allPossibleMissingPositions.clear();
		if (position == NFPosition.PLAIN_LEFT_FOUR_FINGERS) {
			addMissingPositionCheckbox(NFPosition.LEFT_LITTLE_FINGER);
			addMissingPositionCheckbox(NFPosition.LEFT_RING_FINGER);
			addMissingPositionCheckbox(NFPosition.LEFT_MIDDLE_FINGER);
			addMissingPositionCheckbox(NFPosition.LEFT_INDEX_FINGER);
		} else if (position == NFPosition.PLAIN_RIGHT_FOUR_FINGERS) {
			addMissingPositionCheckbox(NFPosition.RIGHT_INDEX_FINGER);
			addMissingPositionCheckbox(NFPosition.RIGHT_MIDDLE_FINGER);
			addMissingPositionCheckbox(NFPosition.RIGHT_RING_FINGER);
			addMissingPositionCheckbox(NFPosition.RIGHT_LITTLE_FINGER);
		} else if (position == NFPosition.PLAIN_THUMBS) {
			addMissingPositionCheckbox(NFPosition.LEFT_THUMB);
			addMissingPositionCheckbox(NFPosition.RIGHT_THUMB);
		}
		panelMissingPositions.revalidate();
		panelMissingPositions.repaint();
	}

	// ===========================================================
	// Inner classes
	// ===========================================================

	private class SegmentHandler implements CompletionHandler<NBiometricTask, Object> {

		@Override
		public void completed(final NBiometricTask task, final Object attachment) {
			SwingUtilities.invokeLater(new Runnable() {

				@Override
				public void run() {
					updateSegmentationStatus(task.getStatus());
				}

			});
		}

		@Override
		public void failed(final Throwable th, final Object attachment) {
			SwingUtilities.invokeLater(new Runnable() {

				@Override
				public void run() {
					showError(th);
				}

			});
		}

	}

}
