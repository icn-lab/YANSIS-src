/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package ejadvisor3;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

/**
 * ファイルを保存するためのクラス
 * @author aito
 */
public class FileSaver {
    public static final int SAVED = 0;
    public static final int CANCELLED = 1;
    public static final int ERROR = 2;
    private JFrame parent;
    private JFileChooser chooser;
    // extension は {".拡張子",".拡張子",...} の形式
    private String[] extensions;
    private String description;
    private String lastFileName;
    
    public FileSaver(JFrame parent, JFileChooser chooser) {
        this.parent = parent;
        if (chooser == null)
            this.chooser = new JFileChooser();
        else
            this.chooser = chooser;
        extensions = null;
        description = "";
        lastFileName = null;
    }
    public void setExtensions(String[] ext) {
        extensions = ext;
    }
    public void setDescription(String desc) {
        description = desc;
    }
    private boolean acceptFilename(String filename) {
        //拡張子が指定されていない場合は何でもアクセプト
        if (extensions == null)
            return true;
        for (int i = 0; i < extensions.length; i++) {
            if (filename.endsWith(extensions[i]))
                return true;
        }
        return false;
    }
    public int save(String text, File currentDirectory) throws IOException {
        chooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
            @Override
            public boolean accept(File file) {
                if (file.isDirectory())
                    return true;
                return acceptFilename(file.getName());
            }
            @Override
            public String getDescription() {
                return description;
            }
            });
        if (currentDirectory != null) {
            chooser.setCurrentDirectory(currentDirectory);
        }
        int ret_val = chooser.showSaveDialog(null);
        if (ret_val != JFileChooser.APPROVE_OPTION) {
            return CANCELLED;
        }
        String file = chooser.getSelectedFile().getPath();
        if (!file.endsWith(".txt")) {
            file = file + ".txt";
        }
        // file の存在チェック
        if (new File(file).exists()) {
            int confirm = JOptionPane.showConfirmDialog(
                    parent,
                    "ファイルが存在します。上書きしますか？",
                    "ファイル保存の確認",
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.WARNING_MESSAGE);
            if (confirm == JOptionPane.NO_OPTION) {
                return save(text, chooser.getCurrentDirectory());
            } else if (confirm == JOptionPane.CANCEL_OPTION) {
                return CANCELLED;
            }
        }
        FileOutputStream fos = new FileOutputStream(file);
        try (PrintWriter bw = new PrintWriter(new OutputStreamWriter(fos,"SHIFT_JIS"))) {
            bw.print(text);
        }
        lastFileName = file;
        return SAVED;
    }
}
