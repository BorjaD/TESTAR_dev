package nl.ou.testar.StateModel.Settings;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import nl.ou.testar.StateModel.ModelDifferenceManager;

public class ModelDifferenceDialog extends JDialog {
	
	private static final long serialVersionUID = 7890181945543399039L;

	public ModelDifferenceDialog(String storeType, String storeServer) {
		initialize(storeType, storeServer);
	}
	
	private JLabel labelStoreType = new JLabel("DataStoreType");
	private JLabel labelStoreServer = new JLabel("DataStoreServer");
	private JLabel labelRoot = new JLabel("RootUser");
	private JLabel labelPassword = new JLabel("RootPassword");
	private JLabel labelStoreDB = new JLabel("Existing DB");
	
	private JTextField textFieldStoreType = new JTextField();
	private JTextField textFieldStoreServer = new JTextField();
	private JTextField textFieldRoot = new JTextField();
	private JPasswordField textFieldPassword = new JPasswordField();
	
	private JButton buttonConnect = new JButton("Connect");
	private JButton buttonModelDiff = new JButton("Model-Diff");
	private JButton buttonCancel = new JButton("Cancel Artefact");
	
	private JComboBox<String> listDatabases = new JComboBox<>();
	
	private JLabel labelApplicationNameOne = new JLabel("1. Application Name");
	private JLabel labelApplicationVersionOne = new JLabel("1. Application Version");
	private JLabel labelApplicationNameTwo = new JLabel("2. Application Name");
	private JLabel labelApplicationVersionTwo = new JLabel("2. Application Version");
	private JTextField textApplicationNameOne = new JTextField();
	private JTextField textApplicationVersionOne = new JTextField();
	private JTextField textApplicationNameTwo = new JTextField();
	private JTextField textApplicationVersionTwo = new JTextField();
	
	private void initialize(String storeType, String storeServer) {

		setTitle("TESTAR State Model Difference");

		setSize(1000, 500);
		setLayout(null);
		setVisible(true);
		setLocationRelativeTo(null);

		labelStoreType.setBounds(10,14,150,27);
		add(labelStoreType);
		textFieldStoreType.setBounds(160,14,125,27);
		textFieldStoreType.setText(storeType);
		add(textFieldStoreType);

		labelStoreServer.setBounds(10,52,150,27);
		add(labelStoreServer);
		textFieldStoreServer.setBounds(160,52,125,27);
		textFieldStoreServer.setText(storeServer);
		add(textFieldStoreServer);

		labelRoot.setBounds(10,90,150,27);
		add(labelRoot);
		textFieldRoot.setBounds(160,90,125,27);
		textFieldRoot.setText("root");
		add(textFieldRoot);

		labelPassword.setBounds(10,128,150,27);
		add(labelPassword);
		textFieldPassword.setBounds(160,128,125,27);
		add(textFieldPassword);

		buttonConnect.setBounds(330, 166, 150, 27);
		buttonConnect.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				ModelDifferenceManager.obtainAvailableDatabases(textFieldStoreType.getText(), 
						textFieldStoreServer.getText(), textFieldRoot.getText(), getPassword(textFieldPassword),
								listDatabases);
			}
		});
		add(buttonConnect);

		labelStoreDB.setBounds(10,166,150,27);
		add(labelStoreDB);
		listDatabases.setBounds(160,166,150,27);
		add(listDatabases);

		labelApplicationNameOne.setBounds(10,204,150,27);
		add(labelApplicationNameOne);
		textApplicationNameOne.setBounds(160,204,325,27);
		textApplicationNameOne.setText("NombreApp");
		add(textApplicationNameOne);

		labelApplicationVersionOne.setBounds(10,242,150,27);
		add(labelApplicationVersionOne);
		textApplicationVersionOne.setBounds(160,242,325,27);
		textApplicationVersionOne.setText("VersionApp");
		add(textApplicationVersionOne);
		
		labelApplicationNameTwo.setBounds(510,204,150,27);
		add(labelApplicationNameTwo);
		textApplicationNameTwo.setBounds(660,204,325,27);
		textApplicationNameTwo.setText("NombreApp");
		add(textApplicationNameTwo);

		labelApplicationVersionTwo.setBounds(510,242,150,27);
		add(labelApplicationVersionTwo);
		textApplicationVersionTwo.setBounds(660,242,325,27);
		textApplicationVersionTwo.setText("VersionApp");
		add(textApplicationVersionTwo);

		buttonModelDiff.setBounds(330, 356, 150, 27);
		buttonModelDiff.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				ModelDifferenceManager.calculateModelDifference(textFieldStoreType.getText(), 
						textFieldStoreServer.getText(), textFieldRoot.getText(), getPassword(textFieldPassword),
						listDatabases.getSelectedItem().toString(),
						textApplicationNameOne.getText(), textApplicationVersionOne.getText(),
						textApplicationNameTwo.getText(), textApplicationVersionTwo.getText());
			}
		});
		add(buttonModelDiff);

		buttonCancel.setBounds(330, 408, 150, 27);
		buttonCancel.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				ModelDifferenceManager.closeOrientDB();
				dispose();
			}
		});
		add(buttonCancel);

		this.addWindowListener(new java.awt.event.WindowAdapter() {
			@Override
			public void windowClosing(java.awt.event.WindowEvent windowEvent) {
				ModelDifferenceManager.closeOrientDB();
			}
		});
	}
	
	/**
	 * Convert password field to string.
	 * @return password as String.
	 */
	private static String getPassword(JPasswordField passField) {
		StringBuilder result= new StringBuilder();
		for(char c : passField.getPassword()) {
			result.append(c);
		}
		return  result.toString();
	}  
	
}
