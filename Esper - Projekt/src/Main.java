import com.espertech.esper.client.EPAdministrator;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.EPStatement;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;

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

        administrator.createEPL("create window MD.std:unique(spolka) (spolka String, wartosc Float, data Date)");
        administrator.createEPL(
                "insert into MD (spolka, wartosc, data) " +
                        "select smatp.spolka, cast( avg(Math.abs(smatp.wartosc - tptab.wartosc)), float), smatp.data " +
                        "from SMATP as smatp unidirectional " +
                        "join TPtab as tptab on smatp.spolka = tptab.spolka " +
                        "group by smatp.spolka, smatp.data"
        );

        //TODO może dodatkowo długościowe lub czasowe na CCI
        administrator.createEPL("create window CCI.std:groupwin(spolka).win:length(30) (spolka String, wartosc Float, data Date)");
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
                        "select distinct  maxB.spolka, maxA.data, cast('-',char) " +
                        "from localMax as maxA unidirectional " +
                        "join localMax as maxB on maxA.spolka = maxB.spolka " +
                        "join CCI as cciA on cciA.spolka = maxA.spolka and cciA.data = maxA.data " +
                        "join CCI as cciB on cciB.spolka = maxB.spolka and cciB.data = maxB.data " +
                        "where maxA.wartosc > maxB.wartosc " +
                        "and cciA.wartosc < cciB.wartosc " +
                        "and maxB.data.before(maxA.data) "
        );
        administrator.createEPL(
                "insert into kup(spolka, data, operacja) " +
                        "select distinct  minB.spolka, minA.data, cast('+',char) " +
                        "from localMin as minA unidirectional " +
                        "join localMin as minB on minA.spolka = minB.spolka " +
                        "join CCI as cciA on cciA.spolka = minA.spolka and cciA.data = minA.data " +
                        "join CCI as cciB on cciB.spolka = minB.spolka and cciB.data = minB.data " +
                        "where minA.wartosc < minB.wartosc " +
                        "and cciA.wartosc > cciB.wartosc " +
                        "and minB.data.before(minA.data) "
        );

        administrator.createEPL("create window kupKurs.win:length(1) (spolka String, data Date, operacja char, kurs Float)");
        administrator.createEPL(
                "insert into kupKurs(spolka, data, operacja, kurs) " +
                        "select k.spolka, ka.data, k.operacja, ka.kursZamkniecia " +
                        "from kup as k " +
                        "join KursAkcji.std:unique(spolka) as ka on k.spolka = ka.spolka"
        );

        administrator.createEPL("create window portfel.std:unique(spolka) (spolka String, data Date, liczba_akcji Integer, suma_gotowki float)");
        administrator.createEPL(
                "on kupKurs(operacja = cast('+',char)) as k " +
                        "merge portfel as p " +
                        "where k.spolka = p.spolka " +
                        "when matched then " +
                        "   update set p.liczba_akcji = p.liczba_akcji + cast(p.suma_gotowki/k.kurs, int)," +
                        "   p.suma_gotowki = p.suma_gotowki - cast((cast(p.suma_gotowki/k.kurs, int))*k.kurs, float), " +
                        "   p.data = k.data " +
                        "when not matched then " +
                        "   insert into portfel(spolka, data, liczba_akcji, suma_gotowki) " +
                        "       select k.spolka, k.data, cast(kasaNaStart/k.kurs, int), cast(kasaNaStart % k.kurs, float)"
        );

        administrator.createEPL(
                "on kupKurs(operacja = cast('-',char)) as k " +
                        "merge portfel as p " +
                        "where k.spolka = p.spolka " +
                        "when matched then " +
                        "   update set " +
                        "   p.suma_gotowki = p.suma_gotowki + cast(p.liczba_akcji*k.kurs, float)," +
                        "   p.liczba_akcji = 0," +
                        "   p.data = k.data  "
        );


//        EPStatement statement = administrator.createEPL(
//                "select irstream * " +
//                        "from kup"
//        );

//        EPStatement statement = administrator.createEPL(
//                "select k.spolka, k.data, k.suma_gotowki, k.liczba_akcji " +
//                        "from portfel as k"
//        );

        EPStatement statement = administrator.createEPL(
                "select ka.data, ka.spolka, ka.kursZamkniecia, k.suma_gotowki, k.liczba_akcji, (ka.kursZamkniecia*k.liczba_akcji+k.suma_gotowki) as SUMA " +
                        "from KursAkcji(Main.sprawdz(data, 2010, 11, 31)) as ka unidirectional " +
                        "join portfel as k on k.spolka = ka.spolka and k.data = ka.data"

//                    "select * from kup()"
        );


        ProstyListener listener = new ProstyListener();
        statement.addListener(listener);


        InputStream inputStream = new InputStream();
        inputStream.generuj(serviceProvider);

    }

    public static boolean sprawdz(Date date, int y, int m, int d) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        return cal.get(Calendar.DAY_OF_MONTH)==d && cal.get(Calendar.YEAR)==y && cal.get(Calendar.MONTH)==m;
    }
}
