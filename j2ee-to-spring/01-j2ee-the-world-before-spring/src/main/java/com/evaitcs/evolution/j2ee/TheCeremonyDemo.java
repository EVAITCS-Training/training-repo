package com.evaitcs.evolution.j2ee;

/**
 * Demo 1 — THE MUSEUM: what J2EE actually asked you to write. (README section
 * "The problem: peak ceremony".)
 *
 * Nothing here runs — that is the point. These are faithful-shaped exhibits of an
 * EJB 2.x "transfer money" component: TWO interfaces, an implementation, and a
 * deployment descriptor, all to execute one UPDATE. Read them aloud in class and
 * count the lines that are about money: it is one comparison and two updates.
 *
 * The exhibits are text blocks (Java 21) so the whole museum fits in one screen-
 * scrollable file. In 2001 each block was its own .java/.xml file, and the vendor
 * server generated a THIRD class (the container's EJBObject stub) at deploy time.
 */
public class TheCeremonyDemo {

    // Exhibit A: the "home" interface — a factory contract the container implements FOR you.
    static final String HOME_INTERFACE = """
            public interface TransferHome extends EJBHome {
                Transfer create() throws CreateException, RemoteException;
                Transfer findByPrimaryKey(String id) throws FinderException, RemoteException;
            }
            """;

    // Exhibit B: the "remote" interface — every business method, declared to throw
    // RemoteException whether it can fail remotely or not. That is the contract tax.
    static final String REMOTE_INTERFACE = """
            public interface Transfer extends EJBObject {
                void moveMoney(long from, long to, BigDecimal amount)
                        throws RemoteException, InsufficientFundsException;
            }
            """;

    // Exhibit C: the bean — the ONLY file containing business logic. Note it implements
    // the container's lifecycle callbacks and may NOT be used directly: clients always
    // go through generated stubs (RemoteException on every call is the stub's tax).
    static final String BEAN_CLASS = """
            public class TransferBean implements SessionBean {
                public void ejbCreate() {}
                public void ejbActivate() {}
                public void ejbPassivate() {}
                public void ejbRemove() {}
                public void setSessionContext(SessionContext ctx) {}

                public void moveMoney(long from, long to, BigDecimal amount)
                        throws InsufficientFundsException {
                    // ...finally, the money: the same 4 lines JdbcTransferDao has...
                }
            }
            """;

    // Exhibit D: the deployment descriptor — XML the container reads to learn what your
    // classes mean. Stage-2's application.yml is a distant, far smaller descendant.
    static final String DEPLOYMENT_DESCRIPTOR = """
            <session>
              <ejb-name>Transfer</ejb-name>
              <home>com.securebank.ejb.TransferHome</home>
              <remote>com.securebank.ejb.Transfer</remote>
              <ejb-class>com.securebank.ejb.TransferBean</ejb-class>
              <session-type>Stateless</session-type>
              <transaction-type>Container</transaction-type>
            </session>
            """;

    // Exhibit E: the client lookup — JNDI ceremony to obtain the home, then create().
    // ServiceLocator.java in this module is the honest, runnable descendant of this.
    static final String JNDI_LOOKUP = """
            Context ctx = new InitialContext();                       // may throw NamingException
            Object ref = ctx.lookup("java:comp/env/ejb/Transfer");    // a STRING. typos = runtime.
            TransferHome home = (TransferHome)
                    PortableRemoteObject.narrow(ref, TransferHome.class);
            Transfer transfer = home.create();                        // remote call #1
            transfer.moveMoney(1, 3, new BigDecimal("100.00"));       // remote call #2
            """;

    public static void main(String[] args) {
        banner("1. One transfer, five artifacts (EJB 2.x, c. 2001)");
        exhibit("A — TransferHome.java (factory interface, implemented by the CONTAINER)", HOME_INTERFACE);
        exhibit("B — Transfer.java (remote interface; RemoteException tax on every method)", REMOTE_INTERFACE);
        exhibit("C — TransferBean.java (the only file with business logic in it)", BEAN_CLASS);
        exhibit("D — ejb-jar.xml (deployment descriptor: XML describing your Java)", DEPLOYMENT_DESCRIPTOR);
        exhibit("E — the client (JNDI lookup: stringly-typed, cast-heavy, remote by default)", JNDI_LOOKUP);

        banner("2. Count the money");
        System.out.println("step 1: lines across the five exhibits        ~ 45");
        System.out.println("step 2: lines that actually move money        ~  4  (check funds, debit, credit, ledger)");
        System.out.println("step 3: everything else is CEREMONY the container demanded");
        System.out.println("verdict: the ratio — not any single file — is why 2002 revolted.");

        banner("3. The revolt, in one paragraph");
        System.out.println("Rod Johnson (Expert One-on-One J2EE Design and Development, 2002): most apps do not");
        System.out.println("need a distributed component container. They need (a) objects wired together,");
        System.out.println("(b) transactions, (c) SQL without the boilerplate. All three are plain-Java problems.");
        System.out.println("His 'interface21' library solved them with an IoC container + a JDBC helper —");
        System.out.println("that library became the Spring Framework. Modules 02-03 build both, from scratch.");

        banner("4. And the servlet track (this module's live code)");
        System.out.println("Teams that skipped EJB still hand-wired everything the servlet API lacks:");
        System.out.println("  dependency wiring -> ServiceLocator.java (runnable anti-pattern, this module)");
        System.out.println("  transactions      -> JdbcTransferDao.java (commit/rollback/close by hand)");
        System.out.println("  HTTP handling     -> TransferServlet.java (parse, validate, status codes inline)");
        System.out.println("LiveServletDemo starts exactly that stack on real HTTP — then module 03 deletes");
        System.out.println("the ServiceLocator, module 04 deletes the rest.");
    }

    static void exhibit(String title, String code) {
        System.out.println();
        System.out.println("--- " + title + " ---");
        System.out.println(code.stripIndent().strip());
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
