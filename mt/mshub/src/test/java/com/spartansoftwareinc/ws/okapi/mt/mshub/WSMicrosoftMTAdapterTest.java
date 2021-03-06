package com.spartansoftwareinc.ws.okapi.mt.mshub;

import com.idiominc.wssdk.WSContext;
import com.idiominc.wssdk.WSException;
import com.idiominc.wssdk.WSObject;
import com.idiominc.wssdk.ais.WSAclPermission;
import com.idiominc.wssdk.ais.WSAisException;
import com.idiominc.wssdk.ais.WSAisManager;
import com.idiominc.wssdk.ais.WSNode;
import com.idiominc.wssdk.ais.WSNodeType;
import com.idiominc.wssdk.ais.WSSystemPropertyKey;
import com.idiominc.wssdk.component.mt.WSMTRequest;
import com.idiominc.wssdk.linguistic.WSLanguage;
import com.idiominc.wssdk.mt.WSMTResult;
import com.idiominc.wssdk.security.acl.WSAcl;
import com.idiominc.wssdk.user.WSUser;

import net.sf.okapi.common.IParameters;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.query.QueryResult;
import net.sf.okapi.common.resource.TextFragment;
import net.sf.okapi.connectors.microsoft.MicrosoftMTConnector;
import net.sf.okapi.connectors.microsoft.Parameters;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class WSMicrosoftMTAdapterTest {

    @Mock
    private WSContext wsContext;

    @Mock
    private WSAisManager wsAisManager;

    @Mock
    private WSNode localeMapAisNode;

    @Mock
    private WSLanguage wsLanguage;

    @Mock
    private MicrosoftMTConnector mtConnector;

    @Mock(extraInterfaces = {IParameters.class})
    private Parameters parameters;

    @Test
    public void testSomeFilteredResponses() {
        WSMicrosoftMTAdapter mtAdapter = spy(new WSMicrosoftMTAdapter());
        doReturn(mtConnector).when(mtAdapter).getMicrosoftMTConnector();
        when(mtConnector.getParameters()).thenReturn(parameters);
        when(wsLanguage.getLocale()).thenReturn(Locale.ENGLISH);
        when(mtConnector.batchQueryText(anyList())).thenReturn(mockBatchQueryReturns(
                "First segment", "Third segment"
        ));
        WSMTRequest[] requests = composeWSMTRequests("First segment", "Second segment", "Third segment");
        mtAdapter.translate(wsContext, requests, wsLanguage, wsLanguage);
        assertEquals(1, requests[0].getMTResults().length);
        assertEquals(0, requests[1].getMTResults().length);
        assertEquals(1, requests[2].getMTResults().length);
        assertEquals(requests[0].getMTResults()[0].getTranslation(), "First segment");
        assertEquals(requests[2].getMTResults()[0].getTranslation(), "Third segment");
    }

    @Test
    public void testSomeFilteredResponsesWithCodes() {
        WSMicrosoftMTAdapter mtAdapter = spy(new WSMicrosoftMTAdapter());
        doReturn(mtConnector).when(mtAdapter).getMicrosoftMTConnector();
        WSMTAdapterConfigurationData config = new WSMTAdapterConfigurationData();
        config.setIncludeCodes(true);
        doReturn(config).when(mtAdapter).getConfiguration();
        when(mtConnector.getParameters()).thenReturn(parameters);
        when(wsLanguage.getLocale()).thenReturn(Locale.ENGLISH);
        when(mtConnector.batchQueryText(anyList())).thenReturn(mockBatchQueryReturns(
                "First segment", "Third segment"
        ));
        WSMTRequest[] requests = composeWSMTRequests("First segment", "Second segment", "Third segment");
        mtAdapter.translate(wsContext, requests, wsLanguage, wsLanguage);
        assertEquals(1, requests[0].getMTResults().length);
        assertEquals(0, requests[1].getMTResults().length);
        assertEquals(1, requests[2].getMTResults().length);
        assertEquals(requests[0].getMTResults()[0].getTranslation(), "First segment");
        assertEquals(requests[2].getMTResults()[0].getTranslation(), "Third segment");
    }

    @Test
    public void testWSResultsAreSetOnWSRequest() {
        WSMicrosoftMTAdapter mtAdapter = spy(new WSMicrosoftMTAdapter());
        doReturn(mtConnector).when(mtAdapter).getMicrosoftMTConnector();
        when(mtConnector.getParameters()).thenReturn(parameters);
        when(wsLanguage.getLocale()).thenReturn(Locale.ENGLISH);
        when(mtConnector.batchQueryText(anyList())).thenReturn(mockBatchQueryReturns(
                "First segment", "Second segment", "Third segment"
        ));

        WSMTRequest[] requests = composeWSMTRequests("First segment", "Second segment", "Third segment");
        mtAdapter.translate(wsContext, requests, wsLanguage, wsLanguage);

        for (WSMTRequest request : requests) {
            assertEquals(request.getMTResults().length, 1);
        }

        assertEquals(requests[0].getMTResults()[0].getTranslation(), "First segment");
        assertEquals(requests[1].getMTResults()[0].getTranslation(), "Second segment");
        assertEquals(requests[2].getMTResults()[0].getTranslation(), "Third segment");
    }

    @Test
    public void testWSResultsAreSetOnWSRequestWithCodes() {
        WSMicrosoftMTAdapter mtAdapter = spy(new WSMicrosoftMTAdapter());
        mtAdapter.getConfiguration().setIncludeCodes(true);
        doReturn(mtConnector).when(mtAdapter).getMicrosoftMTConnector();
        when(mtConnector.getParameters()).thenReturn(parameters);
        when(wsLanguage.getLocale()).thenReturn(Locale.ENGLISH);
        when(mtConnector.batchQueryText(anyList())).thenReturn(mockBatchQueryReturns(
                "First segment", "Second segment", "Third segment"
        ));

        WSMTRequest[] requests = composeWSMTRequests("First segment", "Second segment", "Third segment");
        mtAdapter.translate(wsContext, requests, wsLanguage, wsLanguage);

        for (WSMTRequest request : requests) {
            assertEquals(request.getMTResults().length, 1);
        }

        assertEquals(requests[0].getMTResults()[0].getTranslation(), "First segment");
        assertEquals(requests[1].getMTResults()[0].getTranslation(), "Second segment");
        assertEquals(requests[2].getMTResults()[0].getTranslation(), "Third segment");
    }

    @Test
    public void testLocaleMapping() throws Exception {
        WSMicrosoftMTAdapter mtAdapter = spy(new WSMicrosoftMTAdapter());
        mtAdapter.getConfiguration().setLocaleMapAISPath("/Configuration/locales.txt");
        when(wsContext.getAisManager()).thenReturn(wsAisManager);
        when(wsAisManager.getNode("/Configuration/locales.txt")).thenReturn(localeMapAisNode);
        when(localeMapAisNode.getInputStream()).thenReturn(
            new ByteArrayInputStream("es-CO=es-419".getBytes(StandardCharsets.UTF_8)));
        doReturn(mtConnector).when(mtAdapter).getMicrosoftMTConnector();
        when(mtConnector.getParameters()).thenReturn(parameters);
        when(wsLanguage.getLocale()).thenReturn(new Locale("es", "CO"));
        LocaleId es419 = new LocaleId("es", "419");
        WSMTRequest[] requests = composeWSMTRequests("First segment", "Second segment", "Third segment");
        mtAdapter.translate(wsContext, requests, wsLanguage, wsLanguage);
        // Make sure the locale was set
    }

    private WSMTRequest[] composeWSMTRequests(String ... segments) {
        if (segments == null || segments.length == 0) {
            return new WSMTRequest[]{};
        }

        WSMTRequest[] requests = new WSMTRequest[segments.length];
        for (int i = 0; i < segments.length; i++) {
            requests[i] = new WSMTRequestImpl(segments[i]);
        }

        return requests;
    }

    private List<List<QueryResult>> mockBatchQueryReturns(String ... source) {
        List<List<QueryResult>> queryResultsList = new ArrayList<List<QueryResult>>();
        for (String s : source) {
            List<QueryResult> queryResults = new ArrayList<QueryResult>();
            final QueryResult qr = new QueryResult();
            qr.source = new TextFragment(s);
            qr.target = new TextFragment(s);
            queryResults.add(qr);
            queryResultsList.add(queryResults);
        }

        return queryResultsList;
    }

    private static class WSMTRequestImpl implements WSMTRequest {
        private final String source;
        private WSMTResult[] wsmtResults;

        public WSMTRequestImpl(String source) {
            this.source = source;
        }

        @Override
        public String getSource() {
            return source;
        }

        @Override
        public void setResults(WSMTResult[] wsmtResults) {
            this.wsmtResults = wsmtResults;
        }

        @Override
        public WSMTResult[] getMTResults() {
            return wsmtResults;
        }

        @Override
        public int getScore() {
            return 0;
        }

        @Override
        public boolean isRepetition() {
            return false;
        }
    }
}
