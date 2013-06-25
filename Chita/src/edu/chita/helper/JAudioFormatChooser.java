package edu.chita.helper;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.media.*;
import javax.media.format.*;

import com.sun.media.util.JMFI18N;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

public class JAudioFormatChooser extends JPanel implements ItemListener {

	public static final String ACTION_TRACK_ENABLED = "ACTION_AUDIO_TRACK_ENABLED";
	public static final String ACTION_TRACK_DISABLED = "ACTION_AUDIO_TRACK_DISABLED";
	private AudioFormat formatOld;
	private Format arrSupportedFormats[] = null;
	private Vector<Format> vectorContSuppFormats = new Vector<Format>();
	private boolean boolDisplayEnableTrack;
	private ActionListener listenerEnableTrack;
	private boolean boolEnableTrackSaved = true;
	private JCheckBox checkEnableTrack;
	private JLabel labelEncoding;
	private JComboBox comboEncoding;
	private JLabel labelSampleRate;
	private JComboBox comboSampleRate;
	private JLabel labelHz;
	private JLabel labelBitsPerSample;
	private ButtonGroup groupBitsPerSample;
	private JRadioButton checkBits8;
	private JRadioButton checkBits16;
	private JLabel labelChannels;
	private ButtonGroup groupChannels;
	private JRadioButton checkMono;
	private JRadioButton checkStereo;
	private JLabel labelEndian;
	private ButtonGroup groupEndian;
	private JRadioButton checkEndianBig;
	private JRadioButton checkEndianLittle;
	private JCheckBox checkSigned;
	private boolean boolEnable8 = false;
	private boolean boolEnable16 = false;
	private boolean boolEnableMono = false;
	private boolean boolEnableStereo = false;
	private boolean boolEnableEndianBig = false;
	private boolean boolEnableEndianLittle = false;
	private boolean boolEnableSigned = false;

	public JAudioFormatChooser(Format arrFormats[], AudioFormat formatDefault) {
		this(arrFormats, formatDefault, false, null);
	}

	public JAudioFormatChooser(Format arrFormats[], AudioFormat formatDefault,
			boolean boolDisplayEnableTrack, ActionListener listenerEnableTrack) {
		int i;
		int nCount;

		this.arrSupportedFormats = arrFormats;
		this.boolDisplayEnableTrack = boolDisplayEnableTrack;
		this.listenerEnableTrack = listenerEnableTrack;

		nCount = arrSupportedFormats.length;
		for (i = 0; i < nCount; i++) {
			if (arrSupportedFormats[i] instanceof AudioFormat) {
				vectorContSuppFormats.addElement(arrSupportedFormats[i]);
			}
		}

		if (isFormatSupported(formatDefault)) {
			this.formatOld = formatDefault;
		} else {
			this.formatOld = null;
		}

		try {
			init();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void setEnabled(boolean boolEnable) {
		super.setEnabled(boolEnable);

		if (checkEnableTrack != null) {
			checkEnableTrack.setEnabled(boolEnable);
		}
		enableControls(boolEnable);
	}

	public Format getFormat() {
		int i;
		int nSize;
		String strEncoding;
		double dSampleRate;
		String strSampleRate;
		int nBits;
		int nChannels;
		int nEndian;
		int nSigned;
		Format formatResult = null;
		AudioFormat formatAudioNew;
		AudioFormat formatAudio;
		Object objectFormat;

		strEncoding = (String) comboEncoding.getSelectedItem();
		strSampleRate = (String) comboSampleRate.getSelectedItem();
		dSampleRate = Double.valueOf(strSampleRate).doubleValue();

		if (checkBits8.isSelected() == true && checkBits8.isEnabled() == true) {
			nBits = 8;
		} else if (checkBits16.isSelected() == true
				&& checkBits16.isEnabled() == true) {
			nBits = 16;
		} else {
			nBits = Format.NOT_SPECIFIED;
		}

		if (checkMono.isSelected() == true && checkMono.isEnabled() == true) {
			nChannels = 1;
		} else if (checkStereo.isSelected() == true
				&& checkStereo.isEnabled() == true) {
			nChannels = 2;
		} else {
			nChannels = Format.NOT_SPECIFIED;
		}

		if (checkEndianBig.isSelected() == true
				&& checkEndianBig.isEnabled() == true) {
			nEndian = AudioFormat.BIG_ENDIAN;
		} else if (checkEndianLittle.isSelected() == true
				&& checkEndianLittle.isEnabled() == true) {
			nEndian = AudioFormat.LITTLE_ENDIAN;
		} else {
			nEndian = Format.NOT_SPECIFIED;
		}

		if (checkSigned.isSelected() == true) {
			nSigned = AudioFormat.SIGNED;
		} else {
			nSigned = AudioFormat.UNSIGNED;
		}

		formatAudioNew = new AudioFormat(strEncoding, dSampleRate, nBits,
				nChannels, nEndian, nSigned);

		nSize = vectorContSuppFormats.size();
		for (i = 0; i < nSize && formatResult == null; i++) {
			objectFormat = vectorContSuppFormats.elementAt(i);
			if (!(objectFormat instanceof AudioFormat)) {
				continue;
			}
			formatAudio = (AudioFormat) objectFormat;

			if (!this.isFormatGoodForEncoding(formatAudio)) {
				continue;
			}
			if (!this.isFormatGoodForSampleRate(formatAudio)) {
				continue;
			}
			if (!this.isFormatGoodForBitSize(formatAudio)) {
				continue;
			}
			if (!this.isFormatGoodForChannels(formatAudio)) {
				continue;
			}
			if (!this.isFormatGoodForEndian(formatAudio)) {
				continue;
			}
			if (!this.isFormatGoodForSigned(formatAudio)) {
				continue;
			}

			if (formatAudio.matches(formatAudioNew)) {
				formatResult = formatAudio.intersects(formatAudioNew);
			}
		}

		return (formatResult);
	}

	public void setCurrentFormat(AudioFormat formatDefault) {
		if (isFormatSupported(formatDefault)) {
			this.formatOld = formatDefault;
		}
		updateFields(formatOld);
	}

	public void setSupportedFormats(Format arrFormats[],
			AudioFormat formatDefault) {
		int i;
		int nCount;

		this.arrSupportedFormats = arrFormats;

		nCount = arrSupportedFormats.length;
		vectorContSuppFormats.removeAllElements();
		for (i = 0; i < nCount; i++) {
			if (arrSupportedFormats[i] instanceof AudioFormat) {
				vectorContSuppFormats.addElement(arrSupportedFormats[i]);
			}
		}
		if (isFormatSupported(formatDefault)) {
			this.formatOld = formatDefault;
		} else {
			this.formatOld = null;
		}
		setSupportedFormats(vectorContSuppFormats);
	}

	public void setSupportedFormats(Vector<Format> vectorContSuppFormats) {
		this.vectorContSuppFormats = vectorContSuppFormats;

		if (vectorContSuppFormats.isEmpty()) {
			checkEnableTrack.setSelected(false);
			checkEnableTrack.setEnabled(false);
			onEnableTrack(true);
			return;
		} else {
			checkEnableTrack.setEnabled(true);
			checkEnableTrack.setSelected(boolEnableTrackSaved);
			onEnableTrack(true);
		}

		if (!isFormatSupported(this.formatOld)) {
			this.formatOld = null;
		}

		updateFields(formatOld);
	}

	public void setTrackEnabled(boolean boolEnable) {
		boolEnableTrackSaved = boolEnable;
		if (checkEnableTrack == null) {
			return;
		}
		checkEnableTrack.setSelected(boolEnable);
		onEnableTrack(true);
	}

	public boolean isTrackEnabled() {
		boolean boolEnabled;

		boolEnabled = checkEnableTrack.isSelected();
		return (boolEnabled);
	}

	private void init() throws Exception {
		JPanel panel;
		JPanel panelGroup;
		JPanel panelLabel;
		JPanel panelData;
		JPanel panelEntry;

		this.setLayout(new BorderLayout(6, 6));
		panel = this;

		checkEnableTrack = new JCheckBox("启用通道", true);
		checkEnableTrack.addItemListener(this);
		if (boolDisplayEnableTrack == true) {
			panelGroup = new JPanel(new BorderLayout());
			panel.add(panelGroup, BorderLayout.NORTH);
			panelGroup.add(checkEnableTrack, BorderLayout.WEST);
		}

		panelGroup = new JPanel(new BorderLayout(6, 6));
		panel.add(panelGroup, BorderLayout.CENTER);
		panel = panelGroup;
		panelGroup = new JPanel(new BorderLayout());
		panel.add(panelGroup, BorderLayout.NORTH);

		panelLabel = new JPanel(new GridLayout(0, 1, 6, 6));
		panelGroup.add(panelLabel, BorderLayout.WEST);
		panelData = new JPanel(new GridLayout(0, 1, 6, 6));
		panelGroup.add(panelData, BorderLayout.CENTER);

		labelEncoding = new JLabel("编码", Label.LEFT);
		panelLabel.add(labelEncoding);
		comboEncoding = new JComboBox();
		comboEncoding.addItemListener(this);
		panelData.add(comboEncoding);

		labelSampleRate = new JLabel("采样率", Label.LEFT);
		panelLabel.add(labelSampleRate);
		panelEntry = new JPanel(new BorderLayout(6, 6));
		panelData.add(panelEntry);
		comboSampleRate = new JComboBox();
		comboSampleRate.addItemListener(this);
		panelEntry.add(comboSampleRate, BorderLayout.CENTER);
		labelHz = new JLabel(JMFI18N.getResource("formatchooser.hz"));
		panelEntry.add(labelHz, BorderLayout.EAST);

		labelBitsPerSample = new JLabel("采样率", Label.LEFT);
		panelLabel.add(labelBitsPerSample);
		panelEntry = new JPanel(new GridLayout(1, 0, 6, 6));
		panelData.add(panelEntry);
		groupBitsPerSample = new ButtonGroup();
		checkBits8 = new JRadioButton(
				JMFI18N.getResource("formatchooser.8bit"), false);
		checkBits8.addItemListener(this);
		groupBitsPerSample.add(checkBits8);
		panelEntry.add(checkBits8);
		checkBits16 = new JRadioButton(
				JMFI18N.getResource("formatchooser.16bit"), false);
		checkBits16.addItemListener(this);
		groupBitsPerSample.add(checkBits16);
		panelEntry.add(checkBits16);

		labelChannels = new JLabel("通道", Label.LEFT);
		panelLabel.add(labelChannels);
		panelEntry = new JPanel(new GridLayout(1, 0, 6, 6));
		panelData.add(panelEntry);
		groupChannels = new ButtonGroup();
		checkMono = new JRadioButton("单声道", false);
		checkMono.addItemListener(this);
		groupChannels.add(checkMono);
		panelEntry.add(checkMono);
		checkStereo = new JRadioButton("立体声", false);
		checkStereo.addItemListener(this);
		groupChannels.add(checkStereo);
		panelEntry.add(checkStereo);

		labelEndian = new JLabel("端", Label.LEFT);
		panelLabel.add(labelEndian);
		panelEntry = new JPanel(new GridLayout(1, 0, 6, 6));
		panelData.add(panelEntry);
		groupEndian = new ButtonGroup();
		checkEndianBig = new JRadioButton("大端", false);
		checkEndianBig.addItemListener(this);
		groupEndian.add(checkEndianBig);
		panelEntry.add(checkEndianBig);
		checkEndianLittle = new JRadioButton("小端", false);
		checkEndianLittle.addItemListener(this);
		groupEndian.add(checkEndianLittle);
		panelEntry.add(checkEndianLittle);

		panelGroup = new JPanel(new BorderLayout(6, 6));
		panel.add(panelGroup, BorderLayout.CENTER);
		panel = panelGroup;
		panelGroup = new JPanel(new BorderLayout());
		panel.add(panelGroup, BorderLayout.NORTH);

		checkSigned = new JCheckBox("签名", true);
		checkSigned.addItemListener(this);
		panelGroup.add(checkSigned, BorderLayout.WEST);

		updateFields(formatOld);
	}

	private void updateFields(AudioFormat formatDefault) {
		int i;
		int nSize;
		String strEncoding;
		String strEncodingPref = null;
		Object objectFormat;
		AudioFormat formatAudio;
		Vector<String> vectorEncoding = new Vector<String>();
		boolean boolEnable;

		boolEnable = comboEncoding.isEnabled();
		comboEncoding.setEnabled(false);
		comboEncoding.removeAll();

		nSize = vectorContSuppFormats.size();
		for (i = 0; i < nSize; i++) {
			objectFormat = vectorContSuppFormats.elementAt(i);
			if (!(objectFormat instanceof AudioFormat)) {
				continue;
			}
			formatAudio = (AudioFormat) objectFormat;

			strEncoding = formatAudio.getEncoding().toUpperCase();
			if (vectorEncoding.contains(strEncoding)) {
				continue;
			}
			comboEncoding.addItem(strEncoding);
			vectorEncoding.addElement(strEncoding);
			if (strEncodingPref == null) {
				strEncodingPref = strEncoding;
			}
		}

		if (formatDefault != null) {
			strEncoding = formatDefault.getEncoding();
			comboEncoding.setSelectedItem(strEncoding);
		} else if (strEncodingPref != null) {
			comboEncoding.setSelectedItem(strEncodingPref);
		} else if (comboEncoding.getItemCount() > 0) {
			comboEncoding.setSelectedIndex(0);
		}

		updateFieldsFromEncoding(formatDefault);
		comboEncoding.setEnabled(boolEnable);
	}

	private void updateFieldsFromEncoding(AudioFormat formatDefault) {
		int i;
		int nSize;
		double dSampleRate;
		String strSampleRate;
		String strSampleRatePref = null;
		Object objectFormat;
		AudioFormat formatAudio;
		Vector<String> vectorRates = new Vector<String>();
		boolean boolEnable;

		boolEnable = comboSampleRate.isEnabled();
		comboSampleRate.setEnabled(false);
		comboSampleRate.removeAll();

		nSize = vectorContSuppFormats.size();
		for (i = 0; i < nSize; i++) {
			objectFormat = vectorContSuppFormats.elementAt(i);
			if (!(objectFormat instanceof AudioFormat)) {
				continue;
			}
			formatAudio = (AudioFormat) objectFormat;
			if (!isFormatGoodForEncoding(formatAudio)) {
				continue;
			}

			dSampleRate = formatAudio.getSampleRate();
			strSampleRate = Double.toString(dSampleRate);
			if (vectorRates.contains(strSampleRate)) {
				continue;
			}
			comboSampleRate.addItem(strSampleRate);
			vectorRates.addElement(strSampleRate);
			if (strSampleRatePref == null) {
				strSampleRatePref = strSampleRate;
			}
		}
		if (formatDefault != null && isFormatGoodForEncoding(formatDefault)) {
			comboSampleRate.setSelectedItem(Double.toString(formatDefault
					.getSampleRate()));
		} else if (strSampleRatePref != null) {
			comboEncoding.setSelectedItem(strSampleRatePref);
		} else if (comboSampleRate.getItemCount() > 0) {
			comboSampleRate.setSelectedIndex(0);
		}

		updateFieldsFromRate(formatDefault);
		comboSampleRate.setEnabled(boolEnable);
	}

	private void updateFieldsFromRate(AudioFormat formatDefault) {
		int i;
		int nSize;
		Object objectFormat;
		AudioFormat formatAudio;
		int nBits;
		int nBitsPref = Format.NOT_SPECIFIED;

		boolEnable8 = false;
		boolEnable16 = false;
		nSize = vectorContSuppFormats.size();
		for (i = 0; i < nSize; i++) {
			objectFormat = vectorContSuppFormats.elementAt(i);
			if (!(objectFormat instanceof AudioFormat)) {
				continue;
			}
			formatAudio = (AudioFormat) objectFormat;
			if (!this.isFormatGoodForEncoding(formatAudio)) {
				continue;
			}
			if (!this.isFormatGoodForSampleRate(formatAudio)) {
				continue;
			}

			nBits = formatAudio.getSampleSizeInBits();
			if (nBitsPref == Format.NOT_SPECIFIED) {
				nBitsPref = nBits;
			}

			if (nBits == Format.NOT_SPECIFIED) {
				boolEnable8 = true;
				boolEnable16 = true;
			} else if (nBits == 8) {
				boolEnable8 = true;
			} else if (nBits == 16) {
				boolEnable16 = true;
			}

		}
		checkBits8.setEnabled(boolEnable8);
		checkBits16.setEnabled(boolEnable16);

		if (formatDefault != null
				&& this.isFormatGoodForEncoding(formatDefault)
				&& this.isFormatGoodForSampleRate(formatDefault)) {
			nBits = formatDefault.getSampleSizeInBits();
			if (nBits == 8) {
				checkBits8.setSelected(true);
			} else if (nBits == 16) {
				checkBits16.setSelected(true);
			}
		} else if (nBitsPref != Format.NOT_SPECIFIED) {
			if (nBitsPref == 8) {
				checkBits8.setSelected(true);
			} else if (nBitsPref == 16) {
				checkBits16.setSelected(true);
			}
		} else {
			if (boolEnable8 == true) {
				checkBits8.setSelected(true);
			} else {
				checkBits16.setSelected(true);
			}
		}

		updateFieldsFromBits(formatDefault);
	}

	private void updateFieldsFromBits(AudioFormat formatDefault) {
		int i;
		int nSize;
		Object objectFormat;
		AudioFormat formatAudio;
		int nChannels;
		int nChannelsPref = Format.NOT_SPECIFIED;

		boolEnableMono = false;
		boolEnableStereo = false;

		nSize = vectorContSuppFormats.size();
		for (i = 0; i < nSize; i++) {
			objectFormat = vectorContSuppFormats.elementAt(i);
			if (!(objectFormat instanceof AudioFormat)) {
				continue;
			}
			formatAudio = (AudioFormat) objectFormat;
			if (!this.isFormatGoodForEncoding(formatAudio)) {
				continue;
			}
			if (!this.isFormatGoodForSampleRate(formatAudio)) {
				continue;
			}
			if (!this.isFormatGoodForBitSize(formatAudio)) {
				continue;
			}

			nChannels = formatAudio.getChannels();
			if (nChannelsPref == Format.NOT_SPECIFIED) {
				nChannelsPref = nChannels;
			}

			if (nChannels == Format.NOT_SPECIFIED) {
				boolEnableMono = true;
				boolEnableStereo = true;
			} else if (nChannels == 1) {
				boolEnableMono = true;
			} else {
				boolEnableStereo = true;
			}

		}
		checkMono.setEnabled(boolEnableMono);
		checkStereo.setEnabled(boolEnableStereo);

		if (formatDefault != null
				&& this.isFormatGoodForEncoding(formatDefault)
				&& this.isFormatGoodForSampleRate(formatDefault)
				&& this.isFormatGoodForBitSize(formatDefault)) {
			nChannels = formatDefault.getChannels();
			if (nChannels == 1) {
				checkMono.setSelected(true);
			} else {
				checkStereo.setSelected(true);
			}
		} else if (nChannelsPref != Format.NOT_SPECIFIED) {
			if (nChannelsPref == 1) {
				checkMono.setSelected(true);
			} else {
				checkStereo.setSelected(true);
			}
		} else {
			if (boolEnableMono == true) {
				checkMono.setSelected(true);
			} else {
				checkStereo.setSelected(true);
			}
		}

		updateFieldsFromChannels(formatDefault);
	}

	private void updateFieldsFromChannels(AudioFormat formatDefault) {
		int i;
		int nSize;
		Object objectFormat;
		AudioFormat formatAudio;
		int nEndian;
		int nEndianPref = Format.NOT_SPECIFIED;

		boolEnableEndianBig = false;
		boolEnableEndianLittle = false;

		nSize = vectorContSuppFormats.size();
		for (i = 0; i < nSize; i++) {
			objectFormat = vectorContSuppFormats.elementAt(i);
			if (!(objectFormat instanceof AudioFormat)) {
				continue;
			}
			formatAudio = (AudioFormat) objectFormat;
			if (!this.isFormatGoodForEncoding(formatAudio)) {
				continue;
			}
			if (!this.isFormatGoodForSampleRate(formatAudio)) {
				continue;
			}
			if (!this.isFormatGoodForBitSize(formatAudio)) {
				continue;
			}
			if (!this.isFormatGoodForChannels(formatAudio)) {
				continue;
			}

			nEndian = formatAudio.getEndian();
			if (nEndianPref == Format.NOT_SPECIFIED) {
				nEndianPref = nEndian;
			}

			if (nEndian == Format.NOT_SPECIFIED) {
				boolEnableEndianBig = true;
				boolEnableEndianLittle = true;
			} else if (nEndian == AudioFormat.BIG_ENDIAN) {
				boolEnableEndianBig = true;
			} else {
				boolEnableEndianLittle = true;
			}

		}

		checkEndianBig.setEnabled(boolEnableEndianBig);
		checkEndianLittle.setEnabled(boolEnableEndianLittle);

		if (formatDefault != null
				&& this.isFormatGoodForEncoding(formatDefault)
				&& this.isFormatGoodForSampleRate(formatDefault)
				&& this.isFormatGoodForBitSize(formatDefault)
				&& this.isFormatGoodForChannels(formatDefault)) {
			nEndian = formatDefault.getEndian();
			if (nEndian == AudioFormat.BIG_ENDIAN) {
				checkEndianBig.setSelected(true);
			} else {
				checkEndianLittle.setSelected(true);
			}
		} else if (nEndianPref != Format.NOT_SPECIFIED) {
			if (nEndianPref == AudioFormat.BIG_ENDIAN) {
				checkEndianBig.setSelected(true);
			} else {
				checkEndianLittle.setSelected(true);
			}
		} else {
			if (boolEnableEndianBig == true) {
				checkEndianBig.setSelected(true);
			} else {
				checkEndianLittle.setSelected(true);
			}
		}

		if (checkBits16.isSelected() != true) {
			boolEnableEndianBig = false;
			boolEnableEndianLittle = false;
			checkEndianBig.setEnabled(boolEnableEndianBig);
			checkEndianLittle.setEnabled(boolEnableEndianLittle);
		}

		updateFieldsFromEndian(formatDefault);
	}

	private void updateFieldsFromEndian(AudioFormat formatDefault) {
		int i;
		int nSize;
		Object objectFormat;
		AudioFormat formatAudio;
		int nSigned;
		int nSignedPref = Format.NOT_SPECIFIED;
		boolean boolSigned;
		boolean boolUnsigned;

		boolSigned = false;
		boolUnsigned = false;

		nSize = vectorContSuppFormats.size();
		for (i = 0; i < nSize; i++) {
			objectFormat = vectorContSuppFormats.elementAt(i);
			if (!(objectFormat instanceof AudioFormat)) {
				continue;
			}
			formatAudio = (AudioFormat) objectFormat;
			if (!this.isFormatGoodForEncoding(formatAudio)) {
				continue;
			}
			if (!this.isFormatGoodForSampleRate(formatAudio)) {
				continue;
			}
			if (!this.isFormatGoodForBitSize(formatAudio)) {
				continue;
			}
			if (!this.isFormatGoodForChannels(formatAudio)) {
				continue;
			}
			if (!this.isFormatGoodForEndian(formatAudio)) {
				continue;
			}

			nSigned = formatAudio.getSigned();
			if (nSignedPref == Format.NOT_SPECIFIED) {
				nSignedPref = nSigned;
			}

			if (nSigned == Format.NOT_SPECIFIED) {
				boolSigned = true;
				boolUnsigned = true;
			} else if (nSigned == AudioFormat.SIGNED) {
				boolSigned = true;
			} else {
				boolUnsigned = true;
			}

		}
		boolEnableSigned = boolSigned && boolUnsigned;
		checkSigned.setEnabled(boolEnableSigned);

		if (formatDefault != null
				&& this.isFormatGoodForEncoding(formatDefault)
				&& this.isFormatGoodForSampleRate(formatDefault)
				&& this.isFormatGoodForBitSize(formatDefault)
				&& this.isFormatGoodForChannels(formatDefault)
				&& this.isFormatGoodForEndian(formatDefault)) {
			nSigned = formatDefault.getSigned();
			if (nSigned == AudioFormat.SIGNED) {
				checkSigned.setSelected(true);
			} else {
				checkSigned.setSelected(false);
			}
		} else if (nSignedPref != Format.NOT_SPECIFIED) {
			if (nSignedPref == AudioFormat.SIGNED) {
				checkSigned.setSelected(true);
			} else {
				checkSigned.setSelected(false);
			}
		} else {
			if (boolSigned == true) {
				checkSigned.setSelected(true);
			} else {
				checkSigned.setSelected(false);
			}
		}

		updateFieldsFromSigned(formatDefault);
	}

	private void updateFieldsFromSigned(AudioFormat formatDefault) {
	}

	private boolean isFormatGoodForEncoding(AudioFormat format) {
		String strEncoding;
		boolean boolResult = false;

		strEncoding = (String) comboEncoding.getSelectedItem();
		if (strEncoding != null) {
			boolResult = format.getEncoding().equalsIgnoreCase(strEncoding);
		}
		return (boolResult);
	}

	private boolean isFormatGoodForSampleRate(AudioFormat format) {
		double dSampleRate;
		String strSampleRate;
		boolean boolResult = false;

		strSampleRate = (String) comboSampleRate.getSelectedItem();
		if (strSampleRate != null) {
			dSampleRate = Double.valueOf(strSampleRate).doubleValue();
			if (format.getSampleRate() == Format.NOT_SPECIFIED) {
				boolResult = true;
			} else if (format.getSampleRate() == dSampleRate) {
				boolResult = true;
			}
		}
		return (boolResult);
	}

	private boolean isFormatGoodForBitSize(AudioFormat format) {
		int nBits;
		boolean boolResult = false;

		if (checkBits8.isSelected() == true) {
			nBits = 8;
		} else if (checkBits16.isSelected() == true) {
			nBits = 16;
		} else {
			nBits = Format.NOT_SPECIFIED;
		}

		if (format.getSampleSizeInBits() == Format.NOT_SPECIFIED) {
			boolResult = true;
		} else if (nBits == Format.NOT_SPECIFIED) {
			boolResult = true;
		} else if (format.getSampleSizeInBits() == nBits) {
			boolResult = true;
		} else if (format.getSampleSizeInBits() < 8) 
														
		{
			boolResult = true;
		}

		return (boolResult);
	}

	private boolean isFormatGoodForChannels(AudioFormat format) {
		int nChannels;
		boolean boolResult = false;

		if (checkMono.isSelected() == true) {
			nChannels = 1;
		} else if (checkStereo.isSelected() == true) {
			nChannels = 2;
		} else {
			nChannels = Format.NOT_SPECIFIED;
		}

		if (format.getChannels() == Format.NOT_SPECIFIED) {
			boolResult = true;
		} else if (nChannels == Format.NOT_SPECIFIED) {
			boolResult = true;
		} else if (format.getChannels() == nChannels) {
			boolResult = true;
		}

		return (boolResult);
	}

	private boolean isFormatGoodForEndian(AudioFormat format) {
		int nEndian;
		boolean boolResult = false;

		if (checkEndianBig.isSelected() == true) {
			nEndian = AudioFormat.BIG_ENDIAN;
		} else if (checkStereo.isSelected() == true) {
			nEndian = AudioFormat.LITTLE_ENDIAN;
		} else {
			nEndian = Format.NOT_SPECIFIED;
		}

		if (format.getEndian() == Format.NOT_SPECIFIED) {
			boolResult = true;
		} else if (nEndian == Format.NOT_SPECIFIED) {
			boolResult = true;
		} else if (format.getEndian() == nEndian) {
			boolResult = true;
		}

		return (boolResult);
	}

	private boolean isFormatGoodForSigned(AudioFormat format) {
		int nSigned;
		boolean boolResult = false;

		if (checkSigned.isSelected() == true) {
			nSigned = AudioFormat.SIGNED;
		} else {
			nSigned = AudioFormat.UNSIGNED;
		}

		if (format.getSigned() == Format.NOT_SPECIFIED) {
			boolResult = true;
		} else if (nSigned == Format.NOT_SPECIFIED) {
			boolResult = true;
		} else if (format.getSigned() == nSigned) {
			boolResult = true;
		}

		return (boolResult);
	}

	private boolean isFormatSupported(AudioFormat format) {
		int i;
		int nCount;
		AudioFormat formatAudio;
		boolean boolSupported = false;

		if (format == null) {
			return (boolSupported);
		}

		nCount = vectorContSuppFormats.size();
		for (i = 0; i < nCount && boolSupported == false; i++) {
			formatAudio = (AudioFormat) vectorContSuppFormats.elementAt(i);
			if (formatAudio.matches(format)) {
				boolSupported = true;
			}
		}
		return (boolSupported);
	}

	public void itemStateChanged(ItemEvent event) {
		Object objectSource;

		objectSource = event.getSource();
		if (objectSource == checkEnableTrack) {
			boolEnableTrackSaved = checkEnableTrack.isSelected();
			onEnableTrack(true);
		} else if (objectSource == comboEncoding) {
			updateFieldsFromEncoding(formatOld);
		} else if (objectSource == comboSampleRate) {
			updateFieldsFromRate(formatOld);
		} else if (objectSource == checkBits8 || objectSource == checkBits16) {
			updateFieldsFromBits(formatOld);
		} else if (objectSource == checkMono || objectSource == checkStereo) {
			updateFieldsFromChannels(formatOld);
		} else if (objectSource == checkEndianBig
				|| objectSource == checkEndianLittle) {
			updateFieldsFromEndian(formatOld);
		} else if (objectSource == checkSigned) {
			updateFieldsFromSigned(formatOld);
		}
	}

	private void onEnableTrack(boolean notifyListener) {
		boolean boolEnable;
		ActionEvent event;

		boolEnable = checkEnableTrack.isSelected();
		enableControls(boolEnable && this.isEnabled());

		if (notifyListener == true && listenerEnableTrack != null) {
			if (boolEnable == true) {
				event = new ActionEvent(this, ActionEvent.ACTION_PERFORMED,
						ACTION_TRACK_ENABLED);
			} else {
				event = new ActionEvent(this, ActionEvent.ACTION_PERFORMED,
						ACTION_TRACK_DISABLED);
			}
			listenerEnableTrack.actionPerformed(event);
		}
	}

	private void enableControls(boolean boolEnable) {
		labelEncoding.setEnabled(boolEnable);
		comboEncoding.setEnabled(boolEnable);
		labelSampleRate.setEnabled(boolEnable);
		comboSampleRate.setEnabled(boolEnable);
		labelHz.setEnabled(boolEnable);
		labelBitsPerSample.setEnabled(boolEnable);
		checkBits8.setEnabled(boolEnable && boolEnable8);
		checkBits16.setEnabled(boolEnable && boolEnable16);
		labelChannels.setEnabled(boolEnable);
		checkMono.setEnabled(boolEnable && boolEnableMono);
		checkStereo.setEnabled(boolEnable && boolEnableStereo);
		labelEndian.setEnabled(boolEnable);
		checkEndianBig.setEnabled(boolEnable && boolEnableEndianBig);
		checkEndianLittle.setEnabled(boolEnable && boolEnableEndianLittle);
		checkSigned.setEnabled(boolEnable && boolEnableSigned);
	}
    
}
