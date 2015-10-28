/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package ejadvisor3;
import jplevelanalyzer.*;
import net.java.sen.Token;
import net.java.sen.StringTagger;
import java.io.*;
import jplevelanalyzer.WordWithGrade;

/**
 *
 * @author aito
 */
public class WordPropertyFactory {
    private final StringTagger tagger;
    private final VocabAnalyzer va;

    public WordPropertyFactory(EJConfig conf) throws IOException {
        String path = conf.sen_conf;
        tagger = StringTagger.getInstance(path);
        //tagger = StringTagger.getInstance();
	va = new VocabAnalyzer(conf.grade);

    }
    public WordProperty[] analyzeText(String text) throws IOException {
        Token toks[] = tagger.analyze(text);
        WordWithGrade[] res = va.analyze(toks);
        WordProperty ret_val[] = new WordProperty[res.length];
        for (int i = 0; i < res.length; i++) {
            ret_val[i] = new WordProperty(res[i]);
        }
        return ret_val;
    }
    
}
