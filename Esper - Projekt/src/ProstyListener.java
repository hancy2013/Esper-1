import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.UpdateListener;

public class ProstyListener implements UpdateListener {
    @Override
    public void update(EventBean[] newEvents, EventBean[] oldEvents) {
        if (newEvents != null) {
            for (int i = 0; i < newEvents.length; i++) {
                System.out.println("ISTREAM : " + newEvents[i].getUnderlying());
            }
        }
        if (oldEvents != null) {
            for (int i = 0; i < oldEvents.length; i++) {
                System.out.println("RSTREAM : " + oldEvents[i].getUnderlying());
            }
        }
    }
}
