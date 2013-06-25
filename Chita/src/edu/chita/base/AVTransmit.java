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
	private boolean loopback; 	//�ز���־
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
	 * ��ʼ���䣬���һ���������ؿ��ַ��������򷵻ص��ַ�������������ʧ�ܵ�ԭ��
	 */
	public synchronized String startTrans() {
		String result = null;

		// ͨ��ָ����ý��λ��(MediaLocator)����һ��������(Processor)
		// ���Ҷ����ݱ���������JPEG/RTP��ʽ�����
		try {
			result = createProcessor();
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (result != null)
			return result;

		// ����һ��RTP�Ự(RTP session)�����������������������ָ����IP��ַ�Ͷ˿ں�
		result = createTransmitter();
		if (result != null) {
			processor.close();
			processor = null;
			return result;
		}

		// ��ʼ����
		processor.start();

		return null;
	}

	/**
	 * ���Ѿ���ʼ�Ĵ���ر�
	 */
	public void stopTrans() {
		synchronized (this) {
			if (processor != null) {
				processor.stop();
				processor.close();
				processor = null;
				for (int i = 0; i < rtpMgrs.length; i++) {
					System.err.println("���" + i + "������ֹ");
					rtpMgrs[i].removeTargets("�Ự����.");
					rtpMgrs[i].dispose();
				}
			}
		}
	}

	private String createProcessor() throws Exception {
		if ((videoDevice == null) && (audioDevice == null))
			return "ý��λ�ò�����";

		DataSource ds = null;
		DataSource videoDataSource = null; // ��ƵԴ
		DataSource audioDataSource = null; // ��ƵԴ
		MediaLocator videoLocator = null;
		MediaLocator audioLocator = null;
		// ע�⣺�����ȳ�ʼ��videoDataSource����Ϊ���ܳ����쳣
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
		// ���ֻ��һ������Դ����ô��ȡ���������Դ��������������㽫����������Դ���кϲ�
		if (videoDataSource == null && audioDataSource != null) {
			ds = audioDataSource;
		} else if (videoDataSource != null && audioDataSource == null) {
			ds = videoDataSource;
		} else if (videoDataSource != null && audioDataSource != null) {
			ds = Manager.createMergingDataSource(new DataSource[] {
					videoDataSource, audioDataSource });
		}

		// ���Դ���һ����������������ý������
		try {
			processor = javax.media.Manager.createProcessor(ds);
		} catch (NoProcessorException npe) {
			return "�޷�����������";
		} catch (IOException ioe) {
			return "���������������г���IOException";
		}

		// �ȴ��������������
		boolean result = waitForState(processor, Processor.Configured);
		if (result == false)
			return "�޷����ô�����";

		// ��ô������Ĺ����Ϣ
		TrackControl[] tracks = processor.getTrackControls();

		// �ж��Ƿ�������һ�����
		if (tracks == null || tracks.length < 1)
			return "�޷��ڴ������з���ý����";

		// ��������ݵĸ�ʽ����ΪRAW_RTP
		// �⽫�����ƴ�Track.getSupportedFormats���ص�֧�ָ�ʽ
		// ֻ������Ч��RTP��ʽ
		ContentDescriptor cd = new ContentDescriptor(ContentDescriptor.RAW_RTP);
		processor.setContentDescriptor(cd);

		Format supported[];
		Format chosen;
		boolean atLeastOneTrack = false;

		// �������
		for (int i = 0; i < tracks.length; i++) {
			if (tracks[i].isEnabled()) {

				supported = tracks[i].getSupportedFormats();
				// ��Ϊ֮ǰ�Ѿ�������������ݵĸ�ʽΪRAW_RTP
				// ���Եõ�������֧�ֵĸ�ʽ������RTPЭ���´���
				// Ϊ�˷��㣬����ֻѡȡ��һ��
				if (supported.length > 0) {
					if (supported[0] instanceof VideoFormat) {
						// ������Ƶ��ʽ�����Ǳ����һ��������Ƶ�ĳߴ�
						// ��Ϊ�������еĸ�ʽ�����ڸ��ֳߴ��¹���
						chosen = checkForVideoSizes(tracks[i].getFormat(),
								supported[0]);
					} else
						chosen = supported[0];
					tracks[i].setFormat(chosen);
					System.err.print("��� " + i + " ���趨�Ĵ����ʽΪ:");
					System.err.println("  " + chosen);
					atLeastOneTrack = true;
				} else
					tracks[i].setEnabled(false);
			} else
				tracks[i].setEnabled(false);
		}

		if (!atLeastOneTrack)
			// ����Ϊ�κι���趨��Ч��RTP��ʽ
			return "����Ϊ�κι���趨��Ч��RTP��ʽ";

		// ʵ���������������ｫ�������ش���һ��ͼ���ҳ��Դ���һ��JPEG/RTP��ʽ���������Դ
		result = waitForState(processor, Controller.Realized);
		if (result == false)
			return "�޷�ʹ����������";

		// ��JPEG��������Χ0.5�������Ͽ��������ŵ�
		setJPEGQuality(processor, 0.5f);

		// ��ô��������������Դ
		dataOutput = processor.getDataOutput();

		return null;
	}

	/**
	 * ��RTPManagerΪ��������ÿһ��ý���������Ự
	 */
	private String createTransmitter() {

		// ����ȫ��д������ȷ���������ȼ���ʽ��
		// ������Ϊ�����Ѿ�����ΪRTP_RAW�����Կ���������鲽��
		PushBufferDataSource pbds = (PushBufferDataSource) dataOutput;
		PushBufferStream pbss[] = pbds.getStreams();

		rtpMgrs = new RTPManager[pbss.length];
		SendStream sendStream;
		int port;
		for (int i = 0; i < pbss.length; i++) {
			try {
				rtpMgrs[i] = RTPManager.newInstance();

				port = portBase + 2 * i;

				// ʹ��RTPSocketAdapter����ʼ��RTPManager
				rtpMgrs[i].initialize(new RTPSocketAdapter(InetAddress
						.getByName(ipAddress), port, loopback));

				System.err.println("����RTP�Ự: " + ipAddress + " " + port);

				sendStream = rtpMgrs[i].createSendStream(dataOutput, i);
				sendStream.start();
			} catch (Exception e) {
				return e.getMessage();
			}
		}

		return null;
	}

	/**
	 * ����JPEG��H.263�ĸ�ʽ��ֻ���ض��ĳߴ�������õش��䣬��������Ҫ��һЩ����ļ��
	 */
	Format checkForVideoSizes(Format original, Format supported) {

		int width, height;
		Dimension size = ((VideoFormat) original).getSize();
		Format jpegFmt = new Format(VideoFormat.JPEG_RTP);
		Format h263Fmt = new Format(VideoFormat.H263_RTP);

		if (supported.matches(jpegFmt)) {
			// ���ڣʣУţǸ�ʽ������Ҫȷ�����ĳ����Ҫ�ܱ�������
			width = (size.width % 8 == 0 ? size.width
					: (int) (size.width / 8) * 8);
			height = (size.height % 8 == 0 ? size.height
					: (int) (size.height / 8) * 8);
		} else if (supported.matches(h263Fmt)) {
			// ����H.263��ʽ��ֻ�м����ض��ĳߴ类֧��
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
			// ���������ĸ�ʽ��������
			return supported;
		}

		return (new VideoFormat(null, new Dimension(width, height),
				Format.NOT_SPECIFIED, null, Format.NOT_SPECIFIED))
				.intersects(supported);
	}

	/**
	 * ����JPEG������������Ƽ�0.5
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
	 * Э��������״̬�ļ��׷���
	 ****************************************************************/

	private Integer stateLock = new Integer(0); // ������
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
	 * ��������״̬������
	 ****************************************************************/

	class StateListener implements ControllerListener {

		public void controllerUpdate(ControllerEvent ce) {

			// ��������û���ʵ������ʱ����ִ���
			// ��ô�����������ر�
			if (ce instanceof ControllerClosedEvent)
				setFailed();

			// ���еĿ������¼����ᷢ�ͻ���֪ͨ����waitForState�����еȴ����߳�
			if (ce instanceof ControllerEvent) {
				synchronized (getStateLock()) {
					getStateLock().notifyAll();
				}
			}
		}
	}

	public void run() {
		// ��ʼ����
		String result = startTrans();

		// ���result��Ϊnull���ʾ�����˴���result�ַ��������˴�����Ϣ�������������
		if (result != null) {
			System.err.println("���� : " + result);
			System.exit(0);
		}
	}

	static void prUsage() {
		System.err
				.println("��ʽӦΪ: AVTransmit <sourceURL> <destIP> <destPortBase>");
		System.err.println("  ����  <sourceURL>: ����Դ��URL�����ļ���ַ");
		System.err.println("  ����  <destIP>: ����Ŀ�ĵصĵ�ַ�������ǹ㲥���鲥���ߵ�����ַ");
		System.err.println("  ����  <destPortBase>: ����Ŀ�ĵصĵ�ַ�Ķ˿�");
		System.err
				.println("                     ý�����ĵ�һ�������ʹ��<destPortBase>��Ӧ�Ķ˿�.");
		System.err.println("                     �������������ʹ�õĶ˿ں����μ�2\n");
		return;
	}
}