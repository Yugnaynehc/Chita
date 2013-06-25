package edu.chita.helper;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketException;

import javax.media.protocol.ContentDescriptor;
import javax.media.protocol.PushSourceStream;
import javax.media.protocol.SourceTransferHandler;
import javax.media.rtp.OutputDataStream;
import javax.media.rtp.RTPConnector;

/**
 * ����UDP�׽��ֵ�RTPConnector�ӿ�
 */
public class RTPSocketAdapter implements RTPConnector {

	DatagramSocket dataSock;
	DatagramSocket ctrlSock;
	boolean loopback;
	InetAddress addr;
	int port;

	SockInputStream dataInStrm = null, ctrlInStrm = null;
	SockOutputStream dataOutStrm = null, ctrlOutStrm = null;

	public RTPSocketAdapter(InetAddress addr, int port, boolean loopback) throws IOException {
		this(addr, port, 1, loopback);
	}

	public RTPSocketAdapter(InetAddress addr, int port, int ttl, boolean loopback)
			throws IOException {

		try {
			// ��������ĵ�ַ�Ƕಥ��ַ����ô����ಥ��
			if (addr.isMulticastAddress()) {
				dataSock = new MulticastSocket(port);
				ctrlSock = new MulticastSocket(port + 1);
				((MulticastSocket) dataSock).joinGroup(addr);
				((MulticastSocket) dataSock).setTimeToLive(ttl);
				((MulticastSocket) dataSock).setLoopbackMode(loopback);
				((MulticastSocket) ctrlSock).joinGroup(addr);
				((MulticastSocket) ctrlSock).setTimeToLive(ttl);
				((MulticastSocket) ctrlSock).setLoopbackMode(loopback);
			} 
			// �������ñ���IP�������ݱ��׽���
			else {
				dataSock = new DatagramSocket(port, InetAddress.getLocalHost());
				ctrlSock = new DatagramSocket(port + 1,
						InetAddress.getLocalHost());
			}

		} catch (SocketException e) {
			throw new IOException(e.getMessage());
		}

		this.addr = addr;
		this.port = port;
	}

	/**
	 * �õ�һ��������������RTP����
	 */
	public PushSourceStream getDataInputStream() throws IOException {
		if (dataInStrm == null) {
			dataInStrm = new SockInputStream(dataSock, addr, port);
			dataInStrm.start();
		}
		return dataInStrm;
	}

	/**
	 * �õ�һ�������������RTP����
	 */
	public OutputDataStream getDataOutputStream() throws IOException {
		if (dataOutStrm == null)
			dataOutStrm = new SockOutputStream(dataSock, addr, port + 15);
		return dataOutStrm;
	}

	/**
	 * �õ�һ��������������RTCP����(RTCP���ڿ���RTP����)
	 */
	public PushSourceStream getControlInputStream() throws IOException {
		if (ctrlInStrm == null) {
			ctrlInStrm = new SockInputStream(ctrlSock, addr, port + 1);
			ctrlInStrm.start();
		}
		return ctrlInStrm;
	}

	/**
	 * �õ�һ�������������RTCP����(RTCP���ڿ���RTP����)
	 */
	public OutputDataStream getControlOutputStream() throws IOException {
		if (ctrlOutStrm == null)
			ctrlOutStrm = new SockOutputStream(ctrlSock, addr, port + 15 + 1);
		return ctrlOutStrm;
	}

	/**
	 * �ر����е�RTP����RTCP��
	 */
	public void close() {
		if (dataInStrm != null)
			dataInStrm.kill();
		if (ctrlInStrm != null)
			ctrlInStrm.kill();
		dataSock.close();
		ctrlSock.close();
	}

	/**
	 * Set the receive buffer size of the RTP data channel. This is only a hint
	 * to the implementation. The actual implementation may not be able to do
	 * anything to this.
	 */
	public void setReceiveBufferSize(int size) throws IOException {
		dataSock.setReceiveBufferSize(size);
	}

	/**
	 * Get the receive buffer size set on the RTP data channel. Return -1 if the
	 * receive buffer size is not applicable for the implementation.
	 */
	public int getReceiveBufferSize() {
		try {
			return dataSock.getReceiveBufferSize();
		} catch (Exception e) {
			return -1;
		}
	}

	/**
	 * Set the send buffer size of the RTP data channel. This is only a hint to
	 * the implementation. The actual implementation may not be able to do
	 * anything to this.
	 */
	public void setSendBufferSize(int size) throws IOException {
		dataSock.setSendBufferSize(size);
	}

	/**
	 * Get the send buffer size set on the RTP data channel. Return -1 if the
	 * send buffer size is not applicable for the implementation.
	 */
	public int getSendBufferSize() {
		try {
			return dataSock.getSendBufferSize();
		} catch (Exception e) {
			return -1;
		}
	}

	/**
	 * Return the RTCP bandwidth fraction. This value is used to initialize the
	 * RTPManager. Check RTPManager for more details. Return -1 to use the
	 * default values.
	 */
	public double getRTCPBandwidthFraction() {
		return -1;
	}

	/**
	 * Return the RTCP sender bandwidth fraction. This value is used to
	 * initialize the RTPManager. Check RTPManager for more details. Return -1
	 * to use the default values.
	 */
	public double getRTCPSenderBandwidthFraction() {
		return -1;
	}

	/**
	 * ����ʵ�ֻ���UDP�׽��ֵ�������������ڲ���
	 */
	class SockOutputStream implements OutputDataStream {

		DatagramSocket sock;
		InetAddress addr;
		int port;

		public SockOutputStream(DatagramSocket sock, InetAddress addr, int port) {
			this.sock = sock;
			this.addr = addr;
			this.port = port;
		}

		public int write(byte data[], int offset, int len) {
			try {
				sock.send(new DatagramPacket(data, offset, len, addr, port));
			} catch (Exception e) {
				return -1;
			}
			return len;
		}
	}

	/**
	 * ����ʵ�ֻ���UDP�׽��ֵ������������ڲ���
	 */
	class SockInputStream extends Thread implements PushSourceStream {

		DatagramSocket sock;
		InetAddress addr;
		int port;
		boolean done = false;
		boolean dataRead = false;

		SourceTransferHandler sth = null;

		public SockInputStream(DatagramSocket sock, InetAddress addr, int port) {
			this.sock = sock;
			this.addr = addr;
			this.port = port;
		}

		public int read(byte buffer[], int offset, int length) {
			DatagramPacket p = new DatagramPacket(buffer, offset, length, addr,
					port);
			try {
				sock.receive(p);
			} catch (IOException e) {
				return -1;
			}
			synchronized (this) {
				dataRead = true;
				notify();
			}
			return p.getLength();
		}

		public synchronized void start() {
			super.start();
			if (sth != null) {
				dataRead = true;
				notify();
			}
		}

		public synchronized void kill() {
			done = true;
			notify();
		}

		public int getMinimumTransferSize() {
			return 2 * 1024;
			// twice the MTU size, just to be safe.
		}

		public synchronized void setTransferHandler(SourceTransferHandler sth) {
			this.sth = sth;
			dataRead = true;
			notify();
		}

		// Not applicable.
		public ContentDescriptor getContentDescriptor() {
			return null;
		}

		// Not applicable.
		public long getContentLength() {
			return LENGTH_UNKNOWN;
		}

		// Not applicable.
		public boolean endOfStream() {
			return false;
		}

		// Not applicable.
		public Object[] getControls() {
			return new Object[0];
		}

		// Not applicable.
		public Object getControl(String type) {
			return null;
		}

		/**
		 * Loop and notify the transfer handler of new data.
		 */
		public void run() {
			while (!done) {

				synchronized (this) {
					while (!dataRead && !done) {
						try {
							wait();
						} catch (InterruptedException e) {
						}
					}
					dataRead = false;
				}

				if (sth != null && !done) {
					sth.transferData(this);
				}
			}
		}
	}
}