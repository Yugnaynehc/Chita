package edu.chita.helper;

import java.util.*;
import java.io.*;
import java.net.*;

import javax.swing.JTextArea;

public class DetectInternet {
	static int ip1;
	static int ip2;
	static int ip3;
	static int ip4 = 0;
	static int ipCount = 4;
	static FileWriter writer = null;
	static String regex1 = "TTL";
	static String regex2 = "Request timed out";
	static String regex3 = "无";
	static String regex4 = "unreachable";
	static SortedSet<String> ips = new TreeSet<String>();

	/**
	 * 启动接口
	 * @param ip1
	 * @param ip2
	 * @param ip3
	 */
	public static void setup(int ip1, int ip2, int ip3) {
		try {
			writer = new FileWriter("ip_alive.txt");
		} catch (Exception e) {
			System.out.println("Exception");
		}
		DetectInternet detecInternet = new DetectInternet(ip1, ip2, ip3);
		int ip3Max = 255;
		int ip4Max = 255;
		Vector<ScanThread> allThreads = new Vector<ScanThread>();
		while (ip3 < ip3Max && ip4 < ip4Max) {
			Thread scanner = new ScanThread(ip3, ip4);
			scanner.start();
			allThreads.add((ScanThread) scanner);
			ip4 += ipCount;
			
			if (ip4 > ip4Max) {
				/*
				ip3++;
				ip4 = 0;
				System.out.println(ip4);
				*/
				break;
			}
		}
		while (true) {
			boolean allRun = false;
			for (ScanThread t : allThreads) {
				if (t.threadRun != false) {
					allRun = true;
					break;
				}
			}
			try {
				Thread.sleep(100);
			} catch (Exception e) {
			}
			if (!allRun) {
				System.out.println("all thread scan over... ");
				try {
					Iterator<String> it = ips.iterator();
					System.out.println(ips.size() + " ips are alive");	
					while (it.hasNext()) {
						String ip = it.next();
						System.out.println(ip);
						writer.write(ip + "\r\n");
					}
					writer.close();
				} catch (Exception e) {
				}
				break;
			}
		}
	}

	public DetectInternet(int ip1, int ip2, int ip3) {
		DetectInternet.ip1 = ip1;
		DetectInternet.ip2 = ip2;
		DetectInternet.ip3 = ip3;
		//stringBuffer = new StringBuffer();
	}

}

/**
 * 扫描线程
 * @author Feather
 *
 */
class ScanThread extends Thread {
	int ip3;
	int ip4;

	public boolean threadRun;

	public ScanThread(int ip3, int ip4) {
		this.ip3 = ip3;
		this.ip4 = ip4;
		// System.out.println(this + " scanning from " + ip3 + "." + ip4 +
		// " to " + ip3 + "." + (ip4 + DetectInternet.ipCount - 1));

	}

	/**
	 * 检测IP是否合法
	 * @param ip
	 * @return
	 * @throws Exception
	 */
	private boolean checkIP(String ip) throws Exception {
		Runtime runtime = Runtime.getRuntime();
		String cmd = "ping " + ip;
		Process proc = runtime.exec(cmd);
		BufferedReader theReader = new BufferedReader(new InputStreamReader(
				proc.getInputStream()));
		String infor = theReader.readLine();
		;
		boolean alive = false;
		while (infor != null) {
			// System.out.println(infor);
			// if (infor.startsWith("Reply from"))
			// if (infor.startsWith("来自"))
			// if (infor.matches(regex))
			if (infor.indexOf(DetectInternet.regex1) != -1) {
				alive = true;
				break;
			} else if (infor.indexOf(DetectInternet.regex4) != -1
					|| infor.indexOf(DetectInternet.regex3) != -1) {
				break;
			}
			infor = theReader.readLine();
		}
		theReader.close();
		proc.destroy();
		return alive;
	}

	/**
	 * 线程启动
	 */
	public void run() {
		threadRun = true;
		for (int i = 0; i < DetectInternet.ipCount; i++) {
			try {
				if (ip3 > 255 || ip3 < 0) {
					ip3 = 255;
				}
				if (ip4 > 255 || ip4 < 0) {
					ip4 = 255;
				}
				String ip = DetectInternet.ip1 + "." + DetectInternet.ip2 + "."
						+ ip3 + "." + ip4;
				boolean alive = checkIP(ip);
				if (alive) {
					//System.out.println(ip + " is alive ");
					String ip3str = "";
					String ip4str = "";
	
					ip3str += ip3;
					ip4str += ip4;

					String hostName = InetAddress.getByName(ip)
							.getCanonicalHostName();
					ip = DetectInternet.ip1 + "." + DetectInternet.ip2 + "."
							+ ip3str + "." + ip4str + "      " + hostName;
					
					DetectInternet.ips.add(ip);
				} else {
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			ip4++;
		}
		threadRun = false;
		// System.out.println(this + " scann over... ");
	}
}
