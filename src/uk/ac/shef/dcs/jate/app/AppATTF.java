package uk.ac.shef.dcs.jate.app;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.NIOFSDirectory;
import org.apache.lucene.store.SimpleFSDirectory;
import uk.ac.shef.dcs.jate.JATEException;
import uk.ac.shef.dcs.jate.JATEProperties;
import uk.ac.shef.dcs.jate.algorithm.ATTF;
import uk.ac.shef.dcs.jate.algorithm.Algorithm;
import uk.ac.shef.dcs.jate.algorithm.TermInfoCollector;
import uk.ac.shef.dcs.jate.feature.FrequencyTermBased;
import uk.ac.shef.dcs.jate.feature.FrequencyTermBasedFBMaster;
import uk.ac.shef.dcs.jate.model.JATETerm;
import uk.ac.shef.dcs.jate.util.SolrUtil;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;


public class AppATTF extends App {

    public static void main(String[] args) throws JATEException, IOException {
        if (args.length < 1) {
            printHelp();
            System.exit(1);
        }
        String indexPath = args[args.length - 2];
        String jatePropertyFile = args[args.length-1];
        Map<String, String> params = getParams(args);

        String paramValue=params.get("-o");
        List<JATETerm> terms = new AppATTF().extract(indexPath, jatePropertyFile, params);
        write(terms,paramValue);
    }

    @Override
    public List<JATETerm> extract(String indexPath, String jatePropertyFile, Map<String, String> params) throws IOException, JATEException {
        IndexReader indexReader = SolrUtil.getIndexReader(indexPath);
        JATEProperties properties = new JATEProperties(jatePropertyFile);
        FrequencyTermBasedFBMaster featureBuilder = new
                FrequencyTermBasedFBMaster(indexReader, properties, 0);
        FrequencyTermBased feature = (FrequencyTermBased)featureBuilder.build();
        Algorithm attf = new ATTF();
        attf.registerFeature(FrequencyTermBased.class.getName(), feature);

        List<JATETerm> terms=attf.execute(feature.getMapTerm2TTF().keySet());
        terms=applyThresholds(terms, params.get("-t"), params.get("-n"));
        String paramValue=params.get("-c");
        if(paramValue!=null &&paramValue.equalsIgnoreCase("true")) {
            collectTermInfo(indexReader, terms, properties.getSolrFieldnameJATENGramInfo(),
                    properties.getSolrFieldnameID());
        }
        indexReader.close();
        return terms;
    }
}