package plan;

import java.util.Date;

/**
 * Created by vkherbac on 26/05/15.
 */
public interface Scheduler {
    
    public static final class Lock {}
    final Object lock = new Lock();

    public class actionDuration {
        Date start, end;
        public actionDuration(Date start, Date end) { this.start = start; this.end = end;  }
        public void setStart(Date start) { this.start = start; }
        public void setEnd(Date end) { this.end = end; }
        public Date getStart() { return start; }
        public Date getEnd() { return end; }
    }
}
