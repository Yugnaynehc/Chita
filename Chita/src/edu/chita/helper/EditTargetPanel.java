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

	private boolean multi;		// ��־�Ƿ�Ĭ��ʹ�öಥ��ַ
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
	 * ���캯��*
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
		buttonDetected = new JButton("̽��");
		buttonDetected.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				detectedActionPerformed();
			}
		});
		labelExplain = new JLabel(stringExplain);
		labelIp = new JLabel("IP��ַ��");
		labelPort = new JLabel("�˿ںţ�");
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
		// �ض���������ı���
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
		// ����������Ϊ���ð�ť����
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
	 * ����������Ϣ
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
	 * ̽���������ԾIP
	 */
	protected void detectedActionPerformed() {
		dectetedAddress
				.append("̽��������ڻ�ԾIP�Ĺ����Ѿ���ȡ������Ϊ�������˴������߳������٣�"
						+ "�п�����ɻ��������������������������ܣ�"
						+ "�����е�Դ����:\nEditTargetPanel.java\n�а�DetectInternet.setup��ע��ȡ��");
		// DetectInternet.setup(172, 16, 72);
		// ���������ֱ�Ϊ���IP��ַ��ǰ��λ�����������������
		// ̽�������ڽ�IP��ַ�������Ƿ��ڻ��
	}

	/**
	 * �¼�������
	 */
	@Override
	public void itemStateChanged(ItemEvent event) {

		Object objectSource;
		boolean boolEnable;

		objectSource = event.getSource();

	}

}
