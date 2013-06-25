package edu.chita.helper;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.*;
import javax.media.*;
import javax.media.format.AudioFormat;
import javax.media.format.VideoFormat;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;

public class CaptureDeviceDetector {

	// dsoundЭ��ǰ׺
	public static final String PROTOCOL_DSOUND_PREFIX = "dsound://";
	// javasoundЭ��ǰ׺
	public static final String PROTOCOL_JAVASOUND_PREFIX = "javasound://";
	// vfwЭ��
	public static final String PROTOCOL_VFW_NAME = "vfw";
	// vfwЭ��ǰ׺
	public static final String PROTOCOL_VFW_PREFIX = PROTOCOL_VFW_NAME + "://";
	// sunvideoЭ��
	public static final String PROTOCOL_SUNVIDEO_NAME = "sunvideo";
	// sunvideoЭ��ǰ׺
	public static final String PROTOCOL_SUNVIDEO_PREFIX = PROTOCOL_SUNVIDEO_NAME
			+ "://";
	// v4lЭ��
	public static final String PROTOCOL_V4L_NAME = "v4l";
	// v4lЭ��ǰ׺
	public static final String PROTOCOL_V4L_PREFIX = PROTOCOL_V4L_NAME + "://";
	private static final String DIRECT_SOUND_AUTO_CLASS = "DirectSoundAuto";
	private static final String JAVA_SOUND_AUTO_CLASS = "JavaSoundAuto";
	private static final String V4L_AUTO_CLASS = "V4LAuto";
	private static final String SUN_VIDEO_AUTO_CLASS = "SunVideoAuto";
	private static final String SUN_VIDEO_PLUS_AUTO_CLASS = "SunVideoPlusAuto";
	private boolean hasDSoundCapture = false;
	private boolean hasJavaSoundCapture = false;
	// ��Ƶ�����豸Map
	private Map<String, CaptureDeviceInfo> videoCaptureDeviceMap = new HashMap<String, CaptureDeviceInfo>();

	/**
	 * ��Ⲷ���豸
	 */
	public void detectCaptureDevices() {
		// �������еĲ����豸���ֱ���Ƶ����Ƶ�����豸�����Ӧ��Map
		populateDeviceMap();

		if (!hasDSoundCapture) {
			autoDetectDSoundCapture();
		}

		if (!hasJavaSoundCapture) {
			autoDetectJavaSoundCapture();
		}

		// ��������Windows��ʹ��vfw����Linuxʹ��sunvideo�������֧��sunvideo��ʹ��v4l
		String osName = System.getProperty("os.name");
		if (osName.contains("Windows")) {
			detectVFWCapture();
		} else if (osName.contains("Linux")) {
			if (!autoDetectSunVideoCapture()) {
				autoDetectV4LCapture();
			}
		}
	}

	/**
	 * ��CaptureDeviceManager��ȡ���еĲ����豸���ֱ�������Ƶ�����豸��Map����Ƶ�����豸Map
	 */
	private void populateDeviceMap() {
		@SuppressWarnings("unchecked")
		Vector<CaptureDeviceInfo> devices = (Vector<CaptureDeviceInfo>) CaptureDeviceManager
				.getDeviceList(null);
		for (CaptureDeviceInfo device : devices) {
			if (device.getLocator().toString()
					.startsWith(PROTOCOL_DSOUND_PREFIX)) {
				hasDSoundCapture = true;
			} else if (device.getLocator().toString()
					.startsWith(PROTOCOL_JAVASOUND_PREFIX)) {
				hasJavaSoundCapture = true;
			} else if (device.getLocator().toString()
					.startsWith(PROTOCOL_VFW_PREFIX)) {
				videoCaptureDeviceMap.put(device.getName(), device);
			} else if (device.getLocator().toString()
					.startsWith(PROTOCOL_SUNVIDEO_PREFIX)) {
				videoCaptureDeviceMap.put(device.getName(), device);
			} else if (device.getLocator().toString()
					.startsWith(PROTOCOL_V4L_PREFIX)) {
				videoCaptureDeviceMap.put(device.getName(), device);
			}
		}
	}

	/**
	 * z�Զ����
	 * @param autoClass
	 * @return
	 */
	private Class<?> autoDetect(String autoClass) {
		Class<?> auto = null;
		try {
			auto = Class.forName(autoClass);
			if (auto != null) {
				auto.newInstance();
			}
		} catch (InstantiationException ex) {
			ex.printStackTrace();
		} catch (IllegalAccessException ex) {
			ex.printStackTrace();
		} catch (ClassNotFoundException ex) {
			ex.printStackTrace();
		}
		return auto;
	}

	/**
	 * ���dsound�豸
	 */
	private void autoDetectDSoundCapture() {
		autoDetect(DIRECT_SOUND_AUTO_CLASS);
	}

	/**
	 * ���javasound�豸
	 */
	private void autoDetectJavaSoundCapture() {
		autoDetect(JAVA_SOUND_AUTO_CLASS);
	}

	/**
	 * ���sunvideo�豸
	 * @return
	 */
	private boolean autoDetectSunVideoCapture() {
		Class<?> autoVideo = autoDetect(SUN_VIDEO_AUTO_CLASS);	
		Class<?> autoVideoPlus = autoDetect(SUN_VIDEO_PLUS_AUTO_CLASS);
		return (autoVideo != null) && (autoVideoPlus != null);
	}

	/**
	 * ���vfw�����豸
	 */
	private void detectVFWCapture() {
		boolean isUpdate = false;
		try {
			Set<String> deviceNameSet = new HashSet<String>();
			for (int index = 0; index < 10; index++) {
				String name = com.sun.media.protocol.vfw.VFWCapture
						.capGetDriverDescriptionName(index);
				if (name != null && name.length() > 1) {
					String capName = PROTOCOL_VFW_NAME + ":" + name + ":"
							+ index;
					deviceNameSet.add(capName);
					if (videoCaptureDeviceMap.get(capName) == null) {
						// CaptureDeviceInfo device = new
						// com.sun.media.protocol.vfw.VFWDeviceInfo(index);
						CaptureDeviceInfo device = new com.sun.media.protocol.vfw.VFWDeviceQuery(
								index);
						if (device != null && device.getFormats() != null
								&& device.getFormats().length > 0) {
							// ����ҵ����µ�vfw�豸��ע�ᵽCaptureDeviceManager
							isUpdate = CaptureDeviceManager.addDevice(device);
							if (isUpdate) {
								videoCaptureDeviceMap.put(capName, device);
							}
						}
					}
				}
			}
			// ����ж����vfw�豸����CaptureDeviceManager�Ƴ�
			for (String name : videoCaptureDeviceMap.keySet()) {
				if (!deviceNameSet.contains(name)) {
					isUpdate = CaptureDeviceManager
							.removeDevice(videoCaptureDeviceMap.get(name));
				}
			}
		} finally {
			if (isUpdate) {
				try {
					CaptureDeviceManager.commit();
				} catch (IOException ex) {
					ex.printStackTrace();
				}
			}
		}
	}

	/**
	 * ���v4l�����豸
	 * @return
	 */
	private boolean autoDetectV4LCapture() {
		Class<?> autoVideo = autoDetect(V4L_AUTO_CLASS);
		return autoVideo != null;
	}

	// ����
	public static void main(String[] args) {
		CaptureDeviceDetector searcher = new CaptureDeviceDetector();
		searcher.detectCaptureDevices();

		try {
			// ����processor
			final Processor player = Manager
					.createRealizedProcessor(new ProcessorModel(new Format[] {
							new AudioFormat(AudioFormat.LINEAR),
							new VideoFormat(VideoFormat.YUV) }, null));

			// �������岢ʹ��processor���в���
			JFrame frame = new JFrame("�������");
			Container con = frame.getContentPane();
			con.setLayout(new BorderLayout());
			Component visualComponent = player.getVisualComponent();
			if (visualComponent != null) {
				con.add(visualComponent, BorderLayout.CENTER);
			}
			JPanel controlPanel = new JPanel();
			controlPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
			JButton startButton = new JButton("��ʼ");
			startButton.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					player.start();
				}
			});
			JButton stopButton = new JButton("ֹͣ");
			stopButton.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					player.stop();
					// �ͷ�������Դ
					player.deallocate();
				}
			});
			controlPanel.add(startButton);
			controlPanel.add(stopButton);
			con.add(controlPanel, BorderLayout.SOUTH);
			frame.addWindowListener(new WindowAdapter() {

				@Override
				public void windowClosing(WindowEvent e) {
					// �رղ��ͷ�������Դ
					player.close();
					System.exit(0);
				}
			});
			frame.pack();
			frame.setLocationRelativeTo(null);
			frame.setVisible(true);
		} catch (IOException ex) {
			ex.printStackTrace();
		} catch (NoProcessorException ex) {
			ex.printStackTrace();
		} catch (CannotRealizeException ex) {
			ex.printStackTrace();
		}
	}
    
}
