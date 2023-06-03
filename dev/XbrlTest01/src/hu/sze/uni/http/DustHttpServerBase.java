package hu.sze.uni.http;

import java.io.IOException;
import java.util.Enumeration;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public abstract class DustHttpServerBase implements DustHttpConsts {

//    static final DustUtilsFactory<DustEntity, Set<DustEntity>> typeAtts = new DustUtilsFactory<DustEntity, Set<DustEntity>>(false) {
//        @Override
//        protected Set<DustEntity> create(DustEntity key, Object... hints) {
//            return new HashSet<>();
//        }
//
//        @Override
//        protected void initNew(Set<DustEntity> item, DustEntity key, Object... hints) {
//            DustUtils.accessEntity(DataCommand.processRef, key, DustMetaLinks.TypeAttDefs, new RefProcessor() {
//                @Override
//                public void processRef(DustRef ref) {
//                    item.add(ref.get(RefKey.target));
//                }
//            });
//
//            DustUtils.accessEntity(DataCommand.processRef, key, DustGenericLinks.ConnectedRequires, new RefProcessor() {
//                @Override
//                public void processRef(DustRef ref) {
//                    DustEntity t = ref.get(RefKey.target);
//                    item.addAll(typeAtts.get(t));
//                }
//            });
//        }
//    };

    class ProcessorWrapperServlet extends HttpServlet {
        private static final long serialVersionUID = 1L;

//        DustEntity eSelf;

        String charset;
        String contentType;
        
//        Map<String, DustEntity> attsToRead = new HashMap<>();

//        public ProcessorWrapperServlet(DustEntity eSelf) {
//            this.eSelf = eSelf;
//
//            charset = DustUtils.getSafe(eSelf, DustNetAtts.NetProcessorRespCharset, CHARSET_UTF8);
//            contentType = DustUtils.getSafe(eSelf, DustNetAtts.NetProcessorRespCharset, CONTENT_JSON);
//            
//            DustUtils.accessEntity(DataCommand.processRef, eSelf, DustNetLinks.NetProcessorParamTypes, new RefProcessor() {
//                @Override
//                public void processRef(DustRef ref) {
//                    DustEntity t = ref.get(RefKey.target);
//                    for ( DustEntity a : typeAtts.get(t)) {
//                        if (!attsToRead.containsValue(a) ) {
//                            String id = DustUtils.accessEntity(DataCommand.getValue, a, DustGenericAtts.IdentifiedIdLocal);
//                            attsToRead.put(id, a);
//                        }
//                    }
//                   
//                }
//            });
//        }

        @Override
        protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
//            DustEntity eProc = DustUtils.accessEntity(DataCommand.getEntity, DustDataTypes.Message);
        	Object eProc = null;

            Enumeration<String> ee;
            String n = null;

            for (ee = request.getAttributeNames(); ee.hasMoreElements(); ) {
                n = ee.nextElement();
                optAdd(eProc, n, request.getAttribute(n));
            }

            for (ee = request.getParameterNames(); ee.hasMoreElements(); ) {
                n = ee.nextElement();
                optAdd(eProc, n, request.getParameter(n));
            }

            for (ee = request.getHeaderNames(); ee.hasMoreElements(); ) {
                n = ee.nextElement();
                optAdd(eProc, n, request.getHeader(n));
            }

            response.setCharacterEncoding(charset);
            response.setContentType(contentType);

//            DustUtils.accessEntity(DataCommand.setRef, eProc, DustDataLinks.MessageCommand, DustProcMessages.ProcessorProcess);
//            DustUtils.accessEntity(DataCommand.setValue, eProc, DustNetAtts.NetProcessMethod, request.getMethod());
//            DustUtils.accessEntity(DataCommand.setValue, eProc, DustGenericAtts.StreamWriter, response.getWriter());
//
//            DustUtils.accessEntity(DataCommand.tempSend, eSelf, eProc);
            response.setStatus(HttpServletResponse.SC_OK);
        }
        
        private void optAdd(Object msg, String name, Object val) {
//            DustEntity att = attsToRead.get(name);
//            
//            if ( null != att ) {
//                DustUtils.accessEntity(DataCommand.setValue, msg, att, val);
//            }
        }
    }

    public void activeInit() throws Exception {
        initConnectors();
        initHandlers();
    }

    public void activeRelease() throws Exception {
    }

    public void initConnectors() throws Exception {
//        int pPub = DustUtils.getInt(ContextRef.self, DustNetAtts.NetServerPublicPort, NO_PORT_SET);
//        int pSsl = DustUtils.getInt(ContextRef.self, DustNetAtts.NetServerSslPort, NO_PORT_SET);
//
//        if (NO_PORT_SET != pPub) {
//            initConnectorPublic(pPub, pSsl);
//        }
//
//        if (NO_PORT_SET != pSsl) {
//            initConnectorSsl(pSsl);
//        }
    }

    protected void initHandlers() {
//        DustUtils.accessEntity(DataCommand.processRef, ContextRef.self, DustProcLinks.DispatcherTargets, new RefProcessor() {
//            @Override
//            public void processRef(DustRef ref) {
//                DustEntity sc = ref.get(RefKey.target);
//
//                String ctx = DustUtils.accessEntity(DataCommand.getValue, sc, DustGenericAtts.IdentifiedIdLocal);
//                addServlet("/" + ctx, new ProcessorWrapperServlet(sc));
//            }
//        });
    }

    protected abstract void initConnectorPublic(int portPublic, int portSsl);

    protected void initConnectorSsl(int portSsl) {
    }

    protected abstract void addServlet(String path, HttpServlet servlet);

}
