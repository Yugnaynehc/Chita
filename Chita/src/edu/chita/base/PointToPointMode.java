package edu.chita.base;

import edu.chita.helper.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Vector;

import javax.media.*;
import javax.swing.*;

public class PointToPointMode extends JFrame {
	
	private AVTransmit transmiter;
	private AVReceiver receiver;
	private RTPExport exporter;
	private static final String PROGRESS_CARD = "progress card";
	private static final String CONFIG_CARD = "config card";
	private static final String EDIT_CARD = "edit card";
	private static final String VISUAL_CARD = "visual card";
	private static final String WORKE_FINISH = "finsh";
	private String stringTittle = "<html>欢迎使用对话模式~<br>由于我的硬件选择的模块还没有被写好，所以有时视频通话的质量不好，你能帮我解决这个问题吗？</html>";
	private String stringExplain = "<html>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;请在IP地址栏填入对方的IP地址，在端口栏中<br>填入一个1024-65510范围内的数字。但请注意双<br>方填写的端口号要一致，如果发现通话失败那么请<br>更换端口，因为我没有写端口检测的功能。<br>就这么多了，Enjoy it！</html>";
	private String[] transmitArgs;
	private String[] receiveArgs;
	private String[] exportArgs;
	private boolean hasVideo;
	private boolean hasAudio;
	private JPanel progressbarPanel;
	private CaptureConfigPanel configPanel;
	private JPanel editTargetPanel;
	private JLabel messageLabel;
	private JPanel visualPanel;
	private JTextArea messageArea;
	private JScrollPane scrollPane;
	private JProgressBar progressBar;
	private CardLayout cards;
	private SwingWorker<?, ?> detectCaptureDeviceWorker;
	private SwingWorker<?, ?> connectNetWorker;
	private SwingWorker<?, ?> detectNetWorker;
	private CaptureDeviceInfo videoDevice;
	private CaptureDeviceInfo audioDevice;

	public PointToPointMode() {
		this.setTitle("Chita");
		initComponents(); // 初始化可视窗口的组件
		initWorker(); // 初始化后台

		// detectNetWorker.execute();
		detectCaptureDeviceWorker.execute();

		this.addWindowListener(new WindowAdapter() {

			@Override
			public void windowClosing(WindowEvent e) {
				try {
					disconnectNet();
				} catch (Exception ex) {
					System.exit(0);
				}
				System.exit(0);
			}
		});
	}

	/**
	 * 初始化组件
	 */
	private void initComponents() {
		progressbarPanel = new JPanel() {
			@Override
			public Insets getInsets() {
				Dimension frameSize = PointToPointMode.this.getSize();
				Dimension progressSize = progressBar.getPreferredSize();
				int d = (int) (frameSize.getHeight() - progressSize.getHeight() - 50) / 2;
				return new Insets(d, 0, d, 0);
			}
		};
		progressbarPanel.setPreferredSize(new Dimension(650, 520));
		progressBar = new JProgressBar();
		progressBar.setIndeterminate(true);
		messageLabel = new JLabel("正在捕获设备...");
		progressbarPanel.add(messageLabel);
		progressbarPanel.add(progressBar);

		Container content = this.getContentPane();
		cards = new CardLayout();
		content.setLayout(cards);
		content.add(progressbarPanel, PROGRESS_CARD);

		pack();
	}

	/**
	 ** 
	 * 初始化后台worker
	 */
	private void initWorker() {

		// 自动检测并连接设备
		detectCaptureDeviceWorker = new SwingWorker<Object, Object>() {

			@Override
			protected Object doInBackground() throws Exception {
				CaptureDeviceDetector detector = new CaptureDeviceDetector();
				detector.detectCaptureDevices();
				return WORKE_FINISH;
			}

			@Override
			protected void done() {
				configPanel = new CaptureConfigPanel() {
					@Override
					protected void cancelActionPerformed() {
						super.cancelActionPerformed();
						System.exit(0);
					}

					@Override
					protected void okActionPerformed() {
						super.okActionPerformed();
						messageLabel.setText("正在连接到捕获设备..");
						cards.show(PointToPointMode.this.getContentPane(),
								PROGRESS_CARD);
						detectNetWorker.execute();
					}
				};
				PointToPointMode.this.getContentPane().add(configPanel,
						CONFIG_CARD);
				cards.show(PointToPointMode.this.getContentPane(), CONFIG_CARD);
			}
		};

		// 检测网络
		detectNetWorker = new SwingWorker<Object, Object>() {

			@Override
			protected Object doInBackground() throws Exception {
				videoDevice = configPanel.getVideoDevice();
				if (videoDevice != null)
					hasVideo = true;
				audioDevice = configPanel.getAudioDevice();
				if (audioDevice != null)
					hasAudio = true;
				return WORKE_FINISH;
			}

			@Override
			protected void done() {
				editTargetPanel = new EditTargetPanel(stringTittle, stringExplain, false) {
					@Override
					protected void cancelActionPerformed() {
						super.cancelActionPerformed();
						System.exit(0);
					}

					@Override
					protected void okActionPerformed() {
						super.okActionPerformed();

						transmitArgs = ((EditTargetPanel) editTargetPanel)
								.getArgs();
						int port = Integer.valueOf(transmitArgs[1]);
						if (hasVideo && hasAudio) {
							receiveArgs = new String[2];
							exportArgs = new String[6];
							receiveArgs[0] = transmitArgs[0] + "/" + port;
							receiveArgs[1] = transmitArgs[0] + "/" + (port + 2);
							exportArgs[4] = transmitArgs[0] + ":" + port
									+ "/video";
							exportArgs[5] = transmitArgs[0] + ":" + (port + 2)
									+ "/audio";
						} else if (hasVideo) {
							receiveArgs = new String[1];
							exportArgs = new String[5];
							receiveArgs[0] = transmitArgs[0] + "/" + port;
							exportArgs[4] = transmitArgs[0] + ":" + port
									+ "/video";
						} else if (hasAudio) {
							receiveArgs = new String[1];
							exportArgs = new String[5];
							receiveArgs[0] = transmitArgs[0] + "/" + port;
							exportArgs[4] = transmitArgs[0] + ":" + port
									+ "/audio";
						}

						messageLabel.setText("正在连接到捕获设备..");
						cards.show(PointToPointMode.this.getContentPane(),
								PROGRESS_CARD);
						connectNetWorker.execute();
					}
				};

				PointToPointMode.this.getContentPane().add(editTargetPanel,
						EDIT_CARD);
				cards.show(PointToPointMode.this.getContentPane(), EDIT_CARD);
			}
		};

		connectNetWorker = new SwingWorker<Object, Object>() {
			@Override
			protected Object doInBackground() {
				return WORKE_FINISH;
			}

			@Override
			protected void done() {
				PointToPointMode.this.getContentPane().add(createVisualPanel(),
						VISUAL_CARD);
				cards.show(PointToPointMode.this.getContentPane(), VISUAL_CARD);
			}
		};

	}

	/**
	 * 构建可视面板
	 * 
	 */
	private JPanel createVisualPanel() {
		visualPanel = new JPanel();
		visualPanel.setLayout(new BorderLayout());

		messageArea = new JTextArea(25, 35);
		messageArea.setLineWrap(true);
		scrollPane = new JScrollPane(messageArea);
		visualPanel.add(scrollPane, BorderLayout.CENTER);

		// 将流重定向到messageArea
		System.setOut(new GUIPrintStream(System.out, messageArea));
		System.setErr(new GUIPrintStream(System.err, messageArea));

		JPanel controlPanel = new JPanel();
		controlPanel.setLayout(new GridLayout(1, 3));
		final JButton startReceiveButton = new JButton("开始接收");
		final JButton stopReceiveButton = new JButton("停止接收");
		final JButton startTransmitButton = new JButton("开始发送");
		final JButton stopTransmitButton = new JButton("停止发送");
		final JButton recordButton = new JButton("录制");
		startReceiveButton.setEnabled(true);
		startTransmitButton.setEnabled(true);
		stopReceiveButton.setEnabled(false);
		stopTransmitButton.setEnabled(false);
		recordButton.setEnabled(false);
		startReceiveButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				//connectNet();
				receiver = new AVReceiver(receiveArgs, false);
				receiver.start();
				startReceiveButton.setEnabled(false);
				stopReceiveButton.setEnabled(true);
				recordButton.setEnabled(true);
			}
		});
		stopReceiveButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				//disconnectNet();
				receiver.close();
				receiver.stop(); // 这是过时的方法，暂时不知道好的方法
				startReceiveButton.setEnabled(true);
				stopReceiveButton.setEnabled(false);
			}
		});
		startTransmitButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				//connectNet();
				transmiter = new AVTransmit(videoDevice, audioDevice, transmitArgs[0],
						transmitArgs[1], false);
				transmiter.start();
				startTransmitButton.setEnabled(false);
				stopTransmitButton.setEnabled(true);
				recordButton.setEnabled(true);
			}
		});
		stopTransmitButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				//disconnectNet();
				transmiter.stopTrans();
				startTransmitButton.setEnabled(true);
				stopTransmitButton.setEnabled(false);
			}
		});

		recordButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				/*
				int option;
				JTextField textfield = new JTextField();
				Object[] array = { "请输入", textfield };
				Object[] options = { "确定" };
				String textString = null;
				option = JOptionPane.showOptionDialog(null, array, "将文件保存到",
						JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null,
						options, options[0]);
				if (option == 0) {
					textString = textfield.getText();
				}
				exportArgs[0] = "-o";
				exportArgs[1] = "file:" + textString;
				exportArgs[2] = "-d";
				textfield.setText("");
				option = JOptionPane.showOptionDialog(null, array, "录制时长(秒)",
						JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null,
						options, options[0]);
				if (option == 0) {
					textString = textfield.getText();
				}
				exportArgs[3] = textString;
				exporter = new RTPExport(exportArgs);
				for (int i=0; i<6; ++i)
					System.err.println(exportArgs[i]);
				exporter.start();
				System.err.println("开始录制");
				*/
				System.err.println("抱歉，录制功能还没有完成 o(╯□╰)o");
			}
		});

		controlPanel.add(startReceiveButton);
		controlPanel.add(stopReceiveButton);
		controlPanel.add(startTransmitButton);
		controlPanel.add(stopTransmitButton);
		//controlPanel.add(recordButton);

		visualPanel.add(controlPanel, BorderLayout.SOUTH);
		return visualPanel;
	}

	/**
	 * 通过指定的参数来创建媒体传播对象
	 */
	private void connectNet() {
		// 
		transmiter = new AVTransmit(videoDevice, audioDevice, transmitArgs[0],
				transmitArgs[1], false);
		receiver = new AVReceiver(receiveArgs, false);
		transmiter.start();
		receiver.start();
	}

	/**
	 * 断开连接
	 */
	private void disconnectNet() {
		receiver.close();
		receiver.stop(); // 这是过时的方法，暂时不知道好的方法
		transmiter.stopTrans();
	}

	/**
	 * 启动接口
	 */
	public static void setup() {
		// 将LAF设置为Nimbus
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

		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				PointToPointMode frame = new PointToPointMode();
				frame.setLocationRelativeTo(null);
				frame.setVisible(true);
			}
		});
	}

}
