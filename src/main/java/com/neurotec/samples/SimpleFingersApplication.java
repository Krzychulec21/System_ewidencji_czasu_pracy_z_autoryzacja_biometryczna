package com.neurotec.samples;

import java.awt.BorderLayout;
import java.io.IOException;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import com.neurotec.licensing.NLicenseManager;
import com.neurotec.samples.util.LibraryManager;
import com.neurotec.samples.util.Utils;

public final class SimpleFingersApplication {

	// ===========================================================
	// Public static  method
	// ===========================================================

	static {
		System.load("C:\\Users\\ketow\\Documents\\3R1S\\PZWBI\\Neurotec_Biometric_13_0_SDK_2023-11-07\\Neurotec_Biometric_13_0_SDK\\Bin\\Win64_x64\\NCore.dll");
	}

	public static void main(String[] args) {
		Utils.setupLookAndFeel();
		LibraryManager.initLibraryPath();

		//=========================================================================
		// TRIAL MODE
		//=========================================================================
		// Below code line determines whether TRIAL is enabled or not. To use purchased licenses, don't use below code line.
		// GetTrialModeFlag() method takes value from "Bin/Licenses/TrialFlag.txt" file. So to easily change mode for all our examples, modify that file.
		// Also you can just set TRUE to "TrialMode" property in code.
		//=========================================================================

		try {
			boolean trialMode = Utils.getTrialModeFlag();
			NLicenseManager.setTrialMode(trialMode);
			System.out.println("\tTrial mode: " + trialMode);
		} catch (IOException e) {
			e.printStackTrace();
		}

		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				JFrame frame = new JFrame();
				frame.setTitle("Simple Fingers Sample");
				frame.setIconImage(Utils.createIconImage("images/Logo16x16.png"));
				frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
				frame.add(new MainPanel(), BorderLayout.CENTER);
				frame.pack();
				frame.setLocationRelativeTo(null);
				frame.setVisible(true);
			}
		});
	}

	// ===========================================================
	// Private constructor
	// ===========================================================

	private SimpleFingersApplication() {
	}
}
