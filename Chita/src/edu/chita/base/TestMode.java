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
		initComponents(); // ��ʼ�����Ӵ��ڵ����
		initWorker(); // ��ʼ����̨

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
	 * ��ʼ�����
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
		messageLabel = new JLabel("���ڲ����豸...");
		progressbarPanel.add(messageLabel);
		progressbarPanel.add(progressBar);

		Container content = this.getContentPane();
		cards = new CardLayout();
		content.setLayout(cards);
		content.add(progressbarPanel, PROGRESS_CARD);

		pack();
	}

	/**
	 * ���ø������豸�͸�ʽ����Player
	 * 
	 */
	private Player createPlayer(CaptureDeviceInfo videoDevice,
			Format videoFormat, CaptureDeviceInfo audioDevice,
			Format audioFormat) throws IncompatibleSourceException,
			IOException, NoPlayerException, CannotRealizeException {
		DataSource mergeDataSource = null; // �ϲ�����Ƶ��ƵԴ
		DataSource videoDataSource = null; // ��ƵԴ
		DataSource audioDataSource = null; // ��ƵԴ
		// ע�⣺�����ȳ�ʼ��videoDataSource����Ϊ���ܳ����쳣
		if (videoDevice != null) {
			// ʵ����������ܳ���java.io.IOException: Could not connect to capture
			// device�������ǲ����׳��쳣
			videoDataSource = JMFUtils.initializeCaptureDataSource(null,
					videoDevice.getName(), videoFormat);
			// ����������Ӷ���������С�java.io.IOException: Could not connect to capture
			// device������쳣�Ļ����������׳�
			videoDataSource.connect();
		}
		if (audioDevice != null) {
			audioDataSource = JMFUtils.initializeCaptureDataSource(null,
					audioDevice.getName(), audioFormat);
		}
		// ���ֻ��һ������Դ����ô��ȡ���������Դ��������������㽫����������Դ���кϲ�
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
	 * ��ʼ����̨worker
	 */
	private void initWorker() {

		/**
		 * javax.swing.SwingWorker����Java SE 6���³��ֵ��࣬ʹ��SwingWorker��
		 * ����������һ�������߳����첽��ѯ�������Ϸ���EDT�̡߳���ʾ��ʹ��SwingWorker��
		 * �¼������������أ�����EDT����ִ�к�����UI�¼���ʹ��Swingworker����һ�������߳�
		 * ���������Ӧ���档SwingWorker�Ķ������£� public abstract class SwingWorker extends
		 * Object implements RunnableFuture
		 * SwingWorker�ǳ����࣬��˱���̳�������ִ��������ض����񡣸������������Ͳ�����T��V
		 * T��doInBackground��get�����ķ������ͣ�V��publish��process����Ҫ������������͡�
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
						messageLabel.setText("�������ӵ������豸..");
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
					// �����쳣����playerʧ�ܣ��ٴγ���
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
	 * �����������
	 * 
	 * @return
	 */
	private JPanel createVisualPanel() {
		visualPanel = new JPanel();
		if (player == null) {
			visualPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
			JLabel lable = new JLabel("û���豸����û���ҵ����ʵ��豸��");
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
		final JButton startButton = new JButton("��ʼ");
		final JButton stopButton = new JButton("ֹͣ");
		final JButton recordButton = new JButton("¼��");
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
				// �ͷ�������Դ
				player.deallocate();
				startButton.setEnabled(true);
				stopButton.setEnabled(false);
			}
		});
		recordButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				System.out.println("��Ǹ��¼�ƹ��ܻ�û����� o(�s���t)o");		
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
	 * �����ӿ�
	 */
	public static void setup() {
		// ��LAF����ΪNimbus
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
