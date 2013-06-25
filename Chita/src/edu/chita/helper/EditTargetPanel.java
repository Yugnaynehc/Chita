package edu.chita.helper;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

public class EditTargetPanel extends JPanel implements ItemListener {

	private boolean multi;		// 标志是否默认使用多播地址
	private String stringTittle;
	private String stringExplain;
	private JButton buttonOK;
	private JButton buttonCancel;
	private JButton buttonDetected;
	private JLabel labelTittle;
	private JLabel labelExplain;
	private JLabel labelIp;
	private JLabel labelPort;
	private JTextField ipAddress;
	private JTextField port;
	private JTextArea dectetedAddress;
	private JScrollPane scrollPane;
	private JPanel panel;
	private JPanel panelButtons;
	private JPanel panelContent;
	private JPanel panelTittle;
	private JPanel panelTarget;
	private JPanel panelDetected;
	private String args[];

	/**
	 * 构造函数*
	 * @param tittle
	 * @param explain
	 * @param tag
	 */
	public EditTargetPanel(String tittle, String explain, boolean tag) {
		try {
			this.stringTittle = tittle;
			this.stringExplain = explain;
			this.multi = tag;
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

		panelTarget = new JPanel() {
			@Override
			public Insets getInsets() {
				Dimension frameSize = this.getSize();
				Dimension progressSize = this.getPreferredSize();
				int d = (int) (frameSize.getHeight() - progressSize.getHeight() - 50) / 2;
				return new Insets(d, 10, d, 10);
			}
		};

		FlowLayout flow = new FlowLayout();
		flow.setAlignment(FlowLayout.LEFT);
		panelTarget.setLayout(flow);
		panelTarget.setPreferredSize(new Dimension(290, 240));
		panelTarget.setBorder(BorderFactory.createTitledBorder(""));

		ipAddress = new JTextField();
		if (multi)
			ipAddress.setText("230.0.0.1");
		port = new JTextField();
		buttonDetected = new JButton("探测");
		buttonDetected.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				detectedActionPerformed();
			}
		});
		labelExplain = new JLabel(stringExplain);
		labelIp = new JLabel("IP地址：");
		labelPort = new JLabel("端口号：");
		ipAddress.setPreferredSize(new Dimension(240, 50));
		port.setPreferredSize(new Dimension(240, 50));
		panelTarget.add(labelExplain);
		panelTarget.add(labelIp);
		panelTarget.add(ipAddress);
		panelTarget.add(labelPort);
		panelTarget.add(port);
		panelTarget.add(buttonDetected);

		panelDetected = new JPanel() {
			@Override
			public Insets getInsets() {
				Dimension frameSize = this.getSize();
				Dimension progressSize = this.getPreferredSize();
				int d = (int) (frameSize.getHeight() - progressSize.getHeight() - 50) / 2;
				return new Insets(d, 0, d, 0);
			}
		};
		dectetedAddress = new JTextArea(15, 25);
		dectetedAddress.setLineWrap(true);
		scrollPane = new JScrollPane(dectetedAddress);
		// 重定向输出到文本框
		System.setOut(new GUIPrintStream(System.out, dectetedAddress));
		System.setErr(new GUIPrintStream(System.err, dectetedAddress));

		panelDetected.setPreferredSize(new Dimension(340, 240));
		panelDetected.setBorder(BorderFactory.createTitledBorder(""));
		panelDetected.add(scrollPane);

		panelTittle = new JPanel();
		panelTittle.setPreferredSize(new Dimension(0, 70));
		labelTittle = new JLabel(stringTittle);
		panelTittle.setBorder(BorderFactory.createTitledBorder(""));
		panelTittle.setLayout(new BorderLayout());
		panelTittle.add(labelTittle, BorderLayout.WEST);
		panelContent.add(panelTittle, BorderLayout.NORTH);

		panelContent.add(panelTarget, BorderLayout.WEST);
		panelContent.add(panelDetected, BorderLayout.EAST);
		// 下面的语句是为了让按钮居中
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
	 * 返回输入信息
	 * @return
	 */
	public String[] getArgs() {
		args = new String[2];
		args[0] = ipAddress.getText();
		args[1] = port.getText();
		return args;
	}

	protected void okActionPerformed() {
		//
	}

	protected void cancelActionPerformed() {
		//
	}

	/**
	 * 探测局域网活跃IP
	 */
	protected void detectedActionPerformed() {
		dectetedAddress
				.append("探测局域网内活跃IP的功能已经被取消，因为它启用了大量的线程来加速，"
						+ "有可能造成机器假死。如果你想体验这个功能，"
						+ "请自行到源程序:\nEditTargetPanel.java\n中把DetectInternet.setup的注释取消");
		// DetectInternet.setup(172, 16, 72);
		// 三个参数分别为你的IP地址的前三位，这个函数就是用来
		// 探测与你邻近IP地址的主机是否在活动中
	}

	/**
	 * 事件监听器
	 */
	@Override
	public void itemStateChanged(ItemEvent event) {

		Object objectSource;
		boolean boolEnable;

		objectSource = event.getSource();

	}

}
