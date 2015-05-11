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

//        // zad 8
//        EPStatement statement = administrator.createEPL(
//                "select kursZamkniecia, data, spolka, (max(kursZamkniecia) - kursZamkniecia) as roznica " +
//                        "from KursAkcji.win:time_batch(1 sec)" ) ;

//        // zad 9
//        EPStatement statement = administrator.createEPL(
//                "select kursZamkniecia, data, spolka, (max(kursZamkniecia) - kursZamkniecia) as roznica " +
//                        "from KursAkcji(spolka IN ('IBM', 'Honda', 'Microsoft')).win:time_batch(1 sec)" ) ;

//        // zad 10 a
//        EPStatement statement = administrator.createEPL(
//                "select kursZamkniecia, data, spolka, kursOtwarcia " +
//                        "from KursAkcji(kursZamkniecia > kursOtwarcia).win:length(1)" ) ;

//        // zad 10 b
//        EPStatement statement = administrator.createEPL(
//                "select kursZamkniecia, data, spolka, kursOtwarcia " +
//                        "from KursAkcji(KursAkcji.wzrost(kursOtwarcia, kursZamkniecia)).win:length(1)" ) ;


//        // zad 11
//        EPStatement statement = administrator.createEPL(
//                "select istream kursZamkniecia, data, spolka, max(kursZamkniecia), (max(kursZamkniecia) - kursZamkniecia) as roznica " +
//                        "from KursAkcji(spolka IN('Oracle', 'Apple')).win:time(8)" ) ;

//        // zad 12
//        EPStatement statement = administrator.createEPL(
//                "select istream max(kursZamkniecia) as maksimum " +
//                        "from KursAkcji().win:time_batch(7)" ) ;

//        // zad 13
//        EPStatement statement = administrator.createEPL(
//                "select istream p.kursZamkniecia as kursPep, p.data, c.kursZamkniecia as kursCoc " +
//                        "from KursAkcji(spolka='PepsiCo').win:time(1 sec) p, " +
//                        "KursAkcji(spolka='CocaCola').win:time(1 sec) c " +
//                        "WHERE p.kursZamkniecia > c.kursZamkniecia" ) ;

//        // zad 14
//        EPStatement statement = administrator.createEPL(
//                "select istream b.data, b.kursZamkniecia as kursBiezacy, (b.kursZamkniecia - o.kursZamkniecia) as roznica, b.spolka " +
//                        "from KursAkcji(spolka IN ('PepsiCo', 'CocaCola')).win:time(1 sec) b, " +
//                        "KursAkcji(spolka IN ('PepsiCo', 'CocaCola')).std:firstunique(spolka) o " +
//                        "WHERE b.spolka = o.spolka" ) ;

//        // zad 15
//        EPStatement statement = administrator.createEPL(
//                "select istream b.data, b.kursZamkniecia as kursBiezacy, (b.kursZamkniecia - o.kursZamkniecia) as roznica, b.spolka " +
//                        "from KursAkcji().win:time(1 sec) b, " +
//                        "KursAkcji().std:firstunique(spolka) o " +
//                        "WHERE b.spolka = o.spolka " +
//                        "AND b.kursZamkniecia > o.kursZamkniecia" ) ;

//        // zad 16 - jak to inaczej zrobić?
//        EPStatement statement = administrator.createEPL(
//                "select istream a.data as dataA, b.data as dataB, a.kursOtwarcia as kursA, b.kursOtwarcia as kursB, b.spolka " +
//                        "from KursAkcji().win:time(7 sec) a, " +
//                        "KursAkcji().win:length(1) b " +
//                        "WHERE a.spolka = b.spolka " +
//                        "AND ( Math.abs(a.kursOtwarcia - b.kursOtwarcia) > 3 )" ) ;

//        // zad 17
//        EPStatement statement = administrator.createEPL(
//                "select istream data, spolka, kursOtwarcia, obrot " +
//                        "from KursAkcji(market='NYSE' AND (obrot BETWEEN 60000 AND 7000000)).win:time_batch(3 sec) " +
//                        "ORDER BY kursOtwarcia DESC" ) ;

        // zad 18 ?????
        EPStatement statement = administrator.createEPL(
                "select istream data, spolka, kursOtwarcia, obrot " +
                        "from KursAkcji(market='NYSE' AND (obrot BETWEEN 60000 AND 7000000)).win:time_batch(3 sec) " +
                        "ORDER BY kursOtwarcia DESC" ) ;

        ProstyListener listener = new ProstyListener();
        statement.addListener(listener);

        InputStream inputStream = new InputStream();
        inputStream.generuj(serviceProvider);

    }

}
