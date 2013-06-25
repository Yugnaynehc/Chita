package edu.chita.base;

import edu.chita.helper.*;
import java.awt.*;
import javax.swing.*;

// TODO  Ӧ�ð�ģʽ���÷�����ǰ����Ϊ�����Ӱ��֮�����Ƶ��ʽ��ѡ�񡣻���˵������Ƶ��ʽѡ���ʱ������в����ڴ���ĸ�ʽ�޳�

public class Chita extends JFrame {

	private CardLayout cards;
	private static final String MODE_CARD = "mode card";
	private JPanel modePanel;

	/**
	 * ����������
	 * @param args
	 */
	public static void main(String[] args) {
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
				Chita frame = new Chita();
				frame.pack();
				frame.setLocationRelativeTo(null);
				frame.setVisible(true);
				
			}
		});
	}

	/**
	 * ���캯��
	 */
	public Chita() {

		Container content = this.getContentPane();
		cards = new CardLayout();
		content.setLayout(cards);

		modePanel = new ModeSelectPanel() {
			@Override
			protected void cancelActionPerformed() {
				super.cancelActionPerformed();
				System.exit(0);
			}

			@Override
			protected void okActionPerformed() {
				super.okActionPerformed();
				Chita.this.setVisible(false);
				enterSelectMode();
			}
		};
		modePanel.setPreferredSize(new Dimension(650, 520));
		this.setTitle("Chita ģʽѡ��");
		Chita.this.getContentPane().add(modePanel, MODE_CARD);
		cards.show(Chita.this.getContentPane(), MODE_CARD);

		content.add(modePanel, MODE_CARD);
	}
}
