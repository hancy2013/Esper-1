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

        administrator.createEPL("create window CCI.std:groupwin(spolka).win:length(3) (spolka String, wartosc Float, data Date)");
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

        administrator.createEPL("create window kursMax.std:groupwin(spolka).win:length(2) (spolka String, wartosc Float, data Date)");
        administrator.createEPL(
                "insert into kursMax(spolka, wartosc, data) " +
                        "select  b.spolka, b.wartosc, b.data " +
                        "from lastThree as a " +
                        "join lastThree as b on a.spolka = b.spolka " +
                        "join lastThree as c on a.spolka = c.spolka " +
                        "where a.data.before(b.data) " +
                        "and b.data.before(c.data) " +
                        "and a.wartosc < b.wartosc " +
                        "and c.wartosc <= b.wartosc"
        );

        administrator.createEPL("create window kursMin.std:groupwin(spolka).win:length(2) (spolka String, wartosc Float, data Date)");
        administrator.createEPL(
                "insert into kursMin(spolka, wartosc, data) " +
                        "select  b.spolka, b.wartosc, b.data " +
                        "from lastThree as a " +
                        "join lastThree as b on a.spolka = b.spolka " +
                        "join lastThree as c on a.spolka = c.spolka " +
                        "where a.data.before(b.data) " +
                        "and b.data.before(c.data) " +
                        "and a.wartosc > b.wartosc " +
                        "and c.wartosc >= b.wartosc"
        );

        administrator.createEPL("create window CCIMax.std:groupwin(spolka).win:length(2) (spolka String, wartosc Float, data Date)");
        administrator.createEPL(
                "insert into CCIMax(spolka, wartosc, data) " +
                        "select  b.spolka, b.wartosc, b.data " +
                        "from CCI as a " +
                        "join CCI as b on a.spolka = b.spolka " +
                        "join CCI as c on a.spolka = c.spolka " +
                        "where a.data.before(b.data) " +
                        "and b.data.before(c.data) " +
                        "and a.wartosc < b.wartosc " +
                        "and c.wartosc <= b.wartosc"
        );

        administrator.createEPL("create window CCIMin.std:groupwin(spolka).win:length(2) (spolka String, wartosc Float, data Date)");
        administrator.createEPL(
                "insert into CCIMin(spolka, wartosc, data) " +
                        "select  b.spolka, b.wartosc, b.data " +
                        "from CCI as a " +
                        "join CCI as b on a.spolka = b.spolka " +
                        "join CCI as c on a.spolka = c.spolka " +
                        "where a.data.before(b.data) " +
                        "and b.data.before(c.data) " +
                        "and a.wartosc > b.wartosc " +
                        "and c.wartosc >= b.wartosc"
        );

        administrator.createEPL("create window kup.win:length(1) (spolka String, operacja char)");
        administrator.createEPL(
                "insert into kup(spolka, operacja) " +
                        "select distinct  cciA.spolka, cast('-',char) " +
                        "from CCIMax as cciB unidirectional " +
                        "join CCIMax as cciA on cciB.spolka = cciA.spolka " +
                        "join kursMax as kursA on kursA.spolka = cciA.spolka " +
                        "join kursMax as kursB on kursB.spolka = cciA.spolka " +
                        "where cciA.data.before(cciB.data) " +
                        "and kursA.data.before(kursB.data) " +
                        "and cciA.wartosc > cciB.wartosc " +
                        "and kursA.wartosc < kursB.wartosc"
        );
        administrator.createEPL(
                "insert into kup(spolka, operacja) " +
                        "select distinct  cciA.spolka, cast('+',char) " +
                        "from CCIMin as cciB unidirectional " +
                        "join CCIMin as cciA on cciB.spolka = cciA.spolka " +
                        "join kursMin as kursA on kursA.spolka = cciA.spolka " +
                        "join kursMin as kursB on kursB.spolka = cciA.spolka " +
                        "where cciA.data.before(cciB.data) " +
                        "and kursA.data.before(kursB.data) " +
                        "and cciA.wartosc < cciB.wartosc " +
                        "and kursA.wartosc > kursB.wartosc"
        );


//        administrator.createEPL("create window kup2.win:length(1) (spolka String, operacja char)");
//        administrator.createEPL(
//                "insert into kup2(spolka, operacja) "
//        );




       /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        // Wyznaczenie wskaznika MFI(14)

        administrator.createEPL(""
                // okno "for14days" - oznaczone na schemacie jako "O_01"
                + "insert into for14days "
                + "select data as data"
                + "     , spolka as spolka"
                + "     , case when (wartoscMax + wartoscMin + kursZamkniecia) >= (prev(1, wartoscMax) + prev(1, wartoscMin) + prev(1, kursZamkniecia))"
                + "            then obrot * (wartoscMax + wartoscMin + kursZamkniecia) / 3 else 0d end as obrotForsyPlus"
                + "     , case when (wartoscMax + wartoscMin + kursZamkniecia) < (prev(1, wartoscMax) + prev(1, wartoscMin) + prev(1, kursZamkniecia))"
                + "            then obrot * (wartoscMax + wartoscMin + kursZamkniecia) / 3 else 0d end as obrotForsyMinus"
                + "     , kursZamkniecia as kursZamkniecia"
                + "  from KursAkcji.std:groupwin(spolka).win:length(2)");

        administrator.createEPL(""
                // okno "for3days" - oznaczone na schemacie jako "O_02"
                + "insert into for3days "
                + "select data as data"
                + "     , spolka as spolka"
                + "     , 100d - ( 100d / ( 1d "
                + "                 + ((sum(obrotForsyPlus))  "
                + "                   /  (sum(obrotForsyMinus)) )"
                + "                   )) as MFI_14"
                + "       , kursZamkniecia as kursZamkniecia  "
                + "  from for14days.std:groupwin(spolka).win:length(14) group by spolka");
        // Wyznaczenie dywergencji wskaznika i kursu - sygnałow dla gry gieldowej

        administrator.createEPL(""
                // okno "extremesBuffer" - oznaczone na schemacie jako "O_03"
                + "insert into extremesBuffer "
                + "select f3d.data as data"
                + "     , f3d.spolka as spolka"
                + "     , prev(1, f3d.MFI_14) as MFI_14_1"
                + "     , prev(1, f3d.kursZamkniecia) as kursZamkniecia_1"
                + "     , case when prev(2, f3d.kursZamkniecia) > prev(1, f3d.kursZamkniecia) "
                + "             and prev(1, f3d.kursZamkniecia) < f3d.kursZamkniecia "
                + "            then 'min' "
                + "            when prev(2, f3d.kursZamkniecia) < prev(1, f3d.kursZamkniecia) "
                + "             and prev(1, f3d.kursZamkniecia) > f3d.kursZamkniecia "
                + "            then 'max' "
                + "            else 'brak' end as wspDyw "
                + "     , case when prev(2, f3d.MFI_14) > prev(1, f3d.MFI_14) "
                + "             and prev(1, f3d.MFI_14) < f3d.MFI_14 "
                + "            then 'min' "
                + "            when prev(2, f3d.MFI_14) < prev(1, f3d.MFI_14) "
                + "             and prev(1, f3d.MFI_14) > f3d.MFI_14 "
                + "            then 'max' "
                + "            else 'brak' end as wspMFI"
                + "  from for3days.std:groupwin(spolka).win:length(3) f3d");

        administrator.createEPL(""
                + "on extremesBuffer(wspDyw!='brak' OR wspMFI!='brak') as exBuf"
                // okno "extremeMFI" - oznaczone na schemacie jako "O_04"
                + " insert into extremeMFI "
                + " select exBuf.data as data"
                + "      , exBuf.spolka as spolka"
                + "      , exBuf.MFI_14_1 as value"
                + "      , exBuf.wspDyw as type"
                + "  where exBuf.wspDyw !='brak'"
                // okno "extremePrice" - oznaczone na schemacie jako "O_05"
                + " insert into extremePrice "
                + " select exBuf.data as data, exBuf.spolka as spolka"
                + "      , cast(exBuf.kursZamkniecia_1,Double) as value"
                + "      , exBuf.wspDyw as type"
                + "  where exBuf.wspDyw !='brak'"
                + " output all");

        administrator.createEPL(""
                // okno "trendsMFI" - oznaczone na schemacie jako "O_06"
                + "insert into trendsMFI "
                + "select spolka as spolka"
                + "     , data as data"
                + "     , case when type='max' then 'sign1' else 'sign2' end as dywTrend "
                + "  from extremeMFI.std:groupwin(spolka,type).win:length(2) "
                + " where (value > prev(1,value) and type='min') "
                + "    or (value < prev(1,value) and type='max') ");

        administrator.createEPL(""
                // okno "trendsPrice" - oznaczone na schemacie jako "O_07"
                + "insert into trendsPrice "
                + "select spolka as spolka"
                + "     , data as data"
                + "     , case when type='max' then 'sign1' else 'sign2' end as dywTrend "
                + "  from extremePrice.std:groupwin(spolka,type).win:length(2) "
                + " where (value > prev(1,value) and type='max') "
                + "    or (value < prev(1,value) and type='min') ");

        administrator.createEPL(""
                // okno "MFISignal" - oznaczone na schemacie jako "O_08"
                + "insert into MFISignal "
                + "select tMFI.spolka as spolka"
                + "     , tMFI.data as data"
                + "     , case when tMFI.dywTrend='sign1' then cast('-',char) else cast('+',char) end as signal "
                + " from trendsMFI.std:unique(spolka) as tMFI"
                + "      inner join"
                + "      trendsPrice.std:unique(spolka) as tPrice"
                + "   on tMFI.spolka=tPrice.spolka "
                + "  and tMFI.dywTrend=tPrice.dywTrend");

        /////////////////////////////////////////////////////////////////////////////////////////////////

//        // Gra Gieldowa:
//        administrator.createEPL(""
//                // okno "gameSource" - oznaczone na schemacie jako "O_09"
//                + "insert into gameSource "
//                + "select signal.signal as MFISignal"
//                + "     , ka.kursZamkniecia as kursWymiany"
//                + "     , ka.data as data"
//                + "     , ka.spolka as spolka"
//                + "  from MFISignal.win:length(1) signal"
//                + "  inner join "
//                + "       KursAkcji.win:length(1) ka"
//                + "    on signal.spolka = ka.spolka "
//                + "   and signal.data = ka.data");
//
//        administrator.createEPL(""
//                // okno "stackGame" - oznaczone na schemacie jako "O_10" - utworzenie okna
//                + "create window "
//                + "  stackGame.std:firstunique(spolka) "
//                + "    (spolka String"
//                + "    ,forsa Double"
//                + "    ,akcje Double"
//                + "    ,data Date"
//                + "    )");
//
//        administrator.createEPL(""
//                // okno "stackGame" ("O_10") - dodanie pierwszych danych dla kazdej spolki
//                + " insert into stackGame "
//                + " select spolka as spolka"
//                + "      , 1000d as forsa"
//                + "      , 0d as akcje"
//                + "      , data as data"
//                + "   from KursAkcji");
//
//        administrator.createEPL(""
//                // okno "stackGame" ("O_10") - kupno, sprzedaz akcji
//                + "on gameSource as GS "
//                + "  merge stackGame as sg "
//                + "    where GS.spolka = sg.spolka "
//                + "      when matched and GS.MFISignal = '+' "
//                + "      then update set "
//                + "        sg.data = GS.data,"
//                + "        sg.akcje = sg.akcje + cast(sg.forsa / GS.kursWymiany, int),"
//                + "        sg.forsa = sg.forsa - cast(sg.forsa / GS.kursWymiany, int) * GS.kursWymiany"
//                + "      when matched and GS.MFISignal = '-' "
//                + "      then update set "
//                + "        sg.data = GS.data,"
//                + "        sg.forsa = sg.forsa + sg.akcje * GS.kursWymiany,"
//                + "        sg.akcje = 0d");
//
//        /////////////////////////////////////////////////////////////////////////////////////////////////
//        // Wyznaczenie wartosci portfela na ostatni dzien notowan (27.03.2012):
//        EPStatement statement = administrator.createEPL(""
//                + "select sg.spolka as spoka"
//                + "     , sg.akcje as akcje"
//                + "     , sg.forsa as forsa"
//                + "     , ka.kursZamkniecia as kursPrzeliczen"
//                + "     , sg.forsa+sg.akcje*ka.kursZamkniecia as wartosc"
//                + "  from stackGame sg"
//                + "  inner join "
//                // data ostatniego notowania (27.03.2012)
//                + "       KursAkcji( data.getYear() = 2010 "
//                + "              and data.getMonth() = 11 "
//                + "              and data.getDate() = 31"
//                + "                ) as ka unidirectional"
//                + "    on ka.spolka = sg.spolka"
//                + "   and ka.data = sg.data"
//                + "");


        ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        administrator.createEPL("create window kupKurs.win:length(1) (spolka String, data Date, operacja char, kurs Float)");
        administrator.createEPL(
                "insert into kupKurs(spolka, data, operacja, kurs) " +
                        "select k.spolka, ka.data, k.operacja, ka.kursZamkniecia " +
                        "from kup as k " +
                        "join KursAkcji.std:unique(spolka) as ka on k.spolka = ka.spolka " +
                        "join MFISignal.win:length(1) as signal on signal.spolka = ka.spolka " +
                        "where signal.signal = k.operacja"
        );
//        administrator.createEPL(
//                "insert into kupKurs(spolka, data, operacja, kurs) " +
//                        "select k.spolka, ka.data, k.operacja, ka.kursZamkniecia " +
//                        "from kup as k " +
//                        "join KursAkcji.std:unique(spolka) as ka on k.spolka = ka.spolka " +
//                        "left outer join MFISignal.win:length(1) as signal on signal.data = ka.data and signal.spolka = ka.spolka " +
//                        "where signal.signal = k.operacja"
//        );

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

        EPStatement statement = administrator.createEPL(
                "select ka.data, ka.spolka, ka.kursZamkniecia, k.suma_gotowki, k.liczba_akcji, (ka.kursZamkniecia*k.liczba_akcji+k.suma_gotowki) as SUMA " +
                        "from KursAkcji(Main.sprawdz(data, 2010, 11, 31)) as ka unidirectional " +
                        "join portfel as k on k.spolka = ka.spolka and k.data = ka.data"

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
