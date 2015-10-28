/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package ejadvisor3;

import javax.swing.BoxLayout;
import javax.swing.JWindow;
import javax.swing.JLabel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JProgressBar;
import java.awt.Container;

/**
 *
 * @author aito
 */
public class Splash extends JWindow {
    private JLabel message;
    //private JProgressBar progress;
    public Splash(String basedir) {
        Container content = getContentPane();
        content.setLayout(new BoxLayout(content,BoxLayout.Y_AXIS));
        //System.err.println("Get icon from "+basedir+"/splash.png");
        Icon icon = new ImageIcon(basedir+"splash.png");
        JLabel lab = new JLabel(icon);
        content.add(lab);
        message = new JLabel("初期化しています．．．");
        content.add(message);
        //progress = new JProgressBar(JProgressBar.HORIZONTAL);
        //content.add(progress);
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
        getToolkit().sync();
    }
    public void setMessage(String str) {
        message.setText(str);
        getToolkit().sync();
    }
}
