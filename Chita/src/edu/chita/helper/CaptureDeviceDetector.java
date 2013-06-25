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

	// dsound协议前缀
	public static final String PROTOCOL_DSOUND_PREFIX = "dsound://";
	// javasound协议前缀
	public static final String PROTOCOL_JAVASOUND_PREFIX = "javasound://";
	// vfw协议
	public static final String PROTOCOL_VFW_NAME = "vfw";
	// vfw协议前缀
	public static final String PROTOCOL_VFW_PREFIX = PROTOCOL_VFW_NAME + "://";
	// sunvideo协议
	public static final String PROTOCOL_SUNVIDEO_NAME = "sunvideo";
	// sunvideo协议前缀
	public static final String PROTOCOL_SUNVIDEO_PREFIX = PROTOCOL_SUNVIDEO_NAME
			+ "://";
	// v4l协议
	public static final String PROTOCOL_V4L_NAME = "v4l";
	// v4l协议前缀
	public static final String PROTOCOL_V4L_PREFIX = PROTOCOL_V4L_NAME + "://";
	private static final String DIRECT_SOUND_AUTO_CLASS = "DirectSoundAuto";
	private static final String JAVA_SOUND_AUTO_CLASS = "JavaSoundAuto";
	private static final String V4L_AUTO_CLASS = "V4LAuto";
	private static final String SUN_VIDEO_AUTO_CLASS = "SunVideoAuto";
	private static final String SUN_VIDEO_PLUS_AUTO_CLASS = "SunVideoPlusAuto";
	private boolean hasDSoundCapture = false;
	private boolean hasJavaSoundCapture = false;
	// 视频捕获设备Map
	private Map<String, CaptureDeviceInfo> videoCaptureDeviceMap = new HashMap<String, CaptureDeviceInfo>();

	/**
	 * 检测捕获设备
	 */
	public void detectCaptureDevices() {
		// 查找所有的捕获设备，分别将视频和音频捕获设备放入对应的Map
		populateDeviceMap();

		if (!hasDSoundCapture) {
			autoDetectDSoundCapture();
		}

		if (!hasJavaSoundCapture) {
			autoDetectJavaSoundCapture();
		}

		// 这里我在Windows下使用vfw，在Linux使用sunvideo，如果不支持sunvideo则使用v4l
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
	 * 从CaptureDeviceManager获取所有的捕获设备，分别填入视频捕获设备的Map和音频捕获设备Map
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
	 * z自动检测
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
	 * 检测dsound设备
	 */
	private void autoDetectDSoundCapture() {
		autoDetect(DIRECT_SOUND_AUTO_CLASS);
	}

	/**
	 * 检测javasound设备
	 */
	private void autoDetectJavaSoundCapture() {
		autoDetect(JAVA_SOUND_AUTO_CLASS);
	}

	/**
	 * 检测sunvideo设备
	 * @return
	 */
	private boolean autoDetectSunVideoCapture() {
		Class<?> autoVideo = autoDetect(SUN_VIDEO_AUTO_CLASS);	
		Class<?> autoVideoPlus = autoDetect(SUN_VIDEO_PLUS_AUTO_CLASS);
		return (autoVideo != null) && (autoVideoPlus != null);
	}

	/**
	 * 检测vfw捕获设备
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
							// 如果找到有新的vfw设备，注册到CaptureDeviceManager
							isUpdate = CaptureDeviceManager.addDevice(device);
							if (isUpdate) {
								videoCaptureDeviceMap.put(capName, device);
							}
						}
					}
				}
			}
			// 如果有多余的vfw设备，从CaptureDeviceManager移除
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
	 * 检测v4l捕获设备
	 * @return
	 */
	private boolean autoDetectV4LCapture() {
		Class<?> autoVideo = autoDetect(V4L_AUTO_CLASS);
		return autoVideo != null;
	}

	// 测试
	public static void main(String[] args) {
		CaptureDeviceDetector searcher = new CaptureDeviceDetector();
		searcher.detectCaptureDevices();

		try {
			// 创建processor
			final Processor player = Manager
					.createRealizedProcessor(new ProcessorModel(new Format[] {
							new AudioFormat(AudioFormat.LINEAR),
							new VideoFormat(VideoFormat.YUV) }, null));

			// 构建窗体并使用processor进行捕获
			JFrame frame = new JFrame("捕获测试");
			Container con = frame.getContentPane();
			con.setLayout(new BorderLayout());
			Component visualComponent = player.getVisualComponent();
			if (visualComponent != null) {
				con.add(visualComponent, BorderLayout.CENTER);
			}
			JPanel controlPanel = new JPanel();
			controlPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
			JButton startButton = new JButton("开始");
			startButton.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					player.start();
				}
			});
			JButton stopButton = new JButton("停止");
			stopButton.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					player.stop();
					// 释放所用资源
					player.deallocate();
				}
			});
			controlPanel.add(startButton);
			controlPanel.add(stopButton);
			con.add(controlPanel, BorderLayout.SOUTH);
			frame.addWindowListener(new WindowAdapter() {

				@Override
				public void windowClosing(WindowEvent e) {
					// 关闭并释放所有资源
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
