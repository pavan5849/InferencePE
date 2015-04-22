package org.apache.jena.propertytable.lang;

import static org.apache.jena.riot.RDFLanguages.CSV ;

import java.io.InputStream ;
import java.io.Reader ;

import org.apache.jena.atlas.web.ContentType ;
import org.apache.jena.riot.Lang ;
import org.apache.jena.riot.ReaderRIOT ;
import org.apache.jena.riot.lang.LangRIOT ;
import org.apache.jena.riot.system.ErrorHandler ;
import org.apache.jena.riot.system.ErrorHandlerFactory ;
import org.apache.jena.riot.system.ParserProfile ;
import org.apache.jena.riot.system.StreamRDF ;

import com.hp.hpl.jena.sparql.util.Context ;

public class ReaderRIOTLangCSV implements ReaderRIOT
{
    private final Lang lang ;
    private ErrorHandler errorHandler ; 
    private ParserProfile parserProfile = null ;

    public ReaderRIOTLangCSV(Lang lang) {
        this.lang = lang ;
        errorHandler = ErrorHandlerFactory.getDefaultErrorHandler() ;
    }

    @Override
    public void read(InputStream in, String baseURI, ContentType ct, StreamRDF output, Context context) {
        if ( lang != CSV)
            throw new IllegalArgumentException("The Lang must be 'CSV'!");
        LangRIOT parser = new LangCSV (in, baseURI, baseURI, ErrorHandlerFactory.getDefaultErrorHandler(),  output);
        if ( parserProfile != null )
            parser.setProfile(parserProfile);
        if ( errorHandler != null )
            parser.getProfile().setHandler(errorHandler) ;
        parser.parse() ;
    }

    @Override
    public void read(Reader in, String baseURI, ContentType ct, StreamRDF output, Context context) {
    	if ( lang != CSV)
    	    throw new IllegalArgumentException("The Lang must be 'CSV'!");
    	LangRIOT parser = new LangCSV (in, baseURI, baseURI, ErrorHandlerFactory.getDefaultErrorHandler(),  output);
        if ( parserProfile != null )
            parser.setProfile(parserProfile);
        if ( errorHandler != null )
            parser.getProfile().setHandler(errorHandler) ;
        parser.parse() ;
    }

    @Override public ErrorHandler getErrorHandler()                     { return errorHandler ; }
    @Override public void setErrorHandler(ErrorHandler errorHandler)    { this.errorHandler = errorHandler ; }

    @Override public ParserProfile getParserProfile()                   { return parserProfile ; } 
    @Override public void setParserProfile(ParserProfile parserProfile) { this.parserProfile = parserProfile ; }
}