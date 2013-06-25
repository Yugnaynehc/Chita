package edu.chita.helper;
import com.sun.media.util.JMFI18N;
import java.awt.BorderLayout;
import java.awt.Choice;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Hashtable;
import java.util.Vector;
import javax.media.Format;
import javax.media.format.RGBFormat;
import javax.media.format.VideoFormat;
import javax.media.format.YUVFormat;
import javax.swing.*;

public class JVideoFormatChooser extends JPanel implements ItemListener,
		ActionListener {

	public static final String ACTION_TRACK_ENABLED = "ACTION_VIDEO_TRACK_ENABLED";
	public static final String ACTION_TRACK_DISABLED = "ACTION_VIDEO_TRACK_DISABLED";
	private VideoFormat formatOld;
	private Format arrSupportedFormats[] = null;
	private float customFrameRates[] = null;
	private Vector<Format> vectorContSuppFormats = new Vector<Format>();
	private boolean boolDisplayEnableTrack;
	private ActionListener listenerEnableTrack;
	private boolean boolEnableTrackSaved = true;
	private JCheckBox checkEnableTrack;
	private JLabel labelEncoding;
	private JComboBox comboEncoding;
	private JLabel labelSize;
	private VideoSizeControl controlSize;
	private JLabel labelFrameRate;
	private JComboBox comboFrameRate;
	private JLabel labelExtra;
	private JComboBox comboExtra;
	private int nWidthLabel = 0;
	private int nWidthData = 0;
	private static final int MARGINH = 12;
	private static final int MARGINV = 6;
	private static final float[] standardCaptureRates = new float[] { 15f, 1f,
			2f, 5f, 7.5f, 10f, 12.5f, 20f, 24f, 25f, 30f };
	private static final String DEFAULT_STRING = JMFI18N
			.getResource("formatchooser.default");

	public JVideoFormatChooser(Format arrFormats[], VideoFormat formatDefault,
			float[] frameRates) {
		this(arrFormats, formatDefault, false, null, frameRates);
	}

	public JVideoFormatChooser(Format arrFormats[], VideoFormat formatDefault) {
		this(arrFormats, formatDefault, false, null, null);
	}

	public JVideoFormatChooser(Format arrFormats[], VideoFormat formatDefault,
			boolean boolDisplayEnableTrack, ActionListener listenerEnableTrack) {
		this(arrFormats, formatDefault, boolDisplayEnableTrack,
				listenerEnableTrack, null);
	}

	public JVideoFormatChooser(Format arrFormats[], VideoFormat formatDefault,
			boolean boolDisplayEnableTrack, ActionListener listenerEnableTrack,
			boolean capture) {
		this(arrFormats, formatDefault, boolDisplayEnableTrack,
				listenerEnableTrack, capture ? standardCaptureRates : null);
	}

	public JVideoFormatChooser(Format arrFormats[], VideoFormat formatDefault,
			boolean boolDisplayEnableTrack, ActionListener listenerEnableTrack,
			float[] frameRates) {
		int i;
		int nCount;

		this.arrSupportedFormats = arrFormats;
		this.boolDisplayEnableTrack = boolDisplayEnableTrack;
		this.listenerEnableTrack = listenerEnableTrack;
		this.customFrameRates = frameRates;

		nCount = arrSupportedFormats.length;
		for (i = 0; i < nCount; i++) {
			if (arrSupportedFormats[i] instanceof VideoFormat) {
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
		Integer integerBitsPerPixel;
		String strBitsPerPixel;
		int nYuvType;
		String strYuvType = null;
		Object objectFormat;
		VideoFormat formatVideo = null;
		VideoFormat formatVideoNew;
		RGBFormat formatRGB;
		YUVFormat formatYUV;

		strEncoding = (String) comboEncoding.getSelectedItem();

		nSize = vectorContSuppFormats.size();
		for (i = 0; i < nSize; i++) {
			objectFormat = vectorContSuppFormats.elementAt(i);
			if (!(objectFormat instanceof VideoFormat)) {
				continue;
			}
			formatVideo = (VideoFormat) objectFormat;

			if (!this.isFormatGoodForEncoding(formatVideo)) {
				continue;
			}
			if (!this.isFormatGoodForVideoSize(formatVideo)) {
				continue;
			}
			if (!this.isFormatGoodForFrameRate(formatVideo)) {
				continue;
			}

			if (strEncoding.equalsIgnoreCase(VideoFormat.RGB)
					&& formatVideo instanceof RGBFormat) {
				formatRGB = (RGBFormat) formatVideo;
				integerBitsPerPixel = new Integer(formatRGB.getBitsPerPixel());
				strBitsPerPixel = integerBitsPerPixel.toString();
				if (!(comboExtra.getSelectedItem().equals(strBitsPerPixel))) {
					continue;
				}
			} else if (strEncoding.equalsIgnoreCase(VideoFormat.YUV)
					&& formatVideo instanceof YUVFormat) {
				formatYUV = (YUVFormat) formatVideo;
				nYuvType = formatYUV.getYuvType();
				strYuvType = getYuvType(nYuvType);
				if (strYuvType == null
						|| !(comboExtra.getSelectedItem().equals(strYuvType))) {
					continue;
				}
			}

			break;
		}
		if (i >= nSize) {
			return (null);
		}

		if (formatVideo.getSize() == null) {
			formatVideoNew = new VideoFormat(null, controlSize.getVideoSize(),
					Format.NOT_SPECIFIED, null, -1f);
			formatVideo = (VideoFormat) formatVideoNew.intersects(formatVideo);
		}
		if (customFrameRates != null && formatVideo != null) {
			formatVideoNew = new VideoFormat(null, null, Format.NOT_SPECIFIED,
					null, getFrameRate());
			formatVideo = (VideoFormat) formatVideoNew.intersects(formatVideo);
		}

		return (formatVideo);
	}

	public float getFrameRate() {
		String selection = (String) comboFrameRate.getSelectedItem();
		if (selection != null) {
			if (selection.equals(DEFAULT_STRING)) {
				return (Format.NOT_SPECIFIED);
			}
			try {
				float fr = Float.valueOf(selection).floatValue();
				return fr;
			} catch (NumberFormatException nfe) {
			}
		}
		return (Format.NOT_SPECIFIED);
	}

	public void setCurrentFormat(VideoFormat formatDefault) {
		if (isFormatSupported(formatDefault)) {
			this.formatOld = formatDefault;
		}
		updateFields(formatOld);
	}

	public void setFrameRate(float frameRate) {
		for (int i = 0; i < comboFrameRate.getItemCount(); i++) {
			float value = Float.valueOf((Float) comboFrameRate.getItemAt(i))
					.floatValue();
			if (Math.abs(frameRate - value) < 0.5) {
				comboFrameRate.setSelectedIndex(i);
				return;
			}
		}
	}

	public void setSupportedFormats(Format arrFormats[],
			VideoFormat formatDefault) {
		int i;
		int nCount;

		this.arrSupportedFormats = arrFormats;

		vectorContSuppFormats.removeAllElements();
		nCount = arrSupportedFormats.length;
		for (i = 0; i < nCount; i++) {
			if (arrSupportedFormats[i] instanceof VideoFormat) {
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

	public Dimension getPreferredSize() {
		Dimension dim;
		Dimension dimControl;
		Dimension dimLabel;

		dim = new Dimension();
		if (boolDisplayEnableTrack == true) {
			dimControl = checkEnableTrack.getPreferredSize();
			dim.width = Math.max(dim.width, dimControl.width);
			dim.height += dimControl.height + MARGINV;
		}

		dimLabel = labelEncoding.getPreferredSize();
		nWidthLabel = Math.max(nWidthLabel, dimLabel.width);
		dimControl = comboEncoding.getPreferredSize();
		nWidthData = Math.max(nWidthData, dimControl.width);
		dim.height += Math.max(dimLabel.height, dimControl.height) + MARGINV;

		dimLabel = labelSize.getPreferredSize();
		nWidthLabel = Math.max(nWidthLabel, dimLabel.width);
		dimControl = controlSize.getPreferredSize();
		nWidthData = Math.max(nWidthData, dimControl.width);
		dim.height += Math.max(dimLabel.height, dimControl.height) + MARGINV;

		dimLabel = labelFrameRate.getPreferredSize();
		nWidthLabel = Math.max(nWidthLabel, dimLabel.width);
		dimControl = comboFrameRate.getPreferredSize();
		nWidthData = Math.max(nWidthData, dimControl.width);
		dim.height += Math.max(dimLabel.height, dimControl.height) + MARGINV;

		dimLabel = labelExtra.getPreferredSize();
		nWidthLabel = Math.max(nWidthLabel, dimLabel.width);
		dimControl = comboExtra.getPreferredSize();
		nWidthData = Math.max(nWidthData, dimControl.width);
		dim.height += Math.max(dimLabel.height, dimControl.height);

		dim.width = Math.max(dim.width, nWidthLabel + MARGINH + nWidthData);
		return (dim);
	}

	public void doLayout() {
		Dimension dimControl;
		Dimension dimLabel;
		Dimension dimThis;
		int nLabelOffsetX;
		int nDataOffsetX;
		int nOffsetY;

		getPreferredSize();
		nOffsetY = 0;
		nLabelOffsetX = 0;
		nDataOffsetX = nWidthLabel + MARGINH;
		dimThis = this.getSize();

		if (boolDisplayEnableTrack == true) {
			dimControl = checkEnableTrack.getPreferredSize();
			checkEnableTrack.setBounds(nLabelOffsetX, nOffsetY,
					dimControl.width, dimControl.height);
			nOffsetY += dimControl.height + MARGINV;
		}

		dimLabel = labelEncoding.getPreferredSize();
		dimControl = comboEncoding.getPreferredSize();
		labelEncoding.setBounds(nLabelOffsetX, nOffsetY, nWidthLabel,
				dimLabel.height);
		comboEncoding.setBounds(nDataOffsetX, nOffsetY, dimThis.width
				- nDataOffsetX, dimControl.height);
		nOffsetY += Math.max(dimLabel.height, dimControl.height) + MARGINV;

		dimLabel = labelSize.getPreferredSize();
		dimControl = controlSize.getPreferredSize();
		labelSize.setBounds(nLabelOffsetX, nOffsetY, nWidthLabel,
				dimLabel.height);
		controlSize.setBounds(nDataOffsetX, nOffsetY, dimThis.width
				- nDataOffsetX, dimControl.height);
		nOffsetY += Math.max(dimLabel.height, dimControl.height) + MARGINV;

		dimLabel = labelFrameRate.getPreferredSize();
		dimControl = comboFrameRate.getPreferredSize();
		labelFrameRate.setBounds(nLabelOffsetX, nOffsetY, nWidthLabel,
				dimLabel.height);
		comboFrameRate.setBounds(nDataOffsetX, nOffsetY, dimThis.width
				- nDataOffsetX, dimControl.height);
		nOffsetY += Math.max(dimLabel.height, dimControl.height) + MARGINV;

		dimLabel = labelExtra.getPreferredSize();
		dimControl = comboExtra.getPreferredSize();
		labelExtra.setBounds(nLabelOffsetX, nOffsetY, nWidthLabel,
				dimLabel.height);
		comboExtra.setBounds(nDataOffsetX, nOffsetY, dimThis.width
				- nDataOffsetX, dimControl.height);
		nOffsetY += Math.max(dimLabel.height, dimControl.height) + MARGINV;
	}

	private void init() throws Exception {
		int i;
		String strValue;
		VideoFormat format;
		VideoSize sizeVideo;

		this.setLayout(null);

		checkEnableTrack = new JCheckBox("启用通道：", true);
		checkEnableTrack.addItemListener(this);
		if (boolDisplayEnableTrack == true) {
			this.add(checkEnableTrack);
		}

		labelEncoding = new JLabel("编码：", Label.RIGHT);
		this.add(labelEncoding);
		comboEncoding = new JComboBox();
		comboEncoding.addItemListener(this);
		this.add(comboEncoding);

		labelSize = new JLabel("视频大小：", Label.RIGHT);
		this.add(labelSize);
		if (formatOld == null) {
			controlSize = new VideoSizeControl();
		} else {
			sizeVideo = new VideoSize(formatOld.getSize());
			controlSize = new VideoSizeControl(sizeVideo);
		}
		controlSize.addActionListener(this);
		this.add(controlSize);

		labelFrameRate = new JLabel("帧率：", Label.RIGHT);
		this.add(labelFrameRate);
		comboFrameRate = new JComboBox();
		comboFrameRate.addItemListener(this);
		this.add(comboFrameRate);

		labelExtra = new JLabel("额外：", Label.RIGHT);
		labelExtra.setVisible(false);
		this.add(labelExtra);
		comboExtra = new JComboBox();
		comboExtra.setVisible(false);
		this.add(comboExtra);

		updateFields(formatOld);
	}

	private void updateFields(VideoFormat formatDefault) {
		int i;
		int nSize;
		String strEncoding;
		String strEncodingPref = null;
		Object objectFormat;
		VideoFormat formatVideo;
		Vector<String> vectorEncoding = new Vector<String>();
		boolean boolEnable;

		boolEnable = comboEncoding.isEnabled();
		comboEncoding.setEnabled(false);
		comboEncoding.removeAll();

		nSize = vectorContSuppFormats.size();
		for (i = 0; i < nSize; i++) {
			objectFormat = vectorContSuppFormats.elementAt(i);
			if (!(objectFormat instanceof VideoFormat)) {
				continue;
			}
			formatVideo = (VideoFormat) objectFormat;

			strEncoding = formatVideo.getEncoding().toUpperCase();
			if (strEncodingPref == null) {
				strEncodingPref = strEncoding;
			}

			if (vectorEncoding.contains(strEncoding)) {
				continue;
			}
			comboEncoding.addItem(strEncoding);
			vectorEncoding.addElement(strEncoding);
		}

		if (formatDefault != null) {
			strEncoding = formatDefault.getEncoding().toUpperCase();
			comboEncoding.setSelectedItem(strEncoding);
		} else if (strEncodingPref != null) {
			comboEncoding.setSelectedItem(strEncodingPref);
		} else if (comboEncoding.getItemCount() > 0) {
			comboEncoding.setSelectedIndex(0);
		}

		updateFieldsFromEncoding(formatDefault);
		comboEncoding.setEnabled(boolEnable);
	}

	private void updateFieldsFromEncoding(VideoFormat formatDefault) {
		int i;
		int nSize;
		VideoSize sizeVideo;
		VideoSize sizeVideoPref = null;
		boolean boolVideoSizePref = false;
		Object objectFormat;
		VideoFormat formatVideo;
		Dimension formatVideoSize;
		boolean boolEnable;

		boolEnable = controlSize.isEnabled();
		controlSize.setEnabled(false);
		controlSize.removeAll();

		nSize = vectorContSuppFormats.size();
		for (i = 0; i < nSize; i++) {
			objectFormat = vectorContSuppFormats.elementAt(i);
			if (!(objectFormat instanceof VideoFormat)) {
				continue;
			}
			formatVideo = (VideoFormat) objectFormat;
			if (!this.isFormatGoodForEncoding(formatVideo)) {
				continue;
			}
			formatVideoSize = formatVideo.getSize();
			if (formatVideoSize == null)
			{
				sizeVideo = null;
			} else {
				sizeVideo = new VideoSize(formatVideoSize);
			}
			if (boolVideoSizePref == false) {
				boolVideoSizePref = true;
				sizeVideoPref = sizeVideo;
			}

			controlSize.addItem(sizeVideo);
		}

		if (formatDefault != null
				&& this.isFormatGoodForEncoding(formatDefault)) {
			formatVideoSize = formatDefault.getSize();
			if (formatVideoSize == null) {
				sizeVideo = null;
			}
			else {
				sizeVideo = new VideoSize(formatVideoSize);
			}
			controlSize.select(sizeVideo);
		} else if (boolVideoSizePref == true) {
			controlSize.select(sizeVideoPref);
		} else if (controlSize.getItemCount() > 0) {
			controlSize.select(0);
		}

		updateFieldsFromSize(formatDefault);
		controlSize.setEnabled(boolEnable);
	}

	private void updateFieldsFromSize(VideoFormat formatDefault) {
		int i;
		int nSize;
		Float floatFrameRate;
		Float floatFrameRatePref = null;
		Object objectFormat;
		VideoFormat formatVideo;
		Vector<Float> vectorRates = new Vector<Float>();
		boolean boolEnable;

		boolEnable = comboFrameRate.isEnabled();
		comboFrameRate.setEnabled(false);
		if (customFrameRates == null) {
			comboFrameRate.removeAll();
		} else if (comboFrameRate.getItemCount() < 1) {
			for (i = 0; i < customFrameRates.length; i++) {
				comboFrameRate.addItem(Float.toString(customFrameRates[i]));
			}
		}

		nSize = vectorContSuppFormats.size();
		for (i = 0; i < nSize; i++) {
			objectFormat = vectorContSuppFormats.elementAt(i);
			if (!(objectFormat instanceof VideoFormat)) {
				continue;
			}
			formatVideo = (VideoFormat) objectFormat;
			if (!this.isFormatGoodForEncoding(formatVideo)) {
				continue;
			}
			if (!this.isFormatGoodForVideoSize(formatVideo)) {
				continue;
			}

			if (customFrameRates != null) {
				continue;
			}

			floatFrameRate = new Float(formatVideo.getFrameRate());
			if (floatFrameRatePref == null) {
				floatFrameRatePref = floatFrameRate;
			}

			if (vectorRates.contains(floatFrameRate)) {
				continue;
			}
			if (floatFrameRate.floatValue() == Format.NOT_SPECIFIED) {
				comboFrameRate.addItem(DEFAULT_STRING);
			} else {
				comboFrameRate.addItem(floatFrameRate.toString());
			}
			vectorRates.addElement(floatFrameRate);
		}

		if (formatDefault != null && customFrameRates == null
				&& this.isFormatGoodForEncoding(formatDefault)
				&& this.isFormatGoodForVideoSize(formatDefault)) {
			floatFrameRate = new Float(formatDefault.getFrameRate());
			if (floatFrameRate.floatValue() == Format.NOT_SPECIFIED) {
				comboFrameRate.setSelectedItem(DEFAULT_STRING);
			} else {
				comboFrameRate.setSelectedItem(floatFrameRate.toString());
			}
		} else if (floatFrameRatePref != null) {
			if (floatFrameRatePref.floatValue() == Format.NOT_SPECIFIED) {
				comboFrameRate.setSelectedItem(DEFAULT_STRING);
			} else {
				comboFrameRate.setSelectedItem(floatFrameRatePref.toString());
			}
		} else if (comboFrameRate.getItemCount() > 0) {
			comboFrameRate.setSelectedIndex(0);
		}

		updateFieldsFromRate(formatDefault);
		comboFrameRate.setEnabled(boolEnable);
	}

	private void updateFieldsFromRate(VideoFormat formatDefault) {
		int i;
		int nSize;
		String strEncoding;
		Integer integerBitsPerPixel;
		int nYuvType;
		String strYuvType = null;
		Object objectFormat;
		VideoFormat formatVideo;
		RGBFormat formatRGB;
		YUVFormat formatYUV;
		
		@SuppressWarnings("rawtypes")
		Vector<Comparable> vectorExtra = new Vector<Comparable>();
		boolean boolRGB = false;
		boolean boolYUV = false;
		boolean boolEnable;

		strEncoding = (String) comboEncoding.getSelectedItem();
		if (strEncoding == null) {
			return;
		}

		if (strEncoding.equalsIgnoreCase(VideoFormat.RGB)) {
			labelExtra.setText("像素：");
			labelExtra.setVisible(true);
			comboExtra.setVisible(true);
			boolRGB = true;
		} else if (strEncoding.equalsIgnoreCase(VideoFormat.YUV)) {
			labelExtra.setText("YUV类型：");
			labelExtra.setVisible(true);
			comboExtra.setVisible(true);
			boolYUV = true;
		} else {
			labelExtra.setVisible(false);
			comboExtra.setVisible(false);
			return;
		}

		boolEnable = comboExtra.isEnabled();
		comboExtra.setEnabled(false);
		comboExtra.removeAll();

		nSize = vectorContSuppFormats.size();
		for (i = 0; i < nSize; i++) {
			objectFormat = vectorContSuppFormats.elementAt(i);
			if (!(objectFormat instanceof VideoFormat)) {
				continue;
			}
			formatVideo = (VideoFormat) objectFormat;
			if (!this.isFormatGoodForEncoding(formatVideo)) {
				continue;
			}
			if (!this.isFormatGoodForVideoSize(formatVideo)) {
				continue;
			}
			if (!this.isFormatGoodForFrameRate(formatVideo)) {
				continue;
			}

			if (boolRGB == true && formatVideo instanceof RGBFormat) {
				formatRGB = (RGBFormat) formatVideo;
				integerBitsPerPixel = new Integer(formatRGB.getBitsPerPixel());
				if (!(vectorExtra.contains(integerBitsPerPixel))) {
					comboExtra.addItem(integerBitsPerPixel.toString());
					vectorExtra.addElement(integerBitsPerPixel);
				}
			} else if (boolYUV == true && formatVideo instanceof YUVFormat) {
				formatYUV = (YUVFormat) formatVideo;
				nYuvType = formatYUV.getYuvType();
				strYuvType = getYuvType(nYuvType);
				if (strYuvType != null && !(vectorExtra.contains(strYuvType))) {
					comboExtra.addItem(strYuvType);
					vectorExtra.addElement(strYuvType);
				}
			}

		}
		if (formatDefault != null
				&& this.isFormatGoodForEncoding(formatDefault)
				&& this.isFormatGoodForVideoSize(formatDefault)
				&& this.isFormatGoodForFrameRate(formatDefault)) {
			if (boolRGB == true && formatDefault instanceof RGBFormat) {
				formatRGB = (RGBFormat) formatDefault;
				integerBitsPerPixel = new Integer(formatRGB.getBitsPerPixel());
				comboExtra.setSelectedItem(integerBitsPerPixel.toString());
			} else if (boolYUV == true && formatDefault instanceof YUVFormat) {
				formatYUV = (YUVFormat) formatDefault;
				nYuvType = formatYUV.getYuvType();
				strYuvType = getYuvType(nYuvType);
				if (strYuvType != null) {
					comboExtra.setSelectedItem(strYuvType);
				}
			} else if (comboExtra.getItemCount() > 0) {
				comboExtra.setSelectedIndex(0);
			}
		} else if (comboExtra.getItemCount() > 0) {
			comboExtra.setSelectedIndex(0);
		}

		comboExtra.setEnabled(boolEnable);
	}

	private boolean isFormatGoodForEncoding(VideoFormat format) {
		String strEncoding;
		boolean boolResult = false;

		strEncoding = (String) comboEncoding.getSelectedItem();
		if (strEncoding != null) {
			boolResult = format.getEncoding().equalsIgnoreCase(strEncoding);
		}
		return (boolResult);
	}

	private boolean isFormatGoodForVideoSize(VideoFormat format) {
		VideoSize sizeVideo;
		boolean boolResult = false;
		Dimension formatVideoSize;

		sizeVideo = controlSize.getVideoSize();
		formatVideoSize = format.getSize();
		if (formatVideoSize == null) {
			boolResult = true;
		} else {
			boolResult = sizeVideo.equals(formatVideoSize);
		}

		return (boolResult);
	}

	private boolean isFormatGoodForFrameRate(VideoFormat format) {
		String strFrameRate;
		float fFrameRate1;
		float fFrameRate2;
		boolean boolResult = false;

		if (customFrameRates != null) {
			return true;
		}

		strFrameRate = (String) comboFrameRate.getSelectedItem();
		if (strFrameRate.equals(DEFAULT_STRING)) {
			return true;
		}

		fFrameRate2 = format.getFrameRate();
		if (fFrameRate2 == Format.NOT_SPECIFIED) {
			return true;
		}

		if (strFrameRate != null) {
			fFrameRate1 = Float.valueOf(strFrameRate).floatValue();
			boolResult = (fFrameRate1 == fFrameRate2);
		}
		return (boolResult);
	}

	private boolean isFormatSupported(VideoFormat format) {
		int i;
		int nCount;
		VideoFormat formatVideo;
		boolean boolSupported = false;

		if (format == null) {
			return (boolSupported);
		}

		nCount = vectorContSuppFormats.size();
		for (i = 0; i < nCount && boolSupported == false; i++) {
			formatVideo = (VideoFormat) vectorContSuppFormats.elementAt(i);
			if (formatVideo.matches(format)) {
				boolSupported = true;
			}
		}
		return (boolSupported);
	}

	public void actionPerformed(ActionEvent event) {
		if (event.getActionCommand().equals(
				VideoSizeControl.ACTION_SIZE_CHANGED)) {
			updateFieldsFromSize(formatOld);
		}
	}

	public void itemStateChanged(ItemEvent event) {
		Object objectSource;

		objectSource = event.getSource();
		if (objectSource == checkEnableTrack) {
			boolEnableTrackSaved = checkEnableTrack.isSelected();
			onEnableTrack(true);
		} else if (objectSource == comboEncoding) {
			updateFieldsFromEncoding(formatOld);
		} else if (objectSource == controlSize) {
			updateFieldsFromSize(formatOld);
		} else if (objectSource == comboFrameRate) {
			updateFieldsFromRate(formatOld);
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
		labelSize.setEnabled(boolEnable);
		controlSize.setEnabled(boolEnable);
		labelFrameRate.setEnabled(boolEnable);
		comboFrameRate.setEnabled(boolEnable);
		labelExtra.setEnabled(boolEnable);
		comboExtra.setEnabled(boolEnable);
	}

	private String getYuvType(int nType) {
		String strType = null;

		if ((nType & YUVFormat.YUV_420) == YUVFormat.YUV_420) {
			strType = JMFI18N.getResource("formatchooser.yuv.4:2:0");
		} else if ((nType & YUVFormat.YUV_422) == YUVFormat.YUV_422) {
			strType = JMFI18N.getResource("formatchooser.yuv.4:2:2");
		} else if ((nType & YUVFormat.YUV_YUYV) == YUVFormat.YUV_YUYV) {
			strType = JMFI18N.getResource("formatchooser.yuv.YUYV");
		} else if ((nType & YUVFormat.YUV_111) == YUVFormat.YUV_111) {
			strType = JMFI18N.getResource("formatchooser.yuv.1:1:1");
		} else if ((nType & YUVFormat.YUV_411) == YUVFormat.YUV_411) {
			strType = JMFI18N.getResource("formatchooser.yuv.4:1:1");
		} else if ((nType & YUVFormat.YUV_YVU9) == YUVFormat.YUV_YVU9) {
			strType = JMFI18N.getResource("formatchooser.yuv.YVU9");
		} else {
			strType = null;
		}

		return (strType);
	}
}

class VideoSize extends Dimension {

	/**
	 * 
	 */
	private static final long serialVersionUID = 8191962003589369773L;

	public VideoSize() {
		super();
	}

	public VideoSize(int nWidth, int nHeight) {
		super(nWidth, nHeight);
	}

	public VideoSize(Dimension dim) {
		super(dim);
	}

	public boolean equals(Dimension dim) {
		boolean boolResult = true;

		if (dim == null) {
			boolResult = false;
		}
		if (boolResult == true) {
			boolResult = (this.width == dim.width);
		}
		if (boolResult == true) {
			boolResult = (this.height == dim.height);
		}
		return (boolResult);
	}

	public String toString() {
		return ("" + this.width + " x " + this.height);
	}
}

class VideoSizeControl extends JPanel implements ItemListener,
		ComponentListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2384301564824395488L;
	private JComboBox comboSize;
	private JPanel panelCustom;
	private JTextField textWidth;
	private JTextField textHeight;
	private JLabel labelX;
	private Hashtable<String, VideoSize> htSizes = new Hashtable<String, VideoSize>();
	private VideoSize sizeVideoDefault = null;
	private ActionListener listener;
	public static final String ACTION_SIZE_CHANGED = "Size Changed";
	static final String CUSTOM_STRING = JMFI18N
			.getResource("formatchooser.custom");

	public VideoSizeControl() {
		this(null);
	}

	public VideoSizeControl(VideoSize sizeVideoDefault) {
		super();

		this.sizeVideoDefault = sizeVideoDefault;
		init();
	}

	public void setEnabled(boolean boolEnable) {
		super.setEnabled(boolEnable);

		comboSize.setEnabled(boolEnable);
		textWidth.setEnabled(boolEnable);
		textHeight.setEnabled(boolEnable);
		labelX.setEnabled(boolEnable);

		if (boolEnable == true) {
			updateFields();
		}
	}

	public void addActionListener(ActionListener listener) {
		this.listener = listener;
	}

	public VideoSize getVideoSize() {
		String strItem;
		VideoSize sizeVideo;
		Object objSize;
		int nWidth;
		int nHeight;

		strItem = (String) comboSize.getSelectedItem();
		objSize = htSizes.get(strItem);
		if (objSize == null || !(objSize instanceof VideoSize)
				|| strItem.equals(CUSTOM_STRING)) {
			try {
				nWidth = Integer.valueOf(textWidth.getText()).intValue();
			} catch (Exception exception) {
				nWidth = 0;
			}
			try {
				nHeight = Integer.valueOf(textHeight.getText()).intValue();
			} catch (Exception exception) {
				nHeight = 0;
			}
			sizeVideo = new VideoSize(nWidth, nHeight);
		} else {
			sizeVideo = (VideoSize) objSize;
		}
		return (sizeVideo);
	}

	public void addItem(VideoSize sizeVideo) {
		String strItem;

		if (sizeVideo == null) {
			sizeVideo = new VideoSize(-1, -1);
			strItem = CUSTOM_STRING;
		} else {
			strItem = sizeVideo.toString();
		}

		if (htSizes.containsKey(strItem)) {
			return;
		}

		comboSize.addItem(strItem);
		htSizes.put(strItem, sizeVideo);

		if (comboSize.getItemCount() == 1) {
			updateFields();
		}
	}

	public void removeAll() {
		comboSize.removeAll();
		htSizes = new Hashtable<String, VideoSize>();
		updateFields();
	}

	public void select(VideoSize sizeVideo) {
		if (sizeVideo == null) {
			comboSize.setSelectedItem(CUSTOM_STRING);
		} else {
			comboSize.setSelectedItem(sizeVideo.toString());
		}
		updateFields();
	}

	public void select(int nIndex) {
		comboSize.setSelectedIndex(nIndex);
		updateFields();
	}

	public int getItemCount() {
		return (comboSize.getItemCount());
	}

	private void init() {
		Label label;

		setLayout(new GridLayout(0, 1, 4, 4));

		comboSize = new JComboBox();
		comboSize.addItem(CUSTOM_STRING);
		comboSize.addItemListener(this);
		this.add(comboSize);

		panelCustom = new JPanel(null);
		panelCustom.addComponentListener(this);
		this.add(panelCustom);

		if (sizeVideoDefault == null) {
			textWidth = new JTextField(3);
		} else {
			textWidth = new JTextField("" + sizeVideoDefault.width, 3);
		}
		panelCustom.add(textWidth, BorderLayout.CENTER);

		labelX = new JLabel("x", JLabel.CENTER);
		panelCustom.add(labelX, BorderLayout.WEST);
		if (sizeVideoDefault == null) {
			textHeight = new JTextField(3);
		} else {
			textHeight = new JTextField("" + sizeVideoDefault.height, 3);
		}
		panelCustom.add(textHeight, BorderLayout.CENTER);

		updateFields();
	}

	private void updateFields() {
		String strItem;
		boolean boolEnable;
		VideoSize sizeVideo;

		strItem = (String) comboSize.getSelectedItem();
		if (strItem == null || strItem.equals(CUSTOM_STRING)) {
			boolEnable = true;
		} else {
			sizeVideo = (VideoSize) htSizes.get(strItem);
			if (sizeVideo != null) {
				textWidth.setText("" + sizeVideo.width);
				textHeight.setText("" + sizeVideo.height);
			}
			boolEnable = false;
		}

		textWidth.setEnabled(boolEnable);
		textHeight.setEnabled(boolEnable);
		labelX.setEnabled(boolEnable);
	}

	private void resizeCustomFields() {
		Dimension dimPanel;
		Dimension dimLabelX;
		int nWidth;

		dimPanel = panelCustom.getSize();
		dimLabelX = labelX.getPreferredSize();
		nWidth = (dimPanel.width - dimLabelX.width) / 2;
		textWidth.setBounds(0, 0, nWidth, dimPanel.height);
		labelX.setBounds(nWidth, 0, dimLabelX.width, dimPanel.height);
		textHeight.setBounds(nWidth + dimLabelX.width, 0, nWidth,
				dimPanel.height);
	}

	public void itemStateChanged(ItemEvent event) {
		Object objectSource;
		ActionEvent eventAction;

		objectSource = event.getSource();
		if (objectSource != comboSize) {
			return;
		}
		updateFields();
		if (listener != null) {
			eventAction = new ActionEvent(this, ActionEvent.ACTION_PERFORMED,
					ACTION_SIZE_CHANGED);
			listener.actionPerformed(eventAction);
		}
	}

	public void componentResized(ComponentEvent event) {
		resizeCustomFields();
	}

	public void componentMoved(ComponentEvent event) {
	}

	public void componentShown(ComponentEvent event) {
	}

	public void componentHidden(ComponentEvent event) {
	}
    
}
