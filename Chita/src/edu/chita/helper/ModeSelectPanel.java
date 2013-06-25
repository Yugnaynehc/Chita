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
	private String stringTittle = "<html>欢迎使用Chita！<br>我是一个基于RTP协议的局域网通信软件，你可以通过我来和局域网内的朋友对话。</html>";
	private String stringTestMode = "<html>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;测试模式用来测试本地的硬件设备。在此模式下，你可以查看JMF捕获到的设备信息，并测试硬件是否正常工作。如果在使用中出现了硬件问题，请到此模式进行测试。</html>";
	private String stringPointToPointMode = "<html>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;对话模式模式用来与局域网内的另一台主机对话。通过填入特定的IP和端口，根据你选择的硬件设备可以实现音频及视频的通话。</html>";
	private String stringBroadcastMode = "<html>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;广播模式是用来向局域网发送消息或者接受局域网内的消息，采用了局域网组播技术。在这个模式里，你可以选择向组播组发送消息或者接收组播组传来的消息。可以用于小型的新闻发布或者远程教学。</html>";
	private String stringMeetingMode = "<html>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;会议模式顾名思义就是多人通话，也是采用了组播的技术。在这个功能下，只要与会者都使用同一个组播地址及端口，你便可以召开一个小型会议。</html>";
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
	 * 初始化面板
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

		// 创建模式选项面板
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
		checkTest = new JRadioButton("测试", false);
		checkPointToPoint = new JRadioButton("对话", true);
		checkBroadCast = new JRadioButton("广播", false);
		checkMeeting = new JRadioButton("会议", false);
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

		// 创建模式说明面板
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

		// 创建标题面板
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
	 * 创建按钮面板
	 * @return
	 */
	private JPanel createButtonPanel() {
		JPanel panel = new JPanel();
		panel.setLayout(new FlowLayout(FlowLayout.CENTER));
		buttonOK = new JButton("确定");
		buttonOK.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				okActionPerformed();
			}
		});
		buttonCancel = new JButton("取消");
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
	 * 进入模式选择面板的接口
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
	 * 事件监听器
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
