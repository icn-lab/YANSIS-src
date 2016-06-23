package ejadvisor3;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.Properties;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.event.DocumentEvent;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.JTextComponent;
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author Akinori
 */
public class EJAdvisor3view extends javax.swing.JFrame implements EJAdvisor3GUI {

    static final String TITLE  = "EJAdvisor version 1.4";
    static final String author = "作者: 伊藤 彰則";
    static final String copyrightTextToSpeech ="音声合成には以下のソフトウェアを使用しています\nGyutan, Sasakama:\nCopyright(c) 2015 東北大学 伊藤・能勢研究室\nCopyright(c) 2008-2015 名古屋工業大学 徳田・南角研究室";
    
    private final String propertyFile = ".EJAdvisor3.properties";
    private final String PROPERTY_UIFONT_FAMILY = "UI_FONT_FAMILY";
    private final String PROPERTY_UIFONT_SIZE = "UI_FONT_SIZE";
    private final String PROPERTY_TEXTFONT_FAMILY = "TEXT_FONT_FAMILY";
    private final String PROPERTY_TEXTFONT_SIZE = "TEXT_FONT_SIZE";

    private boolean speechSynthesis;
    private boolean dataCreated;
    private final FileSaver fileSaver;
    private EJAdvisor3App app;
    private EJAdvisor3 ejadv3;
    private WordProperty currentPopup; //現在ポップアップしている単語
    private JTextComponent currentFocus; //現在フォーカスのあるTextComponent;
    private Hanasu hanasu;

    private String[] fontList = {"メイリオ", "ＭＳ ゴシック", "OSAKA"};
    private String[] uiFontList = {"Meiryo UI", "MS UI Gothic", "OSAKA"};

    private Font uiFont, textFont;
    private Font defaultTextFont, defaultUIFont;

    private final int defaultUIFontSize = 14;
    private final int defaultTextFontSize = 18;
    private final int uiFontMinSize = 12;
    private final int uiFontMaxSize = 54;
    private final int textFontMinSize = 12;
    private final int textFontMaxSize = 54;

    private ArrayList<JComponent> uiFontComponents;
    private ArrayList<JComponent> uiSmallFontComponents;
    private ArrayList<JComponent> textFontComponents;

    private final int fontPlus = 10;
    private final int fontMinus = 2;
    private final int fontSmall = 4;

    private String[] availableFontFamilyNames;
    private Properties properties;

    /**
     * Creates new form EJAdvisor3view
     */
    public EJAdvisor3view(EJAdvisor3App app) {
        this.app = app;
        this.ejadv3 = app.ejadv3;

        // 保存されている property の読み込み
        loadProperties();
        
        // Font設定
        setDefaultFont();
        initFont();
        System.err.println("text font:" + textFont.getFamily() + ", ui font:" + uiFont.getFamily());
        availableFontFamilyNames = getAvailableFontFamilyNames("日本語あいうえおアイウエオ１２３４５６７８９０ＡＢＣＤＥー？！”＃＄％＆（）＜＞");
        initComponents();

        // 音声合成機能 
        // フォント反映(InitSettings())前に hanasu を new する必要がある
        hanasu = new Hanasu();
        hanasu.initialize(ejadv3.getBaseDir(), properties);
        hanasu.setMoraSpeed(360);
        // フォント反映
        initSettings();

        // フラグ設定
        dataCreated     = false;
        speechSynthesis = false;

        setTitle(TITLE);

        fileSaver = new FileSaver(this, null);
        String[] ext = {".txt"};
        fileSaver.setExtensions(ext);
        fileSaver.setDescription("Text file");

        // 終了時確認動作
        // 全体の終了処理を禁止する
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                windowClosingConfirmation();
            }
        });

        analysisPane.addHyperlinkListener(new WordPropertyPopupListener());

        // 作成した文書を直接編集したときに「変更あり」にするための処理
        inputText.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                dataCreated = true;
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                dataCreated = true;
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                dataCreated = true;
            }

        });
    }

    class WordPropertyPopupListener implements HyperlinkListener {

        @Override
        public void hyperlinkUpdate(HyperlinkEvent e) {
            //System.out.println("event:"+e.getEventType());
            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                String d = e.getDescription();
                if (d == null) {
                    //System.out.println("Hyperlink is null");
                    return;
                }
                String x[] = d.split(":");
                int s = Integer.parseInt(x[0]);
                int i = Integer.parseInt(x[1]);
                WordProperty w = ejadv3.currentMorph(s, i);
                //System.out.println("s:"+s+" i:"+i+" w:"+w);
                morphemeInfoPopup.setSize(250, 250);
                mpBasicForm.setText(w.getBasicString());
                mpWordTitle.setText(w.toString());
                mpGrade.setText(Integer.toString(w.getGrade()));
                mpPOS.setText(w.getPOS());
                mpPronunciation.setText(w.getPronunciation());
                mpForm.setText(w.getCform());
                morphemeInfoPopup.pack();
                morphemeInfoPopup.setVisible(true);
                currentPopup = w;
            }
        }
    }

    // Windowを閉じるときの確認

    public void windowClosingConfirmation() {
        if (dataCreated) {
            //System.out.println("Data created");
            int confirm = JOptionPane.showConfirmDialog(
                    this,
                    "内容が変更されています。保存しますか？",
                    "ファイル保存の確認",
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.WARNING_MESSAGE
            );
            if (confirm == JOptionPane.YES_OPTION) {
                if (saveCreatedText() != FileSaver.SAVED) {
                    return;
                }
            } else if (confirm == JOptionPane.CANCEL_OPTION) {
                return;
            }
        } else {
            //System.out.println("No change");
        }
        saveProperties();
        System.exit(0);
    }
    /*    
     @Action
     public void showAboutBox() {
     if (aboutBox == null) {
     JFrame mainFrame = EJAdvisor2App.getApplication().getMainFrame();
     aboutBox = new EJAdvisor2AboutBox(mainFrame);
     aboutBox.setLocationRelativeTo(mainFrame);
     }
     EJAdvisor2App.getApplication().show(aboutBox);
     }
     */

    public void initSettings() {
        uiFontComponents = new ArrayList<JComponent>();
        uiSmallFontComponents = new ArrayList<JComponent>();
        textFontComponents = new ArrayList<JComponent>();

        uiFontComponents.add(fileMenu);
        uiFontComponents.add(editMenu);
        uiFontComponents.add(jMenu2);
        uiFontComponents.add(helpMenu);
        uiFontComponents.add(jButton1);
        uiFontComponents.add(jButton2);
        uiFontComponents.add(jLabel1);
        uiFontComponents.add(jLabel2);
        uiFontComponents.add(jLabel3);
        uiFontComponents.add(jLabel4);
        uiFontComponents.add(jLabel5);
        uiFontComponents.add(jButton4);
        uiFontComponents.add(jButton3);
        uiFontComponents.add(jTabbedPane1);
        uiFontComponents.add(jLabel13);
        uiFontComponents.add(jLabel12);
        uiFontComponents.add(jLabel16);
        uiFontComponents.add(jComboBox1);
        uiFontComponents.add(jLabel14);
        uiFontComponents.add(jLabel15);
        uiFontComponents.add(jLabel17);

        // uiFontComponents.add(jLabel18);
        uiFontComponents.add(jLabel19);
        //uiFontComponents.add(jLabel20);
        uiFontComponents.add(jLabel21);

        uiFontComponents.add(jComboBox2);
        uiFontComponents.add(jButton5);
        uiFontComponents.add(jButton6);
        uiFontComponents.add(jCheckBox1);

        textFontComponents.add(inputText);
        textFontComponents.add(analysisPane);
        textFontComponents.add(evaluationPointPane);
        textFontComponents.add(resultText);
        textFontComponents.add(exampleSentenceArea);
        textFontComponents.add(mpBasicForm);
        textFontComponents.add(jLabel7);
        textFontComponents.add(jLabel6);
        textFontComponents.add(jLabel8);
        textFontComponents.add(jLabel9);
        textFontComponents.add(jLabel10);
        textFontComponents.add(mpPOS);
        textFontComponents.add(mpForm);
        textFontComponents.add(mpPronunciation);
        textFontComponents.add(mpGrade);

        uiSmallFontComponents.add(openMenuItem);
        uiSmallFontComponents.add(saveMenuItem);
        uiSmallFontComponents.add(saveAsMenuItem);
        uiSmallFontComponents.add(exitMenuItem);
        uiSmallFontComponents.add(cutMenuItem);
        uiSmallFontComponents.add(copyMenuItem);
        uiSmallFontComponents.add(pasteMenuItem);
        uiSmallFontComponents.add(deleteMenuItem);
        uiSmallFontComponents.add(jMenuItem1);
        uiSmallFontComponents.add(contentsMenuItem);
        uiSmallFontComponents.add(aboutMenuItem);

        updateTextFont();
        updateUIFont();
    }

    public int findFontFamilyIndex(Font f) {
        int index = -1;
        String fontFamilyName = f.getFamily();
        for (int i = 0; i < availableFontFamilyNames.length; i++) {
            if (fontFamilyName.equals(availableFontFamilyNames[i])) {
                index = i;
                break;
            }
        }

        return (index);
    }

    public void updateTextFont() {
        System.out.println("Font:" + textFont.getFamily());
        System.out.println("Size:" + textFont.getSize());
        for (JComponent component : textFontComponents) {
            component.setFont(textFont);
        }
        hanasu.setTextFont(textFont);

        mpWordTitle.setFont(textFont.deriveFont(Font.BOLD, (float) fontPlus + textFont.getSize()));

        int index = findFontFamilyIndex(textFont);
        if (index != -1) {
            jComboBox2.setSelectedIndex(index);
        }

        jSlider2.setValue(textFont.getSize());
        setTextFontProperties();

    }

    public void updateUIFont() {
        //System.out.println("Font:"+textFont.getFamily());
        //System.out.println("Size:"+textFont.getSize());
        for (JComponent component : uiFontComponents) {
            component.setFont(uiFont);
        }

        hanasu.setUIFont(uiFont);

        for (JComponent component : uiSmallFontComponents) {
            component.setFont(uiFont.deriveFont(Font.PLAIN, (float) -fontMinus + uiFont.getSize()));
        }

        hanasu.setUISmallFont(uiSmallFontComponents.get(0).getFont());

        jLabel18.setFont(uiFont.deriveFont(Font.PLAIN, (float) -fontSmall + uiFont.getSize()));
        jLabel20.setFont(uiFont.deriveFont(Font.PLAIN, (float) -fontSmall + uiFont.getSize()));

        int index = findFontFamilyIndex(uiFont);
        if (index != -1) {
            jComboBox1.setSelectedIndex(index);
        }

        jFrame1.pack();
        jSlider1.setValue(uiFont.getSize());
        setUIFontProperties();
    }

    public String[] getAvailableFontFamilyNames(String prtStr) {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        String[] allFonts = ge.getAvailableFontFamilyNames();
        ArrayList<String> tmpArray = new ArrayList<String>();
        for (int i = 0; i < allFonts.length; i++) {
            Font f = new Font(allFonts[i], Font.PLAIN, 12);
            if (prtStr == null || f.canDisplayUpTo(prtStr) == -1) {
                tmpArray.add(allFonts[i]);
            }
        }

        String[] availableFonts = new String[tmpArray.size()];
        for (int i = 0; i < tmpArray.size(); i++) {
            availableFonts[i] = tmpArray.get(i);
        }

        return availableFonts;
    }

    private void loadProperties() {
        properties = new Properties();

        try {
            InputStream is = new FileInputStream(propertyFile);
            properties.load(is);
            is.close();
        } catch (Exception e) {
            e.printStackTrace();;
        }
    }

    private void saveProperties() {
        try {
            //String saveDate = new Date().toString();
            OutputStream os = new FileOutputStream(propertyFile);
            properties.store(os, null);
            os.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setUIFontProperties() {
        properties.setProperty(PROPERTY_UIFONT_FAMILY, uiFont.getFamily());
        properties.setProperty(PROPERTY_UIFONT_SIZE, String.valueOf(uiFont.getSize()));
    }

    private void setTextFontProperties() {
        properties.setProperty(PROPERTY_TEXTFONT_FAMILY, textFont.getFamily());
        properties.setProperty(PROPERTY_TEXTFONT_SIZE, String.valueOf(textFont.getSize()));
    }

    /*
     */
    public void setDefaultFont() {
        // dummy のフォント名を指定して default のフォント名を調べる
        Font ff = Font.decode("abcdefg");
        String defaultFontFamily = ff.getFamily();

        String uiFontFamily = defaultFontFamily;
        for (int i = 0; i < uiFontList.length; i++) {
            Font f = Font.decode(uiFontList[i]);
            if (!f.getFamily().equals(defaultFontFamily)) {
                uiFontFamily = uiFontList[i];
                break;
            }
        }
        defaultUIFont = new Font(uiFontFamily, Font.PLAIN, defaultUIFontSize);

        String textFontFamily = defaultFontFamily;
        for (int i = 0; i < fontList.length; i++) {
            Font f = Font.decode(fontList[i]);
            if (!f.getFamily().equals(defaultFontFamily)) {
                textFontFamily = fontList[i];
                break;
            }
        }

        defaultTextFont = new Font(textFontFamily, Font.PLAIN, defaultTextFontSize);
    }

    public void initFont() {
        String family = properties.getProperty(PROPERTY_TEXTFONT_FAMILY);
        if (family == null) {
            family = defaultTextFont.getFamily();
        }

        int fontSize;
        String sizeStr = properties.getProperty(PROPERTY_TEXTFONT_SIZE);
        if (sizeStr == null) {
            fontSize = defaultTextFont.getSize();
        } else {
            fontSize = Integer.valueOf(sizeStr);
        }

        textFont = new Font(family, Font.PLAIN, fontSize);

        family = properties.getProperty(PROPERTY_UIFONT_FAMILY);
        if (family == null) {
            family = defaultUIFont.getFamily();
        }

        sizeStr = properties.getProperty(PROPERTY_UIFONT_SIZE);
        if (sizeStr == null) {
            fontSize = defaultUIFont.getSize();
        } else {
            fontSize = Integer.valueOf(sizeStr);
        }

        uiFont = new Font(family, Font.PLAIN, fontSize);
    }

    /**
     * ステータスラインにメッセージを表示
     *
     * @param mesg メッセージ文字列
     */
    public void showMessage(String mesg) {
        statusMessageLabel.setText(mesg);
    }

    /**
     * 解析結果の部分をクリア
     */
    public void clearResults() {
        analysisPane.setText("");
        evaluationPointPane.setText("");
        //jTextArea1.setText("");
    }

    /**
     * 解析結果を挿入(HTML)
     */
    public void setAnalysisResult(String htmlForInsert) {
        //System.out.println(htmlForInsert);
        analysisPane.setText(htmlForInsert);
    }

    /**
     * 評価ポイントを挿入(HTML)
     */
    public void setAnalysisPoint(String htmlForInsert) {
        evaluationPointPane.setText(htmlForInsert);
    }

    /**
     * 作成結果テキストを挿入(plain text)
     */
    public void setResultText(String plaintextForInsert) {
        resultText.setText(plaintextForInsert);
        dataCreated = true;
    }

    /**
     * 作成結果テキストを追加(plain text)
     */
    public void appendResultText(String plaintextForInsert) {
        resultText.append(plaintextForInsert);
        dataCreated = true;
    }

    /**
     * 〔使える単語〕を挿入
     */
    public void setUsableWords(String words) {
        exampleSentenceArea.setText(words);
    }

    /**
     * 〔使える単語〕を追加
     */
    public void appendUsableWords(String words) {
        exampleSentenceArea.append(words);
    }

    public void rewindUsableWords() {
        jScrollPane9.getViewport().setViewPosition(new Point(0, 0));
    }

    /**
     * 評価ボタンのアクション
     */
    public void doAnalysis() throws UnsupportedEncodingException {
        showMessage("テキスト解析中...");
        WordProperty[][] currentSent = ejadv3.doAnalysis(inputText.getText());
        String[] feature = hanasu.gyutan.tokenToString(ejadv3.getTokens());
        /*
         for(int i=0;i < feature.length;i++)
         System.err.println(feature[i]);
         */
        clearResults();
        String res = ""; //評価結果
        String evaluationPoints = ""; // アドバイス
        Boolean wordflag = true;

        showMessage("テキスト評価中...");
        for (int s = 0; s < currentSent.length; s++) {
            res += "(" + Integer.toString(s + 1) + "):";
            evaluationPoints += "<h2>文(" + Integer.toString(s + 1) + ")</h2>";
            WordProperty[] w = currentSent[s];
            double score = ejadv3.estimateScore(w);
            String scoreString = String.format("%.2f", score);
            evaluationPoints += "score:" + scoreString + "<br/>";

            for (int i = 0; i < w.length; i++) {
                // 文節間に空白を入れる
                if (i > 0 && w[i].is_content_word()) {
                    //自立語の場合
                    //「する」に対して、直前が名詞-サ変接続の場合には自立語でない
                    if (!w[i].getBasicString().equals("する")
                            || !w[i - 1].getPOS().equals("名詞-サ変接続")) {
                        res += "&nbsp;&nbsp;";
                    }
                }
                // 形態素情報をアンカーに仕込む
                res += "<a href=\"" + s + ":" + i + "\">";

                // 単語が簡単かどうかのアドバイス
                if (w[i].is_easy()) {
                    res += w[i].toString();
                } else if (w[i].is_difficult()) {
                    wordflag = false;
                    res += "<font color='#ff00ff'>" + w[i].toString() + "</font>";
                    evaluationPoints += "<font color='#ff00ff'>" + w[i].toString()
                            + "</font>: 難しい単語です。可能なら簡単な単語に置き換えましょう。<br/>";
                } else {
                    // 級外単語
                    wordflag = false;
                    res += "<font color='#ff0000'>" + w[i].toString() + "</font>";
                    evaluationPoints += "<font color='#ff0000'>" + w[i].toString()
                            + "</font>: ほとんど理解してもらえません。可能なら簡単な単語に置き換えてください。<br/>";
                }
                res += "</a>";
            }
            res += "<br/>";

            if (wordflag) {
                evaluationPoints += "難しい単語はありませんでした。<br/>";
            }

            //System.out.println(res);
            String[] advice = ejadv3.getRecommendations(w);
            for (int i = 0; i < advice.length; i++) {
                evaluationPoints += advice[i] + "<br/>";
            }
        }
        //hanasu.setMoraSpeed(scoreToMoraSpeed(score));  
        setAnalysisResult(res);
        setAnalysisPoint(evaluationPoints);
        hanasu.setTextFeature(feature);
        showMessage("");
    }

    public int scoreToMoraSpeed(double score){
        return 360;
    }
    
    /**
     * 入力テキストを結果ウィンドウに追加
     */
    public void commitText() {
        appendResultText(inputText.getText() + "\n");
        inputText.setText("");
        clearResults();
    }

    /**
     * メニューの「名前を指定して保存」のアクション
     */
    public int saveCreatedText() {
        try {
            if (fileSaver.save(resultText.getText(), null) == FileSaver.SAVED) {
                dataCreated = false;
                return FileSaver.SAVED;
            } else {
                return FileSaver.CANCELLED;
            }
        } catch (IOException e) {
            showMessage(e.toString());
            return FileSaver.ERROR;
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        morphemeInfoPopup = new javax.swing.JDialog();
        mpWordTitle = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        mpBasicForm = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        mpPOS = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        mpForm = new javax.swing.JLabel();
        mpPronunciation = new javax.swing.JLabel();
        jLabel9 = new javax.swing.JLabel();
        jLabel10 = new javax.swing.JLabel();
        mpGrade = new javax.swing.JLabel();
        jButton3 = new javax.swing.JButton();
        jButton4 = new javax.swing.JButton();
        filler1 = new javax.swing.Box.Filler(new java.awt.Dimension(100, 0), new java.awt.Dimension(100, 0), new java.awt.Dimension(100, 32767));
        jFrame1 = new javax.swing.JFrame("設定");
        jTabbedPane1 = new javax.swing.JTabbedPane();
        jPanel1 = new javax.swing.JPanel();
        jSlider1 = new javax.swing.JSlider(uiFontMinSize, uiFontMaxSize, uiFont.getSize());
        jComboBox1 = new javax.swing.JComboBox();
        jComboBox2 = new javax.swing.JComboBox();
        jSlider2 = new javax.swing.JSlider(textFontMinSize, textFontMaxSize, textFont.getSize());
        jLabel13 = new javax.swing.JLabel();
        jLabel14 = new javax.swing.JLabel();
        jLabel12 = new javax.swing.JLabel();
        jLabel15 = new javax.swing.JLabel();
        jLabel16 = new javax.swing.JLabel();
        jLabel17 = new javax.swing.JLabel();
        jLabel18 = new javax.swing.JLabel();
        jLabel19 = new javax.swing.JLabel();
        jLabel20 = new javax.swing.JLabel();
        jLabel21 = new javax.swing.JLabel();
        jButton6 = new javax.swing.JButton();
        jPanel2 = new javax.swing.JPanel();
        jCheckBox1 = new javax.swing.JCheckBox();
        jButton5 = new javax.swing.JButton();
        jLabel11 = new javax.swing.JLabel();
        jMenu1 = new javax.swing.JMenu();
        jLabel1 = new javax.swing.JLabel();
        jButton1 = new javax.swing.JButton();
        jButton2 = new javax.swing.JButton();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jSeparator1 = new javax.swing.JSeparator();
        jScrollPane2 = new javax.swing.JScrollPane();
        inputText = new javax.swing.JTextArea();
        jScrollPane1 = new javax.swing.JScrollPane();
        analysisPane = new javax.swing.JEditorPane();
        jScrollPane3 = new javax.swing.JScrollPane();
        evaluationPointPane = new javax.swing.JEditorPane();
        jScrollPane8 = new javax.swing.JScrollPane();
        resultText = new javax.swing.JTextArea();
        jScrollPane9 = new javax.swing.JScrollPane();
        exampleSentenceArea = new javax.swing.JTextArea();
        statusMessageLabel = new javax.swing.JLabel();
        menuBar = new javax.swing.JMenuBar();
        fileMenu = new javax.swing.JMenu();
        openMenuItem = new javax.swing.JMenuItem();
        saveMenuItem = new javax.swing.JMenuItem();
        saveAsMenuItem = new javax.swing.JMenuItem();
        exitMenuItem = new javax.swing.JMenuItem();
        editMenu = new javax.swing.JMenu();
        cutMenuItem = new javax.swing.JMenuItem();
        copyMenuItem = new javax.swing.JMenuItem();
        pasteMenuItem = new javax.swing.JMenuItem();
        deleteMenuItem = new javax.swing.JMenuItem();
        jMenu2 = new javax.swing.JMenu();
        jMenuItem1 = new javax.swing.JMenuItem();
        helpMenu = new javax.swing.JMenu();
        contentsMenuItem = new javax.swing.JMenuItem();
        aboutMenuItem = new javax.swing.JMenuItem();

        morphemeInfoPopup.setSize(new java.awt.Dimension(0, 0));

        mpWordTitle.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        mpWordTitle.setText("単語名");

        jLabel7.setText("原形");

        mpBasicForm.setText("jLabel8");

        jLabel6.setText("品詞");

        mpPOS.setText("jLabel8");

        jLabel8.setText("活用形");

        mpForm.setText("jLabel9");

        mpPronunciation.setText("xxx");

        jLabel9.setText("読み");

        jLabel10.setText("級");

        mpGrade.setText("jLabel11");

        jButton3.setText("関連する文例");
        jButton3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton3ActionPerformed(evt);
            }
        });

        jButton4.setText("閉じる");
        jButton4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton4ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout morphemeInfoPopupLayout = new javax.swing.GroupLayout(morphemeInfoPopup.getContentPane());
        morphemeInfoPopup.getContentPane().setLayout(morphemeInfoPopupLayout);
        morphemeInfoPopupLayout.setHorizontalGroup(
            morphemeInfoPopupLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(morphemeInfoPopupLayout.createSequentialGroup()
                .addGroup(morphemeInfoPopupLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(morphemeInfoPopupLayout.createSequentialGroup()
                        .addGap(15, 15, 15)
                        .addGroup(morphemeInfoPopupLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(morphemeInfoPopupLayout.createSequentialGroup()
                                .addGroup(morphemeInfoPopupLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jLabel7)
                                    .addComponent(jLabel6)
                                    .addComponent(jLabel8)
                                    .addComponent(jLabel9)
                                    .addComponent(jLabel10))
                                .addGap(27, 27, 27)
                                .addGroup(morphemeInfoPopupLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(mpGrade)
                                    .addComponent(mpForm)
                                    .addComponent(mpPOS)
                                    .addComponent(mpBasicForm)
                                    .addComponent(mpPronunciation)))
                            .addGroup(morphemeInfoPopupLayout.createSequentialGroup()
                                .addComponent(jButton3)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jButton4))))
                    .addGroup(morphemeInfoPopupLayout.createSequentialGroup()
                        .addGap(81, 81, 81)
                        .addComponent(mpWordTitle))
                    .addComponent(filler1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(15, 15, 15))
        );
        morphemeInfoPopupLayout.setVerticalGroup(
            morphemeInfoPopupLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(morphemeInfoPopupLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(mpWordTitle)
                .addGap(0, 0, 0)
                .addComponent(filler1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(9, 9, 9)
                .addGroup(morphemeInfoPopupLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel7)
                    .addComponent(mpBasicForm))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(morphemeInfoPopupLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel6)
                    .addComponent(mpPOS))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(morphemeInfoPopupLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel8)
                    .addComponent(mpForm))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(morphemeInfoPopupLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(mpPronunciation)
                    .addComponent(jLabel9))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(morphemeInfoPopupLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel10)
                    .addComponent(mpGrade))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(morphemeInfoPopupLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButton3)
                    .addComponent(jButton4))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jSlider1.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                jSlider1StateChanged(evt);
            }
        });

        jComboBox1.setModel(new javax.swing.DefaultComboBoxModel(availableFontFamilyNames));
        jComboBox1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jComboBox1ActionPerformed(evt);
            }
        });

        jComboBox2.setModel(new javax.swing.DefaultComboBoxModel(availableFontFamilyNames));
        jComboBox2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jComboBox2ActionPerformed(evt);
            }
        });

        jSlider2.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                jSlider2StateChanged(evt);
            }
        });

        jLabel13.setText("メニュー");

        jLabel14.setText("テキスト");

        jLabel12.setText("フォント");

        jLabel15.setText("フォント");

        jLabel16.setText("文字サイズ");

        jLabel17.setText("文字サイズ");

        jLabel18.setText("A");

        jLabel19.setText("A");

        jLabel20.setText("A");

        jLabel21.setText("A");

        jButton6.setText("リセット");
        jButton6.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton6ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(10, 10, 10)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel12)
                            .addComponent(jLabel16)
                            .addComponent(jLabel17)
                            .addComponent(jLabel15))
                        .addGap(28, 28, 28)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jComboBox2, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(jLabel20)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jSlider2, javax.swing.GroupLayout.DEFAULT_SIZE, 515, Short.MAX_VALUE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel21))
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                                .addComponent(jLabel18)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jSlider1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel19))
                            .addComponent(jComboBox1, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel14)
                            .addComponent(jLabel13)
                            .addComponent(jButton6))
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel13)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(jComboBox1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel12))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(jLabel16)
                    .addComponent(jSlider1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel18)
                    .addComponent(jLabel19))
                .addGap(46, 46, 46)
                .addComponent(jLabel14)
                .addGap(10, 10, 10)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jComboBox2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel15))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(jLabel17)
                    .addComponent(jLabel20)
                    .addComponent(jSlider2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel21))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 217, Short.MAX_VALUE)
                .addComponent(jButton6)
                .addContainerGap())
        );

        jTabbedPane1.addTab("文字のサイズ", jPanel1);

        jCheckBox1.setText("音声合成を使用する");
        jCheckBox1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBox1ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jCheckBox1)
                .addContainerGap(522, Short.MAX_VALUE))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(14, 14, 14)
                .addComponent(jCheckBox1)
                .addContainerGap(425, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("音声合成", jPanel2);

        jButton5.setText("OK");
        jButton5.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton5ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jFrame1Layout = new javax.swing.GroupLayout(jFrame1.getContentPane());
        jFrame1.getContentPane().setLayout(jFrame1Layout);
        jFrame1Layout.setHorizontalGroup(
            jFrame1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jTabbedPane1, javax.swing.GroupLayout.Alignment.TRAILING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jFrame1Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jButton5)
                .addContainerGap())
        );
        jFrame1Layout.setVerticalGroup(
            jFrame1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jFrame1Layout.createSequentialGroup()
                .addComponent(jTabbedPane1)
                .addGap(13, 13, 13)
                .addComponent(jButton5)
                .addContainerGap())
        );

        jLabel11.setText("jLabel11");

        jMenu1.setText("jMenu1");

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jLabel1.setText("入力文");

        jButton1.setText("評価");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        jButton2.setText("追加");
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });

        jLabel2.setText("分析結果");

        jLabel3.setText("評価ポイント");

        jLabel4.setText("作成済みテキスト");

        jLabel5.setText("文例");

        inputText.setColumns(20);
        inputText.setRows(5);
        inputText.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                inputTextFocusGained(evt);
            }
        });
        jScrollPane2.setViewportView(inputText);

        analysisPane.setEditable(false);
        analysisPane.setContentType("text/html"); // NOI18N
        analysisPane.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                analysisPaneFocusGained(evt);
            }
        });
        jScrollPane1.setViewportView(analysisPane);

        evaluationPointPane.setEditable(false);
        evaluationPointPane.setContentType("text/html"); // NOI18N
        evaluationPointPane.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                evaluationPointPaneFocusGained(evt);
            }
        });
        jScrollPane3.setViewportView(evaluationPointPane);

        resultText.setColumns(20);
        resultText.setFont(textFont);
        resultText.setRows(5);
        resultText.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                resultTextFocusGained(evt);
            }
        });
        jScrollPane8.setViewportView(resultText);

        exampleSentenceArea.setEditable(false);
        exampleSentenceArea.setColumns(20);
        exampleSentenceArea.setRows(5);
        exampleSentenceArea.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                exampleSentenceAreaFocusGained(evt);
            }
        });
        jScrollPane9.setViewportView(exampleSentenceArea);

        statusMessageLabel.setText("Ready");

        fileMenu.setMnemonic('f');
        fileMenu.setText("ファイル");

        openMenuItem.setMnemonic('o');
        openMenuItem.setText("開く");
        openMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(openMenuItem);

        saveMenuItem.setMnemonic('s');
        saveMenuItem.setText("保存");
        fileMenu.add(saveMenuItem);

        saveAsMenuItem.setMnemonic('a');
        saveAsMenuItem.setText("名前をつけて保存");
        saveAsMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveAsMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(saveAsMenuItem);

        exitMenuItem.setMnemonic('x');
        exitMenuItem.setText("終了");
        exitMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exitMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(exitMenuItem);

        menuBar.add(fileMenu);

        editMenu.setMnemonic('e');
        editMenu.setText("編集");

        cutMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_X, java.awt.event.InputEvent.CTRL_MASK));
        cutMenuItem.setMnemonic('t');
        cutMenuItem.setText("切り取り");
        cutMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cutMenuItemActionPerformed(evt);
            }
        });
        editMenu.add(cutMenuItem);

        copyMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_C, java.awt.event.InputEvent.CTRL_MASK));
        copyMenuItem.setMnemonic('y');
        copyMenuItem.setText("コピー");
        copyMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                copyMenuItemActionPerformed(evt);
            }
        });
        editMenu.add(copyMenuItem);

        pasteMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_V, java.awt.event.InputEvent.CTRL_MASK));
        pasteMenuItem.setMnemonic('p');
        pasteMenuItem.setText("ペースト");
        pasteMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                pasteMenuItemActionPerformed(evt);
            }
        });
        editMenu.add(pasteMenuItem);

        deleteMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_D, java.awt.event.InputEvent.CTRL_MASK));
        deleteMenuItem.setMnemonic('d');
        deleteMenuItem.setText("削除");
        editMenu.add(deleteMenuItem);

        menuBar.add(editMenu);

        jMenu2.setText("ツール");

        jMenuItem1.setText("設定");
        jMenuItem1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem1ActionPerformed(evt);
            }
        });
        jMenu2.add(jMenuItem1);

        menuBar.add(jMenu2);

        helpMenu.setMnemonic('h');
        helpMenu.setText("ヘルプ");

        contentsMenuItem.setMnemonic('c');
        contentsMenuItem.setText("ヘルプの目次");
        contentsMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                contentsMenuItemActionPerformed(evt);
            }
        });
        helpMenu.add(contentsMenuItem);

        aboutMenuItem.setMnemonic('a');
        aboutMenuItem.setText("バージョン情報");
        aboutMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                aboutMenuItemActionPerformed(evt);
            }
        });
        helpMenu.add(aboutMenuItem);

        menuBar.add(helpMenu);

        setJMenuBar(menuBar);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jScrollPane2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jButton1)
                            .addComponent(jButton2)))
                    .addComponent(jLabel2))
                .addContainerGap())
            .addComponent(jScrollPane1)
            .addComponent(jScrollPane3)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jLabel3)
                .addGap(0, 0, Short.MAX_VALUE))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addComponent(statusMessageLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator1))
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel4)
                    .addComponent(jScrollPane8, javax.swing.GroupLayout.DEFAULT_SIZE, 356, Short.MAX_VALUE))
                .addGap(5, 5, 5)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane9, javax.swing.GroupLayout.DEFAULT_SIZE, 441, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel5)
                        .addGap(0, 0, Short.MAX_VALUE))))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel1)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jButton1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButton2))
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 100, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 134, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel3)
                .addGap(2, 2, 2)
                .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 170, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4)
                    .addComponent(jLabel5))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane9)
                    .addComponent(jScrollPane8))
                .addGap(21, 21, 21)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(statusMessageLabel)))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void exitMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exitMenuItemActionPerformed
        windowClosingConfirmation();
    }//GEN-LAST:event_exitMenuItemActionPerformed

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
        commitText();
    }//GEN-LAST:event_jButton2ActionPerformed

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        try {
            doAnalysis();
        } catch (UnsupportedEncodingException ex) {
            showMessage(ex.toString());
        }
    }//GEN-LAST:event_jButton1ActionPerformed

    private void saveAsMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveAsMenuItemActionPerformed
        saveCreatedText();
    }//GEN-LAST:event_saveAsMenuItemActionPerformed

    private void aboutMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_aboutMenuItemActionPerformed
        JOptionPane.showMessageDialog(this, TITLE+"\n"+author+"\n\n\n"+copyrightTextToSpeech, "このソフトウェアについて", JOptionPane.INFORMATION_MESSAGE);
    }//GEN-LAST:event_aboutMenuItemActionPerformed

    private void openMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_openMenuItemActionPerformed
        JFileChooser fileChooser = new JFileChooser();

        fileChooser.setFileFilter(new FileNameExtensionFilter("*.txt", "txt"));
        int selected = fileChooser.showOpenDialog(this);
        if (selected == JFileChooser.APPROVE_OPTION) {
            String filename = fileChooser.getSelectedFile().getPath();
            try {
                byte fileContentBytes[] = Files.readAllBytes(Paths.get(filename));
                String fileContentStr = new String(fileContentBytes);
                inputText.insert(fileContentStr, 0);
            } catch (IOException e) {
                //System.out.println("catch exception");
            }
        }
    }//GEN-LAST:event_openMenuItemActionPerformed

    private void contentsMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_contentsMenuItemActionPerformed
        app.showHelp();
    }//GEN-LAST:event_contentsMenuItemActionPerformed

    private void cutMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cutMenuItemActionPerformed
        currentFocus.cut();
    }//GEN-LAST:event_cutMenuItemActionPerformed

    private void copyMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_copyMenuItemActionPerformed
        currentFocus.copy();
    }//GEN-LAST:event_copyMenuItemActionPerformed

    private void inputTextFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_inputTextFocusGained
        currentFocus = inputText;
    }//GEN-LAST:event_inputTextFocusGained

    private void analysisPaneFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_analysisPaneFocusGained
        currentFocus = analysisPane;
    }//GEN-LAST:event_analysisPaneFocusGained

    private void evaluationPointPaneFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_evaluationPointPaneFocusGained
        currentFocus = evaluationPointPane;
    }//GEN-LAST:event_evaluationPointPaneFocusGained

    private void resultTextFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_resultTextFocusGained
        currentFocus = resultText;
    }//GEN-LAST:event_resultTextFocusGained

    private void exampleSentenceAreaFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_exampleSentenceAreaFocusGained
        currentFocus = exampleSentenceArea;
    }//GEN-LAST:event_exampleSentenceAreaFocusGained

    private void pasteMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pasteMenuItemActionPerformed
        currentFocus.paste();
    }//GEN-LAST:event_pasteMenuItemActionPerformed

    private void jButton4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton4ActionPerformed
        morphemeInfoPopup.setVisible(false);
    }//GEN-LAST:event_jButton4ActionPerformed

    private void jButton3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton3ActionPerformed
        EJExample[] res = ejadv3.exampleSentence(currentPopup);
        setUsableWords("");
        if (res == null || res[0] == null) {
            appendUsableWords("<<該当なし>>");
            return;
        }
        for (int i = 0; i < res.length; i++) {
            appendUsableWords(res[i].EJ() + "\n");
        }
        rewindUsableWords();
    }//GEN-LAST:event_jButton3ActionPerformed

    private void jButton5ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton5ActionPerformed
        // TODO add your handling code here:
        jFrame1.setVisible(false);
        if (speechSynthesis) {
            hanasu.setVisible(true);
        } else {
            hanasu.setVisible(false);
        }
    }//GEN-LAST:event_jButton5ActionPerformed

    private void jMenuItem1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem1ActionPerformed
        // TODO add your handling code here:
        jFrame1.pack();
        jFrame1.setVisible(true);
    }//GEN-LAST:event_jMenuItem1ActionPerformed

    private void jButton6ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton6ActionPerformed
        // TODO add your handling code here:
        textFont = defaultTextFont;
        updateTextFont();

        uiFont = defaultUIFont;
        updateUIFont();
    }//GEN-LAST:event_jButton6ActionPerformed

    private void jSlider2StateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_jSlider2StateChanged
        // TODO add your handling code here:
        int value = jSlider2.getValue();
        textFont = textFont.deriveFont((float) value);
        updateTextFont();
    }//GEN-LAST:event_jSlider2StateChanged

    private void jComboBox2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jComboBox2ActionPerformed
        // TODO add your handling code here:
        int selectItem = jComboBox2.getSelectedIndex();
        if (selectItem != -1) {
            String fontFamilyName = (String) jComboBox2.getSelectedItem();
            textFont = new Font(fontFamilyName, Font.PLAIN, textFont.getSize());
        }
        updateTextFont();
    }//GEN-LAST:event_jComboBox2ActionPerformed

    private void jComboBox1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jComboBox1ActionPerformed
        // TODO add your handling code here:
        int selectItem = jComboBox1.getSelectedIndex();
        if (selectItem != -1) {
            String fontFamilyName = (String) jComboBox1.getSelectedItem();
            uiFont = new Font(fontFamilyName, Font.PLAIN, uiFont.getSize());
        }
        updateUIFont();
    }//GEN-LAST:event_jComboBox1ActionPerformed

    private void jSlider1StateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_jSlider1StateChanged
        // TODO add your handling code here:
        int value = jSlider1.getValue();
        uiFont = uiFont.deriveFont((float) value);
        updateUIFont();
    }//GEN-LAST:event_jSlider1StateChanged

    private void jCheckBox1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBox1ActionPerformed
        // TODO add your handling code here:
        speechSynthesis = jCheckBox1.isSelected();
    }//GEN-LAST:event_jCheckBox1ActionPerformed

    /**
     * @param args the command line arguments
     */

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JMenuItem aboutMenuItem;
    private javax.swing.JEditorPane analysisPane;
    private javax.swing.JMenuItem contentsMenuItem;
    private javax.swing.JMenuItem copyMenuItem;
    private javax.swing.JMenuItem cutMenuItem;
    private javax.swing.JMenuItem deleteMenuItem;
    private javax.swing.JMenu editMenu;
    private javax.swing.JEditorPane evaluationPointPane;
    private javax.swing.JTextArea exampleSentenceArea;
    private javax.swing.JMenuItem exitMenuItem;
    private javax.swing.JMenu fileMenu;
    private javax.swing.Box.Filler filler1;
    private javax.swing.JMenu helpMenu;
    private javax.swing.JTextArea inputText;
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton3;
    private javax.swing.JButton jButton4;
    private javax.swing.JButton jButton5;
    private javax.swing.JButton jButton6;
    private javax.swing.JCheckBox jCheckBox1;
    private javax.swing.JComboBox jComboBox1;
    private javax.swing.JComboBox jComboBox2;
    private javax.swing.JFrame jFrame1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel17;
    private javax.swing.JLabel jLabel18;
    private javax.swing.JLabel jLabel19;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel20;
    private javax.swing.JLabel jLabel21;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenu jMenu2;
    private javax.swing.JMenuItem jMenuItem1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane8;
    private javax.swing.JScrollPane jScrollPane9;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSlider jSlider1;
    private javax.swing.JSlider jSlider2;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JMenuBar menuBar;
    private javax.swing.JDialog morphemeInfoPopup;
    private javax.swing.JLabel mpBasicForm;
    private javax.swing.JLabel mpForm;
    private javax.swing.JLabel mpGrade;
    private javax.swing.JLabel mpPOS;
    private javax.swing.JLabel mpPronunciation;
    private javax.swing.JLabel mpWordTitle;
    private javax.swing.JMenuItem openMenuItem;
    private javax.swing.JMenuItem pasteMenuItem;
    private javax.swing.JTextArea resultText;
    private javax.swing.JMenuItem saveAsMenuItem;
    private javax.swing.JMenuItem saveMenuItem;
    private javax.swing.JLabel statusMessageLabel;
    // End of variables declaration//GEN-END:variables

}
