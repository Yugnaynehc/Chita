package edu.chita.base;

import edu.chita.helper.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Vector;

import javax.media.*;
import javax.media.rtp.Participant;
import javax.swing.*;

public class MeetingMode extends JFrame {
	
	private AVTransmit transmiter;
	private AVReceiver receiver;
	private RTPExport exporter;
	private static final String PROGRESS_CARD = "progress card";
	private static final String CONFIG_CARD = "config card";
	private static final String EDIT_CARD = "edit card";
	private static final String VISUAL_CARD = "visual card";
	private static final String WORKE_FINISH = "finsh";
	private String stringTittle = "<html>��ӭʹ�û���ģʽ~<br>�ҿ��԰����ٿ�һ��������顣����¼��ģ��û��д�ã���ʱ��֧��¼�ƻ������ݡ����ܸ���һ�㽨����</html>";
	private String stringExplain = "<html>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;����IP��ַ������һ��D���ַ��Ĭ�ϵĵ�ַ<br>��һ���Ϸ���D���ַ�������޸ľͿ�ʹ�á��ڶ�<br>����������һ��1024-65510��Χ�ڵ����֡���ͳ<br>һʹ��ͬһ��IP���˿ڣ�ȷ�����С���Ա���ܼ�<br>�뵽��������</html>";
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

	/**
	 * ���캯��
	 */
	public MeetingMode() {
		this.setTitle("Chita");
		initComponents(); // ��ʼ�����Ӵ��ڵ����
		initWorker(); // ��ʼ����̨

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
	 * ��ʼ�����
	 */
	private void initComponents() {
		progressbarPanel = new JPanel() {
			@Override
			public Insets getInsets() {
				Dimension frameSize = MeetingMode.this.getSize();
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
	 ** 
	 * ��ʼ����̨worker
	 */
	private void initWorker() {

		// �Զ���Ⲣ�����豸
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
						cards.show(MeetingMode.this.getContentPane(),
								PROGRESS_CARD);
						detectNetWorker.execute();
					}
				};
				MeetingMode.this.getContentPane().add(configPanel,
						CONFIG_CARD);
				cards.show(MeetingMode.this.getContentPane(), CONFIG_CARD);
			}
		};

		// �������
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
				editTargetPanel = new EditTargetPanel(stringTittle, stringExplain, true) {
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

						messageLabel.setText("�������ӵ������豸..");
						cards.show(MeetingMode.this.getContentPane(),
								PROGRESS_CARD);
						connectNetWorker.execute();
					}
				};

				MeetingMode.this.getContentPane().add(editTargetPanel,
						EDIT_CARD);
				cards.show(MeetingMode.this.getContentPane(), EDIT_CARD);
			}
		};

		connectNetWorker = new SwingWorker<Object, Object>() {
			@Override
			protected Object doInBackground() {
				return WORKE_FINISH;
			}

			@Override
			protected void done() {
				MeetingMode.this.getContentPane().add(createVisualPanel(),
						VISUAL_CARD);
				cards.show(MeetingMode.this.getContentPane(), VISUAL_CARD);
			}
		};

	}

	/**
	 * �����������
	 * 
	 */
	private JPanel createVisualPanel() {
		visualPanel = new JPanel();
		visualPanel.setLayout(new BorderLayout());

		messageArea = new JTextArea(25, 35);
		messageArea.setLineWrap(true);
		scrollPane = new JScrollPane(messageArea);
		visualPanel.add(scrollPane, BorderLayout.CENTER);

		// �����ض���messageArea
		System.setOut(new GUIPrintStream(System.out, messageArea));
		System.setErr(new GUIPrintStream(System.err, messageArea));

		JPanel controlPanel = new JPanel();
		controlPanel.setLayout(new GridLayout(1, 3));
		final JButton startButton = new JButton("�������");
		final JButton stopButton = new JButton("�뿪����");
		final JButton showParticipantsButton = new JButton("��ʾ������б�");
		final JButton recordButton = new JButton("¼�ƻ���");
		startButton.setEnabled(true);
		stopButton.setEnabled(false);
		showParticipantsButton.setEnabled(false);
		recordButton.setEnabled(false);
		startButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				//connectNet();
				receiver = new AVReceiver(receiveArgs, false);
				receiver.start();
				transmiter = new AVTransmit(videoDevice, audioDevice, transmitArgs[0],
						transmitArgs[1], false);
				transmiter.start();
				startButton.setEnabled(false);
				stopButton.setEnabled(true);
				showParticipantsButton.setEnabled(true);
				recordButton.setEnabled(true);
			}
		});
		stopButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				//disconnectNet();
				receiver.close();
				receiver.stop(); // ���ǹ�ʱ�ķ�������ʱ��֪���õķ���
				transmiter.stopTrans();
				startButton.setEnabled(true);
				stopButton.setEnabled(false);
				showParticipantsButton.setEnabled(false);
				recordButton.setEnabled(false);
			}
		});
		showParticipantsButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Vector participants = receiver.getParticipants();
				for (int i=0 ; i<participants.size(); i++) {
					String t = (String)(participants.elementAt(i));
					System.err.println(t);
				}
			}
		});

		recordButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				/*
				int option;
				JTextField textfield = new JTextField();
				Object[] array = { "������", textfield };
				Object[] options = { "ȷ��" };
				String textString = null;
				option = JOptionPane.showOptionDialog(null, array, "���ļ����浽",
						JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null,
						options, options[0]);
				if (option == 0) {
					textString = textfield.getText();
				}
				exportArgs[0] = "-o";
				exportArgs[1] = "file:" + textString;
				exportArgs[2] = "-d";
				textfield.setText("");
				option = JOptionPane.showOptionDialog(null, array, "¼��ʱ��(��)",
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
				System.err.println("��ʼ¼��");
				*/
				System.err.println("��Ǹ��¼�ƹ��ܻ�û����� o(�s���t)o");
			}
		});

		controlPanel.add(startButton);
		controlPanel.add(stopButton);
		controlPanel.add(showParticipantsButton);
		//controlPanel.add(recordButton);

		visualPanel.add(controlPanel, BorderLayout.SOUTH);
		return visualPanel;
	}

	/**
	 * ͨ��ָ���Ĳ���������ý�崫������
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
	 * �Ͽ�����
	 */
	private void disconnectNet() {
		receiver.close();
		receiver.stop(); // ���ǹ�ʱ�ķ�������ʱ��֪���õķ���
		transmiter.stopTrans();
	}

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
				MeetingMode frame = new MeetingMode();
				frame.setLocationRelativeTo(null);
				frame.setVisible(true);
			}
		});
	}

}
