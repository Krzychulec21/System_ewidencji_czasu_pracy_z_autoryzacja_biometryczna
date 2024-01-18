package com.neurotec.samples;

import java.util.List;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import com.neurotec.util.concurrent.AggregateExecutionException;

public abstract class BasePanel extends JPanel {

	// ===========================================================
	// Private static fields
	// ===========================================================

	private static final long serialVersionUID = 1L;

	// ===========================================================
	// Protected fields
	// ===========================================================

	protected LicensingPanel panelLicensing;
	protected List<String> requiredLicenses;
	protected List<String> optionalLicenses;
	protected boolean obtained;

	// ===========================================================
	// Public methods
	// ===========================================================

	public void init() {
		initGUI();
		setDefaultValues();
		updateControls();
	}

	public final List<String> getRequiredLicenses() {
		return requiredLicenses;
	}

	public final List<String> getOptionalLicenses() {
		return optionalLicenses;
	}

	public final LicensingPanel getLicensingPanel() {
		return panelLicensing;
	}

	public final void updateLicensing(boolean status) {
		panelLicensing.setComponentObtainingStatus(status);
		obtained = status;
	}

	public boolean isObtained() {
		return obtained;
	}

	public void showError(Throwable e) {
		e.printStackTrace();
		if (e instanceof AggregateExecutionException) {
			StringBuilder sb = new StringBuilder(64);
			sb.append("Execution resulted in one or more errors:\n");
			for (Throwable cause : ((AggregateExecutionException) e).getCauses()) {
				sb.append(cause.toString()).append('\n');
			}
			SwingUtilities.invokeLater(() -> { JOptionPane.showMessageDialog(this, sb.toString(), "Execution failed", JOptionPane.ERROR_MESSAGE); });
		} else {
			SwingUtilities.invokeLater(() -> { JOptionPane.showMessageDialog(this, e, "Error", JOptionPane.ERROR_MESSAGE); });
		}
	}

	public void showError(String message) {
		if (message == null) throw new NullPointerException("message");
		SwingUtilities.invokeLater(() -> { JOptionPane.showMessageDialog(this, message); });
	}

	// ===========================================================
	// Abstract methods
	// ===========================================================

	protected abstract void initGUI();
	protected abstract void setDefaultValues();
	protected abstract void updateControls();
	protected abstract void updateFingersTools();

}
