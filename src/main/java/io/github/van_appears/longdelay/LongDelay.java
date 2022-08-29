package io.github.van_appears.longdelay;

import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.function.Consumer;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JRadioButton;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;

import io.github.van_appears.longdelay.EchoMachine.RecordType;

public class LongDelay {
	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMdd-HHmmss");
	private static final String DEFAULT_PROPERTIES_PREFIX = "longloop";
	
	private JSpinner echo1Length = new JSpinner(new SpinnerNumberModel
			(10, EchoModel.MIN_LENGTH_SECONDS, EchoModel.MAX_LENGTH_SECONDS, 0.1));
	private JButton echo1Clear = new JButton("Clear");

	private JComboBox<LineSettings.LineSetting> inputs = new JComboBox<>();
	private JComboBox<LineSettings.LineSetting> outputs = new JComboBox<>();
	private JButton refresh = new JButton("Refresh");
	private JButton restart = new JButton("Restart");
	
	private JRadioButton useOutput = new JRadioButton("Record output");
	private JRadioButton useInput = new JRadioButton("Record input");
	private JSpinner recordLength = new JSpinner(new SpinnerNumberModel
			(180.0, 1.0, StreamWriter.MAX_LENGTH_SECONDS, 1.0));
	private JLabel timerLabel = new JLabel("0");
	private JTextField recordPrefix = new JTextField();
	private JCheckBox clearOnRecord = new JCheckBox("Clear on record");
	private JButton record = new JButton("Record");
	private JButton load = new JButton("Load properties");

	private EchoModel echo1;
	private EchoMachine echoMachine;
	private LineSettings lineSettings;
	private TimerThread timerThread;

	public static void main(String[] args) {
		new LongDelay();
	}

	private LongDelay() {
		echo1 = new EchoModel();
		echoMachine = new EchoMachine(echo1);
		lineSettings = new LineSettings();

		JFrame frame = new JFrame("Long loop");
		buildUI(frame.getContentPane());
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.pack();
		frame.setResizable(false);
		frame.setLocationRelativeTo(null);
		frame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				saveDefaultValues();
			}
		});
		loadDefaultValues();
		frame.setVisible(true);
		SwingUtilities.invokeLater(() -> {
			setInputAndOutput();
			echoMachine.start();
		});
	}

	private void buildUI(Container container) {
		container.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.insets = new Insets(2, 4, 2, 4);

		setInputsAndOutputs();
		layoutLabels(container, c);
		layoutControls(container, c);
		layoutButtons(container, c);
		layoutSeparators(container, c);
		connectListeners(container);
	}
	
	private void setInputsAndOutputs() {
		inputs.removeAllItems();
		outputs.removeAllItems();
		for (LineSettings.LineSetting item : lineSettings.getInputs()) {
			inputs.addItem(item);
		}
		for (LineSettings.LineSetting item : lineSettings.getOutputs()) {
			outputs.addItem(item);
		}
		if (inputs.getItemCount() == 0 || outputs.getItemCount() == 0) {
			// a bit rough...
			System.exit(1);
		}
	}
	
	private void setInputAndOutput() {
		echoMachine.reconnect(
			(LineSettings.LineSetting)inputs.getSelectedItem(),
			(LineSettings.LineSetting)outputs.getSelectedItem()
		);
	}

	private void layoutLabels(Container container, GridBagConstraints c) {
		c.gridwidth = 1;
		c.gridx = 0;
		c.gridy = 0;
		container.add(new JLabel("Input:"), c);
		c.gridy = 1;
		container.add(new JLabel("Output:"), c);
		c.gridy = 3;
		container.add(new JLabel("Echo length:"), c);
		c.gridy = 7;
		container.add(new JLabel("Recording length:"), c);
		c.gridy = 8;
		container.add(new JLabel("File prefix:"), c);
		c.gridy = 9;
		c.gridx = 2;
		container.add(timerLabel, c);
	}

	private void layoutControls(Container container, GridBagConstraints c) {
		c.gridx = 1;
		c.gridwidth = 1;
		c.gridy = 0;
		container.add(inputs, c);
		c.gridy = 1;
		container.add(outputs, c);
		c.gridy = 3;
		container.add(echo1Length, c);
		c.gridy = 7;
		container.add(recordLength, c);
		c.gridwidth = 2;
		c.ipadx = 100;
		c.gridy = 8;
		container.add(recordPrefix, c);
		c.ipadx = 0;
	}

	private void layoutButtons(Container container, GridBagConstraints c) {
		c.gridwidth = 1;
		c.gridx = 3;
		c.gridy = 0;
		container.add(refresh, c);
		c.gridy = 1;
		container.add(restart, c);
		c.gridy = 3;
		container.add(echo1Clear, c);
		c.gridy = 6;
		container.add(clearOnRecord, c);
		c.gridy = 7;
		container.add(record, c);
		c.gridy = 8;
		container.add(load, c);
		c.gridy = 6;
		c.gridx = 0;
		container.add(useOutput, c);
		c.gridx = 1;
		container.add(useInput, c);	
	}

	private void layoutSeparators(Container container, GridBagConstraints c) {
		c.gridwidth = 4;
		c.gridx = 0;
		c.gridy = 2;
		container.add(new JSeparator(), c);
		c.gridy = 5;
		container.add(new JSeparator(), c);
	}

	private void connectListeners(Container container) {
		refresh.addActionListener(a -> setInputsAndOutputs());
		restart.addActionListener(a -> setInputAndOutput());
		echo1Clear.addActionListener(a -> echo1.clear());
		connectLengthControl(echo1Length, i -> echo1.setFrameLength(i));
		
		ButtonGroup group = new ButtonGroup();
		group.add(useOutput);
		group.add(useInput);
		useOutput.setSelected(true);
		useOutput.addActionListener(a -> echoMachine.setRecordType(RecordType.Output));
		useInput.addActionListener(a -> echoMachine.setRecordType(RecordType.Input));

		connectRecordControls();
		connectLoadControl(container);
	}

	private void connectLengthControl(JSpinner length, Consumer<Integer> processor) {
		length.addChangeListener(c -> {
			processor.accept((int)(
				(double)EchoModel.SAMPLE_RATE * (double)length.getValue()
			));
		});
	}
	
	private void connectRecordControls() {
		record.addActionListener(a -> {
			if (echoMachine.isRecording()) {
				record.setText("Start");
				echoMachine.stopRecording();
			} else {
				record.setText("Cancel");
				recordLength.setEnabled(false);
				recordPrefix.setEnabled(false);
				inputs.setEnabled(false);
				outputs.setEnabled(false);
				refresh.setEnabled(false);
				restart.setEnabled(false);
				load.setEnabled(false);
				
				String outFile = recordPrefix.getText().trim();
				if (!outFile.isEmpty()) { outFile += "-"; }
				outFile += DATE_FORMAT.format(new Date());
				
				if (clearOnRecord.isSelected()) {
					echo1.clear();
				}
				echoMachine.startRecording((double)recordLength.getValue(), outFile);
				timerThread = new TimerThread();
				saveValues(new File(outFile + ".properties"));
			}
		});
		echoMachine.setCompletionListener(() -> {
			System.out.println("Completed");
			timerThread.stopTimer();
			record.setText("Start");
			recordLength.setEnabled(true);
			recordPrefix.setEnabled(true);
			inputs.setEnabled(true);
			outputs.setEnabled(true);
			refresh.setEnabled(true);
			restart.setEnabled(true);
			load.setEnabled(true);
		});
	}
	
	private void connectLoadControl(Container container) {
		load.addActionListener(a -> {
			JFileChooser chooser = new JFileChooser(new File("."));
			FileNameExtensionFilter filter = new FileNameExtensionFilter(
				"Properties files", "properties");
			chooser.setFileFilter(filter);
			int returnVal = chooser.showOpenDialog(container);
			if(returnVal == JFileChooser.APPROVE_OPTION) {
				loadValues(chooser.getSelectedFile());
			}
		});
	}

	private void saveDefaultValues() {
		saveValues(new File(DEFAULT_PROPERTIES_PREFIX + ".properties"));
	}

	private void saveValues(File file) {
		try {
			saveProperties(file);
		} catch (Exception e) {
			System.out.println("Failed to save properties: " + e.getMessage());
		}
	}

	private void saveProperties(File file) throws Exception {
		Properties properties = new Properties();
		properties.setProperty("echo1Length", String.valueOf(echo1Length.getValue()));
		properties.setProperty("clearOnRecord", String.valueOf(clearOnRecord.isSelected()));
		properties.setProperty("recordLength", String.valueOf(recordLength.getValue()));
		properties.setProperty("recordPrefix", recordPrefix.getText());
		properties.setProperty("useInput", String.valueOf(useInput.isSelected()));
		properties.store(new FileOutputStream(file), "");
	}
	
	private void loadDefaultValues() {
		loadValues(new File(DEFAULT_PROPERTIES_PREFIX + ".properties"));
	}

	private void loadValues(File file) {
		try {
			loadProperties(file);
		} catch (Exception e) {
			System.out.println("Failed to load properties");
			e.printStackTrace();
		}
	}
	
	private void loadProperties(File file) throws Exception {
		Properties properties = new Properties();
		properties.load(new FileInputStream(file));
		echo1Length.setValue(Double.parseDouble(properties.getProperty("echo1Length")));
		clearOnRecord.setSelected(Boolean.parseBoolean(properties.getProperty("clearOnRecord")));
		recordLength.setValue(Double.parseDouble(properties.getProperty("recordLength")));
		recordPrefix.setText(properties.getProperty("recordPrefix"));
		if (Boolean.parseBoolean("useInput")) {
			useInput.setSelected(true);
		} else {
			useOutput.setSelected(true);
		}
	}
	
	private class TimerThread extends Thread {
		boolean running = true;
		int timer = 0;

		public TimerThread() {
			timerLabel.setText("0");
			start();
		}
		public void stopTimer() {
			running = false;
		}
		public void run() {
			while(running) {
				try {
					sleep(1000);
					timer++;
					timerLabel.setText(String.valueOf(timer));
				} catch (Exception e) {}
			}
		}
	}
}
