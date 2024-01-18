package com.neurotec.samples;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.io.IOException;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public final class MainPanel extends JPanel implements ChangeListener {

	// ===========================================================
	// Private static fields
	// ===========================================================

	private static final long serialVersionUID = 1L;

	// ===========================================================
	// Private fields
	// ===========================================================

	private JTabbedPane tabbedPane;
	private EnrollFromImage enrollFromImage;
	private EnrollFromScanner enrollFromScanner;
	private IdentifyFinger identifyFinger;
	private VerifyFinger verifyFinger;
	private SegmentFingers segmentFingers;

	// ===========================================================
	// Public constructor
	// ===========================================================

	public MainPanel() {
		super(new GridLayout(1, 1));
		initGUI();
	}

	// ===========================================================
	// Private methods
	// ===========================================================

	private void initGUI() {
		tabbedPane = new JTabbedPane();
		tabbedPane.addChangeListener(this);

		enrollFromImage = new EnrollFromImage();
		enrollFromImage.init();
		tabbedPane.addTab("Enroll from image", enrollFromImage);

		enrollFromScanner = new EnrollFromScanner();
		enrollFromScanner.init();
		tabbedPane.addTab("Enroll from scanner", enrollFromScanner);

		identifyFinger = new IdentifyFinger();
		identifyFinger.init();
		tabbedPane.addTab("Identify finger", identifyFinger);

		verifyFinger = new VerifyFinger();
		verifyFinger.init();
		tabbedPane.addTab("Verify finger", verifyFinger);

		segmentFingers = new SegmentFingers();
		segmentFingers.init();
		tabbedPane.addTab("Segment fingers", segmentFingers);

		add(tabbedPane);
		setPreferredSize(new Dimension(680, 600));
		tabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
	}

	// ===========================================================
	// Public methods
	// ===========================================================

	public void obtainLicenses(BasePanel panel) throws IOException {
		if (!panel.isObtained()) {
			boolean status = FingersTools.getInstance().obtainLicenses(panel.getRequiredLicenses());
			FingersTools.getInstance().obtainLicenses(panel.getOptionalLicenses());
			panel.getLicensingPanel().setRequiredComponents(panel.getRequiredLicenses());
			panel.getLicensingPanel().setOptionalComponents(panel.getOptionalLicenses());
			panel.updateLicensing(status);
		}
	}

	// ===========================================================
	// Event handling
	// ===========================================================

	@Override
	public void stateChanged(ChangeEvent evt) {
		if (evt.getSource() == tabbedPane) {
			try {
				switch (tabbedPane.getSelectedIndex()) {
				case 0: {
					obtainLicenses(enrollFromImage);
					enrollFromImage.updateFingersTools();
					break;
				}
				case 1: {
					obtainLicenses(enrollFromScanner);
					enrollFromScanner.updateFingersTools();
					enrollFromScanner.updateScannerList();
					break;
				}
				case 2: {
					obtainLicenses(identifyFinger);
					identifyFinger.updateFingersTools();
					break;
				}
				case 3: {
					obtainLicenses(verifyFinger);
					verifyFinger.updateFingersTools();
					break;
				}
				case 4: {
					obtainLicenses(segmentFingers);
					segmentFingers.updateFingersTools();
					break;
				}
				default: {
					throw new IndexOutOfBoundsException("unreachable");
				}
				}
			} catch (IOException e) {
				e.printStackTrace();
				SwingUtilities.invokeLater(() -> { JOptionPane.showMessageDialog(this, "Could not obtain licenses for components: " + e.toString(), "Error", JOptionPane.ERROR_MESSAGE); });
			}
		}
	}

}
