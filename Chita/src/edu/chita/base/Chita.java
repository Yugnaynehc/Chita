package edu.chita.base;

import edu.chita.helper.*;
import java.awt.*;
import javax.swing.*;

// TODO  应该把模式设置放在最前，因为这个将影响之后的视频格式的选择。或者说，在视频格式选择的时候把所有不利于传输的格式剔除

public class Chita extends JFrame {

	private CardLayout cards;
	private static final String MODE_CARD = "mode card";
	private JPanel modePanel;

	/**
	 * 程序的主入口
	 * @param args
	 */
	public static void main(String[] args) {
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
				Chita frame = new Chita();
				frame.pack();
				frame.setLocationRelativeTo(null);
				frame.setVisible(true);
				
			}
		});
	}

	/**
	 * 构造函数
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
		this.setTitle("Chita 模式选择");
		Chita.this.getContentPane().add(modePanel, MODE_CARD);
		cards.show(Chita.this.getContentPane(), MODE_CARD);

		content.add(modePanel, MODE_CARD);
	}
}
