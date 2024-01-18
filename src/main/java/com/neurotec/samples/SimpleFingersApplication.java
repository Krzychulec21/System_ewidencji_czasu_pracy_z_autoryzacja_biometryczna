package com.neurotec.samples;

import java.awt.BorderLayout;
import java.io.IOException;

import javax.swing.*;

import com.neurotec.licensing.NLicenseManager;
import com.neurotec.samples.util.LibraryManager;
import com.neurotec.samples.util.Utils;
import hibernate.dao.EmployeeDao;
import hibernate.entity.Employee;
import hibernate.util.HibernateUtil;

public final class SimpleFingersApplication {
	// ===========================================================
	// Public static  method
	// ===========================================================

	public static String path = "./src/main/resources/templates/";

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

		EmployeeDao dao = new EmployeeDao();

		Employee emp1 = new Employee("Wojciech", "Olejko", path+"wojtek_index");
		Employee emp2 = new Employee("Krzysztof", "Kaczka", path+"krzychu_index");

		dao.addEmployee(emp1);
		dao.addEmployee(emp2);

		for (Employee emp : dao.getEmployees()) {
			System.out.println(emp);
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
