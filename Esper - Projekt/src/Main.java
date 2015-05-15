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
        administrator.createEPL("create variable int kasaNaStart = 1000");


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

        administrator.createEPL("create window TPtab.std:groupwin(spolka).win:length(okres)  (spolka String, wartosc Float, data Date)");
        administrator.createEPL(
                "insert into TPtab(spolka, wartosc, data) " +
                        "select ka.spolka, cast( (ka.kursZamkniecia + ka.wartoscMax + ka.wartoscMin )/3, float), ka.data " +
                        "from KursAkcji.win:length(1) as ka"
        );

        administrator.createEPL("create window TP.std:unique(spolka) (spolka String, wartosc Float, data Date)");
        administrator.createEPL(
                "insert into TP(spolka, wartosc, data) " +
                        "select spolka, wartosc, data " +
                        "from TPtab " +
                        "limit 1"
        );

        administrator.createEPL("create window SMATP.std:unique(spolka)  (spolka String, wartosc Float, data Date)");
        administrator.createEPL(
                "insert into SMATP(spolka, wartosc, data) " +
                        "select spolka, cast(avg(wartosc),float), data " +
                        "from TPtab"
        );

//        administrator.createEPL("create window MDpom.std:unique(spolka).win:ext_timed(data.getTime(), 1 days) (spolka String, wartosc Float, data Date)");
//        administrator.createEPL(
//                "on SMATP as smatp " +
//                        "merge TPtab as tp " +
//                        "where smatp.spolka = tp.spolka " +
//                        "when matched then " +
//                        "   insert into MDpom(spolka, wartosc, data) " +
//                        "       select smatp.spolka, Math.abs(smatp.wartosc - tp.wartosc), smatp.data"
//
//        );

        administrator.createEPL("create window MD.std:unique(spolka) (spolka String, wartosc Float, data Date)");
        administrator.createEPL(
        "insert into MD (spolka, wartosc, data) " +
                "select smatp.spolka, cast( avg(Math.abs(smatp.wartosc - tptab.wartosc)), float), smatp.data " +
                "from SMATP as smatp unidirectional " +
                "join TPtab as tptab on smatp.spolka = tptab.spolka " +
                "group by smatp.spolka, smatp.data"



//                "insert into MD(spolka, wartosc, data) " +
//                        "select spolka, cast(avg(wartosc), float), data " +
//                        "from MDpom"
        );

        //TODO może dodatkowo długościowe lub czasowe na CCI
        administrator.createEPL("create window CCI.std:groupwin(spolka).win:keepall() (spolka String, wartosc Float, data Date)");
        administrator.createEPL(
                "insert into CCI(spolka, wartosc, data) " +
                        "select md.spolka, cast((tp.wartosc - smatp.wartosc)/(0.015 * md.wartosc), float), md.data " +
                        "from MD as md unidirectional " +
                        "join TP as tp on tp.spolka = md.spolka " +
                        "join SMATP as smatp on smatp.spolka = md.spolka"
        );

        administrator.createEPL("create window lastThree.std:groupwin(spolka).win:length(3) (spolka String, wartosc Float, data Date)");
        administrator.createEPL(
                "insert into lastThree(spolka, wartosc, data) " +
                        "select ka.spolka, ka.kursZamkniecia, ka.data " +
                        "from KursAkcji.win:length(1) as ka "
        );

        administrator.createEPL("create window localMax.std:groupwin(spolka).win:length(2) (spolka String, wartosc Float, data Date)");
        administrator.createEPL(
                "insert into localMax(spolka, wartosc, data) " +
                        "select  b.spolka, b.wartosc, b.data " +
                        "from lastThree as a " +
                        "join lastThree as b on a.spolka = b.spolka " +
                        "join lastThree as c on a.spolka = c.spolka " +
                        "where a.data.before(b.data) " +
                        "and b.data.before(c.data) " +
                        "and a.wartosc < b.wartosc " +
                        "and c.wartosc <= b.wartosc"
        );

        administrator.createEPL("create window localMin.std:groupwin(spolka).win:length(2) (spolka String, wartosc Float, data Date)");
        administrator.createEPL(
                "insert into localMin(spolka, wartosc, data) " +
                        "select  b.spolka, b.wartosc, b.data " +
                        "from lastThree as a " +
                        "join lastThree as b on a.spolka = b.spolka " +
                        "join lastThree as c on a.spolka = c.spolka " +
                        "where a.data.before(b.data) " +
                        "and b.data.before(c.data) " +
                        "and a.wartosc >= b.wartosc " +
                        "and c.wartosc > b.wartosc"
        );

        //TODO jak wywalimy distinct to się generuje milion wpisów
        administrator.createEPL("create window kup.win:length(1) (spolka String, data Date, operacja char)");
        administrator.createEPL(
                "insert into kup(spolka, data, operacja) " +
                        "select distinct  maxB.spolka, maxB.data, cast('-',char) " +
                        "from localMax as maxA unidirectional " +
                        "join localMax as maxB on maxA.spolka = maxB.spolka " +
                        "join CCI as cciA on cciA.spolka = maxA.spolka and cciA.data = maxA.data " +
                        "join CCI as cciB on cciB.spolka = maxB.spolka and cciB.data = maxB.data " +
                        "where maxA.wartosc < maxB.wartosc " +
                        "and cciA.wartosc > cciB.wartosc "
        );
        administrator.createEPL(
                "insert into kup(spolka, data, operacja) " +
                        "select distinct  minB.spolka, minB.data, cast('+',char) " +
                        "from localMin as minA unidirectional " +
                        "join localMin as minB on minA.spolka = minB.spolka " +
                        "join CCI as cciA on cciA.spolka = minA.spolka and cciA.data = minA.data " +
                        "join CCI as cciB on cciB.spolka = minB.spolka and cciB.data = minB.data " +
                        "where minA.wartosc < minB.wartosc " +
                        "and cciA.wartosc > cciB.wartosc "
        );

        administrator.createEPL("create window kupKurs.win:length(1) (spolka String, data Date, operacja char, kurs Float)");
        administrator.createEPL(
                "insert into kupKurs(spolka, data, operacja, kurs) " +
                        "select k.spolka, k.data, k.operacja, ka.kursZamkniecia " +
                        "from kup as k " +
                        "join KursAkcji.win:keepall() as ka on k.spolka = ka.spolka and k.data = ka.data"
        );

        administrator.createEPL("create window portfel.std:unique(spolka) (spolka String, data Date, liczba_akcji Integer, suma_gotowki float)");
        administrator.createEPL(
                "on kupKurs(operacja = cast('+',char)) as k " +
                        "merge portfel as p " +
                        "where k.spolka = p.spolka " +
                        "when matched then " +
                        "   update set p.liczba_akcji = 0 " +
                        "when not matched then " +
                        "   insert into portfel(spolka, data, liczba_akcji, suma_gotowki) " +
                        "       select k.spolka, k.data, cast(kasaNaStart/k.kurs, int), cast(0, float)"
        );



        EPStatement statement = administrator.createEPL(
                "select irstream *" +
                        "from kup"
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
