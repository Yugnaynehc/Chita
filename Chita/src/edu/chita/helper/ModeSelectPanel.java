package edu.chita.helper;

import edu.chita.base.*;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import javax.media.CaptureDeviceManager;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.SwingConstants;

public class ModeSelectPanel extends JPanel implements ItemListener {

	private JButton buttonOK;
	private JButton buttonCancel;
	private ButtonGroup groupMode;
	private JRadioButton checkTest;
	private JRadioButton checkPointToPoint;
	private JRadioButton checkBroadCast;
	private JRadioButton checkMeeting;
	private String stringTittle = "<html>��ӭʹ��Chita��<br>����һ������RTPЭ��ľ�����ͨ������������ͨ�������;������ڵ����ѶԻ���</html>";
	private String stringTestMode = "<html>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;����ģʽ�������Ա��ص�Ӳ���豸���ڴ�ģʽ�£�����Բ鿴JMF���񵽵��豸��Ϣ��������Ӳ���Ƿ����������������ʹ���г�����Ӳ�����⣬�뵽��ģʽ���в��ԡ�</html>";
	private String stringPointToPointMode = "<html>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;�Ի�ģʽģʽ������������ڵ���һ̨�����Ի���ͨ�������ض���IP�Ͷ˿ڣ�������ѡ���Ӳ���豸����ʵ����Ƶ����Ƶ��ͨ����</html>";
	private String stringBroadcastMode = "<html>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;�㲥ģʽ�������������������Ϣ���߽��ܾ������ڵ���Ϣ�������˾������鲥�����������ģʽ������ѡ�����鲥�鷢����Ϣ���߽����鲥�鴫������Ϣ����������С�͵����ŷ�������Զ�̽�ѧ��</html>";
	private String stringMeetingMode = "<html>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;����ģʽ����˼����Ƕ���ͨ����Ҳ�ǲ������鲥�ļ���������������£�ֻҪ����߶�ʹ��ͬһ���鲥��ַ���˿ڣ��������ٿ�һ��С�ͻ��顣</html>";
	private JPanel panel;
	private JPanel panelButtons;
	private JPanel panelContent;
	private JPanel panelChose;
	private JPanel panelText;
	private JPanel panelTittle;
	private JLabel labelTittle;
	private JLabel labelTestMode;
	private JLabel labelPointToPointMode;
	private JLabel labelBroadcastMode;
	private JLabel labelMeetingMode;
	public static int select = 2;

	public ModeSelectPanel() {
		try {
			init();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * ��ʼ�����
	 * @throws Exception
	 */
	private void init() throws Exception {
		this.setLayout(new BorderLayout());
		panelContent = new JPanel(new BorderLayout()) {
			@Override
			public Insets getInsets() {
				return new Insets(10, 10, 10, 10);
			}
		};
		this.add(panelContent, BorderLayout.CENTER);

		// ����ģʽѡ�����
		panelChose = new JPanel() {
			@Override
			public Insets getInsets() {
				Dimension frameSize = this.getSize();
				Dimension progressSize = this.getPreferredSize();
				int d = (int) (frameSize.getHeight() - progressSize.getHeight() - 50) / 2;
				return new Insets(d, 20, d, 0);
			}
		};
		panelChose.setBorder(BorderFactory.createTitledBorder(""));
		panelChose.setPreferredSize(new Dimension(100, 240));
		panelChose.setLayout(new GridLayout(4, 1));
		groupMode = new ButtonGroup();
		checkTest = new JRadioButton("����", false);
		checkPointToPoint = new JRadioButton("�Ի�", true);
		checkBroadCast = new JRadioButton("�㲥", false);
		checkMeeting = new JRadioButton("����", false);
		groupMode.add(checkTest);
		panelChose.add(checkTest);
		checkTest.addItemListener(this);
		groupMode.add(checkPointToPoint);
		panelChose.add(checkPointToPoint);
		checkPointToPoint.addItemListener(this);
		groupMode.add(checkBroadCast);
		panelChose.add(checkBroadCast);
		checkBroadCast.addItemListener(this);
		groupMode.add(checkMeeting);
		panelChose.add(checkMeeting);
		checkMeeting.addItemListener(this);

		// ����ģʽ˵�����
		panelText = new JPanel() {
			@Override
			public Insets getInsets() {
				Dimension frameSize = this.getSize();
				Dimension progressSize = this.getPreferredSize();
				int y = (int) (frameSize.getHeight() - progressSize.getHeight() - 50) / 2;
				return new Insets(y, 30, y, 20);
			}
		};
		panelText.setBorder(BorderFactory.createTitledBorder(""));
		panelText.setPreferredSize(new Dimension(50, 240));
		panelText.setLayout(new GridLayout(4, 1));
		labelTittle = new JLabel(stringTittle);
		labelTestMode = new JLabel(stringTestMode);
		labelPointToPointMode = new JLabel(stringPointToPointMode);
		labelBroadcastMode = new JLabel(stringBroadcastMode);
		labelMeetingMode = new JLabel(stringMeetingMode);
		panelText.add(labelTestMode);
		panelText.add(labelPointToPointMode);
		panelText.add(labelBroadcastMode);
		panelText.add(labelMeetingMode);

		// �����������
		panelTittle = new JPanel();
		panelTittle.setPreferredSize(new Dimension(0, 70));
		panelTittle.setBorder(BorderFactory.createTitledBorder(""));
		panelTittle.setLayout(new BorderLayout());
		panelTittle.add(labelTittle, BorderLayout.WEST);
		panelContent.add(panelTittle, BorderLayout.NORTH);
		panelContent.add(panelChose, BorderLayout.WEST);
		panelContent.add(panelText, BorderLayout.CENTER);
		panel = new JPanel(new FlowLayout(FlowLayout.CENTER));
		panelButtons = createButtonPanel();
		panel.add(panelButtons);
		panelContent.add(panel, BorderLayout.SOUTH);
	}

	/**
	 * ������ť���
	 * @return
	 */
	private JPanel createButtonPanel() {
		JPanel panel = new JPanel();
		panel.setLayout(new FlowLayout(FlowLayout.CENTER));
		buttonOK = new JButton("ȷ��");
		buttonOK.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				okActionPerformed();
			}
		});
		buttonCancel = new JButton("ȡ��");
		buttonCancel.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				cancelActionPerformed();
			}
		});
		panel.add(buttonOK);
		panel.add(buttonCancel);
		return panel;
	}

	/**
	 * ����ģʽѡ�����Ľӿ�
	 */
	public void enterSelectMode() {

		switch (select) {
		case 1:
			TestMode.setup();
			break;
		case 2:
			PointToPointMode.setup();
			break;
		case 3:
			BroadcastMode.setup();
			break;
		case 4:
			MeetingMode.setup();
			break;
		}
	}

	protected void okActionPerformed() {
		//
	}

	protected void cancelActionPerformed() {
		//
	}

	/**
	 * �¼�������
	 */
	@Override
	public void itemStateChanged(ItemEvent event) {

		Object objectSource;

		objectSource = event.getSource();

		if (objectSource == checkTest) {
			select = 1;
		} else if (objectSource == checkPointToPoint) {
			select = 2;
		} else if (objectSource == checkBroadCast) {
			select = 3;
		} else if (objectSource == checkMeeting) {
			select = 4;
		}

	}

}
