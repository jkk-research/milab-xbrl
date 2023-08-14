package hu.sze.uni.http;

import javax.servlet.http.HttpServlet;

public abstract class DustHttpServerBase implements DustHttpConsts {

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

	public abstract void addServlet(String path, HttpServlet servlet);

}
