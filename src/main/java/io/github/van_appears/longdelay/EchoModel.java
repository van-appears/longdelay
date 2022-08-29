package io.github.van_appears.longdelay;

public class EchoModel {
	public static final int READ_OFFSET = 4410;
	public static final int SAMPLE_RATE = 44100;
	public static final double MIN_LENGTH_SECONDS = 1.0;
	public static final double MAX_LENGTH_SECONDS = 60.0;
	
	private double[] data = new double[(int)(SAMPLE_RATE * MAX_LENGTH_SECONDS)];
	private int frameLength = 441000;
	public int readPos = READ_OFFSET; // readPos most be ahead of writePos 
	public int writePos = 0;
	
	public void setFrameLength(int length) {
		if (readPos > length) {
			readPos = READ_OFFSET;
		}
		if (writePos > length) {
			writePos = 0;
		}
		this.frameLength = length;
	}

	public void clear() {
		data = new double[(int)(SAMPLE_RATE * MAX_LENGTH_SECONDS)];
	}

	public void writeNext(byte[] buffer, int bufferPos) {
		int value = (int)buffer[bufferPos + 1] * 256;
		value = value + ((256 + (int)buffer[bufferPos]) % 256);
		data[writePos] = (int)Math.min(Math.max(value, -32768), 32767);
		writePos = (writePos + 1) % frameLength; 
	}
	
	public double readNext() {
		double val = data[readPos];
		readPos = (readPos + 1) % frameLength;		    
		return val;
	}
}
