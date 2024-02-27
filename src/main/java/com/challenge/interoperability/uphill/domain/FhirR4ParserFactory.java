package com.challenge.interoperability.uphill.domain;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.parser.StrictErrorHandler;

public class FhirR4ParserFactory {

    private static IParser parser;

    public static IParser getParser() {
        if(parser == null) {
            /* The StrictErrorHandler will cause 500 error in case of invalid resources according
               to FHIR specification - instead it should be handled as a Bad Request and return an OperationOutcome
            */
            FhirContext ctx = FhirContext.forR4().setParserErrorHandler(new StrictErrorHandler());
            parser = ctx.newJsonParser().setParserErrorHandler(new StrictErrorHandler());
        }
        return parser;
    }
}
