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
 * AVReceive通过RTPConnector来接受RTP消息
 */
public class AVReceiver extends Thread implements ReceiveStreamListener,
		SessionListener, ControllerListener {
	
	private boolean loopback; // 接收回播标志
	private volatile boolean stopRequested; // 中断标志
	String sessions[] = null;
	RTPManager mgrs[] = null;
	Vector playerWindows = null; // 播放器窗口列表
	Vector participants = null; //与会者列表
	boolean dataReceived = false;
	Object dataSync = new Object(); // 对象锁

	/**
	 * 构造函数
	 * @param sessions
	 * @param loopback
	 */
	public AVReceiver(String sessions[], boolean loopback) {
		this.sessions = sessions;
		this.loopback = loopback;
	}

	/**
	 * 初始化函数
	 * @return
	 */
	protected boolean initialize() {

		try {
			mgrs = new RTPManager[sessions.length];
			playerWindows = new Vector();
			participants = new Vector();

			SessionLabel session;

			// 打开RTP会话
			for (int i = 0; i < sessions.length; i++) {

				// 分析传入的会话地址是否合法
				try {
					session = new SessionLabel(sessions[i]);
				} catch (IllegalArgumentException e) {
					System.err.println("无法处理给定的地址: " + sessions[i]);
					return false;
				}

				System.err.println("  - 打开RTP会话地址: addr: " + session.addr
						+ " port: " + session.port + " ttl: " + session.ttl);

				mgrs[i] = (RTPManager) RTPManager.newInstance();
				mgrs[i].addSessionListener(this);
				mgrs[i].addReceiveStreamListener(this);

				// 通过RTPSocketAdapter类来初始化RTPManager
				mgrs[i].initialize(new RTPSocketAdapter(InetAddress
						.getByName(session.addr), session.port + 15,
						session.ttl, loopback));

				// 获得缓冲流控制。这里缓冲区的大小可以自行设置以便获得最好效果
				BufferControl bc = (BufferControl) mgrs[i]
						.getControl("javax.media.control.BufferControl");
				if (bc != null)
					bc.setBufferLength(350);
			}

		} catch (Exception e) {
			System.err.println("无法建立RTP会话: " + e.getMessage());
			return false;
		}

		// 在进行下一步之前等待数据流的到来

		long then = System.currentTimeMillis();
		long waitingPeriod = 60000; // 最长等待60秒

		// 同步方法，用来确保各个会话能顺利进行
		try {
			synchronized (dataSync) {
				while (!dataReceived
						&& (System.currentTimeMillis() - then < waitingPeriod)
						&& !stopRequested) {
					if (!dataReceived)
						System.err.println("  - 等待RTP数据到来");
					dataSync.wait(1000);
				}
			}
		} catch (Exception e) {
		}

		if (!dataReceived) {
			System.err.println("始终无法接收到数据.");
			close();
			return false;
		}

		return true;
	}

	public boolean isDone() {
		return playerWindows.size() == 0;
	}

	/**
	 * 关闭播放器及会话管理器
	 */
	protected void close() {
		// 依序关闭播放器
		for (int i = 0; i < playerWindows.size(); i++) {
			try {
				((PlayerWindow) playerWindows.elementAt(i)).close();
			} catch (Exception e) {
			}
		}

		playerWindows.removeAllElements();

		// 依序关闭RTP会话
		for (int i = 0; i < mgrs.length; i++) {
			if (mgrs[i] != null) {
				mgrs[i].removeTargets("关闭来自AVReceiver的会话");
				mgrs[i].dispose();
				mgrs[i] = null;
			}
		}
		System.err.println("接收中止");
	}

	/**
	 * 在播放器窗口列表中寻找特定的播放器
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
	 * 会话监听器， 一旦产生会话事件便进入此函数
	 */
	public synchronized void update(SessionEvent evt) {
		if (evt instanceof NewParticipantEvent) {
			Participant p = ((NewParticipantEvent) evt).getParticipant();
			participants.add(p.getCNAME());	//将新的与会者加入列表
			System.err.println("  - 一位新的参与者加入: " + p.getCNAME());
		}
	}

	/**
	 * 流接收监听器， 一旦产生接受流便进入此函数
	 */
	public synchronized void update(ReceiveStreamEvent evt) {

		RTPManager mgr = (RTPManager) evt.getSource();
		Participant participant = evt.getParticipant(); // 有可能为空
		ReceiveStream stream = evt.getReceiveStream(); // 有可能为空

		if (evt instanceof RemotePayloadChangeEvent) {

			System.err.println("  - 收到RTP有效载荷改变时间.");
			System.err.println("抱歉，无法处理有效载荷的改变.");
			System.exit(0);

		}

		else if (evt instanceof NewReceiveStreamEvent) {

			try {
				stream = ((NewReceiveStreamEvent) evt).getReceiveStream();
				DataSource ds = stream.getDataSource();

				// 确定媒体流的格式
				RTPControl ctl = (RTPControl) ds
						.getControl("javax.media.rtp.RTPControl");
				if (ctl != null) {
					System.err.println("  - 收到一个新的RTP媒体流: " + ctl.getFormat());
				} else
					System.err.println("  - 收到一个新的RTP媒体流");

				if (participant == null)
					System.err.println("      无法确认流的发送者.");
				else {
					System.err.println("      这条媒体流来自: "
							+ participant.getCNAME());
				}
				// 通过将datasource传入媒体管理器(Media Manager)来创建播放器(Player)
				Player p = javax.media.Manager.createPlayer(ds);
				if (p == null)
					return;

				p.addControllerListener(this);
				p.realize();
				PlayerWindow pw = new PlayerWindow(p, stream);
				playerWindows.addElement(pw); // 将新创建的播放器加入播放器窗口列表

				// 唤醒initialize()以便新的流能够到来
				synchronized (dataSync) {
					dataReceived = true;
					dataSync.notifyAll();
				}

			} catch (Exception e) {
				System.err.println("接收新的媒体流时出现异常" + e.getMessage());
				return;
			}

		}

		// 如果是流映射事件
		else if (evt instanceof StreamMappedEvent) {

			if (stream != null && stream.getDataSource() != null) {
				DataSource ds = stream.getDataSource();
				// 确定媒体流格式
				RTPControl ctl = (RTPControl) ds
						.getControl("javax.media.rtp.RTPControl");
				System.err.println("  - 事先未确认来源的媒体流 ");
				if (ctl != null)
					System.err.println("      " + ctl.getFormat());
				System.err
						.println("      已经被确认为发送自: " + participant.getCNAME());
			}
		}

		else if (evt instanceof ByeEvent) {

			System.err.println("  - 一个会话结束提醒来自: " + participant.getCNAME());
			PlayerWindow pw = find(stream);
			if (pw != null) {
				participants.removeElement(participant.getCNAME());
				pw.close();
				playerWindows.removeElement(pw);
			}
		}

	}

	/**
	 * 播放器的控制监听器，一旦产生新的媒体控制事件便进入此函数
	 */
	public synchronized void controllerUpdate(ControllerEvent ce) {

		Player p = (Player) ce.getSourceController();

		if (p == null)
			return;

		// 当放器处于realized状态时
		if (ce instanceof RealizeCompleteEvent) {
			PlayerWindow pw = find(p);
			if (pw == null) {
				// 无法处理的异常发生
				System.err.println("内部错误!");
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
			System.err.println("内部错误: " + ce);
		}

	}

	/**
	 * 这是用来处理会话地址的类，如果地址合法就相当于贴上一个合格标签
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

				// 判断是否符合地址格式
				off = session.indexOf('/');
				if (off == -1) {
					if (!session.equals(""))
						addr = session;
				} else {
					addr = session.substring(0, off);
					session = session.substring(off + 1);
					// 获取端口
					off = session.indexOf('/');
					if (off == -1) {
						if (!session.equals(""))
							portStr = session;
					} else {
						portStr = session.substring(0, off);
						session = session.substring(off + 1);
						// 获取TTL
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
	 * 播放器窗口组的GUI
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
	 * 播放器的GUI
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
	 * 返回与会者列表
	 * @return
	 */
	public Vector getParticipants() {
		return participants;
	}

	public void run() {
		stopRequested = false;
		if (!initialize()) {
			System.err.println("初始化会话失败");
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
			System.err.println("初始化会话失败");
			System.exit(-1);
		}

		// 每隔一秒检测会话是否结束
		try {
			while (!avReceive.isDone())
				Thread.sleep(1000);
		} catch (Exception e) {
		}

		System.err.println("退出");
		return;
	}

	/**
	 * 格式校验
	 */
	static void prUsage() {
		System.err.println("格式应为: AVReceive <session> <session> ");
		System.err.println("    其中，<session>: <address>/<port>/<ttl>");
		return;
	}
}