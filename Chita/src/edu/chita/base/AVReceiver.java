package edu.chita.base;

import edu.chita.helper.*;
import java.awt.*;
import java.net.*;
import java.util.Vector;
import javax.media.*;
import javax.media.rtp.*;
import javax.media.rtp.event.*;
import javax.media.protocol.*;
import javax.media.control.BufferControl;

/**
 * AVReceiveͨ��RTPConnector������RTP��Ϣ
 */
public class AVReceiver extends Thread implements ReceiveStreamListener,
		SessionListener, ControllerListener {
	
	private boolean loopback; // ���ջز���־
	private volatile boolean stopRequested; // �жϱ�־
	String sessions[] = null;
	RTPManager mgrs[] = null;
	Vector playerWindows = null; // �����������б�
	Vector participants = null; //������б�
	boolean dataReceived = false;
	Object dataSync = new Object(); // ������

	/**
	 * ���캯��
	 * @param sessions
	 * @param loopback
	 */
	public AVReceiver(String sessions[], boolean loopback) {
		this.sessions = sessions;
		this.loopback = loopback;
	}

	/**
	 * ��ʼ������
	 * @return
	 */
	protected boolean initialize() {

		try {
			mgrs = new RTPManager[sessions.length];
			playerWindows = new Vector();
			participants = new Vector();

			SessionLabel session;

			// ��RTP�Ự
			for (int i = 0; i < sessions.length; i++) {

				// ��������ĻỰ��ַ�Ƿ�Ϸ�
				try {
					session = new SessionLabel(sessions[i]);
				} catch (IllegalArgumentException e) {
					System.err.println("�޷���������ĵ�ַ: " + sessions[i]);
					return false;
				}

				System.err.println("  - ��RTP�Ự��ַ: addr: " + session.addr
						+ " port: " + session.port + " ttl: " + session.ttl);

				mgrs[i] = (RTPManager) RTPManager.newInstance();
				mgrs[i].addSessionListener(this);
				mgrs[i].addReceiveStreamListener(this);

				// ͨ��RTPSocketAdapter������ʼ��RTPManager
				mgrs[i].initialize(new RTPSocketAdapter(InetAddress
						.getByName(session.addr), session.port + 15,
						session.ttl, loopback));

				// ��û��������ơ����ﻺ�����Ĵ�С�������������Ա������Ч��
				BufferControl bc = (BufferControl) mgrs[i]
						.getControl("javax.media.control.BufferControl");
				if (bc != null)
					bc.setBufferLength(350);
			}

		} catch (Exception e) {
			System.err.println("�޷�����RTP�Ự: " + e.getMessage());
			return false;
		}

		// �ڽ�����һ��֮ǰ�ȴ��������ĵ���

		long then = System.currentTimeMillis();
		long waitingPeriod = 60000; // ��ȴ�60��

		// ͬ������������ȷ�������Ự��˳������
		try {
			synchronized (dataSync) {
				while (!dataReceived
						&& (System.currentTimeMillis() - then < waitingPeriod)
						&& !stopRequested) {
					if (!dataReceived)
						System.err.println("  - �ȴ�RTP���ݵ���");
					dataSync.wait(1000);
				}
			}
		} catch (Exception e) {
		}

		if (!dataReceived) {
			System.err.println("ʼ���޷����յ�����.");
			close();
			return false;
		}

		return true;
	}

	public boolean isDone() {
		return playerWindows.size() == 0;
	}

	/**
	 * �رղ��������Ự������
	 */
	protected void close() {
		// ����رղ�����
		for (int i = 0; i < playerWindows.size(); i++) {
			try {
				((PlayerWindow) playerWindows.elementAt(i)).close();
			} catch (Exception e) {
			}
		}

		playerWindows.removeAllElements();

		// ����ر�RTP�Ự
		for (int i = 0; i < mgrs.length; i++) {
			if (mgrs[i] != null) {
				mgrs[i].removeTargets("�ر�����AVReceiver�ĻỰ");
				mgrs[i].dispose();
				mgrs[i] = null;
			}
		}
		System.err.println("������ֹ");
	}

	/**
	 * �ڲ����������б���Ѱ���ض��Ĳ�����
	 * @param p
	 * @return
	 */
	PlayerWindow find(Player p) {
		for (int i = 0; i < playerWindows.size(); i++) {
			PlayerWindow pw = (PlayerWindow) playerWindows.elementAt(i);
			if (pw.player == p)
				return pw;
		}
		return null;
	}

	PlayerWindow find(ReceiveStream strm) {
		for (int i = 0; i < playerWindows.size(); i++) {
			PlayerWindow pw = (PlayerWindow) playerWindows.elementAt(i);
			if (pw.stream == strm)
				return pw;
		}
		return null;
	}

	/**
	 * �Ự�������� һ�������Ự�¼������˺���
	 */
	public synchronized void update(SessionEvent evt) {
		if (evt instanceof NewParticipantEvent) {
			Participant p = ((NewParticipantEvent) evt).getParticipant();
			participants.add(p.getCNAME());	//���µ�����߼����б�
			System.err.println("  - һλ�µĲ����߼���: " + p.getCNAME());
		}
	}

	/**
	 * �����ռ������� һ�����������������˺���
	 */
	public synchronized void update(ReceiveStreamEvent evt) {

		RTPManager mgr = (RTPManager) evt.getSource();
		Participant participant = evt.getParticipant(); // �п���Ϊ��
		ReceiveStream stream = evt.getReceiveStream(); // �п���Ϊ��

		if (evt instanceof RemotePayloadChangeEvent) {

			System.err.println("  - �յ�RTP��Ч�غɸı�ʱ��.");
			System.err.println("��Ǹ���޷�������Ч�غɵĸı�.");
			System.exit(0);

		}

		else if (evt instanceof NewReceiveStreamEvent) {

			try {
				stream = ((NewReceiveStreamEvent) evt).getReceiveStream();
				DataSource ds = stream.getDataSource();

				// ȷ��ý�����ĸ�ʽ
				RTPControl ctl = (RTPControl) ds
						.getControl("javax.media.rtp.RTPControl");
				if (ctl != null) {
					System.err.println("  - �յ�һ���µ�RTPý����: " + ctl.getFormat());
				} else
					System.err.println("  - �յ�һ���µ�RTPý����");

				if (participant == null)
					System.err.println("      �޷�ȷ�����ķ�����.");
				else {
					System.err.println("      ����ý��������: "
							+ participant.getCNAME());
				}
				// ͨ����datasource����ý�������(Media Manager)������������(Player)
				Player p = javax.media.Manager.createPlayer(ds);
				if (p == null)
					return;

				p.addControllerListener(this);
				p.realize();
				PlayerWindow pw = new PlayerWindow(p, stream);
				playerWindows.addElement(pw); // ���´����Ĳ��������벥���������б�

				// ����initialize()�Ա��µ����ܹ�����
				synchronized (dataSync) {
					dataReceived = true;
					dataSync.notifyAll();
				}

			} catch (Exception e) {
				System.err.println("�����µ�ý����ʱ�����쳣" + e.getMessage());
				return;
			}

		}

		// �������ӳ���¼�
		else if (evt instanceof StreamMappedEvent) {

			if (stream != null && stream.getDataSource() != null) {
				DataSource ds = stream.getDataSource();
				// ȷ��ý������ʽ
				RTPControl ctl = (RTPControl) ds
						.getControl("javax.media.rtp.RTPControl");
				System.err.println("  - ����δȷ����Դ��ý���� ");
				if (ctl != null)
					System.err.println("      " + ctl.getFormat());
				System.err
						.println("      �Ѿ���ȷ��Ϊ������: " + participant.getCNAME());
			}
		}

		else if (evt instanceof ByeEvent) {

			System.err.println("  - һ���Ự������������: " + participant.getCNAME());
			PlayerWindow pw = find(stream);
			if (pw != null) {
				participants.removeElement(participant.getCNAME());
				pw.close();
				playerWindows.removeElement(pw);
			}
		}

	}

	/**
	 * �������Ŀ��Ƽ�������һ�������µ�ý������¼������˺���
	 */
	public synchronized void controllerUpdate(ControllerEvent ce) {

		Player p = (Player) ce.getSourceController();

		if (p == null)
			return;

		// ����������realized״̬ʱ
		if (ce instanceof RealizeCompleteEvent) {
			PlayerWindow pw = find(p);
			if (pw == null) {
				// �޷�������쳣����
				System.err.println("�ڲ�����!");
				System.exit(-1);
			}
			pw.initialize();
			pw.setVisible(true);
			pw.setLocationRelativeTo(null);
			p.start();
		}

		if (ce instanceof ControllerErrorEvent) {
			p.removeControllerListener(this);
			PlayerWindow pw = find(p);
			if (pw != null) {
				pw.close();
				playerWindows.removeElement(pw);
			}
			System.err.println("�ڲ�����: " + ce);
		}

	}

	/**
	 * ������������Ự��ַ���࣬�����ַ�Ϸ����൱������һ���ϸ��ǩ
	 */
	class SessionLabel {

		public String addr = null;
		public int port;
		public int ttl = 1;

		public SessionLabel(String session) throws IllegalArgumentException {

			int off;
			String portStr = null, ttlStr = null;

			if (session != null && session.length() > 0) {
				while (session.length() > 1 && session.charAt(0) == '/')
					session = session.substring(1);

				// �ж��Ƿ���ϵ�ַ��ʽ
				off = session.indexOf('/');
				if (off == -1) {
					if (!session.equals(""))
						addr = session;
				} else {
					addr = session.substring(0, off);
					session = session.substring(off + 1);
					// ��ȡ�˿�
					off = session.indexOf('/');
					if (off == -1) {
						if (!session.equals(""))
							portStr = session;
					} else {
						portStr = session.substring(0, off);
						session = session.substring(off + 1);
						// ��ȡTTL
						off = session.indexOf('/');
						if (off == -1) {
							if (!session.equals(""))
								ttlStr = session;
						} else {
							ttlStr = session.substring(0, off);
						}
					}
				}
			}

			if (addr == null)
				throw new IllegalArgumentException();

			if (portStr != null) {
				try {
					Integer integer = Integer.valueOf(portStr);
					if (integer != null)
						port = integer.intValue();
				} catch (Throwable t) {
					throw new IllegalArgumentException();
				}
			} else
				throw new IllegalArgumentException();

			if (ttlStr != null) {
				try {
					Integer integer = Integer.valueOf(ttlStr);
					if (integer != null)
						ttl = integer.intValue();
				} catch (Throwable t) {
					throw new IllegalArgumentException();
				}
			}
		}
	}

	/**
	 * �������������GUI
	 */
	class PlayerWindow extends Frame {

		Player player;
		ReceiveStream stream;

		PlayerWindow(Player p, ReceiveStream strm) {
			player = p;
			stream = strm;
		}

		public void initialize() {
			add(new PlayerPanel(player));
		}

		public void close() {
			player.close();
			setVisible(false);
			dispose();
		}

		public void addNotify() {
			super.addNotify();
			pack();
		}
	}

	/**
	 * ��������GUI
	 */
	class PlayerPanel extends Panel {

		Component vc, cc;

		PlayerPanel(Player p) {
			setLayout(new BorderLayout());
			if ((vc = p.getVisualComponent()) != null)
				add("Center", vc);
			if ((cc = p.getControlPanelComponent()) != null)
				add("South", cc);
		}

		public Dimension getPreferredSize() {
			int w = 0, h = 0;
			if (vc != null) {
				Dimension size = vc.getPreferredSize();
				w = size.width;
				h = size.height;
			}
			if (cc != null) {
				Dimension size = cc.getPreferredSize();
				if (w == 0)
					w = size.width;
				h += size.height;
			}
			if (w < 160)
				w = 160;
			return new Dimension(w, h);
		}
	}

	/**
	 * ����������б�
	 * @return
	 */
	public Vector getParticipants() {
		return participants;
	}

	public void run() {
		stopRequested = false;
		if (!initialize()) {
			System.err.println("��ʼ���Ựʧ��");
			System.exit(-1);
		}
		while (!stopRequested)
			;
	}

	public static void main(String argv[]) {
		if (argv.length == 0)
			prUsage();

		AVReceiver avReceive = new AVReceiver(argv, false);
		if (!avReceive.initialize()) {
			System.err.println("��ʼ���Ựʧ��");
			System.exit(-1);
		}

		// ÿ��һ����Ự�Ƿ����
		try {
			while (!avReceive.isDone())
				Thread.sleep(1000);
		} catch (Exception e) {
		}

		System.err.println("�˳�");
		return;
	}

	/**
	 * ��ʽУ��
	 */
	static void prUsage() {
		System.err.println("��ʽӦΪ: AVReceive <session> <session> ");
		System.err.println("    ���У�<session>: <address>/<port>/<ttl>");
		return;
	}
}