package io.github.van_appears.longdelay;

import java.io.ByteArrayInputStream;
import java.util.Queue;
import javax.sound.sampled.AudioInputStream;

public class StreamWriter extends AudioInputStream {

	public static final double MAX_LENGTH_SECONDS = 600.0;
	
	private long remainingFrames;
	private Queue<byte[]> queue;
	private byte[] current;
	private int currentPos;
	private boolean terminated = false;

	private static int asFrames(double seconds) {
		return EchoMachine.MONO_16BIT.getFrameSize() *
			(int)(seconds * EchoMachine.MONO_16BIT.getFrameRate());
	}
	
	public StreamWriter(Queue<byte[]> queue, double seconds) {
		super(new ByteArrayInputStream(new byte[0]),
		    EchoMachine.MONO_16BIT, asFrames(seconds));
		this.remainingFrames = asFrames(seconds);
		this.queue = queue;
	}
	
	public void terminate() {
		terminated = true;
	}

	@Override
	public int read(byte[] bytes, int offset, int len) {
		if (current == null) {
			while (queue.size() == 0 && !terminated) {
				try {
					Thread.sleep(200);
				} catch (Exception e) {}
			}
			current = queue.poll();
			currentPos = 0;
		}
		if (remainingFrames <= 0 || terminated) {
			return -1;
		}
		int written = 0;
		for (int index=0; index<len; index++) {
			bytes[offset] = current[currentPos];  
			offset++;
			written++;
			currentPos++;
			remainingFrames--;
			if (currentPos >= current.length) {
				if (queue.size() > 0) {
					current = queue.poll();
					currentPos = 0;
				} else {
					current = null;
					break;
				}
			}
		}
		return written;
	}

	public boolean isFinished() {
		return terminated || remainingFrames <= 0;
	}	
}
