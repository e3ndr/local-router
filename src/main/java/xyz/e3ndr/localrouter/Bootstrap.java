package xyz.e3ndr.localrouter;

import xyz.e3ndr.localrouter.db.Providers;
import xyz.e3ndr.localrouter.routes.ApiServer;
import xyz.e3ndr.localrouter.routes.ui.UiServer;

public class Bootstrap {

    public static void main(String[] args) throws Exception {
        LR.init();
        Providers.init();
        ApiServer.start();
        UiServer.start();
    }

}
