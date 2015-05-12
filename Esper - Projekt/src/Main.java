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


        administrator.createEPL("create variable int okres = 14");


//        administrator.createEPL("create window High.std:unique(spolka) (spolka String, wartosc Float)");
//        administrator.createEPL(
//                "insert into High(spolka, wartosc) " +
//                        "select ka.spolka, max(ka.kursZamkniecia) " +
//                        "from KursAkcji.win:ext_timed(data.getTime(), okres days) as ka "
//        );
//
//        administrator.createEPL("create window Low.std:unique(spolka) (spolka String, wartosc Float)");
//        administrator.createEPL(
//                "insert into Low(spolka, wartosc) " +
//                        "select ka.spolka, min(ka.kursZamkniecia) " +
//                        "from KursAkcji.win:ext_timed(data.getTime(), okres days) as ka "
//        );
//
//        administrator.createEPL("create window TP.win:length(okres) as KursData");
//        administrator.createEPL(
//                "insert into TP(spolka, kurs, data) " +
//                        "select ka.spolka, cast( (ka.kursZamkniecia " +
//                        "                       +(select wartosc from High as h where ka.spolka = h.spolka) " +
//                        "                       +(select wartosc from Low as l where ka.spolka = l.spolka) " +
//                        "                           )/3, float), ka.data " +
//                        "from KursAkcji.win:length(1) as ka"
//        );

        administrator.createEPL("create window TPtab.win:ext_timed(data.getTime(), okres days)  (spolka String, wartosc Float, data Date)");
        administrator.createEPL(
                "insert into TPtab(spolka, wartosc, data) " +
                        "select ka.spolka, cast( (ka.kursZamkniecia + ka.wartoscMax + ka.wartoscMin )/3, float), ka.data " +
                        "from KursAkcji.win:length(1) as ka"
        );

        administrator.createEPL("create window TP.win:ext_timed(data.getTime(), 1 days)  (spolka String, wartosc Float, data Date)");
        administrator.createEPL(
                "insert into TP(spolka, wartosc, data) " +
                        "select spolka, wartosc, data " +
                        "from TPtab " +
                        "limit 1"
        );

        administrator.createEPL("create window SMATP.win:ext_timed(data.getTime(), 1 days)  (spolka String, wartosc Float, data Date)");
        administrator.createEPL(
                "insert into SMATP(spolka, wartosc, data) " +
                        "select spolka, cast(avg(wartosc),float), data " +
                        "from TPtab"
        );

        administrator.createEPL("create window MDpom.std:unique(spolka).win:ext_timed(data.getTime(), 1 days) (spolka String, wartosc Float, data Date)");
        administrator.createEPL(
//                "insert into MDpom(spolka, wartosc, data) " +
//                        "select smatp.spolka, (select tp.kurs from TP as tp where tp.spolka = smatp.spolka), smatp.data " +
//                        "from SMATP as smatp"
//
                "on SMATP as smatp " +
                        "merge TPtab as tp " +
                        "where smatp.spolka = tp.spolka " +
                        "when matched then " +
                        "   insert into MDpom(spolka, wartosc, data) " +
                        "       select smatp.spolka, Math.abs(smatp.wartosc - tp.wartosc), smatp.data"

        );

        administrator.createEPL("create window MD.win:ext_timed(data.getTime(), 1 days) (spolka String, wartosc Float, data Date)");
        administrator.createEPL(
                "insert into MD(spolka, wartosc, data) " +
                        "select spolka, cast(avg(wartosc), float), data " +
                        "from MDpom"
        );

        administrator.createEPL("create window CCI.win:ext_timed(data.getTime(), 1 days) (spolka String, wartosc Float, data Date)");
        administrator.createEPL(
                "insert into CCI(spolka, wartosc, data) " +
                        "select md.spolka, cast((tp.wartosc - smatp.wartosc)/(0.015 * md.wartosc), float), md.data " +
                        "from MD as md unidirectional " +
                        "join TP as tp on tp.spolka = md.spolka " +
                        "join SMATP as smatp on smatp.spolka = md.spolka"
        );

        EPStatement statement = administrator.createEPL(
                "select istream *" +
                        "from MD"
        );

        // STARE
//        administrator.createEPL("create schema Licznik (kursZamkniecia Integer, liczba Integer, blad Integer)");
//        administrator.createEPL("create variable int wielkoscOkna = 50");
//        administrator.createEPL("create window Top50.ext:sort(wielkoscOkna, liczba desc, blad asc) as Licznik");
//        administrator.createEPL("create variable int rok = 1");
//        administrator.createEPL("create variable Integer liczbaX = 0");
//
//        administrator.createEPL(
//                "on KursAkcji as ka " +
//                        "merge Top50 as top " +
//                        "where cast(ka.kursZamkniecia, int) = top.kursZamkniecia " +
//                        "when matched then " +
//                        "   update set top.liczba = top.liczba + 1 " +
//                        "when not matched then " +
//                        "   insert into Top50(kursZamkniecia, liczba, blad) " +
//                        "       select cast(ka.kursZamkniecia, int), liczbaX+1, liczbaX "
//        );
//
//        administrator.createEPL(
//                "on Top50((select count(*) from Top50) = wielkoscOkna) " +
//                        "set liczbaX = (select min(liczba) from Top50) "
//        );
//
//
//
//        administrator.createEPL(
//                "on KursAkcji as ka " +
//                        "set rok = ka.data.getYear()"
//        );
//
//        EPStatement statement = administrator.createEPL(
//                "on KursAkcji(data.getYear() > rok) " +
//                        "select rok+1 as rok, top.kursZamkniecia as co, top.liczba as ile, top.blad as blad" +
//                        "   from Top50 as top" +
//                        "   limit 1"
//        );
        //STARE


        ProstyListener listener = new ProstyListener();
        statement.addListener(listener);

        InputStream inputStream = new InputStream();
        inputStream.generuj(serviceProvider);

    }

}
