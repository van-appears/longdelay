package io.github.van_appears.longdelay;

import java.io.File;
import java.util.LinkedList;
import java.util.Queue;

import javax.sound.sampled.*;

public class EchoMachine {
	
	public enum RecordType { Input, Output };
	public static final int BUFFER_LENGTH = 1000;
	public static final AudioFormat MONO_16BIT = new AudioFormat
		(AudioFormat.Encoding.PCM_SIGNED, EchoModel.SAMPLE_RATE, 16, 1, 2, EchoModel.SAMPLE_RATE, false);
	
	private TargetDataLine recordLine = null;
	private SourceDataLine playLine = null;
	private Queue<byte[]> toWrite = null;
	private boolean finished = false;
	private RecordType recordType = RecordType.Output;
	private StreamWriter currentWriter = null;
	private CompletionListener completionListener;	
	private EchoModel echo1;

	public EchoMachine(EchoModel echo1) {
		this.echo1 = echo1;
	}

	public void reconnect(LineSettings.LineSetting input, LineSettings.LineSetting output) {
		if (recordLine != null) {
			closeInput();
		}
		if (playLine != null) {
			closeOutput();	
		}
		try {
			openInput(input.getMixer());
			openOutput(output.getMixer());
		}
		catch(Exception e1) {
			e1.printStackTrace();
			exit();
		}
	}

	public void start() {
		readFromMicrophone();
		playThroughSpeakers();
	}

	private void closeInput() {
		try {
			recordLine.stop();
			recordLine.close();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	private void closeOutput() {
		try {
			playLine.stop();
			playLine.close();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public void exit() {
		closeInput();
		closeOutput();
		System.exit(1);
	}

	private void openInput(Mixer mixer) throws LineUnavailableException {
		DataLine.Info recordDataLine = new DataLine.Info(TargetDataLine.class, MONO_16BIT);
		recordLine = (TargetDataLine)mixer.getLine(recordDataLine);
		recordLine.open(MONO_16BIT);
		recordLine.start();
	}
	
	private void openOutput(Mixer mixer) throws LineUnavailableException {
		DataLine.Info playDataLine = new DataLine.Info(SourceDataLine.class, MONO_16BIT);
		playLine = (SourceDataLine)mixer.getLine(playDataLine);
 		playLine.open(MONO_16BIT);
 		playLine.start();
	}
	
	public void setCompletionListener(CompletionListener listener) {
		this.completionListener = listener;
	}
	
	public void setRecordType(RecordType recordType) {
		this.recordType = recordType;
	}
	
	private void readFromMicrophone() {
 		new Thread() {
			public void run() {
				while (!finished) {
	 		 		byte buffer[] = new byte[BUFFER_LENGTH];
	 		 		try {
	 		 			int count = recordLine.read(buffer, 0, buffer.length) / 2;
	 		 			for (int index=0; index<count; index++) {
	 		 				echo1.writeNext(buffer, index*2);
	 		 			}
	 		 		} catch (Exception e) {
	 		 			System.out.println(e);
	 		 			// coarse handle for recordLine being changed
	 		 		}
	 		 		if (toWrite != null && recordType == RecordType.Input) {
	 		 			toWrite.add(buffer);
					}
		        }
			}
		}.start();		
	}
	
	private void playThroughSpeakers() {
 		new Thread() {
 			public void run() {
		        while (!finished) {
		        	byte[] buffer = new byte[BUFFER_LENGTH];
		        	for (int index=0; index<buffer.length/2; index++) {
		        		double val1 = echo1.readNext();
		        		writeDataValue(buffer, index*2, val1);
		        	}
		        	try {
						playLine.write(buffer, 0, buffer.length);
						if (toWrite != null && recordType == RecordType.Output) {
				            toWrite.add(buffer);
						}
			        } catch (Exception e) {
	 		 			// coarse handle for playLine being changed
	 		 		}
		        }
			}
		}.start();		
	}

	public void startRecording(double length, String filename) {
 		new Thread() {
 			public void run() {
 				try {
 					toWrite = new LinkedList<>(); 					
	 				File f = new File(filename + ".wav");
	 				System.out.println(f.getAbsolutePath());
	 				currentWriter = new StreamWriter(toWrite, length);
					AudioSystem.write(currentWriter, AudioFileFormat.Type.WAVE, f);
	 				while (!currentWriter.isFinished()) {
	 					Thread.sleep(1000);
	 				}
	 				toWrite = null;
	 				if (completionListener != null) {
	 					completionListener.completed();
	 				}
	 				currentWriter = null;
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}.start();		
	}
	
	public void stopRecording() {
		if (currentWriter != null) {
			currentWriter.terminate();
		}
	}
	
	public boolean isRecording() {
		return currentWriter != null;
	}
	
	public void writeDataValue(byte[] data, int dataReadPos, double value) {		
		int limited = (int)Math.min(Math.max(value, -32768), 32767);
		data[dataReadPos] = (byte)(limited & 0xff);
		data[dataReadPos + 1] = (byte)((limited >>> 8) & 0xff);
	}
	
	public interface CompletionListener {
		public void completed();
	}
}
