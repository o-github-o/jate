package uk.ac.shef.dcs.jate.app;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.NIOFSDirectory;
import uk.ac.shef.dcs.jate.JATEException;
import uk.ac.shef.dcs.jate.JATEProperties;
import uk.ac.shef.dcs.jate.algorithm.TermEx;
import uk.ac.shef.dcs.jate.algorithm.TermInfoCollector;
import uk.ac.shef.dcs.jate.feature.*;
import uk.ac.shef.dcs.jate.model.JATETerm;
import uk.ac.shef.dcs.jate.util.SolrUtil;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

/**
 * Created by zqz on 24/09/2015.
 */
public class AppTermEx extends App {
    public static void main(String[] args) throws JATEException, IOException {
        if (args.length < 1) {
            printHelp();
            System.exit(1);
        }
        String indexPath = args[args.length - 2];
        String jatePropertyFile=args[args.length - 1];
        Map<String, String> params = getParams(args);

        List<JATETerm> terms = new AppTermEx().extract(indexPath, jatePropertyFile, params);
        String paramValue=params.get("-o");
        write(terms,paramValue);

    }

    @Override
    public List<JATETerm> extract(String indexPath, String jatePropertyFile, Map<String, String> params) throws IOException, JATEException {
        IndexReader indexReader = SolrUtil.getIndexReader(indexPath);
        JATEProperties properties = new JATEProperties(jatePropertyFile);
        FrequencyTermBasedFBMaster ftbb = new
                FrequencyTermBasedFBMaster(indexReader, properties, 0);
        FrequencyTermBased ftb = (FrequencyTermBased)ftbb.build();

        FrequencyTermBasedFBMaster fwbb = new
                FrequencyTermBasedFBMaster(indexReader, properties, 1);
        FrequencyTermBased fwb = (FrequencyTermBased)fwbb.build();

        TTFReferenceFeatureFileBuilder ftrb = new
                TTFReferenceFeatureFileBuilder(params.get("-r"));
        FrequencyTermBased frb = ftrb.build();

        FrequencyCtxDocBasedFBMaster fdbb = new
                FrequencyCtxDocBasedFBMaster(indexReader, properties, 0);
        FrequencyCtxBased fdb = (FrequencyCtxBased) fdbb.build();

        TermEx termex = new TermEx();
        termex.registerFeature(FrequencyTermBased.class.getName(), ftb);
        termex.registerFeature(FrequencyTermBased.class.getName()+TermEx.SUFFIX_WORD, fwb);
        //you can register multiple reference feature, as long as the key has prefix = FrequencyTermBased.class.getName()+TermEx.SUFFIX_REF
        termex.registerFeature(FrequencyTermBased.class.getName()+TermEx.SUFFIX_REF, frb);
        termex.registerFeature(FrequencyCtxBased.class.getName()+TermEx.SUFFIX_DOC, fdb);

        List<JATETerm> terms=termex.execute(ftb.getMapTerm2TTF().keySet());
        terms=applyThresholds(terms, params.get("-t"), params.get("-n"));
        String paramValue=params.get("-c");
        if(paramValue!=null &&paramValue.equalsIgnoreCase("true")) {
            collectTermInfo(indexReader, terms, properties.getSolrFieldnameJATENGramInfo(),
                    properties.getSolrFieldnameID());
        }

        indexReader.close();
        return terms;
    }

    protected static void printHelp() {
        StringBuilder sb = new StringBuilder("TermEx Usage:\n");
        sb.append("java -cp '[CLASSPATH]' ").append(AppATTF.class.getName())
                .append(" [OPTIONS] ").append("-r [REF_TERM_TF_FILE] [LUCENE_INDEX_PATH] [JATE_PROPERTY_FILE]").append("\nE.g.:\n");
        sb.append("java -cp '/libs/*' -t 20 -r /resource/bnc_unifrqs.normal /solr/server/solr/jate/data jate.properties ...\n\n");
        sb.append("[OPTIONS]:\n")
                .append("\t\t-c\t\t'true' or 'false'. Whether to collect term information, e.g., offsets in documents. Default is false.\n")
                .append("\t\t-t\t\tA number. Score threshold for selecting terms. If not set then default -n is used.").append("\n")
                .append("\t\t-n\t\tA number. If an integer is given, top N candidates are selected as terms. \n")
                .append("\t\t\t\tIf a decimal number is given, top N% of candidates are selected. Default is 0.25.\n");
        sb.append("\t\t-o\t\tA file path. If provided, the output is written to the file. \n")
                .append("\t\t\t\tOtherwise, output is written to the console.");
        System.out.println(sb);
    }
}