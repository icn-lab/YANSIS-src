/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ejadvisor3;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import java.awt.*;
import java.awt.event.*;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Properties;
import org.Gyutan.Gyutan;

/**
 *
 * @author nagano
 */
public class Hanasu {

    private final String progname = "音声合成ツールバー";
    private Font uiFont, textFont, uiSmallFont;
    private ArrayList<JComponent> uiFontComponents;
    private ArrayList<JComponent> uiSmallFontComponents;
    private ArrayList<JComponent> textFontComponents;

    Properties property;

    JFrame frame;
    JPanel panel;

    JTextArea textArea;
    JTextField moraSpeedText;
    JSlider moraSpeedSlider;
    JButton doSynthesizeButton;

    JMenuBar menuBar;
    JMenu fileMenu;
    JMenuItem menuItemSave;
    //   JMenu     editMenu;
    JMenu configMenu;
    //   JMenu     helpMenu;
    JLabel mpmLabel;

    Gyutan gyutan;

    String baseDir = null;
    String htsVoiceDir = "./htsvoice";
    String htsVoice = htsVoiceDir + "/" + "tohoku-f01-neutral.htsvoice";
    String[] feature;
    //String senHome     = "./sen";

    int maxMoraPerMin = 800;
    int moraPerMin = 400;
    int minMoraPerMin = 150;
    double ratio = 1.0;

    public void initialize(String baseDir, Properties property) {
        this.baseDir = baseDir;
        if (this.baseDir.endsWith("/") == false) {
            this.baseDir += "/";
        }
        
        // initialize Gyutan;
        gyutan = new Gyutan();

        // initialize GUI;
        initializeGUI();
        initFontSettings();

        this.property = property;

        String propHtsVoice = this.property.getProperty("htsVoice");
        if (propHtsVoice != null) {
            htsVoice = propHtsVoice;
        } else {
            htsVoice = baseDir + htsVoice;
        }

        gyutan.initializeEngine(htsVoice);
        /*
         // set settings
         htsVoice = getProperty("htsVoice");
         if(htsVoice != null)
         gyutan.initializeEngine(htsVoice);
	
         senHome  = getProperty("senHome");
         if(senHome != null)
         gyutan.initializeSen(senHome);
         */
		//gyutan.initializeSen(baseDir+senHome);

        String moraSpeed = property.getProperty("moraSpeed");
        if (moraSpeed != null) {
            setMoraSpeed(Integer.parseInt(moraSpeed));
        } else {
            setMoraSpeed(moraPerMin);
        }

        setSynthesizeButtonState();
    }

    public void initializeGUI() {
        frame = new JFrame();
        frame.setTitle(progname);
        frame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);

        panel = new JPanel();
        panel.setLayout(new BorderLayout());

        textArea = new JTextArea(5, 10);
        textArea.setLineWrap(true);
        textArea.setEditable(false);
        textArea.setEnabled(false);
        JScrollPane scrollPane = new JScrollPane(textArea);

        JPanel subPanel = new JPanel();
        subPanel.setLayout(new BorderLayout());

        JPanel moraPanel = new JPanel();
        new BoxLayout(moraPanel, BoxLayout.X_AXIS);
        mpmLabel = new JLabel("[モーラ/分]");
        mpmLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 20));

        moraSpeedSlider = new JSlider(SwingConstants.HORIZONTAL, minMoraPerMin, maxMoraPerMin, moraPerMin);
        moraSpeedSlider.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent event) {
                moraPerMin = moraSpeedSlider.getValue();
                moraSpeedText.setText(String.format("%d", moraPerMin));
            }
        });

        moraSpeedText = new JTextField(String.format("%d", moraPerMin));

        moraSpeedText.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                try {
                    int value = Integer.parseInt(moraSpeedText.getText());
                    //System.err.println(mpmText.getText());
                    if (minMoraPerMin <= value && value <= maxMoraPerMin) {
                        setMoraSpeed(value);
                    } else if (minMoraPerMin > value) {
                        setMoraSpeed(minMoraPerMin);
                    } else if (maxMoraPerMin < value) {
                        setMoraSpeed(maxMoraPerMin);
                    }
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            }
        });
        moraPanel.add(moraSpeedText);
        moraPanel.add(mpmLabel);

        doSynthesizeButton = new JButton("音声を合成");
        doSynthesizeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                doSynthesize(moraPerMin);
            }
        });

        subPanel.add("East", moraPanel);
        subPanel.add("Center", moraSpeedSlider);
        subPanel.add("South", doSynthesizeButton);

        menuBar = new JMenuBar();
        fileMenu = new JMenu("ファイル");
//		editMenu   = new JMenu("編集");
        configMenu = new JMenu("設定");
		//helpMenu   = new JMenu("ヘルプ");

        menuItemSave = new JMenuItem("Save WAV");
        menuItemSave.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                saveWAV();
            }
        });
        /*
         JMenuItem menuItemExit = new JMenuItem("exit");
         menuItemExit.addActionListener(new ActionListener(){
         public void actionPerformed(ActionEvent event){
         doExit();
         }
         });
         */

        fileMenu.add(menuItemSave);
        //fileMenu.add(menuItemExit);
        menuItemSave.setEnabled(false);
        /*
         JMenuItem menuItemCopy  = new JMenuItem("コピー");
         menuItemCopy.addActionListener(new ActionListener(){
         public void actionPerformed(ActionEvent event){
         editCopy();
         }
         });
		
         JMenuItem menuItemPaste = new JMenuItem("貼り付け");
         menuItemPaste.addActionListener(new ActionListener(){
         public void actionPerformed(ActionEvent event){
         editPaste();
         }
         });
		
         editMenu.add(menuItemCopy);
         editMenu.add(menuItemPaste);
         */
        /*
         JMenuItem menuItemConfigSen = new JMenuItem("Sen Home");
         menuItemConfigSen.addActionListener(new ActionListener(){
         public void actionPerformed(ActionEvent event){
         setSenHome();
         }
         });
         */

        JMenuItem menuItemConfigHTSVoice = new JMenuItem("htsvoice");
        menuItemConfigHTSVoice.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                setHTSVoice();
            }
        });

        //configMenu.add(menuItemConfigSen);
        configMenu.add(menuItemConfigHTSVoice);

        /*
         JMenuItem menuItemAbout = new JMenuItem("About");
         menuItemAbout.addActionListener(new ActionListener(){
         public void actionPerformed(ActionEvent event){
         //JOptionPane.showMessageDialog(frame, String.format("%s\n%s", progname, version));
         }
         });
		
         helpMenu.add(menuItemAbout);
         */
        menuBar.add(fileMenu);
        //menuBar.add(editMenu);
        menuBar.add(configMenu);
		//menuBar.add(helpMenu);

        panel.add("North", menuBar);
        panel.add("Center", scrollPane);
        panel.add("South", subPanel);

        frame.add(panel);
        frame.pack();
        frame.setVisible(false);
    }

    public void initFontSettings() {
        uiFontComponents = new ArrayList<JComponent>();
        uiSmallFontComponents = new ArrayList<JComponent>();
        textFontComponents = new ArrayList<JComponent>();

        uiFontComponents.add(fileMenu);
        uiFontComponents.add(configMenu);
        uiFontComponents.add(moraSpeedText);
        uiFontComponents.add(doSynthesizeButton);

        textFontComponents.add(textArea);

        uiSmallFontComponents.add(menuItemSave);
        uiSmallFontComponents.add(mpmLabel);
    }

    private void updateTextFont() {
        System.out.println("Font:" + textFont.getFamily());
        System.out.println("Size:" + textFont.getSize());
        for (JComponent component : textFontComponents) {
            component.setFont(textFont);
        }
    }

    private void updateUIFont() {
             //System.out.println("Font:"+textFont.getFamily());
        //System.out.println("Size:"+textFont.getSize());
        for (JComponent component : uiFontComponents) {
            component.setFont(uiFont);
        }

        frame.pack();
    }

    public void updateUISmallFont() {
        for (JComponent component : uiSmallFontComponents) {
            component.setFont(uiSmallFont);
        }

        frame.pack();
    }

    public void setTextFont(Font font) {
        textFont = font;
        updateTextFont();
    }

    public void setUIFont(Font font) {
        uiFont = font;
        updateUIFont();
    }

    public void setUISmallFont(Font font) {
        uiSmallFont = font;
        updateUISmallFont();
    }

    public void setVisible(boolean flag) {
        if (flag) {
            frame.pack();
            frame.setVisible(flag);
        } else {
            frame.setVisible(flag);
        }
    }

    public void setButtonState(boolean flag) {
        doSynthesizeButton.setEnabled(flag);
    }

    /*
     public Properties loadProperties(String filename){
     Properties prop = new Properties();
     try{
     prop.load(new FileInputStream(filename));
     }catch(Exception e){
     e.printStackTrace();
     }
		
     return prop;
     }
     public void storeProperties(String filename){
     try{
     properties.store(new FileOutputStream(filename), null);
     }catch(Exception e){
     e.printStackTrace();
     }
     }
	
     public void setProperty(String key, String value){
     properties.setProperty(key, value);
     storeProperties(propertyName);
     }
	
     public String getProperty(String key){
     if(properties.containsKey(key))
     return properties.getProperty(key);
     else
     return null;
     }
     */
    public void setTextFeature(String[] feature) {
        this.feature = feature;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < feature.length; i++) {
            String toks[] = feature[i].split(",");
            sb.append(toks[0]);
        }
        textArea.setText(sb.toString());
    }

    public void doSynthesize() {
        if (feature == null) {
            menuItemSave.setEnabled(false);
            return;
        }

        if (gyutan.availableEngine()) {
            gyutan.make_label(feature);
            gyutan.set_audio_buff_size(10000);
            gyutan.synthesis(null, null);
            menuItemSave.setEnabled(true);
        }
    }

    public void doSynthesize(int speed) {
        if (feature == null) {
            menuItemSave.setEnabled(false);
            return;
        }

        gyutan.make_label(feature);

        int sp1 = synthesizeAndCalcMoraSpeed(ratio);
        int diff = sp1 - speed;
        int cnt = 0;

        if (gyutan.availableEngine()) {
            while (cnt < 20) {
                if (diff == 0) {
                    break;
                }

                double ratio2 = ratio - 0.001 * diff;
                int sp2 = synthesizeAndCalcMoraSpeed(ratio2);
                int diff2 = sp2 - speed;
                //System.err.printf("speed:%d sp1:%d, sp2:%d, ratio:%f, ratio2:%f, diff=%d, diff2=%d\n", speed, sp1, sp2, ratio, ratio2, diff, diff2);

                if (Math.abs(diff) < Math.abs(diff2)) {
                    break;
                }

                diff = diff2;
                ratio = ratio2;
                sp1 = sp2;
                cnt++;
            }

            setMoraSpeed(sp1);
            gyutan.set_audio_buff_size(10000);
            gyutan.set_speed(ratio);
            gyutan.synthesis(null, null);
            menuItemSave.setEnabled(true);
        }
    }

    public int synthesizeAndCalcMoraSpeed(double ratio) {
        gyutan.set_speed(ratio);
        gyutan.set_audio_buff_size(0);
        gyutan.synthesis(null, null);

        String[] strings = gyutan.get_label(true);
        Label label = new Label(strings);

        return label.getMoraSpeed();
    }

    public void setSenHome() {
        /*
         JFileChooser fileChooser = new JFileChooser();
         fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

         while(true){
         int selected = fileChooser.showOpenDialog(frame);
         if(selected == JFileChooser.APPROVE_OPTION){
         senHome = fileChooser.getSelectedFile().toString();
         System.err.printf("file:%s\n", senHome);
         gyutan.initializeSen(senHome);
         if(gyutan.availableSen()){
         setProperty("senHome", senHome);
         break;
         }
         }
         else
         break;
         }
		
         setSynthesizeButtonState();
         */
    }

    public void setHTSVoice() {
        JFileChooser fileChooser = new JFileChooser(htsVoiceDir);
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

        // add .htsvoice extension filter
        FileFilter filter = new FileNameExtensionFilter("htsvoice file", "htsvoice");
        fileChooser.addChoosableFileFilter(filter);

        while (true) {
            int selected = fileChooser.showOpenDialog(frame);
            if (selected == JFileChooser.APPROVE_OPTION) {
                htsVoice = fileChooser.getSelectedFile().toString();
                gyutan.initializeEngine(htsVoice);
                if (gyutan.availableEngine()) {
                    property.setProperty("htsVoice", htsVoice);
                    break;
                }
                /*
                 if(gyutan.availableEngine()){
                 setProperty("htsVoice", htsVoice);
                 break;
                 }
                 */
            } else {
                break;
            }
        }

        setSynthesizeButtonState();
    }

    public void setMoraSpeed(int moraSpeed) {
        System.err.printf("mora speed is set to %d\n", moraSpeed);
        moraPerMin = moraSpeed;
        moraSpeedText.setText(String.format("%d", moraPerMin));
        moraSpeedSlider.setValue(moraPerMin);
//		setProperty("moraSpeed", String.valueOf(moraPerMin));
    }

    public void saveWAV() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        int selected = fileChooser.showSaveDialog(frame);
        try {
            if (selected == JFileChooser.APPROVE_OPTION) {
                gyutan.save_riff(new FileOutputStream(fileChooser.getSelectedFile()));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setSynthesizeButtonState() {
        if (gyutan.availableEngine()) {
            doSynthesizeButton.setEnabled(true);
        } else {
            doSynthesizeButton.setEnabled(false);
        }
    }

    public void editCopy() {
        textArea.copy();
    }

    public void editPaste() {
        textArea.paste();
    }

    public void doExit() {
        System.exit(0);
    }
    /*
     public static void main(String[] args){
     Hanasu hanasu = new Hanasu();
     if(args.length == 1)
     hanasu.initialize(args[0]);
     else
     hanasu.initialize("./");
     }
     */
}
