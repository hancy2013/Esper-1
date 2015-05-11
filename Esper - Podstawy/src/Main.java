import com.espertech.esper.client.EPAdministrator;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.EPStatement;

import java.io.IOException;


public class Main {

    /**
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {

        EPServiceProvider serviceProvider = EPServiceProviderManager.getDefaultProvider();

        EPAdministrator administrator = serviceProvider.getEPAdministrator();


//        // dlaczego miesza kolejność atrybutów przy wyświetlaniu?
//        EPStatement statement = administrator.createEPL(
//                "select irstream data, spolka, kursOtwarcia " +
//                        "from KursAkcji.win:length(3) " +
//                        "WHERE spolka='Oracle' ") ;

        // zad 31, nie działa
        EPStatement statement = administrator.createEPL(
                "select irstream data, spolka, avg(obrot) " +
                        "from KursAkcji.std:groupwin(spolka).win:length(5) " +
                        "HAVING avg(obrot) < 10000000" ) ;

        ProstyListener listener = new ProstyListener();
        statement.addListener(listener);

        InputStream inputStream = new InputStream();
        inputStream.generuj(serviceProvider);

    }

}
