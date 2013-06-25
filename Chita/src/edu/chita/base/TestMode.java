package edu.chita.base;

import edu.chita.helper.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import javax.media.*;
import javax.media.protocol.DataSource;
import javax.swing.*;

import jmapps.util.JMFUtils;

public class TestMode extends JFrame {

	private static final String PROGRESS_CARD = "progress card";
	private static final String CONFIG_CARD = "config card";
	private static final String VISUAL_CARD = "visual card";
	private static final String WORKE_FINISH = "finsh";
	private CardLayout cards;
	private CaptureConfigPanel configPanel;
	private JPanel visualPanel;
	private JPanel progressbarPanel;
	private JLabel messageLabel;
	private JProgressBar progressBar;
	private Player player;
	private SwingWorker<?, ?> detectCaptureDeviceWorker;
	private SwingWorker<?, ?> connectCaputreDeviceWorker;

	public TestMode() {
		this.setTitle("Chita");
		initComponents(); // 初始化可视窗口的组件
		initWorker(); // 初始化后台

		detectCaptureDeviceWorker.execute();

		this.addWindowListener(new WindowAdapter() {

			@Override
			public void windowClosing(WindowEvent e) {
				if (player != null) {
					player.close();
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

			private static final long serialVersionUID = -1010000267122711169L;

			@Override
			public Insets getInsets() {
				Dimension frameSize = TestMode.this.getSize();
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
	 * 利用给定的设备和格式构建Player
	 * 
	 */
	private Player createPlayer(CaptureDeviceInfo videoDevice,
			Format videoFormat, CaptureDeviceInfo audioDevice,
			Format audioFormat) throws IncompatibleSourceException,
			IOException, NoPlayerException, CannotRealizeException {
		DataSource mergeDataSource = null; // 合并的音频视频源
		DataSource videoDataSource = null; // 视频源
		DataSource audioDataSource = null; // 音频源
		// 注意：必须先初始化videoDataSource，因为可能出现异常
		if (videoDevice != null) {
			// 实际上这里可能出现java.io.IOException: Could not connect to capture
			// device”，但是不会抛出异常
			videoDataSource = JMFUtils.initializeCaptureDataSource(null,
					videoDevice.getName(), videoFormat);
			// 这里添加连接动作是如果有“java.io.IOException: Could not connect to capture
			// device”这个异常的话，在这里抛出
			videoDataSource.connect();
		}
		if (audioDevice != null) {
			audioDataSource = JMFUtils.initializeCaptureDataSource(null,
					audioDevice.getName(), audioFormat);
		}
		// 如果只有一个数据源，那么就取用这个数据源；如果有两个，便将这两个数据源进行合并
		if (videoDataSource == null && audioDataSource != null) {
			mergeDataSource = audioDataSource;
		} else if (videoDataSource != null && audioDataSource == null) {
			mergeDataSource = videoDataSource;
		} else if (videoDataSource != null && audioDataSource != null) {
			mergeDataSource = Manager.createMergingDataSource(new DataSource[] {
					videoDataSource, audioDataSource });
		}
		return Manager.createRealizedPlayer(mergeDataSource);
	}

	/**
	 * 初始化后台worker
	 */
	private void initWorker() {

		/**
		 * javax.swing.SwingWorker类是Java SE 6中新出现的类，使用SwingWorker，
		 * 程序能启动一个任务线程来异步查询，并马上返回EDT线程。显示了使用SwingWorker后，
		 * 事件处理立即返回，允许EDT继续执行后续的UI事件。使用Swingworker启动一个任务线程
		 * 可以灵活响应界面。SwingWorker的定义如下： public abstract class SwingWorker extends
		 * Object implements RunnableFuture
		 * SwingWorker是抽象类，因此必须继承它才能执行所需的特定任务。该类有两个类型参数：T及V
		 * T是doInBackground和get方法的返回类型，V是publish和process方法要处理的数据类型。
		 * */

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
						cards.show(TestMode.this.getContentPane(),
								PROGRESS_CARD);
						connectCaputreDeviceWorker.execute();
					}
				};
				TestMode.this.getContentPane().add(configPanel, CONFIG_CARD);
				cards.show(TestMode.this.getContentPane(), CONFIG_CARD);
			}
		};

		connectCaputreDeviceWorker = new SwingWorker<Object, Object>() {

			@Override
			protected Object doInBackground() throws Exception {
				CaptureDeviceInfo videoDevice = configPanel.getVideoDevice();
				Format videoFormat = configPanel.getVideoFormat();
				CaptureDeviceInfo audioDevice = configPanel.getAudioDevice();
				Format audioFormat = configPanel.getAudioFormat();
				try {
					player = createPlayer(videoDevice, videoFormat,
							audioDevice, audioFormat);
					
				} catch (Exception e) {
					// 发生异常构建player失败，再次尝试
					if (player == null) {
						player = createPlayer(videoDevice, videoFormat,
								audioDevice, audioFormat);
					}
				}

				return WORKE_FINISH;
			}

			@Override
			protected void done() {
				TestMode.this.getContentPane().add(createVisualPanel(),
						VISUAL_CARD);
				cards.show(TestMode.this.getContentPane(), VISUAL_CARD);

			}
		};

	}

	/**
	 * 构建可视面板
	 * 
	 * @return
	 */
	private JPanel createVisualPanel() {
		visualPanel = new JPanel();
		if (player == null) {
			visualPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
			JLabel lable = new JLabel("没有设备或者没有找到合适的设备！");
			visualPanel.add(lable);
			return visualPanel;
		}
		visualPanel.setLayout(new BorderLayout());
		Component visualComponent = player.getVisualComponent();

		if (visualComponent != null) {
			visualComponent.setBackground(new Color(73, 74, 88));
			visualPanel.add(visualComponent, BorderLayout.CENTER);
		}

		JPanel controlPanel = new JPanel();
		controlPanel.setLayout(new GridLayout(1, 3));
		final JButton startButton = new JButton("开始");
		final JButton stopButton = new JButton("停止");
		final JButton recordButton = new JButton("录制");
		startButton.setEnabled(true);
		stopButton.setEnabled(false);
		recordButton.setEnabled(false);
		startButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				player.start();
				startButton.setEnabled(false);
				stopButton.setEnabled(true);
				recordButton.setEnabled(true);
			}
		});
		stopButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				player.stop();
				// 释放无用资源
				player.deallocate();
				startButton.setEnabled(true);
				stopButton.setEnabled(false);
			}
		});
		recordButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				System.out.println("抱歉，录制功能还没有完成 o(╯□╰)o");		
			}
		});
		
		
		controlPanel.add(startButton);
		controlPanel.add(stopButton);
		//controlPanel.add(recordButton);

		Component audioComponent = player.getControlPanelComponent();
		if (audioComponent != null) {
			controlPanel.add(audioComponent);
		}

		visualPanel.add(controlPanel, BorderLayout.SOUTH);
		return visualPanel;
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
				TestMode frame = new TestMode();
				frame.setLocationRelativeTo(null);
				frame.setVisible(true);
			}
		});
	}
}
