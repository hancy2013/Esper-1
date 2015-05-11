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


        administrator.createEPL("create schema Licznik (kursZamkniecia Integer, liczba Integer, blad Integer)");
        administrator.createEPL("create variable int wielkoscOkna = 50");
        administrator.createEPL("create window Top50.ext:sort(wielkoscOkna, liczba desc, blad asc) as Licznik");
        administrator.createEPL("create variable int rok = 1");
        administrator.createEPL("create variable Integer liczbaX = 0");

        administrator.createEPL(
                "on KursAkcji as ka " +
                        "merge Top50 as top " +
                        "where cast(ka.kursZamkniecia, int) = top.kursZamkniecia " +
                        "when matched then " +
                        "   update set top.liczba = top.liczba + 1 " +
                        "when not matched then " +
                        "   insert into Top50(kursZamkniecia, liczba, blad) " +
                        "       select cast(ka.kursZamkniecia, int), liczbaX+1, liczbaX "
        );

        administrator.createEPL(
                "on Top50((select count(*) from Top50) = wielkoscOkna) " +
                        "set liczbaX = (select min(liczba) from Top50) "
        );



        administrator.createEPL(
                "on KursAkcji as ka " +
                        "set rok = ka.data.getYear()"
        );

        EPStatement statement = administrator.createEPL(
                "on KursAkcji(data.getYear() > rok) " +
                        "select rok+1 as rok, top.kursZamkniecia as co, top.liczba as ile, top.blad as blad" +
                        "   from Top50 as top" +
                        "   limit 1"
        );

        ProstyListener listener = new ProstyListener();
        statement.addListener(listener);

        InputStream inputStream = new InputStream();
        inputStream.generuj(serviceProvider);

    }

}
