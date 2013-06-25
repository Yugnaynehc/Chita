package edu.chita.helper;
import java.awt.*;
import java.awt.event.*;

import javax.media.*;
import javax.media.format.*;
import javax.media.protocol.*;

import com.sun.media.util.JMFI18N;
import java.util.Vector;
import javax.swing.*;

import jmapps.util.*;

public class CaptureConfigPanel extends JPanel implements ItemListener {

	private JMAppsCfg cfgJMApps;
	private Vector<?> vectorDevices = null;
	private Vector<CaptureDeviceInfo> vectorAudioDevices = null;
	private Vector<CaptureDeviceInfo> vectorVideoDevices = null;
	private JPanel panelDevices;
	private JCheckBox checkUseVideo = null;
	private JCheckBox checkUseAudio = null;
	private JComboBox comboVideoDevice = null;
	private JComboBox comboAudioDevice = null;
	private JPanel panelVideoFormat = null;
	private JPanel panelAudioFormat = null;
	private JAudioFormatChooser chooserAudio = null;
	private JVideoFormatChooser chooserVideo = null;
	private JButton buttonOK;
	private JButton buttonCancel;

	/**
	 * 空构造函数
	 */
	public CaptureConfigPanel() {
		this(null);
	}

	/**
	 * 构造函数
	 * @param cfgJMApps
	 */
	public CaptureConfigPanel(JMAppsCfg cfgJMApps) {
		this.cfgJMApps = cfgJMApps;
		try {
			init();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 标记是否采用视频输入
	 * @return
	 */
	public boolean isVideoDeviceUsed() {
		boolean boolUsed = false;

		if (checkUseVideo != null) {
			boolUsed = checkUseVideo.isSelected();
		}
		return (boolUsed);
	}

	/**
	 * 标记是否采用音频输入
	 * @return
	 */
	public boolean isAudioDeviceUsed() {
		boolean boolUsed = false;

		if (checkUseAudio != null) {
			boolUsed = checkUseAudio.isSelected();
		}
		return (boolUsed);
	}

	/**
	 * 获取选择的视频设备
	 * @return
	 */
	public CaptureDeviceInfo getVideoDevice() {
		int i;
		CaptureDeviceInfo infoCaptureDevice = null;

		if (comboVideoDevice != null && isVideoDeviceUsed()) {
			i = comboVideoDevice.getSelectedIndex();
			infoCaptureDevice = (CaptureDeviceInfo) vectorVideoDevices
					.elementAt(i);
		}
		return (infoCaptureDevice);
	}

	/**
	 * 获取选择的音频设备
	 * @return
	 */
	public CaptureDeviceInfo getAudioDevice() {
		int i;
		CaptureDeviceInfo infoCaptureDevice = null;

		if (comboAudioDevice != null && isAudioDeviceUsed()) {
			i = comboAudioDevice.getSelectedIndex();
			infoCaptureDevice = (CaptureDeviceInfo) vectorAudioDevices
					.elementAt(i);
		}
		return (infoCaptureDevice);
	}

	/**
	 * 获取选择的视频格式
	 * @return
	 */
	public VideoFormat getVideoFormat() {
		VideoFormat format = null;

		if (chooserVideo != null && isVideoDeviceUsed()) {
			format = (VideoFormat) chooserVideo.getFormat();
		}
		return (format);
	}

	/**
	 * 获取选择的音频格式
	 * @return
	 */
	public AudioFormat getAudioFormat() {
		AudioFormat format = null;

		if (chooserAudio != null && isAudioDeviceUsed()) {
			format = (AudioFormat) chooserAudio.getFormat();
		}
		return (format);
	}

	/**
	 * 获得捕获的数据源
	 * @return
	 */
	public DataSource createCaptureDataSource() {
		DataSource dataSource = null;
		String audioDeviceName = null;
		String videoDeviceName = null;
		CaptureDeviceInfo cdi;

		cdi = getAudioDevice();
		if (cdi != null && isAudioDeviceUsed()) {
			audioDeviceName = cdi.getName();
		}
		cdi = getVideoDevice();
		if (cdi != null && isVideoDeviceUsed()) {
			videoDeviceName = cdi.getName();
		}
		dataSource = JMFUtils.createCaptureDataSource(audioDeviceName,
				getAudioFormat(), videoDeviceName, getVideoFormat());

		return (dataSource);
	}

	/**
	 * 初始化
	 * @throws Exception
	 */
	@SuppressWarnings("serial")
	private void init() throws Exception {
		JPanel panel;
		JPanel panelButtons;
		JPanel panelContent;
		JLabel label;

		this.setLayout(new BorderLayout());
		panelContent = new JPanel(new BorderLayout()) {
			@Override
			public Insets getInsets() {
				return new Insets(10, 10, 10, 10);
			}
		};
		this.add(panelContent, BorderLayout.CENTER);

		vectorDevices = CaptureDeviceManager.getDeviceList(null);
		if (vectorDevices == null || vectorDevices.size() < 1) {
			label = new JLabel("没有找到设备");
			panelContent.add(label, BorderLayout.CENTER);
		} else {
			panelDevices = new JPanel(new GridLayout(1, 0, 6, 6));
			panelContent.add(panelDevices, BorderLayout.CENTER);

			panel = createVideoPanel();
			if (panel != null) {
				panelDevices.add(panel);
			}
			panel = createAudioPanel();
			if (panel != null) {
				panelDevices.add(panel);
			}
		}

		panel = new JPanel(new FlowLayout(FlowLayout.CENTER));
		panelContent.add(panel, BorderLayout.SOUTH);

		panelButtons = createButtonPanel();
		panel.add(panelButtons);
	}

	/**
	 * 创建按钮面板
	 * @return
	 */
	private JPanel createButtonPanel() {
		JPanel panel = new JPanel();
		panel.setLayout(new FlowLayout(FlowLayout.CENTER));
		buttonOK = new JButton("确定");
		buttonOK.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				okActionPerformed();
			}
		});
		buttonCancel = new JButton("取消");
		buttonCancel.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				cancelActionPerformed();
			}
		});
		panel.add(buttonOK);
		panel.add(buttonCancel);
		return panel;
	}

	protected void okActionPerformed() {
		//
	}

	protected void cancelActionPerformed() {
		//
	}

	/**
	 * 创建视频设备选择面板
	 * @return
	 * @throws Exception
	 */
	private JPanel createVideoPanel() throws Exception {
		int i, j;
		int nCount;
		JPanel panelVideo;
		JPanel panelContent;
		JPanel panel;
		JPanel panelTemp;
		CaptureDeviceInfo infoCaptureDevice;
		Format arrFormats[];
		boolean boolState = true;
		VideoFormat formatDefault = null;
		String strDeviceName;
		boolean boolContains;
		JMAppsCfg.CaptureDeviceData dataCapture = null;

		nCount = vectorDevices.size();
		vectorVideoDevices = new Vector<CaptureDeviceInfo>();
		for (i = 0; i < nCount; i++) {
			infoCaptureDevice = (CaptureDeviceInfo) vectorDevices.elementAt(i);
			arrFormats = infoCaptureDevice.getFormats();
			for (j = 0; j < arrFormats.length; j++) {
				if (arrFormats[j] instanceof VideoFormat) {
					vectorVideoDevices.addElement(infoCaptureDevice);
					break;
				}
			}
		}

		if (vectorVideoDevices.isEmpty()) {
			return (null);
		}

		if (cfgJMApps != null) {
			dataCapture = cfgJMApps.getLastCaptureVideoData();
		}
		if (dataCapture != null) {
			boolState = dataCapture.boolUse;
			if (dataCapture.format instanceof VideoFormat) {
				formatDefault = (VideoFormat) dataCapture.format;
			}
		}
		panelVideo = new JPanel(new BorderLayout(6, 6));

		panelContent = new JPanel(new BorderLayout(6, 6));
		panelContent.setBorder(BorderFactory.createTitledBorder(""));
		panelVideo.add(panelContent, BorderLayout.CENTER);
		panel = panelContent;

		panelTemp = new JPanel(new BorderLayout(6, 6));
		panel.add(panelTemp, BorderLayout.NORTH);
		checkUseVideo = new JCheckBox("使用视频捕获设备", boolState);
		checkUseVideo.addItemListener(this);
		panelTemp.add(checkUseVideo, BorderLayout.WEST);
		panelTemp = new JPanel(new BorderLayout(6, 6));
		panel.add(panelTemp, BorderLayout.CENTER);
		panel = panelTemp;

		panelTemp = new JPanel(new BorderLayout(6, 6));
		panel.add(panelTemp, BorderLayout.NORTH);
		comboVideoDevice = new JComboBox();
		panelTemp.add(comboVideoDevice, BorderLayout.CENTER);
		nCount = vectorVideoDevices.size();
		boolContains = false;
		for (i = 0; i < nCount; i++) {
			infoCaptureDevice = (CaptureDeviceInfo) vectorVideoDevices
					.elementAt(i);
			strDeviceName = infoCaptureDevice.getName();
			comboVideoDevice.addItem(strDeviceName);
			if (boolContains == false && dataCapture != null
					&& dataCapture.strDeviceName != null) {
				boolContains = dataCapture.strDeviceName.equals(strDeviceName);
			}
		}
		if (boolContains == true) {
			comboVideoDevice.setSelectedItem(dataCapture.strDeviceName);
		}
		comboVideoDevice.addItemListener(this);
		comboVideoDevice.setEnabled(boolState);

		panelVideoFormat = new JPanel(new BorderLayout(6, 6));
		panel.add(panelVideoFormat, BorderLayout.CENTER);
		createVideoChooser(formatDefault);
		if (chooserVideo != null) {
			chooserVideo.setEnabled(boolState);
		}

		return (panelVideo);
	}

	/**
	 * 确定视频设备选择
	 * @param formatDefault
	 */
	private void createVideoChooser(VideoFormat formatDefault) {
		int i;
		CaptureDeviceInfo infoCaptureDevice;
		Format arrFormats[];

		if (panelVideoFormat == null) {
			return;
		}

		panelVideoFormat.removeAll();

		i = comboVideoDevice.getSelectedIndex();
		infoCaptureDevice = (CaptureDeviceInfo) vectorVideoDevices.elementAt(i);
		arrFormats = infoCaptureDevice.getFormats();
		chooserVideo = new JVideoFormatChooser(arrFormats, formatDefault,
				false, null, true);
		panelVideoFormat.add(chooserVideo, BorderLayout.CENTER);
	}

	/**
	 * 创建音频选择面板
	 * @return
	 * @throws Exception
	 */
	private JPanel createAudioPanel() throws Exception {
		int i, j;
		int nCount;
		JPanel panelAudio;
		JPanel panelContent;
		JPanel panel;
		JPanel panelTemp;
		CaptureDeviceInfo infoCaptureDevice;
		Format arrFormats[];
		boolean boolState = true;
		AudioFormat formatDefault = null;
		String strDeviceName;
		boolean boolContains;
		JMAppsCfg.CaptureDeviceData dataCapture = null;

		nCount = vectorDevices.size();
		vectorAudioDevices = new Vector<CaptureDeviceInfo>();
		for (i = 0; i < nCount; i++) {
			infoCaptureDevice = (CaptureDeviceInfo) vectorDevices.elementAt(i);
			arrFormats = infoCaptureDevice.getFormats();
			for (j = 0; j < arrFormats.length; j++) {
				if (arrFormats[j] instanceof AudioFormat) {
					vectorAudioDevices.addElement(infoCaptureDevice);
					break;
				}
			}
		}

		if (vectorAudioDevices.isEmpty()) {
			return (null);
		}

		if (cfgJMApps != null) {
			dataCapture = cfgJMApps.getLastCaptureAudioData();
		}
		if (dataCapture != null) {
			boolState = dataCapture.boolUse;
			if (dataCapture.format instanceof AudioFormat) {
				formatDefault = (AudioFormat) dataCapture.format;
			}
		}

		panelAudio = new JPanel(new BorderLayout(6, 6));

		panelContent = new JPanel(new BorderLayout(6, 6));
		panelContent.setBorder(BorderFactory.createTitledBorder(""));
		panelAudio.add(panelContent, BorderLayout.CENTER);
		panel = panelContent;

		panelTemp = new JPanel(new BorderLayout(6, 6));
		panel.add(panelTemp, BorderLayout.NORTH);
		checkUseAudio = new JCheckBox("使用音频捕获设备", boolState);
		checkUseAudio.addItemListener(this);
		panelTemp.add(checkUseAudio, BorderLayout.WEST);
		panelTemp = new JPanel(new BorderLayout(6, 6));
		panel.add(panelTemp, BorderLayout.CENTER);
		panel = panelTemp;

		panelTemp = new JPanel(new BorderLayout(6, 6));
		panel.add(panelTemp, BorderLayout.NORTH);
		comboAudioDevice = new JComboBox();
		panelTemp.add(comboAudioDevice, BorderLayout.CENTER);
		nCount = vectorAudioDevices.size();
		boolContains = false;
		for (i = 0; i < nCount; i++) {
			infoCaptureDevice = (CaptureDeviceInfo) vectorAudioDevices
					.elementAt(i);
			strDeviceName = infoCaptureDevice.getName();
			comboAudioDevice.addItem(strDeviceName);
			if (boolContains == false && dataCapture != null
					&& dataCapture.strDeviceName != null) {
				boolContains = dataCapture.strDeviceName.equals(strDeviceName);
			}
		}
		if (boolContains == true) {
			comboAudioDevice.setSelectedItem(dataCapture.strDeviceName);
		}
		comboAudioDevice.addItemListener(this);
		comboAudioDevice.setEnabled(boolState);

		panelAudioFormat = new JPanel(new BorderLayout(6, 6));
		panel.add(panelAudioFormat, BorderLayout.CENTER);
		createAudioChooser(formatDefault);
		if (chooserAudio != null) {
			chooserAudio.setEnabled(boolState);
		}

		return (panelAudio);
	}

	/**
	 * 确定选择的音频设备
	 * @param formatDefault
	 */
	private void createAudioChooser(AudioFormat formatDefault) {
		int i;
		CaptureDeviceInfo infoCaptureDevice;
		Format arrFormats[];

		if (panelAudioFormat == null) {
			return;
		}

		panelAudioFormat.removeAll();

		i = comboAudioDevice.getSelectedIndex();
		infoCaptureDevice = (CaptureDeviceInfo) vectorAudioDevices.elementAt(i);
		arrFormats = infoCaptureDevice.getFormats();
		chooserAudio = new JAudioFormatChooser(arrFormats, formatDefault,
				false, null);
		panelAudioFormat.add(chooserAudio, BorderLayout.CENTER);
	}

	/**
	 * 保存捕获的信息
	 */
	private void saveCfgData() {

		JMAppsCfg.CaptureDeviceData dataCapture;

		if (cfgJMApps == null) {
			return;
		}

		// 音频
		dataCapture = cfgJMApps.createCaptureDeviceDataObject();
		dataCapture.boolUse = isAudioDeviceUsed();
		if (comboAudioDevice != null) {
			dataCapture.strDeviceName = (String) comboAudioDevice
					.getSelectedItem();
		}
		if (chooserAudio != null) {
			dataCapture.format = chooserAudio.getFormat();
		}
		cfgJMApps.setLastCaptureAudioData(dataCapture);

		// 视频
		dataCapture = cfgJMApps.createCaptureDeviceDataObject();
		dataCapture.boolUse = isVideoDeviceUsed();
		if (comboVideoDevice != null) {
			dataCapture.strDeviceName = (String) comboVideoDevice
					.getSelectedItem();
		}
		if (chooserVideo != null) {
			dataCapture.format = chooserVideo.getFormat();
		}
		cfgJMApps.setLastCaptureVideoData(dataCapture);
	}

	/**
	 * 面板的事件响应处理函数
	 */
	public void itemStateChanged(ItemEvent event) {
		Object objectSource;
		boolean boolEnable;

		objectSource = event.getSource();

		if (objectSource == checkUseVideo) {
			boolEnable = checkUseVideo.isSelected();
			comboVideoDevice.setEnabled(boolEnable);
			chooserVideo.setEnabled(boolEnable);
		} else if (objectSource == checkUseAudio) {
			boolEnable = checkUseAudio.isSelected();
			comboAudioDevice.setEnabled(boolEnable);
			chooserAudio.setEnabled(boolEnable);
		} else if (objectSource == comboVideoDevice) {
			createVideoChooser(null);
			validate();
		} else if (objectSource == comboAudioDevice) {
			createAudioChooser(null);
			validate();
		}
	}

	/**
	 * 构造字符串信息
	 */
	public String toString() {
		String strValue = "";
		CaptureDeviceInfo cdiAudio;
		CaptureDeviceInfo cdiVideo;
		MediaLocator deviceURL;

		cdiAudio = getAudioDevice();
		if (cdiAudio != null && isAudioDeviceUsed()) {
			deviceURL = cdiAudio.getLocator();
			if (strValue.length() > 0) {
				strValue = strValue + " & ";
			}
			strValue = strValue + deviceURL.toString();
		}

		cdiVideo = getVideoDevice();
		if (cdiVideo != null && isVideoDeviceUsed()) {
			deviceURL = cdiVideo.getLocator();
			if (strValue.length() > 0) {
				strValue = strValue + " & ";
			}
			strValue = strValue + deviceURL.toString();
		}

		return (strValue);
	}

	public static void main(String[] args) {
		try {
			for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager
					.getInstalledLookAndFeels()) {
				if ("Nimbus".equals(info.getName())) {
					javax.swing.UIManager.setLookAndFeel(info.getClassName());
					break;
				}
			}
		} catch (ClassNotFoundException ex) {
		} catch (InstantiationException ex) {
		} catch (IllegalAccessException ex) {
		} catch (javax.swing.UnsupportedLookAndFeelException ex) {
		}
	}
    
}
