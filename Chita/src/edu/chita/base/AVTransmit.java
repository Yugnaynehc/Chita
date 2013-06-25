package edu.chita.base;

import edu.chita.helper.*;
import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;

import javax.media.CannotRealizeException;
import javax.media.CaptureDeviceInfo;
import javax.media.Codec;
import javax.media.Control;
import javax.media.Controller;
import javax.media.ControllerClosedEvent;
import javax.media.ControllerEvent;
import javax.media.ControllerListener;
import javax.media.DataSink;
import javax.media.Format;
import javax.media.Manager;
import javax.media.MediaLocator;
import javax.media.NoDataSourceException;
import javax.media.NoPlayerException;
import javax.media.NoProcessorException;
import javax.media.NotRealizedError;
import javax.media.Owned;
import javax.media.Player;
import javax.media.Processor;
import javax.media.control.QualityControl;
import javax.media.control.TrackControl;
import javax.media.format.VideoFormat;
import javax.media.protocol.ContentDescriptor;
import javax.media.protocol.DataSource;
import javax.media.protocol.PushBufferDataSource;
import javax.media.protocol.PushBufferStream;
import javax.media.rtp.RTPManager;
import javax.media.rtp.SendStream;

import jmapps.util.JMFUtils;

public class AVTransmit extends Thread {

	private CaptureDeviceInfo videoDevice, audioDevice;
	private MediaLocator locator;
	private String ipAddress;
	private int portBase;
	private boolean loopback; 	//回播标志
	private Processor processor = null;
	private RTPManager rtpMgrs[];
	private DataSource dataOutput = null;

	public AVTransmit(CaptureDeviceInfo vdi, CaptureDeviceInfo adi,
			String ipAddress, String pb,  boolean loopback) {
		this.videoDevice = vdi;
		this.audioDevice = adi;
		this.ipAddress = ipAddress;
		Integer integer = Integer.valueOf(pb);
		if (integer != null)
			this.portBase = integer.intValue();
		this.loopback = loopback;
	}

	/**
	 * 开始传输，如果一切正常返回空字符串。否则返回的字符串会描述启动失败的原因
	 */
	public synchronized String startTrans() {
		String result = null;

		// 通过指定的媒体位置(MediaLocator)创建一个处理器(Processor)
		// 并且对数据编码来产生JPEG/RTP格式的输出
		try {
			result = createProcessor();
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (result != null)
			return result;

		// 创建一个RTP会话(RTP session)来传播处理器产生的输出至指定的IP地址和端口号
		result = createTransmitter();
		if (result != null) {
			processor.close();
			processor = null;
			return result;
		}

		// 开始传送
		processor.start();

		return null;
	}

	/**
	 * 将已经开始的传输关闭
	 */
	public void stopTrans() {
		synchronized (this) {
			if (processor != null) {
				processor.stop();
				processor.close();
				processor = null;
				for (int i = 0; i < rtpMgrs.length; i++) {
					System.err.println("轨道" + i + "传输中止");
					rtpMgrs[i].removeTargets("会话结束.");
					rtpMgrs[i].dispose();
				}
			}
		}
	}

	private String createProcessor() throws Exception {
		if ((videoDevice == null) && (audioDevice == null))
			return "媒体位置不存在";

		DataSource ds = null;
		DataSource videoDataSource = null; // 视频源
		DataSource audioDataSource = null; // 音频源
		MediaLocator videoLocator = null;
		MediaLocator audioLocator = null;
		// 注意：必须先初始化videoDataSource，因为可能出现异常
		if (videoDevice != null) {
			videoLocator = videoDevice.getLocator();
			try {
				videoDataSource = javax.media.Manager
						.createDataSource(videoLocator);
			} catch (Exception e) {
				videoDataSource = javax.media.Manager
						.createDataSource(videoLocator);
			}
		}
		if (audioDevice != null) {
			audioLocator = audioDevice.getLocator();
			try {
				audioDataSource = javax.media.Manager
						.createDataSource(audioLocator);
			} catch (Exception e) {
				audioDataSource = javax.media.Manager
						.createDataSource(audioLocator);
			}
		}
		// 如果只有一个数据源，那么就取用这个数据源；如果有两个，便将这两个数据源进行合并
		if (videoDataSource == null && audioDataSource != null) {
			ds = audioDataSource;
		} else if (videoDataSource != null && audioDataSource == null) {
			ds = videoDataSource;
		} else if (videoDataSource != null && audioDataSource != null) {
			ds = Manager.createMergingDataSource(new DataSource[] {
					videoDataSource, audioDataSource });
		}

		// 尝试创建一个处理器来处理本地媒体输入
		try {
			processor = javax.media.Manager.createProcessor(ds);
		} catch (NoProcessorException npe) {
			return "无法创建处理器";
		} catch (IOException ioe) {
			return "创建处理器过程中出现IOException";
		}

		// 等待处理器配置完成
		boolean result = waitForState(processor, Processor.Configured);
		if (result == false)
			return "无法配置处理器";

		// 获得处理器的轨道信息
		TrackControl[] tracks = processor.getTrackControls();

		// 判断是否至少有一个轨道
		if (tracks == null || tracks.length < 1)
			return "无法在处理器中发现媒体轨道";

		// 将输出内容的格式设置为RAW_RTP
		// 这将会限制从Track.getSupportedFormats返回的支持格式
		// 只能是有效的RTP格式
		ContentDescriptor cd = new ContentDescriptor(ContentDescriptor.RAW_RTP);
		processor.setContentDescriptor(cd);

		Format supported[];
		Format chosen;
		boolean atLeastOneTrack = false;

		// 解析轨道
		for (int i = 0; i < tracks.length; i++) {
			if (tracks[i].isEnabled()) {

				supported = tracks[i].getSupportedFormats();
				// 因为之前已经设置了输出内容的格式为RAW_RTP
				// 所以得到的所有支持的格式都能在RTP协议下传输
				// 为了方便，我们只选取第一种
				if (supported.length > 0) {
					if (supported[0] instanceof VideoFormat) {
						// 对于视频格式，我们必须进一步测试视频的尺寸
						// 因为不是所有的格式都能在各种尺寸下工作
						chosen = checkForVideoSizes(tracks[i].getFormat(),
								supported[0]);
					} else
						chosen = supported[0];
					tracks[i].setFormat(chosen);
					System.err.print("轨道 " + i + " 被设定的传输格式为:");
					System.err.println("  " + chosen);
					atLeastOneTrack = true;
				} else
					tracks[i].setEnabled(false);
			} else
				tracks[i].setEnabled(false);
		}

		if (!atLeastOneTrack)
			// 不能为任何轨道设定有效的RTP格式
			return "不能为任何轨道设定有效的RTP格式";

		// 实例化处理器。这里将会隐含地创建一个图像并且尝试创建一个JPEG/RTP格式的输出数据源
		result = waitForState(processor, Controller.Realized);
		if (result == false)
			return "无法使处理器就绪";

		// 将JPEG质量设置围0.5。经验上看这是最优的
		setJPEGQuality(processor, 0.5f);

		// 获得处理器的输出数据源
		dataOutput = processor.getDataOutput();

		return null;
	}

	/**
	 * 用RTPManager为处理器的每一个媒体轨道创建会话
	 */
	private String createTransmitter() {

		// 不安全的写法。正确的做法是先检查格式。
		// 但是因为事先已经设置为RTP_RAW，所以可以跳过检查步骤
		PushBufferDataSource pbds = (PushBufferDataSource) dataOutput;
		PushBufferStream pbss[] = pbds.getStreams();

		rtpMgrs = new RTPManager[pbss.length];
		SendStream sendStream;
		int port;
		for (int i = 0; i < pbss.length; i++) {
			try {
				rtpMgrs[i] = RTPManager.newInstance();

				port = portBase + 2 * i;

				// 使用RTPSocketAdapter来初始化RTPManager
				rtpMgrs[i].initialize(new RTPSocketAdapter(InetAddress
						.getByName(ipAddress), port, loopback));

				System.err.println("创建RTP会话: " + ipAddress + " " + port);

				sendStream = rtpMgrs[i].createSendStream(dataOutput, i);
				sendStream.start();
			} catch (Exception e) {
				return e.getMessage();
			}
		}

		return null;
	}

	/**
	 * 对于JPEG和H.263的格式，只有特定的尺寸才能良好地传输，所以我们要做一些额外的检查
	 */
	Format checkForVideoSizes(Format original, Format supported) {

		int width, height;
		Dimension size = ((VideoFormat) original).getSize();
		Format jpegFmt = new Format(VideoFormat.JPEG_RTP);
		Format h263Fmt = new Format(VideoFormat.H263_RTP);

		if (supported.matches(jpegFmt)) {
			// 对于ＪＰＥＧ格式，我们要确保它的长与宽都要能被８整除
			width = (size.width % 8 == 0 ? size.width
					: (int) (size.width / 8) * 8);
			height = (size.height % 8 == 0 ? size.height
					: (int) (size.height / 8) * 8);
		} else if (supported.matches(h263Fmt)) {
			// 对于H.263格式，只有几个特定的尺寸被支持
			if (size.width < 128) {
				width = 128;
				height = 96;
			} else if (size.width < 176) {
				width = 176;
				height = 144;
			} else {
				width = 352;
				height = 288;
			}
		} else {
			// 对于其它的格式不做处理
			return supported;
		}

		return (new VideoFormat(null, new Dimension(width, height),
				Format.NOT_SPECIFIED, null, Format.NOT_SPECIFIED))
				.intersects(supported);
	}

	/**
	 * 设置JPEG编码的质量，推荐0.5
	 */
	void setJPEGQuality(Player p, float val) {

		Control cs[] = p.getControls();
		QualityControl qc = null;
		VideoFormat jpegFmt = new VideoFormat(VideoFormat.JPEG);

		for (int i = 0; i < cs.length; i++) {

			if (cs[i] instanceof QualityControl && cs[i] instanceof Owned) {
				Object owner = ((Owned) cs[i]).getOwner();

				if (owner instanceof Codec) {
					Format fmts[] = ((Codec) owner)
							.getSupportedOutputFormats(null);
					for (int j = 0; j < fmts.length; j++) {
						if (fmts[j].matches(jpegFmt)) {
							qc = (QualityControl) cs[i];
							qc.setQuality(val);
							System.err.println("- Setting quality to " + val
									+ " on " + qc);
							break;
						}
					}
				}
				if (qc != null)
					break;
			}
		}
	}

	/****************************************************************
	 * 协调处理器状态的简易方法
	 ****************************************************************/

	private Integer stateLock = new Integer(0); // 对象锁
	private boolean failed = false;

	Integer getStateLock() {
		return stateLock;
	}

	void setFailed() {
		failed = true;
	}

	private synchronized boolean waitForState(Processor p, int state) {
		p.addControllerListener(new StateListener());
		failed = false;

		if (state == Processor.Configured) {
			p.configure();
		} else if (state == Processor.Realized) {
			p.realize();
		}

		while (p.getState() < state && !failed) {
			synchronized (getStateLock()) {
				try {
					getStateLock().wait();
				} catch (InterruptedException ie) {
					return false;
				}
			}
		}

		if (failed)
			return false;
		else
			return true;
	}

	/****************************************************************
	 * 处理器的状态监听器
	 ****************************************************************/

	class StateListener implements ControllerListener {

		public void controllerUpdate(ControllerEvent ce) {

			// 如果在配置或者实例化的时候出现错误
			// 那么处理器将被关闭
			if (ce instanceof ControllerClosedEvent)
				setFailed();

			// 所有的控制器事件都会发送唤醒通知给在waitForState方法中等待的线程
			if (ce instanceof ControllerEvent) {
				synchronized (getStateLock()) {
					getStateLock().notifyAll();
				}
			}
		}
	}

	public void run() {
		// 开始传播
		String result = startTrans();

		// 如果result不为null则表示产生了错误。result字符串描述了错误信息。将错误输出。
		if (result != null) {
			System.err.println("错误 : " + result);
			System.exit(0);
		}
	}

	static void prUsage() {
		System.err
				.println("格式应为: AVTransmit <sourceURL> <destIP> <destPortBase>");
		System.err.println("  其中  <sourceURL>: 输入源的URL或者文件地址");
		System.err.println("  其中  <destIP>: 传输目的地的地址，可以是广播、组播或者单播地址");
		System.err.println("  其中  <destPortBase>: 传输目的地的地址的端口");
		System.err
				.println("                     媒体流的第一个轨道会使用<destPortBase>对应的端口.");
		System.err.println("                     接下来各个轨道使用的端口号依次加2\n");
		return;
	}
}