/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package ejadvisor3;

/**
 *
 * @author Akinori
 */
public interface EJAdvisor3GUI {
    /**
     * ステータスバーにメッセージを表示する．
     * @param mesg メッセージ
     */
     public void showMessage(String mesg);
     /**
     * 解析結果の部分をクリア
     */
    public void clearResults();
     /**
     * 解析結果を挿入(HTML)
     */
    public void setAnalysisResult(String htmlForInsert);
    /**
     * 評価ポイントを挿入(HTML)
     */
    public void setAnalysisPoint(String htmlForInsert);
    /**
     * 作成結果テキストを挿入(plain text)
     */
    public void setResultText(String plaintextForInsert);
    /**
     * 作成結果テキストを追加(plain text)
     */
    public void appendResultText(String plaintextForInsert);
    /**
     * 〔使える単語〕を挿入
     */
    public void setUsableWords(String words);
    /**
     * 〔使える単語〕を追加
     */
    public void appendUsableWords(String words);
    /**
     * 〔使える単語〕のスクロール位置を変更
     */
    public void rewindUsableWords();

    public void setVisible(boolean b);

    
}
