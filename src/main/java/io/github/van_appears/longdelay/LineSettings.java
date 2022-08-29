package io.github.van_appears.longdelay;

import java.util.ArrayList;
import java.util.List;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;

public class LineSettings {

	private List<LineSetting> getLines(DataLine.Info required) {
		Mixer.Info[] mixers = AudioSystem.getMixerInfo();
		List<LineSetting> matching = new ArrayList<>();
		for (Mixer.Info info : mixers) {
			Mixer mixer = AudioSystem.getMixer(info);
			if (mixer.isLineSupported(required)) {
				matching.add(new LineSetting(mixer, info.getName()));
			};
		}
		return matching;
	}
	
	public List<LineSetting> getInputs() {
		DataLine.Info recordDataLine = new DataLine.Info(TargetDataLine.class, EchoMachine.MONO_16BIT);
		return getLines(recordDataLine);
	}

	public List<LineSetting> getOutputs() {
		DataLine.Info playDataLine = new DataLine.Info(SourceDataLine.class, EchoMachine.MONO_16BIT);
		return getLines(playDataLine);
	}
	
	public class LineSetting {
		private Mixer mixer;
		private String name;

		public LineSetting(Mixer mixer, String name) {
			this.mixer = mixer;
			this.name = name;
		}

		public Mixer getMixer() {
			return mixer;
		}

		public String toString() {
			return name;
		}
	}
}
