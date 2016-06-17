/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package ejadvisor3;

import java.awt.Desktop;
import java.awt.EventQueue;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.ArrayList;

/**
 *
 * @author Akinori
 */
public class EJAdvisor3App {
    public EJAdvisor3 ejadv3;
    static EJAdvisor3GUI view;
    private static String base;
    private static Splash splash;
 
    public static void determineBase(String args[]) {
        base = "./";
        if (args.length > 0) {
            base = args[0];
        }        
    }
    
    /**
     * Initialize myself
     */
    void initialize(String args[]) {
        showSplash();
        ejadv3 = new EJAdvisor3(base);
        splash.setMessage("語彙ファイルを読み込んでいます...");
        ejadv3.loadVocab();
        splash.setMessage("Senを読み込んでいます...");
        ejadv3.loadSen();
        splash.setMessage("アドバイスを読み込んでいます...");
        ejadv3.loadRecommend();
        splash.setMessage("スコア解析器を読み込んでいます...");
        ejadv3.loadScoreEstimator();
        hideSplash();
    }
      
    /**
     * ヘルプを表示
     */
    public void showHelp() {
        try {
            URI uri = new File(base+"help/index.html").toURI().normalize();
            System.out.println("URI:"+uri);
            Desktop.getDesktop().browse(uri);
        } catch (java.io.IOException e) {
            view.showMessage(e.toString());
        } 
    }
    
    private static void showSplash() {
        EventQueue.invokeLater(new Runnable() {
           public void run() {
               splash.setVisible(true);
           } 
        });         
    }
    
    private static void hideSplash() {
        EventQueue.invokeLater(new Runnable() {
           public void run() {
               splash.setVisible(false);
           } 
        });        
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(EJAdvisor3view.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(EJAdvisor3view.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(EJAdvisor3view.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(EJAdvisor3view.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        determineBase(args);
        splash = new Splash(base);
        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                EJAdvisor3App app = new EJAdvisor3App();
                app.initialize(args);
                view = new EJAdvisor3view(app);
                view.setVisible(true);
            }
        });
    }
    
}
